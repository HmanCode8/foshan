package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.chart.dto.*;
import com.example.mgis.entity.chart.vo.GroupMsgReadVO;
import com.example.mgis.entity.chart.vo.GroupUnreadVO;
import com.example.mgis.service.chart.ChatGroupService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chart/group")
public class ChatGroupController {
    @Resource
    private ChatGroupService groupService;

    // 1. 修改群信息（群名、公告、简介）
    @PostMapping("/update/info")
    public Result<?> updateGroupInfo(@RequestBody GroupUpdateDTO dto){
        return groupService.updateGroupInfo(dto);
    }

    // 2. 修改成员群内昵称
    @PostMapping("/member/nick/update")
    public Result<?> updateMemberNick(@RequestBody GroupMemberNickDTO dto){
        return groupService.updateMemberNick(dto);
    }

    // 3. 解散删除群（仅群主）
    @PostMapping("/delete")
    public Result<?> deleteGroup(@RequestBody GroupDeleteDTO dto){
        return groupService.deleteGroup(dto);
    }

    // 4. 移除群成员
    @PostMapping("/member/remove")
    public Result<?> removeMember(@RequestBody GroupRemoveMemberDTO dto){
        return groupService.removeMember(dto);
    }

    // 5. 转让群主
    @PostMapping("/owner/transfer")
    public Result<?> transferOwner(@RequestBody GroupTransferOwnerDTO dto){
        return groupService.transferOwner(dto);
    }

    // 6. 标记单条群消息已读
    @PostMapping("/msg/read/single")
    public Result<?> readSingleMsg(@RequestBody GroupMsgReadDTO dto){
        return groupService.readSingleMsg(dto);
    }

    // 7. 一键标记本群全部消息已读
    @PostMapping("/msg/read/all")
    public Result<?> readAllMsg(@RequestBody GroupMsgReadDTO dto){
        return groupService.readAllMsg(dto);
    }

    // 8. 获取当前用户群未读消息总数
    @GetMapping("/unread/count")
    public Result<GroupUnreadVO> getUnreadCount(@RequestParam Long groupId, @RequestParam String currentUser){
        GroupUnreadVO vo = groupService.getUnreadCount(groupId, currentUser);
        return Result.success(vo);
    }

    // 9. 查询单条消息已读/未读成员列表
    @GetMapping("/msg/read/list")
    public Result<GroupMsgReadVO> getMsgReadList(@RequestParam Long groupId, @RequestParam Long groupMsgId, @RequestParam String currentUser){
        GroupMsgReadVO vo = groupService.getMsgReadUser(groupId, groupMsgId);
        return Result.success(vo);
    }
    @PostMapping("/status")
    public Result<?> setGroupChatStatus(
            @RequestParam Long groupId,
            @RequestParam String currentUser,
            @RequestParam Integer status
    ) {
        // 状态校验只能0/1
        if(status != 0 && status != 1){
            return Result.fail("状态仅支持0或1");
        }
        groupService.updateMemberChatStatus(groupId, currentUser, status);
        return Result.success("状态更新成功");
    }
}