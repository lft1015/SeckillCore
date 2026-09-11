package com.reditickets.user.controller;

import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RefreshTokenDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制层（严格 RESTful + 版本命名）
 * <p>
 * 资源设计：users（用户集合）、auth（认证会话）、internal/users（内部调用）
 * 对外接口统一使用 /api/v1/ 前缀，通过 HTTP 方法表达操作语义
 * </p>
 *
 * @author gugu
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==================== 用户资源：/api/v1/users ====================

    /**
     * 用户注册
     * <p>
     * POST /api/v1/users —— 向 users 集合新增一个资源
     * </p>
     *
     * @param dto 注册请求参数
     * @return 统一响应结果
     */
    @PostMapping("/api/v1/users")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 获取用户列表（管理员）
     * <p>
     * GET /api/v1/users —— 查询 users 集合
     * </p>
     *
     * @param page 页码
     * @param size 每页数量
     * @return 用户列表
     */
    @GetMapping("/api/v1/users")
    public Result<List<UserVO>> listUsers(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return userService.listUsers(page, size);
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * GET /api/v1/users/me —— me 作为当前认证用户的便捷别名
     * </p>
     *
     * @return 统一响应结果（包含用户视图对象）
     */
    @GetMapping("/api/v1/users/me")
    public Result<UserVO> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 修改用户信息
     * <p>
     * PUT /api/v1/users/me —— 全量更新当前用户资源
     * </p>
     *
     * @param dto 修改信息请求参数
     * @return 统一响应结果（包含最新用户信息）
     */
    @PutMapping("/api/v1/users/me")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO dto) {
        return userService.updateUserInfo(dto);
    }

    /**
     * 修改密码
     * <p>
     * PUT /api/v1/users/me/password —— 更新当前用户的密码子资源
     * </p>
     *
     * @param dto 修改密码请求参数
     * @return 统一响应结果
     */
    @PutMapping("/api/v1/users/me/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        return userService.updatePassword(dto);
    }

    /**
     * 冻结/解冻用户（管理员）
     * <p>
     * PUT /api/v1/users/{userId}/status —— 更新指定用户的状态子资源
     * </p>
     *
     * @param userId 目标用户ID（从路径中提取）
     * @param dto    用户状态修改参数（仅 status 字段有效，userId 由路径提供）
     * @return 统一响应结果
     */
    @PutMapping("/api/v1/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateUserStatusDTO dto) {
        dto.setUserId(userId);
        return userService.updateUserStatus(dto);
    }

    // ==================== 认证资源：/api/v1/auth ====================

    /**
     * 用户登录
     * <p>
     * POST /api/v1/auth/login —— 创建认证会话获取令牌
     * </p>
     *
     * @param dto 登录请求参数
     * @return 统一响应结果（包含 Token、刷新令牌和用户信息）
     */
    @PostMapping("/api/v1/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 用户登出
     * <p>
     * DELETE /api/v1/auth/session —— 销毁当前认证会话
     * </p>
     *
     * @return 统一响应结果
     */
    @DeleteMapping("/api/v1/auth/session")
    public Result<Void> logout() {
        return userService.logout();
    }

    /**
     * Token 刷新
     * <p>
     * POST /api/v1/auth/refresh —— 使用刷新令牌重新获取访问令牌
     * </p>
     *
     * @param dto 刷新令牌请求参数
     * @return 统一响应结果（包含新的访问令牌和刷新令牌）
     */
    @PostMapping("/api/v1/auth/refresh")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return userService.refreshToken(dto);
    }

    // ==================== 内部 Feign 接口 ====================

    /**
     * 内部查询用户信息（供其他微服务 Feign 调用）
     *
     * @param userId 用户ID
     * @return 用户Feign视图对象
     */
    @GetMapping("/api/v1/internal/users/{userId}")
    public UserFeignVO getInternalUser(@PathVariable Long userId) {
        return userService.getInternalUser(userId);
    }

    /**
     * 批量查询用户信息（供其他微服务 Feign 调用）
     *
     * @param userIds 用户ID列表
     * @return 用户Feign视图对象列表
     */
    @PostMapping("/api/v1/internal/users/batch")
    public List<UserFeignVO> batchGetInternalUsers(@RequestBody List<Long> userIds) {
        return userService.batchGetInternalUsers(userIds);
    }
}