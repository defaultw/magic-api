---
name: magic-api
description: 通过 MCP 协议管理 magic-api 接口平台：分组管理、接口增删改查、参数配置、响应设置、脚本调试执行、备份导入导出。当用户要求创建/编辑/删除/调试 magic-api 接口或分组、查询接口列表、导出导入接口包时使用本技能。包含 25 个 MCP 工具的完整参数规范与 magic-api 脚本开发全量知识（官方文档 70 页收录）。
---

# magic-api MCP 技能

magic-api 是基于 Java 的接口快速开发框架，通过界面编写脚本（magic-script）并自动映射为 HTTP 接口，无需 Controller/Service/Dao/Mapper/VO 等对象。本技能使智能体可通过 MCP 协议完整操作 magic-api 的全部能力。

---

## 一、MCP 服务接入

### 端点与协议

- **端点**：`POST http://<host>:<port>/mcp/magic`（路径可通过 `magic-api.mcp.path` 修改）
- **协议**：JSON-RPC 2.0 over HTTP，`Content-Type: application/json; charset=utf-8`
- **协议版本**：`2024-11-05`
- **支持方法**：`initialize` / `notifications/initialized` / `ping` / `tools/list` / `tools/call`
- **鉴权**：默认开放；若服务端配置 `magic-api.mcp.token`，请求须携带 `X-Mcp-Token: <token>` 或 `Authorization: Bearer <token>`

### 接入步骤

```bash
# Step 1 握手（响应头 Mcp-Session-Id 为会话 ID，后续可携带）
curl -X POST http://localhost:8080/mcp/magic -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"agent","version":"1.0"}}}'

# Step 2 通知初始化完成
curl -X POST http://localhost:8080/mcp/magic -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'

# Step 3 获取工具清单
curl -X POST http://localhost:8080/mcp/magic -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# Step 4 调用工具
curl -X POST http://localhost:8080/mcp/magic -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"magic_group_tree","arguments":{"type":"api"}}}'
```

### 响应格式

```json
// 成功
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"<结果>"}],"isError":false}}
// 工具业务错误（参数缺失/资源不存在等）：HTTP 200，isError=true，text 为错误说明
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"缺少必填参数: id"}],"isError":true}}
// 未知工具
{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"未知工具: xxx"}}
```

智能体处理约定：`isError=true` 时把 text 内容作为错误反馈给用户并修正参数重试；未知工具时先调 `tools/list` 核对工具名。

### 即插即用说明

MCP 模块以 `magic-api-mcp` 依赖引入 Spring Boot Web 工程即自动装配，零配置。若工程同时引入 `magic-api-spring-boot-starter`，编辑器（默认 `/magic/web`）与 MCP 端点共存，两边修改实时互通（共享同一套存储与注册机制）。

---

## 二、工具清单与参数规范（25 个）

> 命名规范：`magic_<域>_<动作>`。分组域=group（树/查/存/删/移/复制），接口域=api（列表/详情/保存/删除/移动/锁/搜索），执行域独立，辅助域=resource/config/plugins/backup/export/upload。
> 路径拼接规则：**接口完整路径 = 全局 prefix（默认 /）+ 分组 path + 接口 path**。执行/调试时务必用完整路径（分组 path 与接口 path 都计入）。

### 2.1 分组管理（6 个）

**magic_group_tree** — 获取分组树

```json
{"type": "api"}          // 可选 type：api/function/function-class/datasource/columns，默认 api
// 返回嵌套 TreeNode：[{node:{id,name,type,path,parentId,...},children:[...]}]
```

**magic_group_get** — 分组详情

```json
{"id": "<分组ID>"}       // 必填
```

**magic_group_save** — 新增/编辑分组

```json
// 新增（根级分组）：
{"name": "订单管理", "path": "order"}
// 新增（子分组）：
{"name": "退款", "parentId": "<父分组ID>", "path": "refund"}
// 编辑（传 id，parentId 缺省保持不变则须显式传原 parentId）：
{"id": "<分组ID>", "name": "新名字", "parentId": "<父分组ID>"}
```

| 参数 | 必填 | 说明 |
|---|---|---|
| name | 是 | 分组名（作为文件夹名，不可含非法字符） |
| id | 否 | 传了=编辑，不传=新增 |
| parentId | 否 | 父分组 ID，默认 `"0"`（根节点） |
| path | 否 | 分组路径（拼接进接口 URL，可省略） |
| type | 否 | 默认 `api`；其他合法值：function/function-class/datasource/columns |

**magic_group_delete** — 删除分组（级联删除子分组与文件）或按 id 删文件

```json
{"id": "<分组ID或文件ID>"}
```

**magic_group_move** — 把分组（或文件）移动到目标分组下

```json
{"src": "<源分组ID>", "groupId": "<目标父分组ID>"}
```

**magic_group_copy** — 复制分组（返回新分组 id）

```json
{"src": "<源分组ID>", "target": "<目标父分组ID>"}   // target 缺省为根节点
```

### 2.2 接口管理（8 个）

**magic_api_list** — 分组下接口列表

```json
{"groupId": "<分组ID>"}   // 必填
```

**magic_api_get** — 接口详情（脚本/参数/选项/请求体/响应设置全量）

```json
{"id": "<接口ID>"}        // 必填；返回 ApiInfo 全字段
```

**magic_api_save** — 新增/编辑接口（参数配置+响应设置全承载）

```json
{
  "groupId": "<分组ID>",           // 必填
  "name": "订单详情",               // 必填
  "path": "/detail",               // 必填（相对分组路径）
  "script": "return db.table('t_order').selectById(id)",   // 必填 magic-script 脚本
  "method": "POST",                // 可选，默认 GET；GET/POST/PUT/DELETE...
  "id": "<接口ID>",                // 可选，传了=编辑
  "parameters": [                  // 可选，请求参数定义（编辑器"请求参数"栏）
    {"name": "id", "value": "", "description": "订单ID", "required": true, "dataType": "String"}
  ],
  "headers": [                     // 可选，请求头定义
    {"name": "X-Token", "value": "", "description": "鉴权令牌"}
  ],
  "paths": [                       // 可选，路径变量定义（path 为 /user/{id} 时）
    {"name": "id", "value": "", "description": "用户ID", "required": true, "dataType": "Long"}
  ],
  "options": [                     // 可选，接口选项（鉴权/缓存/事务等）
    {"name": "anonymous", "value": false},
    {"name": "transaction", "value": true}
  ],
  "requestBody": "{\"id\":\"订单ID\"}",            // 可选，RequestBody 说明
  "responseBody": "{\"code\":1,\"data\":{...}}",   // 可选，ResponseBody 说明
  "description": "查询订单详情"                     // 可选，接口描述
}
// 返回：接口 id
```

常用接口选项（options.name）：`anonymous`（允许匿名）、`transaction`（开启事务）、`readonly`（只读事务）、`page`（分页)、`page-size`、`cache`（缓存名）、`cache-ttl`（缓存毫秒）、`permission`（权限编码）、`role`（角色）、`require-login`（需登录）、`time-out`（超时毫秒）、`swagger-define`。与编辑器选项一致。

**magic_api_delete** — 删除接口或分组

```json
{"id": "<接口ID或分组ID>"}
```

**magic_api_move** — 把接口移动到目标分组

```json
{"src": "<接口ID>", "groupId": "<目标分组ID>"}
```

**magic_api_lock / magic_api_unlock** — 锁定/解锁接口（防止他人编辑）

```json
{"id": "<接口ID>"}
```

**magic_api_search** — 按名称/路径模糊搜索

```json
{"keyword": "订单"}       // 必填；返回匹配的接口与分组
```

### 2.3 执行与调试（2 个）

**magic_api_execute** — 执行**已发布**接口（进程内调用，不走 HTTP）

```json
{"method": "POST", "path": "order/detail", "context": {"id": "123"}}
// method 默认 GET；path 为完整路径（分组 path + 接口 path，不含全局 prefix）
// context 注入变量：Query 参数直接放顶层 {"num": 1}；POST body 用 {"body": {...}}；header 用 {"header": {...}}；路径变量用 {"path": {...}}
// 返回：{"code":1,"message":"success","data":...,"timestamp":...,"executeTime":...}
```

**magic_api_debug** — 调试脚本（临时注册后立即注销，**不落盘**）

```json
{
  "script": "return body.num * 2",   // 必填
  "method": "POST",                  // 可选默认 GET
  "groupId": "<分组ID>",             // 可选（影响路径前缀）
  "path": "/debug/test",             // 可选默认 /debug-{uuid}
  "name": "调试接口",                 // 可选
  "parameters": {"num": 21},         // 可选，模拟 Query 参数
  "headers": {"token": "xxx"},       // 可选，模拟请求头
  "pathVariables": {"id": "1"},      // 可选，模拟路径变量
  "body": {"num": 21}                // 可选，模拟请求体（脚本中用 body.xxx 取）
}
// 返回脚本执行结果
```

### 2.4 辅助管理（9 个）

**magic_resource_tree** — 全量资源树（所有分组类型：api/function/datasource 等）

```json
{}
```

**magic_config_get** — 运行配置（prefix/web 路径/存储方式/插件配置等）

```json
{}
```

**magic_plugins_list** — 已安装插件列表

```json
{}
```

**magic_backup_list** — 查询备份记录

```json
{}                        // 可选 {"timestamp": 1700000000000} 查某时间点之前的记录
```

**magic_backup_restore** — 还原备份

```json
{"id": "<备份ID>"}        // 必填；可选 {"timestamp": ...} 缩小范围
```

**magic_backup_delete** — 删除备份

```json
{"id": "<备份ID>"}
```

**magic_backup_full** — 执行全量备份

```json
{}
```

**magic_api_export** — 导出接口包（zip 的 base64）

```json
{"groupId": "<分组ID>"}   // 可选；也可 {"resources": ["<id1>","<id2>"]} 按资源导出；都不传=全量
// 返回 base64 字符串（zip 文件）
```

**magic_api_upload** — 导入接口包（base64→zip→导入）

```json
{"fileBase64": "<magic_api_export 返回的 base64>", "mode": "full"}
// mode：full=全量覆盖（先清空再导入，慎用）；increment=增量（只更新存在的）
```

> 备份类工具依赖 `magic-api.resource.backup` 配置；未启用时返回说明文本不会报错。

---

## 三、magic-script 脚本开发要点

编写 `script` 字段所需的全部语言与模块知识。

### 3.1 参数获取（接口入参）

| 来源 | 脚本访问 | 说明 |
|---|---|---|
| Query/表单参数 | 直接用变量名 `name` | `GET /x?name=abc` 中直接写 `name` |
| 请求体 | `body.xxx` | POST JSON 整体映射为 `body`；body 为数组可遍历 |
| 请求头 | `header.xxx` | 如 `header.token` |
| 路径变量 | `path.xxx` 或变量名 | path 定义 `/user/{id}` 时用 `path.id`（避免与同名 Query 冲突） |
| Cookie | `cookie.xxx` | 如 `cookie.JSESSIONID` |
| Session | `session.xxx` | |
| 多值参数 | `request.getValues('name')` | `import request;` |
| 上传文件 | `request.getFile('name')` | `import request;` |

SQL 中引用：`#{body.xxx}`、`#{header.xxx}`、`#{path.xxx}`。脚本自定义变量与参数同名时，**自定义变量优先**。

### 3.2 脚本语法

- **关键字**：var/if/else/for/in/while/continue/break/return/exit/assert/instanceof/try/catch/finally/import/as/new/true/false/null/async
- **类型字面量**：`123` int、`123l` long、`123.5d` double、`123f` float、`123m` BigDecimal、`true/false`、`'str'`/`"str"`、`"""多行文本块（常用于SQL）"""`、`/\d+/g` 正则、`[1,2,3]` list、`{key:'value'}` map、`(x)=>x+1` lambda
- **类型转换**：`"123"::int`、`"abc"::int(111)`（失败给默认值）、`"2020-01-01"::date('yyyy-MM-dd')`
- **可选链**：`a?.name`（a 为 null 短路返回 null）
- **扩展运算符**：`sum(...[1,2,3])`；`[1,2,...arr]`；`{k1:1,...map}`（map 展开到 map）
- **for 循环**（仅集合与 Map）：
  ```
  for(index,item in list){ ... }
  for(value in range(0,100)){ ... }    // 含 0 含 100
  for(key,value in map){ ... }
  ```
- **调用 Java 类**：
  - `java.util`、`java.lang` 包下的类**直接用简单类名**（默认 `auto-import-package: java.lang.*,java.util.*`），**禁止写 `java.util.` / `java.lang.` 前缀**——脚本中 `java` 不是变量，写全限定链会报 NPE "target is null"：
    ```
    return new ArrayList();                                                    // 直接用
    var requestId = 'reqId-' + UUID.randomUUID().toString().replaceAll('-', '').substring(0, 20);   // 直接用
    ```
  - 其他包的类需要先 import：
    ```
    import "java.text.SimpleDateFormat";
    return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    ```
- **import**：
  ```
  import 'java.lang.System' as System;   // Java 类
  import "userService" as userService;   // Spring Bean
  import log;  import http;  import magic;   // 模块
  import '@/common/encode/md5' as md5;   // 调用页面函数
  import '@get:/api/user/list' as list;  // 调用页面接口（非 http）
  ```
- **new 对象**：`import 'java.util.Date' as Date; return new Date();`
- **异步**：`var val = async db.select('...');` 返回 Future，`val.get()` 取结果
- **exit/assert**（参数校验）：
  ```
  exit 400,'参数填写有误'
  assert count == 0 : 400, '手机号已存在'   // 等价 if(!=) exit
  ```
- **嵌入其它语言**：```` ```javascript ... ``` ```` 围栏块（需 LanguageProvider 支持）

### 3.3 db 模块（默认引入，无需 import）

```
db.select(sql)                 // List<Map>
db.selectInt(sql) / db.selectOne(sql) / db.selectValue(sql)
db.page(sql)                   // 自动分页，自动读 ?page=1&size=10
db.page(sql, limit, offset)    // 手动分页
db.update(sql) / db.insert(sql) / db.batchUpdate(sql, list)
db.select("select * from t where id = #{id}")      // #{} 占位符防注入
db.select("select * from t where name = ${name}")  // ${} 拼接（有注入风险）
db.select("select * from t ?{id!=null,where id = #{id}}")   // 动态 SQL：条件成立才拼接
db.cache("user_cache", 3600*1000).select(sql)      // SQL 缓存；update/insert 时清空同名缓存
db.deleteCache("user_cache")
db.transaction(()=>{ db.update(...); db.update(...); })     // 自动事务，出错回滚
var tx = db.transaction(); try{...; tx.commit();} catch(e){ tx.rollback(); }  // 手动事务
db.slave.select(sql)           // 切换数据源（key 为数据源名）
db.camel().select(sql)         // 列名转驼峰；pascal/upper/lower/normal
db.call('call proc(#{in}, @{out,Types.INTEGER})')   // 存储过程（v2.2.0+）
```

**单表 API**（风格类 MyBatis-Plus）：

```
db.table('sys_user').insert({user_name:'李富贵', role:'admin'})
db.table('sys_user').batchInsert(list)
db.table('sys_user').primary('id').update({id:1, user_name:'王二狗'})
db.table('sys_user').primary('id', uuid()).save(data)   // 有 id 则 update 无则 insert
db.table('sys_user').where().like('user_name','%李%').eq('role','admin').select()
// where 条件：eq/ne/lt/gt/lte/gte/in/notIn/like/notLike
// 附加：logic（逻辑删除）、withBlank（不过滤空值）、column(col[,value])
```

**Mybatis 标签**（1.6.0+）：三引号 SQL 中支持 `<if> <elseif> <else> <where> <foreach> <trim> <set>`，用法与 MyBatis 一致。

### 3.4 其它内置模块

```
http:  import http; http.connect('url').header('k','v').body({...}).contentType('application/json').post().getBody()
       http.get('url') / .param(k,v) / .data(k,v) / put/delete/head/patch/options/trace
magic: import magic; magic.call("GET:/xxx/xxx", params)     // 含 code/message，走拦截器
       magic.execute("GET:/xxx/xxx", params)                // 不含 code/message
       magic.invoke("funcId", params)                       // 调用页面函数
log:   log.info('value={}', v); log.debug/warn/error/trace(...)
env:   env.get('JAVA_HOME') / env.get('k','默认') / env.getInt / env.getLong / env.getBoolean
request: request.getFile('name') / getFiles / getValues('name') / getHeaders('name') / get('param') / getClientIP()
response: response.json(obj) / response.text('内容') / response.redirect('url')
       response.download(流, 文件名) / response.image(流) / response.addHeader(k,v) / setHeader / addCookie
       response.getOutputStream().write(...) + response.end()
```

### 3.5 内置函数与类型扩展

- **聚合函数**（Linq select 中用）：`count/sum/max/min/avg/group_concat`
- **日期**：`date_format(date,pattern)`、`now([pattern])`、`current_timestamp_millis()`、`current_timestamp()`
- **字符串**：`uuid()`、`is_blank(str)`、`not_blank(str)`
- **数学**：`round/floor/ceil/percent(value[,scale])`
- **其它**：`print/println/printf`、`not_null/is_null/ifnull(x,y)`
- **数组初始化**：`new_int_array(len[,default])`（byte/short/long/float/double/boolean/char/string 同理）
- **Object 扩展**：`asInt/asLong/asDouble/asFloat/asDecimal/asDate([pattern])/asString([pattern])`、`isString/isInt/isCollection/...`
- **Number 扩展**：`round/toFixed/floor/ceil/asPercent([scale])`
- **集合扩展**：`map/filter/each/sort/first/last/reserve/max/min/sum/avg/join(separator)/join(list,f,l)/shuffle/group(find=>key)/every/some/reduce/find/findIndex/findNotNull/concat/toMap(key[,value])/skip(n)/limit(n)/distinct()`
- **Date 扩展**：`format(pattern)`；**Class 扩展**：`newInstance/getName/getSimpleName`；**Pattern 扩展**：`/regex/g.test(input)`

### 3.6 Linq 与 Lambda

```
// Lambda
list.map(item => ({name:item.name, upper:item.name.toUpperCase()}))
list.filter(item => item.sex == 0).map(item => item.name)
result.group(item => item.dept)                    // Map<String,List>
result.group(item => item.dept, list => {count:list.size(), sum:list.map(v=>v.value).sum()})
a.join(b, (l,r)=>l.code==r.code, (l,r)=>({'编号':l.code, '去年':r?.sum ?? '-'}))

// Linq（内存集合查询）
select t.name, sum(t.score) score
from results t
left join others t1 on t1.code = t.code
where t.age > 18 and t1.status == 'Y'
group by t.name
having count(t.name) > 1
order by t.name desc
limit 10 offset 0
```

---

## 四、权限与安全（UI/接口鉴权）

- **接口鉴权**：编辑器接口选项配置 `permission`/`role`/`require-login`，后端实现 `RequestInterceptor` 的 `preHandle(ApiInfo, context, request, response)`，读 `info.getOptionValue(Options.PERMISSION)` 判断，无权限返回 `new JsonBean<>(403,"无权访问")`，放行返回 null。拦截器返回值不走 ResultProvider。
- **UI 登录**：单用户 `magic-api.security.{username,password}`；多用户实现 `AuthorizationInterceptor`（`requireLogin()`/`login(username,password)` 返回 `MagicUser` / `getUserByToken(token)`）。
- **UI 操作鉴权**：`AuthorizationInterceptor.allowVisit(user, request, Authorization)`，枚举 SAVE/DELETE/VIEW/LOCK/UNLOCK/DOWNLOAD/UPLOAD/PUSH，可细化到接口（MagicEntity）与分组（Group）级别。
- **编辑器 token**：`magic-api.editor-config: classpath:./magic-editor-config.js` 中 `MAGIC_EDITOR_CONFIG.getMagicTokenValue()` 自定义。

---

## 五、高级扩展点

| 扩展点 | 实现接口 | 说明 |
|---|---|---|
| 自定义拦截器 | `RequestInterceptor`（@Component+@Order） | preHandle 返回对象直接响应（不走 ResultProvider），返回 null 继续；postHandle 处理返回值 |
| SQL 拦截器 | `SQLInterceptor` | preHandle(BoundSql, RequestEntity) 改写 SQL/参数 |
| 单表 API 拦截器 | `NamedTableInterceptor` | preHandle(SqlMode, NamedTable)，如 INSERT 注入 create_by |
| SQL 缓存 | `SqlCache` | put/get/delete |
| 自定义模块 | `@MagicModule("test")` + @Component，方法加 @Comment | 脚本 `import test; test.xxx()`；全局导入 `magic-api.auto-import-module: db,test` |
| 自定义函数 | `MagicFunction`，方法加 @Function+@Comment | 脚本直接调用 |
| 类型扩展 | `ExtensionMethod`，方法 public static、首参为目标类型、@Comment | 脚本 `str.toInt()` |
| 自定义存储 | `Resource` 接口 / 继承 DatabaseResource、RedisResource | |
| 列名转换 | `ColumnMapperProvider` | 配 `magic-api.sql-column-case=custom` 或脚本 `db.columnCase('custom')` |
| 方言 | `Dialect` | match(jdbcUrl) + getPageSql |
| 自定义脚本语言 | `LanguageProvider` | support(languageName) + execute；脚本 ```` ```lang ``` ```` 围栏 |
| 统一异常 | `ResultProvider.buildException` | 或配置 `magic-api.throw-exception: true` 交给 Spring |
| 自定义 JSON 结构 | `magic-api.response`（magic-script 表达式）或 ResultProvider | 默认结构 `{code,message,data,timestamp,executeTime}` |
| 分页自定义 | `PageProvider` | 自定义分页参数获取 |

**Java 侧调用 API**（与 MCP 工具能力对应，便于理解底层）：

```
MagicAPIService: execute("GET","/hello",params) / call(...) / invoke("/func",params) / upload(in,mode) / download(...) / push(...)
MagicResourceService: saveGroup(group) / saveFile(apiInfo) / delete(id) / listFiles(groupId) / tree(type) / refresh() / move(src,groupId) / copyGroup(src,target) / file(id) / getGroup(id) / getGroupPath(groupId)
```

---

## 六、插件

| 插件 | 依赖 | 要点 |
|---|---|---|
| 定时任务 | `magic-api-plugin-task` | UI 左侧出现定时任务，cron+脚本；线程池配置 `magic-api.task.*` |
| 集群 | `magic-api-plugin-cluster` | 多实例同步（依赖 redis 插件）；`magic-api.cluster.channel`；instanceId 每台不同 |
| Redis | `magic-api-plugin-redis` | 存储到 Redis（`resource.type: redis`）+ 脚本 `import redis; redis.set/get/setex` |
| Mongo | `magic-api-plugin-mongo` | `import mongo; mongo.db.coll.insert/find(...).skip().limit().list()/update/count/remove` |
| ElasticSearch | `magic-api-plugin-elasticsearch` | `import elasticsearch; index().save/insert/delete/bulkSave/search(dsl)/rest(...)` |
| Swagger | `magic-api-plugin-swagger` | 仅 Spring Boot 2.x；`@EnableSwagger2`；访问 /swagger-ui.html |
| SpringDoc | `magic-api-plugin-springdoc` | 仅 Spring Boot 3.x |
| Git | `magic-api-plugin-git` | 存储到 git 仓库（resource.type: git）；预览特性 |
| nebula | `magic-api-plugin-nebula` | v2.2.0+；`import nebula; nebula.executeJson(ngsl)` 图数据库 |
| 插件开发 | — | `MagicPluginConfiguration` + spring.factories + 前端 magic-editor/plugins |

---

## 七、配置参考（Spring Boot）

```yaml
magic-api:
  web: /magic/web                    # UI 入口（生产建议关闭）
  prefix: /                          # 接口全局前缀
  resource:
    type: file                       # file（location）/ database（tableName,datasource,prefix）/ redis（prefix）/ git
    location: /data/magic-api        # file 专用；classpath: 开头=只读
    readonly: false
    backup:                          # 备份（MCP backup_* 工具依赖）
      enable: true                   # 默认 false
      max-history: -1                # 保留天数，-1 永久
      table-name: magic_api_backup   # 存数据库
  auto-import-module: db             # 自动导入模块
  allow-override: false              # 允许覆盖应用接口
  sql-column-case: camel             # default/camel/pascal/upper/lower
  show-sql: true
  compile-cache-size: 500
  thread-pool-executor-size: 8       # async 线程池
  throw-exception: false
  support-cross-domain: true
  secret-key: 123456789              # 推送秘钥
  response: |-                       # 自定义 JSON 结构（magic-script 表达式）
    {code: code, message: message, data, timestamp, requestTime, executeTime}
  response-code: {success: 1, invalid: 0, exception: -1}
  crud: {logic-delete-column: deleted, logic-delete-value: 1}
  cache: {enable: true, capacity: 10000, ttl: -1}
  page: {size: size, page: page, default-page: 1, default-size: 10}
  security: {username: admin, password: 123456}
  debug: {timeout: 60}

# MCP 模块附加配置（全部可选）
  mcp:
    enabled: true                    # 是否启用 MCP
    path: /mcp/magic                 # 端点路径
    token: ''                        # 鉴权 token（生产必配）
```

**编辑器配置**（`magic-api.editor-config` 的 JS 文件）：baseURL/serverURL/title/theme(default|dark)/autoSave/jdbcDrivers/options(接口选项列表)/getMagicTokenValue/request.beforeSend/response/themes(配色) 等，用于定制编辑器外观与集成宿主应用鉴权。

---

## 八、FAQ（常见问题速查）

- **脚本报 NPE "target is null"（指向 `java.util.Xxx` 处）**：脚本中 `java` 不是变量，`java.util.` 前缀是全限定成员访问链。java.util/java.lang 包下的类直接用简单类名（如 `UUID.randomUUID()`、`new Date()`），其他包先 `import "java.text.SimpleDateFormat";`（详见 3.2 节）
- **JSON 日期格式**：`spring.jackson.{time-zone: GMT+8, date-format: yyyy-MM-dd HH:mm:ss}`；LocalDateTime 需注册 LocalDateTimeSerializer
- **找不到 db 模块**：未配置数据源或未引入 spring-boot-starter-jdbc
- **`${}` vs `#{}`**：`${}` 拼接（注入风险），`#{}` 占位符（安全）；集合参数 `in(#{ids})` 自动展开为 `in(?,?,?)`
- **多数据源**：@Bean 注册 `MagicDynamicDataSource`（setDefault 必须设置）+ `db.slave.select(...)`
- **自定义返回结果**：magic-api.response 配置 / ResultProvider / 拦截器 / ResponseBodyAdvice（不推荐）
- **UI 加载慢**：`server.compression.enabled: true`（monaco-editor 较大）
- **UI 404**：检查 web 路径、magic-editor 依赖；MVC 若 extends WebMvcConfigurationSupport 改为 implements WebMvcConfigurer
- **无法 DEBUG**：DEBUG/日志依赖 WebSocket，nginx 需配置 Upgrade 头（location /magic/web/console）
- **脚本内容被转义**：XSS 过滤器排除 UI 后台接口
- **Blob 存库**：`#{img::sql('blob')}`（java.sql.Types）
- **1.x → 2.x**：不能直接升级；先导出全部资源 → 升级 → 建 magic_api_file_v2 表（resource.type: database）→ 全量上传 zip

---

## 九、更新日志要点（版本兼容性）

- **v2.0.0**（2022.03）：存储层重构，1.x 不直接兼容；模块改 @MagicModule 注解；集群/Redis/Mongo 拆为插件
- **v2.1.0**（2023.04）：兼容 Spring Boot 3.0；新增自定义脚本语言支持
- **v2.2.0**（2025.05）：兼容 Spring Boot 3.4；新增 nebula 插件；`db.call` 存储过程；http 模块 expectString
- **v2.2.2**（2025.06，当前版本）：修复单表 API 主键类型、兼容 SpringDoc 2.8；保存接口传输加密
- 本 MCP 模块基于 **magic-api 2.2.2**（Spring Boot 2.4.5 / Java 8+，javax.servlet）

---

## 附：官方文档侧边栏完整目录（70 页）

| 章节 | 页面 | URL |
|---|---|---|
| 快速入门 | 简介 | https://www.ssssssss.org/magic-api/pages/quick/intro/ |
| 快速入门 | 快速开始 | https://www.ssssssss.org/magic-api/pages/quick/start/ |
| 快速入门 | 请求参数获取 | https://www.ssssssss.org/magic-api/pages/quick/param/ |
| 快速入门 | 单表crud接口 | https://www.ssssssss.org/magic-api/pages/quick/single/ |
| 快速入门 | 增删改查 | https://www.ssssssss.org/magic-api/pages/quick/crud/ |
| 快速入门 | 分页 | https://www.ssssssss.org/magic-api/pages/quick/page/ |
| 基础教程 | 脚本语法 | https://www.ssssssss.org/magic-api/pages/base/script/ |
| 基础教程 | Lambda | https://www.ssssssss.org/magic-api/pages/base/lambda/ |
| 基础教程 | Linq | https://www.ssssssss.org/magic-api/pages/base/linq/ |
| 基础教程 | 界面简介 | https://www.ssssssss.org/magic-api/pages/base/page/ |
| 基础教程 | 接口发布 | https://www.ssssssss.org/magic-api/pages/base/release/ |
| 基础教程 | 异步调用 | https://www.ssssssss.org/magic-api/pages/base/async/ |
| 基础教程 | Java调用接口 | https://www.ssssssss.org/magic-api/pages/base/api/ |
| 基础教程 | 脚本调用Java | https://www.ssssssss.org/magic-api/pages/base/java/ |
| 基础教程 | 参数校验 | https://www.ssssssss.org/magic-api/pages/base/validate/ |
| 基础教程 | 统一异常处理 | https://www.ssssssss.org/magic-api/pages/base/exception/ |
| 基础教程 | 统一请求响应 | https://www.ssssssss.org/magic-api/pages/base/response/ |
| 基础教程 | 配置多数据源 | https://www.ssssssss.org/magic-api/pages/base/datasource/ |
| 基础教程 | 从1.x迁移 | https://www.ssssssss.org/magic-api/pages/base/upgrade2.x/ |
| 权限配置 | 接口鉴权 | https://www.ssssssss.org/magic-api/pages/security/api/ |
| 权限配置 | UI鉴权登录 | https://www.ssssssss.org/magic-api/pages/security/login/ |
| 权限配置 | UI操作鉴权 | https://www.ssssssss.org/magic-api/pages/security/operation/ |
| 高级应用 | 自定义拦截器 | https://www.ssssssss.org/magic-api/pages/senior/interceptor/ |
| 高级应用 | 自定义SQL拦截器 | https://www.ssssssss.org/magic-api/pages/senior/sql-interceptor/ |
| 高级应用 | 自定义单表API拦截器 | https://www.ssssssss.org/magic-api/pages/senior/table-interceptor/ |
| 高级应用 | 自定义SQL缓存 | https://www.ssssssss.org/magic-api/pages/senior/cache/ |
| 高级应用 | 自定义模块 | https://www.ssssssss.org/magic-api/pages/senior/module/ |
| 高级应用 | 自定义函数 | https://www.ssssssss.org/magic-api/pages/senior/function/ |
| 高级应用 | 自定义类型扩展 | https://www.ssssssss.org/magic-api/pages/senior/extension/ |
| 高级应用 | 自定义接口存储 | https://www.ssssssss.org/magic-api/pages/senior/resource/ |
| 高级应用 | 自定义列名转换 | https://www.ssssssss.org/magic-api/pages/senior/mapping/ |
| 高级应用 | 自定义数据库方言 | https://www.ssssssss.org/magic-api/pages/senior/dialect/ |
| 高级应用 | 自定义脚本语言 | https://www.ssssssss.org/magic-api/pages/senior/script/ |
| 插件 | 插件开发 | https://www.ssssssss.org/magic-api/pages/plugin/dev/ |
| 插件 | 定时任务插件 | https://www.ssssssss.org/magic-api/pages/plugin/task/ |
| 插件 | 集群插件 | https://www.ssssssss.org/magic-api/pages/plugin/cluster/ |
| 插件 | Redis插件 | https://www.ssssssss.org/magic-api/pages/plugin/redis/ |
| 插件 | Mongo插件 | https://www.ssssssss.org/magic-api/pages/plugin/mongo/ |
| 插件 | ElasticSearch插件 | https://www.ssssssss.org/magic-api/pages/plugin/elasticsearch/ |
| 插件 | Swagger插件 | https://www.ssssssss.org/magic-api/pages/plugin/swagger/ |
| 插件 | SpringDoc插件 | https://www.ssssssss.org/magic-api/pages/plugin/springdoc/ |
| 插件 | Git插件 | https://www.ssssssss.org/magic-api/pages/plugin/git/ |
| 插件 | nebula插件 | https://www.ssssssss.org/magic-api/pages/5f9028/ |
| 配置 | SpringBoot配置 | https://www.ssssssss.org/magic-api/pages/config/spring-boot/ |
| 配置 | 编辑器配置 | https://www.ssssssss.org/magic-api/pages/config/editor/ |
| API | db模块 | https://www.ssssssss.org/magic-api/pages/module/db/ |
| API | http模块 | https://www.ssssssss.org/magic-api/pages/module/http/ |
| API | magic模块 | https://www.ssssssss.org/magic-api/pages/module/magic/ |
| API | log模块 | https://www.ssssssss.org/magic-api/pages/module/log/ |
| API | env模块 | https://www.ssssssss.org/magic-api/pages/module/env/ |
| API | request模块 | https://www.ssssssss.org/magic-api/pages/module/request/ |
| API | response模块 | https://www.ssssssss.org/magic-api/pages/module/response/ |
| API | 聚合函数 | https://www.ssssssss.org/magic-api/pages/function/aggregation/ |
| API | 日期函数 | https://www.ssssssss.org/magic-api/pages/function/date/ |
| API | 字符串函数 | https://www.ssssssss.org/magic-api/pages/function/string/ |
| API | 数组函数 | https://www.ssssssss.org/magic-api/pages/function/array/ |
| API | 数学函数 | https://www.ssssssss.org/magic-api/pages/function/math/ |
| API | 其它函数 | https://www.ssssssss.org/magic-api/pages/function/other/ |
| API | Object扩展 | https://www.ssssssss.org/magic-api/pages/extension/object/ |
| API | Number扩展 | https://www.ssssssss.org/magic-api/pages/extension/number/ |
| API | 数组&集合扩展 | https://www.ssssssss.org/magic-api/pages/extension/collection/ |
| API | Date扩展 | https://www.ssssssss.org/magic-api/pages/extension/date/ |
| API | Class扩展 | https://www.ssssssss.org/magic-api/pages/extension/class/ |
| API | Pattern扩展 | https://www.ssssssss.org/magic-api/pages/extension/pattern/ |
| 其它 | 交流群 | https://www.ssssssss.org/magic-api/pages/group/ |
| 其它 | 常见问题 | https://www.ssssssss.org/magic-api/pages/faq/ |
| 其它 | 更新日志 v0.x | https://www.ssssssss.org/magic-api/pages/changelog/v0/ |
| 其它 | 更新日志 v1.x | https://www.ssssssss.org/magic-api/pages/changelog/v1/ |
| 其它 | 更新日志 v2.x | https://www.ssssssss.org/magic-api/pages/changelog/v2/ |
| 其它 | 赞助支持 | https://www.ssssssss.org/magic-api/pages/sponsor/ |
