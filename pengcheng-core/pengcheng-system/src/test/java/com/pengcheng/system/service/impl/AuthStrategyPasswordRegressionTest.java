package com.pengcheng.system.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("认证策略默认密码回归")
class AuthStrategyPasswordRegressionTest {

    @Test
    @DisplayName("自动注册流程不再写死默认密码 123456")
    void autoRegisterStrategiesDoNotUseHardCodedDefaultPassword() throws IOException {
        Path moduleDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path authDir = moduleDir.getParent().resolve("pengcheng-auth")
                .resolve("src/main/java/com/pengcheng/auth/strategy");

        assertSourceDoesNotContainDefaultPassword(authDir.resolve("MiniProgramLoginStrategy.java"));
        assertSourceDoesNotContainDefaultPassword(authDir.resolve("SmsCodeLoginStrategy.java"));
    }

    private static void assertSourceDoesNotContainDefaultPassword(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);
        assertThat(sourceFile).exists();
        assertThat(source).doesNotContain("BCrypt.hashpw(\"123456\")");
        assertThat(source).doesNotContain("// 默认密码");
    }
}
