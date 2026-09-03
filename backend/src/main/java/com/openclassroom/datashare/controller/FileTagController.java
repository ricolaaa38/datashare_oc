package com.openclassroom.datashare.controller;

import com.datashare.api.FileTagsApi;
import com.datashare.model.TagCreateRequest;
import com.openclassroom.datashare.config.CurrentUserProvider;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.service.FileTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileTagController implements FileTagsApi {

    private final FileTagService fileTagService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ResponseEntity<List<com.datashare.model.FileTag>> tagsGet(Integer page, Integer size) {
        List<com.datashare.model.FileTag> tags = fileTagService
                .listTags(currentUserProvider.requireCurrentUserId(), page, size)
                .stream()
                .map(FileTagController::toApiModel)
                .toList();
        return ResponseEntity.ok(tags);
    }

    @Override
    public ResponseEntity<com.datashare.model.FileTag> tagsPost(TagCreateRequest tagCreateRequest) {
        FileTag created = fileTagService.createTag(currentUserProvider.requireCurrentUserId(),
                tagCreateRequest.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiModel(created));
    }

    private static com.datashare.model.FileTag toApiModel(FileTag tag) {
        return new com.datashare.model.FileTag()
                .tagId((long) Math.toIntExact(tag.getTagId()))
                .name(tag.getName())
                .userId((long) Math.toIntExact(tag.getUserId()));
    }
}
