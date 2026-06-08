package com.example.mgis.service.geom;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.PointApply;

import java.util.List;

public interface PointApplyService {
    boolean savePointApply(PointApply apply);
    IPage<PointApply> pagePointApply(Page<PointApply> page,PointApply pointApply,String startDate,String endDate);
    boolean updateById(PointApply entity);
    boolean remoteById(PointApply entity);

    List<PointApply> list(QueryWrapper<PointApply> status);
}