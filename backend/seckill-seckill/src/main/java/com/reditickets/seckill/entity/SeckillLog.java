package com.reditickets.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_seckill_log")
@ApiModel(value = "SeckillLog", description = "秒杀日志表")
public class SeckillLog extends BaseEntity {

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty("商品ID")
    @TableField("product_id")
    private Long productId;

    @ApiModelProperty("活动ID")
    @TableField("activity_id")
    private Long activityId;

    @ApiModelProperty("状态：0待处理/1已下单/2已失败/3已取消")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("失败原因")
    @TableField("fail_reason")
    private String failReason;

    @ApiModelProperty("抢购时间")
    @TableField("seckill_time")
    private LocalDateTime seckillTime;
}