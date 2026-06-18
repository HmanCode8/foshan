package com.example.mgis.entity.chart.vo;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class GroupMsgVO {
    private Long id;
    private Long groupId;
    private String sendUsername;
    private String sendAvatar;
    private String msgContent;
    private Integer msgType;
    private String fileUrl;
    private String fileName;
    private String fileSize;
    private LocalDateTime createTime;
}