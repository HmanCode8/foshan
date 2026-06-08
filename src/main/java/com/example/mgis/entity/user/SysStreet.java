package com.example.mgis.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_street")
public class SysStreet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属区级编码，关联 sys_area.areaCode */
    private String areaCode;
    /** 街道编码 */
    private String streetCode;
    /** 街道名称 */
    private String streetName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}