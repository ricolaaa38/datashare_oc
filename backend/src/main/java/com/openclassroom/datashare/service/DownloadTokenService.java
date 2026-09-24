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
 * Service class for managing download tokens.
 * Provides methods to issue, resolve, and delete download tokens associated with files.
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
     * @param file The File entity to issue a token for.
     * @return The issued token information.
     */
    @Transactional
    public IssuedToken issueFor(File file) {
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
     * Resolves a raw token to its corresponding DownloadToken entity.
     * @param rawToken The raw, cryptographically unpredictable download token. It is never stored directly in the database, only its hash is.
     * @return The resolved DownloadToken entity.
     * @throws ResourceNotFoundException if the token is invalid or no longer exists.
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

    /**
     * Deletes the download token associated with the given file ID.
     * @param fileId The ID of the file whose download token should be deleted.
     */
    @Transactional
    public void deleteFor(Long fileId) {
        downloadTokenRepository.findByFile_FileId(fileId).ifPresent(downloadTokenRepository::delete);
        downloadTokenRepository.flush();
    }

    /**
     * Hashes the raw token using SHA-256.
     * @param rawToken The raw token to hash.
     * @return The hexadecimal representation of the hashed token.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Represents an issued download token with its associated information.
     * @param token The raw download token.
     * @param downloadUrl The URL to download the file using the token.
     * @param createdAt The timestamp when the token was created.
     */
    public record IssuedToken(String token, URI downloadUrl, OffsetDateTime createdAt) {
    }
}
