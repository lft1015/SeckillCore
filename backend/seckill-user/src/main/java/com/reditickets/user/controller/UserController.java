package com.reditickets.user.controller;

import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.dto.UpdatePasswordDTO;
import com.reditickets.user.dto.UpdateUserInfoDTO;
import com.reditickets.user.dto.UpdateUserStatusDTO;
import com.reditickets.user.service.UserService;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserFeignVO;
import com.reditickets.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制层
 * <p>
 * 提供用户注册、登录、登出、信息查询、信息修改、密码修改等 REST API 接口，
 * 同时提供内部 Feign 调用接口供其他微服务使用
 * </p>
 *
 * @author gugu
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口
     * <p>
     * 接收用户名、密码、手机号等信息，完成新用户注册流程
     * </p>
     *
     * @param dto 注册请求参数
     * @return 统一响应结果
     */
    @PostMapping("/api/user/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录接口
     * <p>
     * 支持用户名/手机号两种方式登录，校验凭证后返回 JWT Token 和用户基本信息
     * </p>
     *
     * @param dto 登录请求参数
     * @return 统一响应结果（包含 Token 和用户信息）
     */
    @PostMapping("/api/user/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 用户登出接口
     * <p>
     * JWT 无状态认证，登出仅需客户端清除 Token。
     * 服务端可在此记录登出日志或将 Token 加入 Redis 黑名单
     * </p>
     *
     * @return 统一响应结果
     */
    @PostMapping("/api/user/logout")
    public Result<Void> logout() {
        return userService.logout();
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 从请求头 Token 中解析用户ID，返回脱敏后的用户详细信息
     * </p>
     *
     * @return 统一响应结果（包含用户视图对象）
     */
    @GetMapping("/api/user/info")
    public Result<UserVO> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 修改用户信息
     * <p>
     * 支持修改头像、邮箱、手机号，仅更新传入的非空字段
     * </p>
     *
     * @param dto 修改信息请求参数
     * @return 统一响应结果（包含最新用户信息）
     */
    @PutMapping("/api/user/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO dto) {
        return userService.updateUserInfo(dto);
    }

    /**
     * 修改密码
     * <p>
     * 校验旧密码正确后更新为新密码，密码修改后建议前端引导重新登录
     * </p>
     *
     * @param dto 修改密码请求参数
     * @return 统一响应结果
     */
    @PutMapping("/api/user/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        return userService.updatePassword(dto);
    }

    /**
     * 根据用户ID查询用户信息（对外）
     * <p>
     * 返回脱敏后的用户基本信息，不包含密码等敏感字段
     * </p>
     *
     * @param id 用户ID
     * @return 统一响应结果
     */
    @GetMapping("/api/user/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // ==================== 管理员接口 ====================

    /**
     * 获取用户列表（管理员）
     *
     * @param page 页码
     * @param size 每页数量
     * @return 用户列表
     */
    @GetMapping("/api/user/list")
    public Result<List<UserVO>> listUsers(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return userService.listUsers(page, size);
    }

    /**
     * 冻结/解冻用户（管理员）
     * <p>
     * 不可操作自身账户，冻结后用户无法登录和参与秒杀
     * </p>
     *
     * @param dto 用户状态修改参数
     * @return 统一响应结果
     */
    @PutMapping("/api/user/status")
    public Result<Void> updateUserStatus(@Valid @RequestBody UpdateUserStatusDTO dto) {
        return userService.updateUserStatus(dto);
    }

    // ==================== 内部 Feign 接口 ====================

    /**
     * 内部查询用户信息（供其他微服务 Feign 调用）
     * <p>
     * 仅限内部服务调用，不对外暴露，返回用户核心信息
     * </p>
     *
     * @param userId 用户ID
     * @return 用户Feign视图对象
     */
    @GetMapping("/internal/user/{userId}")
    public UserFeignVO getInternalUser(@PathVariable Long userId) {
        return userService.getInternalUser(userId);
    }

    /**
     * 批量查询用户信息（供其他微服务 Feign 调用）
     *
     * @param userIds 用户ID列表
     * @return 用户Feign视图对象列表
     */
    @PostMapping("/internal/user/batch")
    public List<UserFeignVO> batchGetInternalUsers(@RequestBody List<Long> userIds) {
        return userService.batchGetInternalUsers(userIds);
    }
}