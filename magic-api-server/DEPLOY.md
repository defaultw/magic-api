# magic-api-server 部署文档（Ubuntu / systemd）

本文档回答三个问题：**数据存在哪里**、**如何备份**、**如何在 Ubuntu 上用 systemd 部署**。

---

## 一、数据存储在哪

### 存储机制

`application.yml` 中的关键配置：

```yaml
magic-api:
  resource:
    type: file
    location: ./magic-api-files/   # 相对路径
```

`location` 是**相对路径**，实际目录 = **JVM 启动时的工作目录** + `magic-api-files/`。
注意：与 jar 文件所在目录无关，只看启动命令在哪个目录下执行。

### 开发机（Windows）现状

| 位置 | 说明 |
|---|---|
| `d:\project\magic-api\magic-api-files\` | **当前实际数据目录**（9999 实例的工作目录是 `d:\project\magic-api`）。TEst 分组、接口脚本都在这里 |
| `magic-api-server\magic-api-files\` | 工程自带的初始示例数据（demo 分组），仅当在 `magic-api-server` 目录下启动时才会被使用 |

> 数据目录下包含：`api/`（分组与接口 .ms/.json 文件）、`function/`（函数）、`datasource/`（数据源配置）。每个接口是一个 JSON+脚本文件，可直接文本查看。

### Ubuntu 部署后存在哪里

取决于 systemd unit 的 `WorkingDirectory`：

- **默认（跟随 WorkingDirectory）**：数据存于 `<WorkingDirectory>/magic-api-files/`。本方案 unit 文件设置 `WorkingDirectory=/opt/magic-api`，即数据在 `/opt/magic-api/magic-api-files/`
- **推荐（绝对路径覆盖）**：启动参数显式指定，与工作目录彻底解耦：

  ```bash
  java -jar magic-api-server-2.2.2.jar --magic-api.resource.location=/var/lib/magic-api
  ```

  本方案的 unit 文件采用此方式，数据固定在 `/var/lib/magic-api`（FHS 规范的可变数据目录）

> ⚠️ **警告**：`location` 若以 `classpath:` 开头（如 `classpath:magic-api`），数据将打包进 jar 内且为**只读模式**——只能启动时加载、编辑器无法保存。生产环境不要使用。
>
> ⚠️ **修改存储位置后首次启动**：新目录为空，编辑器里看不到旧接口。迁移数据 = 停服 → 把旧 `magic-api-files/` 内容复制到新位置 → 重启。

### 切换为数据库存储（可选）

多实例/集群场景建议改用数据库存储（表 `magic_api_file_v2`），备份随之变为数据库备份：

```yaml
magic-api:
  resource:
    type: database
    table-name: magic_api_file_v2
    prefix: /magic-api
```

单机文件存储足够时无需切换。

---

## 二、如何备份

### 方式一：tar 打包数据目录（推荐，最简单）

文件存储模式下，数据就是普通文件，直接打包即可。**备份前建议先停服**（或接受极小概率的写入竞态）：

```bash
# 手动备份：产出 /opt/backup/magic-api-20260101.tar.gz
BACKUP_DIR=/opt/backup
DATA_DIR=/var/lib/magic-api        # 与 unit 中 --magic-api.resource.location 一致
mkdir -p $BACKUP_DIR
tar -czf $BACKUP_DIR/magic-api-$(date +%Y%m%d-%H%M%S).tar.gz -C $DATA_DIR .
# 恢复：停服 → 清空/替换数据目录 → tar -xzf xxx.tar.gz -C $DATA_DIR → 启动
```

自动备份脚本 `backup-magic-api.sh`（放 `/opt/magic-api/backup-magic-api.sh`）：

```bash
#!/bin/bash
# magic-api 数据备份：保留最近 14 份
set -e
BACKUP_DIR=/opt/backup
DATA_DIR=/var/lib/magic-api
KEEP=14
mkdir -p "$BACKUP_DIR"
tar -czf "$BACKUP_DIR/magic-api-$(date +%Y%m%d-%H%M%S).tar.gz" -C "$DATA_DIR" .
# 清理过期备份
ls -1t "$BACKUP_DIR"/magic-api-*.tar.gz | tail -n +$((KEEP + 1)) | xargs -r rm -f
```

配合 systemd timer 每日 02:30 备份（见下方两个 unit）：

`/etc/systemd/system/magic-api-backup.service`：

```ini
[Unit]
Description=Backup magic-api data directory

[Service]
Type=oneshot
ExecStart=/opt/magic-api/backup-magic-api.sh
```

`/etc/systemd/system/magic-api-backup.timer`：

```ini
[Unit]
Description=Daily backup of magic-api data

[Timer]
OnCalendar=*-*-* 02:30:00
Persistent=true

[Install]
WantedBy=timers.target
```

启用：`sudo systemctl enable --now magic-api-backup.timer`

### 方式二：编辑器 / MCP 全量导出 zip

- **UI 导出**：编辑器右上角菜单 →「导出」→ 全量导出 zip；恢复时「上传」该 zip（全量上传覆盖，增量上传只新增不删除）
- **MCP 导出**：调用 MCP 工具 `magic_api_export`（返回 zip 的 base64）与 `magic_api_upload`（上传恢复）
- 适合跨环境迁移（Windows 开发机 → Ubuntu 服务器）与临时快照

### 方式三：启用内置 backup 表（可选，需数据库）

magic-api 内置接口修改历史备份，**仅支持存数据库表**（demo 未配数据源，默认关闭）。启用方式：

```yaml
magic-api:
  backup:
    enable: true        # 默认 false
    max-history: -1     # 保留天数，-1 永久
    table-name: magic_api_backup
```

> 它是「编辑历史」级别的细粒度备份，不能替代方式一/二的整站备份。

---

## 三、systemd 部署方案

### 前置条件

- Ubuntu 20.04+，已安装 Java 8+（`java -version` 验证；magic-api 2.2.2 兼容 Java 8~11，Spring Boot 2.4.5）
- 已构建产物：`magic-api-server/target/magic-api-server-2.2.2.jar`（Windows 开发机执行 `mvn -pl magic-api-server -am clean package -DskipTests`）

### 第 1 步：上传 jar 并建立目录

```bash
# 本机执行：上传 jar（示例，换成你的服务器地址）
scp magic-api-server/target/magic-api-server-2.2.2.jar user@server:/tmp/

# 服务器执行：
sudo mkdir -p /opt/magic-api            # 应用目录（jar、脚本）
sudo mkdir -p /var/lib/magic-api        # 数据目录（接口脚本存储）
sudo mv /tmp/magic-api-server-2.2.2.jar /opt/magic-api/
# （可选）迁移已有数据：把旧 magic-api-files/ 内容复制到 /var/lib/magic-api/
sudo useradd -r -s /usr/sbin/nologin magic 2>/dev/null || true   # 专用运行账号
sudo chown -R magic:magic /opt/magic-api /var/lib/magic-api
```

### 第 2 步：创建备份脚本

按「二、方式一」创建 `/opt/magic-api/backup-magic-api.sh` 并赋权：

```bash
sudo chmod +x /opt/magic-api/backup-magic-api.sh
sudo chown magic:magic /opt/magic-api/backup-magic-api.sh
```

### 第 3 步：创建 systemd 服务

`/etc/systemd/system/magic-api-server.service`（完整内容，直接复制）：

```ini
[Unit]
Description=Magic API Server (magic-api 2.2.2 + OIDC + MCP)
After=network.target

[Service]
Type=simple
User=magic
Group=magic
# 数据目录用绝对路径覆盖，与工作目录解耦；内存按服务器规格调整 -Xms/-Xmx
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/magic-api/magic-api-server-2.2.2.jar --magic-api.resource.location=/var/lib/magic-api
WorkingDirectory=/opt/magic-api
Restart=always
RestartSec=10
# 进程被 kill -9 时也重启
SuccessExitStatus=143
# 日志走 journald：journalctl -u magic-api-server -f
StandardOutput=journal
StandardError=journal
# 基础安全加固（可选，按需放开）
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

> 端口、OIDC 等配置已内嵌在 jar 的 application.yml 中；如需覆盖，在 `ExecStart` 追加参数，如 `--server.port=8080`、`--spring.config.location=...`。

### 第 4 步：启动与自启

```bash
sudo systemctl daemon-reload
sudo systemctl enable magic-api-server      # 开机自启
sudo systemctl start magic-api-server       # 启动
```

### 第 5 步：验证

```bash
sudo systemctl status magic-api-server               # active (running)
journalctl -u magic-api-server -f                    # 看启动日志（约 15~25s 就绪）
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9999/magic/web   # 200
ls /var/lib/magic-api                                # 数据目录出现 api/ 等子目录
```

浏览器访问 `http://<服务器IP>:9999/magic/web/index.html`，登录后确认接口列表完整。

### 第 6 步：防火墙

```bash
sudo ufw allow 9999/tcp        # 或仅对内网放行：sudo ufw allow from 10.0.0.0/8 to any port 9999
```

### 日常运维速查

| 操作 | 命令 |
|---|---|
| 查看日志 | `journalctl -u magic-api-server -f`（`-n 200` 看最近 200 行） |
| 重启 | `sudo systemctl restart magic-api-server` |
| 停止 | `sudo systemctl stop magic-api-server` |
| 升级 jar | 停服 → 覆盖 `/opt/magic-api/magic-api-server-2.2.2.jar` → 起服（数据目录不动即无缝升级） |
| 手动备份 | `/opt/magic-api/backup-magic-api.sh`，产物在 `/opt/backup/` |
| 备份恢复 | 停服 → `rm -rf /var/lib/magic-api/*` → `tar -xzf 备份包 -C /var/lib/magic-api` → `chown -R magic:magic` → 起服 |
| 检查备份定时器 | `systemctl list-timers | grep magic-api-backup` |

### OIDC 回调注意事项

服务器部署后访问地址从 `127.0.0.1:9999` 变为 `服务器IP:9999`（或域名），TOPIAM 应用的「登录 Redirect URI 白名单」需同步增加对应条目，例如：

```
http://<服务器IP>:9999/magic/web/oauth2/callback
```

并在 application.yml（或启动参数 `--magic-api.security.oauth2.redirect-uri=...`）中同步修改，二者必须逐字符一致。
