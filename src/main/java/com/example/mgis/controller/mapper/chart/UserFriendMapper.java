package com.example.mgis.controller.mapper.chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mgis.entity.chart.UserFriend;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
public interface UserFriendMapper extends BaseMapper<UserFriend> {
    //获取已通过好友账号
    @Select("select friend_uid from user_friend where uid=#{uid} and status=1 union select uid from user_friend where friend_uid=#{uid} and status=1")
    List<String> selectPassFriend(@Param("uid")String uid);
    //获取全部关联用户(申请/好友/拉黑)
    @Select("select friend_uid from user_friend where uid=#{uid} union select uid from user_friend where friend_uid=#{uid}")
    List<String> selectAllRelation(@Param("uid")String uid);
    //查询我收到的待审批申请
    @Select("select uid from user_friend where friend_uid=#{myName} and status=0")
    List<String> selectMyApply(@Param("myName")String myName);
}