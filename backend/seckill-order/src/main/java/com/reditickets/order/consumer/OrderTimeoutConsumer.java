package com.reditickets.order.consumer;

import com.reditickets.order.entity.Order;
import com.reditickets.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时取消 RocketMQ 消费者
 * <p>
 * 监听 order-delay-topic，消费延时消息执行超时取消逻辑。
 * 创建订单时投递 15 分钟延时消息到此 topic，到期后自动取消未支付订单
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-delay-topic",
        consumerGroup = "order-timeout-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<String> {

    private final OrderService orderService;

    public OrderTimeoutConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void onMessage(String message) {
        log.info("[超时取消] 收到超时消息: {}", message);

        Long orderId;
        try {
            orderId = Long.parseLong(message);
        } catch (NumberFormatException e) {
            log.error("[超时取消] 消息格式错误，无法解析订单ID: {}", message);
            return;
        }

        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                log.warn("[超时取消] 订单不存在: orderId={}", orderId);
                return;
            }

            if (order.getOrderStatus() != 0) {
                log.info("[超时取消] 订单已处理，跳过: orderId={}, status={}", orderId, order.getOrderStatus());
                return;
            }

            if (order.getExpireTime() != null && order.getExpireTime().isAfter(LocalDateTime.now())) {
                log.info("[超时取消] 订单尚未过期，跳过: orderId={}, expireTime={}", orderId, order.getExpireTime());
                return;
            }

            orderService.cancelOrder(orderId);

        } catch (Exception e) {
            log.error("[超时取消] 取消处理异常: orderId={}", orderId, e);
            throw e;
        }
    }
}