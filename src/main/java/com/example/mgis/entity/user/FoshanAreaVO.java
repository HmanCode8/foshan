package com.example.mgis.entity.user;

import lombok.Data;
import java.util.List;

/**
 * 最终返回前端：佛山市 两级结构
 */
@Data
public class FoshanAreaVO {
    private String cityCode;
    private String cityName;
    private List<DistrictVO> districts;

    @Data
    public static class DistrictVO {
        private String areaCode;
        private String areaName;
        private List<StreetVO> streets;
    }

    @Data
    public static class StreetVO {
        private String code;
        private String name;
    }
}