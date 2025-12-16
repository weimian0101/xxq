package com.example.gdms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {

    @Value("${encryption.password.strength:12}")
    private int passwordStrength;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 创建BCrypt编码器
        BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder(passwordStrength);

        // 创建DelegatingPasswordEncoder，设置bcrypt为默认编码器
        Map<String, PasswordEncoder> encoders = Map.of(
            "bcrypt", bcryptEncoder,
            "noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance()
        );

        return new DelegatingPasswordEncoder("bcrypt", encoders);
    }
}

