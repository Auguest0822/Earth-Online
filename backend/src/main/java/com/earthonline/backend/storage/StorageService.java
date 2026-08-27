package com.earthonline.backend.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.net.URI;

@Service
public class StorageService {

    private static final Logger log =
            LoggerFactory.getLogger(StorageService.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicUrl;

    public StorageService(
            MinioClient minioClient,
            @Value("${minio.bucket}") String bucket,
            @Value("${minio.public-url:}") String publicUrl
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicUrl = publicUrl == null ? "" : publicUrl.strip().replaceAll("/+$", "");
    }

    public String uploadRecordImage(String username, MultipartFile file) {
        return uploadImage(username, file, "records");
    }

    public String uploadAvatarImage(String username, MultipartFile file) {
        return uploadImage(username, file, "avatars");
    }

    public String uploadChatImage(String username, MultipartFile file) {
        return uploadImage(username, file, "chat");
    }

    public String getChatImagePreviewUrl(String username, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        String expectedPrefix = "chat/" + username + "/";
        return objectKey.startsWith(expectedPrefix) ? getPreviewUrlOrNull(objectKey) : null;
    }

    private String uploadImage(String username, MultipartFile file, String folder) {
        validateImage(file);

        try {
            ensureBucketExists();

            String extension = getExtension(file.getOriginalFilename());
            String objectName = folder + "/"
                    + username + "/"
                    + UUID.randomUUID() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1L
                            )
                            .contentType(file.getContentType())
                            .build()
            );

            // 暂时返回 MinIO 内的对象名；后续由记录接口转换为可展示的图片地址。
            return objectName;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("MinIO image upload failed", exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "图片上传失败，请检查 MinIO 服务和连接配置"
            );
        }
    }
    public String getPreviewUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            String signedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            return publicUrl.isBlank() ? signedUrl : withPublicOrigin(signedUrl);
        } catch (Exception exception) {
            log.error("Failed to create MinIO preview URL", exception);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "图片访问地址生成失败"
            );
        }
    }

    private String withPublicOrigin(String signedUrl) {
        URI uri = URI.create(signedUrl);
        String query = uri.getRawQuery();
        return publicUrl + uri.getRawPath() + (query == null ? "" : "?" + query);
    }

    /**
     * Preview URLs are optional presentation data. A stopped MinIO instance must not make
     * profile, friends, records or chat APIs unavailable.
     */
    public String getPreviewUrlOrNull(String objectKey) {
        try {
            return getPreviewUrl(objectKey);
        } catch (ResponseStatusException exception) {
            log.warn("MinIO preview unavailable for object {}: {}", objectKey, exception.getReason());
            return null;
        }
    }

    public String getAvatarPreviewUrl(String username, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        if ("/default-avatar.png".equals(objectKey)) {
            return objectKey;
        }

        String expectedPrefix = "avatars/" + username + "/";
        return objectKey.startsWith(expectedPrefix) ? getPreviewUrlOrNull(objectKey) : null;
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucket)
                            .build()
            );
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "请选择一张图片"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "单张图片不能超过 5MB"
            );
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "只支持 JPG、PNG、WebP 图片"
            );
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "图片文件名不正确"
            );
        }

        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
