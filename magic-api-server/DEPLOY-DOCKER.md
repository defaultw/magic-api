# magic-api-server Docker 部署操作手册（Docker Compose）

一句话简介：本手册给出 magic-api-server 基于 Docker / Docker Compose 的完整部署流程——镜像构建、容器运行、数据持久化、凭据注入、登录认证与 OIDC 回调（可选）、备份恢复与日常运维，所有命令可直接复制执行。

> **与 [DEPLOY.md](DEPLOY.md) 的关系**：[DEPLOY.md](DEPLOY.md) 是 Ubuntu + systemd 的传统部署路线；本文档是 **Docker 路线**。两条路线的数据文件格式与凭据环境变量完全一致，数据可在两者间互相迁移（见第六节）。

---

## 一、前提条件

| 项目 | 要求 | 验证命令 |
|---|---|---|
| Docker | Docker Engine 24+（Linux）或 Docker Desktop（Windows/macOS） | `docker version` |
| Compose | Compose v2.24+（`env_file` 的 `required` 语法需要） | `docker compose version` |
| 网络 | 首次构建需联网拉取基础镜像与 Maven/npm 依赖（约数百 MB） | — |
| 磁盘 | 可用空间建议 ≥ 2 GB（镜像 + 依赖缓存 + 数据目录） | — |
| 源码 | 本仓库源码（构建上下文 = 仓库根目录），如 `d:\project\magic-api` | `git status` |

> Windows PowerShell 中请使用 `curl.exe`（`curl` 是 `Invoke-WebRequest` 的别名，参数不兼容）。

---

## 二、快速开始

```bash
cd magic-api-server
docker compose up -d --build          # 构建镜像并启动（首次构建下载数百 MB，耗时数分钟）
docker compose logs -f                # 观察启动日志（Ctrl+C 退出跟踪）
```

应用启动约需 15~25s，healthy 之前 `docker compose ps` 显示 starting 属正常。启动完成后验证：

```bash
# Linux / Git Bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9999/magic/web/index.html   # 期望 200
```

```powershell
# Windows PowerShell
curl.exe -s -o NUL -w "%{http_code}" http://127.0.0.1:9999/magic/web/index.html       # 期望 200
```

浏览器访问编辑器：`http://127.0.0.1:9999/magic/web/index.html`。

> 默认以账号密码登录（admin/123456）开箱即用；生产环境务必用环境变量覆盖默认密码，OIDC 统一认证默认关闭、按需启用，见第七节。

---

## 三、从 Docker Hub 拉取镜像（推荐）

不想本地构建时，可直接拉取 CI 自动发布的官方镜像（发布机制见第十四节），跳过本地 Maven/npm 构建。镜像命名空间即 Docker Hub 用户名（下文以 `<username>` 占位，替换为你的 `DOCKERHUB_USERNAME`，当前为 `wangxiaojiang10088`）：

```bash
docker pull wangxiaojiang10088/magic-api-server:2.2.2
```

**tag 规则：**

| tag | 说明 |
|---|---|
| `<版本>`（如 `2.2.2`） | 版本镜像，与同名 GitHub Release 一一对应 |
| `latest` | 始终指向最新一次发布（手动触发且 `push_latest=false` 时不更新） |

**运行示例**（与 compose 等价，在 magic-api-server 目录执行）：

```bash
# bash
docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v "$(pwd)/magic-api-data:/data/magic-api" \
  --env-file magic-api.env \
  -e TZ=Asia/Shanghai \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --restart unless-stopped \
  wangxiaojiang10088/magic-api-server:2.2.2
```

```powershell
# PowerShell（在 magic-api-server 目录）
docker run -d --name magic-api-server `
  -p 9999:9999 `
  -v "${PWD}\magic-api-data:/data/magic-api" `
  --env-file magic-api.env `
  -e TZ=Asia/Shanghai `
  -e JAVA_OPTS="-Xms256m -Xmx512m" `
  --restart unless-stopped `
  wangxiaojiang10088/magic-api-server:2.2.2
```

> `--env-file` 要求文件必须存在；暂无凭据时直接省略该参数。

**两种方式怎么选**：服务器能访问外网、想跳过本地 Maven/npm 构建选拉取；内网/离线环境或需定制 Dockerfile 选本地构建（见第四节）。

> 拉取镜像与本地构建产出的镜像完全等价：数据持久化目录映射与凭据注入方式不变，详见第六节、第七节。

---

## 四、镜像构建详解

### 4.1 两种构建方式

**方式 A：docker compose（推荐，在 magic-api-server 目录执行）**

```bash
cd magic-api-server
docker compose build
```

**方式 B：纯 docker build（必须在仓库根目录执行）**

```bash
# bash
cd /path/to/magic-api
docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .
```

```powershell
# PowerShell
cd d:\project\magic-api
docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .
```

**为什么构建上下文必须是仓库根目录？** 这是多模块 Maven 工程，镜像内的构建命令是 `mvn -B -ntp -pl magic-api-server -am package -DskipTests`，`-pl ... -am` 会连带构建 magic-api-server 依赖的其它模块（如 magic-api-plugin-task），需要读取根 pom.xml 与各模块源码。上下文虽是仓库根，但无关内容由 `magic-api-server/Dockerfile.dockerignore` 剔除（排除 `**/target`、`**/node_modules`、`**/magic-api-files`、`**/magic-api-data`、`**/magic-api.env`、`**/application-local.yml`、`.git`、`.trae`、`.idea`、`**/*.iml`、`out`、`bin`），因此本地开发数据 `d:\project\magic-api\magic-api-files` 与凭据文件都不会进入镜像。

### 4.2 为何构建阶段需要 Node

`magic-api-plugin-task` 模块的 pom 在构建期强制执行 npm 安装与前端插件构建，所以阶段一在 `maven:3.9-eclipse-temurin-17` 基础上额外安装 Node v16.20.2（从 npmmirror 下载）。

### 4.3 镜像结构（两阶段构建）

- **阶段一（构建）**：`maven:3.9-eclipse-temurin-17` + Node v16.20.2，执行 `mvn -B -ntp -pl magic-api-server -am package -DskipTests`
  - 为何用 JDK 17 构建：依赖闭包中 `magic-api-servlet-jakarta` 模块的 pom 强制 Java 17 编译（运行时 Spring Boot 2.x 不会加载其类），而各业务模块经 `maven.compiler.source/target=1.8` 仍编译为 Java 8 字节码
- **阶段二（运行）**：`eclipse-temurin:8-jre-jammy`
  - 非 root 用户 `magic`（uid/gid 10001），镜像内置 curl（HEALTHCHECK 可用）
  - jar 位于 `/app/app.jar`，工作目录 `/app`，`EXPOSE 9999`
  - `HEALTHCHECK`：`curl http://127.0.0.1:9999/magic/web/index.html`（interval=30s timeout=5s start-period=60s retries=5）
  - `ENTRYPOINT` 固定注入 `--magic-api.resource.location=${MAGIC_API_RESOURCE_LOCATION:-/data/magic-api}`（数据目录默认 `/data/magic-api`，可用环境变量覆盖）
  - `ENV` 默认：`MAGIC_API_RESOURCE_LOCATION=/data/magic-api`、`TZ=Asia/Shanghai`、`JAVA_OPTS="-Xms256m -Xmx512m"`

### 4.4 国内加速 build-arg

| build-arg | 默认值 | 说明 |
|---|---|---|
| `MAVEN_MIRROR_URL` | `https://maven.aliyun.com/repository/public` | Maven 仓库源 |
| `NPM_REGISTRY` | `https://registry.npmmirror.com` | npm registry |
| `NODE_DIST_BASE_URL` | npmmirror 的 Node 发行版镜像 | 海外环境可换官方 `https://nodejs.org/dist` |

传参写法：

```bash
# compose 方式（在 magic-api-server 目录）
docker compose build \
  --build-arg MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public \
  --build-arg NPM_REGISTRY=https://registry.npmmirror.com

# docker build 方式（在仓库根目录）
docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 \
  --build-arg NODE_DIST_BASE_URL=https://nodejs.org/dist .
```

---

## 五、运行详解

### 5.1 compose 运行（推荐）

```bash
cd magic-api-server
docker compose up -d
```

> **为什么必须在 magic-api-server 目录执行？** compose 文件中的相对路径绑定（数据目录 `magic-api-data`、`env_file`）都以 compose 文件所在目录为基准，cd 进去执行最不容易出错。

### 5.2 等价 docker run 命令

```bash
# bash（在 magic-api-server 目录）
docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v "$(pwd)/magic-api-data:/data/magic-api" \
  --env-file magic-api.env \
  -e TZ=Asia/Shanghai \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --restart unless-stopped \
  magic-api-server:2.2.2
```

```powershell
# PowerShell（在 magic-api-server 目录）
docker run -d --name magic-api-server `
  -p 9999:9999 `
  -v "${PWD}\magic-api-data:/data/magic-api" `
  --env-file magic-api.env `
  -e TZ=Asia/Shanghai `
  -e JAVA_OPTS="-Xms256m -Xmx512m" `
  --restart unless-stopped `
  magic-api-server:2.2.2
```

> `--env-file` 要求文件必须存在；暂无凭据时直接省略该参数（compose 的 `env_file` 用 `required: false` 声明，缺失不阻断启动）。

### 5.3 端口 / 时区 / 内存调整

| 项目 | 调整方法 |
|---|---|
| 端口 | 改 compose `ports` 左侧宿主端口，如 `"19999:9999"`（容器内固定 9999）；改对外端口后需同步 OIDC 白名单（见第九节） |
| 时区 | 环境变量 `TZ`，镜像默认 `Asia/Shanghai`，如 `-e TZ=Asia/Shanghai` |
| JVM 内存 | 环境变量 `JAVA_OPTS`，镜像默认 `-Xms256m -Xmx512m`，如 `-e JAVA_OPTS="-Xms512m -Xmx1g"`，也可写入 magic-api.env |

---

## 六、持久化目录与数据迁移

### 6.1 目录映射

| 宿主机（magic-api-server/ 下） | 容器内 | 内容 |
|---|---|---|
| `magic-api-data/` | `/data/magic-api` | bind mount 根（路径相对 compose 文件目录） |
| `magic-api-data/api/` | `/data/magic-api/api/` | 分组与接口（.ms / .json 文件） |
| `magic-api-data/function/` | `/data/magic-api/function/` | 函数 |
| `magic-api-data/datasource/` | `/data/magic-api/datasource/` | 数据源配置 |

首次启动应用会自动创建 `api/`、`function/`、`datasource/` 子目录。

### 6.2 迁移来源 A：Windows 开发机数据（d:\project\magic-api\magic-api-files）

```powershell
cd d:\project\magic-api\magic-api-server
docker compose stop                    # 先停容器
Copy-Item -Path d:\project\magic-api\magic-api-files\* -Destination .\magic-api-data\ -Recurse -Force
docker compose start
```

### 6.3 迁移来源 B：Linux systemd 部署的数据（/var/lib/magic-api，见 DEPLOY.md）

```bash
cd magic-api-server
docker compose stop                    # 先停容器
cp -a /var/lib/magic-api/. magic-api-data/
sudo chown -R 10001:10001 magic-api-data   # Linux 原生宿主机必须
docker compose start
```

### 6.4 迁移来源 C：编辑器全量导出 zip（跨环境通用）

源环境编辑器右上角菜单「导出」→ 全量导出 zip；目标环境编辑器「上传」该 zip（全量上传覆盖、增量上传只增不删）。适合 Windows 开发机 ↔ 服务器任意方向迁移。

### 6.5 bind mount 权限说明

- **Docker Desktop（Windows/macOS）**：bind mount 走文件共享机制，权限无需处理。
- **Linux 原生宿主机**：容器以 uid/gid 10001（magic）运行，目录属主不对会写失败，需 `sudo chown -R 10001:10001 magic-api-data`。

---

## 七、凭据注入

jar 内 application.yml 的全部认证项均为环境变量占位符。默认账号密码登录 admin/123456 开箱即用（公开默认凭据，生产必须覆盖）；OIDC 统一认证默认关闭；MCP token 默认空（不鉴权）。

### 7.1 法一：magic-api.env（推荐）

```bash
cd magic-api-server
cp magic-api.env.example magic-api.env        # PowerShell: Copy-Item magic-api.env.example magic-api.env
# 编辑 magic-api.env，填入真值
docker compose up -d                           # 重新创建容器使新变量生效
```

| 变量 | 用途 | 默认值 |
|---|---|---|
| `MAGIC_API_SECURITY_USERNAME` | 编辑器登录用户名 | `admin`（公开默认，生产必须覆盖） |
| `MAGIC_API_SECURITY_PASSWORD` | 编辑器登录密码 | `123456`（公开默认，生产必须覆盖） |
| `MAGIC_API_OAUTH2_ENABLED` | 统一认证（OIDC/SSO）开关 | `false` |
| `MAGIC_API_OAUTH2_CLIENT_ID` | OIDC 应用 client-id（启用 OIDC 时必填） | 空 |
| `MAGIC_API_OAUTH2_CLIENT_SECRET` | OIDC 应用 client-secret（启用 OIDC 时必填） | 空 |
| `MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED` | 密码登录开关（OIDC 启用时仍可并存） | `true` |
| `MAGIC_API_OAUTH2_AUTO_REDIRECT` | 打开编辑器自动跳转 OIDC 授权页 | `false` |
| `MAGIC_API_MCP_TOKEN` | MCP 端点鉴权 token | 空（不鉴权） |

- compose 的 `env_file` 为 `required: false`：`magic-api.env` 缺失不阻断启动。
- `magic-api.env` 已被 .gitignore 排除，切勿提交入库。
- MCP token 用 `openssl rand -hex 32` 生成（Windows 可在 Git Bash 中执行）。配置后所有 `/mcp/magic` 请求必须携带 `X-Mcp-Token` 或 `Authorization: Bearer`，否则返回 401：

```bash
curl -i http://127.0.0.1:9999/mcp/magic -H "X-Mcp-Token: <MAGIC_API_MCP_TOKEN 的值>"
```

### 7.2 法二：挂载 application-local.yml

对应 jar 内 `spring.config.import: optional:file:./application-local.yml`（容器工作目录 `/app`）。compose 文件 volumes 中已预留注释行，文件就绪后放开即可：

```yaml
    volumes:
      - ./magic-api-data:/data/magic-api
      - ./application-local.yml:/app/application-local.yml:ro
```

本地准备：`cp application-local.yml.example application-local.yml` 后填值（模板含 username/password/client-id/client-secret/mcp token 五项）。

> ⚠ **宿主机文件不存在时不能放开该行**——Docker 会把不存在的宿主机路径按目录创建出同名空目录，挂载即错。

### 7.3 法三（补充）：改密 / 关闭密码登录（环境变量，推荐）

改密 = 覆盖 `MAGIC_API_SECURITY_USERNAME` / `MAGIC_API_SECURITY_PASSWORD`。在 `magic-api.env` 中更新两行后 `docker compose up -d` 重建容器；`docker run` 等价方式：

```bash
docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v "$(pwd)/magic-api-data:/data/magic-api" \
  -e MAGIC_API_SECURITY_USERNAME=admin \
  -e MAGIC_API_SECURITY_PASSWORD='<强密码>' \
  --restart unless-stopped \
  magic-api-server:2.2.2    # 其余参数（env-file、TZ、JAVA_OPTS 等）见 5.2
```

仅 OIDC 模式（关闭密码登录，需先配好 `MAGIC_API_OAUTH2_CLIENT_ID` / `MAGIC_API_OAUTH2_CLIENT_SECRET`）：

```bash
-e MAGIC_API_OAUTH2_ENABLED=true -e MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED=false
```

**高级：挂载配置覆盖任意键**

`issuer`、`redirect-uri` 等键没有显式环境变量占位符，可用 Spring Boot 外置配置覆盖：挂载 `/app/config/application.yml:ro`（工作目录 `/app`，`config/` 子目录优先级高于 jar 内配置），**只写想覆盖的键**：

```yaml
magic-api:
  security:
    oauth2:
      issuer: https://<oidc-server>
      redirect-uri: http://<host>:9999/magic/web/oauth2/callback   # 与 TOPIAM 白名单逐字符一致
      # password-login-enabled 已有环境变量 MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED，无需在此覆盖
```

> 同样注意宿主机文件不存在时不要挂载；`ENTRYPOINT` 已强制注入 `--magic-api.resource.location`（命令行优先级最高），数据目录固定 `/data/magic-api`，勿在此文件改 `resource.location`。

---

## 八、启动验证清单

```bash
cd magic-api-server
docker compose ps                                                     # STATUS 应含 (health: healthy)
docker inspect --format "{{.State.Health.Status}}" $(docker compose ps -q)   # 期望 healthy
ls magic-api-data                                                     # 应出现 api/ function/ datasource/
```

HTTP 验证（期望 200）：

```bash
# Linux / Git Bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9999/magic/web/index.html
```

```powershell
# Windows PowerShell
curl.exe -s -o NUL -w "%{http_code}" http://127.0.0.1:9999/magic/web/index.html
```

> 应用启动需 15~25s（首次启动还要建数据子目录），healthy 前 `starting` 属正常；超 2 分钟再排查日志。

---

## 九、OIDC 回调白名单

OIDC 统一认证为**可选功能**，默认关闭；启用方式见第七节 `MAGIC_API_OAUTH2_ENABLED=true`。

内置默认 `redirect-uri: http://127.0.0.1:9999/magic/web/oauth2/callback`，**仅适用于本机 127.0.0.1 访问编辑器**。改用 IP / 域名访问时：

1. **TOPIAM 应用白名单**：「登录 Redirect URI 白名单」添加 `http://<host>:9999/magic/web/oauth2/callback`（`<host>` 为服务器 IP 或域名）。
2. **应用侧 redirect-uri 覆盖**，与白名单条目**逐字符一致**，两种方式任选：
   - 环境变量：`magic-api.env` 追加一行，然后 `docker compose up -d` 重建容器：

     ```ini
     MAGIC_API_SECURITY_OAUTH2_REDIRECTURI=http://<host>:9999/magic/web/oauth2/callback
     ```

   - 挂载 config/application.yml（见 7.3）写 `redirect-uri`：

     ```yaml
     magic-api:
       security:
         oauth2:
           redirect-uri: http://<host>:9999/magic/web/oauth2/callback
     ```

---

## 十、备份与恢复

### 10.1 手动备份（tar 打包 magic-api-data，先停容器）

```bash
# bash
cd magic-api-server
docker compose stop
tar -czf /tmp/magic-api-data-$(date +%Y%m%d-%H%M%S).tar.gz magic-api-data
docker compose start
```

```powershell
# PowerShell
cd d:\project\magic-api\magic-api-server
docker compose stop
tar -czf "$env:TEMP\magic-api-data-$(Get-Date -Format yyyyMMdd-HHmmss).tar.gz" magic-api-data
docker compose start
```

### 10.2 恢复

```bash
# bash
cd magic-api-server
docker compose stop
rm -rf magic-api-data/*                                  # 或整目录替换
tar -xzf /tmp/magic-api-data-<时间戳>.tar.gz -C magic-api-data
sudo chown -R 10001:10001 magic-api-data                 # Linux 原生宿主机必须
docker compose start
```

```powershell
# PowerShell
cd d:\project\magic-api\magic-api-server
docker compose stop
Remove-Item .\magic-api-data\* -Recurse -Force
tar -xzf <备份包路径> -C .\magic-api-data
docker compose start
```

### 10.3 cron 每日自动备份（Linux）

先 `mkdir -p /opt/backup`，然后 `crontab -e` 追加一行（crontab 中 `%` 必须写成 `\%`）：

```cron
30 2 * * * cd /opt/magic-api-server && docker compose stop && tar -czf /opt/backup/magic-api-data-$(date +\%Y\%m\%d).tar.gz magic-api-data && docker compose start
```

### 10.4 编辑器 zip 导出 / 导入

编辑器右上角菜单「导出」全量 zip /「上传」恢复（全量上传覆盖、增量上传只增不删），跨环境通用，详见 [DEPLOY.md](DEPLOY.md)「方式二」。

---

## 十一、版本升级

```bash
cd magic-api-server
git pull
docker compose build
docker compose up -d
```

数据目录在宿主机 `magic-api-data`，不随容器销毁——数据目录不动即无缝升级。升级完可顺手清理旧悬空镜像：`docker image prune`。

---

## 十二、日常运维速查表

（以下命令均在 magic-api-server 目录执行）

| 操作 | 命令 |
|---|---|
| 看日志 | `docker compose logs -f`（`-n 200` 看最近 200 行） |
| 重启 | `docker compose restart` |
| 停止并删容器 | `docker compose down`（数据在宿主机，不丢） |
| 进入容器 | `docker compose exec magic-api-server sh`（运行时镜像无 bash，用 sh；服务名以 `docker compose ps` 为准） |
| 查看实时数据 | `ls magic-api-data`（宿主机直接看，无需进容器） |
| 手动备份 | 见第十节 |
| 清理悬空镜像 | `docker image prune` |

---

## 十三、常见问题 FAQ

**1. 端口 9999 被占用**
- 现象：`docker compose up` 报 `port is already allocated` / `address already in use`。
- 原因：宿主机 9999 被其它进程占用（如同机还跑着 systemd 版 magic-api）。
- 解决：compose `ports` 改为 `"19999:9999"`，用 `http://127.0.0.1:19999/magic/web/index.html` 访问，并同步 TOPIAM 白名单与 redirect-uri（`http://<host>:19999/magic/web/oauth2/callback`，见第九节）。

**2. Linux 上容器写数据目录 Permission denied**
- 现象：日志或编辑器保存时报写文件失败 / Permission denied。
- 原因：容器以 uid/gid 10001（magic）运行，Linux 原生 bind mount 校验宿主机目录权限。
- 解决：`sudo chown -R 10001:10001 magic-api-data` 后 `docker compose restart`。Docker Desktop（Windows/macOS）无需处理。

**3. 构建时 Maven/npm 拉取失败**
- 现象：阶段一 BUILD FAILED，依赖下载超时。
- 原因：默认仓库源当前网络不可达（海外/内网限制）。
- 解决：按 3.4 用 `--build-arg` 切换 `MAVEN_MIRROR_URL` / `NPM_REGISTRY` / `NODE_DIST_BASE_URL`（如官方 `https://nodejs.org/dist`）。

**4. 长时间 starting 不 healthy**
- 现象：`docker compose ps` 长期显示 `(health: starting)`。
- 原因：应用启动需 15~25s；首次启动还要创建 api/ function/ datasource/ 目录，healthcheck 的 start-period=60s，starting 属正常。
- 解决：超过 2 分钟仍不 healthy，再查 `docker compose logs`。

**5. 容器时区不对**
- 现象：日志/业务时间与本地相差 8 小时。
- 原因：`TZ` 未按预期注入。
- 解决：compose `environment` 或 `docker run -e TZ=Asia/Shanghai` 指定时区（镜像默认已设 Asia/Shanghai）。

**6. 内存不足 OOM**
- 现象：容器无故退出，日志见 `OutOfMemoryError`，或被宿主 OOM killer 杀死。
- 原因：默认 `JAVA_OPTS="-Xms256m -Xmx512m"` 对当前负载偏小。
- 解决：调大堆，如 `-e JAVA_OPTS="-Xms512m -Xmx1g"` 后重启。

**7. 修改了 application.yml 不生效**
- 现象：改配置并重启后行为未变。
- 原因：jar 内 application.yml 只是兜底默认值，改它需重建镜像才进容器；运行时容器实际读取的是环境变量与外置挂载配置。
- 解决：按第七节用 `magic-api.env` 或挂载 `config/application.yml` 覆盖，然后 `docker compose up -d` 重建容器。

---

## 十四、CI 自动发布镜像（GitHub Actions）

仓库内置 workflow [docker-release.yml](../.github/workflows/docker-release.yml)，自动构建并推送镜像到 Docker Hub。镜像命名空间动态取自 Secret `DOCKERHUB_USERNAME`（当前为 `wangxiaojiang10088`，换账号只需更新该 Secret，无需改 workflow），镜像名即 `<DOCKERHUB_USERNAME>/magic-api-server`。

### 14.1 两种触发方式

| 方式 | 触发条件 | 推送 tag |
|---|---|---|
| 自动 | 发布 GitHub Release（tag `v2.2.2` 或 `2.2.2` 均可） | `<版本>`（自动去 `v` 前缀）+ `latest` |
| 手动 | Actions 页 → **docker-release** → **Run workflow** | 填 `version`（如 `2.2.2`）；`push_latest` 勾选框控制是否更新 latest（默认更新） |

手动触发路径：仓库 **Actions** 标签页 → 左侧选 **docker-release** → **Run workflow** → 填写 version / 勾选 push_latest → 运行。

### 14.2 一次性配置：Docker Hub Secrets

workflow 推送前需先在仓库配置凭据（只配一次）：

1. 生成 Docker Hub Access Token：Docker Hub → **Account Settings** → **Security** → **New Access Token**，权限必须选 **Read & Write**（默认 Read-only 会报 `insufficient_scope: authorization failed`）。
2. 写入仓库 Secrets：GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**，添加两条：
   - `DOCKERHUB_USERNAME`：Docker Hub 用户名（如 `wangxiaojiang10088`，不能用邮箱；同时决定镜像命名空间）
   - `DOCKERHUB_TOKEN`：上一步生成的 Access Token

未配置 Secrets 时：构建步骤正常执行，登录/推送步骤失败并提示配置（构建失败不影响本地使用）。

### 14.3 发布流程与验证

```bash
# 1. 发布 release（网页操作或 CLI）后 workflow 自动触发
gh release create v2.2.3 --title "v2.2.3" --notes "..."

# 2. 在 Actions 页观察构建进度，完成后验证远端镜像
docker manifest inspect wangxiaojiang10088/magic-api-server:2.2.3
docker pull wangxiaojiang10088/magic-api-server:2.2.3
```

每次运行会在 run 页面生成 `image-digest` artifact（含镜像 digest 与推送结果，保留 30 天），排障时下载查看。

### 14.4 CI 构建 vs 本地构建

- CI 镜像（`<DOCKERHUB_USERNAME>/magic-api-server`，即 `wangxiaojiang10088/magic-api-server`）：由 GitHub Actions 构建推送，适合服务器直接 `docker pull`（见第三节），免本地 Maven/npm 环境。
- 本地构建（第四节）：适合内网/离线环境、需要修改 Dockerfile 或在推送前自验证的场景。
- 两者产出的镜像内容一致（同一 Dockerfile、同一构建上下文规则）。
