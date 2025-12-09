package com.example.gdms.config;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 明文密码编码器 - 不进行任何加密，直接返回原始密码
 * 注意：仅用于开发/测试环境，生产环境应使用加密密码
 */
public class PlainTextPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        // 直接返回明文密码
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // 直接比较明文密码
        return rawPassword.toString().equals(encodedPassword);
    }
}

