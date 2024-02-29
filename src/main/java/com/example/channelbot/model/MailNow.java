package com.example.channelbot.model;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity(name = "mailNow")
public class MailNow {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long chatId;
    @Column(length = 2550000)
    private String body;
    private Timestamp date;
    private String photo;
    private String channel;
    private String doc;
    private String DocPath;
    private String picPath;
    @Column(length = 25500)
    private String title;
    private String bodyAfter;
}
