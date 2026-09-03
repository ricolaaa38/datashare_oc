package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.entity.FileTag;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.PayloadTooLargeException;
import com.openclassroom.datashare.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the input controls of US01 (size, forbidden extensions, password
 * length, expiration window) and the side effects of an upload.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceTest {

    private static final Long OWNER_ID = 42L;

    @Mock
    private FileRepository fileRepository;
    @Mock
    private DownloadTokenService downloadTokenService;
    @Mock
    private FileTagService fileTagService;
    @Mock
    private FileStorageService storageService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        // deliberately spaced out and partly dot-less to prove the configuration is
        // normalized before use
        fileService = new FileService(fileRepository, downloadTokenService, fileTagService,
                storageService, passwordEncoder, ".exe, .bat , sh");

        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setFileId(1L);
            return saved;
        });
        when(downloadTokenService.issueFor(any(File.class))).thenReturn(
                new DownloadTokenService.IssuedToken("raw-token",
                        URI.create("http://localhost:8080/downloads/raw-token"), OffsetDateTime.now()));
        when(fileTagService.resolveOrCreate(any(), any())).thenReturn(Set.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    @Test
    void expirationDefaultsToSevenDaysWhenNotProvided() {
        FileService.UploadResult result = fileService.upload(command(document(), null, null, List.of()));

        assertThat(result.file().getExpiresAt())
                .isBetween(OffsetDateTime.now().plusDays(7).minusMinutes(1),
                        OffsetDateTime.now().plusDays(7).plusMinutes(1));
    }

    @Test
    void expirationBeyondSevenDaysIsRejectedBeforeAnythingIsStored() {
        assertThatThrownBy(() -> fileService.upload(command(document(), 8, null, List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 1 and 7");

        verify(storageService, never()).store(any(), anyString(), anyString());
    }

    @Test
    void emptyFileIsRejected() {
        MultipartFile empty = new MockMultipartFile("file", "note.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(command(empty, 3, null, List.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void fileLargerThanOneGigabyteIsRejected() {
        MultipartFile huge = mock(MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getSize()).thenReturn(FileService.MAX_SIZE_BYTES + 1);
        when(huge.getOriginalFilename()).thenReturn("huge.bin");

        assertThatThrownBy(() -> fileService.upload(command(huge, 3, null, List.of())))
                .isInstanceOf(PayloadTooLargeException.class);
    }

    @Test
    void forbiddenExtensionIsRejectedEvenWhenTheConfigurationContainsSpaces() {
        MultipartFile script = new MockMultipartFile("file", "install.sh", "text/plain", "echo hi".getBytes());

        assertThatThrownBy(() -> fileService.upload(command(script, 3, null, List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void passwordShorterThanSixCharactersIsRejected() {
        assertThatThrownBy(() -> fileService.upload(command(document(), 3, "12345", List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("6 characters");
    }

    @Test
    void onlyThePasswordHashIsStored() {
        FileService.UploadResult result = fileService.upload(command(document(), 3, "s3cret!", List.of()));

        assertThat(result.file().getPasswordHash()).isEqualTo("hashed");
        verify(passwordEncoder).encode("s3cret!");
    }

    @Test
    void originalNameFallsBackToTheMultipartFilenameAndDropsAnyPath() {
        MultipartFile withPath = new MockMultipartFile("file", "C:\\Users\\bob\\report.pdf",
                "application/pdf", "content".getBytes());

        FileService.UploadResult result = fileService.upload(
                new FileService.UploadCommand(withPath, null, null, 3, null, List.of(), OWNER_ID));

        assertThat(result.file().getOriginalName()).isEqualTo("report.pdf");
    }

    @Test
    void tagsAreResolvedAndAttachedToTheFile() {
        FileTag holidays = new FileTag(OWNER_ID, "vacances");
        when(fileTagService.resolveOrCreate(OWNER_ID, List.of("vacances"))).thenReturn(Set.of(holidays));

        FileService.UploadResult result = fileService.upload(command(document(), 3, null, List.of("vacances")));

        assertThat(result.file().getTags()).containsExactly(holidays);
    }

    @Test
    void storedObjectIsRemovedWhenPersistenceFails() {
        when(fileRepository.save(any(File.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> fileService.upload(command(document(), 3, null, List.of())))
                .isInstanceOf(IllegalStateException.class);

        verify(storageService).delete(anyString());
    }

    @Test
    void uploadLinksTheFileToItsOwnerAndReturnsADownloadLink() {
        FileService.UploadResult result = fileService.upload(command(document(), 3, null, List.of()));

        assertThat(result.file().getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.downloadToken().token()).isEqualTo("raw-token");
        assertThat(result.downloadToken().downloadUrl()).hasToString("http://localhost:8080/downloads/raw-token");
    }

    private MultipartFile document() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());
    }

    private FileService.UploadCommand command(MultipartFile file, Integer expiresInDays, String password,
            List<String> tags) {
        return new FileService.UploadCommand(file, file.getOriginalFilename(), null, expiresInDays, password, tags,
                OWNER_ID);
    }
}
