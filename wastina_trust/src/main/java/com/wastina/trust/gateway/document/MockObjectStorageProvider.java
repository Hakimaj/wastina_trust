package com.wastina.trust.gateway.document;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test | local | default")
public class MockObjectStorageProvider implements ObjectStorageProvider {
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public String providerName() {
        return "mock-quarantine-storage";
    }

    @Override
    public void putQuarantined(String bucket, String objectKey, byte[] content) {
        objects.put(bucket + "/" + objectKey, content.clone());
    }

    @Override
    public byte[] read(String bucket, String objectKey, String objectVersion) {
        byte[] content = objects.get(bucket + "/" + objectKey);
        if (content == null) {
            throw new IllegalArgumentException("Object not found");
        }
        return content.clone();
    }

    @Override
    public void delete(String bucket, String objectKey, String objectVersion) {
        objects.remove(bucket + "/" + objectKey);
    }

    @Override
    public String createUploadUrl(String bucket, String objectKey, int expirySeconds) {
        return "mock://upload/" + bucket + "/" + objectKey + "?expires=" + expirySeconds;
    }

    @Override
    public String createDownloadUrl(String bucket, String objectKey, String objectVersion, int expirySeconds) {
        return "mock://download/" + bucket + "/" + objectKey + "?version=" + objectVersion + "&expires=" + expirySeconds;
    }
}
