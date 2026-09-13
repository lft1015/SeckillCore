package com.reditickets.interfaces.controller;

import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.interfaces.feign.ActivityFeignClient;
import com.reditickets.interfaces.feign.ProductFeignClient;
import com.reditickets.interfaces.vo.SeckillPageVO;
import com.reditickets.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀页面聚合控制器（BFF 层）
 * <p>
 * 聚合活动服务、商品服务的数据，为前端提供一站式秒杀页面数据，
 * 减少前端请求次数，降低网络开销
 * </p>
 *
 * @author gugu
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
public class SeckillPageController {

    private final ActivityFeignClient activityFeignClient;
    private final ProductFeignClient productFeignClient;

    /**
     * 秒杀活动详情页聚合接口
     * <p>
     * 一次请求同时获取活动信息 + 关联商品信息，
     * 前端只需调用此接口即可渲染完整的秒杀商品详情页
     * </p>
     *
     * @param activityId 活动ID
     * @param userId 用户ID（由网关 JWT 过滤器注入 X-User-Id）
     * @return 聚合后的秒杀页面视图对象
     */
    @GetMapping("/seckill-page/{activityId}")
    public Result<SeckillPageVO> seckillPage(@PathVariable Long activityId,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Result<ActivityVO> activityResult = activityFeignClient.detail(activityId);
        if (activityResult.getCode() != ResultCode.SUCCESS.getCode() || activityResult.getData() == null) {
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        }

        ActivityVO activity = activityResult.getData();
        Long productId = activity.getProductId();
        if (productId == null) {
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND.getCode(), "活动未关联商品");
        }

        Result<ProductVO> productResult = productFeignClient.detail(productId);
        if (productResult.getCode() != ResultCode.SUCCESS.getCode() || productResult.getData() == null) {
            return Result.fail(ResultCode.PRODUCT_NOT_FOUND);
        }

        SeckillPageVO vo = new SeckillPageVO();
        vo.setActivity(activity);
        vo.setProduct(productResult.getData());
        vo.setCanSeckill(userId != null && activity.getStatus() != null && activity.getStatus() == 1);

        return Result.success(vo);
    }
}