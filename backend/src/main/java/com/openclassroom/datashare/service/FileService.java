package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ForbiddenException;
import com.openclassroom.datashare.exception.PayloadTooLargeException;
import com.openclassroom.datashare.exception.ResourceNotFoundException;
import com.openclassroom.datashare.repository.FileRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Business rules of US01: an authenticated user uploads a file, gets a unique
 * download link, and finds the file again in a personal history.
 */
@Service
public class FileService {

    public static final long MAX_SIZE_BYTES = 1024L * 1024L * 1024L; // 1 GB
    public static final int MAX_EXPIRATION_DAYS = 7;
    public static final int DEFAULT_EXPIRATION_DAYS = 7;
    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final FileRepository fileRepository;
    private final DownloadTokenService downloadTokenService;
    private final FileTagService fileTagService;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final List<String> forbiddenExtensions;

    public FileService(FileRepository fileRepository,
            DownloadTokenService downloadTokenService,
            FileTagService fileTagService,
            FileStorageService storageService,
            PasswordEncoder passwordEncoder,
            @Value("${app.upload.forbidden-extensions:.exe,.bat,.cmd,.sh,.msi,.dll,.com,.scr,.jar,.vbs,.ps1}") String forbiddenExtensions) {
        this.fileRepository = fileRepository;
        this.downloadTokenService = downloadTokenService;
        this.fileTagService = fileTagService;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
        this.forbiddenExtensions = normalizeExtensions(forbiddenExtensions);
    }

    @Transactional
    public UploadResult upload(UploadCommand command) {
        MultipartFile file = command.file();
        String originalName = resolveOriginalName(command.originalName(), file);
        int expiresInDays = command.expiresInDays() == null ? DEFAULT_EXPIRATION_DAYS : command.expiresInDays();

        validateUpload(file, originalName, expiresInDays, command.password());

        String storageKey = "files/" + UUID.randomUUID() + "-" + sanitize(originalName);
        String mimeType = resolveMimeType(command.mimeType(), file);
        Set<FileTag> tags = fileTagService.resolveOrCreate(command.ownerId(), command.tags());

        storageService.store(file, storageKey, mimeType);
        try {
            File entity = new File();
            entity.setOwnerId(command.ownerId());
            entity.setOriginalName(originalName);
            entity.setMimeType(mimeType);
            entity.setSizeBytes(file.getSize());
            entity.setStorageKey(storageKey);
            entity.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
            entity.setTags(tags);
            if (hasPassword(command.password())) {
                entity.setPasswordHash(passwordEncoder.encode(command.password()));
            }

            entity = fileRepository.save(entity);
            // the download link is issued right away so the caller does not need a
            // second round trip to share the file
            return new UploadResult(entity, downloadTokenService.issueFor(entity));
        } catch (RuntimeException e) {
            // never leave an orphan object behind in the bucket
            storageService.delete(storageKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<File> listFiles(Long ownerId, int page, int size, String tag, String q) {
        Specification<File> spec = ownedBy(ownerId).and(notExpired());

        if (tag != null && !tag.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                // a file may match several rows through the FILE_TAG join
                query.distinct(true);
                return cb.equal(cb.lower(root.join("tags", JoinType.INNER).get("name")), tag.toLowerCase(Locale.ROOT));
            });
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("originalName")),
                    "%" + q.toLowerCase(Locale.ROOT) + "%"));
        }

        return fileRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Returns the file only when it belongs to the caller. A file owned by
     * somebody else is reported as "not found" rather than "forbidden" so the API
     * does not leak the existence of other users' files.
     */
    @Transactional(readOnly = true)
    public File getOwnedFile(Long fileId, Long ownerId) {
        return fileRepository.findByFileIdAndOwnerId(fileId, ownerId)
                .filter(file -> !file.isExpired(OffsetDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    @Transactional(readOnly = true)
    public File requireOwnedFile(Long fileId, Long ownerId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!java.util.Objects.equals(ownerId, file.getOwnerId())) {
            throw new ForbiddenException("You do not own this file");
        }
        if (file.isExpired(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("File not found");
        }
        return file;
    }

    @Transactional
    public DownloadTokenService.IssuedToken createDownloadToken(Long fileId, Long ownerId) {
        return downloadTokenService.issueFor(requireOwnedFile(fileId, ownerId));
    }

    @Transactional
    public File updateFile(Long fileId, Long ownerId, String originalName,
            Integer expiresInDays, String password, List<String> tags) {
        File file = requireOwnedFile(fileId, ownerId);

        if (originalName != null && !originalName.isBlank()) {
            file.setOriginalName(originalName.trim());
        }
        if (expiresInDays != null) {
            validateExpiration(expiresInDays);
            file.setExpiresAt(OffsetDateTime.now().plusDays(expiresInDays));
        }
        if (password != null) {
            // an explicit blank password removes the protection
            if (password.isBlank()) {
                file.setPasswordHash(null);
            } else {
                validatePassword(password);
                file.setPasswordHash(passwordEncoder.encode(password));
            }
        }
        if (tags != null) {
            file.setTags(fileTagService.resolveOrCreate(ownerId, tags));
        }
        return fileRepository.save(file);
    }

    @Transactional
    public File replaceTags(Long fileId, Long ownerId, List<String> tags) {
        File file = requireOwnedFile(fileId, ownerId);
        file.setTags(fileTagService.resolveOrCreate(ownerId, tags == null ? List.of() : tags));
        return fileRepository.save(file);
    }

    @Transactional
    public void deleteFile(Long fileId, Long ownerId) {
        purge(requireOwnedFile(fileId, ownerId));
    }

    /**
     * Removes the metadata, the FILE_TAG associations, the download token and the
     * stored object. The bucket deletion runs inside the transaction so that a
     * storage failure rolls the whole thing back and lets the caller retry.
     */
    @Transactional
    public void purge(File file) {
        downloadTokenService.deleteFor(file.getFileId());
        file.getTags().clear();
        fileRepository.delete(file);
        fileRepository.flush();
        storageService.delete(file.getStorageKey());
    }

    @Transactional(readOnly = true)
    public List<File> findExpired(OffsetDateTime now, int batchSize) {
        return fileRepository.findAllByExpiresAtBefore(now, PageRequest.of(0, batchSize));
    }

    private void validateUpload(MultipartFile file, String originalName, int expiresInDays, String password) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The uploaded file must not be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new PayloadTooLargeException("Maximum file size is 1 GB");
        }
        validateExpiration(expiresInDays);
        if (hasPassword(password)) {
            validatePassword(password);
        }
        if (isForbiddenExtension(originalName)) {
            throw new BadRequestException("This file type is not allowed");
        }
    }

    private void validateExpiration(int expiresInDays) {
        if (expiresInDays < 1 || expiresInDays > MAX_EXPIRATION_DAYS) {
            throw new BadRequestException("expiresInDays must be between 1 and " + MAX_EXPIRATION_DAYS);
        }
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException(
                    "password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }

    private boolean hasPassword(String password) {
        return password != null && !password.isBlank();
    }

    private boolean isForbiddenExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return forbiddenExtensions.stream().anyMatch(lower::endsWith);
    }

    /**
     * Falls back to the name carried by the multipart part, and keeps only the
     * last path segment because some clients send a full path.
     */
    private String resolveOriginalName(String requestedName, MultipartFile file) {
        String candidate = requestedName != null && !requestedName.isBlank()
                ? requestedName
                : (file == null ? null : file.getOriginalFilename());
        if (candidate == null || candidate.isBlank()) {
            return "file";
        }
        String baseName = Paths.get(candidate.replace('\\', '/')).getFileName().toString().trim();
        return baseName.isEmpty() ? "file" : baseName;
    }

    private String resolveMimeType(String requestedMimeType, MultipartFile file) {
        if (requestedMimeType != null && !requestedMimeType.isBlank()) {
            return requestedMimeType;
        }
        String contentType = file == null ? null : file.getContentType();
        return contentType != null && !contentType.isBlank() ? contentType : DEFAULT_MIME_TYPE;
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Specification<File> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    private static Specification<File> notExpired() {
        // an expired file is logically gone even if the cleanup job has not run yet
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expiresAt"), OffsetDateTime.now());
    }

    private static List<String> normalizeExtensions(String rawExtensions) {
        return Arrays.stream(rawExtensions.split(","))
                .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                .filter(extension -> !extension.isEmpty())
                .map(extension -> extension.startsWith(".") ? extension : "." + extension)
                .toList();
    }

    public record UploadCommand(MultipartFile file, String originalName, String mimeType,
            Integer expiresInDays, String password, List<String> tags, Long ownerId) {
    }

    public record UploadResult(File file, DownloadTokenService.IssuedToken downloadToken) {
    }
}
