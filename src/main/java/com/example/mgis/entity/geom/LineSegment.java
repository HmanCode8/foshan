package com.example.mgis.entity.geom;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("line_segment")
public class LineSegment {
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

    // ✅ 统一用 Object
    private Object coords;

    private BigDecimal lengthKm;
}