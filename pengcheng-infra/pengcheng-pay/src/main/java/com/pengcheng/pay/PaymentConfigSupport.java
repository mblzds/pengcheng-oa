package com.pengcheng.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pengcheng.system.helper.SystemConfigHelper;

final class PaymentConfigSupport {

    private static final String PAYMENT_GROUP = "payment";

    private PaymentConfigSupport() {
    }

    static JsonNode getProviderConfig(SystemConfigHelper configHelper, String providerGroup) {
        JsonNode paymentConfig = configHelper.getConfig(PAYMENT_GROUP);
        JsonNode legacyConfig = configHelper.getConfig(providerGroup);
        if (paymentConfig != null) {
            JsonNode providerConfig = paymentConfig.get(providerGroup);
            if (providerConfig != null && !providerConfig.isNull()) {
                if (providerConfig.isObject() && legacyConfig != null && legacyConfig.isObject()) {
                    ObjectNode mergedConfig = ((ObjectNode) providerConfig).deepCopy();
                    legacyConfig.fields().forEachRemaining(entry -> {
                        if (!mergedConfig.has(entry.getKey()) || mergedConfig.get(entry.getKey()).isNull()) {
                            mergedConfig.set(entry.getKey(), entry.getValue());
                        }
                    });
                    return mergedConfig;
                }
                return providerConfig;
            }
        }
        return legacyConfig;
    }

    static String getProviderString(SystemConfigHelper configHelper, String providerGroup, String key, String defaultValue) {
        JsonNode providerConfig = getProviderConfig(configHelper, providerGroup);
        if (providerConfig != null) {
            JsonNode value = providerConfig.get(key);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return defaultValue;
    }

    static boolean getBoolean(JsonNode config, String key, boolean defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        JsonNode value = config.get(key);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asBoolean(defaultValue);
    }

    static String getString(JsonNode config, String key) {
        if (config == null) {
            return "";
        }
        JsonNode value = config.get(key);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText();
    }
}
