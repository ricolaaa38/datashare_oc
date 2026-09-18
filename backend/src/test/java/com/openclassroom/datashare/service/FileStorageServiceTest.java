package com.openclassroom.datashare.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Test
    void storingAFileSendsItsContentAndMetadataToTheConfiguredBucket() {
        FileStorageService storageService = new FileStorageService(s3Client, "test-files");
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());

        storageService.store(file, "files/report.pdf", "application/pdf");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-files");
        assertThat(requestCaptor.getValue().key()).isEqualTo("files/report.pdf");
        assertThat(requestCaptor.getValue().contentLength()).isEqualTo(7L);
    }

    @Test
    void loadingAndDeletingAFileUseTheConfiguredBucket() {
        FileStorageService storageService = new FileStorageService(s3Client, "test-files");
        ResponseInputStream<GetObjectResponse> response = org.mockito.Mockito.mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        assertThat(storageService.load("files/report.pdf")).isSameAs(response);
        storageService.delete("files/report.pdf");

        ArgumentCaptor<GetObjectRequest> loadRequest = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(loadRequest.capture());
        assertThat(loadRequest.getValue().bucket()).isEqualTo("test-files");
        ArgumentCaptor<DeleteObjectRequest> deleteRequest = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteRequest.capture());
        assertThat(deleteRequest.getValue().key()).isEqualTo("files/report.pdf");
    }

    @Test
    void anExistingBucketIsAcceptedAtStartup() {
        FileStorageService storageService = new FileStorageService(s3Client, "test-files");

        storageService.ensureBucketExists();

        ArgumentCaptor<HeadBucketRequest> requestCaptor = ArgumentCaptor.forClass(HeadBucketRequest.class);
        verify(s3Client).headBucket(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-files");
    }
}