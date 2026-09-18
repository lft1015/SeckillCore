package com.reditickets.interfaces.feign;

import com.reditickets.common.result.Result;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.vo.SeckillResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 秒杀服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 seckill-seckill 微服务
 * </p>
 *
 * @author gugu
 */
@FeignClient(name = "seckill-seckill", url = "${seckill-seckill.url:http://localhost:8080}")
public interface SeckillFeignClient {

    @PostMapping("/api/seckill/execute")
    Result<SeckillResultVO> execute(@RequestBody SeckillExecuteDTO dto);

    @GetMapping("/api/seckill/result/{seckillLogId}")
    Result<SeckillResultVO> result(@PathVariable("seckillLogId") Long seckillLogId);
}