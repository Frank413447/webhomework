package com.itheima.webhomework.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "教师ID不能为空")
    private String tid;
    private String tname;
    @NotBlank(message = "密码不能为空")
    private String password;
}
