package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.DownloadToken;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.exception.FileExpiredException;
import com.openclassroom.datashare.exception.InvalidTokenOrPasswordException;
import com.openclassroom.datashare.repository.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadTokenRepository downloadTokenRepository;
    private final FileStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    public DownloadResult downloadByToken(String rawToken, String password) {
        DownloadToken downloadToken = downloadTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenOrPasswordException("Invalid or unknown download token"));

        File file = downloadToken.getFile();

        if (file.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new FileExpiredException("This file has expired");
        }

        if (file.getPasswordHash() != null
                && (password == null || !passwordEncoder.matches(password, file.getPasswordHash()))) {
            throw new InvalidTokenOrPasswordException("A valid password is required to download this file");
        }

        InputStreamResource resource = new InputStreamResource(storageService.load(file.getStorageKey()));
        return new DownloadResult(resource, file);
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

    public record DownloadResult(InputStreamResource resource, File file) {
    }
}
