package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.DownloadToken;
import com.openclassroom.datashare.entity.File;
import com.openclassroom.datashare.repository.DownloadTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadTokenServiceTest {

    @Mock
    private DownloadTokenRepository downloadTokenRepository;

    private DownloadTokenService downloadTokenService;

    @BeforeEach
    void setUp() {
        downloadTokenService = new DownloadTokenService(downloadTokenRepository);
        ReflectionTestUtils.setField(downloadTokenService, "publicUrl", "https://datashare.test");
    }

    @Test
    void issuingATokenPersistsOnlyItsHashAndReturnsTheDownloadUrl() {
        File file = file();
        when(downloadTokenRepository.findByFile_FileId(file.getFileId())).thenReturn(Optional.empty());
        when(downloadTokenRepository.save(any(DownloadToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DownloadTokenService.IssuedToken issuedToken = downloadTokenService.issueFor(file);

        ArgumentCaptor<DownloadToken> tokenCaptor = ArgumentCaptor.forClass(DownloadToken.class);
        verify(downloadTokenRepository).save(tokenCaptor.capture());
        assertThat(issuedToken.token()).hasSize(43);
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(issuedToken.token());
        assertThat(issuedToken.downloadUrl()).hasToString("https://datashare.test/downloads/" + issuedToken.token());
    }

    @Test
    void issuingATokenReplacesThePreviousTokenForTheFile() {
        File file = file();
        DownloadToken previousToken = new DownloadToken();
        when(downloadTokenRepository.findByFile_FileId(file.getFileId())).thenReturn(Optional.of(previousToken));
        when(downloadTokenRepository.save(any(DownloadToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        downloadTokenService.issueFor(file);

        verify(downloadTokenRepository).delete(previousToken);
        verify(downloadTokenRepository).flush();
    }

    @Test
    void resolvingAKnownTokenReturnsItsPersistedLink() {
        DownloadToken token = new DownloadToken();
        when(downloadTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        DownloadToken resolvedToken = downloadTokenService.resolve("recipient-token");

        assertThat(resolvedToken).isSameAs(token);
    }

    @Test
    void deletingAFileTokenFlushesTheRemoval() {
        DownloadToken token = new DownloadToken();
        when(downloadTokenRepository.findByFile_FileId(anyLong())).thenReturn(Optional.of(token));

        downloadTokenService.deleteFor(4L);

        verify(downloadTokenRepository).delete(token);
        verify(downloadTokenRepository).flush();
    }

    private File file() {
        File file = new File();
        file.setFileId(4L);
        return file;
    }
}