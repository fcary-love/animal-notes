package com.pettimeline.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    private static final String[] WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // 白名单放行
        for (String wl : WHITELIST) {
            if (path.equals(wl)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 静态资源放行
        if (path.startsWith("/files/") || path.startsWith("/static/")) {
            chain.doFilter(request, response);
            return;
        }

        // OPTIONS 放行
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            write401(res, "未登录或登录已过期");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            write401(res, "未登录或登录已过期");
            return;
        }

        chain.doFilter(request, response);
    }

    private void write401(HttpServletResponse res, String message) throws IOException {
        res.setStatus(401);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<?> body = ApiResponse.fail(401, message);
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
