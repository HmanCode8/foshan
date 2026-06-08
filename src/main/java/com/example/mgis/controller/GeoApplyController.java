package com.example.mgis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.common.result.Result;
import com.example.mgis.entity.geom.AreaApply;
import com.example.mgis.entity.geom.AuditDTO;
import com.example.mgis.entity.geom.LineApply;
import com.example.mgis.entity.geom.PointApply;
import com.example.mgis.service.geom.AreaApplyService;
import com.example.mgis.service.geom.LineApplyService;
import com.example.mgis.service.geom.PointApplyService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/processAudit")
public class GeoApplyController {

    @Resource
    private AreaApplyService areaApplyService;

    @Resource
    private LineApplyService lineApplyService;

    @Resource
    private PointApplyService pointApplyService;

    // ===================== 【新加接口】查询所有已审核（点+线+面） =======================
    @GetMapping("/all/audited")
    public Result<Map<String, Object>> getAllAudited() {

        String auditedStatus = "approved,reject";

        // ========== 1. 查询全部 面（不分页） ==========
        List<AreaApply> areaList = areaApplyService.list(
                new QueryWrapper<AreaApply>().in("status", auditedStatus.split(","))
        );

        // ========== 2. 查询全部 线，然后拆分 ==========
        List<LineApply> allLineList = lineApplyService.list(
                new QueryWrapper<LineApply>().in("status", auditedStatus.split(","))
        );

        // 普通路线 line
        List<LineApply> lineList = allLineList.stream()
                .filter(i -> "line".equals(i.getType()))
                .toList();

        // 转场路线 resLine
        List<LineApply> resLineList = allLineList.stream()
                .filter(i -> "resLine".equals(i.getType()))
                .toList();

        // ========== 3. 查询全部 点（不分页） ==========
        List<PointApply> pointList = pointApplyService.list(
                new QueryWrapper<PointApply>().in("status", auditedStatus.split(","))
        );

        // ========== 返回你要的结构 ==========
        Map<String, Object> result = new HashMap<>();
        result.put("area_audit_rows_v1", areaList);
        result.put("line_audit_rows_v1", lineList);
        result.put("parking_audit_rows_v1", pointList);
        result.put("res_line_audit_rows_v1", resLineList);

        return Result.success(result);
    }

    // ===================== 面 =======================
    @PostMapping("/area/save")
    public Result<String> saveArea(@RequestBody AreaApply apply) {
        boolean ok = areaApplyService.saveAreaApply(apply);
        return ok ? Result.success("保存成功") : Result.fail("保存失败");
    }

    @GetMapping("/area/query")
    public Result<IPage<AreaApply>> areaList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<AreaApply> pageParam = new Page<>(page, size);
        AreaApply areaApply = new AreaApply();
        areaApply.setStatus(status);
        IPage<AreaApply> result = areaApplyService.pageAreaApply(pageParam, areaApply, startDate, endDate);
        return Result.success(result);
    }

    // ===================== 面：审核接口 =======================
    @PostMapping("/area/audit")
    public Result<String> auditArea(@RequestBody AuditDTO dto) {
        AreaApply apply = new AreaApply();
        apply.setId(dto.getId());
        apply.setStatus(dto.getStatus());
        boolean ok = areaApplyService.updateById(apply);
        return ok ? Result.success("审核成功") : Result.fail("审核失败");
    }

    @PostMapping("/area/delete")
    public Result<String> deleteArea(@RequestBody AuditDTO dto){
        AreaApply areaApply = new AreaApply();
        areaApply.setId(dto.getId());
        boolean ok = areaApplyService.remoteById(areaApply);
        return ok ? Result.success("删除成功") : Result.fail("删除失败");
    }

    // ===================== 线 =======================
    @PostMapping("/line/save")
    public Result<String> saveLine(@RequestBody LineApply apply) {
        boolean ok = lineApplyService.saveLineApply(apply);
        return ok ? Result.success("保存成功") : Result.fail("保存失败");
    }

    @GetMapping("/line/query")
    public Result<IPage<LineApply>> lineList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<LineApply> pageParam = new Page<>(page, size);
        LineApply lineApply = new LineApply();
        lineApply.setStatus(status);
        lineApply.setType(type);
        IPage<LineApply> result = lineApplyService.pageLineApply(pageParam, lineApply, startDate, endDate);
        return Result.success(result);
    }

    // ===================== 线：审核接口 =======================
    @PostMapping("/line/audit")
    public Result<String> auditLine(@RequestBody AuditDTO dto) {
        LineApply apply = new LineApply();
        apply.setId(dto.getId());
        apply.setStatus(dto.getStatus());
        apply.setRemark(dto.getRemark());
        boolean ok = lineApplyService.updateById(apply);
        return ok ? Result.success("审核成功") : Result.fail("审核失败");
    }

    @PostMapping("/line/edit")
    public Boolean updateLine(@RequestBody LineApply lineApply){
        return true;
    }

    @PostMapping("/line/delete")
    public Result<String> deleteLine(@RequestBody AuditDTO dto){
        LineApply lineApply = new LineApply();
        lineApply.setId(dto.getId());
        boolean ok = lineApplyService.remoteById(lineApply);
        return ok ? Result.success("删除成功") : Result.fail("删除失败");
    }

    // ===================== 点 =======================
    @PostMapping("/point/save")
    public Result<String> savePoint(@RequestBody PointApply apply) {
        boolean ok = pointApplyService.savePointApply(apply);
        return ok ? Result.success("保存成功") : Result.fail("保存失败");
    }

    @GetMapping("/point/query")
    public Result<IPage<PointApply>> pointList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<PointApply> pageParam = new Page<>(page, size);
        PointApply pointApply = new PointApply();
        pointApply.setStatus(status);
        IPage<PointApply> result = pointApplyService.pagePointApply(pageParam, pointApply, startDate, endDate);
        return Result.success(result);
    }

    // ===================== 点：审核接口 =======================
    @PostMapping("/point/audit")
    public Result<String> auditPoint(@RequestBody AuditDTO dto) {
        PointApply apply = new PointApply();
        apply.setId(dto.getId());
        apply.setStatus(dto.getStatus());
        boolean ok = pointApplyService.updateById(apply);
        return ok ? Result.success("审核成功") : Result.fail("审核失败");
    }

    @PostMapping("/point/delete")
    public Result<String> deletePoint(@RequestBody AuditDTO dto){
        PointApply pointApply = new PointApply();
        pointApply.setId(dto.getId());
        boolean ok = pointApplyService.remoteById(pointApply);
        return ok ? Result.success("删除成功") : Result.fail("删除失败");
    }
}