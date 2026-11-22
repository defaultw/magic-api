package org.ssssssss.magicapi.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP JSON-RPC 2.0 响应构建与内容结构工具
 *
 * @since 2.3.0
 */
public class McpProtocol {

	public static final String PROTOCOL_VERSION = "2024-11-05";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static ObjectMapper mapper() {
		return MAPPER;
	}

	/**
	 * 成功响应
	 */
	public static Map<String, Object> result(Object id, Object value) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("result", value);
		return response;
	}

	/**
	 * 错误响应
	 */
	public static Map<String, Object> error(Object id, int code, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		Map<String, Object> err = new LinkedHashMap<>();
		err.put("code", code);
		err.put("message", message);
		response.put("error", err);
		return response;
	}

	/**
	 * 构建 initialize 结果
	 */
	public static Map<String, Object> initializeResult(String serverName, String serverVersion) {
		Map<String, Object> capabilities = new LinkedHashMap<>();
		capabilities.put("tools", new LinkedHashMap<>());
		Map<String, Object> serverInfo = new LinkedHashMap<>();
		serverInfo.put("name", serverName);
		serverInfo.put("version", serverVersion);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("protocolVersion", PROTOCOL_VERSION);
		result.put("capabilities", capabilities);
		result.put("serverInfo", serverInfo);
		result.put("instructions", "magic-api MCP server: 通过 tools 管理与调试 magic-api 的分组、接口、备份等全部能力");
		return result;
	}

	/**
	 * 构建 tools/list 结果
	 */
	public static Map<String, Object> toolsListResult(List<Map<String, Object>> tools) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("tools", tools);
		return result;
	}

	/**
	 * 构建 tools/call 的文本内容结果
	 */
	public static Map<String, Object> callResult(String text) {
		Map<String, Object> content = new LinkedHashMap<>();
		content.put("type", "text");
		content.put("text", text);
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> contents = new ArrayList<>();
		contents.add(content);
		result.put("content", contents);
		result.put("isError", false);
		return result;
	}

	/**
	 * 构建 tools/call 的错误内容结果（isError=true，HTTP 仍为 200）
	 */
	public static Map<String, Object> callErrorResult(String text) {
		Map<String, Object> result = callResult(text);
		result.put("isError", true);
		return result;
	}

	/**
	 * 解析 JSON，失败抛 UncheckedIOException
	 */
	public static JsonNode parse(String json) {
		try {
			return MAPPER.readTree(json);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 序列化为 JSON 字符串
	 */
	public static String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (IOException e) {
			return String.valueOf(value);
		}
	}

	/**
	 * 序列化（排除 null 字段）
	 */
	public static String toJsonNoNull(Object value) {
		try {
			return MAPPER.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(value);
		} catch (IOException e) {
			return String.valueOf(value);
		}
	}
}
