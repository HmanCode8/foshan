package com.example.mgis.service.geom;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.LineApply;

import java.util.List;

public interface LineApplyService {
    boolean saveLineApply(LineApply apply);
    IPage<LineApply> pageLineApply(Page<LineApply> page,LineApply lineApply,String startDate,String endDate);
    boolean updateById(LineApply entity);
    boolean remoteById(LineApply entity);

    List<LineApply> list(QueryWrapper<LineApply> status);
}