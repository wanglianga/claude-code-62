package com.funeral.config;

import com.funeral.common.ApiResponse;
import com.funeral.common.CurrentUser;
import com.funeral.entity.AppUser;
import com.funeral.repo.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** 简易 Token 认证：登录后返回 user:{id} 作为 token，前端放在 X-Auth-Token 头 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_PREFIX = "user:";

    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static boolean matches(String raw, String encoded) {
        return new BCryptPasswordEncoder().matches(raw, encoded);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            return deny(response, "未登录或登录已失效");
        }
        Long userId;
        try {
            userId = Long.parseLong(token.substring(TOKEN_PREFIX.length()));
        } catch (Exception e) {
            return deny(response, "无效的登录凭证");
        }
        Optional<AppUser> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActive())) {
            return deny(response, "账号不存在或已停用");
        }
        CurrentUser.set(userOpt.get());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUser.clear();
    }

    private boolean deny(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(401, msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
