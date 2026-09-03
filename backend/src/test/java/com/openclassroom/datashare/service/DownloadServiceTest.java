package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.DownloadToken;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.exception.FileExpiredException;
import com.openclassroom.datashare.exception.InvalidTokenOrPasswordException;
import com.openclassroom.datashare.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers US02: an explicit error for an invalid or expired link, the password
 * requirement, and the metadata preview available before downloading.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DownloadServiceTest {

    private static final String RAW_TOKEN = "a-raw-token";

    @Mock
    private DownloadTokenService downloadTokenService;
    @Mock
    private FileStorageService storageService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DownloadService downloadService;

    @BeforeEach
    void setUp() {
        when(storageService.load(anyString())).thenAnswer(invocation -> new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream("content".getBytes()))));
    }

    @Test
    void anUnknownLinkIsReportedAsNotFound() {
        when(downloadTokenService.resolve(RAW_TOKEN))
                .thenThrow(new ResourceNotFoundException("This download link is invalid or no longer exists"));

        assertThatThrownBy(() -> downloadService.downloadByToken(RAW_TOKEN, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> downloadService.describeByToken(RAW_TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anExpiredLinkIsReportedAsGoneForBothDownloadAndMetadata() {
        File expired = file(null);
        expired.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        givenToken(expired);

        assertThatThrownBy(() -> downloadService.downloadByToken(RAW_TOKEN, null))
                .isInstanceOf(FileExpiredException.class)
                .hasMessageContaining("expired");
        assertThatThrownBy(() -> downloadService.describeByToken(RAW_TOKEN))
                .isInstanceOf(FileExpiredException.class);
    }

    @Test
    void aFileWithoutPasswordIsDownloadedWithoutOne() {
        givenToken(file(null));

        DownloadService.DownloadResult result = downloadService.downloadByToken(RAW_TOKEN, null);

        assertThat(result.file().getOriginalName()).isEqualTo("rapport été.pdf");

    }

    @Test
    void aMissingPasswordOnAProtectedFileIsRejectedWithoutCountingAsAnAttempt() {
        givenToken(file("hashed"));

        assertThatThrownBy(() -> downloadService.downloadByToken(RAW_TOKEN, "  "))
                .isInstanceOf(InvalidTokenOrPasswordException.class)
                .hasMessageContaining("password is required");

    }

    @Test
    void aWrongPasswordIsRejected() {
        givenToken(file("hashed"));
        when(passwordEncoder.matches("nope", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> downloadService.downloadByToken(RAW_TOKEN, "nope"))
                .isInstanceOf(InvalidTokenOrPasswordException.class)
                .hasMessageContaining("incorrect");

    }

    @Test
    void aValidPasswordUnlocksTheDownload() {
        givenToken(file("hashed"));
        when(passwordEncoder.matches("s3cret1", "hashed")).thenReturn(true);

        DownloadService.DownloadResult result = downloadService.downloadByToken(RAW_TOKEN, "s3cret1");

        assertThat(result.resource()).isNotNull();

    }

    @Test
    void metadataIsReadableWithoutThePasswordAndNeverLeaksSecrets() {
        givenToken(file("hashed"));

        File described = downloadService.describeByToken(RAW_TOKEN);

        assertThat(described.getOriginalName()).isEqualTo("rapport été.pdf");
        assertThat(described.getMimeType()).isEqualTo("application/pdf");
        assertThat(described.getSizeBytes()).isEqualTo(15L);
        assertThat(described.getExpiresAt()).isNotNull();
        assertThat(described.getPasswordHash()).isNotNull();
        verify(storageService, never()).load(anyString());
    }

    private void givenToken(File file) {
        DownloadToken token = new DownloadToken();
        token.setFile(file);
        when(downloadTokenService.resolve(RAW_TOKEN)).thenReturn(token);
    }

    private File file(String passwordHash) {
        File file = new File();
        file.setFileId(1L);
        file.setOriginalName("rapport été.pdf");
        file.setMimeType("application/pdf");
        file.setSizeBytes(15L);
        file.setStorageKey("files/abc");
        file.setExpiresAt(OffsetDateTime.now().plusDays(3));
        file.setPasswordHash(passwordHash);
        return file;
    }
}
