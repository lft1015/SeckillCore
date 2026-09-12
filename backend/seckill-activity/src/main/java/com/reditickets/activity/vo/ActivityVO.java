package com.reditickets.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动视图对象（VO）
 * <p>
 * 用于接口返回秒杀活动信息，包含秒杀价、库存、时间范围等完整字段
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

    /** 秒杀总限额 */
    private Integer totalLimit;

    /** 剩余限额 */
    private Integer remainingLimit;

    /** 每用户限购数量 */
    private Integer perUserLimit;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态：0=未开始，1=进行中，2=已结束，3=已取消 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}