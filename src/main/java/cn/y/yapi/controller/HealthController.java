package cn.y.yapi.controller;

import cn.y.yapi.common.BaseResponse;
import cn.y.yapi.common.ErrorCode;
import cn.y.yapi.common.ResultUtils;
import cn.y.yapi.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查
 */
@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    /**
     * 健康检查：返回服务状态和 JVM 运行信息
     *
     * @return
     */
    @GetMapping
    public BaseResponse<Map<String, Object>> health() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("totalMemory", runtime.totalMemory());
        info.put("freeMemory", runtime.freeMemory());
        info.put("maxMemory", runtime.maxMemory());
        log.info("health check, status: UP");
        return ResultUtils.success(info);
    }

    /**
     * 回显接口：返回收到的 query 参数和请求头（测试 GET 参数、自定义请求头）
     *
     * @param request
     * @return
     */
    @GetMapping("/echo")
    public BaseResponse<Map<String, Object>> echoGet(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, String.join(",", values)));
        result.put("params", params);
        result.put("headers", getHeaders(request));
        return ResultUtils.success(result);
    }

    /**
     * 回显接口：原样返回收到的 JSON 请求体和请求头（测试 POST 参数）
     *
     * @param body
     * @param request
     * @return
     */
    @PostMapping("/echo")
    public BaseResponse<Map<String, Object>> echoPost(@RequestBody Object body, HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("body", body);
        result.put("headers", getHeaders(request));
        return ResultUtils.success(result);
    }

    /**
     * 模拟耗时接口（测试调用超时场景）：?ms=2000，最大 10 秒
     *
     * @param ms
     * @return
     */
    @GetMapping("/sleep")
    public BaseResponse<String> sleep(@RequestParam(defaultValue = "1000") long ms) {
        if (ms > 10000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大只能模拟 10 秒");
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ResultUtils.success("sleep " + ms + " ms");
    }

    /**
     * 业务异常测试接口：测试全局异常处理器的 BusinessException 分支
     *
     * @return
     */
    @GetMapping("/business-error")
    public BaseResponse<String> businessError() {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "这是一个业务异常测试");
    }

    /**
     * 系统异常测试接口：测试全局异常处理器的 RuntimeException 分支
     *
     * @return
     */
    @GetMapping("/system-error")
    public BaseResponse<String> systemError() {
        throw new RuntimeException("这是一个系统异常测试");
    }

    /**
     * 获取请求头 map
     *
     * @param request
     * @return
     */
    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
