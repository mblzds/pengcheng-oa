package com.pengcheng.ai.config;

import com.pengcheng.ai.function.CommissionCalcFunction;
import com.pengcheng.ai.function.ReportQueryFunction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI Alibaba 配置类
 * <p>
 * 配置 ChatClient 和 EmbeddingModel，集成通义千问大模型。
 * DashScope API Key 和模型参数通过 application.yml 配置。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class SpringAiConfig {

    /**
     * 构建 ChatClient，用于 AI 对话、Function Calling 等场景。
     * ChatModel 由 spring-ai-alibaba-starter 自动装配。
     */
    @Bean
    public ChatClient chatClient(ObjectProvider<ChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            chatModel = prompt -> new ChatResponse(List.of(
                    new Generation(new AssistantMessage("AI 服务未配置，已返回降级结果。"))
            ));
        }
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个房产销售管理系统的AI助手，负责协助处理客户判重、佣金计算、报表查询和知识库问答等任务。请用中文回答。")
                .build();
    }

    /** 无可用 EmbeddingModel 时，按需构造一个轻量兜底实现 */
    private EmbeddingModel createFallbackEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> results = new ArrayList<>();
                List<String> inputs = request.getInstructions();
                for (int i = 0; i < inputs.size(); i++) {
                    results.add(new Embedding(toVector(inputs.get(i)), i));
                }
                return new EmbeddingResponse(results);
            }
            @Override
            public float[] embed(Document document) {
                return toVector(document.getText());
            }
            @Override
            public int dimensions() {
                return 8;
            }
            private float[] toVector(String text) {
                float[] vector = new float[8];
                if (text == null || text.isBlank()) return vector;
                byte[] bytes = text.getBytes();
                for (int i = 0; i < bytes.length; i++) {
                    int idx = i % vector.length;
                    vector[idx] += (bytes[i] & 0xFF) / 255.0f;
                }
                return vector;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore fallbackVectorStore(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        EmbeddingModel embeddingModel = embeddingModelProvider.orderedStream()
                .findFirst()
                .orElseGet(this::createFallbackEmbeddingModel);
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 注册报表查询 FunctionToolCallback，供 AI Function Calling 使用。
     */
    @Bean("reportQueryFunctionCallback")
    public FunctionToolCallback<ReportQueryFunction.Request, ReportQueryFunction.Response> reportQueryFunctionCallback(
            ReportQueryFunction reportQueryFunction) {
        return FunctionToolCallback.builder("reportQueryFunction", reportQueryFunction)
                .description("查询业务报表数据。queryType支持: overview(业务概览), project_ranking(项目排行), alliance_ranking(联盟商排行), funnel(转化漏斗)。startDate和endDate格式为yyyy-MM-dd。")
                .inputType(ReportQueryFunction.Request.class)
                .build();
    }

    /**
     * 注册佣金计算 FunctionToolCallback，供 AI Function Calling 使用。
     */
    @Bean("commissionCalcFunctionCallback")
    public FunctionToolCallback<CommissionCalcFunction.Request, CommissionCalcFunction.Response> commissionCalcFunctionCallback(
            CommissionCalcFunction commissionCalcFunction) {
        return FunctionToolCallback.builder("commissionCalcFunction", commissionCalcFunction)
                .description("根据项目佣金规则自动计算各项佣金。projectId为项目ID，dealAmount为成交金额，dealCount为该项目累计成交套数（含本次）。")
                .inputType(CommissionCalcFunction.Request.class)
                .build();
    }
}
