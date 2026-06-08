package com.example.mgis.service.Impl.geomImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.LineSegment;
import com.example.mgis.entity.geom.PointApply;
import com.example.mgis.entity.geom.PointSegment;
import com.example.mgis.controller.mapper.geom.PointApplyMapper;
import com.example.mgis.controller.mapper.geom.PointSegmentMapper;
import com.example.mgis.service.geom.PointApplyService;
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
public class PointApplyServiceImpl implements PointApplyService {

    @Resource
    private PointApplyMapper pointApplyMapper;

    @Resource
    private PointSegmentMapper pointSegmentMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CurrentUserUtil currentUserUtil;

    @Override
    @Transactional
    public boolean savePointApply(PointApply apply) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        // 自动绑定当前用户
        apply.setUserId(userId);

        if (apply.getId() == null || apply.getId().isEmpty()) {
            apply.setId("app_" + UUID.randomUUID());
        }
        pointApplyMapper.insert(apply);

        List<PointSegment> segments = apply.getSegments();
        if (segments != null && !segments.isEmpty()) {
            for (PointSegment seg : segments) {
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
                pointSegmentMapper.insert(seg);
            }
        }
        return true;
    }

    @Override
    public IPage<PointApply> pagePointApply(Page<PointApply> page, PointApply pointApply, String startDate, String endDate) {
        Long userId = currentUserUtil.getCurrentUserId();
        QueryWrapper<PointApply> wrapper = new QueryWrapper<>();
        // 追加用户过滤
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        QueryWrapperUtils.buildStatusAndDateQuery(wrapper, pointApply.getStatus(), startDate, endDate);

        IPage<PointApply> iPage = pointApplyMapper.selectPage(page, wrapper);

        iPage.getRecords().forEach(apply -> {
            QueryWrapper<PointSegment> segWrapper = new QueryWrapper<>();
            segWrapper.eq("apply_id", apply.getId());
            if (userId != null) {
                segWrapper.eq("user_id", userId);
            }
            List<PointSegment> segments = pointSegmentMapper.selectList(segWrapper);
            apply.setSegments(segments);
        });
        return iPage;
    }

    @Override
    public boolean updateById(PointApply entity) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || !StringUtils.hasText(entity.getId())) {
            return false;
        }
        // 校验归属
        QueryWrapper<PointApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", entity.getId()).eq("user_id", userId);
        if (pointApplyMapper.selectCount(checkWrapper) <= 0) {
            return false;
        }
        return pointApplyMapper.updateById(entity) > 0;
    }

    public boolean remoteById(PointApply pointApply) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || pointApply == null || !StringUtils.hasText(pointApply.getId())) {
            return false;
        }
        // 校验归属
        QueryWrapper<PointApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", pointApply.getId()).eq("user_id", userId);
        if (pointApplyMapper.selectCount(checkWrapper) <= 0) {
            return false;
        }
        // 删除主表
        boolean delMain = pointApplyMapper.deleteById(pointApply) > 0;
        // 级联删除子表
        QueryWrapper<PointSegment> segDel = new QueryWrapper<>();
        segDel.eq("apply_id", pointApply.getId()).eq("user_id", userId);
        pointSegmentMapper.delete(segDel);
        return delMain;
    }

    @Override
    public List<PointApply> list(QueryWrapper<PointApply> status) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId != null) {
            status.eq("user_id", userId);
        }
        List<PointApply> list = pointApplyMapper.selectList(status);

        if (list != null && !list.isEmpty()) {
            list.forEach(apply -> {
                QueryWrapper<PointSegment> segWrapper = new QueryWrapper<>();
                segWrapper.eq("apply_id", apply.getId());
                if (userId != null) {
                    segWrapper.eq("user_id", userId);
                }
                List<PointSegment> segments = pointSegmentMapper.selectList(segWrapper);
                apply.setSegments(segments);
            });
        }
        return list;
    }
}