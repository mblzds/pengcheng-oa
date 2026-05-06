package com.pengcheng.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * 联盟商系统移动端适配配置
 * <p>
 * 确保接口兼容 UniApp 小程序和主流移动端浏览器：
 * <ul>
 *   <li>设置 Cache-Control 避免移动端缓存敏感数据</li>
 *   <li>设置 X-Content-Type-Options 防止 MIME 嗅探</li>
 *   <li>设置 Content-Type 为 UTF-8 确保中文正确显示</li>
 * </ul>
 */
@Configuration
public class MobileAdaptConfig {

    @Bean
    public FilterRegistrationBean<MobileResponseFilter> mobileResponseFilter() {
        FilterRegistrationBean<MobileResponseFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MobileResponseFilter());
        registration.addUrlPatterns("/web/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 移动端响应头过滤器
     */
    static class MobileResponseFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            // 禁止缓存敏感业务数据
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            // 防止 MIME 类型嗅探
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            // 确保 UTF-8 编码
            httpResponse.setCharacterEncoding("UTF-8");
            chain.doFilter(request, response);
        }
    }
}
