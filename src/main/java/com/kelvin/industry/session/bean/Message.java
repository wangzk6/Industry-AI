package com.kelvin.industry.session.bean;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {

    private String id;

    private String content;

    private Role role;

    private LocalDateTime timestamp;
}
