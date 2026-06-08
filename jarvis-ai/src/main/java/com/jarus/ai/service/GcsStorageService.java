package com.jarus.ai.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GcsStorageService {

    @Autowired
    private Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket:jarus-files}")
    private String bucketName;

    public String upload(byte[] data, String path, String contentType) {
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        storage.create(blobInfo, data);
        return path;
    }

    public byte[] download(String path) {
        return storage.readAllBytes(bucketName, path);
    }

    public void delete(String path) {
        storage.delete(BlobId.of(bucketName, path));
    }

    public void deleteFolder(String prefix) {
        storage.list(bucketName, Storage.BlobListOption.prefix(prefix))
                .iterateAll()
                .forEach(blob -> blob.delete());
    }
}
