package com.smartlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 鱼快创领 - 客服中心产品自动化平台
 * 启动类
 *
 * @author smartlink
 */
@SpringBootApplication
@MapperScan("com.smartlink.mapper")
public class SmartlinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartlinkApplication.class, args);
    }
}
