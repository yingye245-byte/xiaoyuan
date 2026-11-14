package com.campus.secondhand.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品发布/编辑表单
 */
@Data
public class ItemPublishDTO {

    /** 编辑时携带 */
    private Long id;

    @NotNull(message = "请选择物品分类")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Size(min = 4, max = 60, message = "标题长度需在 4-60 个字符之间")
    private String title;

    @NotBlank(message = "物品描述不能为空")
    @Size(min = 10, max = 2000, message = "描述长度需在 10-2000 个字符之间")
    private String description;

    @NotNull(message = "请填写售价")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    @DecimalMax(value = "999999.99", message = "售价超出上限")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    @DecimalMax(value = "999999.99", message = "原价超出上限")
    private BigDecimal originalPrice;

    @NotNull(message = "请选择成色")
    @Min(value = 1, message = "成色取值不合法")
    @Max(value = 5, message = "成色取值不合法")
    private Integer conditionLevel;

    @NotBlank(message = "请填写交易校区")
    @Size(max = 20, message = "校区名称过长")
    private String campus;

    /** 新上传的图片，最多 5 张 */
    private List<MultipartFile> images = new ArrayList<>();

    /** 编辑时保留的旧图（相对路径） */
    private List<String> keepImages = new ArrayList<>();
}