package com.pengcheng.admin.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MessageWebSocketHandlerIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.dashscope.api-key=test-key",
                "spring.ai.dashscope.agent.api-key=test-key"
        }
)
@DisplayName("Message WebSocket integration")
class MessageWebSocketHandlerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final StandardWebSocketClient client = new StandardWebSocketClient();

    private WebSocketSession senderSession;

    private WebSocketSession receiverSession;

    @AfterEach
    void tearDown() throws IOException {
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.close();
        }
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.close();
        }
    }

    @Test
    @DisplayName("握手通过后，message 端点可完成 connected / ping-pong / 私聊投递")
    void messageEndpointSupportsConnectedPingAndPrivateChat() throws Exception {
        TestClientHandler senderHandler = new TestClientHandler();
        TestClientHandler receiverHandler = new TestClientHandler();

        senderSession = connect(1L, senderHandler);
        receiverSession = connect(2L, receiverHandler);

        assertThat(awaitType(senderHandler.messages, "connected").get("content").asText()).isEqualTo("连接成功");
        assertThat(awaitType(receiverHandler.messages, "connected").get("content").asText()).isEqualTo("连接成功");

        senderSession.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
        assertThat(awaitType(senderHandler.messages, "pong").get("content").asText()).isEqualTo("pong");

        senderSession.sendMessage(new TextMessage("""
                {"type":"chat","receiverId":2,"content":"hello websocket"}
                """.trim()));

        JsonNode chat = awaitType(receiverHandler.messages, "chat");
        assertThat(chat.get("senderId").asLong()).isEqualTo(1L);
        assertThat(chat.get("content").asText()).isEqualTo("hello websocket");
    }

    private WebSocketSession connect(Long userId, TestClientHandler handler) throws Exception {
        String url = "ws://127.0.0.1:" + port + "/ws/message?token=" + userId;
        CompletableFuture<WebSocketSession> future =
                client.execute(handler, new WebSocketHttpHeaders(), java.net.URI.create(url));
        return future.get(5, TimeUnit.SECONDS);
    }

    private JsonNode awaitType(BlockingQueue<String> queue, String type) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            String payload = queue.poll(200, TimeUnit.MILLISECONDS);
            if (payload == null) {
                continue;
            }
            JsonNode json = objectMapper.readTree(payload);
            if (type.equals(json.path("type").asText())) {
                return json;
            }
        }
        throw new AssertionError("Timed out waiting for message type: " + type);
    }

    private static final class TestClientHandler extends TextWebSocketHandler {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            PgVectorStoreAutoConfiguration.class
    })
    @Import(TestWebSocketConfig.class)
    static class TestApp {
    }

    @Configuration
    @EnableWebSocket
    static class TestWebSocketConfig {

        @Bean
        MessageWebSocketHandler messageWebSocketHandler(ObjectMapper objectMapper) {
            return new MessageWebSocketHandler(objectMapper);
        }

        @Bean
        HandshakeInterceptor handshakeInterceptor() {
            return new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                               @NonNull ServerHttpResponse response,
                                               @NonNull WebSocketHandler wsHandler,
                                               @NonNull Map<String, Object> attributes) {
                    if (request instanceof ServletServerHttpRequest servletRequest) {
                        String token = servletRequest.getServletRequest().getParameter("token");
                        if (token != null && !token.isBlank()) {
                            attributes.put("userId", Long.parseLong(token));
                            attributes.put("token", token);
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void afterHandshake(@NonNull ServerHttpRequest request,
                                           @NonNull ServerHttpResponse response,
                                           @NonNull WebSocketHandler wsHandler,
                                           @Nullable Exception exception) {
                }
            };
        }

        @Bean
        WebSocketConfigurer webSocketConfigurer(MessageWebSocketHandler handler,
                                                HandshakeInterceptor interceptor) {
            return new WebSocketConfigurer() {
                @Override
                public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                    registry.addHandler(handler, "/ws/message")
                            .addInterceptors(interceptor)
                            .setAllowedOrigins("*");
                }
            };
        }
    }
}
