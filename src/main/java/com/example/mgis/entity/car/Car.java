package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@TableName("car")
public class Car {
    @JsonIgnore
    private Long userId;
    private String id;
    private String plateNo;
    private String type;
    private String status;
    private String category;
    private String region;
    private String enterprise;
    private String photo;
    private String color;
    private String startTime;
    private String endTime;
    private String statusText;
    private Date createTime;
    private Date updateTime;

    // 关联对象
    @TableField(exist = false)
    private CarSecurity securityInfo;

    @TableField(exist = false)
    private CarTerminal terminalInfo;

    @TableField(exist = false)
    private Map<String, Object> history;

    @TableField(exist = false)
    private List<List<Double>> actualRoute;

    @TableField(exist = false)
    private List<List<Double>> plannedRoute;
}
