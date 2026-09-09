package com.reditickets.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.entity.Activity;
import com.reditickets.activity.mapper.ActivityMapper;
import com.reditickets.activity.service.ActivityService;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 活动服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现活动分页查询、详情查询和库存扣减等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    /**
     * 分页查询活动列表实现
     * <p>
     * 步骤：构建分页对象 → LambdaQueryWrapper 条件查询 → 分页查询 → Page.convert() 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<Page<ActivityVO>> listActivities(ActivityListDTO dto) {
        // TODO: 1. 构建分页对象 Page<Activity> page = new Page<>(dto.getPage(), dto.getSize());
        // TODO: 2. 使用 LambdaQueryWrapper 构建查询条件
        //    LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        //    wrapper.eq(Activity::getIsDeleted, 0);
        //    if (dto.getStatus() != null) { wrapper.eq(Activity::getStatus, dto.getStatus()); }
        //    wrapper.orderByDesc(Activity::getCreateTime);
        // TODO: 3. 执行分页查询 this.page(page, wrapper)
        // TODO: 4. 使用 Page.convert() 将 Activity 转为 ActivityVO
        //    Page<ActivityVO> voPage = page.convert(activity -> {
        //        ActivityVO vo = new ActivityVO();
        //        BeanUtils.copyProperties(activity, vo);
        //        return vo;
        //    });
        // TODO: 5. 返回 Result.success(voPage)
        throw new UnsupportedOperationException("TODO: 实现活动列表查询");
    }

    /**
     * 活动详情查询实现
     * <p>
     * 步骤：查询活动 → 校验存在且未删除 → BeanUtils 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<ActivityVO> getActivityById(Long id) {
        // TODO: 1. 使用 this.getById(id) 查询活动
        //    Activity activity = this.getById(id);
        // TODO: 2. 校验活动是否存在且未被删除
        //    if (activity == null || activity.getIsDeleted() == 1) {
        //        return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        //    }
        // TODO: 3. 使用 BeanUtils.copyProperties 转换为 ActivityVO 返回
        //    ActivityVO vo = new ActivityVO();
        //    BeanUtils.copyProperties(activity, vo);
        //    return Result.success(vo);
        throw new UnsupportedOperationException("TODO: 实现活动详情查询");
    }

    /**
     * 活动库存扣减实现
     * <p>
     * 步骤：LambdaUpdateWrapper + version 乐观锁扣减 → 校验扣减结果 → 返回
     * </p>
     */
    @Override
    public Result<Void> deductRemainingStock(Long activityId) {
        // TODO: 1. 使用乐观锁扣减剩余库存（版本号 version 控制）
        //    LambdaUpdateWrapper<Activity> wrapper = new LambdaUpdateWrapper<>();
        //    wrapper.eq(Activity::getId, activityId)
        //           .gt(Activity::getRemainingStock, 0)
        //           .eq(Activity::getIsDeleted, 0);
        //    wrapper.setSql("remaining_stock = remaining_stock - 1");
        //    boolean success = this.update(wrapper);
        // TODO: 2. 校验扣减结果
        //    if (!success) { return Result.fail(ResultCode.STOCK_NOT_ENOUGH); }
        // TODO: 3. 返回成功
        //    return Result.success();
        throw new UnsupportedOperationException("TODO: 实现活动库存扣减");
    }
}