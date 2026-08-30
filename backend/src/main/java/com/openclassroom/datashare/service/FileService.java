package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.DownloadToken;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ForbiddenException;
import com.openclassroom.datashare.exception.PayloadTooLargeException;
import com.openclassroom.datashare.exception.ResourceNotFoundException;
import com.openclassroom.datashare.repository.DownloadTokenRepository;
import com.openclassroom.datashare.repository.FileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_SIZE_BYTES = 1024L * 1024L * 1024L; // 1 GB
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final FileRepository fileRepository;
    private final DownloadTokenRepository downloadTokenRepository;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.forbidden-extensions:.exe,.bat,.cmd,.sh,.msi,.dll,.com,.scr,.jar,.vbs,.ps1}")
    private String forbiddenExtensionsProperty;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    public UploadResult uploadFile(MultipartFile file, String originalName,
            Integer expiresInDays,
            String mimeType, String password, List<String> tags, Long ownerId) {

        validateUpload(file, expiresInDays, password);

        String key = "files/" + UUID.randomUUID() + "-" + sanitize(originalName);
        String resolvedMimeType = (mimeType != null && !mimeType.isBlank())
                ? mimeType
                : (file.getContentType() != null ? file.getContentType() : "application/octet-stream");

        storageService.store(file, key, resolvedMimeType);

        File entity = new File();
        entity.setOwnerId(ownerId);
        entity.setOriginalName(originalName);
        entity.setMimeType(resolvedMimeType);
        entity.setSizeBytes(file.getSize());
        entity.setStorageKey(key);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
        if (password != null && !password.isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(password));
        }

        entity = fileRepository.save(entity);
        // a download link is generated immediately so the caller can share/store it
        // without a second call
        DownloadTokenResult tokenResult = generateDownloadToken(entity);
        return new UploadResult(entity, tokenResult);
    }

    public Page<File> listFiles(Long ownerId, int page, int size, String tag,
            String q) {
        Specification<File> spec = (root, query, cb) -> cb.equal(root.get("ownerId"),
                ownerId);

        if (tag != null && !tag.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("tags").get("name"), tag));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("originalName")),
                    "%" + q.toLowerCase(Locale.ROOT) + "%"));
        }

        return fileRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public File getOwnedFile(Long fileId, Long ownerId) {
        // returned as "not found" (rather than forbidden) to avoid leaking the
        // existence of files owned by others
        return fileRepository.findByFileIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    public File requireOwnedFile(Long fileId, Long ownerId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!ownerId.equals(file.getOwnerId())) {
            throw new ForbiddenException("You do not own this file");
        }
        return file;
    }

    public DownloadTokenResult createDownloadToken(Long fileId, Long ownerId) {
        File file = requireOwnedFile(fileId, ownerId);
        return generateDownloadToken(file);
    }

    private DownloadTokenResult generateDownloadToken(File file) {
        // flush so the DELETE reaches the DB before the INSERT below (Hibernate would
        // otherwise order the insert first)
        downloadTokenRepository.findByFile_FileId(file.getFileId()).ifPresent(existing -> {
            downloadTokenRepository.delete(existing);
            downloadTokenRepository.flush();
        });

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        DownloadToken downloadToken = new DownloadToken();
        downloadToken.setFile(file);
        downloadToken.setTokenHash(hash(rawToken));
        downloadToken = downloadTokenRepository.save(downloadToken);

        URI downloadUrl = URI.create(publicUrl + "/downloads/" + rawToken);
        return new DownloadTokenResult(rawToken, downloadUrl, downloadToken.getCreatedAt());
    }

    public File updateFile(Long fileId, Long ownerId, String originalName,
            Integer expiresInDays, String password, List<String> tags) {
        File file = requireOwnedFile(fileId, ownerId);

        if (originalName != null && !originalName.isBlank()) {
            file.setOriginalName(originalName);
        }
        if (expiresInDays != null) {
            if (expiresInDays < 1 || expiresInDays > 7) {
                throw new BadRequestException("expiresInDays must be between 1 and 7");
            }
            file.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
        }
        if (password != null) {
            if (!password.isBlank() && password.length() < 6) {
                throw new BadRequestException("password must be at least 6 characters long");
            }
            file.setPasswordHash(password.isBlank() ? null : passwordEncoder.encode(password));
        }
        return fileRepository.save(file);
    }

    public void deleteFile(Long fileId, Long ownerId) {
        File file = requireOwnedFile(fileId, ownerId);
        downloadTokenRepository.findByFile_FileId(fileId).ifPresent(downloadTokenRepository::delete);
        storageService.delete(file.getStorageKey());
        fileRepository.delete(file);
    }

    private void validateUpload(MultipartFile file, Integer expiresInDays, String password) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The uploaded file must not be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new PayloadTooLargeException("Maximum file size is 1 GB");
        }
        if (expiresInDays == null || expiresInDays < 1 || expiresInDays > 7) {
            throw new BadRequestException("expiresInDays must be between 1 and 7");
        }
        if (password != null && !password.isBlank() && password.length() < 6) {
            throw new BadRequestException("password must be at least 6 characters long");
        }
        String originalName = file.getOriginalFilename();
        if (isForbiddenExtension(originalName)) {
            throw new BadRequestException("This file type is not allowed");
        }
    }

    private boolean isForbiddenExtension(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return forbiddenExtensions().stream().anyMatch(lower::endsWith);
    }

    private List<String> forbiddenExtensions() {
        return List.of(forbiddenExtensionsProperty.split(","));
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record DownloadTokenResult(String token, URI downloadUrl, OffsetDateTime createdAt) {
    }

    public record UploadResult(File file, DownloadTokenResult downloadToken) {
    }
}
