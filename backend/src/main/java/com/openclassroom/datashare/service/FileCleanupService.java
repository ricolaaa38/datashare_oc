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
 * Service responsible for cleaning up expired files from the system.
 * It periodically checks for files that have expired and attempts to purge them.
 */
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final FileService fileService;

    @Value("${app.cleanup.batch-size:200}")
    private int batchSize;

    /**
     * Scheduled method that purges expired files based on the configured cron expression.
     * The default cron expression runs the cleanup every minute.
     */
    @Scheduled(cron = "${app.cleanup.cron:0 */1 * * * *}")
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
