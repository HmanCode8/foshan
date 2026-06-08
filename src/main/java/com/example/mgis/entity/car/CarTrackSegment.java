package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("car_track_segment")
public class CarTrackSegment {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private String trackDate;
    private String type;
    private String label;

    // 你就用这两个！！！
    private Integer startPct;
    private Integer endPct;

    private Double startLinePct;
    private Double endLinePct;
    private String startTime;
    private String endTime;
    private String duration;
}