package cn.y.yapi;


import cn.y.yapiclientsdk.client.YApiClient;
import cn.y.yapiclientsdk.model.InterfaceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主类测试
 */
@SpringBootTest
class MainApplicationTests {

    @Test
    void contextLoads() {
        YApiClient yApiClient = new YApiClient("a6b072c7a80671c2c1e0a9fae4da16e8", "152120b4bcab38863c3dfec04af96e29");
        InterfaceInfo info = new InterfaceInfo();
        info.setUrl("/api/u2092123055068327938/api/name/get");  // 对外 path
        info.setMethod("GET");
        info.setRequestParams("name=test");                      // 业务参数

        String result = yApiClient.invokeInterface(info);
        System.out.println(result);
    }

}
