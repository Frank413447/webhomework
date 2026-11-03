package com.itheima.webhomework.service.Impl;

import com.itheima.webhomework.entity.Student;
import com.itheima.webhomework.mapper.StudentMapper;
import com.itheima.webhomework.service.StudentService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StudentServiceImpl implements StudentService {

    private static final String CACHE_KEY = "allStudents";
    private static final long CACHE_TTL_MINUTES = 30; // 缓存30分钟自动过期

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 应用启动时清除旧缓存，确保不会因为旧数据导致显示异常
     */
    @PostConstruct
    public void init() {
        clearCache();
        System.out.println("应用启动：已清除学生数据缓存");
    }

    @Override
    public List<Student> getAllStudents() {
        List<Student> students = null;
        try {
            @SuppressWarnings("unchecked")
            List<Student> cached = (List<Student>) redisTemplate.opsForValue().get(CACHE_KEY);
            students = cached;
            if (students != null) {
                System.out.println("从Redis缓存返回学生数据，共" + students.size() + "条");
                return students;
            }
            System.out.println("Redis缓存未命中，从MySQL查询学生数据");
            students = studentMapper.selectAllStudents();
            if (students != null) {
                redisTemplate.opsForValue().set(CACHE_KEY, students, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                System.out.println("数据已存入Redis，TTL=" + CACHE_TTL_MINUTES + "分钟");
            }
        } catch (Exception e) {
            System.out.println("Redis异常，降级直接查MySQL");
            e.printStackTrace();
            students = studentMapper.selectAllStudents();
        }
        return students;
    }

    @Override
    public void addStudent(Student student) throws Exception {
        Student existingStudent = studentMapper.selectStudentBySid(student.getSid());
        if (existingStudent != null) {
            throw new Exception("学号已存在");
        }
        studentMapper.insertStudent(student);
        clearCache(); // 新增学生后清除缓存
    }

    @Override
    public void deleteStudent(String sid) {
        studentMapper.deleteStudentBySid(sid);
        clearCache(); // 删除学生后清除缓存
        System.out.println("已删除学生：" + sid + "，并清理缓存");
    }

    /**
     * 清除学生列表的 Redis 缓存
     */
    public void clearCache() {
        try {
            redisTemplate.delete(CACHE_KEY);
            System.out.println("已清理 allStudents 缓存");
        } catch (Exception e) {
            System.out.println("清理缓存失败（Redis可能不可用）: " + e.getMessage());
        }
    }

    @Override
    public Student getStudentBySid(String sid) {
        return studentMapper.selectStudentBySid(sid);
    }
}
