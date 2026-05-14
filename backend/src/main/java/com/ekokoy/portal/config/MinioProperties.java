package com.ekokoy.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String publicBucket;
    private String privateBucket;
    private String adminBucket;
    private String profilesBucket;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getPublicBucket() { return publicBucket; }
    public void setPublicBucket(String publicBucket) { this.publicBucket = publicBucket; }

    public String getPrivateBucket() { return privateBucket; }
    public void setPrivateBucket(String privateBucket) { this.privateBucket = privateBucket; }

    public String getAdminBucket() { return adminBucket; }
    public void setAdminBucket(String adminBucket) { this.adminBucket = adminBucket; }

    public String getProfilesBucket() { return profilesBucket; }
    public void setProfilesBucket(String profilesBucket) { this.profilesBucket = profilesBucket; }
}
