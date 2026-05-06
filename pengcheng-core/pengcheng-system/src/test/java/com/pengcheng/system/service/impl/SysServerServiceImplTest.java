package com.pengcheng.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.crypto.CryptoService;
import com.pengcheng.system.entity.SysServer;
import com.pengcheng.system.mapper.SysServerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrefixCryptoService implements CryptoService {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPublicKey() {
        return "public";
    }

    @Override
    public String getEncryptedAesKey() {
        return "aes";
    }

    @Override
    public String decrypt(String encryptedData) {
        return encryptedData != null && encryptedData.startsWith("enc(") && encryptedData.endsWith(")")
                ? encryptedData.substring(4, encryptedData.length() - 1)
                : encryptedData;
    }

    @Override
    public String encrypt(String data) {
        return data == null ? null : "enc(" + data + ")";
    }

    @Override
    public String encryptResponse(String data) {
        return data;
    }

    @Override
    public void refreshKeyPair() {
    }
}

@DisplayName("SysServerServiceImpl 敏感字段处理")
class SysServerServiceImplTest {

    @Test
    @DisplayName("save：密码/私钥/口令会加密后入库")
    void save_encryptsSensitiveFields() {
        SysServerMapper mapper = mock(SysServerMapper.class);
        when(mapper.insert(any(SysServer.class))).thenReturn(1);

        SysServerServiceImpl service = new SysServerServiceImpl(new PrefixCryptoService());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        SysServer server = server("plain-password", "plain-private-key", "plain-passphrase");
        boolean saved = service.save(server);

        assertThat(saved).isTrue();
        assertThat(server.getPassword()).isEqualTo("enc(plain-password)");
        assertThat(server.getPrivateKey()).isEqualTo("enc(plain-private-key)");
        assertThat(server.getPassphrase()).isEqualTo("enc(plain-passphrase)");
    }

    @Test
    @DisplayName("updateById：非空敏感字段加密，空字段保持不更新语义")
    void updateById_encryptsProvidedFieldsAndSkipsBlankSensitiveFields() {
        SysServerMapper mapper = mock(SysServerMapper.class);
        when(mapper.updateById(any(SysServer.class))).thenReturn(1);

        SysServerServiceImpl service = new SysServerServiceImpl(new PrefixCryptoService());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        SysServer server = server("new-password", "  ", "");
        boolean updated = service.updateById(server);

        assertThat(updated).isTrue();
        assertThat(server.getPassword()).isEqualTo("enc(new-password)");
        assertThat(server.getPrivateKey()).isNull();
        assertThat(server.getPassphrase()).isNull();
    }

    @Test
    @DisplayName("getById：读取时会解密敏感字段")
    void getById_decryptsSensitiveFields() {
        SysServerMapper mapper = mock(SysServerMapper.class);
        when(mapper.selectById(1L)).thenReturn(server("enc(db-password)", "enc(db-private-key)", "enc(db-passphrase)"));

        SysServerServiceImpl service = new SysServerServiceImpl(new PrefixCryptoService());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        SysServer server = service.getById(1L);

        assertThat(server.getPassword()).isEqualTo("db-password");
        assertThat(server.getPrivateKey()).isEqualTo("db-private-key");
        assertThat(server.getPassphrase()).isEqualTo("db-passphrase");
    }

    @Test
    @DisplayName("pageList：列表结果继续脱敏")
    void pageList_masksSensitiveFields() {
        SysServerMapper mapper = mock(SysServerMapper.class);
        SysServer masked = server("enc(db-password)", "enc(db-private-key)", "enc(db-passphrase)");
        masked.setId(1L);

        when(mapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<SysServer> page = invocation.getArgument(0);
            page.setRecords(List.of(masked));
            page.setTotal(1);
            return page;
        });

        SysServerServiceImpl service = new SysServerServiceImpl(new PrefixCryptoService());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        Page<SysServer> result = service.pageList(new Page<>(1, 10), null, 1);

        assertThat(result.getRecords()).hasSize(1);
        SysServer server = result.getRecords().get(0);
        assertThat(server.getPassword()).isNull();
        assertThat(server.getPrivateKey()).isNull();
        assertThat(server.getPassphrase()).isNull();
    }

    @Test
    @DisplayName("testConnection(id)：使用解密后的凭据")
    void testConnection_usesDecryptedCredentialsFromGetById() {
        SysServerMapper mapper = mock(SysServerMapper.class);
        SysServer encrypted = server("enc(db-password)", "enc(db-private-key)", "enc(db-passphrase)");
        encrypted.setId(8L);
        encrypted.setHost("127.0.0.1");
        encrypted.setPort(22);
        encrypted.setUsername("root");
        encrypted.setAuthType(1);
        when(mapper.selectById(8L)).thenReturn(encrypted);

        ConnectionCapturingSysServerServiceImpl service =
                new ConnectionCapturingSysServerServiceImpl(new PrefixCryptoService());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        boolean connected = service.testConnection(8L);

        assertThat(connected).isTrue();
        assertThat(service.capturedPassword).isEqualTo("db-password");
        assertThat(service.capturedPrivateKey).isEqualTo("db-private-key");
        assertThat(service.capturedPassphrase).isEqualTo("db-passphrase");
    }

    private static SysServer server(String password, String privateKey, String passphrase) {
        SysServer server = new SysServer();
        server.setPassword(password);
        server.setPrivateKey(privateKey);
        server.setPassphrase(passphrase);
        return server;
    }

    private static final class ConnectionCapturingSysServerServiceImpl extends SysServerServiceImpl {

        private String capturedPassword;
        private String capturedPrivateKey;
        private String capturedPassphrase;

        private ConnectionCapturingSysServerServiceImpl(CryptoService cryptoService) {
            super(cryptoService);
        }

        @Override
        public boolean testConnection(String host, Integer port, String username, Integer authType,
                                      String password, String privateKey, String passphrase) {
            this.capturedPassword = password;
            this.capturedPrivateKey = privateKey;
            this.capturedPassphrase = passphrase;
            return true;
        }
    }
}
