package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审核记录 Mapper
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {

    /**
     * 某件物品的审核历史
     */
    @Select("SELECT a.*, u.nickname AS admin_name, i.title AS item_title " +
            "FROM tb_audit_record a " +
            "LEFT JOIN tb_user u ON u.id = a.admin_id " +
            "LEFT JOIN tb_item i ON i.id = a.item_id " +
            "WHERE a.item_id = #{itemId} ORDER BY a.create_time DESC")
    List<AuditRecord> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 后台审核记录分页
     */
    @Select("SELECT a.*, u.nickname AS admin_name, i.title AS item_title " +
            "FROM tb_audit_record a " +
            "LEFT JOIN tb_user u ON u.id = a.admin_id " +
            "LEFT JOIN tb_item i ON i.id = a.item_id " +
            "ORDER BY a.create_time DESC")
    IPage<AuditRecord> selectRecordPage(IPage<AuditRecord> page);
}