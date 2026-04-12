# DAG Docker Sim Java

这是一个基于 Spring Boot 的 DAG 账本仿真后端项目，目录风格参考了 `yupao-backend` 一类常见 Java 后端分层方式，并结合当前场景保留了云端节点、融合终端、设备模拟、MySQL 持久化和 Redis 缓存。

## 技术栈

- Java 11
- Maven
- Spring Boot 2.7
- Spring Web
- Spring Data JPA
- MySQL 8
- Spring Data Redis

## 目录结构

### 主源码

- `src/main/java/com/dagdockersim`
  应用入口和主包
- `config`
  Spring 配置，例如 Redis 缓存配置
- `common`
  通用响应体和返回工具
- `constant`
  缓存名称等常量
- `controller`
  REST 接口入口
- `core`
  账本、云端、融合终端、设备模拟等核心领域代码
- `exception`
  全局异常处理
- `mapper`
  JPA Repository
- `model`
  请求体、实体、领域对象、返回对象
- `service`
  Service 接口
- `service/impl`
  运行时实现
- `service/impl/support`
  持久化、VO 组装、terminal worker、异步落库协调器等支撑类
- `utils`
  公共工具类

### 并发实验目录

- `tools/concurrency-lab`
  单独存放并发注册、并发遥测和完整场景脚本，不和主业务代码混在一起

## 系统角色

- `cloud`
  云端总账本节点
- `fusion1 / fusion2 / fusion3`
  融合终端
- `device`
  模拟终端接入的新型主体

## 当前并发模型

项目现在采用“多主体并发提交、同 terminal 串行落账、不同 terminal 并行处理”的方式：

- 接口请求可以并发进入
- 同一个融合终端内部通过单线程 worker 顺序修改账本
- 不同融合终端之间可以并行处理
- MySQL 账本快照写入已经改成异步执行，减少 terminal worker 被数据库写入阻塞
- Redis 继续负责高频读接口缓存

这部分核心代码在：

- [SimulationRuntimeContext.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/SimulationRuntimeContext.java)
- [TerminalTaskDispatcher.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/support/TerminalTaskDispatcher.java)
- [AsyncLedgerPersistenceCoordinator.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/support/AsyncLedgerPersistenceCoordinator.java)

## MySQL 和 Redis 的职责

### MySQL

- 持久化 `ledger_transactions`
- 持久化 `device_sessions`
- 服务重启后恢复账本和设备会话

### Redis

- 缓存 `/api/health`
- 缓存 `/api/topology`
- 缓存 `/api/cloud/ledger`
- 缓存 `/api/fusions`
- 缓存 `/api/fusions/{terminalId}/ledger`
- 缓存 `/api/devices`

## 启动前准备

### 1. 创建数据库

执行：

```sql
source sql/init-database.sql;
```

或者手动执行：

```sql
CREATE DATABASE IF NOT EXISTS dag_docker_sim
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 2. 准备 Redis

默认 Redis 地址：

- `localhost:6379`

### 3. 环境变量

默认配置位于 [application.yml](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/resources/application.yml)，可通过环境变量覆盖：

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/dag_docker_sim?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="1"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
```

## 启动方式

```powershell
mvn -s maven-settings.xml spring-boot:run
```

默认访问：

```text
http://localhost:8080/api/health
```

## 编译

```powershell
mvn -s maven-settings.xml -q -DskipTests compile
```

## 主要接口

- `GET /api/health`
- `GET /api/topology`
- `GET /api/cloud/ledger`
- `GET /api/fusions`
- `GET /api/fusions/{terminalId}/ledger`
- `GET /api/devices`
- `POST /api/fusions/{terminalId}/devices/register`
- `POST /api/fusions/{terminalId}/devices/{deviceId}/telemetry`

### 注册设备示例

```json
{
  "deviceName": "device41",
  "useBootstrapIdentity": false,
  "autoConfirm": true
}
```

### 提交遥测示例

```json
{
  "dataPayload": {
    "sequence": 1,
    "device_name": "device41",
    "captured_at": 1773821000.0,
    "metrics": {
      "voltage_v": 228.4,
      "current_a": 16.2,
      "temperature_c": 33.8,
      "active_power_kw": 3.701
    }
  }
}
```

## 并发实验

并发模拟脚本已经单独放到 [tools/concurrency-lab/README.md](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/tools/concurrency-lab/README.md)。

最常用的是：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\concurrency-lab\Full-Scenario.ps1 `
  -DeviceCount 90 `
  -RegisterConcurrency 20 `
  -MessagesPerDevice 4 `
  -TelemetryConcurrency 20
```
