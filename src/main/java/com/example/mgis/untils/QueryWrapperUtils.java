package com.example.mgis.untils;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

public class QueryWrapperUtils {

    /**
     * 通用封装：状态 + 开始日期 + 结束日期
     * @param wrapper 查询器
     * @param status 状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    public static void buildStatusAndDateQuery(QueryWrapper<?> wrapper,
                                               String status,
                                               String startDate,
                                               String endDate
                                             ) {
        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        // 日期区间
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("apply_date", startDate);
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("apply_date", endDate);
        }
        // 默认排序
        wrapper.orderByDesc("apply_date");
    }
    // ======================
    // 【带type版】线/点等有type字段的模块用
    // 多了 type 筛选
    // ======================
    public static void buildTypeQuery(QueryWrapper<?> wrapper,
                                        String type,
                                        String status,
                                        String startDate,
                                        String endDate) {
        // type 筛选（只有需要的模块才传）
        if (StringUtils.hasText(type)) {
            wrapper.eq("type", type);
        }
        // 其他条件复用通用版
        buildStatusAndDateQuery(wrapper, status, startDate, endDate);
    }
}
