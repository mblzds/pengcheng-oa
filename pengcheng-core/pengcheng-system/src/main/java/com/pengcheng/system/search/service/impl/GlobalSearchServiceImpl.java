package com.pengcheng.system.search.service.impl;

import com.pengcheng.system.search.dto.SearchResultDTO;
import com.pengcheng.system.search.dto.SearchResultDTO.CategoryResult;
import com.pengcheng.system.search.dto.SearchResultDTO.SearchItem;
import com.pengcheng.system.search.mapper.SearchHistoryMapper;
import com.pengcheng.system.search.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局智能搜索服务实现
 * <p>
 * 使用 MySQL FULLTEXT 索引进行多表联合搜索，
 * 每个分类独立查询后聚合返回。后续可接入 RAG 语义搜索增强。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final SearchHistoryMapper historyMapper;

    @Override
    public SearchResultDTO search(String keyword, String scope, Long userId, Long deptId, int maxPerCategory) {
        long start = System.currentTimeMillis();
        String booleanKeyword = toBooleanModeQuery(keyword);

        List<CategoryResult> categories = new ArrayList<>();

        if ("all".equals(scope) || "customer".equals(scope)) {
            categories.add(searchCustomers(booleanKeyword, keyword, userId, deptId, maxPerCategory));
        }
        if ("all".equals(scope) || "project".equals(scope)) {
            categories.add(searchProjects(booleanKeyword, keyword, maxPerCategory));
        }
        if ("all".equals(scope) || "alliance".equals(scope)) {
            categories.add(searchAlliances(booleanKeyword, keyword, maxPerCategory));
        }
        if ("all".equals(scope) || "chat".equals(scope)) {
            categories.add(searchChats(booleanKeyword, keyword, userId, maxPerCategory));
        }
        if ("all".equals(scope) || "notice".equals(scope)) {
            categories.add(searchNotices(booleanKeyword, keyword, maxPerCategory));
        }
        if ("all".equals(scope) || "pm_project".equals(scope)) {
            categories.add(searchPmProjects(keyword, maxPerCategory));
        }
        if ("all".equals(scope) || "pm_task".equals(scope)) {
            categories.add(searchPmTasks(keyword, maxPerCategory));
        }

        categories.removeIf(c -> c.getCount() == 0);
        int totalCount = categories.stream().mapToInt(CategoryResult::getCount).sum();
        long costMs = System.currentTimeMillis() - start;

        saveHistoryAsync(userId, keyword, scope, totalCount);

        return SearchResultDTO.builder()
                .keyword(keyword)
                .totalCount(totalCount)
                .categories(categories)
                .costMs(costMs)
                .build();
    }

    @Override
    public List<String> getSearchHistory(Long userId, int limit) {
        return historyMapper.getRecentKeywords(userId, limit);
    }

    @Override
    public List<Map<String, Object>> getHotSearches(int limit) {
        return historyMapper.getHotSearches(limit);
    }

    // ==================== 各模块搜索 ====================

    private CategoryResult searchCustomers(String booleanKw, String keyword, Long userId, Long deptId, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, customer_name, agent_name, status, update_time,
                   MATCH(customer_name, agent_name) AGAINST(? IN BOOLEAN MODE) AS score
            FROM customer
            WHERE deleted = 0 AND MATCH(customer_name, agent_name) AGAINST(? IN BOOLEAN MODE)
            """);
        List<Object> params = new ArrayList<>(List.of(booleanKw, booleanKw));

        if (userId != null && !isAdmin(userId)) {
            if (deptId != null && deptId > 0) {
                sql.append(" AND (creator_id = ? OR creator_id IN (SELECT id FROM sys_user WHERE dept_id = ?))");
                params.add(userId);
                params.add(deptId);
            } else {
                sql.append(" AND creator_id = ?");
                params.add(userId);
            }
        }
        sql.append(" ORDER BY score DESC LIMIT ?");
        params.add(limit);

        List<SearchItem> items = jdbcTemplate.query(sql.toString(), (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("customer_name"))
                .snippet("经纪人: " + rs.getString("agent_name"))
                .type("customer")
                .route("/realty/customer?id=" + rs.getLong("id"))
                .updatedAt(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
                .extra(Map.of("status", rs.getInt("status")))
                .build(), params.toArray());

        int count = items.size();
        return CategoryResult.builder().scope("customer").label("客户").count(count).items(items).build();
    }

    /**
     * 判断用户是否为管理员（有全部数据权限）
     */
    private boolean isAdmin(Long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = ? AND r.role_key IN ('admin', 'super_admin')",
                    Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private CategoryResult searchProjects(String booleanKw, String keyword, int limit) {
        String sql = """
            SELECT id, name, address, description, update_time,
                   MATCH(name, address, description) AGAINST(? IN BOOLEAN MODE) AS score
            FROM project
            WHERE deleted = 0 AND MATCH(name, address, description) AGAINST(? IN BOOLEAN MODE)
            ORDER BY score DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("name"))
                .snippet(truncate(rs.getString("address"), 60))
                .type("project")
                .route("/realty/project?id=" + rs.getLong("id"))
                .updatedAt(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
                .build(), booleanKw, booleanKw, limit);

        int count = countFulltext("project", "name, address, description", booleanKw);
        return CategoryResult.builder().scope("project").label("项目").count(count).items(items).build();
    }

    private CategoryResult searchAlliances(String booleanKw, String keyword, int limit) {
        String sql = """
            SELECT id, company_name, contact_name, update_time,
                   MATCH(company_name, contact_name) AGAINST(? IN BOOLEAN MODE) AS score
            FROM alliance
            WHERE deleted = 0 AND MATCH(company_name, contact_name) AGAINST(? IN BOOLEAN MODE)
            ORDER BY score DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("company_name"))
                .snippet("联系人: " + rs.getString("contact_name"))
                .type("alliance")
                .route("/realty/alliance?id=" + rs.getLong("id"))
                .updatedAt(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
                .build(), booleanKw, booleanKw, limit);

        int count = countFulltext("alliance", "company_name, contact_name", booleanKw);
        return CategoryResult.builder().scope("alliance").label("联盟商").count(count).items(items).build();
    }

    private CategoryResult searchChats(String booleanKw, String keyword, Long userId, int limit) {
        String sql = """
            SELECT id, sender_id, content, send_time,
                   MATCH(content) AGAINST(? IN BOOLEAN MODE) AS score
            FROM sys_chat_message
            WHERE MATCH(content) AGAINST(? IN BOOLEAN MODE)
              AND (sender_id = ? OR receiver_id = ?)
            ORDER BY score DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title("聊天消息")
                .snippet(truncate(rs.getString("content"), 80))
                .type("chat")
                .route("/message/chat")
                .updatedAt(rs.getTimestamp("send_time") != null ? rs.getTimestamp("send_time").toLocalDateTime() : null)
                .build(), booleanKw, booleanKw, userId, userId, limit);

        return CategoryResult.builder().scope("chat").label("聊天").count(items.size()).items(items).build();
    }

    private CategoryResult searchNotices(String booleanKw, String keyword, int limit) {
        String sql = """
            SELECT id, title, content, update_time,
                   MATCH(title, content) AGAINST(? IN BOOLEAN MODE) AS score
            FROM sys_notice
            WHERE deleted = 0 AND MATCH(title, content) AGAINST(? IN BOOLEAN MODE)
            ORDER BY score DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .snippet(truncate(rs.getString("content"), 80))
                .type("notice")
                .route("/message/notice")
                .updatedAt(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
                .build(), booleanKw, booleanKw, limit);

        int count = countFulltext("sys_notice", "title, content", booleanKw);
        return CategoryResult.builder().scope("notice").label("通知").count(count).items(items).build();
    }

    private CategoryResult searchPmProjects(String keyword, int limit) {
        String like = "%" + keyword + "%";
        String sql = """
            SELECT id, name, description, status, create_time
            FROM pm_project
            WHERE deleted = 0 AND (name LIKE ? OR description LIKE ?)
            ORDER BY create_time DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("name"))
                .snippet(truncate(rs.getString("description"), 80))
                .type("pm_project")
                .route("/project/" + rs.getLong("id"))
                .updatedAt(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null)
                .build(), like, like, limit);
        return CategoryResult.builder().scope("pm_project").label("项目管理").count(items.size()).items(items).build();
    }

    private CategoryResult searchPmTasks(String keyword, int limit) {
        String like = "%" + keyword + "%";
        String sql = """
            SELECT t.id, t.title, t.status, t.project_id, t.due_date, t.create_time, p.name AS project_name
            FROM pm_task t LEFT JOIN pm_project p ON t.project_id = p.id
            WHERE t.deleted = 0 AND (t.title LIKE ? OR t.description LIKE ?)
            ORDER BY t.create_time DESC
            LIMIT ?
            """;
        List<SearchItem> items = jdbcTemplate.query(sql, (rs, i) -> SearchItem.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .snippet("[" + rs.getString("project_name") + "] " + rs.getString("status"))
                .type("pm_task")
                .route("/project/" + rs.getLong("project_id"))
                .updatedAt(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null)
                .build(), like, like, limit);
        return CategoryResult.builder().scope("pm_task").label("项目任务").count(items.size()).items(items).build();
    }

    // ==================== 工具方法 ====================

    private int countFulltext(String table, String columns, String booleanKw) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE deleted = 0 AND MATCH(" + columns + ") AGAINST(? IN BOOLEAN MODE)";
        try {
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, booleanKw);
            return result != null ? result : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 转为 MySQL BOOLEAN MODE 格式：每个词加 + 前缀表示必须包含
     */
    private String toBooleanModeQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) return "";
        return Arrays.stream(keyword.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> "+" + s + "*")
                .collect(Collectors.joining(" "));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @Async
    protected void saveHistoryAsync(Long userId, String keyword, String scope, int resultCount) {
        try {
            historyMapper.saveHistory(userId, keyword, scope, resultCount);
            historyMapper.incrementHotSearch(keyword);
        } catch (Exception e) {
            log.warn("保存搜索历史失败: {}", e.getMessage());
        }
    }
}
