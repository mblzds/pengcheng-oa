package com.pengcheng.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.message.entity.MessageCategory;
import com.pengcheng.message.mapper.MessageCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 消息智能优先级服务
 * <p>
 * 基于发送人角色、内容关键词、交互频率计算消息优先级，
 * 支持用户对会话进行分类（关注/星标/静音/普通）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePriorityService {

    private final MessageCategoryMapper categoryMapper;

    private static final Pattern URGENT_PATTERN = Pattern.compile(
            "(紧急|urgent|加急|尽快|立即|马上|今天必须|deadline|重要通知|催|限时)", Pattern.CASE_INSENSITIVE);

    private static final Pattern IMPORTANT_PATTERN = Pattern.compile(
            "(签约|合同|付款|审批|客户到访|带看|成交|佣金|投诉|退款|重要)", Pattern.CASE_INSENSITIVE);

    /**
     * 根据内容自动计算消息优先级
     *
     * @return 0=普通 1=重要 2=紧急
     */
    public int calculatePriority(String content, String senderRole) {
        if (content == null) return 0;

        if (URGENT_PATTERN.matcher(content).find()) {
            return 2;
        }

        if ("admin".equals(senderRole) || "manager".equals(senderRole)) {
            if (IMPORTANT_PATTERN.matcher(content).find()) {
                return 2;
            }
            return 1;
        }

        if (IMPORTANT_PATTERN.matcher(content).find()) {
            return 1;
        }

        return 0;
    }

    /**
     * 设置会话分类
     */
    public void setCategory(Long userId, String chatType, Long targetId, String category) {
        MessageCategory existing = categoryMapper.selectOne(new LambdaQueryWrapper<MessageCategory>()
                .eq(MessageCategory::getUserId, userId)
                .eq(MessageCategory::getChatType, chatType)
                .eq(MessageCategory::getTargetId, targetId));

        if (existing != null) {
            existing.setCategory(category);
            categoryMapper.updateById(existing);
        } else {
            MessageCategory mc = new MessageCategory();
            mc.setUserId(userId);
            mc.setChatType(chatType);
            mc.setTargetId(targetId);
            mc.setCategory(category);
            categoryMapper.insert(mc);
        }
    }

    /**
     * 获取用户所有分类
     */
    public List<MessageCategory> getUserCategories(Long userId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<MessageCategory>()
                .eq(MessageCategory::getUserId, userId)
                .orderByDesc(MessageCategory::getUpdatedAt));
    }

    /**
     * 获取指定分类的会话列表
     */
    public List<MessageCategory> getCategoryChats(Long userId, String category) {
        return categoryMapper.selectList(new LambdaQueryWrapper<MessageCategory>()
                .eq(MessageCategory::getUserId, userId)
                .eq(MessageCategory::getCategory, category));
    }

    /**
     * 检查会话是否被静音
     */
    public boolean isMuted(Long userId, String chatType, Long targetId) {
        MessageCategory mc = categoryMapper.selectOne(new LambdaQueryWrapper<MessageCategory>()
                .eq(MessageCategory::getUserId, userId)
                .eq(MessageCategory::getChatType, chatType)
                .eq(MessageCategory::getTargetId, targetId));
        return mc != null && "muted".equals(mc.getCategory());
    }

    /**
     * 删除分类
     */
    public void removeCategory(Long userId, String chatType, Long targetId) {
        categoryMapper.delete(new LambdaQueryWrapper<MessageCategory>()
                .eq(MessageCategory::getUserId, userId)
                .eq(MessageCategory::getChatType, chatType)
                .eq(MessageCategory::getTargetId, targetId));
    }
}
