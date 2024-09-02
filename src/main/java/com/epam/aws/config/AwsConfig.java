package com.epam.aws.config;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {
    @Bean
    public S3Client s3Client() {
        // Использование роли EC2 инстанса через DefaultCredentialsProvider
        String region = EC2MetadataUtils.getEC2InstanceRegion();
        return S3Client.builder()
                .region(Region.of(region))  // Регион можно получить из переменной окружения
                .credentialsProvider(DefaultCredentialsProvider.create()) // Использование провайдера по умолчанию
                .build();
    }
}