package com.funeral.common;

import com.funeral.entity.AppUser;

/** 当前登录用户（请求线程上下文，由 AuthInterceptor 写入） */
public class CurrentUser {

    private static final ThreadLocal<AppUser> HOLDER = new ThreadLocal<>();

    public static void set(AppUser user) {
        HOLDER.set(user);
    }

    public static AppUser get() {
        return HOLDER.get();
    }

    public static Long id() {
        AppUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static String name() {
        AppUser u = HOLDER.get();
        return u == null ? "系统" : u.getDisplayName();
    }

    public static String role() {
        AppUser u = HOLDER.get();
        return u == null ? "SYSTEM" : u.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
