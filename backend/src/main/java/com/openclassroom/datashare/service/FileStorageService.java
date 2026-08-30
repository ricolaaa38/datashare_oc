package com.openclassroom.datashare.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Thin wrapper around the S3 client used to store and retrieve file content.
 */
@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public FileStorageService(S3Client s3Client, @Value("${aws.s3.bucket:datashare-files}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @PostConstruct
    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    public void store(MultipartFile file, String key, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read uploaded file content", e);
        }
    }

    public ResponseInputStream<GetObjectResponse> load(String key) {
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
