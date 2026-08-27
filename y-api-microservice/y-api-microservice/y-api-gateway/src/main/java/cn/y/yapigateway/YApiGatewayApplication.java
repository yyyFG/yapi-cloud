package cn.y.yapigateway;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;


// 排除数据源自动配置：gateway 不连数据库，但 y-api-common 传递引入的 spring-jdbc
// 会触发 DataSourceAutoConfiguration，缺少 datasource 配置会导致启动失败
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Slf4j
@EnableDiscoveryClient
public class YApiGatewayApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(YApiGatewayApplication.class, args);
        Environment env= run.getEnvironment();
        log.info("\n" +
                        "-------------------------------------------------\n" +
                        "    Application is running! Access URLs:\n" +
                        "    Local:    http://localhost:{}\n" +
                        "    Doc:      http://localhost:{}/doc.html\n" +
                        "-------------------------------------------------",
                env.getProperty("server.port"),
                env.getProperty("server.port")
        );
    }
}
