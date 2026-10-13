package com.wastina.trust.gateway.document;

public interface ObjectStorageProvider {
    String providerName();

    void putQuarantined(String bucket, String objectKey, byte[] content);

    byte[] read(String bucket, String objectKey, String objectVersion);

    void delete(String bucket, String objectKey, String objectVersion);

    String createUploadUrl(String bucket, String objectKey, int expirySeconds);

    String createDownloadUrl(String bucket, String objectKey, String objectVersion, int expirySeconds);
}
