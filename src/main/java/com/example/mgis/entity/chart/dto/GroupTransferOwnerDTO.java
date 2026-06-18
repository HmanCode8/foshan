package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupTransferOwnerDTO {
    private Long groupId;
    private String currentUser;
    private String newOwnerUsername;
}
