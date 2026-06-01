package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.UserMapper;
import com.pettimeline.model.dto.LoginDTO;
import com.pettimeline.model.dto.RegisterDTO;
import com.pettimeline.model.entity.User;
import com.pettimeline.model.vo.UserVO;
import com.pettimeline.service.AuthService;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    public UserVO register(RegisterDTO dto) {
        User exist = userMapper.findByUsername(dto.getUsername());
        if (exist != null) {
            throw new BusinessException(409, "用户名已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        userMapper.insert(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return buildVO(user, token);
    }

    @Override
    public UserVO login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return buildVO(user, token);
    }

    @Override
    public UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return buildVO(user, null);
    }

    private UserVO buildVO(User user, String token) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(token)
                .build();
    }
}
