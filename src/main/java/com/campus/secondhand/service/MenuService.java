package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.entity.Menu;
import com.campus.secondhand.entity.Role;
import com.campus.secondhand.mapper.MenuMapper;
import com.campus.secondhand.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单服务：按用户角色动态生成后台侧边栏，实现菜单级权限控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;

    /**
     * 当前用户可见的菜单树
     */
    public List<Menu> menusOfUser(Long userId) {
        List<Menu> flat = menuMapper.selectMenusByUserId(userId);
        List<Menu> roots = new ArrayList<>();
        for (Menu menu : flat) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                roots.add(menu);
            }
        }
        for (Menu root : roots) {
            List<Menu> children = flat.stream()
                    .filter(m -> root.getId().equals(m.getParentId()))
                    .toList();
            root.setChildren(new ArrayList<>(children));
        }
        return roots;
    }

    public List<Menu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getSort).orderByAsc(Menu::getId));
    }

    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }
}