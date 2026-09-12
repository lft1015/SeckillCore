package com.reditickets.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.entity.Activity;
import com.reditickets.activity.mapper.ActivityMapper;
import com.reditickets.activity.service.ActivityService;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 活动服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现活动分页查询、详情查询（含 Redis 缓存）和库存扣减（乐观锁）等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final String INFO_CACHE_KEY = "activity:info:";
    private static final String STOCK_CACHE_KEY = "activity:stock:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询活动列表实现
     * <p>
     * 步骤：构建分页对象 → LambdaQueryWrapper 条件查询 → 分页查询 → Page.convert() 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<Page<ActivityVO>> listActivities(ActivityListDTO dto) {
        Page<Activity> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getIsDeleted, 0);
        if (dto.getStatus() != null) {
            wrapper.eq(Activity::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(Activity::getCreateTime);

        this.page(page, wrapper);

        Page<ActivityVO> voPage = (Page<ActivityVO>) page.convert(activity -> {
            ActivityVO vo = new ActivityVO();
            copyToVO(activity, vo);
            return vo;
        });

        return Result.success(voPage);
    }

    /**
     * 活动详情查询实现
     * <p>
     * 步骤：查 Redis 缓存 → 命中则返回 → 未命中则查库 → 回写缓存 → 动态计算状态 → 返回
     * </p>
     */
    @Override
    public Result<ActivityVO> getActivityById(Long id) {
        String cacheKey = INFO_CACHE_KEY + id;

        ActivityVO cached = getFromCache(cacheKey);
        if (cached != null) {
            return Result.success(cached);
        }

        Activity activity = this.getById(id);
        if (activity == null || activity.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        }

        ActivityVO vo = new ActivityVO();
        copyToVO(activity, vo);
        recalculateStatus(activity, vo);

        setToCache(cacheKey, vo);

        return Result.success(vo);
    }

    /**
     * 活动库存扣减实现
     * <p>
     * 步骤：查活动获取当前版本号 → 校验活动状态和时间窗口 → updateById 触发 @Version 乐观锁 → 校验扣减结果 → 同步 Redis → 返回
     * </p>
     */
    @Override
    public Result<Void> deductRemainingStock(Long activityId) {
        Activity activity = this.getById(activityId);
        if (activity == null || activity.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        }

        if (activity.getStatus() != 1) {
            if (activity.getStatus() == 0) {
                return Result.fail(ResultCode.ACTIVITY_NOT_STARTED);
            }
            return Result.fail(ResultCode.ACTIVITY_ENDED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return Result.fail(ResultCode.ACTIVITY_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndTime())) {
            return Result.fail(ResultCode.ACTIVITY_ENDED);
        }

        if (activity.getRemainingLimit() < 1) {
            return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
        }

        activity.setRemainingLimit(activity.getRemainingLimit() - 1);
        if (activity.getRemainingLimit() == 0) {
            activity.setStatus(2);
        }

        boolean success = this.updateById(activity);
        if (!success) {
            return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
        }

        stringRedisTemplate.opsForValue().decrement(STOCK_CACHE_KEY + activityId);

        return Result.success();
    }

    private void copyToVO(Activity activity, ActivityVO vo) {
        vo.setId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setProductId(activity.getProductId());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setTotalLimit(activity.getTotalLimit());
        vo.setRemainingLimit(activity.getRemainingLimit());
        vo.setPerUserLimit(activity.getPerUserLimit());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setCreateTime(activity.getCreateTime());
        vo.setUpdateTime(activity.getUpdateTime());
    }

    private void recalculateStatus(Activity activity, ActivityVO vo) {
        if (activity.getStatus() == 3) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() == 0 && now.isAfter(activity.getStartTime()) && now.isBefore(activity.getEndTime())) {
            vo.setStatus(1);
        } else if (activity.getStatus() == 1 && now.isAfter(activity.getEndTime())) {
            vo.setStatus(2);
        }
    }

    private ActivityVO getFromCache(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, ActivityVO.class);
            }
        } catch (JsonProcessingException e) {
            log.error("活动缓存反序列化失败 key={}", cacheKey, e);
        }
        return null;
    }

    private void setToCache(String cacheKey, ActivityVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.error("活动缓存序列化失败 key={}", cacheKey, e);
        }
    }
}