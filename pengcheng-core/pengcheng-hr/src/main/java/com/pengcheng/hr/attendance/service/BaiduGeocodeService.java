package com.pengcheng.hr.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 百度地图逆地理编码服务（GPS 经纬度 → 中文地址）。
 *
 * AK 复用「系统配置 → 考勤设置 → 百度地图 AK」(attendance.baiduMapAk)，与管理端
 * BaiduMapPicker / AttendanceConfig 共用同一份配置，运维改 AK 不必发版。
 *
 * 失败 / AK 未配置 / 网络异常都不抛错——调用方根据返回 null 自行降级，签到不应该
 * 因为地址翻译失败而落库失败。
 *
 * 注意：百度 AK 需在控制台勾选 "WebService API → 逆地理编码" 才能调本接口；
 * 仅勾选 "JavaScript API" 时管理端选点能用，但本接口会返回 status:240。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduGeocodeService {

    private static final String GEOCODE_URL = "https://api.map.baidu.com/reverse_geocoding/v3/";
    private static final int TIMEOUT_MS = 2000;

    private final SystemConfigHelper systemConfigHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;

    /**
     * 把 WGS-84 经纬度翻译成中文地址（如「广东省广州市天河区xx路 - xx大厦」）。
     * @return formatted_address；失败 / 未配置 AK / 经纬度无效时返回 null
     */
    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;
        // 优先读「服务端 AK」字段；为空时兼容老配置回退到 baiduMapAk
        // —— 但旧字段一般是浏览器端 AK，调 WebService 会被 240，仅作过渡兼容
        String ak = systemConfigHelper.getAttendanceBaiduMapServerAk();
        if (ak == null || ak.isBlank()) {
            ak = systemConfigHelper.getAttendanceBaiduMapAk();
        }
        if (ak == null || ak.isBlank()) {
            log.debug("百度地图服务端 AK 未在系统配置中设置，跳过逆地理编码");
            return null;
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                    .queryParam("ak", ak)
                    .queryParam("output", "json")
                    // 小程序 gcj02ToWgs84 后上报，与 sign_in_record.latitude/longitude 坐标系一致
                    .queryParam("coordtype", "wgs84ll")
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("extensions_poi", "0")
                    .build()
                    .encode()
                    .toUri();

            String body = getRestTemplate().getForObject(uri, String.class);
            if (body == null || body.isBlank()) return null;
            JsonNode root = objectMapper.readTree(body);
            int status = root.path("status").asInt(-1);
            if (status != 0) {
                // 200=AK 不存在 / 240=APP 服务被禁用（未勾选逆地理服务）/ 210=Referer 校验失败
                log.warn("百度逆地理失败 status={} message={}", status, root.path("message").asText());
                return null;
            }
            JsonNode result = root.path("result");
            String formatted = result.path("formatted_address").asText(null);
            String business = result.path("business").asText(null);
            if (formatted == null || formatted.isBlank()) return business;
            if (business != null && !business.isBlank()) {
                return formatted + " (" + business + ")";
            }
            return formatted;
        } catch (Exception e) {
            log.warn("调用百度逆地理编码异常：{}", e.getMessage());
            return null;
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(TIMEOUT_MS);
            factory.setReadTimeout(TIMEOUT_MS);
            restTemplate = new RestTemplate(factory);
        }
        return restTemplate;
    }
}
