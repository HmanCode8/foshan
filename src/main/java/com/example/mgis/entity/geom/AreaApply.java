package com.example.mgis.entity.geom;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("area_apply")
public class AreaApply {
    @JsonIgnore
    private Long userId;
    private String id;
    private String type;
    private String companyName;

    // 👇 把 LocalDateTime 改成 LocalDate ！！！
    private String applyDate;

    private String contactName;
    private String contactPhone;
    private BigDecimal totalAreaSqKm;
    private Integer segmentCount;
    private String status;
    private String remark;

    @TableField(exist = false)
    private List<AreaSegment> segments;
}