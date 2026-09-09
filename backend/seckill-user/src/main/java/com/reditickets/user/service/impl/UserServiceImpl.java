package com.reditickets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.entity.User;
import com.reditickets.user.mapper.UserMapper;
import com.reditickets.user.service.UserService;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现用户注册、登录、信息查询等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册实现
     * <p>
     * 步骤：校验用户名唯一性 → BCrypt 加密密码 → 保存用户信息 → 返回结果
     * </p>
     */
    @Override
    public Result<Void> register(RegisterDTO dto) {
        // TODO: 1. 使用 LambdaQueryWrapper 校验用户名是否已存在
        //    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        //    wrapper.eq(User::getUsername, dto.getUsername());
        //    long count = this.count(wrapper);
        //    if (count > 0) { return Result.fail(ResultCode.USERNAME_EXISTS); }
        //
        // TODO: 2. 使用 BCryptPasswordEncoder 加密密码
        //    String encodedPassword = passwordEncoder.encode(dto.getPassword());
        //
        // TODO: 3. 构建 User 实体并使用 this.save() 保存到数据库
        //    User user = new User()
        //        .setUsername(dto.getUsername())
        //        .setPassword(encodedPassword)
        //        .setPhone(dto.getPhone())
        //        .setEmail(dto.getEmail())
        //        .setStatus(1);
        //    this.save(user);
        //
        // TODO: 4. 返回成功
        //    return Result.success();
        throw new UnsupportedOperationException("TODO: 实现注册逻辑");
    }

    /**
     * 用户登录实现
     * <p>
     * 步骤：查询用户 → 校验状态 → BCrypt 验密 → 生成 JWT → 存入 Redis → 更新登录信息 → 返回 LoginVO
     * </p>
     */
    @Override
    public Result<LoginVO> login(LoginDTO dto) {
        // TODO: 1. 使用 LambdaQueryWrapper 根据用户名查询用户
        //    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        //    wrapper.eq(User::getUsername, dto.getUsername());
        //    User user = this.getOne(wrapper);
        //
        // TODO: 2. 校验用户是否存在、是否被冻结
        //    if (user == null) { return Result.fail(ResultCode.USER_NOT_FOUND); }
        //    if (user.getStatus() == 0) { return Result.fail(ResultCode.USER_FROZEN); }
        //
        // TODO: 3. 使用 BCryptPasswordEncoder.matches() 校验密码
        //    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        //        return Result.fail(ResultCode.PASSWORD_ERROR);
        //    }
        //
        // TODO: 4. 生成 JWT Token（使用 JJWT 库或类似工具）
        //    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        //
        // TODO: 5. 将 Token 存入 Redis（用于后续校验和黑名单）
        //    stringRedisTemplate.opsForValue().set(
        //        "token:" + user.getId(), token, 7, TimeUnit.DAYS);
        //
        // TODO: 6. 使用 LambdaUpdateWrapper 更新最后登录时间和 IP
        //    LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        //    updateWrapper.eq(User::getId, user.getId())
        //        .set(User::getLastLoginTime, LocalDateTime.now())
        //        .set(User::getLastLoginIp, ip);
        //    this.update(updateWrapper);
        //
        // TODO: 7. 组装 LoginVO 返回
        //    UserVO userVO = new UserVO();
        //    BeanUtils.copyProperties(user, userVO);
        //    LoginVO loginVO = new LoginVO();
        //    loginVO.setToken(token);
        //    loginVO.setUserInfo(userVO);
        //    return Result.success(loginVO);
        throw new UnsupportedOperationException("TODO: 实现登录逻辑");
    }

    /**
     * 根据用户ID查询用户信息
     * <p>
     * 步骤：查询用户 → 校验存在 → 转换为 UserVO（脱敏）→ 返回
     * </p>
     */
    @Override
    public Result<UserVO> getUserById(Long userId) {
        // TODO: 1. 使用 this.getById() 查询用户（ServiceImpl 内置方法）
        //    User user = this.getById(userId);
        //
        // TODO: 2. 校验用户是否存在
        //    if (user == null) { return Result.fail(ResultCode.USER_NOT_FOUND); }
        //
        // TODO: 3. 使用 BeanUtils.copyProperties 转换为 UserVO 返回
        //    UserVO userVO = new UserVO();
        //    BeanUtils.copyProperties(user, userVO);
        //    return Result.success(userVO);
        throw new UnsupportedOperationException("TODO: 实现查询用户逻辑");
    }
}