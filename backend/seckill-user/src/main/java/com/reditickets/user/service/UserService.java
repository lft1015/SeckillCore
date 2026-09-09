package com.reditickets.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.dto.UpdatePasswordDTO;
import com.reditickets.user.dto.UpdateUserInfoDTO;
import com.reditickets.user.dto.UpdateUserStatusDTO;
import com.reditickets.user.entity.User;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserFeignVO;
import com.reditickets.user.vo.UserVO;

import java.util.List;

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
     * 用户登录（支持用户名/手机号）
     *
     * @param dto 登录请求参数
     * @return 登录结果（包含 Token 和用户信息）
     */
    Result<LoginVO> login(LoginDTO dto);

    /**
     * 用户登出
     * <p>
     * 客户端清除 Token，服务端可将 Token 加入 Redis 黑名单
     * </p>
     *
     * @return 登出结果
     */
    Result<Void> logout();

    /**
     * 获取当前登录用户信息
     * <p>
     * 从 Token 中解析用户ID，返回脱敏后的用户详细信息
     * </p>
     *
     * @return 当前用户视图对象
     */
    Result<UserVO> getCurrentUserInfo();

    /**
     * 修改用户信息（头像、邮箱、手机号）
     * <p>
     * 仅更新传入的非空字段，手机号需校验唯一性
     * </p>
     *
     * @param dto 修改信息请求参数
     * @return 最新用户信息
     */
    Result<UserVO> updateUserInfo(UpdateUserInfoDTO dto);

    /**
     * 修改密码
     * <p>
     * 校验旧密码正确后更新为新密码
     * </p>
     *
     * @param dto 修改密码请求参数
     * @return 修改结果
     */
    Result<Void> updatePassword(UpdatePasswordDTO dto);

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户视图对象（脱敏）
     */
    Result<UserVO> getUserById(Long userId);

    /**
     * 获取用户列表（管理员）
     *
     * @param page 页码
     * @param size 每页数量
     * @return 用户列表
     */
    Result<List<UserVO>> listUsers(Integer page, Integer size);

    /**
     * 冻结/解冻用户（管理员）
     *
     * @param dto 用户状态修改参数
     * @return 操作结果
     */
    Result<Void> updateUserStatus(UpdateUserStatusDTO dto);

    /**
     * 内部查询用户信息（供 Feign 调用）
     *
     * @param userId 用户ID
     * @return 用户Feign视图对象（仅核心字段）
     */
    UserFeignVO getInternalUser(Long userId);

    /**
     * 批量查询用户信息（供 Feign 调用）
     *
     * @param userIds 用户ID列表
     * @return 用户Feign视图对象列表
     */
    List<UserFeignVO> batchGetInternalUsers(List<Long> userIds);
}