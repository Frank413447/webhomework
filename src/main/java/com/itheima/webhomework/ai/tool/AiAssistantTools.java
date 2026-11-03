package com.itheima.webhomework.ai.tool;

import com.itheima.webhomework.entity.Student;
import com.itheima.webhomework.vo.StudentScoreVO;
import com.itheima.webhomework.service.ScoreService;
import com.itheima.webhomework.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAssistantTools {

    private final StudentService studentService;
    private final ScoreService scoreService;

    @Tool(description = "根据学生姓名查询学生的基本信息，包括学号、性别和班级")
    public String queryStudentByName(@ToolParam(description = "学生姓名，支持模糊匹配") String studentName) {
        List<Student> allStudents = studentService.getAllStudents();

        List<Student> matchedStudents = allStudents.stream()
                .filter(s -> s.getSname() != null && s.getSname().contains(studentName))
                .toList();

        if (matchedStudents.isEmpty()) {
            return "未找到姓名为 \"" + studentName + "\" 的学生";
        }

        StringBuilder result = new StringBuilder();
        for (Student student : matchedStudents) {
            result.append(String.format(
                    "学号: %s, 姓名: %s, 性别: %s, 班级: %s\n",
                    student.getSid(),
                    student.getSname(),
                    student.getGender(),
                    student.getClazz()
            ));
        }

        return result.toString().trim();
    }

    @Tool(description = "根据学号查询学生的考试成绩，包括语文、数学、英语成绩和平均分")
    public String queryStudentScoreById(@ToolParam(description = "学生学号") String studentId) {
        StudentScoreVO scoreVO = scoreService.getStudentScoreBySid(studentId);

        if (scoreVO == null) {
            return "未找到学号为 \"" + studentId + "\" 的成绩记录";
        }

        Double chinese = scoreVO.getChinese();
        Double math = scoreVO.getMath();
        Double english = scoreVO.getEnglish();
        Double average = scoreVO.getAverage();

        return String.format(
                "学生: %s (学号: %s)\n" +
                        "语文: %.1f 分\n" +
                        "数学: %.1f 分\n" +
                        "英语: %.1f 分\n" +
                        "平均分: %.1f 分",
                scoreVO.getSname(),
                studentId,
                chinese != null ? chinese : 0,
                math != null ? math : 0,
                english != null ? english : 0,
                average != null ? average : 0
        );
    }

    @Tool(description = "查询全班学生的成绩排名，按平均分从高到低排序")
    public String queryAllStudentScoresRanking() {
        List<StudentScoreVO> allScores = scoreService.getAllStudentScores();

        if (allScores.isEmpty()) {
            return "暂无学生成绩数据";
        }

        List<StudentScoreVO> sortedScores = allScores.stream()
                .filter(vo -> vo.getAverage() != null)
                .sorted((a, b) -> Double.compare(b.getAverage(), a.getAverage()))
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder("📊 全班成绩排名:\n\n");
        int rank = 1;

        for (StudentScoreVO vo : sortedScores) {
            result.append(String.format(
                    "第%d名: %s (学号: %s)\n" +
                            "  语文: %.1f, 数学: %.1f, 英语: %.1f, 平均: %.1f\n",
                    rank++,
                    vo.getSname(),
                    vo.getSid(),
                    vo.getChinese() != null ? vo.getChinese() : 0,
                    vo.getMath() != null ? vo.getMath() : 0,
                    vo.getEnglish() != null ? vo.getEnglish() : 0,
                    vo.getAverage()
            ));
        }

        return result.toString().trim();
    }

    @Tool(description = "根据班级名称查询该班级的所有学生信息")
    public String queryStudentsByClass(@ToolParam(description = "班级名称") String className) {
        List<Student> allStudents = studentService.getAllStudents();

        List<Student> classStudents = allStudents.stream()
                .filter(s -> s.getClazz() != null && s.getClazz().equals(className))
                .collect(Collectors.toList());

        if (classStudents.isEmpty()) {
            return "未找到班级为 \"" + className + "\" 的学生";
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("🏫 %s 班级学生列表 (%d人):\n\n", className, classStudents.size()));

        for (Student student : classStudents) {
            result.append(String.format(
                    "学号: %s, 姓名: %s, 性别: %s\n",
                    student.getSid(),
                    student.getSname(),
                    student.getGender()
            ));
        }

        return result.toString().trim();
    }

    @Tool(description = "查询考试成绩不及格（低于60分）的学生，可以指定科目或查询所有科目")
    public String queryFailedStudents(@ToolParam(description = "科目名称，如 chinese/math/english，不传则查询所有科目") String subject) {
        List<StudentScoreVO> allScores = scoreService.getAllStudentScores();

        if (allScores.isEmpty()) {
            return "暂无学生成绩数据";
        }

        StringBuilder result = new StringBuilder();

        if (subject == null || subject.isEmpty()) {
            result.append("⚠️ 不及格学生名单:\n\n");

            for (StudentScoreVO vo : allScores) {
                boolean hasFailed = false;
                StringBuilder failedSubjects = new StringBuilder();

                if (vo.getChinese() != null && vo.getChinese() < 60) {
                    hasFailed = true;
                    failedSubjects.append(String.format("语文(%.1f) ", vo.getChinese()));
                }
                if (vo.getMath() != null && vo.getMath() < 60) {
                    hasFailed = true;
                    failedSubjects.append(String.format("数学(%.1f) ", vo.getMath()));
                }
                if (vo.getEnglish() != null && vo.getEnglish() < 60) {
                    hasFailed = true;
                    failedSubjects.append(String.format("英语(%.1f) ", vo.getEnglish()));
                }

                if (hasFailed) {
                    result.append(String.format(
                            "%s (学号: %s): %s\n",
                            vo.getSname(),
                            vo.getSid(),
                            failedSubjects.toString().trim()
                    ));
                }
            }
        } else {
            result.append(String.format("⚠️ %s 不及格学生名单:\n\n", getSubjectName(subject)));

            for (StudentScoreVO vo : allScores) {
                Double subjectScore = getSubjectScore(vo, subject);
                if (subjectScore != null && subjectScore < 60) {
                    result.append(String.format(
                            "%s (学号: %s): %.1f 分\n",
                            vo.getSname(),
                            vo.getSid(),
                            subjectScore
                    ));
                }
            }
        }

        String resultStr = result.toString().trim();
        return resultStr.isEmpty() ? "没有不及格的学生，大家都很棒！👍" : resultStr;
    }

    private String getSubjectName(String subject) {
        return switch (subject.toLowerCase()) {
            case "chinese" -> "语文";
            case "math" -> "数学";
            case "english" -> "英语";
            default -> subject;
        };
    }

    private Double getSubjectScore(StudentScoreVO vo, String subject) {
        return switch (subject.toLowerCase()) {
            case "chinese" -> vo.getChinese();
            case "math" -> vo.getMath();
            case "english" -> vo.getEnglish();
            default -> null;
        };
    }
}
