package com.example.mgis.entity.geom;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class AuditDTO {
    @JsonIgnore
    private Long userId;
    private String id;
    private String status;
    private String remark;
}
