package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.car.Car;
import com.example.mgis.service.car.CarService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/aiCar")
public class CarController {

    @Resource
    private CarService carService;

    // ====================== 【核心接口】获取所有车辆（完整数据） ======================
    @GetMapping("/list")
    public Result<List<Car>> carList() {
        return Result.success(carService.carList());
    }

    // ====================== 保存/更新车辆 ======================
//    @PostMapping("/save")
//    public Result<String> saveCar(@RequestBody Car car) {
//        carService.saveCar(car);
//        return Result.success("保存成功");
//    }

    // ====================== 删除车辆 ======================
    @DeleteMapping("/delete")
    public Result<String> deleteCar(@RequestParam String id) {
        carService.deleteCar(id);
        return Result.success("删除成功");
    }
}