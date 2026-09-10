package com.reditickets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.common.util.JwtUtil;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.dto.UpdatePasswordDTO;
import com.reditickets.user.dto.UpdateUserInfoDTO;
import com.reditickets.user.dto.UpdateUserStatusDTO;
import com.reditickets.user.entity.User;
import com.reditickets.user.mapper.UserMapper;
import com.reditickets.user.service.UserService;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserFeignVO;
import com.reditickets.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
/**
 * 用户服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现用户注册、登录、登出、信息管理、密码修改等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    /**
     * 用户注册实现
     * <p>
     * 步骤：确认密码一致 → 校验用户名唯一性 → 校验手机号唯一性 → BCrypt 加密密码 → 保存用户信息 → 返回结果
     * </p>
     */
    @Override
    public Result<Void> register(RegisterDTO dto) {
        // 1. 校验两次密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "两次密码输入不一致");
        }

        // 2. 使用 LambdaQueryWrapper 校验用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        long count = this.count(wrapper);
        if (count > 0) { return Result.fail(ResultCode.USERNAME_EXISTS, "用户名已存在"); }

        // 3. 校验手机号是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        count = this.count(wrapper);
        if (count > 0) { return Result.fail(ResultCode.PHONE_EXISTS, "手机号已存在"); }

        // 4. 使用 BCryptPasswordEncoder 加密密码
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 5. 构建 User 实体并使用 this.save() 保存到数据库
        User user = new User()
                .setUsername(dto.getUsername())
                .setPassword(encodedPassword)
                .setPhone(dto.getPhone())
                .setEmail(dto.getEmail())
                .setStatus(1);
        this.save(user);

        // 6. 返回成功
        log.info("用户注册成功: userId={}, username={}, phone={}", user.getId(), dto.getUsername(), dto.getPhone());
        return Result.success();
    }

    /**
     * 用户登录实现
     * <p>
     * 步骤：查询用户（支持用户名/手机号）→ 校验状态 → BCrypt 验密 → 生成 JWT → 存入 Redis → 更新登录信息 → 返回 LoginVO
     * </p>
     */
    @Override
    public Result<LoginVO> login(LoginDTO dto) {
        // 1. 使用 LambdaQueryWrapper 根据用户名或手机号查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsernameOrPhone())
                .or()
                .eq(User::getPhone, dto.getUsernameOrPhone())
                .last("LIMIT 1");
        User user = this.getOne(wrapper);

        // 2. 校验用户是否存在、是否被冻结
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == 0) {
            return Result.fail(ResultCode.USER_FROZEN, "用户已被冻结");
        }

        // 3. 校验登录失败次数（Redis 计数器，key 基于用户ID，防止用户名/手机号切换绕过）
        String failKey = "user:login:fail:" + user.getId();
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= 5) {
            return Result.fail(ResultCode.LOGIN_LOCKED, "登录失败次数过多，请15分钟后再试");
        }

        // 4. BCrypt 验密，失败则记录次数并设置过期
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, 15, TimeUnit.MINUTES);
            return Result.fail(ResultCode.PASSWORD_ERROR, "密码错误");
        }

        // 5. 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);

        // 6. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 7. 将 Token 存入 Redis
        stringRedisTemplate.opsForValue().set("token:" + user.getId(), token, 2, TimeUnit.HOURS);

        // 8. 更新最后登录时间和真实IP
        String clientIp = getClientIp();
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .set(User::getLastLoginTime, LocalDateTime.now())
                .set(User::getLastLoginIp, clientIp);
        this.update(updateWrapper);

        // 9. 组装 LoginVO 返回
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userVO);
        return Result.success(loginVO);
    }

    /**
     * 用户登出实现
     * <p>
     * 步骤：从 Token 获取用户ID → 将 Token 加入 Redis 黑名单 → 清除登录缓存
     * </p>
     */
    @Override
    public Result<Void> logout() {
        // 1. 从 SecurityContextHolder 或请求头中获取当前用户ID和 Token
        Long userId = getCurrentUserId();
        String token = getCurrentToken();

        // 2. 将 Token 加入 Redis 黑名单（有效期与 Token 过期时间一致）
        stringRedisTemplate.opsForValue().set(
                "token:blacklist:" + token, "1", 2, TimeUnit.HOURS);

        // 3. 删除登录缓存
        stringRedisTemplate.delete("token:" + userId);

        // 4. 返回成功
        return Result.success();
    }

    /**
     * 获取当前登录用户信息实现
     * <p>
     * 步骤：从 Token 解析用户ID → 查询用户 → 转换为 UserVO（脱敏）→ 返回
     * </p>
     */
    @Override
    public Result<UserVO> getCurrentUserInfo() {
        // 1. 从请求头中获取当前用户ID
        Long userId = getCurrentUserId();

        // 2. 查询用户
        User user = this.getById(userId);
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 3. 转换为 UserVO（手机号脱敏：138****1234）
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setPhone(maskPhone(user.getPhone()));
        return Result.success(userVO);
    }

    /**
     * 修改用户信息实现
     * <p>
     * 步骤：从 Token 解析用户ID → 校验手机号唯一性 → 仅更新非空字段 → 返回最新信息
     * </p>
     */
    @Override
    public Result<UserVO> updateUserInfo(UpdateUserInfoDTO dto) {
        // 1. 从请求头中获取当前用户ID
        Long userId = getCurrentUserId();

        // 2. 如果修改手机号，校验唯一性
        if (dto.getPhone() != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone())
                    .ne(User::getId, userId);
            if (this.count(wrapper) > 0) {
                return Result.fail(ResultCode.PHONE_EXISTS, "手机号已存在");
            }
        }

        // 3. 使用 LambdaUpdateWrapper 仅更新非空字段
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if (dto.getAvatar() != null) { updateWrapper.set(User::getAvatar, dto.getAvatar()); }
        if (dto.getEmail() != null) { updateWrapper.set(User::getEmail, dto.getEmail()); }
        if (dto.getPhone() != null) { updateWrapper.set(User::getPhone, dto.getPhone()); }
        this.update(updateWrapper);

        // 4. 查询最新用户信息并返回
        User updatedUser = this.getById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(updatedUser, userVO);
        return Result.success(userVO);
    }

    /**
     * 修改密码实现
     * <p>
     * 步骤：校验确认密码一致 → 校验旧密码正确 → 校验新旧密码不同 → BCrypt 加密新密码 → 更新
     * </p>
     */
    @Override
    public Result<Void> updatePassword(UpdatePasswordDTO dto) {
        // 1. 校验两次新密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "两次密码输入不一致");
        }

        // 2. 从请求头中获取当前用户ID并查询用户
        Long userId = getCurrentUserId();
        User user = this.getById(userId);

        // 3. 使用 BCryptPasswordEncoder 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.fail(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        // 4. 校验新旧密码不能相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "新密码不能与旧密码相同");
        }

        // 5. 加密新密码并更新
        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getPassword, encodedNewPassword);
        this.update(updateWrapper);

        // 6. 返回成功
        return Result.success();
    }

    /**
     * 根据用户ID查询用户信息实现
     * <p>
     * 步骤：查询用户 → 校验存在 → 转换为 UserVO（脱敏）→ 返回
     * </p>
     */
    @Override
    public Result<UserVO> getUserById(Long userId) {
        // 1. 使用 this.getById() 查询用户（ServiceImpl 内置方法）
        User user = this.getById(userId);

        // 2. 校验用户是否存在
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 3. 转换为 UserVO（手机号脱敏：138****1234）
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setPhone(maskPhone(user.getPhone()));
        return Result.success(userVO);
    }

    /**
     * 获取用户列表实现（管理员）
     * <p>
     * 步骤：构建分页查询 → 查询未删除用户 → 转换为 UserVO 列表 → 返回
     * </p>
     */
    @Override
    public Result<List<UserVO>> listUsers(Integer page, Integer size) {
        // 1. 使用 MyBatis Plus 分页查询
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = this.page(pageParam, wrapper);

        // 2. 转换为 UserVO 列表（手机号脱敏：138****1234）
        List<UserVO> voList = userPage.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            vo.setPhone(maskPhone(user.getPhone()));
            return vo;
        }).collect(Collectors.toList());

        // 3. 返回结果
        return Result.success(voList);
    }

    /**
     * 冻结/解冻用户实现（管理员）
     * <p>
     * 步骤：校验用户存在 → 校验不可操作自身 → 更新状态
     * </p>
     */
    @Override
    public Result<Void> updateUserStatus(UpdateUserStatusDTO dto) {
        // 1. 校验目标用户是否存在
        User targetUser = this.getById(dto.getUserId());
        if (targetUser == null) {
            return Result.fail(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 2. 从请求头中获取当前管理员ID，不可操作自身
        Long currentUserId = getCurrentUserId();
        if (currentUserId.equals(dto.getUserId())) {
            return Result.fail(ResultCode.FORBIDDEN.getCode(), "不能操作自己的账户");
        }

        // 3. 使用 LambdaUpdateWrapper 更新用户状态
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, dto.getUserId())
                .set(User::getStatus, dto.getStatus());
        this.update(updateWrapper);

        // 4. 返回成功
        return Result.success();
    }

    /**
     * 内部查询用户信息实现（供 Feign 调用）
     * <p>
     * 步骤：查询用户 → 转换为 UserFeignVO（仅核心字段）→ 返回
     * </p>
     */
    @Override
    public UserFeignVO getInternalUser(Long userId) {
        // 1. 查询用户
        User user = this.getById(userId);
        if (user == null) {
            return null;
        }

        // 2. 转换为 UserFeignVO 返回（手机号脱敏：138****1234）
        UserFeignVO vo = new UserFeignVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setStatus(user.getStatus());
        return vo;
    }

    /**
     * 批量查询用户信息实现（供 Feign 调用）
     * <p>
     * 步骤：批量查询用户 → 转换为 UserFeignVO 列表（手机号脱敏：138****1234） → 返回
     * </p>
     */
    @Override
    public List<UserFeignVO> batchGetInternalUsers(List<Long> userIds) {
        // 1. 使用 this.listByIds() 批量查询用户
        List<User> users = this.listByIds(userIds);

        // 2. 转换为 UserFeignVO 列表（手机号脱敏：138****1234）
        return users.stream().map(user -> {
            UserFeignVO vo = new UserFeignVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setPhone(maskPhone(user.getPhone()));
            vo.setStatus(user.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    //=================辅助方法=================

    /**
     * 手机号脱敏处理
     * <p>
     * 将手机号中间四位替换为 ****，例如：138****1234
     * </p>
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 从请求头中获取当前 JWT Token
     * <p>
     * 从 Authorization 请求头中提取 Bearer Token，去除 "Bearer " 前缀
     * </p>
     *
     * @return JWT Token 字符串
     */
    private String getCurrentToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 从请求头中获取当前登录用户ID
     * <p>
     * 从 Authorization 请求头中提取 Token，解析后获取 userId
     * </p>
     *
     * @return 当前用户ID
     */
    private Long getCurrentUserId() {
        // 1. 从请求头中获取 Token
        String token = getCurrentToken();
        if (token != null) {
            // 2. 解析 Token 并获取 userId
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 从 HttpServletRequest 中提取真实客户端 IP
     * <p>
     * 优先从反向代理头（X-Forwarded-For、X-Real-IP）获取，兼容 Nginx/网关等场景
     * </p>
     *
     * @return 客户端真实 IP 地址
     */
    private String getClientIp() {
        // 1. 从 X-Forwarded-For 头获取 IP，优先返回第一个非 unknown 的 IP
        String ip = request.getHeader("X-Forwarded-For");
        // 2. 如果 X-Forwarded-For 为空或 unknown，再从 X-Real-IP 头获取
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // 3. 返回第一个非 unknown 的 IP
            return ip.split(",")[0].trim();
        }
        // 4. 如果 X-Real-IP 为空或 unknown，再从 HttpServletRequest 获取客户端 IP
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // 5. 返回第一个非 unknown 的 IP
            return ip;
        }
        // 6. 如果 HttpServletRequest 也为空或 unknown，返回默认值
        return request.getRemoteAddr();
    }
}