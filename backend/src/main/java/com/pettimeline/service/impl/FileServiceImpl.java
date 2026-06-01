package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public String uploadFile(byte[] bytes, String originalFilename, String subDir) {
        if (bytes.length > MAX_SIZE) {
            throw new BusinessException(413, "图片大小不能超过10MB");
        }

        String ext = getExtension(originalFilename);
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException(415, "仅支持JPG/PNG/WebP/GIF格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadPath, subDir);
        try {
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.write(filePath, bytes);
            return "/files/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败");
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1);
    }
}
