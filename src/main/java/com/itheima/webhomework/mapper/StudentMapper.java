package com.itheima.webhomework.mapper;
import com.itheima.webhomework.entity.Student;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentMapper {

    // 查询所有学生（class AS clazz 解决数据库列名与Java字段名不一致的问题）
    @Select("SELECT sid, sname, gender, `class` AS clazz FROM student_web_assignment")
    List<Student> selectAllStudents();

    // 根据学号查询学生
    @Select("SELECT sid, sname, gender, `class` AS clazz FROM student_web_assignment WHERE sid = #{sid}")
    Student selectStudentBySid(String sid);

    // 根据姓名查询学生（支持模糊匹配，可能返回多个结果）
    @Select("SELECT sid, sname, gender, `class` AS clazz FROM student_web_assignment WHERE sname = #{name}")
    List<Student> selectStudentByName(String name);

    // 插入学生（`class` 用反引号因为它是MySQL保留字）
    @Insert("INSERT INTO student_web_assignment(sid, sname, gender, `class`) VALUES(#{sid}, #{sname}, #{gender}, #{clazz})")
    void insertStudent(Student student);


    // 删除学生
    @Delete("DELETE FROM student_web_assignment WHERE sid = #{sid}")
    void deleteStudentBySid(String sid);
}

