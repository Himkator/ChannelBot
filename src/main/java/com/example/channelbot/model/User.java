package com.example.channelbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
@Getter
@Setter
@Entity(name="UserRep")
public class User {
    @Id
    private Long Chatid;
    private String name;
    private Timestamp time;

}
