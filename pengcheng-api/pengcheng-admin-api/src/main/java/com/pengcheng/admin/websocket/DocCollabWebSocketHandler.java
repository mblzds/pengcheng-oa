package com.pengcheng.admin.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.service.DocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档协同编辑 WebSocket 处理器
 * 实现多人实时编辑同步：光标位置、内容变更广播
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocCollabWebSocketHandler implements WebSocketHandler {

    private final DocService docService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** docId → 在线编辑用户会话集合 */
    private final Map<Long, Set<WebSocketSession>> docSessions = new ConcurrentHashMap<>();

    /** session → docId 反向映射 */
    private final Map<String, Long> sessionDocMap = new ConcurrentHashMap<>();

    /** session → userId */
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("[DocCollab] 连接建立: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage)) return;

        String payload = ((TextMessage) message).getPayload();
        JsonNode json = objectMapper.readTree(payload);
        String type = json.has("type") ? json.get("type").asText() : "";

        switch (type) {
            case "join" -> handleJoin(session, json);
            case "edit" -> handleEdit(session, json);
            case "cursor" -> handleCursor(session, json);
            case "save" -> handleSave(session, json);
            case "leave" -> handleLeave(session);
            default -> log.debug("[DocCollab] 未知消息类型: {}", type);
        }
    }

    /**
     * 用户加入文档编辑
     */
    private void handleJoin(WebSocketSession session, JsonNode json) throws IOException {
        Long docId = json.get("docId").asLong();
        Long userId = json.has("userId") ? json.get("userId").asLong() : 0L;
        String userName = json.has("userName") ? json.get("userName").asText() : "未知用户";

        sessionDocMap.put(session.getId(), docId);
        sessionUserMap.put(session.getId(), userId);
        docSessions.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);

        Doc doc = docService.getDoc(docId);
        String content = doc != null ? (doc.getContent() != null ? doc.getContent() : "") : "";

        String response = objectMapper.writeValueAsString(Map.of(
                "type", "joined",
                "docId", docId,
                "content", content,
                "onlineUsers", getOnlineUsers(docId)
        ));
        session.sendMessage(new TextMessage(response));

        broadcastToDoc(docId, session, Map.of(
                "type", "user_joined",
                "userId", userId,
                "userName", userName,
                "onlineUsers", getOnlineUsers(docId)
        ));

        log.info("[DocCollab] 用户 {} 加入文档 {}, 当前在线 {} 人",
                userId, docId, docSessions.get(docId).size());
    }

    /**
     * 内容编辑广播
     */
    private void handleEdit(WebSocketSession session, JsonNode json) throws IOException {
        Long docId = sessionDocMap.get(session.getId());
        if (docId == null) return;

        broadcastToDoc(docId, session, Map.of(
                "type", "edit",
                "userId", sessionUserMap.getOrDefault(session.getId(), 0L),
                "content", json.has("content") ? json.get("content").asText() : "",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 光标位置同步
     */
    private void handleCursor(WebSocketSession session, JsonNode json) throws IOException {
        Long docId = sessionDocMap.get(session.getId());
        if (docId == null) return;

        broadcastToDoc(docId, session, Map.of(
                "type", "cursor",
                "userId", sessionUserMap.getOrDefault(session.getId(), 0L),
                "position", json.has("position") ? json.get("position").asInt() : 0,
                "userName", json.has("userName") ? json.get("userName").asText() : ""
        ));
    }

    /**
     * 保存文档
     */
    private void handleSave(WebSocketSession session, JsonNode json) throws IOException {
        Long docId = sessionDocMap.get(session.getId());
        Long userId = sessionUserMap.getOrDefault(session.getId(), 0L);
        if (docId == null) return;

        String content = json.has("content") ? json.get("content").asText() : "";
        Doc doc = new Doc();
        doc.setId(docId);
        doc.setContent(content);
        docService.updateDoc(doc, userId);

        broadcastToDoc(docId, null, Map.of(
                "type", "saved",
                "userId", userId,
                "version", json.has("version") ? json.get("version").asInt() : 0,
                "timestamp", System.currentTimeMillis()
        ));

        log.info("[DocCollab] 用户 {} 保存文档 {}", userId, docId);
    }

    /**
     * 用户离开
     */
    private void handleLeave(WebSocketSession session) throws IOException {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[DocCollab] 传输错误: {}, {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            removeSession(session);
        } catch (IOException e) {
            log.error("[DocCollab] 清理会话异常", e);
        }
        log.debug("[DocCollab] 连接关闭: {}, status: {}", session.getId(), status);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void removeSession(WebSocketSession session) throws IOException {
        Long docId = sessionDocMap.remove(session.getId());
        Long userId = sessionUserMap.remove(session.getId());

        if (docId != null) {
            Set<WebSocketSession> sessions = docSessions.get(docId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    docSessions.remove(docId);
                } else {
                    broadcastToDoc(docId, null, Map.of(
                            "type", "user_left",
                            "userId", userId != null ? userId : 0L,
                            "onlineUsers", getOnlineUsers(docId)
                    ));
                }
            }
        }
    }

    private void broadcastToDoc(Long docId, WebSocketSession exclude, Map<String, Object> message) throws IOException {
        Set<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions == null) return;

        String payload = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(payload);

        for (WebSocketSession s : sessions) {
            if (s.isOpen() && (exclude == null || !s.getId().equals(exclude.getId()))) {
                try {
                    s.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("[DocCollab] 发送消息失败: {}", s.getId());
                }
            }
        }
    }

    private java.util.List<Map<String, Object>> getOnlineUsers(Long docId) {
        Set<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions == null) return java.util.List.of();

        return sessions.stream()
                .filter(WebSocketSession::isOpen)
                .map(s -> Map.<String, Object>of(
                        "userId", sessionUserMap.getOrDefault(s.getId(), 0L),
                        "sessionId", s.getId()
                ))
                .toList();
    }
}
