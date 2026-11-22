package org.ssssssss.magicapi.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Streamable-HTTP 端点（JSON-RPC 2.0）
 *
 * @since 2.3.0
 */
@RestController
public class McpController {

	private static final Logger logger = LoggerFactory.getLogger(McpController.class);

	private static final Set<String> SESSIONS = ConcurrentHashMap.newKeySet();

	private static final int PARSE_ERROR = -32700;
	private static final int INVALID_REQUEST = -32600;
	private static final int METHOD_NOT_FOUND = -32601;
	private static final int INVALID_PARAMS = -32602;
	private static final int INTERNAL_ERROR = -32603;

	private final MagicApiTools tools;

	private final String serverName;

	public McpController(MagicApiTools tools, String serverName) {
		this.tools = tools;
		this.serverName = serverName;
	}

	@PostMapping(value = "${magic-api.mcp.path:/mcp/magic}", consumes = {MediaType.APPLICATION_JSON_VALUE,
			"application/json-rpc", "application/json;charset=UTF-8"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> handle(@RequestBody(required = false) JsonNode body) {
		if (body == null || body.isNull()) {
			return respond(McpProtocol.error(null, INVALID_REQUEST, "请求体不能为空"));
		}
		Object id = body.has("id") && !body.get("id").isNull() ? convertId(body.get("id")) : null;
		boolean notification = !body.has("id");
		String method = body.path("method").asText(null);
		if (StringUtils.isBlank(method)) {
			return respond(McpProtocol.error(id, INVALID_REQUEST, "缺少 method 字段"));
		}
		JsonNode params = body.get("params");
		try {
			Object result;
			switch (method) {
				case "initialize":
					result = McpProtocol.initializeResult(serverName, "2.2.2-mcp");
					Map<String, Object> response = McpProtocol.result(id, result);
					return ResponseEntity.ok()
							.header("Mcp-Session-Id", UUID.randomUUID().toString())
							.body(response);
				case "notifications/initialized":
				case "notifications/cancelled":
					// 通知无需响应体
					return notification ? ResponseEntity.accepted().build() : respond(McpProtocol.result(id, Collections.emptyMap()));
				case "ping":
					return respond(McpProtocol.result(id, Collections.emptyMap()));
				case "tools/list":
					return respond(McpProtocol.result(id, McpProtocol.toolsListResult(tools.toolDefinitions())));
				case "tools/call":
					return respond(McpProtocol.result(id, callTool(params)));
				default:
					return notification
							? ResponseEntity.accepted().build()
							: respond(McpProtocol.error(id, METHOD_NOT_FOUND, "未知方法: " + method));
			}
		} catch (McpIllegalArgumentException e) {
			return respond(McpProtocol.error(id, INVALID_PARAMS, e.getMessage()));
		} catch (Throwable t) {
			logger.error("MCP 请求处理失败: {}", method, t);
			return respond(McpProtocol.error(id, INTERNAL_ERROR, t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
		}
	}

	/**
	 * 执行工具调用，工具内部异常统一转为 isError=true 的文本结果
	 */
	private Map<String, Object> callTool(JsonNode params) throws Exception {
		if (params == null) {
			throw new McpIllegalArgumentException("缺少 params 字段");
		}
		String name = params.path("name").asText(null);
		if (StringUtils.isBlank(name)) {
			throw new McpIllegalArgumentException("缺少工具名称 name");
		}
		JsonNode argumentsNode = params.get("arguments");
		Map<String, Object> arguments = new LinkedHashMap<>();
		if (argumentsNode != null && argumentsNode.isObject()) {
			argumentsNode.fields().forEachRemaining(entry -> arguments.put(entry.getKey(), McpProtocol.mapper().convertValue(entry.getValue(), Object.class)));
		}
		Method method;
		try {
			method = MagicApiTools.class.getMethod(toolMethodName(name), Map.class);
		} catch (NoSuchMethodException e) {
			throw new McpIllegalArgumentException("未知工具: " + name);
		}
		try {
			Object value = method.invoke(tools, arguments);
			String text;
			if (value instanceof String && !isJsonLike((String) value)) {
				text = (String) value;
			} else {
				text = McpProtocol.toJsonNoNull(value);
			}
			return McpProtocol.callResult(text);
		} catch (java.lang.reflect.InvocationTargetException target) {
			Throwable cause = target.getCause() == null ? target : target.getCause();
			logger.warn("MCP 工具执行失败: {}", name, cause);
			return McpProtocol.callErrorResult(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
		}
	}

	private static boolean isJsonLike(String value) {
		String trimmed = value.trim();
		return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"");
	}

	private String toolMethodName(String toolName) {
		// magic_group_tree -> groupTree
		String[] parts = toolName.split("_");
		if (parts.length < 3) {
			return toolName;
		}
		StringBuilder builder = new StringBuilder(parts[1]);
		for (int i = 2; i < parts.length; i++) {
			if (!parts[i].isEmpty()) {
				builder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
			}
		}
		return builder.toString();
	}

	private static Object convertId(JsonNode idNode) {
		if (idNode.isIntegralNumber()) {
			return idNode.longValue();
		}
		return idNode.asText();
	}

	private static ResponseEntity<Map<String, Object>> respond(Map<String, Object> response) {
		return ResponseEntity.ok(response);
	}

	private static class McpIllegalArgumentException extends RuntimeException {
		public McpIllegalArgumentException(String message) {
			super(message);
		}
	}
}
