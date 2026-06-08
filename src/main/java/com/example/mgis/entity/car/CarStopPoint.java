package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("car_stop_point")
public class CarStopPoint {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private String trackDate;
    private Double lng;
    private Double lat;
    private String type;
    private String duration;

}