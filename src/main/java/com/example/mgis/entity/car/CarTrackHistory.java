package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@TableName("car_track_history")
public class CarTrackHistory {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private String trackDate;
    private String startTime;
    private String endTime;
    private String totalDuration;
    private String totalDistance;
    private String stayDuration;
    private String maxSpeed;
    private String avgSpeed;
    private String deviationStatus;
}