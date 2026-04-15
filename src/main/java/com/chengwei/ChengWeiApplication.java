package com.chengwei;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.chengwei.mapper")
@SpringBootApplication
//暴露代理对象
@EnableAspectJAutoProxy(exposeProxy =true)
public class ChengWeiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChengWeiApplication.class, args);
    }

}
