package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * 发送短信验证码功能
     *
     * @param phone   手机号
     * @param session session
     * @return 返回结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 用户登录功能
     * @param loginForm 登录表单
     * @param session session
     * @return 返回结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
