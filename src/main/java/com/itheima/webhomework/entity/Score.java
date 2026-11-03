package com.itheima.webhomework.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sid;
    private Double chinese;
    private Double math;
    private Double english;
    private Date createTime;
    private Date updateTime;

    public Double getAverage() {
        int count = 0;
        double sum = 0;
        if (chinese != null) { sum += chinese; count++; }
        if (math != null) { sum += math; count++; }
        if (english != null) { sum += english; count++; }
        return count > 0 ? sum / count : null;
    }
}
