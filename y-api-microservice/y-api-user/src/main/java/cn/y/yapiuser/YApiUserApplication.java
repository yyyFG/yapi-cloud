package cn.y.yapiuser;

import cn.y.yapicommon.aop.AuthInterceptor;
import cn.y.yapicommon.aop.LogInterceptor;
import cn.y.yapicommon.cache.CaffRedisCacheConfig;
import cn.y.yapicommon.cache.LocalCacheConfig;
import cn.y.yapicommon.config.MyBatisPlusConfig;
import cn.y.yapicommon.cache.RedisCacheManagerConfig;
import cn.y.yapicommon.config.SessionSerializerConfig;
import cn.y.yapicommon.exception.GlobalExceptionHandler;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDubbo
@MapperScan("cn.y.yapiuser.mapper")
@Import({GlobalExceptionHandler.class, AuthInterceptor.class, LogInterceptor.class,
        MyBatisPlusConfig.class, SessionSerializerConfig.class, LocalCacheConfig.class,
        RedisCacheManagerConfig.class, CaffRedisCacheConfig.class})
public class YApiUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiUserApplication.class, args);
    }

}
