package com.reditickets.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.entity.User;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserVO;

/**
 * 用户服务接口
 * <p>
 * 继承 MyBatis Plus IService 接口，提供用户业务逻辑的标准定义
 * </p>
 *
 * @author gugu
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param dto 注册请求参数
     * @return 注册结果
     */
    Result<Void> register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录请求参数
     * @return 登录结果（包含 Token 和用户信息）
     */
    Result<LoginVO> login(LoginDTO dto);

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户视图对象（脱敏）
     */
    Result<UserVO> getUserById(Long userId);
}