package com.itheima.webhomework.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentScoreVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sid;
    private String sname;
    private String gender;
    private String clazz;
    private Double chinese;
    private Double math;
    private Double english;

    public Double getAverage() {
        int count = 0;
        double sum = 0;
        if (chinese != null) { sum += chinese; count++; }
        if (math != null) { sum += math; count++; }
        if (english != null) { sum += english; count++; }
        return count > 0 ? sum / count : null;
    }
}
