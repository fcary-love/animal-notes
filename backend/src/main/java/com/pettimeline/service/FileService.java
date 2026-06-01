package com.pettimeline.service;

public interface FileService {
    String uploadFile(byte[] bytes, String originalFilename, String subDir);
}
