package com.reditickets.interfaces.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.activity.dto.ActivityListDTO;
import com.reditickets.activity.vo.ActivityVO;
import com.reditickets.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 活动服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 seckill-activity 微服务
 * </p>
 *
 * @author gugu
 */
@FeignClient(name = "seckill-activity", url = "http://localhost:8080", path = "/api/v1/activity")
public interface ActivityFeignClient {

    @GetMapping("/list")
    Result<Page<ActivityVO>> list(@SpringQueryMap ActivityListDTO dto);

    @GetMapping("/detail/{id}")
    Result<ActivityVO> detail(@PathVariable("id") Long id);
}