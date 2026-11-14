package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户拥有的角色
     */
    @Select("SELECT r.* FROM tb_role r INNER JOIN tb_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<Role> selectByUserId(@Param("userId") Long userId);
}