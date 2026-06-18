package com.example.mgis.controller.mapper.chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.chart.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
// 必须绑定实体 ChatGroupMember
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {
    @Select("SELECT username FROM chat_group_member WHERE group_id = #{groupId}")
    List<String> selectUsernameByGroupId(@Param("groupId") Long groupId);
}