+++
title = "可观察性"
weight = 5
+++

## 源码编译

从 Github 下载 Apache ShardingSphere 源码，对源码进行编译，操作命令如下。

```shell
git clone --depth 1 https://github.com/apache/shardingsphere.git
cd shardingsphere
mvn clean install -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Drat.skip=true -Djacoco.skip=true -DskipITs -DskipTests -Prelease
```
agent 包输出目录为 distribution/agent/target/apache-shardingsphere-${latest.release.version}-shardingsphere-agent-bin.tar.gz

## agent 配置

* 目录说明

创建 agent 目录，解压 agent 二进制包到 agent 目录。

```shell
mkdir agent
tar -zxvf apache-shardingsphere-${latest.release.version}-shardingsphere-agent-bin.tar.gz -C agent
cd agent
tree 
.
├── LICENSE
├── NOTICE
├── conf
│   └── agent.yaml
├── lib
│   ├── shardingsphere-agent-metrics-core-${latest.release.version}.jar
│   └── shardingsphere-agent-plugin-core-${latest.release.version}.jar
├── plugins
│   ├── shardingsphere-agent-logging-file-${latest.release.version}.jar
│   ├── shardingsphere-agent-metrics-prometheus-${latest.release.version}.jar
│   ├── shardingsphere-agent-tracing-jaeger-${latest.release.version}.jar
│   ├── shardingsphere-agent-tracing-opentelemetry-${latest.release.version}.jar
│   ├── shardingsphere-agent-tracing-opentracing-${latest.release.version}.jar
│   └── shardingsphere-agent-tracing-zipkin-${latest.release.version}.jar
└── shardingsphere-agent-${latest.release.version}.jar
```

* 配置说明

`conf/agent.yaml` 用于管理 agent 配置。
内置插件包括 Jaeger、OpenTracing、Zipkin、OpenTelemetry、Log 及 Prometheus。
默认不开启任何插件。

```yaml
plugins:
#  logging:
#    File:
#      props:
#        level: "INFO"
#  metrics:
#    Prometheus:
#      host:  "localhost"
#      port: 9090
#      props:
#        jvm-information-collector-enabled: "true"
#  tracing:
#    Jaeger:
#      host: "localhost"
#      port: 6831
#      props:
#        service-name: "shardingsphere"
#        jaeger-sampler-type: "const"
#        jaeger-sampler-param: "1"
#        jaeger-reporter-flush-interval: "1000"
#        jaeger-reporter-max-queue-size: "100"
#    Zipkin:
#      host: "localhost"
#      port: 9411
#      props:
#        service-name: "shardingsphere"
#        url-version: "/api/v2/spans"
#        sampler-type: "const"
#        sampler-param: "1"
#    OpenTracing:
#      props:
#        opentracing-tracer-class-name: "org.apache.skywalking.apm.toolkit.opentracing.SkywalkingTracer"
#    OpenTelemetry:
#      props:
#        otel-resource-attributes: "service.name=shardingsphere"
#        otel-traces-exporter: "zipkin"
```

* 参数说明；

| 名称                                | 说明                        | 取值范围                                                                                                                    | 默认值                               |
|:----------------------------------|:-----------------------------|:------------------------------------------------------------------------------------------------------------------------|:----------------------------------|
| jvm-information-collector-enabled | 是否开启 JVM 采集器             | true、false                                                                                                              | true                              |
| service-name                      | 链路跟踪的服务名称               | 自定义                                                                                                                     | shardingsphere                    |
| jaeger-sampler-type               | Jaeger 采样率类型               | const、probabilistic、ratelimiting、remote                                                                                 | const                             |
| jaeger-sampler-param              | Jaeger 采样率参数               | const：0、1，probabilistic：0.0 - 1.0，ratelimiting：> 0，自定义每秒采集数量，remote：需要自定义配置远程采样率管理服务地址，JAEGER_SAMPLER_MANAGER_HOST_PORT | 1（const 类型）|
| jaeger-reporter-flush-interval    | Jaeger 上报数据刷新间隔(毫秒)    | 自定义                                                                                                                     | 1000                              |
| jaeger-reporter-max-queue-size    | Jaeger 上报 span 时最大队列大小   | 自定义                                                                                                                     | 100                               |
| url-version                       | Zipkin url 地址                 | 自定义                                                                                                                     | /api/v2/spans                     |
| sampler-type                      | Zipkin 采样率类型                | const、counting、ratelimiting、boundary                                                                                    | const                             |
| sampler-param                     | Zipkin 采样率参数                | const： 0、1，counting：0.01 - 1.0，ratelimiting：> 0，自定义每秒采集数量，boundary: 0.0001 - 1.0                               | 1（const 类型）                    |
| otel-resource-attributes          | opentelemetry 资源属性          | 字符串键值对（,分割）                                                                                                         | service.name=shardingsphere-agent |
| otel-traces-exporter              | Tracing expoter                | zipkin、jaeger                                                                                                            | zipkin                            |
| otel-traces-sampler               | opentelemetry 采样率类型         | always_on、always_off、traceidratio                                                                                        | always_on                         |
| otel-traces-sampler-arg           | opentelemetry 采样率参数         | traceidratio：0.0 - 1.0                                                                                                    | 1.0                               |

## ShardingSphere-Proxy 中使用

### 通过非容器环境使用

* 编辑启动脚本

配置 shardingsphere-agent-${latest.release.version}.jar 的绝对路径到 ShardingSphere-Proxy 的 start.sh 启动脚本中，请注意配置自己对应的绝对路径。

```shell
nohup java ${JAVA_OPTS} ${JAVA_MEM_OPTS} \
-javaagent:/xxxxx/agent/shardingsphere-agent-${latest.release.version}.jar \
-classpath ${CLASS_PATH} ${MAIN_CLASS} >> ${STDOUT_FILE} 2>&1 &
```

* 启动 ShardingSphere-Proxy

```shell
bin/start.sh
```
正常启动后，可以在 ShardingSphere-Proxy 日志中找到 plugin 的加载信息，访问 Proxy 后，可以通过配置的监控地址查看到 `Metric` 和 `Tracing` 的数据。

### 通过容器环境使用

- 假设本地已完成如下的对应配置。
  - 包含 ShardingSphere-Agent 二进制包解压后的所有文件的文件夹 `./custom/agent/`
  - 包含 `server.yaml` 等 ShardingSphere-Proxy 的配置文件的文件夹为 `./custom/conf/`

- 此时可通过环境变量 `JVM_OPT` 来配置 ShardingSphere-Agent 的使用。
  以在 Docker Compose 环境下启动为例，合理的 `docker-compose.yml` 示例如下。

```yaml
version: "3.8"

services:
  apache-shardingsphere-proxy:
    image: apache/shardingsphere-proxy:latest
    environment:
      JVM_OPTS: "-javaagent:/agent/shardingsphere-agent-${latest.release.version}.jar"
      PORT: 3308
    volumes:
      - ./custom/agent/:/agent/
      - ./custom/conf/:/opt/shardingsphere-proxy/conf/
    ports:
      - "13308:3308"
```

## Metrics

| 指标名称                            | 指标类型             | 指标描述                                                                                |
| :-------------------------------- | :------------------ | :------------------------------------------------------------------------------------ |
| build_info                        | GAUGE_METRIC_FAMILY | 构建信息                                                                                |
| parsed_sql_total                  | COUNTER             | 按类型（INSERT、UPDATE、DELETE、SELECT、DDL、DCL、DAL、TCL、RQL、RDL、RAL、RUL）分类的解析总数 |
| routed_sql_total                  | COUNTER             | 按类型（INSERT、UPDATE、DELETE、SELECT）分类的路由总数                                      |
| routed_result_total               | COUNTER             | 路由结果总数(数据源路由结果、表路由结果)                                                    |
| proxy_state                       | GAUGE_METRIC_FAMILY | ShardingSphere-Proxy 状态信息。0 表示正常状态；1 表示熔断状态；2 锁定状态                      |
| proxy_meta_data_info              | GAUGE_METRIC_FAMILY | ShardingSphere-Proxy 元数据信息，schema_count：逻辑库数量， database_count：数据源数量        |
| proxy_current_connections         | GAUGE               | ShardingSphere-Proxy 的当前连接数                                                        |
| proxy_requests_total              | COUNTER             | ShardingSphere-Proxy 的接受请求总数                                                      |
| proxy_commit_transactions_total   | COUNTER             | ShardingSphere-Proxy 的事务提交总数                                                      |
| proxy_rollback_transactions_total | COUNTER             | ShardingSphere-Proxy 的事务回滚总数                                                      |
| proxy_execute_latency_millis      | HISTOGRAM           | ShardingSphere-Proxy 的执行耗时毫秒直方图                                                 |
| proxy_execute_errors_total        | COUNTER             | ShardingSphere-Proxy 的执行异常总数                                                      |
