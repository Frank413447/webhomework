
package com.itheima.webhomework.service.Impl;

import com.itheima.webhomework.entity.Score;
import com.itheima.webhomework.vo.StudentScoreVO;
import com.itheima.webhomework.mapper.ScoreMapper;
import com.itheima.webhomework.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private ScoreMapper scoreMapper;

    @Override
    public List<StudentScoreVO> getAllStudentScores() {
        return scoreMapper.selectAllStudentScores();
    }

    @Override
    public StudentScoreVO getStudentScoreBySid(String sid) {
        return scoreMapper.selectStudentScoreBySid(sid);
    }

    @Override
    public void saveOrUpdateScore(StudentScoreVO scoreVO) {
        Score score = new Score();
        score.setSid(scoreVO.getSid());
        score.setChinese(scoreVO.getChinese());
        score.setMath(scoreVO.getMath());
        score.setEnglish(scoreVO.getEnglish());

        // 检查是否存在该学生的成绩记录
        Score existingScore = scoreMapper.selectScoreBySid(score.getSid());
        if (existingScore != null) {
            // 更新成绩
            scoreMapper.updateScore(score);
        } else {
            // 插入新成绩
            scoreMapper.insertScore(score);
        }
    }
}

