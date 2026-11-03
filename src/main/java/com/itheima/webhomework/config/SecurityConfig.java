package com.itheima.webhomework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级权限控制（支持 @PreAuthorize 等注解）
public class SecurityConfig {

    // 密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 安全过滤链
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        // 放行静态资源和首页
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/*.html").permitAll()
                        .requestMatchers("/*.css").permitAll()
                        .requestMatchers("/*.js").permitAll()
                        .requestMatchers("/*.png").permitAll()
                        .requestMatchers("/*.jpg").permitAll()
                        .requestMatchers("/*.jpeg").permitAll()
                        .requestMatchers("/*.gif").permitAll()
                        .requestMatchers("/*.ico").permitAll()
                        // 放行登录/登出接口（由 Controller 自行处理认证逻辑）
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/logout").permitAll()
                        // 业务接口：Spring Security 放行，由 LoginInterceptor 做 JWT 校验
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/ai/**").permitAll()
                        .requestMatchers("/student/**").permitAll()
                        .requestMatchers("/score/**").permitAll()
                        .requestMatchers("/dashboard").permitAll()
                        // 其他所有接口需要认证
                        .anyRequest().authenticated()
                )
                // 关闭 CSRF（前后端分离项目必须）
                .csrf(csrf -> csrf.disable())
                // 允许 iframe 嵌入（开发环境使用，生产环境需谨慎）
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
