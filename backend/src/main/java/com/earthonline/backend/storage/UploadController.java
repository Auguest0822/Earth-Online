package com.earthonline.backend.storage;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, String> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        String objectKey = storageService.uploadRecordImage(
                authentication.getName(),
                file
        );

        return Map.of("objectKey", objectKey);
    }

    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, String> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        String username = authentication.getName();
        String objectKey = storageService.uploadAvatarImage(username, file);

        return Map.of(
                "objectKey", objectKey,
                "previewUrl", storageService.getAvatarPreviewUrl(username, objectKey)
        );
    }
}
