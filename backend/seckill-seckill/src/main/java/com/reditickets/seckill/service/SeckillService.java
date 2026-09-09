package com.reditickets.seckill.service;

import com.reditickets.common.result.Result;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.vo.SeckillResultVO;

public interface SeckillService {

    Result<SeckillResultVO> executeSeckill(SeckillExecuteDTO dto);

    Result<SeckillResultVO> getSeckillResult(Long orderId);
}