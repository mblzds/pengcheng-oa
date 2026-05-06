package com.pengcheng.realty.alliance.dto;

import com.pengcheng.realty.alliance.entity.Alliance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联盟商展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceVO {

    private Long id;
    private String companyName;
    private String officeAddress;
    private String contactName;
    private String contactPhone;
    private Integer staffSize;
    private Integer level;
    private Integer status;
    private Long userId;
    private Long channelUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AllianceVO fromEntity(Alliance alliance) {
        if (alliance == null) {
            return null;
        }
        return AllianceVO.builder()
                .id(alliance.getId())
                .companyName(alliance.getCompanyName())
                .officeAddress(alliance.getOfficeAddress())
                .contactName(alliance.getContactName())
                .contactPhone(alliance.getContactPhone())
                .staffSize(alliance.getStaffSize())
                .level(alliance.getLevel())
                .status(alliance.getStatus())
                .userId(alliance.getUserId())
                .channelUserId(alliance.getChannelUserId())
                .createTime(alliance.getCreateTime())
                .updateTime(alliance.getUpdateTime())
                .build();
    }
}
