package com.treasure.restart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MomentListRow {

    private Long id;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
