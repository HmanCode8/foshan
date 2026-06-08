package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("car_track_point")
public class CarTrackPoint {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private String trackDate;
    // 加上这一行！！
    private String pointType; // 用来区分 planned / actual
    private Double lng;
    private Double lat;
    private Integer sort;
}