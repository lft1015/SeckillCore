package com.reditickets.seckill.controller;

import com.reditickets.common.result.Result;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.service.SeckillService;
import com.reditickets.seckill.vo.SeckillResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/execute")
    public Result<SeckillResultVO> execute(@Valid @RequestBody SeckillExecuteDTO dto) {
        return seckillService.executeSeckill(dto);
    }

    @GetMapping("/result/{orderId}")
    public Result<SeckillResultVO> result(@PathVariable Long orderId) {
        return seckillService.getSeckillResult(orderId);
    }
}