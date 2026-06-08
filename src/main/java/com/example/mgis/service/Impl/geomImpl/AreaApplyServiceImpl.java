package com.example.mgis.service.Impl.geomImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.AreaApply;
import com.example.mgis.entity.geom.AreaSegment;
import com.example.mgis.controller.mapper.geom.AreaApplyMapper;
import com.example.mgis.controller.mapper.geom.AreaSegmentMapper;
import com.example.mgis.entity.geom.LineSegment;
import com.example.mgis.service.geom.AreaApplyService;
import com.example.mgis.untils.CurrentUserUtil;
import com.example.mgis.untils.QueryWrapperUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class AreaApplyServiceImpl implements AreaApplyService {

    @Resource
    private AreaApplyMapper areaApplyMapper;

    @Resource
    private AreaSegmentMapper areaSegmentMapper;

    @Resource
    private ObjectMapper objectMapper;

    // 注入当前用户工具类
    @Resource
    private CurrentUserUtil currentUserUtil;

    /**
     * 新增区域申请：自动绑定当前登录用户
     */
    @Override
    @Transactional
    public boolean saveAreaApply(AreaApply apply) {
        Long userId = currentUserUtil.getCurrentUserId();
        // 未登录直接返回失败
        if (userId == null) {
            return false;
        }
        // 自动赋值所属用户ID，前端不传
        apply.setUserId(userId);

        // 自动生成唯一 ID
        if (apply.getId() == null || apply.getId().isEmpty()) {
            apply.setId("app_" + UUID.randomUUID());
        }

        areaApplyMapper.insert(apply);

        List<AreaSegment> segments = apply.getSegments();
        if (segments != null && !segments.isEmpty()) {
            for (AreaSegment seg : segments) {
                seg.setApplyId(apply.getId());
                seg.setUserId(userId); // 这里已经赋值了
                try {
                    // 把 coords 序列化为字符串
                    String json = objectMapper.writeValueAsString(seg.getCoords());
                    seg.setCoords(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 单条插入，确保 userId 被拼进 SQL
                areaSegmentMapper.insert(seg);
            }
        }
        return true;
    }

    /**
     * 分页查询：只查询当前用户的数据
     */
    @Override
    public IPage<AreaApply> pageAreaApply(Page<AreaApply> page,
                                          AreaApply areaApply,
                                          String startDate,
                                          String endDate) {
        Long userId = currentUserUtil.getCurrentUserId();
        QueryWrapper<AreaApply> wrapper = new QueryWrapper<>();

        // 核心：追加用户过滤条件
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }

        // 原有状态、日期条件
        QueryWrapperUtils.buildStatusAndDateQuery(wrapper, areaApply.getStatus(), startDate, endDate);

        IPage<AreaApply> iPage = areaApplyMapper.selectPage(page, wrapper);

        // 装填子表数据，子表也加用户过滤
        iPage.getRecords().forEach(apply -> {
            QueryWrapper<AreaSegment> segWrapper = new QueryWrapper<>();
            segWrapper.eq("apply_id", apply.getId());
            if (userId != null) {
                segWrapper.eq("user_id", userId);
            }
            List<AreaSegment> segments = areaSegmentMapper.selectList(segWrapper);
            apply.setSegments(segments);
        });

        return iPage;
    }

    /**
     * 修改：校验数据归属，只能改自己的数据
     */
    @Override
    public boolean updateById(AreaApply entity) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || !StringUtils.hasText(entity.getId())) {
            return false;
        }

        // 先校验：该数据是否属于当前用户
        QueryWrapper<AreaApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", entity.getId()).eq("user_id", userId);
        Long count = areaApplyMapper.selectCount(checkWrapper);
        if (count <= 0) {
            return false;
        }

        return areaApplyMapper.updateById(entity) > 0;
    }

    /**
     * 删除：校验数据归属，只能删自己的数据
     */
    @Override
    public boolean remoteById(AreaApply areaApply) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || areaApply == null || !StringUtils.hasText(areaApply.getId())) {
            return false;
        }

        // 校验归属
        QueryWrapper<AreaApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", areaApply.getId()).eq("user_id", userId);
        Long count = areaApplyMapper.selectCount(checkWrapper);
        if (count <= 0) {
            return false;
        }

        // 删除主表
        boolean delMain = areaApplyMapper.deleteById(areaApply) > 0;
        // 级联删除子表（加用户过滤）
        QueryWrapper<AreaSegment> segDel = new QueryWrapper<>();
        segDel.eq("apply_id", areaApply.getId()).eq("user_id", userId);
        areaSegmentMapper.delete(segDel);

        return delMain;
    }

    /**
     * 列表查询：只查当前用户数据
     */
    @Override
    public List<AreaApply> list(QueryWrapper<AreaApply> queryWrapper) {
        Long userId = currentUserUtil.getCurrentUserId();
        // 追加用户条件
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }

        List<AreaApply> list = areaApplyMapper.selectList(queryWrapper);

        // 装填子表，子表加用户过滤
        if (list != null && !list.isEmpty()) {
            list.forEach(apply -> {
                QueryWrapper<AreaSegment> segWrapper = new QueryWrapper<>();
                segWrapper.eq("apply_id", apply.getId());
                if (userId != null) {
                    segWrapper.eq("user_id", userId);
                }
                List<AreaSegment> segments = areaSegmentMapper.selectList(segWrapper);
                apply.setSegments(segments);
            });
        }

        return list;
    }
}