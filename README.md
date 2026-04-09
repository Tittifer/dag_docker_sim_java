# DAG Docker Sim Java 接口版

这是把原始 `D:\dag_docker_sim` 原型项目重构为 Java 后，再整理成 Spring Boot 接口版的工程。

## 当前结构

项目现在按“微服务边界 + 共享模块”的方式组织，但仍保持为单个 Maven 工程，方便本地调试和后续逐步拆分。

- `com.dagdockersim`
  应用启动入口
- `com.dagdockersim.cloud`
  云端节点相关代码
- `com.dagdockersim.fusion`
  融合终端相关代码
- `com.dagdockersim.device`
  设备模拟相关代码
- `com.dagdockersim.simulation`
  当前单进程编排层与 REST 接口
- `com.dagdockersim.shared`
  各模块共享的账本、模型、加密、bootstrap、工具
- `com.dagdockersim.support`
  自检和辅助代码

## 标准 Maven 目录

- `src/main/java`
  主源码目录
- `src/test/java`
  测试源码目录
- `pom.xml`
  Maven 构建配置

## 主要代码位置

- `src/main/java/com/dagdockersim/App.java`
  Spring Boot 启动入口，保留 `--self-check`
- `src/main/java/com/dagdockersim/cloud`
  云端节点
- `src/main/java/com/dagdockersim/fusion`
  融合终端
- `src/main/java/com/dagdockersim/device`
  设备模拟
- `src/main/java/com/dagdockersim/simulation/api`
  REST 控制器
- `src/main/java/com/dagdockersim/simulation/application`
  编排服务
- `src/main/java/com/dagdockersim/shared/ledger`
  DAG 账本核心
- `src/main/java/com/dagdockersim/shared/model`
  交易模型
- `src/main/java/com/dagdockersim/shared/bootstrap`
  预置环境与种子数据
- `src/main/java/com/dagdockersim/support/LedgerSelfCheck.java`
  行为自检

## 运行环境

- JDK 11
- Maven 3.9+

## 启动方式

首次启动会通过 Maven 下载依赖：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

启动后默认访问：

```text
http://localhost:8080/api/health
```

## 自检

```powershell
mvn -s maven-settings.xml -q -DskipTests compile
& 'D:\JDK11\bin\java.exe' -cp 'D:\Java_study\dag_docker_sim_java\dag_docker_sim_java\target\classes' com.dagdockersim.App --self-check
```

## 主要接口

### 查看服务状态

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

### 注册模拟设备

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

### 提交设备遥测数据

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

### 查看当前模拟设备

```http
GET /api/devices
```

## 说明

- 当前是单进程内存版 Spring Boot 接口服务
- 包结构已经按微服务角色边界拆分，后续再做多模块或多服务拆分会更自然
- 原 Python 项目的 Docker Compose、绘图导出、实验脚本暂未继续迁移
