package com.toolshare.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_SUBDIRS = new HashSet<>(Arrays.asList(
            "avatars", "tools", "toolboxes"
    ));

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final int MAGIC_BYTES_LENGTH = 12;

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87A_MAGIC = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89A_MAGIC = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] WEBP_RIFF_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_FORMAT_MAGIC = new byte[]{0x57, 0x45, 0x42, 0x50};

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    public String storeFile(MultipartFile file, String subDir) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("不支持的文件类型，仅支持 JPG、PNG、GIF、WEBP 格式的图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("文件大小不能超过 5MB");
        }

        String detectedType = detectFileTypeByMagicBytes(file);
        if (detectedType == null || !ALLOWED_IMAGE_TYPES.contains(detectedType)) {
            throw new BadRequestException("文件内容与声明类型不符，仅支持 JPG、PNG、GIF、WEBP 格式的图片");
        }

        if (subDir != null && !subDir.isEmpty() && !ALLOWED_SUBDIRS.contains(subDir)) {
            throw new BadRequestException("非法的存储目录");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new BadRequestException("文件名包含非法字符: " + originalFileName);
        }

        String fileExtension = getFileExtension(originalFileName);
        String normalizedExt = normalizeExtension(fileExtension, detectedType);
        String fileName = UUID.randomUUID().toString() + normalizedExt;

        try {
            Path targetLocation = this.fileStorageLocation;
            if (subDir != null && !subDir.isEmpty()) {
                targetLocation = targetLocation.resolve(subDir);
                Files.createDirectories(targetLocation);
            }
            targetLocation = targetLocation.resolve(fileName).normalize();

            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("非法的文件存储路径");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            if (subDir != null && !subDir.isEmpty()) {
                return "/uploads/" + subDir + "/" + fileName;
            }
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new BadRequestException("文件名不能为空");
            }

            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("非法的文件访问路径");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("文件未找到或不可读: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件未找到: " + fileName, ex);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            String relativePath = fileUrl.replaceFirst("^/uploads/", "");
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();

            if (!filePath.startsWith(this.fileStorageLocation)) {
                return;
            }

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
        }
    }

    private String detectFileTypeByMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[MAGIC_BYTES_LENGTH];
            int bytesRead = is.read(header);
            if (bytesRead < 3) {
                return null;
            }

            if (startsWith(header, JPEG_MAGIC)) {
                return "image/jpeg";
            }
            if (startsWith(header, PNG_MAGIC)) {
                return "image/png";
            }
            if (startsWith(header, GIF87A_MAGIC) || startsWith(header, GIF89A_MAGIC)) {
                return "image/gif";
            }
            if (bytesRead >= 12 && startsWith(header, WEBP_RIFF_MAGIC)
                    && startsWith(header, 8, WEBP_FORMAT_MAGIC)) {
                return "image/webp";
            }

            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String normalizeExtension(String originalExtension, String detectedType) {
        switch (detectedType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            default:
                return originalExtension != null && !originalExtension.isEmpty() ? originalExtension : "";
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }
}
