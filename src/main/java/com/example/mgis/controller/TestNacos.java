package com.example.mgis.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class TestNacos {
    @Value("${test.msg:暂无数据12}")
    private String msg;

    @GetMapping("/msg")
    public String getMsg(){
        return msg;
    }
}
