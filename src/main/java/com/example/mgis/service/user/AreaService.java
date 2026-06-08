package com.example.mgis.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mgis.controller.mapper.user.SysAreaMapper;
import com.example.mgis.controller.mapper.user.SysStreetMapper;
import com.example.mgis.entity.user.AreaSimpleVO;
import com.example.mgis.entity.user.FoshanAreaVO;
import com.example.mgis.entity.user.SysArea;
import com.example.mgis.entity.user.SysStreet;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AreaService {

    @Resource
    private SysAreaMapper areaMapper;

    @Resource
    private SysStreetMapper streetMapper;

    // 原有：完整三级结构接口方法
    public FoshanAreaVO getFoshanAreaData() {
        SysArea city = areaMapper.selectOne(
                new LambdaQueryWrapper<SysArea>()
                        .eq(SysArea::getParentCode, "0")
                        .eq(SysArea::getAreaCode, "440600")
        );
        if (city == null) {
            return null;
        }

        List<SysArea> districtList = areaMapper.selectList(
                new LambdaQueryWrapper<SysArea>()
                        .eq(SysArea::getParentCode, city.getAreaCode())
        );

        List<SysStreet> streetList = streetMapper.selectList(null);
        Map<String, List<SysStreet>> streetMap = streetList.stream()
                .collect(Collectors.groupingBy(SysStreet::getAreaCode));

        FoshanAreaVO result = new FoshanAreaVO();
        result.setCityCode(city.getAreaCode());
        result.setCityName(city.getAreaName());

        List<FoshanAreaVO.DistrictVO> districtVOList = districtList.stream().map(dist -> {
            FoshanAreaVO.DistrictVO districtVO = new FoshanAreaVO.DistrictVO();
            districtVO.setAreaCode(dist.getAreaCode());
            districtVO.setAreaName(dist.getAreaName());

            List<FoshanAreaVO.StreetVO> streetVOList = streetMap.getOrDefault(dist.getAreaCode(), List.of())
                    .stream()
                    .map(street -> {
                        FoshanAreaVO.StreetVO streetVO = new FoshanAreaVO.StreetVO();
                        streetVO.setCode(street.getStreetCode());
                        streetVO.setName(street.getStreetName());
                        return streetVO;
                    }).collect(Collectors.toList());

            districtVO.setStreets(streetVOList);
            return districtVO;
        }).collect(Collectors.toList());

        result.setDistricts(districtVOList);
        return result;
    }

    /**
     * 新增：查询佛山所有一级区域（市下所有区，仅编码+名称）
     */
    public List<AreaSimpleVO> getFirstLevelArea() {
        // 先查佛山市编码
        SysArea city = areaMapper.selectOne(
                new LambdaQueryWrapper<SysArea>()
                        .eq(SysArea::getParentCode, "0")
                        .eq(SysArea::getAreaCode, "440600")
        );
        if (city == null) {
            return List.of();
        }

        // 查询该市下所有区
        List<SysArea> areaList = areaMapper.selectList(
                new LambdaQueryWrapper<SysArea>()
                        .eq(SysArea::getParentCode, city.getAreaCode())
        );

        // 转换为简单VO
        return areaList.stream()
                .map(area -> {
                    AreaSimpleVO vo = new AreaSimpleVO();
                    vo.setAreaCode(area.getAreaCode());
                    vo.setAreaName(area.getAreaName());
                    return vo;
                }).collect(Collectors.toList());
    }
}