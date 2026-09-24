package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.exception.FileExpiredException;
import com.openclassroom.datashare.exception.InvalidTokenOrPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Closeable;
import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Service for handling file downloads, including token resolution, password validation,
 * and file retrieval from storage.
 */
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadTokenService downloadTokenService;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Describes a file associated with the given download token.
     * @param rawToken The raw, cryptographically unpredictable download token. It is never stored directly in the database, only its hash is.
     * @return The File entity associated with the token.
     * @throws FileExpiredException if the file has expired.
     */
    @Transactional(readOnly = true)
    public File describeByToken(String rawToken) {
        File file = downloadTokenService.resolve(rawToken).getFile();
        requireNotExpired(file);
        return file;
    }

    /**
     * Downloads a file associated with the given download token, validating the password if required.
     * @param rawToken The raw, cryptographically unpredictable download token. It is never stored directly in the database, only its hash is.
     * @param password The password for the file, if it is password-protected.
     * @return A DownloadResult containing the InputStreamResource and File entity.
     * @throws FileExpiredException if the file has expired.
     * @throws InvalidTokenOrPasswordException if the password is invalid or missing for a protected file.
     */
    @Transactional(readOnly = true)
    public DownloadResult downloadByToken(String rawToken, String password) {

        File file = downloadTokenService.resolve(rawToken).getFile();
        requireNotExpired(file);
        requireValidPassword(file, password);

        var content = storageService.load(file.getStorageKey());
        try {
            return new DownloadResult(new InputStreamResource(content), file);
        } catch (RuntimeException e) {
            closeQuietly(content);
            throw e;
        }
    }

    /**
     * Checks if the file has expired and throws an exception if it has.
     * @param file The File entity to check for expiration.
     * @throws FileExpiredException if the file has expired.
     */
    private void requireNotExpired(File file) {
        if (file.isExpired(OffsetDateTime.now())) {
            throw new FileExpiredException("This link has expired and the file is no longer available");
        }
    }

    /**
     * Checks if the provided password is valid for the given file and throws an exception if it is not.
     * @param file The File entity to check the password for.
     * @param password The password to validate.
     * @throws InvalidTokenOrPasswordException if the password is invalid or missing for a protected file.
     */
    private void requireValidPassword(File file, String password) {
        if (file.getPasswordHash() == null) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new InvalidTokenOrPasswordException("This file is password protected, a password is required");
        }
        if (!passwordEncoder.matches(password, file.getPasswordHash())) {
            throw new InvalidTokenOrPasswordException("The supplied password is incorrect");
        }
    }

    /**
     * Closes the given Closeable quietly, ignoring any IOException that occurs.
     * @param closeable The Closeable to close.
     */
    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * A record representing the result of a download operation, containing the InputStreamResource and the File entity.
     * @param resource The InputStreamResource of the file content.
     * @param file The File entity.
     */
    public record DownloadResult(InputStreamResource resource, File file) {
    }
}
