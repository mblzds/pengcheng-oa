package com.pengcheng.ai.orchestration;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

/**
 * 简化版 StateGraph 编排引擎
 * 建模 路由 → 权限 → 执行 → 审计 → 记忆 流程
 * 未来可平滑迁移到 spring-ai-alibaba-graph 的 StateGraph
 */
@Slf4j
public class StateGraph<S> {

    private final Map<String, Function<S, S>> nodes = new LinkedHashMap<>();
    private final Map<String, Function<S, String>> conditionalEdges = new LinkedHashMap<>();
    private final Map<String, String> edges = new LinkedHashMap<>();
    private String startNode;
    private String endNode = "__END__";

    public StateGraph<S> addNode(String name, Function<S, S> action) {
        nodes.put(name, action);
        return this;
    }

    public StateGraph<S> setStartNode(String name) {
        this.startNode = name;
        return this;
    }

    public StateGraph<S> addEdge(String from, String to) {
        edges.put(from, to);
        return this;
    }

    public StateGraph<S> addConditionalEdge(String from, Function<S, String> router) {
        conditionalEdges.put(from, router);
        return this;
    }

    /**
     * 执行状态图
     */
    public S execute(S initialState) {
        if (startNode == null) throw new IllegalStateException("未设置起始节点");

        String current = startNode;
        S state = initialState;
        Set<String> visited = new HashSet<>();
        int maxSteps = 20;

        while (!endNode.equals(current) && maxSteps-- > 0) {
            if (visited.contains(current) && !conditionalEdges.containsKey(current)) {
                log.warn("[StateGraph] 检测到循环，终止执行: {}", current);
                break;
            }
            visited.add(current);

            Function<S, S> nodeAction = nodes.get(current);
            if (nodeAction == null) {
                log.warn("[StateGraph] 节点不存在: {}", current);
                break;
            }

            log.debug("[StateGraph] 执行节点: {}", current);
            state = nodeAction.apply(state);

            if (conditionalEdges.containsKey(current)) {
                current = conditionalEdges.get(current).apply(state);
            } else if (edges.containsKey(current)) {
                current = edges.get(current);
            } else {
                break;
            }
        }

        return state;
    }
}
