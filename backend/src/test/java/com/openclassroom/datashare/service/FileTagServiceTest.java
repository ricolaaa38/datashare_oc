package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ConflictException;
import com.openclassroom.datashare.repository.FileTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTagServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private FileTagRepository fileTagRepository;

    @InjectMocks
    private FileTagService fileTagService;

    @Test
    void commaSeparatedListIsParsed() {
        assertThat(FileTagService.parseCommaSeparated("photos, vacances")).containsExactly("photos", " vacances");
        assertThat(FileTagService.parseCommaSeparated(null)).isEmpty();
        assertThat(FileTagService.parseCommaSeparated("  ")).isEmpty();
    }

    @Test
    void namesAreTrimmedDeduplicatedAndReusedWhenTheyAlreadyExist() {
        FileTag existing = new FileTag(1L, USER_ID, "photos");
        when(fileTagRepository.findByUserIdAndNameIgnoreCase(USER_ID, "photos")).thenReturn(Optional.of(existing));

        Set<FileTag> resolved = fileTagService.resolveOrCreate(USER_ID, List.of(" photos ", "PHOTOS", "  "));

        assertThat(resolved).containsExactly(existing);
        verify(fileTagRepository, times(1)).findByUserIdAndNameIgnoreCase(eq(USER_ID), any());
        verify(fileTagRepository, times(0)).save(any());
    }

    @Test
    void anonymousUploadsDoNotGetTags() {
        assertThat(fileTagService.resolveOrCreate(null, List.of("photos"))).isEmpty();
    }

    @Test
    void aTagNameLongerThanThirtyCharactersIsRejected() {
        String tooLong = "a".repeat(31);

        assertThatThrownBy(() -> fileTagService.createTag(USER_ID, tooLong))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("30 characters");
    }

    @Test
    void aBlankTagNameIsRejected() {
        assertThatThrownBy(() -> fileTagService.createTag(USER_ID, "   "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void creatingAnExistingTagConflicts() {
        when(fileTagRepository.findByUserIdAndNameIgnoreCase(USER_ID, "photos"))
                .thenReturn(Optional.of(new FileTag(1L, USER_ID, "photos")));

        assertThatThrownBy(() -> fileTagService.createTag(USER_ID, "photos"))
                .isInstanceOf(ConflictException.class);
    }
}
