package com.openclassroom.datashare.controller;

import com.datashare.api.DownloadsApi;
import com.datashare.model.DownloadMetadata;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Controller for handling file downloads.
 * Provides endpoints to retrieve file metadata and download files using a unique token.
 */
@RestController
@RequiredArgsConstructor
public class DownloadController implements DownloadsApi {

    private final DownloadService downloadService;

    /**
     * Retrieves metadata for a file associated with the given download token.
     * @param token The raw, cryptographically unpredictable download token. It is never stored directly in the database, only its hash is.
     * @return A ResponseEntity containing the download metadata.
     */
    @Override
    public ResponseEntity<DownloadMetadata> downloadsTokenMetadataGet(String token) {
        File file = downloadService.describeByToken(token);

        DownloadMetadata metadata = new DownloadMetadata()
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .sizeBytes(file.getSizeBytes())
                .expiresAt(file.getExpiresAt())
                .hasPassword(file.getPasswordHash() != null);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Downloads a file associated with the given download token.
     * @param token The raw, cryptographically unpredictable download token. It is never stored directly in the database, only its hash is.
     * @param xFilePassword The password for the file, if it is password-protected.
     * @return A ResponseEntity containing the file as a Resource.
     */
    @Override
    public ResponseEntity<Resource> downloadsTokenGet(String token, String xFilePassword) {
        DownloadService.DownloadResult result = downloadService.downloadByToken(token, xFilePassword);
        File file = result.file();

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(safeMediaType(file.getMimeType()))
                .contentLength(file.getSizeBytes())
                .body(result.resource());
    }

    /**
     * Safely parses the given MIME type string into a MediaType object.
     * If the MIME type is invalid, it defaults to application/octet-stream.
     * @param mimeType The MIME type string to parse.
     * @return A MediaType object representing the parsed MIME type or application/octet-stream if invalid.
     */
    private MediaType safeMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
