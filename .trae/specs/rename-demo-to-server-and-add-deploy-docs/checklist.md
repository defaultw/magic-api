# Checklist

- [ ] 目录/模块/artifactId/name 统一为 magic-api-server，无 magic-api-demo 残留引用（历史 spec 目录除外）
- [ ] `mvn -pl magic-api-server -am clean package -DskipTests` 成功，产物为 magic-api-server-2.2.2.jar
- [ ] 新 jar 启动后 9999 端口 /magic/web 返回 200，既有接口数据（TEst 分组）正常加载
- [ ] DemoApplication 类名与包名 org.ssssssss.magicapi.demo 保持不变（仅工程名改动）
- [ ] DEPLOY.md 数据存储章节明确：相对路径语义、开发机现状、Ubuntu 部署后位置、classpath: 只读警告
- [ ] DEPLOY.md 备份章节含三种方式（tar/导出 zip/backup 表）+ 备份脚本与 timer 示例
- [ ] DEPLOY.md systemd 章节含完整 unit 文件、分步部署命令、防火墙与验证命令，可直接照做
