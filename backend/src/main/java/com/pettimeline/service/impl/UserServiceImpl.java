package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.UserMapper;
import com.pettimeline.model.dto.ChangePasswordDTO;
import com.pettimeline.model.dto.UpdateProfileDTO;
import com.pettimeline.model.entity.User;
import com.pettimeline.model.vo.UserVO;
import com.pettimeline.service.FileService;
import com.pettimeline.service.UserService;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final FileService fileService;

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname().trim());
        }
        userMapper.updateById(user);
        return buildVO(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码不正确");
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(user);
    }

    @Override
    public UserVO uploadAvatar(Long userId, byte[] fileBytes, String originalFilename) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String url = fileService.uploadFile(fileBytes, originalFilename, "avatars");
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        return buildVO(user);
    }

    private UserVO buildVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
