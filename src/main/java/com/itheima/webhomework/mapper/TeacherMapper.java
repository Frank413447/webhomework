// TeacherMapper.java
package com.itheima.webhomework.mapper;

import com.itheima.webhomework.entity.Teacher;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TeacherMapper {

    // 根据教师ID查询教师
    @Select("SELECT * FROM teacher_web_assignment WHERE tid = #{tid}")
    Teacher selectById(String tid);
}

