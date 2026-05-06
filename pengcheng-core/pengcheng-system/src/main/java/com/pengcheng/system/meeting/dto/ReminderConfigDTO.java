package com.pengcheng.system.meeting.dto;

import lombok.Data;

@Data
public class ReminderConfigDTO {

    private Integer defaultReminder = 15;
    private Boolean internalNotification = Boolean.TRUE;
    private Boolean email = Boolean.FALSE;
}
