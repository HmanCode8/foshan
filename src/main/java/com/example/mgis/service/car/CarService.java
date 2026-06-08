package com.example.mgis.service.car;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mgis.controller.mapper.car.*;
import com.example.mgis.entity.car.*;
import com.example.mgis.untils.CurrentUserUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class CarService {

    @Resource
    private CarMapper carMapper;
    @Resource
    private CarSecurityMapper securityMapper;
    @Resource
    private CarTerminalMapper terminalMapper;
    @Resource
    private CarTrackHistoryMapper historyMapper;
    @Resource
    private CarTrackPointMapper pointMapper;
    @Resource
    private CarTrackSegmentMapper segmentMapper;
    @Resource
    private CarStopPointMapper stopPointMapper;

    // 注入当前登录用户工具类
    @Resource
    private CurrentUserUtil currentUserUtil;

    /**
     * 查询当前用户名下所有车辆（数据隔离）
     */
    public List<Car> carList() {
        Long userId = currentUserUtil.getCurrentUserId();
        // 未登录直接返回空
        if (userId == null) {
            return new ArrayList<>();
        }

        // 核心：只查当前用户的车辆
        QueryWrapper<Car> carWrapper = new QueryWrapper<>();
        carWrapper.eq("user_id", userId);
        List<Car> carList = carMapper.selectList(carWrapper);

        for (Car car : carList) {
            String carId = car.getId();
            // 1.安全员（加用户过滤）
            QueryWrapper<CarSecurity> secWrapper = new QueryWrapper<>();
            secWrapper.eq("car_id", carId).eq("user_id", userId);
            car.setSecurityInfo(securityMapper.selectOne(secWrapper));

            // 2.终端信息（加用户过滤）
            QueryWrapper<CarTerminal> terWrapper = new QueryWrapper<>();
            terWrapper.eq("car_id", carId).eq("user_id", userId);
            car.setTerminalInfo(terminalMapper.selectOne(terWrapper));

            // 3.轨迹历史（加用户过滤）
            Map<String, Object> historyMap = new HashMap<>();
            QueryWrapper<CarTrackHistory> hisWrapper = new QueryWrapper<>();
            hisWrapper.eq("car_id", carId).eq("user_id", userId);
            List<CarTrackHistory> historyDataList = historyMapper.selectList(hisWrapper);

            for (CarTrackHistory historyData : historyDataList) {
                String dateKey = historyData.getTrackDate();
                Map<String, Object> dayMap = new HashMap<>();

                dayMap.put("startTime", historyData.getStartTime());
                dayMap.put("endTime", historyData.getEndTime());

                // 轨迹点位（加用户过滤）
                QueryWrapper<CarTrackPoint> pointWrapper = new QueryWrapper<>();
                pointWrapper.eq("car_id", carId)
                        .eq("track_date", dateKey)
                        .eq("user_id", userId)
                        .orderByAsc("sort");
                List<CarTrackPoint> pointList = pointMapper.selectList(pointWrapper);

                List<List<Double>> plannedRoute = new ArrayList<>();
                List<List<Double>> actualRoute = new ArrayList<>();
                for (CarTrackPoint point : pointList) {
                    List<Double> xy = Arrays.asList(point.getLng(), point.getLat());
                    if ("planned".equals(point.getPointType())) {
                        plannedRoute.add(xy);
                    } else {
                        actualRoute.add(xy);
                    }
                }
                dayMap.put("plannedRoute", plannedRoute);
                dayMap.put("actualRoute", actualRoute);

                // 状态分段（加用户过滤）
                QueryWrapper<CarTrackSegment> segWrapper = new QueryWrapper<>();
                segWrapper.eq("car_id", carId)
                        .eq("track_date", dateKey)
                        .eq("user_id", userId);
                List<CarTrackSegment> segmentList = segmentMapper.selectList(segWrapper);
                dayMap.put("statusSegments", segmentList);

                // 统计信息
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalDuration", historyData.getTotalDuration());
                stats.put("totalDistance", historyData.getTotalDistance());
                stats.put("stayDuration", historyData.getStayDuration());
                stats.put("maxSpeed", historyData.getMaxSpeed());
                stats.put("avgSpeed", historyData.getAvgSpeed());
                stats.put("deviationStatus", historyData.getDeviationStatus());
                dayMap.put("stats", stats);

                // 停留点（加用户过滤）
                QueryWrapper<CarStopPoint> stopWrapper = new QueryWrapper<>();
                stopWrapper.eq("car_id", carId)
                        .eq("track_date", dateKey)
                        .eq("user_id", userId);
                List<CarStopPoint> stopList = stopPointMapper.selectList(stopWrapper);
                List<Map<String, Object>> stopPoints = new ArrayList<>();
                for (CarStopPoint stop : stopList) {
                    Map<String, Object> stopItem = new HashMap<>();
                    stopItem.put("coords", Arrays.asList(stop.getLng(), stop.getLat()));
                    stopItem.put("type", stop.getType());
                    stopItem.put("duration", stop.getDuration());
                    stopPoints.add(stopItem);
                }
                dayMap.put("stopPoints", stopPoints);

                historyMap.put(dateKey, dayMap);
            }
            car.setHistory(historyMap);

            // 首页最新路线（加用户过滤）
            if (!historyDataList.isEmpty()) {
                CarTrackHistory latest = historyDataList.get(0);
                QueryWrapper<CarTrackPoint> latestPointWrapper = new QueryWrapper<>();
                latestPointWrapper.eq("car_id", carId)
                        .eq("track_date", latest.getTrackDate())
                        .eq("user_id", userId)
                        .orderByAsc("sort");
                List<CarTrackPoint> latestPoints = pointMapper.selectList(latestPointWrapper);

                List<List<Double>> newPlan = new ArrayList<>();
                List<List<Double>> newAct = new ArrayList<>();
                for (CarTrackPoint p : latestPoints) {
                    List<Double> xy = Arrays.asList(p.getLng(), p.getLat());
                    if ("planned".equals(p.getPointType())) newPlan.add(xy);
                    else newAct.add(xy);
                }
                car.setPlannedRoute(newPlan);
                car.setActualRoute(newAct);
            }
        }
        return carList;
    }

    /**
     * 删除车辆：校验归属 + 级联删除子数据（防越权删除别人数据）
     */
    public void deleteCar(String carId) {
        if (!StringUtils.hasText(carId)) {
            return;
        }
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null) {
            return;
        }

        // 先校验：该车辆是否属于当前登录用户，不属于则直接返回
        QueryWrapper<Car> checkCar = new QueryWrapper<>();
        checkCar.eq("id", carId).eq("user_id", userId);
        Long count = carMapper.selectCount(checkCar);
        if (count <= 0) {
            return;
        }

        // 归属校验通过，再级联删除（全部加上 user_id 双重防护）
        carMapper.deleteById(carId);

        QueryWrapper<CarSecurity> secDel = new QueryWrapper<>();
        secDel.eq("car_id", carId).eq("user_id", userId);
        securityMapper.delete(secDel);

        QueryWrapper<CarTerminal> terDel = new QueryWrapper<>();
        terDel.eq("car_id", carId).eq("user_id", userId);
        terminalMapper.delete(terDel);

        QueryWrapper<CarTrackHistory> hisDel = new QueryWrapper<>();
        hisDel.eq("car_id", carId).eq("user_id", userId);
        historyMapper.delete(hisDel);

        QueryWrapper<CarTrackPoint> pointDel = new QueryWrapper<>();
        pointDel.eq("car_id", carId).eq("user_id", userId);
        pointMapper.delete(pointDel);

        QueryWrapper<CarTrackSegment> segDel = new QueryWrapper<>();
        segDel.eq("car_id", carId).eq("user_id", userId);
        segmentMapper.delete(segDel);

        QueryWrapper<CarStopPoint> stopDel = new QueryWrapper<>();
        stopDel.eq("car_id", carId).eq("user_id", userId);
        stopPointMapper.delete(stopDel);
    }

    // 【补充】新增车辆：自动赋值 userId，前端不用传
    public int addCar(Car car) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        // 自动绑定当前登录用户
        car.setUserId(userId);
        return carMapper.insert(car);
    }
}