package com.pengcheng.admin.websocket;

import com.pengcheng.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * 注册业务WebSocket处理器，容器配置由 pengcheng-websocket 模块提供
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final SshWebSocketHandler sshWebSocketHandler;
    private final DocCollabWebSocketHandler docCollabWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(MessageWebSocketHandler messageWebSocketHandler,
                          SshWebSocketHandler sshWebSocketHandler,
                          DocCollabWebSocketHandler docCollabWebSocketHandler,
                          WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.sshWebSocketHandler = sshWebSocketHandler;
        this.docCollabWebSocketHandler = docCollabWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 消息 WebSocket
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        // SSH 终端 WebSocket
        registry.addHandler(sshWebSocketHandler, "/ws/ssh")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        // 文档协同编辑 WebSocket
        registry.addHandler(docCollabWebSocketHandler, "/ws/doc")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
