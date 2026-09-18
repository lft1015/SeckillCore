package com.reditickets.interfaces.feign;

import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RefreshTokenDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.dto.UpdatePasswordDTO;
import com.reditickets.user.dto.UpdateUserInfoDTO;
import com.reditickets.user.dto.UpdateUserStatusDTO;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 seckill-user 微服务
 * </p>
 *
 * @author gugu
 */
@FeignClient(name = "seckill-user", url = "${seckill-user.url:http://localhost:8080}")
public interface UserFeignClient {

    @PostMapping("/api/v1/users")
    Result<Void> register(@RequestBody RegisterDTO dto);

    @GetMapping("/api/v1/users")
    Result<List<UserVO>> listUsers(@RequestParam("page") Integer page,
                                   @RequestParam("size") Integer size);

    @GetMapping("/api/v1/users/me")
    Result<UserVO> getCurrentUserInfo();

    @PutMapping("/api/v1/users/me")
    Result<UserVO> updateUserInfo(@RequestBody UpdateUserInfoDTO dto);

    @PutMapping("/api/v1/users/me/password")
    Result<Void> updatePassword(@RequestBody UpdatePasswordDTO dto);

    @PutMapping("/api/v1/users/{userId}/status")
    Result<Void> updateUserStatus(@PathVariable("userId") Long userId,
                                  @RequestBody UpdateUserStatusDTO dto);

    @PostMapping("/api/v1/auth/login")
    Result<LoginVO> login(@RequestBody LoginDTO dto);

    @DeleteMapping("/api/v1/auth/session")
    Result<Void> logout();

    @PostMapping("/api/v1/auth/refresh")
    Result<LoginVO> refreshToken(@RequestBody RefreshTokenDTO dto);
}