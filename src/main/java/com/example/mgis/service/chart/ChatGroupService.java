package com.example.mgis.service.chart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.mgis.common.result.Result;
import com.example.mgis.controller.mapper.chart.ChatGroupMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMemberMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgReadMapper;
import com.example.mgis.entity.chart.*;
import com.example.mgis.entity.chart.dto.*;
import com.example.mgis.entity.chart.vo.GroupMsgReadVO;
import com.example.mgis.entity.chart.vo.GroupUnreadVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatGroupService {
    @Resource
    private ChatGroupMapper groupMapper;
    @Resource
    private ChatGroupMemberMapper memberMapper;
    @Resource
    private ChatGroupMsgMapper groupMsgMapper;
    @Resource
    private ChatGroupMsgReadMapper msgReadMapper;

    // 1. 修改群基础信息（群名、公告、简介）
    public Result<?> updateGroupInfo(GroupUpdateDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser();
        // 校验是否群主
        ChatGroup group = groupMapper.selectById(groupId);
        if(group == null) return Result.fail("群不存在");
        if(!group.getOwnerUsername().equals(currentUser)){
            return Result.fail("仅群主可修改群信息");
        }
        group.setGroupNickname(dto.getGroupNickname());
        group.setNotice(dto.getNotice());
        group.setRemark(dto.getRemark());
        groupMapper.updateById(group);
        return Result.success("修改成功");
    }

    // 2. 修改群成员专属昵称
    public Result<?> updateMemberNick(GroupMemberNickDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser();
        String targetUser = dto.getTargetUsername();
        // 查询群
        ChatGroup group = groupMapper.selectById(groupId);
        if(group == null) return Result.fail("群不存在");
        // 查询目标成员
        LambdaQueryWrapper<ChatGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, targetUser);
        ChatGroupMember member = memberMapper.selectOne(wrapper);
        if(member == null) return Result.fail("该用户不在群内");
        // 权限：自己改自己 || 群主改所有人
        boolean isOwner = group.getOwnerUsername().equals(currentUser);
        boolean isSelf = currentUser.equals(targetUser);
        if(!isOwner && !isSelf){
            return Result.fail("无权限修改他人昵称");
        }
        member.setMemberNick(dto.getMemberNick());
        memberMapper.updateById(member);
        return Result.success("昵称修改完成");
    }

    // 3. 解散/删除群（仅群主）
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteGroup(GroupDeleteDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser();
        ChatGroup group = groupMapper.selectById(groupId);
        if(group == null) return Result.fail("群不存在");
        if(!group.getOwnerUsername().equals(currentUser)){
            return Result.fail("只有群主可以解散群");
        }
        // 级联删除：已读记录 → 群消息 → 群成员 → 群本身
        msgReadMapper.deleteByGroupId(groupId);
        groupMsgMapper.deleteByGroupId(groupId);
        LambdaQueryWrapper<ChatGroupMember> memberWrap = new LambdaQueryWrapper<>();
        memberWrap.eq(ChatGroupMember::getGroupId, groupId);
        memberMapper.delete(memberWrap);
        groupMapper.deleteById(groupId);
        return Result.success("群已解散删除");
    }

    // 4. 移除群内成员（群主）
    public Result<?> removeMember(GroupRemoveMemberDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser();
        String targetUser = dto.getTargetUsername();
        ChatGroup group = groupMapper.selectById(groupId);
        if(group == null) return Result.fail("群不存在");
        if(!group.getOwnerUsername().equals(currentUser)){
            return Result.fail("仅群主可移除成员");
        }
        if(group.getOwnerUsername().equals(targetUser)){
            return Result.fail("无法移除群主，请先转让群主");
        }
        LambdaQueryWrapper<ChatGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, targetUser);
        memberMapper.delete(wrapper);
        return Result.success("成员已移出群");
    }

    // 5. 转让群主
    @Transactional
    public Result<?> transferOwner(GroupTransferOwnerDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser(); // 原群主
        String newOwner = dto.getNewOwnerUsername(); // 新群主
        ChatGroup group = groupMapper.selectById(groupId);
        if(group == null) return Result.fail("群不存在");
        // 校验操作人是当前群主
        if(!group.getOwnerUsername().equals(currentUser)){
            return Result.fail("仅群主可转让权限");
        }
        // 校验新群主在群内
        LambdaQueryWrapper<ChatGroupMember> existWrap = new LambdaQueryWrapper<>();
        existWrap.eq(ChatGroupMember::getGroupId, groupId).eq(ChatGroupMember::getUsername, newOwner);
        if(memberMapper.selectCount(existWrap) == 0){
            return Result.fail("目标用户不在群内");
        }

        // ========== 1、更新群表群主字段 ==========
        group.setOwnerUsername(newOwner);
        groupMapper.updateById(group);

        // ========== 2、原群主：0(群主) → 1(普通成员) ==========
        LambdaUpdateWrapper<ChatGroupMember> oldOwnerWrap = new LambdaUpdateWrapper<>();
        oldOwnerWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser)
                .set(ChatGroupMember::getMemberType, 1);
        memberMapper.update(null, oldOwnerWrap);

        // ========== 3、新群主：1(普通成员) → 0(群主) ==========
        LambdaUpdateWrapper<ChatGroupMember> newOwnerWrap = new LambdaUpdateWrapper<>();
        newOwnerWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, newOwner)
                .set(ChatGroupMember::getMemberType, 0);
        memberMapper.update(null, newOwnerWrap);

        return Result.success("群主转让完成");
    }

    // 6. 标记单条群消息已读
    public Result<?> readSingleMsg(GroupMsgReadDTO dto) {
        Long groupMsgId = dto.getGroupMsgId();
        String username = dto.getCurrentUser();
        LambdaUpdateWrapper<ChatGroupMsgRead> updateWrap = new LambdaUpdateWrapper<>();
        updateWrap.eq(ChatGroupMsgRead::getGroupMsgId, groupMsgId)
                .eq(ChatGroupMsgRead::getUsername, username)
                .set(ChatGroupMsgRead::getIsRead, 1)
                .set(ChatGroupMsgRead::getReadTime, LocalDateTime.now());
        msgReadMapper.update(null, updateWrap);
        return Result.success();
    }

    // 7. 标记群全部消息已读
    public Result<?> readAllMsg(GroupMsgReadDTO dto) {
        msgReadMapper.updateAllReadByUser(dto.getGroupId(), dto.getCurrentUser());
        return Result.success();
    }

    // 8. 获取群未读消息数量
    public GroupUnreadVO getUnreadCount(Long groupId, String username) {
        int count = msgReadMapper.countUnreadMsg(groupId, username);
        GroupUnreadVO vo = new GroupUnreadVO();
        vo.setGroupId(groupId);
        vo.setUnreadCount(count);
        return vo;
    }

    // 9. 查询单条消息已读/未读人员
    public GroupMsgReadVO getMsgReadUser(Long groupId, Long groupMsgId) {
        // 查询该消息所有记录
        LambdaQueryWrapper<ChatGroupMsgRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatGroupMsgRead::getGroupMsgId, groupMsgId);
        List<ChatGroupMsgRead> list = msgReadMapper.selectList(wrapper);
        List<String> read = list.stream().filter(x->x.getIsRead()==1).map(ChatGroupMsgRead::getUsername).collect(Collectors.toList());
        List<String> unread = list.stream().filter(x->x.getIsRead()==0).map(ChatGroupMsgRead::getUsername).collect(Collectors.toList());
        GroupMsgReadVO vo = new GroupMsgReadVO();
        vo.setGroupMsgId(groupMsgId);
        vo.setReadUserList(read);
        vo.setUnreadUserList(unread);
        return vo;
    }
    @Resource
    private ChatGroupMemberMapper groupMemberMapper;

    public void updateMemberChatStatus(Long groupId, String username, Integer status) {
        LambdaUpdateWrapper<ChatGroupMember> updateWrap = new LambdaUpdateWrapper<>();
        updateWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, username)
                .set(ChatGroupMember::getIsChatting, status);
        groupMemberMapper.update(null, updateWrap);
    }
}