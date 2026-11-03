// ScoreMapper.java
package com.itheima.webhomework.mapper;

import com.itheima.webhomework.entity.Score;
import com.itheima.webhomework.vo.StudentScoreVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScoreMapper {

    // 查询所有学生的成绩信息（关联学生表）
    @Select("SELECT s.sid, s.sname, s.gender, s.`class` AS clazz, sc.chinese, sc.math, sc.english " +
            "FROM student_web_assignment s LEFT JOIN score_web_assignment sc ON s.sid = sc.sid")
    List<StudentScoreVO> selectAllStudentScores();

    // 根据学号查询学生的成绩信息
    @Select("SELECT s.sid, s.sname, s.gender, s.`class` AS clazz, sc.chinese, sc.math, sc.english " +
            "FROM student_web_assignment s LEFT JOIN score_web_assignment sc ON s.sid = sc.sid " +
            "WHERE s.sid = #{sid}")
    StudentScoreVO selectStudentScoreBySid(String sid);

    // 根据学号查询成绩
    @Select("SELECT * FROM score_web_assignment WHERE sid = #{sid}")
    Score selectScoreBySid(String sid);

    // 插入成绩
    @Insert("INSERT INTO score_web_assignment(sid, chinese, math, english) VALUES(#{sid}, #{chinese}, #{math}, #{english})")
    void insertScore(Score score);

    // 更新成绩
    @Update("UPDATE score_web_assignment SET chinese = #{chinese}, math = #{math}, english = #{english} WHERE sid = #{sid}")
    void updateScore(Score score);

    // 删除成绩
    @Delete("DELETE FROM score_web_assignment WHERE sid = #{sid}")
    void deleteScoreBySid(String sid);
}


