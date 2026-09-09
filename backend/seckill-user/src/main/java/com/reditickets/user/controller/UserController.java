package com.reditickets.user.controller;

import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.service.UserService;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制层
 * <p>
 * 提供用户注册、登录、信息查询等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口
     * <p>
     * 接收用户名、密码、手机号等信息，完成新用户注册流程
     * </p>
     *
     * @param dto 注册请求参数（用户名、密码、手机号、邮箱）
     * @return 统一响应结果（成功/失败）
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录接口
     * <p>
     * 校验用户名和密码，登录成功后返回 JWT Token 和用户基本信息
     * </p>
     *
     * @param dto 登录请求参数（用户名、密码）
     * @return 统一响应结果（包含 Token 和用户信息）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 根据用户ID查询用户信息
     * <p>
     * 返回脱敏后的用户基本信息，不包含密码等敏感字段
     * </p>
     *
     * @param id 用户ID
     * @return 统一响应结果（包含用户视图对象）
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}