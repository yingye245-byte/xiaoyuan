package com.campus.secondhand.security;

import com.campus.secondhand.entity.Role;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.MenuMapper;
import com.campus.secondhand.mapper.RoleMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录时加载用户、角色与菜单权限，装配成 LoginUser
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public UserDetailsServiceImpl(UserMapper userMapper, RoleMapper roleMapper, MenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<Role> roles = roleMapper.selectByUserId(user.getId());
        Set<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> perms = new HashSet<>(menuMapper.selectPermsByUserId(user.getId()));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roleCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        perms.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setEmail(user.getEmail());
        loginUser.setCampus(user.getCampus());
        loginUser.setEnabled(user.getStatus() != null && user.getStatus() == 1);
        loginUser.setRoles(roleCodes);
        loginUser.setPerms(perms);
        loginUser.setAuthorities(authorities);
        return loginUser;
    }
}