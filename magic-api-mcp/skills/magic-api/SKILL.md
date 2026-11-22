---
name: magic-api
description: 通过 MCP 协议管理 magic-api 接口平台：分组管理、接口增删改查、参数配置、响应设置、脚本调试执行、备份导入导出。当用户要求创建/编辑/删除/调试 magic-api 接口或分组、查询接口列表、导出导入接口包时使用本技能。包含 25 个 MCP 工具的完整参数规范与 magic-api 脚本开发全量知识（官方文档 70 页收录）。
---

# magic-api MCP 技能

magic-api：基于 Java 的接口快速开发框架，界面编写脚本（magic-script）自动映射为 HTTP 接口，无需 Controller/Service/Dao/Mapper/VO。本技能使智能体经 MCP 完整操作其全部能力。

## 一、MCP 服务接入

- 端点：`POST http://<host>:<port>/mcp/magic`（路径可由 `magic-api.mcp.path` 改）；JSON-RPC 2.0 over HTTP；`Content-Type: application/json; charset=utf-8`；协议版本 `2024-11-05`；方法：`initialize`/`notifications/initialized`/`ping`/`tools/list`/`tools/call`
- 鉴权：默认开放；若配置 `magic-api.mcp.token`，须携带 `X-Mcp-Token: <token>` 或 `Authorization: Bearer <token>`

接入步骤（均 POST `http://localhost:8080/mcp/magic`，带上述 Content-Type）：

```bash
# Step1 握手（响应头 Mcp-Session-Id 为会话 ID，后续可携带）→ Step2 通知初始化完成 → Step3 tools/list → Step4 tools/call
-d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"agent","version":"1.0"}}}'
-d '{"jsonrpc":"2.0","method":"notifications/initialized"}'
-d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
-d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"magic_group_tree","arguments":{"type":"api"}}}'
```

响应格式：成功 `{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"<结果>"}],"isError":false}}`；业务错误（参数缺失/资源不存在）HTTP 仍 200，text 为错误说明（如"缺少必填参数: id"）且 `isError:true`；未知工具返回 `{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"未知工具: xxx"}}`。
智能体约定：`isError=true` 时把 text 作为错误反馈并修正参数重试；未知工具先调 `tools/list` 核对工具名。
即插即用：`magic-api-mcp` 依赖引入 Spring Boot Web 工程即自动装配，零配置；若同时引入 `magic-api-spring-boot-starter`，编辑器（默认 `/magic/web`）与 MCP 端点共存，共享存储与注册机制，修改实时互通。

## 二、工具清单与参数规范（25 个）

> 命名规范 `magic_<域>_<动作>`：分组域=group（树/查/存/删/移/复制），接口域=api（列表/详情/保存/删除/移动/锁/搜索），执行域独立，辅助域=resource/config/plugins/backup/export/upload。
> **路径拼接：接口完整路径 = 全局 prefix（默认 /）+ 分组 path + 接口 path**。执行/调试务必用完整路径（分组 path 与接口 path 都计入）。

### 2.1 分组管理（6 个）

- **magic_group_tree** 获取分组树：`{"type":"api"}`；type 可选 api/function/function-class/datasource/columns，默认 api。返回嵌套 TreeNode：`[{node:{id,name,type,path,parentId,...},children:[...]}]`
- **magic_group_get** 分组详情：`{"id":"<分组ID>"}`（必填）
- **magic_group_save** 新增/编辑分组。示例：根级新增 `{"name":"订单管理","path":"order"}`；子分组 `{"name":"退款","parentId":"<父ID>","path":"refund"}`；编辑 `{"id":"<分组ID>","name":"新名字","parentId":"<父ID>"}`（parentId 缺省保持不变则须显式传原 parentId）。参数：name 必填（作文件夹名，不可含非法字符）；id（传=编辑/不传=新增）；parentId（默认 `"0"` 根节点）；path（拼接进接口 URL，可省略）；type（默认 api，可选 function/function-class/datasource/columns）
- **magic_group_delete** 删除分组（级联删子分组与文件）或按 id 删文件：`{"id":"<分组ID或文件ID>"}`
- **magic_group_move** 把分组（或文件）移到目标分组下：`{"src":"<源分组ID>","groupId":"<目标父分组ID>"}`
- **magic_group_copy** 复制分组（返回新分组 id）：`{"src":"<源分组ID>","target":"<目标父分组ID>"}`；target 缺省为根节点

### 2.2 接口管理（8 个）

- **magic_api_list** 分组下接口列表：`{"groupId":"<分组ID>"}`（必填）
- **magic_api_get** 接口详情（脚本/参数/选项/请求体/响应设置全量）：`{"id":"<接口ID>"}`（必填，返回 ApiInfo 全字段）
- **magic_api_save** 新增/编辑接口（参数+响应设置全承载）：

```json
{"groupId":"<分组ID>","name":"订单详情","path":"/detail","script":"return db.table('t_order').selectById(id)","method":"POST","id":"<接口ID>",
 "parameters":[{"name":"id","value":"","description":"订单ID","required":true,"dataType":"String"}],
 "headers":[{"name":"X-Token","value":"","description":"鉴权令牌"}],
 "paths":[{"name":"id","value":"","description":"用户ID","required":true,"dataType":"Long"}],
 "options":[{"name":"anonymous","value":false},{"name":"transaction","value":true}],
 "requestBody":"<说明JSON>","responseBody":"<说明JSON>","description":"查询订单详情"}
```

  必填：groupId/name/path（相对分组路径）/script（magic-script）。可选：method（默认 GET，GET/POST/PUT/DELETE...）、id（传=编辑）、parameters（请求参数）、headers（请求头）、paths（路径变量，path 为 /user/{id} 时）、options（鉴权/缓存/事务等）、requestBody/responseBody（请求/响应体说明）、description；返回接口 id。
  常用 options.name：`anonymous`（匿名）、`transaction`（事务）、`readonly`（只读事务）、`page`（分页）、`page-size`、`cache`（缓存名）、`cache-ttl`（缓存毫秒）、`permission`（权限编码）、`role`（角色）、`require-login`（需登录）、`time-out`（超时毫秒）、`swagger-define`。与编辑器选项一致。
- **magic_api_delete** 删除接口或分组：`{"id":"<接口ID或分组ID>"}`
- **magic_api_move** 把接口移到目标分组：`{"src":"<接口ID>","groupId":"<目标分组ID>"}`
- **magic_api_lock / magic_api_unlock** 锁定/解锁接口（防止他人编辑）：`{"id":"<接口ID>"}`
- **magic_api_search** 按名称/路径模糊搜索：`{"keyword":"订单"}`（必填；返回匹配的接口与分组）

### 2.3 执行与调试（2 个）

- **magic_api_execute** 执行**已发布**接口（进程内调用，不走 HTTP）。示例：`{"method":"POST","path":"order/detail","context":{"id":"123"}}`。method 默认 GET；path 为完整路径（分组 path+接口 path，不含全局 prefix）；context 注入：Query 直接放顶层 `{"num":1}`，POST body 用 `{"body":{...}}`，header 用 `{"header":{...}}`，路径变量用 `{"path":{...}}`。返回：`{"code":1,"message":"success","data":...,"timestamp":...,"executeTime":...}`
- **magic_api_debug** 调试脚本（临时注册后立即注销，**不落盘**）。示例：`{"script":"return body.num * 2","method":"POST","groupId":"<分组ID>","path":"/debug/test","name":"调试接口","parameters":{"num":21},"headers":{"token":"xxx"},"pathVariables":{"id":"1"},"body":{"num":21}}`。script 必填；method 默认 GET；groupId 可选（影响路径前缀）；path 默认 `/debug-{uuid}`；parameters/headers/pathVariables/body 分别模拟 Query/请求头/路径变量/请求体（脚本中 body.xxx 取）；返回脚本执行结果。

### 2.4 辅助管理（9 个）

- **magic_resource_tree** 全量资源树（各分组类型：api/function/datasource 等）：`{}`
- **magic_config_get** 运行配置（prefix/web 路径/存储方式/插件配置等）：`{}`
- **magic_plugins_list** 已安装插件列表：`{}`
- **magic_backup_list** 查询备份记录：`{}`；可选 `{"timestamp":1700000000000}` 查某时间点之前的记录
- **magic_backup_restore** 还原备份：`{"id":"<备份ID>"}`（必填）；可选 `{"timestamp":...}` 缩小范围
- **magic_backup_delete** 删除备份：`{"id":"<备份ID>"}`
- **magic_backup_full** 执行全量备份：`{}`
- **magic_api_export** 导出接口包（zip 的 base64）：可选 `{"groupId":"<分组ID>"}` 或 `{"resources":["<id1>","<id2>"]}` 按资源导出；都不传=全量。返回 base64（zip 文件）
- **magic_api_upload** 导入接口包（base64→zip→导入）：`{"fileBase64":"<magic_api_export 返回的 base64>","mode":"full"}`；mode：full=全量覆盖（先清空再导入，慎用）/increment=增量（只更新存在的）

> 备份类工具依赖 `magic-api.resource.backup` 配置；未启用时返回说明文本不会报错。

## 三、magic-script 脚本开发要点

### 3.1 参数获取（接口入参）

Query/表单参数直接用变量名 `name`（`GET /x?name=abc` 直接写 `name`）；请求体 `body.xxx`（POST JSON 整体映射为 body，为数组可遍历）；请求头 `header.xxx`（如 `header.token`）；路径变量 `path.xxx` 或变量名（path 定义 `/user/{id}` 时用 `path.id`，避免与同名 Query 冲突）；Cookie `cookie.xxx`（如 `cookie.JSESSIONID`）；Session `session.xxx`；多值参数 `request.getValues('name')`、上传文件 `request.getFile('name')`（需 `import request;`）。
SQL 中引用：`#{body.xxx}`、`#{header.xxx}`、`#{path.xxx}`。脚本自定义变量与参数同名时，**自定义变量优先**。

### 3.2 脚本语法

- 关键字：var/if/else/for/in/while/continue/break/return/exit/assert/instanceof/try/catch/finally/import/as/new/true/false/null/async
- 类型字面量：`123` int、`123l` long、`123.5d` double、`123f` float、`123m` BigDecimal、`true/false`、`'str'`/`"str"`、`"""多行文本块（常用于SQL）"""`、`/\d+/g` 正则、`[1,2,3]` list、`{key:'value'}` map、`(x)=>x+1` lambda
- 类型转换：`"123"::int`、`"abc"::int(111)`（失败给默认值）、`"2020-01-01"::date('yyyy-MM-dd')`；可选链 `a?.name`（null 短路返回 null）
- 扩展运算符：`sum(...[1,2,3])`；`[1,2,...arr]`；`{k1:1,...map}`（map 展开到 map）
- for 循环（仅集合与 Map）：`for(index,item in list){...}`、`for(value in range(0,100)){...}`（含 0 含 100）、`for(key,value in map){...}`
- 调用 Java 类：`java.util`/`java.lang` 包下的类**直接用简单类名**（默认 `auto-import-package: java.lang.*,java.util.*`），**禁写 `java.util.`/`java.lang.` 前缀**——`java` 不是变量，写全限定链报 NPE "target is null"：

```javascript
return new ArrayList();
var requestId = 'reqId-' + UUID.randomUUID().toString().replaceAll('-','').substring(0,20);
import "java.text.SimpleDateFormat";   // 其他包的类需要先 import
return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
```

- import：Java 类 `import 'java.lang.System' as System;`；Spring Bean `import "userService" as userService;`；模块 `import log; import http; import magic;`；页面函数 `import '@/common/encode/md5' as md5;`；页面接口（非 http）`import '@get:/api/user/list' as list;`
- new 对象：`import 'java.util.Date' as Date; return new Date();`
- 异步：`var val = async db.select('...');` 返回 Future，`val.get()` 取结果
- exit/assert（参数校验）：`exit 400,'参数填写有误'`；`assert count == 0 : 400, '手机号已存在'`（等价 if(!=) exit）
- 嵌入其它语言：```` ```javascript ... ``` ```` 围栏块（需 LanguageProvider 支持）

### 3.3 db 模块（默认引入，无需 import）

```
db.select(sql)      // List<Map>；另有 selectInt(sql)/selectOne(sql)/selectValue(sql)
db.page(sql)        // 自动分页，自动读 ?page=1&size=10；db.page(sql,limit,offset) 手动
db.update(sql) / db.insert(sql) / db.batchUpdate(sql, list)
db.select("select * from t where id = #{id}")      // #{} 占位符防注入
db.select("select * from t where name = ${name}")  // ${} 拼接（有注入风险）
db.select("select * from t ?{id!=null,where id = #{id}}")   // 动态 SQL：条件成立才拼接
db.cache("user_cache", 3600*1000).select(sql)      // SQL 缓存；update/insert 时清空同名缓存；db.deleteCache("user_cache")
db.transaction(()=>{ db.update(...); db.update(...); })     // 自动事务，错回滚
var tx = db.transaction(); try{...; tx.commit();} catch(e){ tx.rollback(); }  // 手动事务
db.slave.select(sql)      // 切换数据源（key 为数据源名）
db.camel().select(sql)    // 列名转驼峰；pascal/upper/lower/normal
db.call('call proc(#{in}, @{out,Types.INTEGER})')   // 存储过程（v2.2.0+）
```

**单表 API**（风格类 MyBatis-Plus）：

```
db.table('sys_user').insert({user_name:'李富贵', role:'admin'})  /  .batchInsert(list)
db.table('sys_user').primary('id').update({id:1, user_name:'王二狗'})
db.table('sys_user').primary('id', uuid()).save(data)   // 有 id 则 update 无则 insert
db.table('sys_user').where().like('user_name','%李%').eq('role','admin').select()
// where：eq/ne/lt/gt/lte/gte/in/notIn/like/notLike
// 附加：logic（逻辑删除）、withBlank（不过滤空值）、column(col[,value])
```

**Mybatis 标签**（1.6.0+）：三引号 SQL 中支持 `<if> <elseif> <else> <where> <foreach> <trim> <set>`，用法与 MyBatis 一致。

### 3.4 其它内置模块

```
http:  import http; http.connect('url').header('k','v').body({...}).contentType('application/json').post().getBody()
       http.get('url') / .param(k,v) / .data(k,v) / put/delete/head/patch/options/trace
magic: import magic; magic.call("GET:/xxx/xxx", params)   // 含 code/message，走拦截器
       magic.execute("GET:/xxx/xxx", params)              // 不含 code/message
       magic.invoke("funcId", params)                     // 调用页面函数
log:   log.info('value={}', v); log.debug/warn/error/trace(...)
env:   env.get('JAVA_HOME') / env.get('k','默认') / env.getInt / env.getLong / env.getBoolean
request: request.getFile('name') / getFiles / getValues('name') / getHeaders('name') / get('param') / getClientIP()
response: response.json(obj) / response.text('内容') / response.redirect('url')
       response.download(流, 文件名) / response.image(流) / response.addHeader(k,v) / setHeader / addCookie
       response.getOutputStream().write(...) + response.end()
```

### 3.5 内置函数与类型扩展

- 聚合函数（Linq select 用）：`count/sum/max/min/avg/group_concat`
- 日期：`date_format(date,pattern)`、`now([pattern])`、`current_timestamp_millis()`、`current_timestamp()`
- 字符串：`uuid()`、`is_blank(str)`、`not_blank(str)`；数学：`round/floor/ceil/percent(value[,scale])`；其它：`print/println/printf`、`not_null/is_null/ifnull(x,y)`
- 数组初始化：`new_int_array(len[,default])`（byte/short/long/float/double/boolean/char/string 同理）
- Object 扩展：`asInt/asLong/asDouble/asFloat/asDecimal/asDate([pattern])/asString([pattern])`、`isString/isInt/isCollection/...`
- Number 扩展：`round/toFixed/floor/ceil/asPercent([scale])`
- 集合扩展：`map/filter/each/sort/first/last/reserve/max/min/sum/avg/join(separator)/join(list,f,l)/shuffle/group(find=>key)/every/some/reduce/find/findIndex/findNotNull/concat/toMap(key[,value])/skip(n)/limit(n)/distinct()`
- Date 扩展 `format(pattern)`；Class 扩展 `newInstance/getName/getSimpleName`；Pattern 扩展 `/regex/g.test(input)`

### 3.6 Linq 与 Lambda

```
// Lambda
list.map(item => ({name:item.name, upper:item.name.toUpperCase()}))
list.filter(item => item.sex == 0).map(item => item.name)
result.group(item => item.dept)                // Map<String,List>
result.group(item => item.dept, list => {count:list.size(), sum:list.map(v=>v.value).sum()})
a.join(b, (l,r)=>l.code==r.code, (l,r)=>({'编号':l.code, '去年':r?.sum ?? '-'}))
// Linq（内存集合查询）：select t.name, sum(t.score) score from results t
// left join others t1 on t1.code = t.code where t.age > 18 and t1.status == 'Y'
// group by t.name having count(t.name) > 1 order by t.name desc limit 10 offset 0
```

## 四、权限与安全（UI/接口鉴权）

- 接口鉴权：接口选项配 `permission`/`role`/`require-login`，后端实现 `RequestInterceptor.preHandle(ApiInfo, context, request, response)`，读 `info.getOptionValue(Options.PERMISSION)` 判断，无权限返回 `new JsonBean<>(403,"无权访问")`，放行返回 null。拦截器返回值不走 ResultProvider。
- UI 登录：单用户 `magic-api.security.{username,password}`；多用户实现 `AuthorizationInterceptor`（`requireLogin()`/`login(username,password)` 返回 `MagicUser` / `getUserByToken(token)`）。
- UI 操作鉴权：`AuthorizationInterceptor.allowVisit(user, request, Authorization)`，枚举 SAVE/DELETE/VIEW/LOCK/UNLOCK/DOWNLOAD/UPLOAD/PUSH，可细化到接口（MagicEntity）/分组（Group）级。
- 编辑器 token：`magic-api.editor-config: classpath:./magic-editor-config.js` 中 `MAGIC_EDITOR_CONFIG.getMagicTokenValue()` 自定义。

## 五、高级扩展点

| 扩展点 | 实现接口 | 说明 |
|---|---|---|
| 自定义拦截器 | `RequestInterceptor`（@Component+@Order） | preHandle 返回对象直接响应（不走 ResultProvider），null 继续；postHandle 处理返回值 |
| SQL 拦截器 | `SQLInterceptor` | preHandle(BoundSql, RequestEntity) 改写 SQL/参数 |
| 单表 API 拦截器 | `NamedTableInterceptor` | preHandle(SqlMode, NamedTable)，如 INSERT 注入 create_by |
| SQL 缓存 | `SqlCache` | put/get/delete |
| 自定义模块 | `@MagicModule("test")` + @Component，方法加 @Comment | 脚本 `import test; test.xxx()`；全局导入 `magic-api.auto-import-module: db,test` |
| 自定义函数 | `MagicFunction`，方法加 @Function+@Comment | 脚本直接调用 |
| 类型扩展 | `ExtensionMethod`，方法 public static、首参为目标类型、@Comment | 脚本 `str.toInt()` |
| 自定义存储 | `Resource` 接口 / 继承 DatabaseResource、RedisResource | |
| 列名转换 | `ColumnMapperProvider` | 配 `magic-api.sql-column-case=custom` 或脚本 `db.columnCase('custom')` |
| 方言 | `Dialect` | match(jdbcUrl) + getPageSql |
| 自定义脚本语言 | `LanguageProvider` | support(languageName)+execute；脚本 ```` ```lang ``` ```` 围栏 |
| 统一异常 | `ResultProvider.buildException` | 或配 `magic-api.throw-exception: true` 交 Spring |
| 自定义 JSON 结构 | `magic-api.response`（magic-script 表达式）或 ResultProvider | 默认 `{code,message,data,timestamp,executeTime}` |
| 分页自定义 | `PageProvider` | 自定义分页参数获取 |

**Java 侧调用 API**（与 MCP 工具对应，便于理解底层）：

```
MagicAPIService: execute("GET","/hello",params) / call(...) / invoke("/func",params) / upload(in,mode) / download(...) / push(...)
MagicResourceService: saveGroup(group) / saveFile(apiInfo) / delete(id) / listFiles(groupId) / tree(type) / refresh() / move(src,groupId) / copyGroup(src,target) / file(id) / getGroup(id) / getGroupPath(groupId)
```

## 六、插件

| 插件 | 依赖 | 要点 |
|---|---|---|
| 定时任务 | `magic-api-plugin-task` | UI 左侧出现定时任务，cron+脚本；线程池配 `magic-api.task.*` |
| 集群 | `magic-api-plugin-cluster` | 多实例同步（依赖 redis 插件）；`magic-api.cluster.channel`；instanceId 每台不同 |
| Redis | `magic-api-plugin-redis` | 存储到 Redis（`resource.type: redis`）+ 脚本 `import redis; redis.set/get/setex` |
| Mongo | `magic-api-plugin-mongo` | `import mongo; mongo.db.coll.insert/find(...).skip().limit().list()/update/count/remove` |
| ElasticSearch | `magic-api-plugin-elasticsearch` | `import elasticsearch; index().save/insert/delete/bulkSave/search(dsl)/rest(...)` |
| Swagger | `magic-api-plugin-swagger` | 仅 Spring Boot 2.x；`@EnableSwagger2`；访问 /swagger-ui.html |
| SpringDoc | `magic-api-plugin-springdoc` | 仅 Spring Boot 3.x |
| Git | `magic-api-plugin-git` | 存 git 仓库（resource.type: git）；预览特性 |
| nebula | `magic-api-plugin-nebula` | v2.2.0+；`import nebula; nebula.executeJson(ngsl)` 图数据库 |
| 插件开发 | — | `MagicPluginConfiguration` + spring.factories + 前端 magic-editor/plugins |

## 七、配置参考（Spring Boot）

```yaml
magic-api:
  web: /magic/web              # UI 入口（生产建议关闭）
  prefix: /                    # 接口全局前缀
  resource:
    type: file                 # file（location）/ database（tableName,datasource,prefix）/ redis（prefix）/ git
    location: /data/magic-api  # file 专用；classpath: 开头=只读
    readonly: false
    backup:                    # 备份（MCP backup_* 依赖）
      enable: true             # 默认 false
      max-history: -1          # 保留天数，-1 永久
      table-name: magic_api_backup  # 存数据库
  auto-import-module: db       # 自动导入模块
  allow-override: false        # 允许覆盖应用接口
  sql-column-case: camel       # default/camel/pascal/upper/lower
  show-sql: true
  compile-cache-size: 500
  thread-pool-executor-size: 8 # async 线程池
  throw-exception: false
  support-cross-domain: true
  secret-key: 123456789        # 推送秘钥
  response: |-                 # 自定义 JSON 结构（magic-script 表达式）
    {code: code, message: message, data, timestamp, requestTime, executeTime}
  response-code: {success: 1, invalid: 0, exception: -1}
  crud: {logic-delete-column: deleted, logic-delete-value: 1}
  cache: {enable: true, capacity: 10000, ttl: -1}
  page: {size: size, page: page, default-page: 1, default-size: 10}
  security: {username: admin, password: 123456}
  debug: {timeout: 60}
  mcp:                         # MCP 附加配置（全部可选）
    enabled: true              # 启用 MCP
    path: /mcp/magic           # 端点路径
    token: ''                  # 鉴权 token（生产必配）
```

**编辑器配置**（`magic-api.editor-config` 的 JS 文件）：baseURL/serverURL/title/theme(default|dark)/autoSave/jdbcDrivers/options(接口选项列表)/getMagicTokenValue/request.beforeSend/response/themes(配色) 等，定制编辑器外观与集成宿主应用鉴权。

## 八、FAQ（常见问题速查）

- 脚本报 NPE "target is null"（指向 `java.util.Xxx` 处）：脚本中 `java` 不是变量，`java.util.` 前缀是全限定成员访问链。java.util/java.lang 包下的类直接用简单类名（如 `UUID.randomUUID()`、`new Date()`），其他包先 import（详见 3.2）
- JSON 日期格式：`spring.jackson.{time-zone: GMT+8, date-format: yyyy-MM-dd HH:mm:ss}`；LocalDateTime 需注册 LocalDateTimeSerializer
- 找不到 db 模块：未配数据源或未引入 spring-boot-starter-jdbc
- `${}` vs `#{}`：`${}` 拼接（注入风险），`#{}` 占位符（安全）；集合参数 `in(#{ids})` 自动展开为 `in(?,?,?)`
- 多数据源：@Bean 注册 `MagicDynamicDataSource`（setDefault 必设）+ `db.slave.select(...)`
- 自定义返回结果：magic-api.response 配置 / ResultProvider / 拦截器 / ResponseBodyAdvice（不推荐）
- UI 加载慢：`server.compression.enabled: true`（monaco-editor 较大）
- UI 404：查 web 路径、magic-editor 依赖；MVC 若 extends WebMvcConfigurationSupport 改为 implements WebMvcConfigurer
- 无法 DEBUG：DEBUG/日志依赖 WebSocket，nginx 需配 Upgrade 头（location /magic/web/console）
- 脚本内容被转义：XSS 过滤器排除 UI 后台接口
- Blob 存库：`#{img::sql('blob')}`（java.sql.Types）
- 1.x → 2.x：不能直接升级；先导出全部资源 → 升级 → 建 magic_api_file_v2 表（resource.type: database）→ 全量上传 zip

## 九、更新日志要点（版本兼容性）

- v2.0.0（2022.03）：存储层重构，1.x 不直接兼容；模块改 @MagicModule 注解；集群/Redis/Mongo 拆为插件
- v2.1.0（2023.04）：兼容 Spring Boot 3.0；新增自定义脚本语言
- v2.2.0（2025.05）：兼容 Spring Boot 3.4；新增 nebula 插件；`db.call` 存储过程；http 模块 expectString
- v2.2.2（2025.06，当前版本）：修复单表 API 主键类型、兼容 SpringDoc 2.8；保存接口传输加密
- 本 MCP 模块基于 magic-api **2.2.2**（Spring Boot 2.4.5 / Java 8+，javax.servlet）

## 附：官方文档侧边栏完整目录（70 页）

所有 URL = `https://www.ssssssss.org/magic-api/pages/` + 路径(页面名)：

- 快速入门：quick/ 下 intro(简介)、start(快速开始)、param(参数获取)、single(单表crud)、crud(增删改查)、page(分页)
- 基础教程：base/ 下 script(脚本语法)、lambda(Lambda)、linq(Linq)、page(界面简介)、release(接口发布)、async(异步调用)、api(Java调用接口)、java(脚本调用Java)、validate(参数校验)、exception(统一异常处理)、response(统一请求响应)、datasource(配置多数据源)、upgrade2.x(从1.x迁移)
- 权限配置：security/ 下 api(接口鉴权)、login(UI鉴权登录)、operation(UI操作鉴权)
- 高级应用：senior/ 下 interceptor(拦截器)、sql-interceptor(SQL拦截器)、table-interceptor(单表API拦截器)、cache(SQL缓存)、module、function、extension(类型扩展)、resource(接口存储)、mapping(列名转换)、dialect(数据库方言)、script(脚本语言)，页面名均冠"自定义"
- 插件：plugin/ 下 dev(插件开发)、task(定时任务)、cluster(集群)、redis(Redis)、mongo(Mongo)、elasticsearch(ElasticSearch)、swagger(Swagger)、springdoc(SpringDoc)、git(Git)；nebula 为 `5f9028`
- 配置：config/ 下 spring-boot(SpringBoot配置)、editor(编辑器配置)
- API：module/ 下 db/http/magic/log/env/request/response（页面名即 xx模块）；function/ 下 aggregation(聚合)、date、string、array、math、other；extension/ 下 object(Object扩展)、number(Number扩展)、collection(数组&集合扩展)、date(Date扩展)、class(Class扩展)、pattern(Pattern扩展)
- 其它：group(交流群)、faq(常见问题)、changelog/v0、changelog/v1、changelog/v2(更新日志v0.x/v1.x/v2.x)、sponsor(赞助支持)
