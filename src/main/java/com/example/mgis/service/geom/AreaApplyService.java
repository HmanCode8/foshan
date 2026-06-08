package com.example.mgis.service.geom;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.AreaApply;

import java.util.List;

public interface AreaApplyService {
    boolean saveAreaApply(AreaApply apply);

    IPage<AreaApply> pageAreaApply(Page<AreaApply> page,AreaApply areaApply,String startDate,String endDate);


    boolean updateById(AreaApply entity);
    boolean remoteById(AreaApply areaApply);

    List<AreaApply> list(QueryWrapper<AreaApply> status);
}