package org.ssssssss.magicapi.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具定义构建器（name/description/inputSchema）
 *
 * @since 2.3.0
 */
public class McpToolBuilder {

	private final Map<String, Object> tool = new LinkedHashMap<>();

	private final Map<String, Object> schema = new LinkedHashMap<>();

	private final Map<String, Object> properties = new LinkedHashMap<>();

	private final List<String> required = new java.util.ArrayList<>();

	public McpToolBuilder(String name, String description) {
		tool.put("name", name);
		tool.put("description", description);
		schema.put("type", "object");
	}

	public McpToolBuilder param(String name, String type, String description) {
		return param(name, type, description, null, false);
	}

	public McpToolBuilder param(String name, String type, String description, boolean required) {
		return param(name, type, description, null, required);
	}

	@SuppressWarnings("unchecked")
	public McpToolBuilder param(String name, String type, String description, Object items, boolean required) {
		Map<String, Object> property = new LinkedHashMap<>();
		property.put("type", type);
		if (description != null) {
			property.put("description", description);
		}
		if (items != null) {
			property.put("items", items);
		}
		properties.put(name, property);
		if (required) {
			this.required.add(name);
		}
		return this;
	}

	public Map<String, Object> build() {
		schema.put("properties", properties);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		tool.put("inputSchema", schema);
		return tool;
	}

	/**
	 * 便捷构造 object 数组的 items 描述
	 */
	public static Map<String, Object> arrayOf(Map<String, Object> objectSchema) {
		Map<String, Object> items = new LinkedHashMap<>();
		items.put("type", "object");
		if (objectSchema != null) {
			items.put("properties", objectSchema);
		}
		return items;
	}
}
