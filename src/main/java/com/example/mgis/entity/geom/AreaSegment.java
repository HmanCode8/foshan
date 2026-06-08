package com.example.mgis.entity.geom;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("area_segment")
public class AreaSegment {
    @JsonIgnore
    private Long userId;
    private String id;
    private String applyId;
    private String auditType;
    private String name;
    private String code;
    private String areaCode;
    private String areaName;
    private String streetCode;
    private String streetName;
    private String type;

    // ✅ 改成 Object，接收数组/字符串都不会报错
    private Object coords;

    private BigDecimal areaSqKm;
}