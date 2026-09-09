package com.reditickets.activity.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.service.ActivityService;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/list")
    public Result<Page<ActivityVO>> list(ActivityListDTO dto) {
        return activityService.listActivities(dto);
    }

    @GetMapping("/detail/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return activityService.getActivityById(id);
    }
}