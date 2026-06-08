package com.example.mgis.entity.car;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("car_security")
public class CarSecurity {
    @JsonIgnore
    private Long userId;
    private Long id;
    private String carId;
    private String name;
    private String gender;
    private String phone;
    private String unit;
    private String licenseNo;
    private String licenseValidUntil;
}
