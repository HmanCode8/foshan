package com.example.mgis.entity.geom;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("point_segment")
public class PointSegment {
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
}