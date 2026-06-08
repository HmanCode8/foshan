package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.AreaSimpleVO;
import com.example.mgis.entity.user.FoshanAreaVO;
import com.example.mgis.service.user.AreaService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/districts")
public class AreaController {

    @Resource
    private AreaService areaService;

    /**
     * 完整三级结构：市 -> 区 -> 街道
     * 地址：GET /districts
     */
    @GetMapping
    public Result<FoshanAreaVO> getDistricts() {
        FoshanAreaVO vo = areaService.getFoshanAreaData();
        return Result.success(vo);
    }

    /**
     * 新增：仅查询一级区域（所有区，只返回编码+名称）
     * 地址：GET /districts/first
     */
    @GetMapping("/first")
    public Result<List<AreaSimpleVO>> getFirstLevelArea() {
        List<AreaSimpleVO> list = areaService.getFirstLevelArea();
        return Result.success(list);
    }
}