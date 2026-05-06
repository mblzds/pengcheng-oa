package com.pengcheng.ai.config;

import com.pengcheng.ai.function.CommissionCalcFunction;
import com.pengcheng.ai.function.ReportQueryFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("SpringAiConfig")
class SpringAiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SpringAiConfig.class, TestSupportConfig.class);

    @Test
    @DisplayName("存在外部 EmbeddingModel 时仍能创建 VectorStore")
    void createsVectorStoreWhenExternalEmbeddingModelExists() {
        contextRunner
                .withBean("dashscopeEmbeddingModel", EmbeddingModel.class, () -> mock(EmbeddingModel.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VectorStore.class);
                });
    }

    @Configuration
    static class TestSupportConfig {

        @Bean
        ReportQueryFunction reportQueryFunction() {
            return mock(ReportQueryFunction.class);
        }

        @Bean
        CommissionCalcFunction commissionCalcFunction() {
            return mock(CommissionCalcFunction.class);
        }
    }
}
