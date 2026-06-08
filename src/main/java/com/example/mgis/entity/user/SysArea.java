package com.example.mgis.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_area")
public class SysArea {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区域编码 */
    private String areaCode;
    /** 区域名称 */
    private String areaName;
    /** 父级编码，0 代表顶级城市 */
    private String parentCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /**
     * 下属街道集合
     * 数据库无此字段，仅内存组装、返回前端使用
     */
    @TableField(exist = false)
    private List<SysStreet> streets;
}