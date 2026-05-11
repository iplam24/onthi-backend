package com.onthi.v_edu.push.dto;

import lombok.Data;

@Data
public class PushSubscriptionRequest {
    private Integer userId;
    private String endpoint;
    private String p256dh;
    private String auth;
}
