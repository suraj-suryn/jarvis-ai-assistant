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
            Map<?, ?> result = cloudinary.uploader().upload(data,
                    ObjectUtils.asMap(
                            "public_id", path,
                            "resource_type", "raw",
                            "use_filename", false,
                            "unique_filename", false
                    ));
            return (String) result.get("public_id");
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public byte[] download(String publicId) {
        try {
            // Generate a signed URL — required for raw uploads on restricted Cloudinary accounts
            String signedUrl = cloudinary.privateDownload(publicId, null,
                    ObjectUtils.asMap("resource_type", "raw", "attachment", false));
            return new URL(signedUrl).openStream().readAllBytes();
        } catch (Exception e) {
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

