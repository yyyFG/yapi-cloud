package cn.y.yapiinterface;

import cn.y.yapicommon.aop.AuthInterceptor;
import cn.y.yapicommon.aop.LogInterceptor;
import cn.y.yapicommon.exception.GlobalExceptionHandler;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDubbo
@MapperScan("cn.y.yapiinterface.mapper")
@Import({GlobalExceptionHandler.class, AuthInterceptor.class, LogInterceptor.class})
public class YApiInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiInterfaceApplication.class, args);
    }

}
