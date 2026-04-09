# DAG Docker Sim Java 接口版

这是将原始 DAG 仿真项目重构为 Java Spring Boot 接口版后的实现。当前版本已经完成：

- 使用 MySQL 持久化账本和设备会话
- 使用 Redis 缓存高频查询接口
- 按接口层、应用层、运行时上下文、基础设施层重新整理目录

项目目前仍是单体 Spring Boot 工程，但代码组织已经按微服务拆分思路进行模块化，便于后续继续拆成独立服务。

## 技术栈

- Java 11
- Maven
- Spring Boot 2.7
- Spring Web
- Spring Data JPA
- MySQL 8
- Spring Data Redis

## 目录结构

### 顶层模块

- `com.dagdockersim`
  应用启动入口
- `com.dagdockersim.cloud`
  云端节点逻辑
- `com.dagdockersim.fusion`
  融合终端逻辑
- `com.dagdockersim.device`
  设备模拟器
- `com.dagdockersim.shared`
  公共账本、模型、加密、bootstrap、工具类
- `com.dagdockersim.simulation`
  仿真接口与运行时编排

### simulation 模块细分

- `simulation.api`
  REST 控制器
- `simulation.api.dto`
  请求 DTO
- `simulation.application.command`
  写操作服务，负责注册设备、提交遥测、缓存失效
- `simulation.application.query`
  读操作服务，负责健康检查、拓扑、账本查询、设备查询
- `simulation.application.runtime`
  运行时上下文，负责维护 cloud、fusion、device session 的内存态协作与启动恢复
- `simulation.infrastructure.cache`
  Redis 缓存配置与缓存名称常量
- `simulation.infrastructure.persistence.ledger`
  账本持久化适配层
- `simulation.infrastructure.persistence.session`
  设备会话持久化适配层

## 当前模块职责

### 1. 接口层

- [SimulationController.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/api/SimulationController.java)
  只负责接收 HTTP 请求并转发给查询服务或命令服务

### 2. 应用层

- [SimulationQueryService.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/application/query/SimulationQueryService.java)
  负责读接口与 Redis 缓存
- [SimulationCommandService.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/application/command/SimulationCommandService.java)
  负责写接口与缓存失效
- [SimulationRuntimeContext.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/application/runtime/SimulationRuntimeContext.java)
  负责运行态装配、启动恢复、账本同步、设备会话恢复

### 3. 基础设施层

- [RedisCacheConfig.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/infrastructure/cache/RedisCacheConfig.java)
  Redis 缓存 TTL 和序列化策略
- [SimulationCacheNames.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/infrastructure/cache/SimulationCacheNames.java)
  缓存名称统一常量
- [LedgerStateStore.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/infrastructure/persistence/ledger/LedgerStateStore.java)
  MySQL 账本快照读写
- [TransactionPersistenceMapper.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/infrastructure/persistence/ledger/TransactionPersistenceMapper.java)
  Transaction 与数据库实体之间的转换
- [DeviceSessionStore.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/simulation/infrastructure/persistence/session/DeviceSessionStore.java)
  设备会话读写

## 持久化与缓存设计

### MySQL 中保存的内容

- `ledger_transactions`
  保存 `cloud`、`fusion1`、`fusion2`、`fusion3` 的账本快照
- `device_sessions`
  保存已注册设备的运行会话，便于应用重启后继续提交遥测

### Redis 中缓存的内容

- `/api/health`
- `/api/topology`
- `/api/cloud/ledger`
- `/api/fusions`
- `/api/fusions/{terminalId}/ledger`
- `/api/devices`

写接口执行后会自动：

- 刷新 MySQL 中的账本快照
- 清理相关 Redis 缓存

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

默认连接：

- `localhost:6379`

### 3. 环境变量

默认配置位于 [application.yml](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/resources/application.yml)，也可以通过环境变量覆盖：

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

启动后默认访问：

```text
http://localhost:8080/api/health
```

## 编译

执行：

```powershell
mvn -s maven-settings.xml -q -DskipTests compile
```

## 主要接口

### 健康检查

```http
GET /api/health
```

### 查看整体拓扑

```http
GET /api/topology
```

### 查看云端账本

```http
GET /api/cloud/ledger
```

### 查看融合终端列表

```http
GET /api/fusions
```

### 查看某个融合终端账本

```http
GET /api/fusions/{terminalId}/ledger
```

### 注册设备

```http
POST /api/fusions/{terminalId}/devices/register
Content-Type: application/json
```

请求示例：

```json
{
  "deviceName": "device41",
  "useBootstrapIdentity": false,
  "autoConfirm": true
}
```

### 提交遥测数据

```http
POST /api/fusions/{terminalId}/devices/{deviceId}/telemetry
Content-Type: application/json
```

请求示例：

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

### 查看当前模拟设备

```http
GET /api/devices
```

## 本次重构收益

- 读写职责分离，接口层更薄
- 运行时状态集中到 `runtime`，启动恢复逻辑更清晰
- MySQL 和 Redis 配置收敛到 `infrastructure`，目录可读性更强
- 持久化代码按 `ledger` 和 `session` 分开，后续扩展更自然

## 当前边界

- 目前仍是单进程 Spring Boot 应用，不是真正分布式部署
- 账本写入仍采用整终端快照覆盖方式，优先保证实现简单和可维护
- 暂未引入消息队列、分布式锁、统一鉴权和监控体系
