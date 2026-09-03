package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.DownloadToken;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.exception.ResourceNotFoundException;
import com.openclassroom.datashare.repository.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Single owner of the download-token lifecycle: generation, hashing, rotation
 * and resolution. The raw token is returned to the caller only once; only its
 * SHA-256 hash is persisted.
 */
@Service
@RequiredArgsConstructor
public class DownloadTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final DownloadTokenRepository downloadTokenRepository;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    /**
     * Issues a token for the file, replacing (and therefore invalidating) any
     * previous one, since the data model allows a single token per file.
     */
    @Transactional
    public IssuedToken issueFor(File file) {
        // flush so the DELETE reaches the database before the INSERT below,
        // which Hibernate would otherwise order the other way around
        downloadTokenRepository.findByFile_FileId(file.getFileId()).ifPresent(existing -> {
            downloadTokenRepository.delete(existing);
            downloadTokenRepository.flush();
        });

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        DownloadToken downloadToken = new DownloadToken();
        downloadToken.setFile(file);
        downloadToken.setTokenHash(hash(rawToken));
        downloadToken = downloadTokenRepository.save(downloadToken);

        URI downloadUrl = URI.create(publicUrl + "/downloads/" + rawToken);
        return new IssuedToken(rawToken, downloadUrl, downloadToken.getCreatedAt());
    }

    /**
     * Resolves the token to its download link. An unknown token is reported as
     * "not found" so the recipient gets an explicit error, distinct from a wrong
     * password. This does not help enumeration: the token is 256 bits of entropy.
     */
    @Transactional(readOnly = true)
    public DownloadToken resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResourceNotFoundException("This download link is invalid or no longer exists");
        }
        return downloadTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This download link is invalid or no longer exists"));
    }

    @Transactional
    public void deleteFor(Long fileId) {
        downloadTokenRepository.findByFile_FileId(fileId).ifPresent(downloadTokenRepository::delete);
        downloadTokenRepository.flush();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record IssuedToken(String token, URI downloadUrl, OffsetDateTime createdAt) {
    }
}
