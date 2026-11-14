package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单权限 Mapper：菜单级 + 按钮级权限均从数据库动态加载
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 查询用户的按钮级权限标识（如 item:audit）
     */
    @Select("SELECT DISTINCT m.perms FROM tb_menu m " +
            "INNER JOIN tb_role_menu rm ON rm.menu_id = m.id " +
            "INNER JOIN tb_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms <> ''")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户可见的菜单（不含按钮），用于后台侧边栏动态渲染
     */
    @Select("SELECT DISTINCT m.* FROM tb_menu m " +
            "INNER JOIN tb_role_menu rm ON rm.menu_id = m.id " +
            "INNER JOIN tb_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} AND m.type IN (0, 1) AND m.visible = 1 " +
            "ORDER BY m.parent_id ASC, m.sort ASC")
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
}