package com.example.mgis.controller.mapper.chart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.chart.ChatGroupMsgRead;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatGroupMsgReadMapper extends BaseMapper<ChatGroupMsgRead> {
    // 批量插入未读记录
    int insertBatch(@Param("list") List<ChatGroupMsgRead> list);

    // 批量更新该用户群所有消息为已读
    int updateAllReadByUser(@Param("groupId") Long groupId, @Param("username") String username);

    // 标记单条群消息为已读
    int updateSingleMsgRead(
            @Param("groupMsgId") Long groupMsgId,
            @Param("username") String username
    );

    // 统计群未读数量
    @Select("SELECT COUNT(1) FROM chat_group_msg_read WHERE group_id = #{groupId} AND username = #{username} AND is_read = 0")
    int countUnreadMsg(@Param("groupId") Long groupId, @Param("username") String username);

    // 根据群ID删除全部未读记录（解散群级联删除）
    @Delete("DELETE FROM chat_group_msg_read WHERE group_id = #{groupId}")
    void deleteByGroupId(@Param("groupId") Long groupId);
}