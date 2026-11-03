// ScoreService.java
package com.itheima.webhomework.service;

import com.itheima.webhomework.vo.StudentScoreVO;
import java.util.List;

public interface ScoreService {
    /**
     * 获取所有学生的成绩信息
     * @return 学生成绩列表
     */
    List<StudentScoreVO> getAllStudentScores();

    /**
     * 根据学号获取学生成绩信息
     * @param sid 学号
     * @return 学生成绩信息
     */
    StudentScoreVO getStudentScoreBySid(String sid);

    /**
     * 保存或更新学生成绩
     * @param scoreVO 学生成绩信息
     */
    void saveOrUpdateScore(StudentScoreVO scoreVO);
}

