package com.nutzycraft.pilotai.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Upload a file to Cloudinary
     * @param file The multipart file to upload
     * @return The secure URL of the uploaded image
     * @throws IOException if upload fails
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file) throws IOException {
        Map<String, Object> uploadParams = (Map<String, Object>) ObjectUtils.asMap(
                "folder", "user-avatars",
                "public_id", "avatar_" + UUID.randomUUID().toString(),
                "overwrite", true,
                "resource_type", "image",
                "width", 400,
                "height", 400,
                "crop", "fill",
                "gravity", "face",
                "quality", "auto",
                "fetch_format", "auto"
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Upload a base64 image string to Cloudinary
     * @param base64Image The base64 encoded image (with or without data URL prefix)
     * @return The secure URL of the uploaded image
     * @throws IOException if upload fails
     */
    @SuppressWarnings("unchecked")
    public String uploadBase64Image(String base64Image) throws IOException {
        // Remove data URL prefix if present (e.g., "data:image/png;base64,")
        String base64Data = base64Image;
        if (base64Image.contains(",")) {
            base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
        }

        // Decode base64 to byte array
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        Map<String, Object> uploadParams = (Map<String, Object>) ObjectUtils.asMap(
                "folder", "user-avatars",
                "public_id", "avatar_" + UUID.randomUUID().toString(),
                "overwrite", true,
                "resource_type", "image",
                "width", 400,
                "height", 400,
                "crop", "fill",
                "gravity", "face",
                "quality", "auto",
                "fetch_format", "auto"
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(imageBytes, uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Delete an image from Cloudinary by URL
     * @param imageUrl The URL of the image to delete
     * @return true if deletion was successful
     */
    public boolean deleteImage(String imageUrl) {
        try {
            // Extract public_id from URL
            // Cloudinary URLs format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // Format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{folder}/{public_id}.{format}
            // or: https://res.cloudinary.com/{cloud_name}/image/upload/{folder}/{public_id}.{format}
            if (url.contains("/image/upload/")) {
                String[] parts = url.split("/image/upload/")[1].split("/");
                // Get the last part (public_id + extension) and remove extension
                String lastPart = parts[parts.length - 1];
                return lastPart.substring(0, lastPart.lastIndexOf("."));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
