package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("car_terminal")
public class CarTerminal {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private Double speed;
    private String status;
    private Integer power;
    private Integer batteryTemp;
    private String signalStatus;
    private String gear;
    private String lightStatus;
    private String hornStatus;
    private String signalRealtime;
}
