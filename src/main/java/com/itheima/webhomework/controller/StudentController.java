// StudentController.java
package com.itheima.webhomework.controller;

import com.itheima.webhomework.entity.Student;
import com.itheima.webhomework.entity.Result;
import com.itheima.webhomework.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    // 查看学生列表 - 返回JSON数据
    @GetMapping("/list")
    public Result listStudents() {
        List<Student> students = studentService.getAllStudents();
        return new Result(true, "获取成功", students);
    }

    // 根据学号查询学生
    @GetMapping("/{sid}")
    public Result getStudentBySid(@PathVariable String sid) {
        try {
            Student student = studentService.getStudentBySid(sid);
            if (student != null) {
                return new Result(true, "查询成功", student);
            } else {
                return new Result(false, "学生不存在");
            }
        } catch (Exception e) {
            return new Result(false, "查询失败: " + e.getMessage());
        }
    }

    // 添加学生 - 接收JSON数据
    @PostMapping("/add")
    public Result addStudent(@RequestBody Student student) {
        try {
            studentService.addStudent(student);
            return new Result(true, "学生添加成功", student);
        } catch (Exception e) {
            return new Result(false, "添加失败: " + e.getMessage());
        }
    }
    // 删除学生
    @DeleteMapping("/delete/{sid}")
    public Result deleteStudent(@PathVariable String sid) {
        try {
            studentService.deleteStudent(sid);
            return new Result(true, "学生删除成功");
        } catch (Exception e) {
            return new Result(false, "删除失败: " + e.getMessage());
        }
    }

    // 手动清除缓存（调试用）
    @PostMapping("/cache/clear")
    public Result clearCache() {
        try {
            studentService.clearCache();
            return new Result(true, "缓存已清除，下次查询将从数据库获取最新数据");
        } catch (Exception e) {
            return new Result(false, "清除缓存失败: " + e.getMessage());
        }
    }
}
