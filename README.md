# DAG Docker Sim Java 接口版

这是把原始 `D:\dag_docker_sim` 原型项目重构为 Java 之后，再进一步改成的 Spring Boot 接口版本。

## 当前能力

- 保留了核心 DAG 账本逻辑
- 保留了创世节点和 bootstrap 预置设备
- 保留了注册交易、业务交易、权重累计、确认、软删除、硬删除语义
- 提供了 Spring Boot REST 接口
- 提供了一个自检入口，用来校验关键账本行为

## 主要目录

- `src/App.java`
  Spring Boot 启动入口，同时保留 `--self-check` 自检模式
- `src/com/dagdockersim/ledger`
  DAG 账本核心实现
- `src/com/dagdockersim/bootstrap`
  初始预置数据和账本灌入逻辑
- `src/com/dagdockersim/service`
  `cloud`、`fusion`、`device` 的内存协作实现
- `src/com/dagdockersim/api`
  Spring Boot 控制器和运行时编排服务

## 运行环境

- JDK 11
- Maven 3.9+

## 启动方式

首次启动 Spring Boot 需要 Maven 拉取依赖：

```powershell
mvn spring-boot:run
```

默认启动后访问：

```text
http://localhost:8080/api/health
```

## 自检

如果你想先跑账本行为验证：

```powershell
mvn -q -DskipTests compile
java -cp target/classes App --self-check
```

## 主要接口

### 1. 查看服务健康状态

```http
GET /api/health
```

### 2. 查看整体拓扑摘要

```http
GET /api/topology
```

### 3. 查看云端账本

```http
GET /api/cloud/ledger
```

### 4. 查看所有融合终端

```http
GET /api/fusions
```

### 5. 查看某个融合终端账本

```http
GET /api/fusions/{terminalId}/ledger
```

例如：

```http
GET /api/fusions/fusion1/ledger
```

### 6. 注册一个模拟设备

```http
POST /api/fusions/{terminalId}/devices/register
Content-Type: application/json
```

请求体示例：

```json
{
  "deviceName": "device41",
  "useBootstrapIdentity": false,
  "autoConfirm": true
}
```

说明：

- `deviceName`：设备名称
- `useBootstrapIdentity`：是否使用预置身份
- `autoConfirm`：是否在注册后立即做一次云端确认并同步到各 fusion

### 7. 提交设备遥测数据

```http
POST /api/fusions/{terminalId}/devices/{deviceId}/telemetry
Content-Type: application/json
```

请求体示例：

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

如果请求体为空，系统会自动生成一份默认遥测数据。

### 8. 查看当前已注册的模拟设备

```http
GET /api/devices
```

## 一个最小调用流程

1. 启动应用

```powershell
mvn spring-boot:run
```

2. 注册设备

```powershell
curl -X POST "http://localhost:8080/api/fusions/fusion1/devices/register" `
  -H "Content-Type: application/json" `
  -d "{\"deviceName\":\"device41\",\"useBootstrapIdentity\":false,\"autoConfirm\":true}"
```

3. 查看设备列表，拿到 `deviceId`

```powershell
curl "http://localhost:8080/api/devices"
```

4. 提交业务数据

```powershell
curl -X POST "http://localhost:8080/api/fusions/fusion1/devices/{deviceId}/telemetry" `
  -H "Content-Type: application/json" `
  -d "{\"dataPayload\":{\"sequence\":1,\"device_name\":\"device41\",\"metrics\":{\"voltage_v\":228.4,\"current_a\":16.2,\"temperature_c\":33.8,\"active_power_kw\":3.701}}}"
```

## 说明

- 当前版本是单进程内存版 Spring Boot 接口服务，不是多容器部署版
- 原 Python 项目的 FastAPI、Docker Compose、绘图导出、实验脚本还没有继续迁移到 Java
- 现在这版更适合先做接口联调、逻辑验证和后续二次开发
