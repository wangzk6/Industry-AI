package com.kelvin.industry.session.bean;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Queue;

/**
 *  会话bean
 */
@Data
public class Session {
    /**
     *  sessionId
     */
    private String id;

    //创建实践
    private LocalDateTime createdAt;

    //消息Id集合
    private Queue<Message> messages;

    public boolean chat(Message message){
        this.messages.offer(message);
        return true;
    }

}
