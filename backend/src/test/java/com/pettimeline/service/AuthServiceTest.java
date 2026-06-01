package com.pettimeline.service;

import com.pettimeline.mapper.UserMapper;
import com.pettimeline.model.dto.LoginDTO;
import com.pettimeline.model.dto.RegisterDTO;
import com.pettimeline.model.entity.User;
import com.pettimeline.model.vo.UserVO;
import com.pettimeline.service.impl.AuthServiceImpl;
import com.pettimeline.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtils jwtUtils;
    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUser() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setNickname("Test");

        when(userMapper.findByUsername("testuser")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(jwtUtils.generateToken(any(), any())).thenReturn("fake-token");

        UserVO vo = authService.register(dto);

        assertEquals("testuser", vo.getUsername());
        assertEquals("fake-token", vo.getToken());
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("exist");
        dto.setPassword("123456");

        User exist = new User();
        exist.setUsername("exist");
        when(userMapper.findByUsername("exist")).thenReturn(exist);

        assertThrows(RuntimeException.class, () -> authService.register(dto));
    }

    @Test
    void login_shouldReturnTokenOnSuccess() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        user.setNickname("Test");

        when(userMapper.findByUsername("testuser")).thenReturn(user);
        when(jwtUtils.generateToken(1L, "testuser")).thenReturn("fake-token");

        UserVO vo = authService.login(dto);

        assertEquals("testuser", vo.getUsername());
        assertEquals("fake-token", vo.getToken());
    }

    @Test
    void login_shouldFailOnWrongPassword() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrong");

        User user = new User();
        user.setId(1L);
        user.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));

        when(userMapper.findByUsername("testuser")).thenReturn(user);

        assertThrows(RuntimeException.class, () -> authService.login(dto));
    }
}
