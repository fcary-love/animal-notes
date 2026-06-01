package com.pettimeline.service;

import com.pettimeline.model.dto.ChangePasswordDTO;
import com.pettimeline.model.dto.UpdateProfileDTO;
import com.pettimeline.model.vo.UserVO;

public interface UserService {
    UserVO updateProfile(Long userId, UpdateProfileDTO dto);
    void changePassword(Long userId, ChangePasswordDTO dto);
    UserVO uploadAvatar(Long userId, byte[] fileBytes, String originalFilename);
}
