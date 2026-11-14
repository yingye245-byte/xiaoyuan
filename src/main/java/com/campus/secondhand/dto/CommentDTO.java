package com.campus.secondhand.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 留言表单
 */
@Data
public class CommentDTO {

    @NotNull(message = "缺少物品信息")
    private Long itemId;

    /** 为空表示直接评论物品，非空表示回复某条留言 */
    private Long parentId;

    @NotBlank(message = "留言内容不能为空")
    @Size(min = 1, max = 500, message = "留言内容不能超过 500 个字符")
    private String content;
}