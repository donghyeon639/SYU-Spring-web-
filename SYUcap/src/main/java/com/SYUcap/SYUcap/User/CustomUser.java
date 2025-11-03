package com.SYUcap.SYUcap.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUser extends User {
    private final Long id;
    private final String userName;

    public CustomUser(String username,
                      String password,
                      Collection<? extends GrantedAuthority> authorities,
                      Long id,
                      String userName) {
        super(username, password, authorities);
        this.id = id;
        this.userName = userName;
    }

    public Long getId() {
        return this.id;
    }

    public String getUserName() {
        return this.userName;
    }
}