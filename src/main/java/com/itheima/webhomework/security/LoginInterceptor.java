package com.itheima.webhomework.security;

import com.itheima.webhomework.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.itheima.webhomework.constant.LoginConstant.LOGIN_URL;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.getWriter().write("Token无效");
            return false;
        }
        String tid = jwtUtil.getTidFromToken(token);
        String redisToken = stringRedisTemplate.opsForValue().get(LOGIN_URL + tid);
        if (!token.equals(redisToken)) {
            response.setStatus(401);
            response.getWriter().write("Token已过期");
            return false;
        }
        return true;
    }
}
