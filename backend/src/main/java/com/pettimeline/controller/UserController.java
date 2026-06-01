package com.pettimeline.controller;

import com.pettimeline.model.dto.ChangePasswordDTO;
import com.pettimeline.model.dto.UpdateProfileDTO;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.UserVO;
import com.pettimeline.service.UserService;
import com.pettimeline.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@RequestHeader("Authorization") String auth,
                                             @Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok("资料更新成功", userService.updateProfile(userId, dto));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestHeader("Authorization") String auth,
                                            @Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = getUserId(auth);
        userService.changePassword(userId, dto);
        return ApiResponse.ok("密码修改成功", null);
    }

    @PostMapping("/avatar")
    public ApiResponse<UserVO> uploadAvatar(@RequestHeader("Authorization") String auth,
                                            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = getUserId(auth);
        return ApiResponse.ok("头像更新成功", userService.uploadAvatar(userId, file.getBytes(), file.getOriginalFilename()));
    }

    private Long getUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
