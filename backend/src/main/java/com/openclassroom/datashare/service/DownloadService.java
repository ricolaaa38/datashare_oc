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
 * Serves US02: anyone holding a valid link can see what is shared and download
 * it, providing the password when the file is protected.
 */
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadTokenService downloadTokenService;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Metadata shown to the recipient before downloading. Deliberately requires
     * no password: it only reveals what the sender chose to share, and the
     * {@code hasPassword} flag is what lets the client decide whether to prompt
     * for one.
     */
    @Transactional(readOnly = true)
    public File describeByToken(String rawToken) {
        File file = downloadTokenService.resolve(rawToken).getFile();
        requireNotExpired(file);
        return file;
    }

    @Transactional(readOnly = true)
    public DownloadResult downloadByToken(String rawToken, String password) {

        File file = downloadTokenService.resolve(rawToken).getFile();
        requireNotExpired(file);
        requireValidPassword(file, password);

        // the stream stays open until the response body has been written, so it is
        // only closed here when building the result fails
        var content = storageService.load(file.getStorageKey());
        try {
            return new DownloadResult(new InputStreamResource(content), file);
        } catch (RuntimeException e) {
            closeQuietly(content);
            throw e;
        }
    }

    private void requireNotExpired(File file) {
        if (file.isExpired(OffsetDateTime.now())) {
            // the scheduled cleanup deletes the content shortly after expiry; refuse
            // the link immediately so an expired one is never served
            throw new FileExpiredException("This link has expired and the file is no longer available");
        }
    }

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

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // nothing useful to do: the original failure is the one worth reporting
        }
    }

    public record DownloadResult(InputStreamResource resource, File file) {
    }
}
