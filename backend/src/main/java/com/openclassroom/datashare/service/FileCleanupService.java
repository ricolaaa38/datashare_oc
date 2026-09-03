package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Enforces the "the file and its metadata are automatically deleted at
 * expiration" rule of US01.
 * <p>
 * Each file is purged in its own transaction so that a single storage failure
 * does not prevent the other expired files from being removed; the failing one
 * is simply retried on the next run.
 */
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final FileService fileService;

    @Value("${app.cleanup.batch-size:200}")
    private int batchSize;

    @Scheduled(cron = "${app.cleanup.cron:0 */15 * * * *}")
    public void purgeExpiredFiles() {
        List<File> expired = fileService.findExpired(OffsetDateTime.now(), batchSize);
        if (expired.isEmpty()) {
            return;
        }

        int purged = 0;
        for (File file : expired) {
            try {
                fileService.purge(file);
                purged++;
            } catch (RuntimeException e) {
                log.error("Unable to purge expired file {} (storage key {}), will retry on the next run",
                        file.getFileId(), file.getStorageKey(), e);
            }
        }
        log.info("Purged {}/{} expired files", purged, expired.size());
    }
}
