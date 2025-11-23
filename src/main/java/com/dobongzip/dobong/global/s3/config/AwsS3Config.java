package com.dobongzip.dobong.global.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Value("${aws.region}") private String regionId;
    @Value("${aws.credentials.access-key}") private String accessKey;
    @Value("${aws.credentials.secret-key}") private String secretKey;

    @Bean
    public Region awsRegion() {
        return Region.of(regionId);
    }

    @Bean
    public StaticCredentialsProvider awsCredentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    @Bean
    public S3Client s3Client(Region region, StaticCredentialsProvider creds) {
        return S3Client.builder()
                .region(region)
                .credentialsProvider(creds)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(Region region, StaticCredentialsProvider creds) {
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(creds)
                .build();
    }
}
