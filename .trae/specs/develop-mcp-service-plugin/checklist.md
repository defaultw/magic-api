# Checklist

- [x] 引入 magic-api-mcp 依赖后，无任何额外配置即可通过 /mcp/magic 完成 initialize 握手
- [x] tools/list 返回全部工具（分组/接口/执行/调试/辅助），Schema 完整（25 个）
- [x] 新增分组工具调用成功，分组树可见新节点
- [x] 新增接口工具调用成功（含参数配置/请求头/选项/响应设置字段保存，paramCount=1、responseBody 持久化验证通过）
- [x] 编辑接口工具调用成功，详情与最后一次保存一致（执行结果 data:2→data:101 验证）
- [x] 删除接口工具调用成功
- [x] 接口执行工具调用成功，返回脚本执行结果（POST data:42 / GET data:21；注意完整路径=分组path+接口path）
- [x] 调试工具对临时脚本执行成功且不留残留接口（return 42→42，搜索无残留）
- [x] 辅助工具（resource_tree/config_get/plugins_list/export/upload）调用正常；备份工具在未配置时返回友好提示而非 500
- [x] api_move / group_move 移动工具调用成功（含修复后回归验证）
- [x] lock/unlock、group_copy（返回新 id）、api_export（zip base64）验证通过
- [x] SKILLS 配置文件存在于 magic-api-mcp/skills/magic-api/SKILL.md，含 frontmatter、接入步骤、全部工具说明与示例
- [x] SKILL.md 官方文档收录覆盖侧边栏全部条目（70/70 页逐页遍历，附完整目录清单）
- [x] USAGE.md 使用说明完整（引入方式/端点/工具/调试/FAQ）
- [x] mvn 打包成功，demo 启动无兼容性问题，编辑器原功能正常（/magic/web 200）
