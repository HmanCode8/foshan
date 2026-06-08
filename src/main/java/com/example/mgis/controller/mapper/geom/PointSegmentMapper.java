package com.example.mgis.controller.mapper.geom;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.geom.PointSegment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PointSegmentMapper extends BaseMapper<PointSegment> {

    @Insert("<script>" +
            "INSERT INTO point_segment(id,apply_id,audit_type,name,code,area_code,area_name,street_code,street_name,type,coords) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id},#{item.applyId},#{item.auditType},#{item.name},#{item.code},#{item.areaCode},#{item.areaName},#{item.streetCode},#{item.streetName},#{item.type},#{item.coords})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<PointSegment> list);
}