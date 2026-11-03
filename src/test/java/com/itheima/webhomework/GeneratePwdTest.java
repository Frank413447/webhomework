package com.itheima.webhomework;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePwdTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPwd = encoder.encode("1234"); // 确保这里是 "1234"
        System.out.println(encodedPwd);
    }
}