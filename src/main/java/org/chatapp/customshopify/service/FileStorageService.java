package org.chatapp.customshopify.service;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation = Paths.get(System.getProperty("user.dir") + "/uploads").toAbsolutePath().normalize();

    // Folders must exist manually as per configuration

    public String storeFile(MultipartFile file, String type) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String contentType = file.getContentType();
        if(type != null){
            if(!Objects.equals(contentType, type))
                throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORT);
        }
        String subDir = "others";
        String urlPrefix = "/api/files/others/";

        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                subDir = "images";
                urlPrefix = "/api/files/images/";
            } else if (contentType.startsWith("video/")) {
                subDir = "videos";
                urlPrefix = "/api/files/videos/";
            }
        }

        try {
            // Check if subDir exists, log warning if not (since we removed auto-create logic)
            Path targetDir = this.fileStorageLocation.resolve(subDir);
            if (!Files.exists(targetDir)) {
                log.warn("Target directory does not exist: {}", targetDir);
                // Depending on strict requirement: error or try create. User said "remove constructor, init path inline" 
                // but implied "add enough folders". 
                // Logic: files usually fail if dir missing. 
            }

            // Generate unique filename
            String fileExtension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                fileExtension = fileName.substring(dotIndex);
            }
            
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = targetDir.resolve(newFileName);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return urlPrefix + newFileName;

        } catch (IOException e) {
            log.error("Could not store file " + fileName, e);
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public Resource loadFileAsResource(String type, String fileName) {
        try {
            // Validate type to prevent directory traversal
            if (!type.equals("images") && !type.equals("videos") && !type.equals("others")) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            Path filePath = this.fileStorageLocation.resolve(type).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new AppException(ErrorCode.INVALID_REQUEST); // File not found
            }
        } catch (MalformedURLException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
