# Tasks

- [x] Task 1: 创建 magic-api-demo 子工程骨架
  - [x] SubTask 1.1: 根 pom.xml `<modules>` 追加 `<module>magic-api-demo</module>`
  - [x] SubTask 1.2: 新建 `magic-api-demo/pom.xml`：parent=magic-api-parent(2.2.2)，依赖 spring-boot-starter-web、spring-boot-starter-aop、h2(runtime)、magic-api-spring-boot-starter；build 配置 spring-boot-maven-plugin repackage（mainClass 指向 DemoApplication，版本 `${spring-boot.version}`）
  - [x] SubTask 1.3: 新建 `DemoApplication.java`：`@SpringBootApplication` 主类

- [x] Task 2: 编写 application.yml（TOPIAM 对接配置）
  - [x] SubTask 2.1: server.port=9999；`magic-api.web=/magic/web`；`magic-api.resource.type=file`、`location=./magic-api-files/`；`magic-api.prefix=` 留空
  - [x] SubTask 2.2: security.username=admin/password=123456；oauth2.enabled=true；issuer/client-id/auto-redirect/password-login-enabled 完整；client-secret 占位符标注 TODO
  - [x] SubTask 2.3: yml 顶部注释含 TOPIAM 白名单 redirect_uri 说明 `http://localhost:9999/magic/web/oauth2/callback`

- [x] Task 3: 初始化测试接口文件与打包验证
  - [x] SubTask 3.1: 初始化 `magic-api-files/api/demo/group.json` 与 `hello.ms`（标准 magic-api JSON + 脚本分隔格式）
  - [x] SubTask 3.2: `mvn -pl magic-api-demo -am package -DskipTests` BUILD SUCCESS；repackage 生效产出 `magic-api-demo-2.2.2.jar`
  - [x] SubTask 3.3: `java -jar target/magic-api-demo-2.2.2.jar` 启动成功（9999，context path 空）；`config.json` 返回 `oauth2.enabled=true, authorizePath=oauth2/authorize`；`index.html` 200 且含 OIDC 注入脚本（验证后已停进程）

# 附加修复（Task 3.2 过程中暴露）
- [x] 修复 `MagicOAuth2Controller.urlEncode` 声明 `throws UnsupportedEncodingException` 导致受检异常在 callback 方法未被捕获：内部 catch 后转 IllegalStateException；`authorize`/`callback` 方法签名不再有受检异常
