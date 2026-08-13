package com.smartlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartlink.mapper")
public class SmartlinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartlinkApplication.class, args);
    }
}
