package com.openclassroom.datashare.controller;

import com.datashare.api.FilesApi;
import com.datashare.model.DownloadTokenResponse;
import com.datashare.model.FileResource;
import com.datashare.model.FileTagsUpdateRequest;
import com.datashare.model.FileUpdateRequest;
import com.datashare.model.PagedFiles;
import com.datashare.model.PresignedUrlResponse;
import com.openclassroom.datashare.config.CurrentUserProvider;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.service.DownloadTokenService;
import com.openclassroom.datashare.service.FileService;
import com.openclassroom.datashare.service.FileTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesApi {

    private final FileService fileService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ResponseEntity<FileResource> filesPost(MultipartFile file, String originalName, String mimeType,
            Integer expiresInDays, String password, String tags) {
        FileService.UploadCommand command = new FileService.UploadCommand(
                file, originalName, mimeType, expiresInDays, password,
                FileTagService.parseCommaSeparated(tags), currentUserProvider.requireCurrentUserId());

        FileService.UploadResult result = fileService.upload(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiModel(result.file(), result.downloadToken()));
    }

    @Override
    public ResponseEntity<PagedFiles> filesGet(Integer page, Integer size, String tag, String q) {
        Page<File> result = fileService.listFiles(currentUserProvider.requireCurrentUserId(), page, size, tag, q);

        PagedFiles response = new PagedFiles()
                .total((int) result.getTotalElements())
                .page(page)
                .size(size)
                .items(result.getContent().stream().map(f -> toApiModel(f, null)).toList());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FileResource> filesFileIdGet(Long fileId) {
        File file = fileService.getOwnedFile(fileId, currentUserProvider.requireCurrentUserId());
        return ResponseEntity.ok(toApiModel(file, null));
    }

    @Override
    public ResponseEntity<FileResource> filesFileIdPatch(Long fileId, FileUpdateRequest fileUpdateRequest) {
        File updated = fileService.updateFile(fileId, currentUserProvider.requireCurrentUserId(),
                fileUpdateRequest.getOriginalName(), fileUpdateRequest.getExpiresInDays(),
                fileUpdateRequest.getPassword(), fileUpdateRequest.getTags());
        return ResponseEntity.ok(toApiModel(updated, null));
    }

    @Override
    public ResponseEntity<FileResource> filesFileIdTagsPut(Long fileId, FileTagsUpdateRequest request) {
        File updated = fileService.replaceTags(fileId, currentUserProvider.requireCurrentUserId(), request.getTags());
        return ResponseEntity.ok(toApiModel(updated, null));
    }

    @Override
    public ResponseEntity<Void> filesFileIdDelete(Long fileId) {
        fileService.deleteFile(fileId, currentUserProvider.requireCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DownloadTokenResponse> filesFileIdDownloadTokensPost(Long fileId) {
        DownloadTokenService.IssuedToken issued = fileService.createDownloadToken(fileId,
                currentUserProvider.requireCurrentUserId());
        DownloadTokenResponse response = new DownloadTokenResponse()
                .token(issued.token())
                .downloadUrl(issued.downloadUrl())
                .createdAt(issued.createdAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<PresignedUrlResponse> filesFileIdPresignedUrlGet(Long fileId, String operation) {
        // Direct S3 storage is handled server-side by POST /files; presigned URLs are
        // not implemented yet.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Maps the entity to the API contract. {@code storageKey} is deliberately not
     * exposed: it is an internal bucket coordinate.
     */
    private FileResource toApiModel(File file, DownloadTokenService.IssuedToken downloadToken) {
        List<String> tagNames = file.getTags().stream()
                .map(FileTag::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        return new FileResource()
                .fileId(file.getFileId())
                .ownerId(file.getOwnerId())
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .createdAt(file.getCreatedAt())
                .expiresAt(file.getExpiresAt())
                .hasPassword(file.getPasswordHash() != null)
                .tags(tagNames)
                .downloadToken(downloadToken != null ? downloadToken.token() : null)
                .downloadUrl(downloadToken != null ? downloadToken.downloadUrl() : null);
    }
}
