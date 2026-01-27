package org.chatapp.customshopify.controller;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.io.IOException;
import org.chatapp.customshopify.service.FileStorageService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 30MB
    private static final int MAX_FILE_COUNT = 5;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg"
    );

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<String>>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        if (files.length > MAX_FILE_COUNT) {
            throw new AppException(ErrorCode.FILE_LIMIT_EXCEEDED);
        }

        long totalSize = 0;
        for (MultipartFile file : files) {
            totalSize += file.getSize();
            if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORT);
            }
        }

        if (totalSize > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            
            String url = fileStorageService.storeFile(file,null);
            fileUrls.add(url);
        }
        
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .data(fileUrls)
                .build());
    }

    @GetMapping("/{type}/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String type, @PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(type, fileName);
        
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ex) {
            log.warn("Could not determine file type.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
