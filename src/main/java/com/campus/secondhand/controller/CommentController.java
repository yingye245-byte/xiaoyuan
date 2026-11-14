package com.campus.secondhand.controller;

import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.service.CommentService;
import com.campus.secondhand.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * 留言控制器：物品详情页提交留言/回复、删除自己的留言
 */
@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comment")
    public String add(@Valid @ModelAttribute("commentDTO") CommentDTO dto,
                      BindingResult bindingResult,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/item/" + dto.getItemId();
        }
        try {
            commentService.add(dto, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "留言成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/item/" + dto.getItemId() + "#comments";
    }

    @PostMapping("/comment/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Long itemId,
                         RedirectAttributes redirectAttributes) {
        try {
            commentService.delete(id, SecurityUtils.getUserId());
            redirectAttributes.addFlashAttribute("msg", "留言已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/item/" + itemId + "#comments";
    }
}