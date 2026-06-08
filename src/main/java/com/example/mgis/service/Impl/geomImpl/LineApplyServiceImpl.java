package com.example.mgis.service.Impl.geomImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mgis.entity.geom.LineApply;
import com.example.mgis.entity.geom.LineSegment;
import com.example.mgis.controller.mapper.geom.LineApplyMapper;
import com.example.mgis.controller.mapper.geom.LineSegmentMapper;
import com.example.mgis.service.geom.LineApplyService;
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
public class LineApplyServiceImpl implements LineApplyService {

    @Resource
    private LineApplyMapper lineApplyMapper;

    @Resource
    private LineSegmentMapper lineSegmentMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CurrentUserUtil currentUserUtil;

    @Override
    @Transactional
    public boolean saveLineApply(LineApply apply) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        // 自动绑定当前用户
        apply.setUserId(userId);

        if (apply.getId() == null || apply.getId().isEmpty()) {
            apply.setId("app_" + UUID.randomUUID());
        }
        lineApplyMapper.insert(apply);

        List<LineSegment> segments = apply.getSegments();
        // 替换原来的 segments.forEach + insertBatch
        if (segments != null && !segments.isEmpty()) {
            for (LineSegment seg : segments) {
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
                lineSegmentMapper.insert(seg);
            }
        }
        return true;
    }

    @Override
    public IPage<LineApply> pageLineApply(Page<LineApply> page, LineApply lineApply, String startDate, String endDate) {
        Long userId = currentUserUtil.getCurrentUserId();
        QueryWrapper<LineApply> wrapper = new QueryWrapper<>();
        // 追加用户过滤
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        QueryWrapperUtils.buildTypeQuery(wrapper, lineApply.getType(), lineApply.getStatus(), startDate, endDate);

        IPage<LineApply> iPage = lineApplyMapper.selectPage(page, wrapper);

        iPage.getRecords().forEach(apply -> {
            QueryWrapper<LineSegment> segWrapper = new QueryWrapper<>();
            segWrapper.eq("apply_id", apply.getId());
            if (userId != null) {
                segWrapper.eq("user_id", userId);
            }
            List<LineSegment> segments = lineSegmentMapper.selectList(segWrapper);
            apply.setSegments(segments);
        });
        return iPage;
    }

    @Override
    public boolean updateById(LineApply entity) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || !StringUtils.hasText(entity.getId())) {
            return false;
        }
        // 校验归属
        QueryWrapper<LineApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", entity.getId()).eq("user_id", userId);
        if (lineApplyMapper.selectCount(checkWrapper) <= 0) {
            return false;
        }
        return lineApplyMapper.updateById(entity) > 0;
    }

    public boolean remoteById(LineApply lineApply) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId == null || lineApply == null || !StringUtils.hasText(lineApply.getId())) {
            return false;
        }
        // 校验归属
        QueryWrapper<LineApply> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("id", lineApply.getId()).eq("user_id", userId);
        if (lineApplyMapper.selectCount(checkWrapper) <= 0) {
            return false;
        }
        // 删除主表
        boolean delMain = lineApplyMapper.deleteById(lineApply) > 0;
        // 级联删除子表
        QueryWrapper<LineSegment> segDel = new QueryWrapper<>();
        segDel.eq("apply_id", lineApply.getId()).eq("user_id", userId);
        lineSegmentMapper.delete(segDel);
        return delMain;
    }

    @Override
    public List<LineApply> list(QueryWrapper<LineApply> status) {
        Long userId = currentUserUtil.getCurrentUserId();
        if (userId != null) {
            status.eq("user_id", userId);
        }
        List<LineApply> list = lineApplyMapper.selectList(status);

        if (list != null && !list.isEmpty()) {
            list.forEach(apply -> {
                QueryWrapper<LineSegment> segWrapper = new QueryWrapper<>();
                segWrapper.eq("apply_id", apply.getId());
                if (userId != null) {
                    segWrapper.eq("user_id", userId);
                }
                List<LineSegment> segments = lineSegmentMapper.selectList(segWrapper);
                apply.setSegments(segments);
            });
        }
        return list;
    }
}