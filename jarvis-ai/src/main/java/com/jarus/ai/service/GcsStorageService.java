package com.jarus.ai.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Service
public class GcsStorageService {

    @Autowired
    private Cloudinary cloudinary;

    public String upload(byte[] data, String path, String contentType) {
        try {
            String publicId = path.replace("/", "_").replace(".", "_");
            Map<?, ?> result = cloudinary.uploader().upload(data,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "raw"
                    ));
            return (String) result.get("public_id");
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public byte[] download(String publicId) {
        try {
            String url = cloudinary.url().resourceType("raw").generate(publicId);
            return new URL(url).openStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("File download failed", e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "raw"));
        } catch (IOException e) {
            throw new RuntimeException("File delete failed", e);
        }
    }

    public void deleteFolder(String prefix) {
        // Cloudinary free tier doesn't support prefix deletion; no-op
    }
}

