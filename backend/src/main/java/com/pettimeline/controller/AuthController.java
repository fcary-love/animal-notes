package com.pettimeline.controller;

import com.pettimeline.model.dto.LoginDTO;
import com.pettimeline.model.dto.RegisterDTO;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.UserVO;
import com.pettimeline.service.AuthService;
import com.pettimeline.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return ApiResponse.ok("注册成功", authService.register(dto));
    }

    @PostMapping("/login")
    public ApiResponse<UserVO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResponse.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        return ApiResponse.ok(authService.me(userId));
    }
}
