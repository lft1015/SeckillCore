package com.reditickets.common.controller;

import com.reditickets.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BackendInfoController {

    @GetMapping("/")
    public Result<Map<String, String>> info() {
        return Result.success(Map.of(
                "service", "SeckillCore backend",
                "status", "UP"
        ));
    }
}
