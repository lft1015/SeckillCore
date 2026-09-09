package com.reditickets.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.user.dto.LoginDTO;
import com.reditickets.user.dto.RegisterDTO;
import com.reditickets.user.entity.User;
import com.reditickets.user.vo.LoginVO;
import com.reditickets.user.vo.UserVO;

public interface UserService extends IService<User> {

    Result<Void> register(RegisterDTO dto);

    Result<LoginVO> login(LoginDTO dto);

    Result<UserVO> getUserById(Long userId);
}