package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.dto.PasswordDTO;
import com.campus.secondhand.dto.ProfileDTO;
import com.campus.secondhand.dto.RegisterDTO;
import com.campus.secondhand.entity.Role;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.UserRole;
import com.campus.secondhand.exception.BusinessException;
import com.campus.secondhand.mapper.RoleMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.UserRoleMapper;
import com.campus.secondhand.mq.MailMessage;
import com.campus.secondhand.mq.MqProducer;
import com.campus.secondhand.security.LoginUser;
import com.campus.secondhand.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务：注册、资料、改密、头像、角色授予与后台用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileUtils fileUtils;
    private final MqProducer mqProducer;

    /**
     * 注册：唯一性校验 + 密码加密 + 默认角色 + 异步欢迎邮件
     */
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        Long sameName = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (sameName != null && sameName > 0) {
            throw new BusinessException("登录名已被注册，请更换");
        }
        Long sameEmail = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail()));
        if (sameEmail != null && sameEmail > 0) {
            throw new BusinessException("邮箱已被注册，请更换");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setCampus(dto.getCampus());
        user.setStudentNo(dto.getStudentNo());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setAvatar("/upload/demo/avatar-01.jpg");
        user.setStatus(1);
        user.setCreditScore(100);
        userMapper.insert(user);

        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, Constants.ROLE_USER));
        if (userRole == null) {
            throw new BusinessException("系统角色数据缺失，请联系管理员");
        }
        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        Map<String, Object> params = new HashMap<>();
        params.put("nickname", user.getNickname());
        params.put("username", user.getUsername());
        mqProducer.sendMail(new MailMessage(user.getEmail(), user.getNickname(),
                "欢迎加入校园二手闲置交易平台", "welcome", params, user.getId()));
        log.info("新用户注册成功：{}（id={}）", user.getUsername(), user.getId());
        return user;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 更新个人资料
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileDTO dto) {
        Long sameEmail = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail()).ne(User::getId, userId));
        if (sameEmail != null && sameEmail > 0) {
            throw new BusinessException("该邮箱已被其他账号使用");
        }
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setEmail(dto.getEmail());
        update.setPhone(dto.getPhone());
        update.setCampus(dto.getCampus());
        update.setStudentNo(dto.getStudentNo());
        userMapper.updateById(update);
        refreshPrincipal(userId);
    }

    /**
     * 修改密码：校验原密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordDTO dto) {
        User user = getById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword())));
        log.info("用户 {} 修改了登录密码", user.getUsername());
    }

    /**
     * 更新头像，旧头像文件一并清理
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = getById(userId);
        String path = fileUtils.save(file, "avatar");
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId).set(User::getAvatar, path));
        if (user.getAvatar() != null && user.getAvatar().startsWith("/upload/avatar/")) {
            fileUtils.delete(user.getAvatar());
        }
        refreshPrincipal(userId);
        return path;
    }

    /**
     * 发布过物品的用户自动升级为卖家角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void upgradeToSeller(Long userId) {
        Role sellerRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, Constants.ROLE_SELLER));
        if (sellerRole == null) {
            return;
        }
        Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, sellerRole.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        UserRole relation = new UserRole();
        relation.setUserId(userId);
        relation.setRoleId(sellerRole.getId());
        userRoleMapper.insert(relation);
        log.info("用户 {} 发布物品成功，已自动授予卖家角色", userId);
        refreshPrincipal(userId);
    }

    /**
     * 后台用户分页：支持昵称/登录名/邮箱搜索与角色筛选
     */
    public IPage<User> pageUsers(String keyword, Long roleId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (roleId != null) {
            wrapper.inSql(User::getId, "SELECT user_id FROM tb_user_role WHERE role_id = " + roleId);
        }
        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(this::fillRoleNames);
        return page;
    }

    /**
     * 启用/禁用账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long userId) {
        User user = getById(userId);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("超级管理员账号不允许禁用");
        }
        int target = user.getStatus() != null && user.getStatus() == 1 ? 0 : 1;
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId).set(User::getStatus, target));
        log.info("账号 {} 状态被管理员调整为 {}", user.getUsername(), target == 1 ? "启用" : "禁用");
    }

    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    /**
     * 用户拥有的角色 id 列表（角色分配页回显）
     */
    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    /**
     * 重置用户角色（保留后台分配结果）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRoles(Long userId, List<Long> roleIds) {
        getById(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> distinct = roleIds == null ? new ArrayList<>()
                : roleIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BusinessException("至少保留一个角色");
        }
        for (Long roleId : distinct) {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
        refreshPrincipal(userId);
    }

    /**
     * 同步刷新登录态中的用户信息（改资料/改头像后页头立即生效）
     */
    public void refreshPrincipal(Long userId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser loginUser) || !userId.equals(loginUser.getId())) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setEmail(user.getEmail());
        loginUser.setCampus(user.getCampus());
    }

    private void fillRoleNames(User user) {
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        user.setRoleNames(roles.stream().map(Role::getName).collect(Collectors.joining("、")));
    }
}