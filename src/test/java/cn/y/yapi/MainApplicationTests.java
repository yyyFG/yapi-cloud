package cn.y.yapi;


import cn.y.yapiclientsdk.client.YApiClient;
import cn.y.yapiclientsdk.model.InterfaceInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SDK 调用接口测试
 * 不需要启动 Spring 上下文，直接发 HTTP 请求到网关，秒级运行
 */
class MainApplicationTests {

    private static final String ACCESS_KEY = "a6b072c7a80671c2c1e0a9fae4da16e8";
    private static final String SECRET_KEY = "152120b4bcab38863c3dfec04af96e29";
    // 对外 path：网关地址 + 接口表中的 path 字段
    private static final String GATEWAY_URL = "/api/u2092123055068327938/api/name/get";

    @Test
    void testInvokeInterfaceBySdk() {
        String result = invokeOnce();
        System.out.println(result);
        Assertions.assertNotNull(result);
    }

    /**
     * 并发调用测试限流：
     * 网关用户级限流为 2 次/秒，10 个并发里预期约 2 个成功、其余被 429 拒绝
     */
    @Test
    void testRateLimitReject() throws InterruptedException {
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rateLimitCount = new AtomicInteger();
        AtomicInteger otherFailCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    String result = invokeOnce();
                    successCount.incrementAndGet();
                    System.out.println("调用成功: " + result);
                } catch (RuntimeException e) {
                    // SDK 对非 2xx 响应抛异常，消息里带状态码，429 即被限流
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        rateLimitCount.incrementAndGet();
                        System.out.println("被限流: " + e.getMessage());
                    } else {
                        otherFailCount.incrementAndGet();
                        System.out.println("其他失败: " + e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        System.out.println("成功: " + successCount.get() + "，被限流: " + rateLimitCount.get() + "，其他失败: " + otherFailCount.get());
        Assertions.assertTrue(successCount.get() > 0, "预期至少部分请求成功");
        Assertions.assertTrue(rateLimitCount.get() > 0, "预期存在被限流(429)的请求");
        Assertions.assertEquals(0, otherFailCount.get(), "存在非限流原因的失败");
    }

    private String invokeOnce() {
        YApiClient yApiClient = new YApiClient(ACCESS_KEY, SECRET_KEY);
        InterfaceInfo info = new InterfaceInfo();
        info.setUrl(GATEWAY_URL);
        info.setMethod("GET");
        info.setRequestParams("name=test");
        return yApiClient.invokeInterface(info);
    }

}
