package cn.y.yapiinterface;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("cn.y.yapiinterface.mapper")
public class YApiInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiInterfaceApplication.class, args);
    }

}
