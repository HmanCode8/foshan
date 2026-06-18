package com.example.mgis.controller.mapper.chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.chart.ChatGroupMsg;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 绑定群消息实体 ChatGroupMsg
public interface ChatGroupMsgMapper extends BaseMapper<ChatGroupMsg> {
    @Delete("DELETE FROM chat_group_msg WHERE group_id = #{groupId}")
    void deleteByGroupId(@Param("groupId") Long groupId);
}