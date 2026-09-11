package com.reditickets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.common.util.JwtUtil;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RefreshTokenDTO;
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
import java.util.ArrayList;
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

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 用户注册实现
     * <p>
     * 步骤：XSS过滤 → 确认密码一致 → 校验用户名唯一性 → 校验手机号唯一性 → IP限流 → BCrypt 加密密码 → 保存用户信息 → 返回结果
     * </p>
     */
    @Override
    public Result<Void> register(RegisterDTO dto) {
        // 1. 校验两次密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "两次密码输入不一致");
        }

        // 2. XSS 过滤用户名
        String sanitizedUsername = sanitizeInput(dto.getUsername());
        if (!sanitizedUsername.equals(dto.getUsername())) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "用户名包含非法字符");
        }

        // 3. IP 级别限流：单 IP 每分钟最多 3 次注册
        String clientIp = getClientIp();
        String rateLimitKey = "rate:limit:register:" + clientIp;
        String rateCount = stringRedisTemplate.opsForValue().get(rateLimitKey);
        if (rateCount != null && Integer.parseInt(rateCount) >= 3) {
            return Result.fail(ResultCode.REGISTER_TOO_FREQUENT);
        }

        // 4. 使用 LambdaQueryWrapper 校验用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, sanitizedUsername);
        long count = this.count(wrapper);
        if (count > 0) { return Result.fail(ResultCode.USERNAME_EXISTS, "用户名已存在"); }

        // 5. 校验手机号是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        count = this.count(wrapper);
        if (count > 0) { return Result.fail(ResultCode.PHONE_EXISTS, "手机号已存在"); }

        // 6. 使用 BCryptPasswordEncoder 加密密码
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 7. 构建 User 实体并使用 this.save() 保存到数据库
        // 密码历史记录初始化为包含当前密码的 JSON 数组
        String initialHistory = "[\"" + encodedPassword + "\"]";
        User user = new User()
                .setUsername(sanitizedUsername)
                .setPassword(encodedPassword)
                .setPhone(dto.getPhone())
                .setEmail(dto.getEmail() != null ? sanitizeInput(dto.getEmail()) : null)
                .setStatus(1)
                .setPasswordHistory(initialHistory);
        this.save(user);

        // 8. IP 限流计数 +1
        stringRedisTemplate.opsForValue().increment(rateLimitKey);
        stringRedisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);

        // 9. 返回成功
        log.info("用户注册成功: userId={}, username={}, phone={}", user.getId(), sanitizedUsername, dto.getPhone());
        return Result.success();
    }

    /**
     * 用户登录实现
     * <p>
     * 步骤：查询用户（支持用户名/手机号）→ 校验状态 → BCrypt 验密 → 生成 JWT + 刷新令牌 → 设备并发控制 → 存入 Redis → 更新登录信息 → 返回 LoginVO
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
            return Result.fail(ResultCode.PASSWORD_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.fail(ResultCode.USER_FROZEN, "账户已被冻结，请联系管理员");
        }

        // 3. 校验登录失败次数（Redis 计数器，key 基于登录凭证，防止暴力破解）
        String failKey = "user:login:fail:" + dto.getUsernameOrPhone();
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= 5) {
            return Result.fail(ResultCode.LOGIN_LOCKED);
        }

        // 4. BCrypt 验密，失败则记录次数并设置过期
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, 15, TimeUnit.MINUTES);
            return Result.fail(ResultCode.PASSWORD_ERROR, "用户名或密码错误");
        }

        // 5. 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);

        // 6. 生成 JWT 访问令牌和刷新令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 7. 设备并发控制：同一账号最多 3 个设备在线
        String deviceId = getClientIp() + ":" + (request.getHeader("User-Agent") != null ? request.getHeader("User-Agent").hashCode() : "");
        String deviceKey = "user:devices:" + user.getId();
        String deviceSetKey = "refresh:token:" + user.getId() + ":" + deviceId;
        Long deviceCount = stringRedisTemplate.opsForSet().size(deviceKey);
        if (deviceCount != null && deviceCount >= 3) {
            // 踢出最早设备：删除最早设备的刷新令牌
            String oldestDevice = stringRedisTemplate.opsForSet().pop(deviceKey);
            if (oldestDevice != null) {
                stringRedisTemplate.delete("refresh:token:" + user.getId() + ":" + oldestDevice);
            }
        }
        stringRedisTemplate.opsForSet().add(deviceKey, deviceId);
        stringRedisTemplate.expire(deviceKey, 7, TimeUnit.DAYS);

        // 8. 将访问令牌和刷新令牌存入 Redis
        stringRedisTemplate.opsForValue().set("token:" + user.getId(), token, 2, TimeUnit.HOURS);
        stringRedisTemplate.opsForValue().set(deviceSetKey, refreshToken, 7, TimeUnit.DAYS);

        // 9. 更新最后登录时间和真实IP
        String clientIp = getClientIp();
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .set(User::getLastLoginTime, LocalDateTime.now())
                .set(User::getLastLoginIp, clientIp);
        this.update(updateWrapper);

        // 10. 组装 LoginVO 返回（符合 V2.0 需求文档格式）
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setAvatar(user.getAvatar());

        log.info("用户登录成功: userId={}, username={}, ip={}", user.getId(), user.getUsername(), clientIp);
        return Result.success(loginVO);
    }

    /**
     * 用户登出实现
     * <p>
     * 步骤：从 Token 获取用户ID → 将 Token 加入 Redis 黑名单 → 清除刷新令牌 → 清除设备记录 → 清除登录缓存
     * </p>
     */
    @Override
    public Result<Void> logout() {
        // 1. 从 SecurityContextHolder 或请求头中获取当前用户ID和 Token
        Long userId = getCurrentUserId();
        String token = getCurrentToken();
        if (userId == null || token == null) {
            return Result.success();
        }

        // 2. 将 Token 加入 Redis 黑名单（有效期与 Token 剩余时间一致）
        long remaining = jwtUtil.getRemainingTime(token);
        if (remaining > 0) {
            stringRedisTemplate.opsForValue().set(
                    "token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
        }

        // 3. 清除当前设备的刷新令牌
        String deviceId = getClientIp() + ":" + (request.getHeader("User-Agent") != null ? request.getHeader("User-Agent").hashCode() : "");
        stringRedisTemplate.delete("refresh:token:" + userId + ":" + deviceId);
        stringRedisTemplate.opsForSet().remove("user:devices:" + userId, deviceId);

        // 4. 删除登录缓存
        stringRedisTemplate.delete("token:" + userId);

        // 5. 删除用户信息缓存
        deleteUserCache(userId);

        log.info("用户登出成功: userId={}, ip={}", userId, getClientIp());
        return Result.success();
    }

    /**
     * Token 刷新实现
     * <p>
     * 步骤：解析刷新令牌 → 校验有效期 → 滚动刷新策略 → 旧刷新令牌失效 → 返回新令牌对
     * </p>
     */
    @Override
    public Result<LoginVO> refreshToken(RefreshTokenDTO dto) {
        // 1. 解析刷新令牌
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(dto.getRefreshToken());
        } catch (Exception e) {
            return Result.fail(ResultCode.REFRESH_TOKEN_INVALID);
        }
        if (userId == null) {
            return Result.fail(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 校验刷新令牌是否有效（未过期、签名正确）
        if (!jwtUtil.validateToken(dto.getRefreshToken())) {
            return Result.fail(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        // 3. 防刷新令牌盗用：查找当前设备对应的刷新令牌
        String deviceId = getClientIp() + ":" + (request.getHeader("User-Agent") != null ? request.getHeader("User-Agent").hashCode() : "");
        String deviceTokenKey = "refresh:token:" + userId + ":" + deviceId;
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(deviceTokenKey);

        if (storedRefreshToken == null) {
            // 设备对应的刷新令牌不存在，可能已被使用或过期
            return Result.fail(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!storedRefreshToken.equals(dto.getRefreshToken())) {
            // 刷新令牌不匹配，可能被盗用：立即失效该用户所有令牌
            stringRedisTemplate.delete("token:" + userId);
            // 删除所有设备的刷新令牌
            String deviceKey = "user:devices:" + userId;
            stringRedisTemplate.opsForSet().members(deviceKey).forEach(d -> {
                stringRedisTemplate.delete("refresh:token:" + userId + ":" + d);
            });
            stringRedisTemplate.delete(deviceKey);
            log.warn("刷新令牌被盗用检测: userId={}, ip={}", userId, getClientIp());
            return Result.fail(ResultCode.REFRESH_TOKEN_REUSED);
        }

        // 4. 查询用户，校验状态
        User user = this.getById(userId);
        if (user == null || user.getStatus() == 0) {
            return Result.fail(ResultCode.USER_FROZEN);
        }

        // 5. 滚动刷新：生成新的访问令牌和刷新令牌
        String newToken = jwtUtil.generateToken(userId, user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 6. 更新 Redis：新访问令牌 + 新刷新令牌（旧刷新令牌自动被覆盖）
        stringRedisTemplate.opsForValue().set("token:" + userId, newToken, 2, TimeUnit.HOURS);
        stringRedisTemplate.opsForValue().set(deviceTokenKey, newRefreshToken, 7, TimeUnit.DAYS);

        // 7. 旧访问令牌加入黑名单
        String oldToken = getCurrentToken();
        if (oldToken != null) {
            long remaining = jwtUtil.getRemainingTime(oldToken);
            if (remaining > 0) {
                stringRedisTemplate.opsForValue().set(
                        "token:blacklist:" + oldToken, "1", remaining, TimeUnit.MILLISECONDS);
            }
        }

        // 8. 组装返回
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(newToken);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setAvatar(user.getAvatar());

        log.info("Token 刷新成功: userId={}", userId);
        return Result.success(loginVO);
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

        // 2. 先查 Redis 缓存
        UserVO cached = getUserFromCache(userId);
        if (cached != null) {
            return Result.success(cached);
        }

        // 3. 缓存未命中，查询数据库
        User user = this.getById(userId);
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 4. 转换为 UserVO（手机号脱敏：138****1234，邮箱脱敏：u***@example.com）
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setPhone(maskPhone(user.getPhone()));
        userVO.setEmail(maskEmail(user.getEmail()));

        // 5. 写入缓存
        setUserToCache(userId, userVO);
        return Result.success(userVO);
    }

    /**
     * 修改用户信息实现
     * <p>
     * 步骤：从 Token 解析用户ID → 校验手机号唯一性 → 手机号冷却期检查 → 仅更新非空字段 → 返回最新信息
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

            // 手机号冷却期检查：原手机号 30 天内不可被其他用户注册
            User currentUser = this.getById(userId);
            if (currentUser.getPhone() != null && !currentUser.getPhone().equals(dto.getPhone())) {
                stringRedisTemplate.opsForValue().set(
                        "user:phone:cooldown:" + currentUser.getPhone(),
                        "1", 30, TimeUnit.DAYS);
            }
        }

        // 3. 使用 LambdaUpdateWrapper 仅更新非空字段
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if (dto.getAvatar() != null) { updateWrapper.set(User::getAvatar, dto.getAvatar()); }
        if (dto.getEmail() != null) { updateWrapper.set(User::getEmail, sanitizeInput(dto.getEmail())); }
        if (dto.getPhone() != null) { updateWrapper.set(User::getPhone, dto.getPhone()); }
        this.update(updateWrapper);

        // 4. 查询最新用户信息并返回
        User updatedUser = this.getById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(updatedUser, userVO);
        userVO.setPhone(maskPhone(updatedUser.getPhone()));
        userVO.setEmail(maskEmail(updatedUser.getEmail()));

        // 5. 删除缓存，下次查询时重新加载
        deleteUserCache(userId);
        return Result.success(userVO);
    }

    /**
     * 修改密码实现
     * <p>
     * 步骤：校验确认密码一致 → 校验旧密码正确 → 校验新旧密码不同 → 密码强度校验 → 密码历史校验 → BCrypt 加密 → 更新密码 → 失效所有 Token
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

        // 5. 校验密码历史：新密码不能与最近 3 次历史密码相同
        String passwordHistory = user.getPasswordHistory();
        if (passwordHistory != null && !passwordHistory.isEmpty()) {
            try {
                List<String> historyList = objectMapper.readValue(passwordHistory,
                        new TypeReference<List<String>>() {});
                for (String oldHash : historyList) {
                    if (passwordEncoder.matches(dto.getNewPassword(), oldHash)) {
                        return Result.fail(ResultCode.PASSWORD_HISTORY_REPEATED);
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("解析密码历史失败: userId={}", userId, e);
            }
        }

        // 6. 加密新密码并更新
        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());

        // 7. 更新密码历史（保留最近 3 次）
        List<String> newHistory = new ArrayList<>();
        newHistory.add(encodedNewPassword);
        if (passwordHistory != null && !passwordHistory.isEmpty()) {
            try {
                List<String> oldHistory = objectMapper.readValue(passwordHistory,
                        new TypeReference<List<String>>() {});
                for (String oldHash : oldHistory) {
                    if (newHistory.size() >= 3) break;
                    newHistory.add(oldHash);
                }
            } catch (JsonProcessingException e) {
                log.warn("解析密码历史失败: userId={}", userId, e);
            }
        }
        String newHistoryJson;
        try {
            newHistoryJson = objectMapper.writeValueAsString(newHistory);
        } catch (JsonProcessingException e) {
            newHistoryJson = "[\"" + encodedNewPassword + "\"]";
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getPassword, encodedNewPassword)
                .set(User::getPasswordHistory, newHistoryJson);
        this.update(updateWrapper);

        // 8. 密码修改成功后，立即失效该用户所有 Token
        invalidateAllUserTokens(userId);

        // 9. 清除用户信息缓存
        deleteUserCache(userId);

        log.info("密码修改成功: userId={}", userId);
        return Result.success();
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

        // 2. 转换为 UserVO 列表（手机号脱敏：138****1234，邮箱脱敏：u***@example.com）
        List<UserVO> voList = userPage.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            vo.setPhone(maskPhone(user.getPhone()));
            vo.setEmail(maskEmail(user.getEmail()));
            return vo;
        }).collect(Collectors.toList());

        // 3. 返回结果
        return Result.success(voList);
    }

    /**
     * 冻结/解冻用户实现（管理员）
     * <p>
     * 步骤：校验用户存在 → 校验不可操作自身 → 更新状态 → 冻结时失效所有 Token
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

        // 4. 冻结操作：立即失效该用户所有 Token
        if (dto.getStatus() != null && dto.getStatus() == 0) {
            invalidateAllUserTokens(dto.getUserId());
        }

        // 5. 删除缓存，下次查询时重新加载
        deleteUserCache(dto.getUserId());

        log.info("用户状态变更: targetUserId={}, status={}, operatorId={}", dto.getUserId(), dto.getStatus(), currentUserId);
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
        // 1. 先查 Redis 缓存
        UserVO cached = getUserFromCache(userId);
        if (cached != null) {
            return toUserFeignVO(cached);
        }

        // 2. 缓存未命中，查询数据库
        User user = this.getById(userId);
        if (user == null) {
            return null;
        }

        // 3. 转换为 UserVO 并写入缓存
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setPhone(maskPhone(user.getPhone()));
        setUserToCache(userId, userVO);

        // 4. 转换为 UserFeignVO 返回
        return toUserFeignVO(userVO);
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

    //=================缓存辅助方法=================

    private static final String USER_INFO_CACHE_PREFIX = "user:info:";
    private static final long USER_INFO_CACHE_TTL = 30;

    /**
     * 从 Redis 缓存中获取用户信息
     */
    private UserVO getUserFromCache(Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(USER_INFO_CACHE_PREFIX + userId);
            if (json != null) {
                return objectMapper.readValue(json, UserVO.class);
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化用户缓存失败: userId={}", userId, e);
        }
        return null;
    }

    /**
     * 将用户信息写入 Redis 缓存
     */
    private void setUserToCache(Long userId, UserVO userVO) {
        try {
            String json = objectMapper.writeValueAsString(userVO);
            stringRedisTemplate.opsForValue().set(
                    USER_INFO_CACHE_PREFIX + userId, json, USER_INFO_CACHE_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("序列化用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 删除 Redis 用户信息缓存
     */
    private void deleteUserCache(Long userId) {
        stringRedisTemplate.delete(USER_INFO_CACHE_PREFIX + userId);
    }

    /**
     * 将 UserVO 转换为 UserFeignVO
     */
    private UserFeignVO toUserFeignVO(UserVO userVO) {
        UserFeignVO vo = new UserFeignVO();
        vo.setUserId(userVO.getId());
        vo.setUsername(userVO.getUsername());
        vo.setPhone(userVO.getPhone());
        vo.setStatus(userVO.getStatus());
        return vo;
    }

    //=================其他辅助方法=================

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
     * 邮箱脱敏处理
     * <p>
     * 保留首字符和域名，中间替换为 ***，例如：u***@example.com
     * </p>
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (prefix.length() <= 1) {
            return prefix + "***" + domain;
        }
        return prefix.charAt(0) + "***" + domain;
    }

    /**
     * XSS 输入净化
     * <p>
     * 过滤 HTML 特殊字符，防止 XSS 攻击
     * </p>
     *
     * @param input 原始输入
     * @return 净化后的字符串
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 失效用户所有 Token
     * <p>
     * 用于密码修改、冻结用户等场景，立即踢出所有登录设备
     * </p>
     *
     * @param userId 用户ID
     */
    private void invalidateAllUserTokens(Long userId) {
        // 删除访问令牌
        stringRedisTemplate.delete("token:" + userId);
        // 删除所有设备的刷新令牌
        String deviceKey = "user:devices:" + userId;
        stringRedisTemplate.opsForSet().members(deviceKey).forEach(d -> {
            stringRedisTemplate.delete("refresh:token:" + userId + ":" + d);
        });
        stringRedisTemplate.delete(deviceKey);
        log.info("已失效用户所有 Token: userId={}", userId);
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