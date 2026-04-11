# DAG Docker Sim Java

这是一个基于 Spring Boot 的 DAG 账本仿真后端项目。当前版本已经参考 `D:\yupao\yupao-backend-master` 的工程风格，整理为更常见的后端分层目录：

- `config`
- `common`
- `constant`
- `controller`
- `core`
- `exception`
- `mapper`
- `model`
- `service`
- `utils`

项目能力保持不变，仍然支持：

- MySQL 持久化账本数据与设备会话
- Redis 缓存高频查询接口
- 云端节点、融合终端、设备模拟器协同运行
- DAG 交易确认、软删除、硬删除等核心逻辑

## 技术栈

- Java 11
- Maven
- Spring Boot 2.7
- Spring Web
- Spring Data JPA
- MySQL 8
- Spring Data Redis

## 目录结构

### 顶层目录

- `com.dagdockersim`
  Spring Boot 启动入口
- `com.dagdockersim.config`
  项目配置
- `com.dagdockersim.common`
  通用返回体与响应工具
- `com.dagdockersim.constant`
  常量定义
- `com.dagdockersim.controller`
  REST 控制器
- `com.dagdockersim.core`
  DAG 仿真核心领域代码
- `com.dagdockersim.exception`
  全局异常处理
- `com.dagdockersim.mapper`
  持久化访问接口
- `com.dagdockersim.model`
  请求对象、领域对象、数据库实体
- `com.dagdockersim.service`
  Service 接口
- `com.dagdockersim.service.impl`
  Service 实现与运行时协作逻辑
- `com.dagdockersim.utils`
  工具类

### core 模块

- `core.bootstrap`
  预置设备与创世数据
- `core.cloud`
  云端节点逻辑
- `core.crypto`
  加密与签名工具
- `core.device`
  设备模拟器
- `core.fusion`
  融合终端逻辑
- `core.ledger`
  DAG 账本核心实现

### model 模块

- `model.domain`
  领域对象，例如交易、生命周期动作
- `model.entity`
  JPA 实体
- `model.request`
  接口请求体
- `model.vo`
  接口返回对象

### service 模块

- `service`
  对外暴露的查询服务、命令服务接口
- `service.impl`
  具体实现
- `service.impl.support`
  运行时上下文、账本持久化适配、设备会话存取等内部支撑类

## 关键文件

- [App.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/App.java)
  应用启动入口
- [BaseResponse.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/common/BaseResponse.java)
  通用响应体
- [GlobalExceptionHandler.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/exception/GlobalExceptionHandler.java)
  全局异常处理
- [SimulationController.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/controller/SimulationController.java)
  接口入口
- [SimulationQueryService.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/SimulationQueryService.java)
  查询服务接口
- [SimulationCommandService.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/SimulationCommandService.java)
  命令服务接口
- [SimulationRuntimeContext.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/SimulationRuntimeContext.java)
  运行时状态装配与恢复
- [DagLedger.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/core/ledger/DagLedger.java)
  DAG 账本核心
- [RedisCacheConfig.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/config/RedisCacheConfig.java)
  Redis 缓存配置
- [LedgerStateStore.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/support/LedgerStateStore.java)
  MySQL 账本快照读写
- [DeviceSessionStore.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/service/impl/support/DeviceSessionStore.java)
  设备会话读写
- [HealthVO.java](/d:/Java_study/dag_docker_sim_java/dag_docker_sim_java/src/main/java/com/dagdockersim/model/vo/HealthVO.java)
  健康检查返回对象

## MySQL 与 Redis 的职责

### MySQL

- 保存 `ledger_transactions`
- 保存 `device_sessions`
- 应用重启后从数据库恢复账本与设备会话

### Redis

- 缓存 `/api/health`
- 缓存 `/api/topology`
- 缓存 `/api/cloud/ledger`
- 缓存 `/api/fusions`
- 缓存 `/api/fusions/{terminalId}/ledger`
- 缓存 `/api/devices`

写接口执行后会自动清理相关缓存。

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

注册设备请求示例：

```json
{
  "deviceName": "device41",
  "useBootstrapIdentity": false,
  "autoConfirm": true
}
```

提交遥测请求示例：

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

## 本次整理结果

- 目录结构更接近常规 Spring Boot 后端脚手架
- controller、service、model、config、mapper 的职责更清晰
- DAG 相关代码统一收敛到 `core`
- 便于继续扩展接口、异常处理、通用响应体等标准后端模块
