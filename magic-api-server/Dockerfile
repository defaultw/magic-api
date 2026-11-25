# ==================================================================
# magic-api-server 多阶段构建
#
# 构建上下文 = 仓库根目录（需包含全部 Maven 模块），在仓库根执行：
#   docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .
#
# 构建闭包：magic-api-server -> magic-api-spring-boot-starter
#   -> magic-api-servlet-jakarta（该模块 pom 强制 Java 17 编译，
#   运行时不被 Spring Boot 2.x 加载）-> 需 JDK 17 构建；
#   magic-api-plugin-task 在 generate-resources 阶段强制执行
#   npm install && npm run build（console/dist 不入库），
#   因此构建阶段需要 JDK17 + Maven + Node 双工具链。
#   各模块经 maven.compiler.source/target=1.8 编译为 Java 8 字节码，
#   运行时仍是 JRE 8。
#
# 国内加速：默认预置阿里云 Maven 镜像与 npmmirror；如需覆盖：
#   --build-arg MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
#   --build-arg NPM_REGISTRY=https://registry.npmmirror.com
# ==================================================================

# syntax=docker/dockerfile:1

# ---------- 阶段一：源码构建（JDK17 + Maven + Node） ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
ARG NPM_REGISTRY=https://registry.npmmirror.com
ARG NODE_VERSION=v16.20.2
ARG NODE_DIST_BASE_URL=https://npmmirror.com/mirrors/node

# 安装 Node.js（plugin-task 前端构建需要）
# 注：maven:3.9-eclipse-temurin-17 已内置 curl 与 CA 证书，无需 apt-get 安装
RUN curl -fsSL "${NODE_DIST_BASE_URL}/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.gz" \
      -o /tmp/node.tar.gz \
 && tar -xzf /tmp/node.tar.gz -C /usr/local --strip-components=1 \
 && rm -f /tmp/node.tar.gz \
 && node -v && npm -v

# Maven 镜像源（settings.xml 仅覆盖 central，不影响工程内仓库配置）
RUN mkdir -p /root/.m2 \
 && printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n  <mirrors>\n    <mirror>\n      <id>mirror</id>\n      <mirrorOf>central</mirrorOf>\n      <url>%s</url>\n    </mirror>\n  </mirrors>\n</settings>\n' "${MAVEN_MIRROR_URL}" \
      > /root/.m2/settings.xml

# npm 镜像源（mvn exec 调起的 npm 子进程读取 /root/.npmrc）
RUN echo "registry=${NPM_REGISTRY}" > /root/.npmrc

WORKDIR /build
COPY . /build

# 构建 boot jar（-am 联动构建全部上游模块；plugin-task 触发 npm 构建）
RUN mvn -B -ntp -pl magic-api-server -am package -DskipTests

# ---------- 阶段二：运行时（JRE8，非 root） ----------
FROM eclipse-temurin:8-jre-jammy

ARG MAGIC_UID=10001

# 注：eclipse-temurin:8-jre-jammy 已内置 curl（HEALTHCHECK 可用），仅需创建用户与目录
RUN groupadd -g ${MAGIC_UID} magic \
 && useradd -u ${MAGIC_UID} -g magic -M -s /usr/sbin/nologin magic \
 && mkdir -p /data/magic-api /app \
 && chown -R magic:magic /data/magic-api /app

WORKDIR /app
# 工作目录 /app 与 application.yml 的 spring.config.import
# "optional:file:./application-local.yml" 对应：需要本地凭据文件时挂载到
# /app/application-local.yml；结构化配置覆盖可挂载 /app/config/application.yml
COPY --from=builder /build/magic-api-server/target/magic-api-server-*.jar /app/app.jar

ENV MAGIC_API_RESOURCE_LOCATION=/data/magic-api \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

USER magic

EXPOSE 9999

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS -o /dev/null http://127.0.0.1:9999/magic/web/index.html || exit 1

# 数据目录经环境变量注入（可被运行时 -e MAGIC_API_RESOURCE_LOCATION=... 覆盖）。
# 调试执行任意命令需显式覆盖入口：
#   docker run --rm --entrypoint sh magic-api-server:2.2.2 -c "java -version"
# 或追加 Spring Boot 参数：docker run magic-api-server:2.2.2 --server.port=8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --magic-api.resource.location=${MAGIC_API_RESOURCE_LOCATION:-/data/magic-api} \"$@\"", "--"]
CMD []
