package com.example.mgis.service.chart;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mgis.controller.mapper.chart.UserFriendMapper;
import com.example.mgis.entity.chart.UserFriend;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class UserFriendService extends ServiceImpl<UserFriendMapper, UserFriend> {
    @Resource
    private UserFriendMapper friendMapper;
    public List<String> getPassFriend(String username){
        return friendMapper.selectPassFriend(username);
    }
    public List<String> getAllRelation(String username){
        return friendMapper.selectAllRelation(username);
    }
    public List<String> getMyApplyUser(String myName){
        return friendMapper.selectMyApply(myName);
    }
}