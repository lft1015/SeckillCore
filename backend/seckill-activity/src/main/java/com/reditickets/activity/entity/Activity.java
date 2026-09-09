package com.reditickets.activity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_activity")
@ApiModel(value = "Activity", description = "秒杀活动表")
public class Activity extends BaseEntity {

    @ApiModelProperty("活动名称")
    @TableField("activity_name")
    private String activityName;

    @ApiModelProperty("关联商品ID")
    @TableField("product_id")
    private Long productId;

    @ApiModelProperty("秒杀价（冗余）")
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    @ApiModelProperty("秒杀总限额")
    @TableField("total_limit")
    private Integer totalLimit;

    @ApiModelProperty("剩余限额")
    @TableField("remaining_limit")
    private Integer remainingLimit;

    @ApiModelProperty("活动开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    @ApiModelProperty("活动结束时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    @ApiModelProperty("每用户限购数量")
    @TableField("per_user_limit")
    private Integer perUserLimit;

    @ApiModelProperty("状态：0未开始/1进行中/2已结束/3已取消")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("乐观锁版本号（防超卖最后防线）")
    @TableField("version")
    @Version
    private Integer version;
}