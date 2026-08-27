package com.project.ecommerce_backend.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Component
public class S3BucketInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3BucketInitializer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void run(String... args) throws Exception{
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            logger.info("Bucket already exists: {}", bucketName);
        } catch (NoSuchBucketException e) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                logger.info("Bucket created: {}", bucketName);
            } catch (Exception ex) {
                logger.warn("Could not create bucket {}: {}", bucketName, ex.getMessage());
            }
        } catch (Exception e) {
            logger.warn("S3 bucket initialization skipped (MinIO may not be running): {}", e.getMessage());
        }
    }
}
