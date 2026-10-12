# 多版本构建 · 当前状态

> 快照时间：2026-10-13。替代此前 S2 阶段的进度快照；设计背景仍见 `MULTIVERSION-BUILD-PLAN.zh-CN.md`。

## 1. 结论

一套主要源码经 Stonecutter 0.9.8 + ModDevGradle 2.0.148（Gradle 9.4.1）分别构建两个独立 JAR：

| 目标 | Java | Loader | TC4R | 产物 |
|---|---|---|---|---|
| `1.20.1-forge` | 17 | Forge 47.4.23 | 20721 | `technom-1.20.1-forge-0.1.0-dev.jar`（reobf） |
| `1.21.1-neoforge` | 21 | NeoForge 21.1.259 | 20736 | `technom-1.21.1-neoforge-0.1.0-dev.jar` |

`gradlew buildAndCollect` 依次构建、跑 JUnit、检查两个 JAR，全部成功后收集到 `build/distributions/`。不支持一个 JAR 跨版本，也不覆盖其它 1.20.x/1.21.x 小版本或同版本另一加载器。

## 2. 结构

- 规范源仍是 1.20.1 方言；NeoForge 差异用 `stonecutter.gradle` 的有边界替换和 `//? if forge/neoforge` 条件。
- 加载器、MC 版本、Java 版本在 `versions/<目标>/gradle.properties` 里独立声明（`mod_loader`、`java_version`）。
- 平台边界集中在 `common/platform/`：`CapabilityAccess`（查询外部能力）、`StackData`（物品自定义数据读改写）、`PlatformVersion`；NeoForge 专有的能力注册在 `versions/1.21.1-neoforge/src/.../NeoCapabilities` 与 `NeoGtCapabilities`。
- 能源账本、端口规则、机器玩法与存档键共享；NeoForge 用 `RegisterCapabilitiesEvent` + 可空查询 + `invalidateCapabilities`，不是把 `LazyOptional` 换成 `Optional`。
- 资源由 `gradle/neoforge-resources.gradle` 在构建时转换：`neoforge.mods.toml`、`pack_format` 34、单数数据目录、配方 `result.id`、`neoforge:conditions`、`forge:` 公共标签转 `c:`、罐子掉落 `copy_nbt` 转 `copy_components`。

## 3. 本轮实测（2026-10-12/13）

| 检查 | 结果 |
|---|---|
| `buildAndCollect` | 成功，两份 JAR 均通过 `tools/check_target_jar.py`（元数据展开、字节码 61/65、无 GameTest、目录正确） |
| JUnit | 两个目标各 272/272（含 `TargetResourcesTest` 读取各自实际产物） |
| Forge GameTest：默认 / `withGtceu` / `withBotania=false` | 各 111/111；带 GT 时真实 LV 电炉接收 EU |
| NeoForge GameTest：默认 / `withKubejs` | 109/109（加罐子掉落测试前）/ 111/111 |
| NeoForge 数据校验 `--require-inventory` | 0 error（16 warning，同旧版类别声明提示） |
| NeoForge 客户端入隔离世界 | PASS：40 方块、368 状态、71 物品、14 自定义物品渲染、18 方块实体渲染器；手册界面截图正常 |

实测中修复的问题：罐子掉落表在新版无法解析；三个渲染器跨 RenderType 复用已结束的缓冲区导致崩溃；NeoForge 兼容层已调用旧 `initializeClient`，额外注册导致重复。

## 4. 已知限制

- **NeoForge + GTCEu 7.0.2：不可用。** 专用服务端在 GTCEu 自身构造阶段加载客户端类 `ClientLevel` 而崩溃，与 Technomancy 无关、单独启用 GT 也复现。EU 适配代码已编译，但未经运行验证，CI 不跑此组合。该版本 Maven 上 7.0.2 是最新正式版。
- **Botania 联动仅 1.20.1。** 官方 1.21.1 NeoForge 移植在 `VazkiiMods/Botania` 的 `1.21.1-porting` 分支开发中（核对时为 2026-10-11 的 `f036739`），Maven/Modrinth 上还没有可依赖的正式版本，因此 1.21.1 暂不打包相关类、配方、掉落和客户端接线。
  - 核对该分支：Technomancy 用到的 `ManaReceiver` / `PoolOverlayProvider` 签名不变；`ManaPool` 去掉了 `getColor/setColor`；能力改为 `ManaReceiver.LOOKUP` 经 `BotaniaNeoForgeCapabilities` 注册；魔力灌注配方的输出改用 `id`。等正式版发布后即可按这些差异接回。
- NeoForge GameTest 进程在模组加载失败时可能以 0 退出；`runGameTestServer` 已加日志门禁，未看到“全部通过”即失败。
- 旧 Rosetta 客户端探针只支持 Forge 1.20.1；新版客户端验证使用 `-PclientSmoke=true` 的内置检查。
- 未做 1.20.1 → 1.21.1 存档升级测试，也不承诺降级。
