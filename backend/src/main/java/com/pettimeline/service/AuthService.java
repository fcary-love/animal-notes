package com.pettimeline.service;

import com.pettimeline.model.dto.LoginDTO;
import com.pettimeline.model.dto.RegisterDTO;
import com.pettimeline.model.vo.UserVO;

public interface AuthService {
    UserVO register(RegisterDTO dto);
    UserVO login(LoginDTO dto);
    UserVO me(Long userId);
}
