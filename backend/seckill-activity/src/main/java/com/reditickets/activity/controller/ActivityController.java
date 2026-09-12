package com.reditickets.activity.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.service.ActivityService;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 活动控制层
 * <p>
 * 提供秒杀活动列表分页查询、活动详情查询和内部库存扣减等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@RestController
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
    @GetMapping("/api/v1/activity/list")
    public Result<Page<ActivityVO>> list(@Valid ActivityListDTO dto) {
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
    @GetMapping("/api/v1/activity/detail/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return activityService.getActivityById(id);
    }

    /**
     * 活动库存扣减接口（内部 Feign 调用）
     * <p>
     * 由秒杀模块调用，每次扣减 1 个库存单位，使用乐观锁保证并发安全
     * </p>
     *
     * @param body 请求体，包含 activityId
     * @return 扣减结果
     */
    @PostMapping("/api/v1/internal/activity/deductStock")
    public Result<Void> deductStock(@RequestBody Map<String, Long> body) {
        return activityService.deductRemainingStock(body.get("activityId"));
    }
}