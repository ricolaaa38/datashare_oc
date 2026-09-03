package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileCleanupService fileCleanupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileCleanupService, "batchSize", 200);
    }

    @Test
    void everyExpiredFileIsPurged() {
        File first = expiredFile(1L);
        File second = expiredFile(2L);
        when(fileService.findExpired(any(OffsetDateTime.class), anyInt())).thenReturn(List.of(first, second));

        fileCleanupService.purgeExpiredFiles();

        verify(fileService).purge(first);
        verify(fileService).purge(second);
    }

    @Test
    void aFailingFileDoesNotPreventTheOthersFromBeingPurged() {
        File failing = expiredFile(1L);
        File healthy = expiredFile(2L);
        when(fileService.findExpired(any(OffsetDateTime.class), anyInt())).thenReturn(List.of(failing, healthy));
        doThrow(new IllegalStateException("storage unavailable")).when(fileService).purge(failing);

        fileCleanupService.purgeExpiredFiles();

        verify(fileService).purge(healthy);
    }

    @Test
    void nothingIsPurgedWhenNoFileHasExpired() {
        when(fileService.findExpired(any(OffsetDateTime.class), anyInt())).thenReturn(List.of());

        fileCleanupService.purgeExpiredFiles();

        verify(fileService, never()).purge(any());
    }

    private File expiredFile(Long id) {
        File file = new File();
        file.setFileId(id);
        file.setStorageKey("files/" + id);
        file.setExpiresAt(OffsetDateTime.now().minusDays(1));
        return file;
    }
}
