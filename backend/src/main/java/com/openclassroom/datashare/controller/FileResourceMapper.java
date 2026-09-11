package com.openclassroom.datashare.controller;

import com.datashare.model.FileResource;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.service.DownloadTokenService;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Maps the entity to the API contract. {@code storageKey} is deliberately not
 * exposed: it is an internal bucket coordinate.
 */
final class FileResourceMapper {

    private FileResourceMapper() {
    }

    static FileResource toApiModel(File file, DownloadTokenService.IssuedToken downloadToken) {
        List<String> tagNames = file.getTags().stream()
                .map(FileTag::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        FileResource.StatusEnum status = file.isExpired(OffsetDateTime.now())
                ? FileResource.StatusEnum.EXPIRED
                : FileResource.StatusEnum.VALID;

        return new FileResource()
                .fileId(file.getFileId())
                .ownerId(file.getOwnerId())
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .createdAt(file.getCreatedAt())
                .expiresAt(file.getExpiresAt())
                .hasPassword(file.getPasswordHash() != null)
                .status(status)
                .tags(tagNames)
                .downloadToken(downloadToken != null ? downloadToken.token() : null)
                .downloadUrl(downloadToken != null ? downloadToken.downloadUrl() : null);
    }
}
