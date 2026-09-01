package cn.y.yapiuserinterface;

import cn.y.yapicommon.aop.AuthInterceptor;
import cn.y.yapicommon.aop.LogInterceptor;
import cn.y.yapicommon.config.MyBatisPlusConfig;
import cn.y.yapicommon.exception.GlobalExceptionHandler;
import cn.y.yapicommon.ratelimit.config.RedissonConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDubbo
@MapperScan("cn.y.yapiuserinterface.mapper")
@Import({GlobalExceptionHandler.class, AuthInterceptor.class, LogInterceptor.class,
        MyBatisPlusConfig.class, RedissonConfig.class})
public class YApiUserinterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiUserinterfaceApplication.class, args);
    }

}
