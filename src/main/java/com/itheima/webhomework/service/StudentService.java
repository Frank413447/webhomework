// StudentService.java
package com.itheima.webhomework.service;

import com.itheima.webhomework.entity.Student;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StudentService {
    // 获取所有学生
    List<Student> getAllStudents();

    // 添加学生
    void addStudent(Student student) throws Exception;

    // 删除学生
    void deleteStudent(String sid);

    // 根据学号查询学生
    Student getStudentBySid(String sid);

    // 清除缓存
    void clearCache();
}
