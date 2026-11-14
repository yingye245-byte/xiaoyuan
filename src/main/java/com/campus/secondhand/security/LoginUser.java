package com.campus.secondhand.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 登录用户：同时保存角色编码与菜单权限标识，供页面 sec:authorize 与后端校验使用
 */
@Getter
@Setter
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String campus;

    private boolean enabled;

    /** 角色编码，如 ROLE_ADMIN */
    private Set<String> roles = Collections.emptySet();

    /** 权限标识，如 item:audit */
    private Set<String> perms = Collections.emptySet();

    private Collection<? extends GrantedAuthority> authorities = Collections.emptyList();

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
        return enabled;
    }

    /** 是否拥有指定角色 */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /** 是否拥有指定权限标识 */
    public boolean hasPerm(String perm) {
        return perms != null && perms.contains(perm);
    }
}