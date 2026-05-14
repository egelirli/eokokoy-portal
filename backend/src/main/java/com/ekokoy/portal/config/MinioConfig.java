package com.ekokoy.portal.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private final MinioProperties properties;

    public MinioConfig(MinioProperties properties) {
        this.properties = properties;
    }

    /** MinIO istemcisi — bağlantı kurulumu yapılmaz, sadece URL/kimlik bilgisi ayarlanır. */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
