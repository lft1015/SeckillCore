package com.reditickets.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动视图对象（VO）
 * <p>
 * 用于接口返回秒杀活动信息，包含商品ID、秒杀价、库存、时间范围等完整字段
 * </p>
 *
 * @author gugu
 */
@Data
public class ActivityVO {

    /** 活动ID */
    private Long id;

    /** 活动名称 */
    private String activityName;

    /** 关联商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 秒杀总库存 */
    private Integer seckillStock;

    /** 剩余库存 */
    private Integer remainingStock;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态：0=未开始，1=进行中，2=已结束 */
    private Integer status;

    /** 乐观锁版本号 */
    private Long version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}