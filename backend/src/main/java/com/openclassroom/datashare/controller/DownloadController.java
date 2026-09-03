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

@RestController
@RequiredArgsConstructor
public class DownloadController implements DownloadsApi {

    private final DownloadService downloadService;

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

    @Override
    public ResponseEntity<Resource> downloadsTokenGet(String token, String xFilePassword) {
        DownloadService.DownloadResult result = downloadService.downloadByToken(token, xFilePassword);
        File file = result.file();

        // the UTF-8 form keeps accented filenames readable instead of mangling them
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
     * A stored MIME type that cannot be parsed must not turn a valid download into
     * a 500; the generic binary type is always an acceptable answer.
     */
    private MediaType safeMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
