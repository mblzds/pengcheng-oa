package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.service.AiLlmService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 待办事项管理接口
 */
@Slf4j
@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    @Autowired(required = false)
    private AiLlmService aiLlmService;

    @GetMapping("/list")
    public Result<List<Todo>> list(@RequestParam(required = false) Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(todoService.getUserTodos(userId, status));
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(Map.of("pending", todoService.countPending(userId)));
    }

    @PostMapping("/create")
    public Result<Todo> create(@RequestBody Todo todo) {
        todo.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(todoService.createTodo(todo));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Todo todo) {
        todoService.updateTodo(todo);
        return Result.ok();
    }

    @PostMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        todoService.completeTodo(id, userId);
        return Result.ok();
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        todoService.cancelTodo(id, userId);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return Result.ok();
    }

    @PostMapping("/extract")
    public Result<List<Todo>> extractFromMessage(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String content = (String) body.get("content");
        Long sourceId = body.get("sourceId") != null ? Long.valueOf(body.get("sourceId").toString()) : null;
        String chatType = (String) body.get("chatType");
        boolean useLlm = Boolean.TRUE.equals(body.get("useLlm"));

        List<Todo> todos;
        if (useLlm && aiLlmService != null) {
            todos = extractWithLlm(content, userId, sourceId, chatType);
        } else {
            todos = todoService.extractFromMessage(content, userId, sourceId, chatType);
        }
        for (Todo todo : todos) {
            todoService.createTodo(todo);
        }
        return Result.ok(todos);
    }

    private List<Todo> extractWithLlm(String content, Long userId, Long sourceId, String chatType) {
        try {
            String json = aiLlmService.extractTodosFromMessage(content);
            if (json == null || json.isBlank()) return List.of();
            String cleaned = json.trim();
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']');
            if (start < 0 || end < 0) return List.of();
            cleaned = cleaned.substring(start, end + 1);

            ObjectMapper om = new ObjectMapper();
            List<Map<String, Object>> items = om.readValue(cleaned, new TypeReference<>() {});
            List<Todo> todos = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Todo todo = new Todo();
                todo.setUserId(userId);
                todo.setTitle((String) item.getOrDefault("title", ""));
                todo.setSourceType("chat");
                todo.setSourceId(sourceId);
                todo.setSourceChatType(chatType);
                todo.setPriority(item.get("priority") != null ? ((Number) item.get("priority")).intValue() : 0);
                todo.setStatus(0);
                todo.setDescription("AI 从聊天消息中提取：" + content);
                todos.add(todo);
            }
            return todos;
        } catch (Exception e) {
            log.warn("[Todo] LLM 提取失败，回退正则: {}", e.getMessage());
            return todoService.extractFromMessage(content, userId, sourceId, chatType);
        }
    }
}
