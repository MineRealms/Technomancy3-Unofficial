# 多版本构建 · 当前状态与下一步

> 本文是 `docs/MULTIVERSION-BUILD-PLAN.zh-CN.md`（下称"计划书"）的**进度快照**。
> 计划书是设计与验收的权威来源；本文只回答两个问题：**现在到哪了**、**下一步做什么**。
> 快照时间：2026-10-12。

---

## 1. 目标回顾

把 `Technomancy-1.20.1` 从「单一 Forge 1.20.1 构建」改成「一套源码 + Stonecutter 预处理，
同时产出 1.20.1 Forge 与 1.21.1 NeoForge 两个工件」。

- 工具链：ForgeGradle 6（Gradle 8.8）→ **Stonecutter 0.9.8 + ModDevGradle 2.0.148（Gradle 9.4.1）**。
- 依赖：TC4R 的 1.20.1（`dev.tc4port:thaumcraft-forge`）与 1.21.1（`dev.tc4port:thaumcraft-neoforge`）
  都是**不可再分发的本地 jar**，通过 `local-repo/` + `verifyLocalDependencies` 校验。
- 规范源：`src/` 保持 **1.20.1 方言**；1.21.1 的差异由 Stonecutter 的
  替换表（机械层）+ `//? if` 条件注释（结构层）生成。

分阶段：**S0 仓库重构 → S1 1.21.1 节点骨架 → S2 机械层 → S3 结构层 → S4 运行时与 CI**。

---

## 2. 现状总结

### 2.1 已完成并推送

| 阶段 | 内容 | 分支 / 提交 | 验收结果 |
| --- | --- | --- | --- |
| S-1a | 确认 Stonecutter 0.9.8 的 Gradle 下限 | `452b4a4` | 0.9.x 必须 Gradle 9；0.7.11 是 Gradle 8.8 上最后一版 |
| S-1b | 确认 MDG legacyforge 能复现 1.20.1 构建 | `452b4a4` | jar 逐条比对 862/863 字节相同 |
| S-1c | 确认 Replacements 对 JSON/资源生效 | `452b4a4` | 已验证，并据此定下 `handlers { inherit(...) }` 方案 |
| **S0** | 仓库重构为 Stonecutter 结构，只保留 1.20.1 节点 | `multiversion` `05a059c`；文档 `main` `c86d015` / `63e2d1c` | **全绿** |
| **S1** | 接入 1.21.1 NeoForge 节点骨架 | `multiversion` `abff1d2`，合并 `df690c2`；文档 `main` `3bb4193` | **配置期全绿** |

**S0 门禁（1.20.1 零回归）——全部通过：** jar 比对 862/863 字节相同（唯一差异是
`META-INF/MANIFEST.MF` 的时间戳/工具行）；JUnit 267/267；GameTest 107/107；`runClient` 起到主菜单。

**S1 门禁（1.21.1 节点接线）——通过：** 两节点都能配置；1.21.1 全部依赖可解析；
每节点独立的 `local-repo/DEPENDENCIES.<node>.json` 校验通过；
`compileJava` 报 202 个错误、全部是 `net.minecraftforge.*`（正是 S2/S3 的迁移面）。

### 2.2 进行中：S2 机械层（**已跑通，未提交**）

**目标**：用 Stonecutter 替换表把 1.21.1 的编译错误压到「只剩结构性文件」。

#### 2.2.1 关键突破：`parameters {} 是逐节点重放的`

S2 一开始卡在 Kotlin 写法 `current.parsed >= '1.21'` 在 Groovy 里报
`No such property: parsed for class DefaultMapProperty$EntryProvider`。

查清了**两层**原因（都在 `stonecutter.gradle`）：

1. **`current` 被闭包 delegate 遮蔽。** `constants {}` / `replacements {}` 的 delegate 是
   `ConstantContainer` / `ReplacementContainer`，二者都继承 Gradle 的 `MapProperty`，因此
   **暴露 `get(String)`**。Groovy 的「类 Map 访问」会把裸 `current` 解析成
   `delegate.get("current")`，拿回一个 `Provider`，而不是扩展的 `getCurrent()`。
2. **`parameters {}` 不是"根项目一次性配置"，而是被控制器逐版本重放一次**，
   每次以该节点的 `StonecutterBuildExtension` 作为闭包 delegate —— 所以块内的 `current`
   才是"正在配置的那个节点"。把版本判断写在**脚本顶层**（我第一版就是）只会在**根项目**上求值一次，
   此时 `current` 是 `stonecutter.active(...)` 钉住的**活动版本**（1.20.1），于是 `neo` 恒为 `false`，
   两个节点都拿不到替换。

**正确写法**（也是本次的修复）：

```groovy
stonecutter.parameters {
    def neo = eval(current.version, '>=1.21')   // 在块内算，逐节点求值

    constants {
        put('forge', !neo)
        put('neoforge', neo)
    }
    replacements {
        string(neo) { replace('net.minecraftforge.fml.', 'net.neoforged.fml.') }
        // ...
    }
}
```

`eval(version, condition)` 是 `VersionOperations` 上的普通布尔方法，绕开了 Kotlin 专有的
`current.parsed >= '1.21'`。传给 `string(...)` / `regex(...)` 的这个布尔量正是
**`direction` 标志**（true = 正向 from→to，false = 反向 to→from）。规范源是 1.20.1 方言，
所以 1.20.1 节点上每条规则都走反向、找不到目标，等价于 no-op。

> **验证方法**：往 `parameters {}` 里插一行 `println`，会看到**每个节点打印一次**
> （`project=1.20.1-forge` / `1.21.1-neoforge`）；插在脚本顶层则只打印一次（`project=<根项目名>`）。

#### 2.2.2 已落地的替换规则

- Forge → NeoForge 包名/类名替换（**具体优先、通配兜底**排序，避免
  `net.minecraftforge.api.distmarker` 变成 `net.neoforged.neoforge.api.distmarker`）。
- `new ResourceLocation(...)` ⇄ `ResourceLocation.fromNamespaceAndPath(...)` 的正反双向正则。
- **能力包搬迁**：NeoForge 1.21.1 把能力 API 从 `common.capabilities` 移到了顶层
  `capabilities`，并把 `ForgeCapabilities` 改名 `Capabilities`。已加入替换表。
- `handlers { inherit('json5','json'); inherit('json5','mcmeta') }` 注册资源处理器。

#### 2.2.3 顺带修正：规范源里的 8 处 API 混用

`src/` 里有 **5 个文件共 8 处**写的是 `ResourceLocation.fromNamespaceAndPath(...)`。
Forge 1.20.1 **反移植**了这个工厂（字节码就是 `new ResourceLocation(ns, path)` 的别名），
所以它在 1.20.1 上也能编译 —— 但这会让 1.20.1 节点的反向规则把它改回 `new ResourceLocation(...)`，
**破坏"1.20.1 节点 = 源码直通"这条不变式**。

已把这 8 处统一回 1.20.1 方言的 `new ResourceLocation(...)`。之后：

| 节点 | 生成树 vs `src/` |
| --- | --- |
| 1.20.1-forge | **0 处差异**（逐字节直通） |
| 1.21.1-neoforge | 见下 |

#### 2.2.4 S2 实测结果

**1.21.1 节点替换效果**（生成树统计）：

| 指标 | 值 |
| --- | --- |
| 残留 `net.minecraftforge.*` | **0** |
| `net.neoforged.*` | 59 个不同包/类 |
| 残留 `new ResourceLocation(` | **0** |
| `ResourceLocation.fromNamespaceAndPath(` | 55 处 |
| 双重前缀（`neoforged.neoforge.neoforge`） | **0** |

**1.21.1 编译门禁**（`-Xmaxerrs` 放开后的全量）：

- **755 个错误 / 124 个文件**（放开前被 javac 默认的 100 上限截断）。
- 错误来源拆解（按涉及的 import 行计）：

  | 缺失项 | 行数 | 性质 |
  | --- | --- | --- |
  | `net.neoforged.neoforge.common.util.LazyOptional` | 44 | **结构**：1.21.1 已移除 `LazyOptional` |
  | `net.neoforged.neoforge.capabilities.Capability` | 44 | **结构**：拆成 `BlockCapability`/`ItemCapability`/`EntityCapability` |
  | `vazkii.botania.api.*` | 24 | **模块缺席**：Botania 无 1.21.1 构建 |
  | `net.neoforged.neoforge.event.TickEvent` | 8 | **结构**：`TickEvent` 拆分 |
  | `net.neoforged.neoforge.network.*`（`NetworkEvent`/`SimpleChannel`/…） | 12 | **结构**：改 payload/codec 模型 |
  | `net.neoforged.fml.*`（`DistExecutor`/`FMLJavaModLoadingContext`） | 10 | **结构**：已删除/移位 |
  | `net.neoforged.neoforge.event.AttachCapabilitiesEvent` | 4 | **结构**：改为 `RegisterCapabilitiesEvent`（推模型） |
  | `com.gregtechceu.gtceu.*` | 8 | **结构**：可选模块，1.21.1 版本 API 不同 |
  | `net.neoforged.neoforge.client.gui.overlay.*` | 2 | **结构**：改为 `LayeredDraw` + `RegisterGuiLayersEvent` |
  | `net.neoforged.neoforge.registries.ForgeRegistries` | 4 | **结构**：改名/移位 |

- **关键校正：能力那一批是 22 个文件，与计划书的估算一致。**
  同时 import `LazyOptional` 与 `Capability` 的文件恰好是同一批 22 个（重叠度 100%）：
  `EnergyProtocolExtension`、`MachineEnergy`、`GtceuEnergyProtocol`、`GameTestEnergySink`、
  `WandChargeEvents`，以及 17 个 `*BlockEntity`（各机器/发电机）。
- **剩下的 ~102 个文件是"级联失败"**：它们自己不 import 被删的类，但引用了上述 22 个文件的类型/方法，
  于是跟着报"找不到符号"。修好那 22 个文件后，级联会成片消失。
  所以计划书写的「53 个结构性文件」低估了**观测到的报错文件数**，但没有低估**真正要动手的文件数**。

### 2.3 未开始

- **S3 结构层**：能力系统（pull → push 模型）、`TickEvent` 拆分、`DistExecutor`、
  主类构造、`PacketDistributor`/网络重写、GUI overlay（`LayeredDraw`）、`openScreen`。
- **S2 收尾·资源层**（见 §3.1）：目录单数化、配方 `result.item`→`result.id`、`pack_format` 15→34。
- **S4 运行时与 CI**：1.21.1 的 datapack 加载、GameTest、DEV 客户端探针（端口 48792）、
  CI 矩阵、文档收尾。
- **可选兼容模块**：GTCEu / Botania / Jade / KubeJS / JEI。**Botania 没有 1.21.1 构建**，
  该模块在 1.21.1 侧只能缺席。

---

## 3. 下一步（按顺序）

### 3.1 收尾 S2 的资源层（当前第一优先）

替换表只作用于**文件内容**，不作用于**路径**，所以下面两件事必须另做：

1. **目录单数化**（1.21 的破坏性改名，共 119 个文件）：

   | 1.20.1 | 文件数 | 1.21.1 |
   | --- | --- | --- |
   | `data/technom/recipes/` | 70 | `data/technom/recipe/` |
   | `data/technom/loot_tables/` | 40 | `data/technom/loot_table/` |
   | `data/technom/structures/` | 2 | `data/technom/structure/` |
   | `data/technom/tags/items/` | 4 | `data/technom/tags/item/` |
   | `data/minecraft/tags/blocks/` | 2 | `data/minecraft/tags/block/` |
   | `data/thaumcraft/tags/items/` | 1 | `data/thaumcraft/tags/item/` |

   **做法**：在 `build.gradle` 的 1.21.1 分支里注册一个 `Sync` 任务，把 Stonecutter 生成出的资源
   重命名到 `build/generated/datapack-1.21.1/`，再把这个目录加进 `sourceSets.main.resources`。
   正则要锚在路径段边界（`(?<=/)recipes/`），否则会误伤 `data/technom/thaumcraft/research/` 这类目录。
   注意 **`tags/worldgen/biome/` 与 `data_maps/` 在两个版本里都是复数，不要改**。

2. **配方与元数据内容**（可以走替换表，但要先确认 TC4R 侧序列化器）：
   - `"result": { "item": ... }` → `"result": { "id": ... }`（68/70 个配方）。
     `key` 里的 `{"item": ...}` / `{"tag": ...}` **不变**。
   - `pack.mcmeta` 的 `pack_format` 15 → 34。
   - ⚠️ 这些配方是 **TC4R 自定义类型**（`thaumcraft:arcane_shaped`），格式取决于 TC4R 20736 的
     序列化器，**不能默认套用原版漂移**。要拿 20736 自带配方对照后再定。
   - ⚠️ `data/technom/thaumcraft/research/*.json` 的 `research` 字段写法需与 20736 的
     `ResearchCatalog` 复核（本工程写 `"technom:BIOMORPHER"`，TC4R 自带写不带命名空间的
     `"ADVALCHEMYFURNACE"`）。

### 3.2 S3 结构层

按 `docs/MIGRATION-1.21.1.zh-CN.md` 的 D 桶逐项改写，用 `//? if neoforge { ... } //? else { ... } //? endif`
承载。顺序建议按"影响面 × 依赖方向"排：

1. **能力系统（22 个文件，最大项）**：`LazyOptional` → `Optional`；
   `Capability<T>` → `BlockCapability`/`ItemCapability`/`EntityCapability`；
   `AttachCapabilitiesEvent`（拉）→ `RegisterCapabilitiesEvent` + `Capabilities.*`（推）。
   **先做这个**，因为它是 ~102 个级联失败的总源头。
2. **网络（~12 个文件）**：`SimpleChannel`/`NetworkEvent` → `RegisterPayloadHandlersEvent` +
   `IPayloadHandler`/`IPayloadContext`；`PacketDistributor` 调用点重写。
3. **`TickEvent` 拆分（8 个文件）**；**`DistExecutor`/`FMLJavaModLoadingContext`（~10 个文件）**；
   **GUI overlay → `LayeredDraw`（2 个文件）**；`openScreen`。
4. **可选模块**：GTCEu（8）、Botania（24，1.21.1 侧直接不编译该模块）。

### 3.3 S4 运行时与 CI

1. 1.21.1 的 datapack 能加载（验证 §3.1 的资源迁移真的对）。
2. `:1.21.1-neoforge:runGameTestServer` 跑通，把 GameTest 接进门禁。
3. DEV 客户端探针：**端口 48792**（1.20.1 用 48791，两者不能共用）。
   注意当前机器上 48791 被用户的 Isorropia 客户端占用。
4. CI 改成矩阵（1.20.1 + 1.21.1）；`TC4R_ARCHIVE_URL` 门禁保持。
5. 更新 README 与计划书，收尾。

---

## 4. 风险与开放问题

| 编号 | 风险 | 状态 / 应对 |
| --- | --- | --- |
| R8 | 两个节点的 `runData` 产物互相覆盖 | 已缓解：生成资源分目录（`resources` vs `resources-1.21.1`） |
| R9 | 两个节点共用桥接端口 | 已缓解：48791 / 48792 分离 |
| R11 | 替换写在 `build.gradle` 被静默忽略；资源扩展名未注册 handler | 已固化：写在 `stonecutter.gradle` 的 `parameters { replacements {} }`；`json`/`mcmeta` 已注册 |
| R12 | 资源路径单数化替换表管不到 | **开放**：见 §3.1，需专门的 `Sync` 机制 |
| R13（新） | **替换方向位的求值位置**：版本判断必须写在 `parameters {}` **块内**，写块外会退化成"根项目一次性求值" | **已固化**：见 §2.2.1；门禁用"1.20.1 生成树与 `src/` 逐字节比对 = 0 差异"兜住 |
| R14（新） | **规范源 API 混用**（如 5 个文件用了 1.20.1 Forge 反移植的 `fromNamespaceAndPath`）会破坏 1.20.1 直通性 | **已修**：8 处统一回 `new ResourceLocation(...)`；直通性已恢复到 0 差异 |
| — | 计划书的「53 个结构性文件」低估了观测报错文件数 | 已澄清：真正要动手的仍是那 22 个能力文件 + ~30 个其它结构文件，其余为级联 |
| — | Botania 无 1.21.1 构建 | 已接受：该兼容模块 1.21.1 侧缺席 |
| — | `main` 工作区有未提交的 TC4R 20721→20732 WIP | 未触碰，与本任务无关 |

---

## 5. 一句话结论

**S0、S1 已完成并推送；S2 的机械替换层已跑通并实测验证**——1.20.1 节点与 `src/` **逐字节零差异**，
1.21.1 节点残留 `net.minecraftforge.*` 归零、无双重前缀，编译错误已从 202 收敛到
**755 个 / 124 个文件、全部是结构性**（其中能力那一批恰为 22 个文件，与计划书估算吻合）。

**下一步：先收尾 S2 的资源层（目录单数化 + 配方/`pack_format` 内容），
再进 S3 结构层——从影响面最大的能力系统（22 个文件）入手，它会带走 ~102 个级联失败。**
