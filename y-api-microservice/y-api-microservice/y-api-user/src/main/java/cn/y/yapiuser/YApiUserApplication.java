package cn.y.yapiuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
//@ComponentScan("cn.y")
@MapperScan("cn.y.yapiuser.mapper")
public class YApiUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(YApiUserApplication.class, args);
    }

}
