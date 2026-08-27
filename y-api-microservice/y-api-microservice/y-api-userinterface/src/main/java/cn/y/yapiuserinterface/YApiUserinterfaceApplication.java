package cn.y.yapiuserinterface;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("cn.y.yapiuserinterface.mapper")
public class YApiUserinterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiUserinterfaceApplication.class, args);
    }

}
