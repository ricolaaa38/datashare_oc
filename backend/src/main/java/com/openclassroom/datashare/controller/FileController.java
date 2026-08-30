package com.openclassroom.datashare.controller;

import com.datashare.api.FilesApi;
import com.datashare.model.DownloadTokenResponse;
import com.datashare.model.FileResource;
import com.datashare.model.FileUpdateRequest;
import com.datashare.model.PagedFiles;
import com.datashare.model.PresignedUrlResponse;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.repository.UserRepository;
import com.openclassroom.datashare.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesApi {

    private final FileService fileService;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<FileResource> filesPost(MultipartFile file, String originalName,
            Integer expiresInDays, String mimeType, String password, List<String> tags) {
        Long ownerId = currentUserId();
        FileService.UploadResult result = fileService.uploadFile(
                file, originalName, expiresInDays, mimeType, password, tags, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiModel(result.file(), result.downloadToken()));
    }

    @Override
    public ResponseEntity<PagedFiles> filesGet(Integer page, Integer size, String tag, String q) {
        Long ownerId = currentUserId();
        Page<File> result = fileService.listFiles(ownerId, page, size, tag, q);

        PagedFiles response = new PagedFiles()
                .total((int) result.getTotalElements())
                .page(page)
                .size(size)
                .items(result.getContent().stream().map(f -> toApiModel(f, null)).toList());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FileResource> filesFileIdGet(Integer fileId) {
        Long ownerId = currentUserId();
        File file = fileService.getOwnedFile(Long.valueOf(fileId), ownerId);
        return ResponseEntity.ok(toApiModel(file, null));
    }

    @Override
    public ResponseEntity<FileResource> filesFileIdPatch(Integer fileId, FileUpdateRequest fileUpdateRequest) {
        Long ownerId = currentUserId();
        File updated = fileService.updateFile(Long.valueOf(fileId), ownerId,
                fileUpdateRequest.getOriginalName(), fileUpdateRequest.getExpiresInDays(),
                fileUpdateRequest.getPassword(), fileUpdateRequest.getTags());
        return ResponseEntity.ok(toApiModel(updated, null));
    }

    @Override
    public ResponseEntity<Void> filesFileIdDelete(Integer fileId) {
        Long ownerId = currentUserId();
        fileService.deleteFile(Long.valueOf(fileId), ownerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DownloadTokenResponse> filesFileIdDownloadTokensPost(Integer fileId) {
        Long ownerId = currentUserId();
        FileService.DownloadTokenResult result = fileService.createDownloadToken(Long.valueOf(fileId), ownerId);
        DownloadTokenResponse response = new DownloadTokenResponse()
                .token(result.token())
                .downloadUrl(result.downloadUrl())
                .createdAt(result.createdAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<PresignedUrlResponse> filesFileIdPresignedUrlGet(Integer fileId, String operation) {
        // Direct S3 storage is handled server-side by POST /files; presigned URLs are
        // not implemented yet.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private FileResource toApiModel(File file, FileService.DownloadTokenResult downloadToken) {
        return new FileResource()
                .fileId(Math.toIntExact(file.getFileId()))
                .ownerId(file.getOwnerId() != null ? Math.toIntExact(file.getOwnerId()) : null)
                .originalName(file.getOriginalName())
                .storageKey(file.getStorageKey())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .createdAt(file.getCreatedAt())
                .expiresAt(file.getExpiresAt())
                .hasPassword(file.getPasswordHash() != null)
                .downloadToken(downloadToken != null ? downloadToken.token() : null)
                .downloadUrl(downloadToken != null ? downloadToken.downloadUrl() : null);
    }

    private Long currentUserId() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + login))
                .getId();
    }
}
