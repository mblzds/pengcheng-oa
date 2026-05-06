package com.pengcheng.system.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.mapper.TodoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 待办事项服务
 * 支持手动创建、AI 从聊天消息提取待办
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoMapper todoMapper;

    private static final Pattern TASK_PATTERN = Pattern.compile(
        "(记得|别忘了|需要|务必|请|帮我|安排|提醒|跟进|回复|准备|联系|通知|确认|处理)(.{2,50})");

    public List<Todo> getUserTodos(Long userId, Integer status) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
            .eq(Todo::getUserId, userId);
        if (status != null) {
            wrapper.eq(Todo::getStatus, status);
        }
        wrapper.orderByAsc(Todo::getStatus)
               .orderByDesc(Todo::getPriority)
               .orderByAsc(Todo::getDueDate);
        return todoMapper.selectList(wrapper);
    }

    public Todo createTodo(Todo todo) {
        if (todo.getStatus() == null) todo.setStatus(0);
        if (todo.getPriority() == null) todo.setPriority(0);
        todoMapper.insert(todo);
        return todo;
    }

    public void updateTodo(Todo todo) {
        todoMapper.updateById(todo);
    }

    public void completeTodo(Long id, Long userId) {
        Todo todo = todoMapper.selectById(id);
        if (todo != null && todo.getUserId().equals(userId)) {
            todo.setStatus(2);
            todo.setCompletedAt(LocalDateTime.now());
            todoMapper.updateById(todo);
        }
    }

    public void cancelTodo(Long id, Long userId) {
        Todo todo = todoMapper.selectById(id);
        if (todo != null && todo.getUserId().equals(userId)) {
            todo.setStatus(3);
            todoMapper.updateById(todo);
        }
    }

    public void deleteTodo(Long id) {
        todoMapper.deleteById(id);
    }

    public int countPending(Long userId) {
        return todoMapper.countPending(userId);
    }

    /**
     * 从聊天消息中提取待办（规则引擎版本，不依赖 LLM）
     * 后续可替换为 LLM 分析
     */
    public List<Todo> extractFromMessage(String messageContent, Long userId, Long sourceId, String chatType) {
        List<Todo> todos = new java.util.ArrayList<>();

        Matcher taskMatcher = TASK_PATTERN.matcher(messageContent);
        while (taskMatcher.find()) {
            String taskContent = taskMatcher.group(2).trim();
            if (taskContent.length() < 3) continue;

            Todo todo = new Todo();
            todo.setUserId(userId);
            todo.setTitle(taskContent);
            todo.setDescription("从聊天消息中提取：" + messageContent);
            todo.setSourceType("chat");
            todo.setSourceId(sourceId);
            todo.setSourceChatType(chatType);
            todo.setPriority(detectPriority(messageContent));
            todo.setStatus(0);

            todos.add(todo);
        }

        return todos;
    }

    private int detectPriority(String content) {
        if (content.contains("紧急") || content.contains("马上") || content.contains("立刻") || content.contains("尽快")) {
            return 2;
        }
        if (content.contains("重要") || content.contains("务必") || content.contains("一定")) {
            return 1;
        }
        return 0;
    }
}
