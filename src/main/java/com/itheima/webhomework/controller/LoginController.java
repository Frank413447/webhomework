package com.itheima.webhomework.controller;

import com.itheima.webhomework.security.jwt.JwtUtil;
import com.itheima.webhomework.entity.Teacher;
import com.itheima.webhomework.entity.Result;
import com.itheima.webhomework.service.TeacherService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class LoginController {

    @Autowired
    private TeacherService teacherService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    public Result login(@Valid Teacher teacher) {
        log.info("用户 {} 发起登录请求", teacher.getTid());

        Teacher loginTeacher = teacherService.login(teacher.getTid(), teacher.getPassword());

        String token = jwtUtil.generateToken(loginTeacher.getTid());
        stringRedisTemplate.opsForValue().set("login:" + loginTeacher.getTid(), token, 24, TimeUnit.HOURS);

        log.info("用户 {} 登录成功", teacher.getTid());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("teacher", loginTeacher);
        responseData.put("token", token);

        return new Result(true, "登录成功", responseData);
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String tid = jwtUtil.getTidFromToken(token);
            stringRedisTemplate.delete("login:" + tid);
            log.info("用户 {} 退出登录", tid);
        }
        return new Result(true, "退出成功");
    }

    @GetMapping("/dashboard")
    public Result dashboard(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.validateToken(token)) {
                String tid = jwtUtil.getTidFromToken(token);
                String redisToken = stringRedisTemplate.opsForValue().get("login:" + tid);
                if (token.equals(redisToken)) {
                    Teacher teacher = teacherService.getTeacherById(tid);
                    return new Result(true, "已登录", teacher);
                }
            }
        }
        return new Result(false, "未登录");
    }
}
