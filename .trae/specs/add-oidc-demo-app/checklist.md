# Checklist

- [x] 根 pom modules 含 magic-api-demo，reactor 构建可识别
- [x] magic-api-demo/pom.xml 依赖齐全（web/aop/h2/magic-api-spring-boot-starter），spring-boot-maven-plugin repackage 生效
- [x] DemoApplication 可被 spring-boot-maven-plugin 打成可执行 jar（repackage 日志确认 Replacing main artifact）
- [x] application.yml：9999 端口、/magic/web、file 存储 ./magic-api-files、prefix 留空、oauth2 TOPIAM 配置完整（secret 占位）
- [x] yml 顶部注释含 TOPIAM 白名单 redirect_uri 说明 `http://localhost:9999/magic/web/oauth2/callback`
- [x] magic-api-files 初始测试接口存在且格式正确（group.json + hello.ms JSON + 分隔线 + 脚本）
- [x] `mvn -pl magic-api-demo -am clean package -DskipTests` BUILD SUCCESS（reactor 8 模块全部成功，18s）
- [x] 启动 jar 后 `http://localhost:9999/magic/web/config.json` 返回 `oauth2.enabled=true, authorizePath=oauth2/authorize, prefix=""`；`/magic/web/index.html` 200 且包含 OIDC 注入脚本（验证后已停止进程）
- [x] 未改动既有 magic-api/starter/editor 任何文件（仅修复了 `MagicOAuth2Controller.urlEncode` 的受检异常声明——属于 `add-oidc-unified-auth` 的编译 bug 修复，不涉及既有业务逻辑）
