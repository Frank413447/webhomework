package com.itheima.webhomework.service.Impl;

import com.itheima.webhomework.entity.Teacher;
import com.itheima.webhomework.mapper.TeacherMapper;
import com.itheima.webhomework.service.TeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.itheima.webhomework.constant.LoginConstant.FAIL_COUNT;
import static com.itheima.webhomework.constant.LoginConstant.LOCK_KEY;

@Service
@Slf4j
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 10;

    @Override
    public Teacher login(String tid, String password) {
        // 1. 检查账号是否被锁定
        String lockKey = LOCK_KEY + tid;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new RuntimeException("账号被锁定，请10分钟再试");
        }
        // 2. 检查密码
        log.info("教师 {} 尝试登录", tid);
        Teacher teacher = teacherMapper.selectById(tid);
        if (teacher == null) {
            recordFailCount(tid);
            throw new RuntimeException("用户不存在");
        }
        // 验证密码：用户输入的密码 vs 数据库中的密码
        if (!passwordEncoder.matches(password, teacher.getPassword())) {
            recordFailCount(tid);
            throw new RuntimeException("密码错误，剩余尝试次数：" + (MAX_FAIL_COUNT - getFailCount(tid)));
        }
        // 登录成功，清空失败次数
        stringRedisTemplate.delete(FAIL_COUNT + tid);
        return teacher;
    }

    @Override
    public Teacher getTeacherById(String tid) {
        return teacherMapper.selectById(tid);
    }

    // 记录失败次数
    private void recordFailCount(String tid) {
        String key = FAIL_COUNT + tid;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == 1) {
            stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
        if (count >= MAX_FAIL_COUNT) {
            stringRedisTemplate.opsForValue().set(LOCK_KEY + tid, "locked", LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    // 获取失败次数
    private int getFailCount(String tid) {
        String key = FAIL_COUNT + tid;
        String count = stringRedisTemplate.opsForValue().get(key);
        return count == null ? 0 : Integer.parseInt(count);
    }
}
