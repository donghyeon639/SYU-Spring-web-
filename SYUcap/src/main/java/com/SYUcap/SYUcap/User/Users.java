package com.SYUcap.SYUcap.User;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@ToString
@Getter
@Setter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true)
    private String userId; // 사용자 아이디

    @Column(nullable = false)
    private String password; // 암호화된 비밀번호
    private String userName; // 사용자 이름
}
