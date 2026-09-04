package com.openclassroom.datashare.controller;

import com.datashare.api.AnonymousFilesApi;
import com.datashare.model.FileResource;
import com.openclassroom.datashare.config.CurrentUserProvider;
import com.openclassroom.datashare.exception.ForbiddenException;
import com.openclassroom.datashare.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * US07: same upload rules as US01, but the file is not attached to any user and
 * therefore never appears in a personal history.
 */
@RestController
@RequiredArgsConstructor
public class AnonymousFileController implements AnonymousFilesApi {

    private final FileService fileService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ResponseEntity<FileResource> anonymousFilesPost(MultipartFile file, String originalName,
                                                           String mimeType, Integer expiresInDays, String password) {
        if (currentUserProvider.isAuthenticated()) {
            // an authenticated caller must use POST /files so the file stays manageable
            throw new ForbiddenException("Anonymous upload is reserved for unauthenticated users");
        }

        FileService.UploadCommand command = new FileService.UploadCommand(
                file, originalName, mimeType, expiresInDays, password, List.of(), null);

        FileService.UploadResult result = fileService.upload(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FileResourceMapper.toApiModel(result.file(), result.downloadToken()));
    }
}