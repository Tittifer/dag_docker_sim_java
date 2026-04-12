# 并发实验工具

这个目录专门用来做“多主体并发注册 / 并发遥测上报”的实验，不和主业务源码混在一起。

这里的思路是：

- PowerShell 只负责启动工具
- 真正的并发请求由 `src/ConcurrencyLabRunner.java` 发起
- 输出结果落到 `output/` 目录，默认是 CSV

## 前提

先启动主服务：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

默认接口地址：

```text
http://localhost:8080
```

## 目录说明

- `Common.ps1`
  PowerShell 包装层，负责定位 JDK、编译并运行实验工具
- `Register-Burst.ps1`
  批量并发注册设备
- `Telemetry-Burst.ps1`
  基于注册结果批量并发上报遥测
- `Full-Scenario.ps1`
  一键执行“先注册、再上报”的完整场景
- `src/ConcurrencyLabRunner.java`
  独立的 JDK11 并发实验工具
- `output/`
  运行后自动生成结果文件

## 1. 并发注册

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\concurrency-lab\Register-Burst.ps1 `
  -DeviceCount 60 `
  -Concurrency 15
```

默认输出：

```text
tools/concurrency-lab/output/register-results.csv
```

## 2. 并发遥测

先执行注册脚本，然后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\concurrency-lab\Telemetry-Burst.ps1 `
  -MessagesPerDevice 5 `
  -Concurrency 20
```

默认读取：

```text
tools/concurrency-lab/output/register-results.csv
```

默认输出：

```text
tools/concurrency-lab/output/telemetry-results.csv
```

## 3. 一键完整场景

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\concurrency-lab\Full-Scenario.ps1 `
  -DeviceCount 90 `
  -RegisterConcurrency 20 `
  -MessagesPerDevice 4 `
  -TelemetryConcurrency 20
```

这个脚本会：

1. 按轮询方式把设备分配到 `fusion1 / fusion2 / fusion3`
2. 并发发起设备注册
3. 基于注册成功的设备继续并发发起遥测上报
4. 把结果落到 `tools/concurrency-lab/output/`

## 结果文件

结果文件会记录：

- 终端编号
- 设备编号
- 请求是否成功
- 单次耗时
- 错误消息

这套工具更适合做“并发场景验证”和“接口链路回归”，不是替代 JMeter、k6 这类专业压测平台。
