package com.example.mgis.controller.mapper.geom;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.geom.LineSegment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LineSegmentMapper extends BaseMapper<LineSegment> {

    @Insert("<script>" +
            "INSERT INTO line_segment(id,apply_id,name,code,area_code,area_name,street_code,street_name,type,coords,length_km) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id},#{item.applyId},#{item.name},#{item.code},#{item.areaCode},#{item.areaName},#{item.streetCode},#{item.streetName},#{item.type},#{item.coords},#{item.lengthKm})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<LineSegment> list);
}