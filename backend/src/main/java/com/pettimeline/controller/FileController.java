package com.pettimeline.controller;

import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.PetVO;
import com.pettimeline.service.FileService;
import com.pettimeline.service.PetService;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final PetService petService;
    private final JwtUtils jwtUtils;

    @PostMapping("/files/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileService.uploadFile(file.getBytes(), file.getOriginalFilename(), "uploads");
        return ApiResponse.ok(Map.of("url", url));
    }

    @PostMapping("/pets/{id}/avatar")
    public ApiResponse<PetVO> uploadAvatar(@RequestHeader("Authorization") String auth,
                                           @PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = getUserId(auth);
        String url = fileService.uploadFile(file.getBytes(), file.getOriginalFilename(), "avatars");
        return ApiResponse.ok(petService.updateAvatar(userId, id, url));
    }

    private Long getUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
