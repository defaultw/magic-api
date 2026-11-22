package org.ssssssss.magicapi.mcp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.backup.model.Backup;
import org.ssssssss.magicapi.backup.service.MagicBackupService;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.BaseDefinition;
import org.ssssssss.magicapi.core.model.DataType;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.Header;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.Option;
import org.ssssssss.magicapi.core.model.Parameter;
import org.ssssssss.magicapi.core.model.Path;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.model.SelectedResource;
import org.ssssssss.magicapi.core.model.TreeNode;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * magic-api 全量操作工具实现（进程内调用 MagicResourceService / MagicAPIService / RequestHandler，
 * 与官方 Web 端点逻辑对齐）
 *
 * @since 2.3.0
 */
public class MagicApiTools {

	private static final Logger logger = LoggerFactory.getLogger(MagicApiTools.class);

	private static final String TYPE_API = "api";

	private final MagicResourceService resourceService;

	private final MagicAPIService magicAPIService;

	private final MagicConfiguration configuration;

	private final RequestMagicDynamicRegistry requestMagicDynamicRegistry;

	private final List<Plugin> plugins;

	public MagicApiTools(MagicConfiguration configuration, List<Plugin> plugins) {
		this.configuration = configuration;
		this.resourceService = configuration.getMagicResourceService();
		this.magicAPIService = configuration.getMagicAPIService();
		this.requestMagicDynamicRegistry = configuration.getMagicDynamicRegistries().stream()
				.filter(registry -> registry instanceof RequestMagicDynamicRegistry)
				.map(registry -> (RequestMagicDynamicRegistry) registry)
				.findFirst().orElse(null);
		this.plugins = plugins == null ? Collections.emptyList() : plugins;
	}

	// ==================== 分组管理 ====================

	public Object groupTree(Map<String, Object> args) {
		String type = str(args, "type");
		TreeNode<Group> tree = resourceService.tree(StringUtils.defaultIfBlank(type, TYPE_API));
		return tree == null ? Collections.emptyList() : tree;
	}

	public Object groupGet(Map<String, Object> args) {
		String id = required(args, "id");
		Group group = resourceService.getGroup(id);
		notNull(group, "分组不存在: " + id);
		return group;
	}

	public Object groupSave(Map<String, Object> args) {
		Group group = new Group();
		String id = str(args, "id");
		if (StringUtils.isNotBlank(id)) {
			Group old = resourceService.getGroup(id);
			notNull(old, "分组不存在: " + id);
			group = old;
		}
		group.setName(required(args, "name"));
		String path = str(args, "path");
		if (StringUtils.isNotBlank(path)) {
			group.setPath(path);
		}
		// type 与 parentId 为官方 saveGroup 的必填校验项，缺省给默认值
		String type = str(args, "type");
		group.setType(StringUtils.defaultIfBlank(type, TYPE_API));
		String parentId = str(args, "parentId");
		group.setParentId(StringUtils.defaultIfBlank(parentId, Constants.ROOT_ID));
		notNull(resourceService.saveGroup(group), "保存分组失败");
		return group;
	}

	public Object groupDelete(Map<String, Object> args) {
		String id = required(args, "id");
		notNull(resourceService.getGroup(id) != null || resourceService.file(id) != null, "分组或文件不存在: " + id);
		return resourceService.delete(id);
	}

	public Object apiMove(Map<String, Object> args) {
		return resourceService.move(required(args, "src"), required(args, "groupId"));
	}

	public Object groupMove(Map<String, Object> args) {
		return resourceService.move(required(args, "src"), required(args, "groupId"));
	}

	public Object groupCopy(Map<String, Object> args) {
		return resourceService.copyGroup(required(args, "src"), str(args, "target"));
	}

	// ==================== 接口管理 ====================

	public Object apiList(Map<String, Object> args) {
		String groupId = required(args, "groupId");
		List<Map<String, Object>> result = new ArrayList<>();
		for (MagicEntity entity : resourceService.listFiles(groupId)) {
			result.add(entitySummary(entity));
		}
		return result;
	}

	public Object apiGet(Map<String, Object> args) {
		String id = required(args, "id");
		MagicEntity entity = resourceService.file(id);
		notNull(entity, "接口不存在: " + id);
		return entity;
	}

	public Object apiSave(Map<String, Object> args) {
		ApiInfo info = new ApiInfo();
		String id = str(args, "id");
		if (StringUtils.isNotBlank(id)) {
			MagicEntity old = resourceService.file(id);
			notNull(old instanceof ApiInfo, "接口不存在: " + id);
			info = (ApiInfo) old;
		}
		info.setGroupId(required(args, "groupId"));
		info.setName(required(args, "name"));
		info.setPath(required(args, "path"));
		info.setMethod(StringUtils.defaultIfBlank(str(args, "method"), "GET").toUpperCase());
		info.setScript(required(args, "script"));
		info.setParameters(applyDefinitions(info.getParameters(), args.get("parameters"), Parameter.class));
		info.setHeaders(applyDefinitions(info.getHeaders(), args.get("headers"), Header.class));
		info.setPaths(applyDefinitions(info.getPaths(), args.get("paths"), Path.class));
		applyOptions(info, args.get("options"));
		String requestBody = str(args, "requestBody");
		if (requestBody != null) {
			info.setRequestBody(requestBody);
		}
		String responseBody = str(args, "responseBody");
		if (responseBody != null) {
			info.setResponseBody(responseBody);
		}
		String description = str(args, "description");
		if (description != null) {
			info.setDescription(description);
		}
		notNull(resourceService.saveFile(info), "保存接口失败");
		return info.getId();
	}

	private void applyOptions(ApiInfo info, Object optionsValue) {
		if (!(optionsValue instanceof List)) {
			return;
		}
		List<Option> options = new ArrayList<>();
		for (Object item : (List<?>) optionsValue) {
			if (item instanceof Map) {
				Object name = ((Map<?, ?>) item).get("name");
				if (name != null) {
					options.add(new Option(name.toString(), Objects.toString(((Map<?, ?>) item).get("value"), null)));
				}
			} else if (item instanceof Option) {
				options.add((Option) item);
			}
		}
		info.setOption(options);
	}

	private <T extends BaseDefinition> List<T> applyDefinitions(List<T> defaults, Object value, Class<T> clazz) {
		if (!(value instanceof List)) {
			return defaults;
		}
		List<T> list = new ArrayList<>();
		for (Object item : (List<?>) value) {
			if (!(item instanceof Map)) {
				continue;
			}
			Map<?, ?> map = (Map<?, ?>) item;
			T def;
			try {
				def = clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalArgumentException("无法构造 " + clazz.getSimpleName() + ": " + e.getMessage());
			}
			def.setName(Objects.toString(map.get("name"), null));
			def.setValue(map.get("value"));
			if (map.containsKey("description")) {
				def.setDescription(Objects.toString(map.get("description"), null));
			}
			if (map.containsKey("required")) {
				def.setRequired(Boolean.parseBoolean(Objects.toString(map.get("required"), "false")));
			}
			if (map.containsKey("dataType")) {
				try {
					def.setDataType(DataType.valueOf(Objects.toString(map.get("dataType"), "String")));
				} catch (IllegalArgumentException ignored) {
				}
			}
			if (map.containsKey("defaultValue")) {
				def.setDefaultValue(Objects.toString(map.get("defaultValue"), null));
			}
			if (map.containsKey("validateType")) {
				def.setValidateType(Objects.toString(map.get("validateType"), null));
			}
			if (map.containsKey("expression")) {
				def.setExpression(Objects.toString(map.get("expression"), null));
			}
			list.add(def);
		}
		return list;
	}

	public Object apiDelete(Map<String, Object> args) {
		String id = required(args, "id");
		notNull(resourceService.file(id) != null || resourceService.getGroup(id) != null, "接口或分组不存在: " + id);
		return resourceService.delete(id);
	}

	public Object apiLock(Map<String, Object> args) {
		return resourceService.lock(required(args, "id"));
	}

	public Object apiUnlock(Map<String, Object> args) {
		return resourceService.unlock(required(args, "id"));
	}

	public Object apiSearch(Map<String, Object> args) {
		String keyword = required(args, "keyword").toLowerCase();
		List<Map<String, Object>> result = new ArrayList<>();
		for (MagicEntity entity : resourceService.files(TYPE_API)) {
			String name = Objects.toString(entity.getName(), "").toLowerCase();
			String path = entity instanceof PathMagicEntity ? Objects.toString(((PathMagicEntity) entity).getPath(), "").toLowerCase() : "";
			if (name.contains(keyword) || path.contains(keyword)) {
				result.add(entitySummary(entity));
			}
		}
		return result;
	}

	// ==================== 接口执行与调试 ====================

	public Object apiExecute(Map<String, Object> args) {
		String method = StringUtils.defaultIfBlank(str(args, "method"), "GET");
		String path = required(args, "path");
		return magicAPIService.call(method, path, mapArg(args.get("context")));
	}

	public Object apiDebug(Map<String, Object> args) {
		// 调试 = 临时注册接口 -> 进程内执行脚本 -> 注销，不落盘，等价于编辑器调试
		ApiInfo info = new ApiInfo();
		info.setId("mcp-debug-" + UUID.randomUUID().toString().replace("-", ""));
		info.setGroupId(str(args, "groupId"));
		info.setName(StringUtils.defaultIfBlank(str(args, "name"), "mcp-debug"));
		info.setPath(StringUtils.defaultIfBlank(str(args, "path"), "/mcp-debug-" + info.getId().substring(9, 17)));
		info.setMethod(StringUtils.defaultIfBlank(str(args, "method"), "GET").toUpperCase());
		info.setScript(required(args, "script"));
		notNull(requestMagicDynamicRegistry != null, "RequestMagicDynamicRegistry 未就绪");
		notNull(requestMagicDynamicRegistry.register(info), "临时接口注册失败: " + info.getMethod() + ":" + info.getPath());
		try {
			Map<String, Object> context = mapArg(args.get("context"));
			context.putAll(mapArg(args.get("parameters")));
			context.putAll(mapArg(args.get("pathVariables")));
			Map<String, Object> headers = mapArg(args.get("headers"));
			if (!headers.isEmpty()) {
				context.put("header", headers);
			}
			Object body = args.get("body");
			if (body != null) {
				context.put("body", body);
			}
			return magicAPIService.execute(info.getMethod(), info.getPath(), context);
		} finally {
			requestMagicDynamicRegistry.unregister(info);
		}
	}

	// ==================== 辅助管理 ====================

	public Object resourceTree(Map<String, Object> args) {
		Map<String, TreeNode<Group>> tree = resourceService.tree();
		List<Map<String, Object>> result = new ArrayList<>();
		tree.forEach((type, node) -> {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("type", type);
			item.put("tree", node);
			result.add(item);
		});
		return result;
	}

	public Object configGet(Map<String, Object> args) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("instanceId", configuration.getInstanceId());
		config.put("editorConfig", configuration.getEditorConfig());
		config.put("enableWeb", configuration.isEnableWeb());
		config.put("debugTimeout", configuration.getDebugTimeout());
		config.put("throwException", configuration.isThrowException());
		return config;
	}

	public Object pluginsList(Map<String, Object> args) {
		return plugins;
	}

	public Object backupList(Map<String, Object> args) {
		MagicBackupService service = configuration.getMagicBackupService();
		if (service == null) {
			return "备份服务未启用（magic-api.resource.backup 默认未配置），无法查询备份";
		}
		Long timestamp = longArg(args.get("timestamp"));
		return service.backupList(timestamp == null ? System.currentTimeMillis() : timestamp);
	}

	public Object backupRestore(Map<String, Object> args) throws IOException {
		MagicBackupService service = configuration.getMagicBackupService();
		if (service == null) {
			return "备份服务未启用，无法还原备份";
		}
		String id = required(args, "id");
		Long timestamp = longArg(args.get("timestamp"));
		Backup backup = service.backupInfo(id, timestamp == null ? 0L : timestamp);
		notNull(backup, "备份不存在: " + id);
		if ("full".equals(id)) {
			service.doBackupAll("还原全量备份前，系统自动全量备份", "mcp");
			magicAPIService.upload(new ByteArrayInputStream(backup.getContent()), Constants.UPLOAD_MODE_FULL);
			return true;
		}
		if (backup.getType().endsWith("-group")) {
			Group group = JsonUtils.readValue(backup.getContent(), Group.class);
			return resourceService.saveGroup(group);
		}
		MagicEntity entity = configuration.getMagicDynamicRegistries().stream()
				.map(MagicDynamicRegistry::getMagicResourceStorage)
				.filter(storage -> storage.folder().equals(backup.getType()))
				.map(storage -> storage.read(backup.getContent()))
				.findFirst().orElse(null);
		notNull(entity, "无法识别备份类型: " + backup.getType());
		return resourceService.saveFile(entity);
	}

	public Object backupDelete(Map<String, Object> args) {
		MagicBackupService service = configuration.getMagicBackupService();
		if (service == null) {
			return "备份服务未启用，无法删除备份";
		}
		return service.removeBackup(required(args, "id"));
	}

	public Object backupFull(Map<String, Object> args) throws IOException {
		MagicBackupService service = configuration.getMagicBackupService();
		if (service == null) {
			return "备份服务未启用，无法执行全量备份";
		}
		service.doBackupAll("MCP 主动全量备份", "mcp");
		return true;
	}

	public Object apiExport(Map<String, Object> args) throws IOException {
		String groupId = str(args, "groupId");
		List<SelectedResource> resources = parseSelectedResources(args.get("resources"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		magicAPIService.download(groupId, resources, baos);
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	public Object apiUpload(Map<String, Object> args) throws IOException {
		String fileBase64 = required(args, "fileBase64");
		String mode = StringUtils.defaultIfBlank(str(args, "mode"), Constants.UPLOAD_MODE_FULL);
		byte[] bytes = Base64.getDecoder().decode(fileBase64);
		return magicAPIService.upload(new ByteArrayInputStream(bytes), mode);
	}

	// ==================== 工具目录 ====================

	public List<Map<String, Object>> toolDefinitions() {
		List<Map<String, Object>> tools = new ArrayList<>();
		// 分组管理
		tools.add(new McpToolBuilder("magic_group_tree", "获取 magic-api 分组树")
				.param("type", "string", "分组类型过滤：api/function/function-class/datasource/columns，默认 api", false).build());
		tools.add(new McpToolBuilder("magic_group_get", "获取分组详情")
				.param("id", "string", "分组ID", true).build());
		tools.add(new McpToolBuilder("magic_group_save", "新增或编辑分组")
				.param("id", "string", "编辑时传分组ID，新增留空", false)
				.param("parentId", "string", "父分组ID，根分组留空", false)
				.param("name", "string", "分组名称", true)
				.param("path", "string", "分组路径（URL 前缀），如 demo", false)
				.param("type", "string", "分组类型，默认 api", false).build());
		tools.add(new McpToolBuilder("magic_group_delete", "删除分组或文件")
				.param("id", "string", "分组ID或文件ID", true).build());
		tools.add(new McpToolBuilder("magic_group_move", "移动分组/接口到目标分组")
				.param("src", "string", "源ID", true)
				.param("groupId", "string", "目标分组ID", true).build());
		tools.add(new McpToolBuilder("magic_group_copy", "复制分组")
				.param("src", "string", "源分组ID", true)
				.param("target", "string", "目标父分组ID，可空", false).build());
		// 接口管理
		tools.add(new McpToolBuilder("magic_api_list", "获取分组下的接口列表")
				.param("groupId", "string", "分组ID", true).build());
		tools.add(new McpToolBuilder("magic_api_get", "获取接口详情（含脚本、参数配置、请求头、选项、响应设置等）")
				.param("id", "string", "接口ID", true).build());
		tools.add(new McpToolBuilder("magic_api_save", "新增或编辑接口，完整承载参数配置/请求头/路径变量/接口选项/请求体/响应设置")
				.param("id", "string", "编辑时传接口ID，新增留空", false)
				.param("groupId", "string", "所属分组ID", true)
				.param("name", "string", "接口名称", true)
				.param("path", "string", "接口路径，如 /test", true)
				.param("method", "string", "请求方法 GET/POST/PUT/DELETE 等，默认 GET", false)
				.param("script", "string", "magic-script 脚本内容", true)
				.param("parameters", "array", "请求参数配置 [{name,value,description,required,dataType}]", McpToolBuilder.arrayOf(null), false)
				.param("headers", "array", "请求头配置 [{name,value,description}]", McpToolBuilder.arrayOf(null), false)
				.param("paths", "array", "路径变量配置 [{name,value,description}]", McpToolBuilder.arrayOf(null), false)
				.param("options", "array", "接口选项 [{name,value}]，如 anonymous/require_login/disabled_unknown_parameter", McpToolBuilder.arrayOf(null), false)
				.param("requestBody", "string", "请求体示例/说明", false)
				.param("responseBody", "string", "响应结果示例/说明（响应设置）", false)
				.param("description", "string", "接口描述", false).build());
		tools.add(new McpToolBuilder("magic_api_delete", "删除接口或分组")
				.param("id", "string", "接口ID或分组ID", true).build());
		tools.add(new McpToolBuilder("magic_api_move", "移动接口到目标分组")
				.param("src", "string", "源接口ID", true)
				.param("groupId", "string", "目标分组ID", true).build());
		tools.add(new McpToolBuilder("magic_api_lock", "锁定接口")
				.param("id", "string", "接口ID", true).build());
		tools.add(new McpToolBuilder("magic_api_unlock", "解锁接口")
				.param("id", "string", "接口ID", true).build());
		tools.add(new McpToolBuilder("magic_api_search", "按名称/路径搜索接口")
				.param("keyword", "string", "搜索关键字", true).build());
		// 执行与调试
		tools.add(new McpToolBuilder("magic_api_execute", "执行已发布的 magic-api 接口")
				.param("method", "string", "请求方法，默认 GET", false)
				.param("path", "string", "接口路径，如 demo/test", true)
				.param("context", "object", "脚本上下文变量 {key:value}", false).build());
		tools.add(new McpToolBuilder("magic_api_debug", "调试脚本（临时注册执行，不落盘，等价于编辑器调试）")
				.param("groupId", "string", "所属分组ID，可空", false)
				.param("name", "string", "临时接口名称", false)
				.param("path", "string", "临时接口路径", false)
				.param("method", "string", "请求方法，默认 GET", false)
				.param("script", "string", "magic-script 脚本内容", true)
				.param("parameters", "object", "测试参数 {key:value}", false)
				.param("headers", "object", "测试请求头 {key:value}", false)
				.param("pathVariables", "object", "路径变量 {key:value}", false)
				.param("body", "object", "请求体（任意 JSON）", false).build());
		// 辅助管理
		tools.add(new McpToolBuilder("magic_resource_tree", "获取全量资源树（全部分组类型及节点）").build());
		tools.add(new McpToolBuilder("magic_config_get", "获取 magic-api 运行配置").build());
		tools.add(new McpToolBuilder("magic_plugins_list", "获取已安装插件列表").build());
		tools.add(new McpToolBuilder("magic_backup_list", "查询备份记录（备份服务未启用时返回说明文本）")
				.param("timestamp", "number", "查询该时间戳之前的备份，默认当前时间", false).build());
		tools.add(new McpToolBuilder("magic_backup_restore", "还原备份")
				.param("id", "string", "备份对象ID，全量备份传 full", true)
				.param("timestamp", "number", "备份时间戳", false).build());
		tools.add(new McpToolBuilder("magic_backup_delete", "删除备份")
				.param("id", "string", "备份对象ID", true).build());
		tools.add(new McpToolBuilder("magic_backup_full", "执行全量备份").build());
		tools.add(new McpToolBuilder("magic_api_export", "导出接口包（zip，返回 base64）")
				.param("groupId", "string", "分组ID，可空表示全部", false)
				.param("resources", "array", "指定资源 [{id,type}]", McpToolBuilder.arrayOf(null), false).build());
		tools.add(new McpToolBuilder("magic_api_upload", "导入接口备份包（zip 的 base64）")
				.param("fileBase64", "string", "zip 文件 base64 内容", true)
				.param("mode", "string", "导入模式 full(覆盖)/increment(增量)，默认 full", false).build());
		return tools;
	}

	// ==================== 内部辅助 ====================

	private Map<String, Object> entitySummary(MagicEntity entity) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("id", entity.getId());
		summary.put("name", entity.getName());
		summary.put("groupId", entity.getGroupId());
		summary.put("updateTime", entity.getUpdateTime());
		if (entity instanceof PathMagicEntity) {
			summary.put("path", ((PathMagicEntity) entity).getPath());
			if (entity instanceof ApiInfo) {
				summary.put("method", ((ApiInfo) entity).getMethod());
			}
		}
		return summary;
	}

	private List<SelectedResource> parseSelectedResources(Object value) {
		if (!(value instanceof List)) {
			return null;
		}
		List<SelectedResource> resources = new ArrayList<>();
		for (Object item : (List<?>) value) {
			if (item instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) item;
				SelectedResource resource = new SelectedResource();
				resource.setId(Objects.toString(map.get("id"), null));
				resource.setType(Objects.toString(map.get("type"), null));
				resources.add(resource);
			}
		}
		return resources;
	}

	private static String str(Map<String, Object> args, String key) {
		Object value = args == null ? null : args.get(key);
		return value == null ? null : value.toString();
	}

	private static String required(Map<String, Object> args, String key) {
		String value = str(args, key);
		notNull(StringUtils.isNotBlank(value), "缺少必填参数: " + key);
		return value;
	}

	private static Map<String, Object> mapArg(Object value) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (value instanceof Map) {
			((Map<?, ?>) value).forEach((k, v) -> result.put(Objects.toString(k), v));
		}
		return result;
	}

	private static Long longArg(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void notNull(Object value, String message) {
		if (value == null || Boolean.FALSE.equals(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
