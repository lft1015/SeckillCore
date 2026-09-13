package com.reditickets.interfaces.vo;

import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.product.vo.ProductVO;
import lombok.Data;

/**
 * 秒杀首页聚合视图对象
 * <p>
 * 将秒杀活动、商品信息聚合为一个视图，减少前端请求次数
 * </p>
 *
 * @author gugu
 */
@Data
public class SeckillPageVO {

    /** 秒杀活动信息 */
    private ActivityVO activity;

    /** 商品信息 */
    private ProductVO product;

    /** 用户是否可秒杀（已登录且活动进行中） */
    private Boolean canSeckill;
}