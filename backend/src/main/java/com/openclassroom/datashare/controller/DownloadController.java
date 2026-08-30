package com.openclassroom.datashare.controller;

import com.datashare.api.DownloadsApi;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DownloadController implements DownloadsApi {

    private final DownloadService downloadService;

    @Override
    public ResponseEntity<Resource> downloadsTokenGet(String token, String xFilePassword) {
        DownloadService.DownloadResult result = downloadService.downloadByToken(token, xFilePassword);
        File file = result.file();

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOriginalName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .contentLength(file.getSizeBytes())
                .body(result.resource());
    }
}
