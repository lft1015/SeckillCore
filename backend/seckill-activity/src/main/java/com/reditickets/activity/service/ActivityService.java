package com.reditickets.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.entity.Activity;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;

/**
 * 活动服务接口
 * <p>
 * 继承 MyBatis Plus IService 接口，提供秒杀活动的查询和库存扣减等业务逻辑定义
 * </p>
 *
 * @author gugu
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 分页查询活动列表
     *
     * @param dto 分页查询参数
     * @return 分页活动列表
     */
    Result<Page<ActivityVO>> listActivities(ActivityListDTO dto);

    /**
     * 根据活动ID查询活动详情
     *
     * @param id 活动ID
     * @return 活动详情视图对象
     */
    Result<ActivityVO> getActivityById(Long id);

    /**
     * 扣减活动剩余库存
     * <p>
     * 使用乐观锁（version 字段）保证并发安全，每次秒杀成功扣减 1 个库存
     * </p>
     *
     * @param activityId 活动ID
     * @return 扣减结果
     */
    Result<Void> deductRemainingStock(Long activityId);
}