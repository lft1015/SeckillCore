package com.reditickets.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.entity.Activity;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;

public interface ActivityService extends IService<Activity> {

    Result<Page<ActivityVO>> listActivities(ActivityListDTO dto);

    Result<ActivityVO> getActivityById(Long id);

    Result<Void> deductRemainingStock(Long activityId);
}