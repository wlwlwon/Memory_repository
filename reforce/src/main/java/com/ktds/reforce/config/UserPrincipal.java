package com.ktds.reforce.config;

import com.ktds.reforce.user.dto.User;
import com.ktds.reforce.user.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private String id;
    private String username;

    transient private String password;  // serialization 방지
    transient private UserDTO user;    // login 기능에만 사용

    private Set<GrantedAuthority> authorities;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
