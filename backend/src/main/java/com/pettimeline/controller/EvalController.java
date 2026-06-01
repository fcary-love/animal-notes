package com.pettimeline.controller;

import com.pettimeline.ai.eval.EvalReport;
import com.pettimeline.ai.eval.EvalService;
import com.pettimeline.model.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;

    @PostMapping("/run")
    public ApiResponse<EvalReport> run() {
        long start = System.currentTimeMillis();
        EvalReport report = evalService.runFullEval();
        long elapsed = System.currentTimeMillis() - start;
        var result = ApiResponse.ok(report);
        result.setMessage("评估完成，耗时 " + elapsed + "ms");
        return result;
    }
}
