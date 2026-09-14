package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.common.BusinessException;
import com.funeral.config.AuthInterceptor;
import com.funeral.entity.AppUser;
import com.funeral.repo.AppUserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository userRepo;

    public AuthController(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("账号已停用");
        }
        if (!AuthInterceptor.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", AuthInterceptor.TOKEN_PREFIX + user.getId());
        data.put("user", userInfo(user));
        return ApiResponse.ok(data);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        AppUser u = com.funeral.common.CurrentUser.get();
        return ApiResponse.ok(userInfo(u));
    }

    static Map<String, Object> userInfo(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("displayName", u.getDisplayName());
        m.put("role", u.getRole());
        return m;
    }
}
