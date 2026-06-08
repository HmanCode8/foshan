package com.example.mgis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.example.mgis.**.mapper") // 改成这个！
@EnableDiscoveryClient
public class MgisApplication {
    public static void main(String[] args) {
        SpringApplication.run(MgisApplication.class, args);
    }
}
