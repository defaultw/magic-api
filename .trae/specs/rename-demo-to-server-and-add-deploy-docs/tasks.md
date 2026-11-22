# Tasks

- [x] Task 1: 工程改名 magic-api-demo → magic-api-server
  - [x] 1.1 停止运行中的 demo 实例（如有），清理 `magic-api-demo/target/` 旧产物
  - [x] 1.2 目录改名：`magic-api-demo/` → `magic-api-server/`（源码/resources/.gitignore/magic-api-files 初始数据随目录迁移）
  - [x] 1.3 父 pom.xml：`<module>magic-api-demo</module>` → `<module>magic-api-server</module>`
  - [x] 1.4 子 pom.xml：artifactId/name 改为 magic-api-server（DemoApplication 类名与包名不动）
  - [x] 1.5 application.yml 头部注释：目录名、`mvn -pl magic-api-server spring-boot:run`、jar 名 magic-api-server-2.2.2.jar 同步更新（仅注释，配置值不动）
- [x] Task 2: 编写 DEPLOY.md 部署文档（magic-api-server/DEPLOY.md）
  - [x] 2.1 「数据存储在哪」章节：file 存储相对路径语义（工作目录/magic-api-files）、开发机现状、Ubuntu 部署后位置（WorkingDirectory 或绝对路径覆盖）、classpath: 只读警告
  - [x] 2.2 「备份」章节：tar 打包数据目录、UI/MCP 全量导出 zip、可选 magic-api.backup（需数据库）三种方式 + 备份脚本与 systemd timer 示例
  - [x] 2.3 「systemd 部署」章节：完整 magic-api-server.service unit（Java 8 路径、WorkingDirectory、ExecStart、Restart=always、内存 JVM 参数、日志输出）、分步部署命令、防火墙端口、验证命令
- [x] Task 3: 构建与启动验证
  - [x] 3.1 `mvn -pl magic-api-server -am clean package -DskipTests` BUILD SUCCESS，产物 magic-api-server-2.2.2.jar
  - [x] 3.2 后台启动新 jar（cwd=根目录加载真实数据），9999 端口 `/magic/web` 200、noticeMsg 接口可访问（requestId 正常生成，转发上游域名本机不可解析属预期），验证后停止
- [x] Task 4: 收尾
  - [x] 4.1 全库 Grep 检查 magic-api-demo 残留引用（仅历史 spec 目录，代码/配置无残留）
  - [x] 4.2 勾选 tasks.md / checklist.md 全部完成项

# Task Dependencies
- Task 1 → Task 3（改名完成才能构建验证）；Task 2 独立可并行；Task 4 依赖 Task 1/2/3 全部完成
- 注意：Task 1.1 停服前确认 9999 进程无用户正在编辑；数据目录在根 `magic-api-files\`，不在改名目录内，迁移不影响数据
