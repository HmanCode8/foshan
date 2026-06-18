package com.example.mgis.entity.chart.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendApplyVO{
    private String applyUser;
    private LocalDateTime createTime;
}