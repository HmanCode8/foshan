package com.example.mgis.entity.geom;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
@Data
@TableName("point_apply")
public class PointApply {
    @JsonIgnore
    private Long userId;
    private String id;
    private String type;
    private String companyName;
    private String applyDate;
    private String contactName;
    private String contactPhone;
    private Integer totalCount;
    private Integer segmentCount;
    private String status;
    private String remark;

    @TableField(exist = false)
    private List<PointSegment> segments;
}
