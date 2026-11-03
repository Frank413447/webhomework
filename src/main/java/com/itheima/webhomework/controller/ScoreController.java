// ScoreController.java
package com.itheima.webhomework.controller;

import com.itheima.webhomework.vo.StudentScoreVO;
import com.itheima.webhomework.entity.Result;
import com.itheima.webhomework.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    // 查看学生成绩 - 返回JSON数据
    @GetMapping("/list")
    public Result listScores() {
        List<StudentScoreVO> scores = scoreService.getAllStudentScores();
        return new Result(true, "获取成功", scores);
    }

    // 根据学号查询学生成绩
    @GetMapping("/{sid}")
    public Result getStudentScoreBySid(@PathVariable String sid) {
        try {
            StudentScoreVO scoreVO = scoreService.getStudentScoreBySid(sid);
            if (scoreVO != null) {
                return new Result(true, "查询成功", scoreVO);
            } else {
                return new Result(false, "成绩记录不存在");
            }
        } catch (Exception e) {
            return new Result(false, "查询失败: " + e.getMessage());
        }
    }

    // 添加/修改成绩 - 接收JSON数据
    @PostMapping("/edit")
    public Result editScore(@RequestBody StudentScoreVO scoreVO) {
        try {
            scoreService.saveOrUpdateScore(scoreVO);
            return new Result(true, "成绩保存成功", scoreVO);
        } catch (Exception e) {
            return new Result(false, "保存失败: " + e.getMessage());
        }
    }
}
