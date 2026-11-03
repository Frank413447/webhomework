// TeacherService.java
package com.itheima.webhomework.service;

import com.itheima.webhomework.entity.Teacher;

public interface TeacherService {
    /**
     * 教师登录验证
     * @param tid 教师账号
     * @param password 密码
     * @return 验证成功的教师对象，失败返回null
     */
    Teacher login(String tid, String password);

    Teacher getTeacherById(String tid);
}

