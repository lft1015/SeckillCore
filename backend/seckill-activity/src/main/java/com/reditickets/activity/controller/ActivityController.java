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

/**
 * 活动控制层
 * <p>
 * 提供秒杀活动列表分页查询和活动详情查询等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 活动列表分页查询接口
     * <p>
     * 支持按活动状态筛选，返回分页后的活动列表
     * </p>
     *
     * @param dto 分页查询参数（页码、每页条数、状态）
     * @return 分页活动列表
     */
    @GetMapping("/list")
    public Result<Page<ActivityVO>> list(ActivityListDTO dto) {
        return activityService.listActivities(dto);
    }

    /**
     * 活动详情查询接口
     * <p>
     * 根据活动ID查询活动详细信息，包含秒杀价格、库存、时间范围等
     * </p>
     *
     * @param id 活动ID
     * @return 活动详情视图对象
     */
    @GetMapping("/detail/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return activityService.getActivityById(id);
    }
}