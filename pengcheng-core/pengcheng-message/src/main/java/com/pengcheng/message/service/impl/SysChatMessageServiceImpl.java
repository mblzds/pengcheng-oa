package com.pengcheng.message.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.message.entity.SysChatMessage;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.message.mapper.SysChatMessageMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.message.service.SysChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysChatMessageServiceImpl implements SysChatMessageService {

    private final SysChatMessageMapper chatMessageMapper;
    private final SysUserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysChatMessage send(SysChatMessage message) {
        Long senderId = StpUtil.getLoginIdAsLong();
        SysUser sender = userMapper.selectById(senderId);
        
        message.setSenderId(senderId);
        message.setSenderName(sender != null ? sender.getNickname() : "未知用户");
        message.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        message.setIsRead(0);
        message.setRecalled(0);
        message.setDelivered(0);
        message.setReadFlag(0);
        message.setSeq(nextSeq());
        message.setSendTime(LocalDateTime.now());
        
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public Page<SysChatMessage> getChatHistory(Long userId, Long targetId, Integer page, Integer pageSize) {
        Page<SysChatMessage> pageParam = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        // 查询两人之间的所有消息
        wrapper.and(w -> w
                .and(w1 -> w1.eq(SysChatMessage::getSenderId, userId).eq(SysChatMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(SysChatMessage::getSenderId, targetId).eq(SysChatMessage::getReceiverId, userId))
        );
        wrapper.orderByDesc(SysChatMessage::getSendTime);
        
        return chatMessageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<SysChatMessage> getRecentContacts(Long userId) {
        // 查询用户最近的消息
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(SysChatMessage::getSenderId, userId)
                .or()
                .eq(SysChatMessage::getReceiverId, userId)
        );
        wrapper.orderByDesc(SysChatMessage::getSendTime);
        wrapper.last("LIMIT 100");
        
        List<SysChatMessage> messages = chatMessageMapper.selectList(wrapper);
        
        // 按联系人分组，取最新一条
        Map<Long, SysChatMessage> contactMap = new LinkedHashMap<>();
        for (SysChatMessage msg : messages) {
            Long contactId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (contactId > 0 && !contactMap.containsKey(contactId)) {
                contactMap.put(contactId, msg);
            }
        }
        
        return new ArrayList<>(contactMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long senderId) {
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId);
        wrapper.eq(SysChatMessage::getSenderId, senderId);
        wrapper.eq(SysChatMessage::getIsRead, 0);
        wrapper.set(SysChatMessage::getIsRead, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return chatMessageMapper.selectUnreadCount(userId);
    }

    @Override
    public int getUnreadCountWithUser(Long userId, Long senderId) {
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId);
        wrapper.eq(SysChatMessage::getSenderId, senderId);
        wrapper.eq(SysChatMessage::getIsRead, 0);
        return Math.toIntExact(chatMessageMapper.selectCount(wrapper));
    }
    
    @Override
    public SysChatMessage getLatestMessage(Long userId, Long targetId) {
        return chatMessageMapper.selectLatestMessage(userId, targetId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId, Long targetId) {
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(SysChatMessage::getSenderId, userId).eq(SysChatMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(SysChatMessage::getSenderId, targetId).eq(SysChatMessage::getReceiverId, userId))
        );
        chatMessageMapper.delete(wrapper);
    }

    private static final long RECALL_WINDOW_MS = 2 * 60 * 1000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recallMessage(Long messageId, Long userId) {
        SysChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || !message.getSenderId().equals(userId)) {
            return false;
        }
        if (System.currentTimeMillis() - java.sql.Timestamp.valueOf(message.getSendTime()).getTime() > RECALL_WINDOW_MS) {
            return false;
        }
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getId, messageId)
               .set(SysChatMessage::getRecalled, 1)
               .set(SysChatMessage::getContent, "此消息已撤回");
        chatMessageMapper.update(null, wrapper);
        return true;
    }

    @Override
    public Page<SysChatMessage> searchMessages(Long userId, String keyword, Integer page, Integer pageSize) {
        Page<SysChatMessage> pageParam = new Page<>(page, pageSize);
        QueryWrapper<SysChatMessage> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
                .eq("sender_id", userId)
                .or()
                .eq("receiver_id", userId)
        );
        wrapper.eq("recalled", 0);
        wrapper.apply("MATCH(content) AGAINST({0} IN BOOLEAN MODE)", keyword);
        wrapper.orderByDesc("send_time");
        return chatMessageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long nextSeq() {
        jdbcTemplate.update("UPDATE sys_chat_sequence SET current_seq = current_seq + 1 WHERE scope = 'global'");
        Long seq = jdbcTemplate.queryForObject("SELECT current_seq FROM sys_chat_sequence WHERE scope = 'global'", Long.class);
        return seq != null ? seq : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ackMessage(Long messageId, Long userId) {
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getId, messageId)
               .eq(SysChatMessage::getReceiverId, userId)
               .set(SysChatMessage::getDelivered, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public List<SysChatMessage> getOfflineMessages(Long userId, Long lastSeq) {
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId)
               .gt(SysChatMessage::getSeq, lastSeq)
               .eq(SysChatMessage::getDelivered, 0)
               .eq(SysChatMessage::getRecalled, 0)
               .orderByAsc(SysChatMessage::getSeq)
               .last("LIMIT 200");
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOfflineDelivered(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId)
               .in(SysChatMessage::getId, messageIds)
               .set(SysChatMessage::getDelivered, 1);
        chatMessageMapper.update(null, wrapper);
    }
}
