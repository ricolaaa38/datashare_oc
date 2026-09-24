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
 * Service for handling file storage operations in AWS S3.
 * Provides methods to store, load, and delete files in a specified S3 bucket.
 */
@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public FileStorageService(S3Client s3Client, @Value("${aws.s3.bucket:datashare-files}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * Ensures that the specified S3 bucket exists, creating it if necessary.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    /**
     * Stores a file in the S3 bucket with the specified key and content type.
     *
     * @param file        the file to store
     * @param key         the key under which to store the file
     * @param contentType the content type of the file
     */
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

    /**
     * Loads a file from the S3 bucket with the specified key.
     *
     * @param key the key of the file to load
     * @return a ResponseInputStream containing the file's content
     */
    public ResponseInputStream<GetObjectResponse> load(String key) {
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * Deletes a file from the S3 bucket with the specified key.
     *
     * @param key the key of the file to delete
     */
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
