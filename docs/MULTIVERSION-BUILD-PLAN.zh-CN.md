# 多版本构建管线重构方案：1.20.1 (Forge) + 1.21.1 (NeoForge)

核对日期：2026-10-12。本文是**方案报告**，不含任何代码改动。所有数字都可用文中命令复现，所有关于第三方工具的能力声明都附了出处；未能核实的点在第 11/12 节单独列为风险与待验证项。

配套文档：[MIGRATION-1.21.1.zh-CN.md](MIGRATION-1.21.1.zh-CN.md)（1.21.1 的 API 迁移面评估）。

---

## 0. 结论与推荐

**推荐架构：Stonecutter 0.9.8 + ModDevGradle（双节点）。**

一条命令、一份源码，产出两个 jar：

```
./gradlew buildAndCollect
  → build/libs/0.1.0-dev/technom-1.20.1-forge-0.1.0-dev.jar
  → build/libs/0.1.0-dev/technom-1.21.1-neoforge-0.1.0-dev.jar
```

**为什么是它**：本工程 304 个源文件里，**251 个（83%）在 1.21.1 上的差异是纯机械的**（改包名、改类名、构造器换名、加一个参数、目录改名），只有 **53 个（17%）需要真正的结构性改写**。Stonecutter 的设计目标正是"83% 共享 + 17% 条件化"，而且它提供 **replacements（正则替换）**，能让那 83% 里的绝大多数**在源码里一个字都不用改**。

**最大的不确定项不是架构，是工具链版本**：1.20.1 节点现在跑在 ForgeGradle 6.0.54 上，而 ForgeGradle 6 与 Gradle 9 不兼容；Stonecutter 0.9.x 的 Gradle 下限官方未写明（模板用 Gradle 9.8）。

- 若 Stonecutter 能在 **Gradle 8.8** 上跑 → 1.20.1 节点**可以继续用 ForgeGradle 6，已发布的构建零风险**。
- 若必须 Gradle 9 → 1.20.1 节点必须换成 ModDevGradle 的 **LegacyForge** 插件（官方支持 1.17–1.20.1），这是本方案**唯一的高风险项**。

所以执行计划的第一件事是**一个丢弃式的 spike**（第 10 节 S-1），先把这个版本问题钉死，再决定动不动 1.20.1 的构建。

---

## 1. 目标与约束

### 1.1 目标

1. 一份源码维护两个版本，不产生两份需要手工同步的 Java 树。
2. 一条命令产出两个 jar；CI 一条流水线跑两个版本。
3. 1.20.1 的**现有产出与验证不回归**（它已经在发货）。
4. 机械性差异（包名、目录名、JSON 字段）由**声明式规则**处理，不进源码。
5. 只有真正的结构性差异才出现在源码里。

### 1.2 硬约束

| 约束 | 内容 |
|---|---|
| MC 版本 | 1.20.1 vs 1.21.1（**不是**同一个原版，vanilla API 有漂移） |
| 加载器 | Forge 47.4.23 vs NeoForge 21.1.248+（**不是**同一个平台，API 大面积改名/删除） |
| Java | 17 vs 21（同一构建内可逐节点设置 toolchain） |
| TC4R | 每版本一份、**不可再分发**、且两版本的**打包形态不同**（见 §7） |
| 1.21.1 运行时依赖 | **当前缺失**——`20736-dev.zip` 只有 api + sources，没有运行时 jar（见 MIGRATION §1.3） |
| 验证装置 | 现有验证挂在 Gradle run 配置上：`run-client`（Rosetta 桥，端口 48791）、`run-gametest`（`forge.enabledGameTestNamespaces`）、`run-data`、`run-server` |
| CI | 依赖不可再分发 → 用仓库变量门控，默认"skipped 而非 failed" |

---

## 2. 现状盘点

### 2.1 源码规模与耦合度

复现命令：

```bash
python - <<'PY'
import re, glob, collections
files = sorted(glob.glob('src/main/java/**/*.java', recursive=True))
b = collections.defaultdict(lambda: [0,0])
for f in files:
    imps=[re.match(r'import (static )?([A-Za-z0-9_.]+);',l).group(2)
         for l in open(f,encoding='utf-8',errors='replace')
         if re.match(r'import (static )?([A-Za-z0-9_.]+);',l)]
    loc=sum(1 for _ in open(f,encoding='utf-8',errors='replace'))
    loader=any(i.startswith(('net.minecraftforge','net.neoforged')) for i in imps)
    mc=any(i.startswith('net.minecraft.') for i in imps)
    tc=any(i.startswith('dev.tc4port') for i in imps)
    k='loader-coupled' if loader else 'vanilla-only' if mc else 'tc4r-only' if tc else 'pure-java'
    b[k][0]+=1; b[k][1]+=loc
for k,(n,l) in sorted(b.items(), key=lambda x:-x[1][1]): print(k,n,l)
PY
```

| 类别 | 文件 | LOC | 占比 |
|---|---|---|---|
| 加载器耦合（import `net.minecraftforge`） | 102 | 15,208 | 46.4% |
| 仅原版耦合 | 118 | 15,077 | 46.0% |
| 纯 Java（无 MC、无加载器） | 41 | 1,873 | 5.7% |
| 仅 TC4R（无原版） | 7 | 648 | 2.0% |
| **合计** | **268** | **32,806** | |

加上 36 个测试文件（4,718 LOC），总计 **304 个 Java 文件**。

### 2.2 分歧的性质（这是选型的决定性数据）

把"必须结构性改写"的标记（`LazyOptional` / `Capability` / `ForgeCapabilities` / `ICapabilityProvider` / `AttachCapabilitiesEvent` / `TickEvent` / `DistExecutor` / `SimpleChannel`+`NetworkRegistry`+`NetworkDirection`+`NetworkHooks` / `IGuiOverlay`+`RegisterGuiOverlaysEvent` / `FMLJavaModLoadingContext` / `LivingHurtEvent` / `ItemStack` 的 `getOrCreateTag`/`getTag`/`setTag`）与"是否 import Forge"交叉统计：

| 分类 | 文件数 | 说明 |
|---|---|---|
| **只需机械变换** | **251（83%）** | 含 57 个"import 了 Forge 但只需改包名/类名"的文件 |
| **需要结构性改写** | **53（17%）** | 其中 46 个同时 import Forge，7 个不 import |
| 其中：既不 import Forge 也无结构性标记 | 194 | 最多只需要原版层的机械变换 |

**结论**：这是一个"83% 机械 + 17% 结构"的分歧分布。机械部分用**声明式替换**处理，结构性部分用**条件编译**处理——正好是 Stonecutter 的两大能力。

### 2.3 资源与数据包

| 项 | 数量 |
|---|---|
| `src/main/resources` 文件总数 | 547（textures 189 / models 163 / blockstates 44 / data 137 / lang 2 …） |
| 需要改目录名的数据包文件 | 119（recipes 70、loot_tables 40、tags/items 4、minecraft/tags/blocks 2、thaumcraft/tags/items 1、structures 2） |
| 需要改 JSON 字段的配方 | 68（`result.item` → `result.id`） |

### 2.4 构建与验证现状

| 项 | 现状 |
|---|---|
| Gradle | 8.8 |
| 构建插件 | `net.minecraftforge.gradle` 6.0.54（Groovy DSL） |
| 依赖注入方式 | `fg.deobf(...)` + 大量 `transitive = false` |
| 本地依赖校验 | `verifyLocalDependencies` 任务，读 `local-repo/DEPENDENCIES.json` 校验 SHA-256；`compileJava` / `check` 依赖它 |
| mixin | `technom.mixins.json`（1 个 mixin）+ manifest `MixinConfigs` + `mixin.env.remapRefMap` 运行属性 |
| 运行配置 | `client`（`run-client/`，`rosetta.remote.port=48791`，可选 `--quickPlaySingleplayer`）、`server`、`gameTestServer`（`forge.enabledGameTestNamespaces`）、`data` |
| 测试 | JUnit（267 用例 / 35 类）+ 30 个 GameTest 文件（24 个 `@GameTestHolder`） |
| CI | `.github/workflows/build.yml`，`validate` / `build` / `gametest`，后两者门控在 `vars.TC4R_ARCHIVE_URL` |

---

## 3. 候选架构对比

| | **A. Stonecutter + MDG 双节点**（推荐） | **B. 两个独立构建，只共享资源** | **C. Git 分支** | **D. 两个构建 + 自研生成器** |
|---|---|---|---|---|
| 源码份数 | **1** | 2（33k LOC ×2） | 2（分支各一份） | 1（生成物） |
| 83% 机械差异 | 声明式替换，**源码零噪音** | 手工改两份 | 每次 merge 冲突 | 自研规则，等价于 A 的一部分 |
| 17% 结构差异 | 条件注释 + 版本目录 | 天然隔离，**最干净** | 天然隔离 | 条件注释（仍要自造语法） |
| 1.20.1 风险 | 取决于 Gradle 版本（见 §0） | **零** | 零 | 零（不动 ForgeGradle） |
| 工具成熟度 | 1715 个模组在用，官方模板 | 无新工具 | 无新工具 | 自己维护一个预处理器 |
| IDE 体验 | 需装 Stonecutter IDEA 插件；`src/` 只显示当前激活版本 | 两个独立工程，最好 | 切分支 | 生成物不在 VCS 里，跳转可能失效 |
| 一条命令出两个 jar | 原生（`buildAndCollect`） | 需 composite build 包装 | 否 | 需自研 |
| 维护成本 | 中（一次性） | **高**（长期双份） | 高（每次改动两次 merge） | 高（自研工具要自己修） |

**为什么排除 B**：83% 的文件只有 1–3 行差异，双份维护等于把 27k LOC 的近似重复代码长期留在仓库里；而"共享资源"并不能解决 Java 侧的重复。

**为什么排除 C**：用户要的是"同时生产"，分支模型天然做不到一条流水线出两个产物，且每次改动都要 merge 两次。

**为什么排除 D**：D 等于把 Stonecutter 的核心（条件语法、替换引擎、版本目录覆盖、IDE 集成）自己重写一遍，收益为零。

**为什么 A 的前提是 §0 那个版本问题**：如果 Stonecutter 能留在 Gradle 8.8，A 的风险就和 B 一样低，同时拿到 B 拿不到的单源。

---

## 4. 推荐架构：Stonecutter + ModDevGradle

### 4.1 工具链组合（已核实的出处）

| 节点 | Gradle 插件 | 支持的 MC | 出处 |
|---|---|---|---|
| `1.20.1-forge` | `net.neoforged.moddev.legacyforge` | **Forge 1.17 – 1.20.1** | ModDevGradle `LEGACY.md`，含完整的 1.20.1 示例 |
| `1.21.1-neoforge` | `net.neoforged.moddev` | NeoForge 1.20.5+ | ModDevGradle README；兼容性明确写着 **"compatible with Gradle 8.8"** |
| 多版本编排 | `dev.kikugie.stonecutter` **0.9.8** | 平台无关 | 插件页（2026-08-31 发布） |

**关键点：两个节点可以用同一套构建插件家族**，不需要混用 ForgeGradle + ModDevGradle。社区模板 `QiHuang02/StonecutterTemplate`（Fabric + Forge + NeoForge）已经证明了这个组合可行：它的 `build.forge.gradle.kts` 就是 `id("net.neoforged.moddev.legacyforge")`，并且把 `createMinecraftArtifacts` 挂在 `stonecutterGenerate` 之后。

> 这也是为什么 §0 说"若必须 Gradle 9，则 1.20.1 节点改用 LegacyForge"——因为 LegacyForge 是官方支持的替代品，而不是自造方案。

### 4.2 节点定义

`settings.gradle.kts`：

```kotlin
pluginManagement { repositories { gradlePluginPortal() } }

plugins { id("dev.kikugie.stonecutter") version "0.9.8" }

stonecutter {
    create(rootProject) {
        // 节点名 = versions/ 下的目录名；第二个参数是条件求值用的目标版本
        versions("1.20.1-forge", "1.21.1-neoforge")
        vcsVersion = "1.20.1-forge"   // 默认激活哪个节点
    }
}
```

- 节点名与目标版本分离是 Stonecutter 的一等能力（文档原文即举了 `1.21-fabric` / `1.21-neoforge` 的例子）。
- `src/` 里写代码；`versions/<节点>/` 放该节点独有的源码/资源与 `gradle.properties`。
- IDE 里用 `./gradlew "Set active project to 1.21.1-neoforge"` 切换 `src/` 的解析目标。

### 4.3 每个节点的构建脚本

`build.1.20.1-forge.gradle.kts`（要点）：

```kotlin
plugins { id("net.neoforged.moddev.legacyforge") }

legacyForge {
    version = "1.20.1-${property("deps.forge")}"
    validateAccessTransformers = true
    runs { client { client() }; server { server() }; data { data() } }
    mods { register(property("mod.id") as String) { sourceSet(sourceSets["main"]) } }
}
// SRG 重混淆由插件自动挂到 jar 上；reobfJar 的产物在 build/libs，未重混淆的在 build/devlibs
mixin { add(sourceSets.main.get(), "${property("mod.id")}.refmap.json") }
tasks.named("jar") { manifest.attributes["MixinConfigs"] = "${property("mod.id")}.mixins.json" }
```

`build.1.21.1-neoforge.gradle.kts`（要点）：

```kotlin
plugins { id("net.neoforged.moddev") }

neoForge {
    version = property("deps.neoforge") as String
    runs { register("client") { client() }; register("server") { server() }; register("data") { data() } }
    mods { register(property("mod.id") as String) { sourceSet(sourceSets["main"]) } }
}
// 1.21.1 有 [[mixins]] 支持，不再需要 manifest 属性（改为写在 neoforge.mods.toml 里）
```

**依赖声明的机械迁移**：`fg.deobf("g:a:v", { transitive = false })` → `modCompileOnly("g:a:v")` / `modRuntimeOnly(...)`。ModDevGradle 自动创建 `modImplementation` / `modRuntimeOnly` / `modCompileOnly` / `modApi` 等**重映射配置**（SRG → official）。注意官方文档明确写着 **这些配置不传递**——与本工程现在到处写的 `transitive = false` 语义一致，属于刚好合拍。

---

## 5. 分歧处理的三层机制

这是整个方案的核心。**不要把所有差异都写成条件注释**——那会把源码淹掉。按差异性质分三层：

### 第 1 层：声明式替换（源码零改动）——覆盖 251 个文件

Stonecutter 的 **Replacements**（跨文件的正则替换）与 **Swaps**（构建脚本变量注入）。规则按节点生效，例如只对 `1.21.1-neoforge` 应用：

| 目标 | 规则 | 规模 |
|---|---|---|
| Forge → NeoForge 包名 | `net\.minecraftforge\.` → `net.neoforged.`（配例外表：`bus.api` / `api.distmarker` / `neoforgespi`） | 32 个 import |
| 改类名 | 9 条逐条替换（`ForgeConfigSpec`→`ModConfigSpec`、`MinecraftForge`→`NeoForge`、`RegistryObject`→`DeferredHolder`、`ForgeRegistries`→`BuiltInRegistries`、`ForgeFlowingFluid`→`BaseFlowingFluid`、`IForgeMenuType`→`IMenuTypeExtension`、`LivingHurtEvent`→`LivingIncomingDamageEvent`、`RegisterGuiOverlaysEvent`→`RegisterGuiLayersEvent` …） | 9 |
| `ResourceLocation` 构造器 | `new ResourceLocation\(` → `ResourceLocation.fromNamespaceAndPath(` | 47 处 / 36 文件 |
| `BlockEntity` 存读档签名 | `saveAdditional(CompoundTag` → `saveAdditional(CompoundTag tag, HolderLookup.Provider registries`（load 同理） | 58 处 / 30 文件 |
| `pack.mcmeta` | `"pack_format": 15` → `34` | 1 |
| `mods.toml` | 文件改名 `neoforge.mods.toml`；`modId="forge"` → `"neoforge"`；`mandatory=true` → `type="required"`；加 `[[mixins]]` | 1 |
| 数据包目录 | 资源重定位：`recipes/`→`recipe/`、`loot_tables/`→`loot_table/`、`structures/`→`structure/`、`tags/items/`→`tags/item/`、`tags/blocks/`→`tags/block/` | 119 |
| 配方 JSON | `"result": { "item":` → `"result": { "id":` | 68 |

Stonecutter 的 reference 里 **Replacements** 与 **Custom File Formats**（定义某类文件如何被解析/转换）正是为这类需求准备的；资源侧的移动/转换在配套插件 fletching-table 的 **Resource Relocation** 里有对应能力。**这两项要在 S-1 spike 里实测**（§12），如果 Replacements 对 JSON/资源支持不足，退回"Gradle `Copy` + 一个小 Python 脚本"，本工程已有 `tools/*.py` 的传统。

### 第 2 层：内联条件注释——只覆盖 53 个结构性文件

Stonecutter 的 **Conditions**，语法形如：

```java
//? if neoforge {
import net.neoforged.neoforge.capabilities.Capabilities;
//?} else {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
 *//?}
```

这 53 个文件里真正需要**设计**（而非替换）的是 capability 相关的那一组（`LazyOptional` 23 / `Capability` 22 / `ForgeCapabilities` 17，去重后是同一批机器类）。其余（`TickEvent` 4、`DistExecutor` 6、主类构造器 1、GUI overlay 1、`openScreen` 1、网络 2）都是"换个写法"，不涉及设计。

### 第 3 层：版本独有文件——放 `versions/<节点>/src/`

凡是整段逻辑只对一个版本成立、或文件形态本身不同（`mods.toml` vs `neoforge.mods.toml`、capability 注册类、网络注册类），直接放节点目录，不进 `src/`。这样条件注释的密度还能再降。

---

## 6. 重构后的仓库结构

```
Technomancy/
├─ settings.gradle.kts                 Stonecutter：声明两个节点
├─ stonecutter.gradle.kts              共享配置：替换规则、条件常量、buildAndCollect
├─ build.gradle.kts                    共享构建逻辑（group/version/测试/jar 排除/资源展开）
├─ build.1.20.1-forge.gradle.kts       → legacyforge
├─ build.1.21.1-neoforge.gradle.kts    → moddev
├─ gradle.properties                   共享属性（mod_id/name/authors/license/junit…）
├─ src/
│   ├─ main/java/theflogat/technomancy/        ← 唯一源码树（251 文件零条件，53 文件带 //?）
│   ├─ main/resources/                          ← 规范形态（1.20.1 形状），1.21.1 由替换/重定位生成
│   └─ test/java/…                              JUnit
├─ versions/
│   ├─ 1.20.1-forge/
│   │   ├─ gradle.properties           deps.minecraft / deps.forge / tc4r.version …
│   │   ├─ deps.json                   （原 DEPENDENCIES.json）
│   │   ├─ local-repo/                 不可再分发（.gitignore，只留 README + deps.json）
│   │   └─ src/main/resources/META-INF/mods.toml
│   └─ 1.21.1-neoforge/
│       ├─ gradle.properties
│       ├─ deps.json
│       ├─ local-repo/
│       └─ src/main/resources/META-INF/neoforge.mods.toml
├─ tools/                              原样保留（client_probe.py 等），端口改为按节点区分
├─ docs/                               原样保留 + 新增本文与 MIGRATION
└─ .github/workflows/build.yml         改为按节点矩阵
```

要点：

- **`src/main/resources` 保持 1.20.1 形态**作为规范源。理由：1.20.1 是已发货、已被验证的形态，把它当基准可以让"1.20.1 产物不变"成为一条可断言的性质（S0 的门禁）。
- **`local-repo` 从仓库根移到节点目录**：两版本各一份，`verifyLocalDependencies` 改为读 `versions/<节点>/deps.json`，路径可参数化。
- **`versions/<节点>/gradle.properties`** 承载所有逐版本不同的值（MC、loader、TC4R、curios、terrablender、gtceu、jei…）。Stonecutter 的 structured properties（`stonecutter.properties.toml`）是这一层更现代的写法，可在 spike 里比较。

---

## 7. 依赖与 `local-repo` 改造

现状：`local-repo/` 只有一份，装的是 TC4R 1.20.1（`0.1.0-20721` 完整 jar + `0.1.0-20732` 的 jar/pom）。1.21.1 需要**另一份完全不同的布局**。

| | 1.20.1 | 1.21.1 |
|---|---|---|
| 坐标 | `dev.tc4port:thaumcraft-forge:0.1.0-20721/20732` | `dev.tc4port:thaumcraft-…:…-20736`（坐标形态待确认） |
| 运行时 jar | **有**（19.2 MB） | **无**（只有 `-api.jar` + `-sources.jar`） |
| maven 布局 | 有（`dev/tc4port/thaumcraft-forge/<v>/`） | **无**（zip 里是扁平 `tc/` `fm/` `tm/` `tt/`） |
| 内部类可用性 | 有 | 无（api jar 只含 `api.**` 与 `network.**`） |

**要做的事**：

1. 1.21.1 节点建一套 maven 布局的 `local-repo`，`deps.json` 记录 SHA-256。
2. `tools/bootstrap-local-deps.ps1` 扩展成按节点复制。
3. CI 的 `TC4R_ARCHIVE_URL` 拆成两个变量（`TC4R_ARCHIVE_URL` / `TC4R_ARCHIVE_URL_1211`），各自门控一个节点；1.21.1 节点在拿到运行时 jar 之前**始终 skipped**。
4. **§1.2 那个硬缺口必须先解决**：本工程有 16 个 import 落在 TC4R 内部类上（`registry.TCBlocks`、`block.entity.*`、`research.ResearchCatalog`…），api jar 里没有。没有运行时 jar，1.21.1 节点连编译都到不了。

> 顺带记一笔：1.20.1 的 TC4R 打包在 20721→20732 之间从"api+jar+sources"变成了"jar+pom"（工作区里 20732 只有两个文件），说明上游的打包形态本身在变。1.21.1 那边拿到运行时 jar 时也要先确认它是不是同一个形状。

---

## 8. 运行、测试与验证管线

### 8.1 运行配置映射

| 现在（ForgeGradle） | 目标 |
|---|---|
| `run-client` + `rosetta.remote.port=48791` + `--quickPlaySingleplayer` | 各节点的 `client` run，属性照搬；**端口按节点区分**（48791 / 48792），否则两个 DEV 客户端不能同时探针 |
| `run-server` + `--nogui` | 各节点的 `server` run |
| `run-data` + `--mod … --all --output src/generated/resources/` | 各节点的 `data` run；`src/generated/resources` 需按节点分目录，否则两版本互相覆盖 |
| `run-gametest` + `forge.enabledGameTestNamespaces` | **待验证**（见风险 R2） |
| `mixin.env.remapRefMap` / `refMapRemappingFile` | LegacyForge 下由 mixin AP + refmap 机制接管；1.21.1 侧不需要 refmap 重映射 |

### 8.2 验证装置的复用

- **JUnit 267 用例**：与加载器无关的部分（`CondenserBalance`、`WandCharge`、`EnergyUnits` 等）在两边都能跑；涉及 `ModList.get()`、`CapabilityManager` 的用例受平台影响（见既有记忆里的 class-init 陷阱）。
- **GameTest 30 个文件**：`@GameTestHolder` / `@PrefixGameTestTemplate` 的包名两版本不同（`net.minecraftforge.gametest` → `net.neoforged.neoforge.gametest`），第 1 层替换即可；**能不能跑起来**是 R2。
- **DEV 客户端 + Rosetta 桥**：这是本工程的主验证路径。两个节点各有一套 `run-client`，探针脚本加一个 `--port` 参数即可。
- **`tools/validate_technom_data.py`**：数据包校验器，与加载器无关，**建议扩展成同时校验两个版本的资源树**——它正好是第 1 层替换规则（目录改名、`result.id`）的回归测试。

### 8.3 产物

- 1.20.1：`reobfJar`（SRG）→ `build/libs`；dev jar 在 `build/devlibs`。
- 1.21.1：普通 `jar` → `build/libs`。
- `buildAndCollect` 把两个 jar 收进 `build/libs/<version>/`，CI 直接上传。

---

## 9. CI 改造

现有 `.github/workflows/build.yml` 的三个 job 基本可以保留形状，改为按节点矩阵：

| job | 改动 |
|---|---|
| `validate` | 不变（或扩展成校验两个资源树） |
| `build` | 矩阵 `[1.20.1-forge, 1.21.1-neoforge]`；每个节点用自己的门控变量；`./gradlew :<节点>:build` |
| `gametest` | 矩阵 × 现有的 `-PwithGtceu` / `-PwithBotania` 组合；1.21.1 节点在运行时 jar 到位前 skipped |

`forge-setup` 复合 action 改成接收节点名，解压到 `versions/<节点>/local-repo`，校验路径同步改。

**默认绿**的性质要保持：1.21.1 节点在 `TC4R_ARCHIVE_URL_1211` 未设置时显示 skipped 而不是 failed。

---

## 10. 分阶段执行计划

每一阶段都有明确的 go/no-go 门禁；**任一阶段不通过就停在那里**，不要带着未知继续往下走。

### S-1：三个丢弃式 spike（不改主仓库）

三个互不相关的实验，各自在一个一次性分支或临时目录里做：

| spike | 问题 | 通过判据 |
|---|---|---|
| **S-1a** | Stonecutter 0.9.8 能在 **Gradle 8.8** 上跑吗？ | 一个空项目 `stonecutter { create { versions("1.20.1-forge","1.21.1-neoforge") } }` 能在 8.8 上配置成功并产出两个空 jar。**这一条决定 1.20.1 节点是否必须换工具链。** |
| **S-1b** | ModDevGradle LegacyForge 能复现现在的 1.20.1 构建吗？ | 一个最小 Forge 1.20.1 工程（TC4R 依赖 + 1 个 mixin + GameTest 运行）能编译、能 `runClient`、能跑 GameTest。**这一条决定 R1/R2 的严重程度。** |
| **S-1c** | Stonecutter 的 Replacements 能作用到 JSON 与资源路径吗？ | 对一份 `pack.mcmeta` + 一个配方 JSON + 一次目录改名写出替换规则，并确认只在目标节点生效。 |

**S-1a 的两个分支结果**：

- **能跑 8.8** → 1.20.1 节点保留 ForgeGradle 6，两个节点用两套不同插件。**风险降到最低**，直接进 S0。
- **必须 Gradle 9** → 1.20.1 节点换 LegacyForge。此时以 S-1b 的结论为准：S-1b 过，进 S0；S-1b 不过，**回到方案 B**（两个独立构建）或把 1.21.1 单独做一个仓库。

### S-1 结果（2026-10-12 实测完成）

三个 spike 全部跑完，落点在 `H:\MinecraftMods\_spikes\`（`sc-gradle/`、`mdg-1201/`、`sc-replace/`），不进主仓库。

**S-1a — Stonecutter × Gradle 版本下限：走"必须 Gradle 9"分支。**

- Stonecutter **0.9.8 与 0.8.4 在 Gradle 8.8 上均报 `Stonecutter requires at least Gradle 9`**；**0.7.11 是最后一个能跑在 8.8 上的版本**（实测）。0.9.8 在 Gradle 9.4.1 上正常。
- 结论：**R3 成立，R1 被激活** —— 1.20.1 节点不能留在 ForgeGradle 6 + Gradle 8.8，必须换 LegacyForge（见 §0 的推荐路径）。若不接受这个迁移，退路是 Stonecutter 0.7.11 + Gradle 8.8（放弃 0.8+ 的 `handlers {}` 自定义文件格式，见 S-1c）。

**S-1b — ModDevGradle LegacyForge 复现 1.20.1 构建：核心通过。**

- `net.neoforged.moddev.legacyforge` **2.0.148** 在 **Gradle 9.4.1** 上：能解析 `1.20.1-47.4.23`、能用 `modCompileOnly("dev.tc4port:thaumcraft-forge:0.1.0-20721") { transitive = false }` 对着本地 TC4R 编译、`jar` 产出 `build/libs/*.jar`（经 `reobfJar`）+ `build/devlibs/*.jar`。
- `gameTestServer` 是合法 run 类型，MDG 能注册（`type = 'gameTestServer'` + `systemProperty 'forge.enabledGameTestNamespaces', ...`）。**R2 降级**（可注册 ≠ 已跑通，实际 GameTest 执行仍待 S4）。
- 未验证：`runClient` 真正起来、以及真实 GameTest 用例执行。S0 的门禁里要补这两项。

**S-1c — Replacements 对 JSON/资源的覆盖：通过，但有一组必须记住的 API 陷阱。**

| 陷阱 | 事实 | 后果 |
|---|---|---|
| **声明位置** | 替换必须写在 `stonecutter.gradle.kts` 的 `stonecutter parameters { replacements { ... } }` 里。写在 `build.gradle.kts` 的 `stonecutter { replacements { ... } }` **会被静默忽略**（不报错、不生效） | 极易浪费整段时间排查"为什么替换没作用" |
| **regex 必须给反向** | `regex { replace(from, to) }` 两参形式抛 `Cannot query the value of property 'reversePattern'`。必须用四参 `replace(from, to, reverseFrom, reverseTo)` 或 `Pair` 重载 | 规则表要成对写 |
| **string 的语义** | `string(方向条件) { replace(from, to) }` 首参是**方向**（bidirectional）；两参形式即可用 | 反向由方向位自动处理 |
| **资源默认不处理** | 默认被处理的扩展名只有 `java/scala/json5/fsh/vsh/kt/kts/yml/yaml/aw/accesswidener/cfg`。**纯 `json`、`mcmeta`、`toml`、`properties`、`xml` 都不在其中** | 配方/`pack.mcmeta`/`mods.toml` 的替换默认全不生效 |
| **启用资源的正解** | `stonecutter handlers { inherit("json5", "json"); inherit("json5", "mcmeta") }`（`handlers {}` 需 0.8+） | 注册后替换即覆盖到资源 |
| **不要用 `filters` 加文件** | 官方明确：`filters` **只能 exclude，不能 include**（include 会在构建期报错，且有未定义副作用） | 别走这条死路 |

- 实测对照（同一份源，两个节点各生成一次）：

  | 源 | 1.20.1-forge（对照） | 1.21.1-neoforge |
  |---|---|---|
  | `Sample.java` | `net.minecraftforge.common.MinecraftForge` / `new ResourceLocation(` | `net.neoforged.neoforge.common.NeoForge` / `ResourceLocation.fromNamespaceAndPath(` |
  | `pack.mcmeta` | `"pack_format": 15` | `"pack_format": 34` |
  | `widget.json` | `"result": { "item": ...` | `"result": { "id": ...` |

- 结论：**第 1 层（声明式替换）能覆盖源码与资源**，前提是先把要处理的资源扩展名注册成 handler。原方案 §5 里"若 Replacements 对 JSON 支持不足就退回 Copy+Python"的退路**不需要启用**。
- 附注：`.json` 走 `json5` 配置意味着 `//` 会被当注释；**条件注释**（`//? if`）在 JSON 里会破坏语法（false 分支会被注释成非法 JSON），所以 JSON 侧**只用替换、不用条件**；需要整文件分叉的配方一律进 `versions/<节点>/src/main/resources`（第 3 层）。


### S0：仓库重构，只保留 1.20.1 节点

- 引入 Stonecutter，声明**两个**节点，但只让 `1.20.1-forge` 可用（另一个先空着）。
- 移动 `local-repo` / `DEPENDENCIES.json` 到 `versions/1.20.1-forge/`，参数化 `verifyLocalDependencies`。
- `src/` 内容**一个字节都不改**（此时没有条件注释）。
- **门禁**：`./gradlew :1.20.1-forge:build` 产出的 jar 与当前 `main` 产出的 jar **内容一致**（允许 manifest 里的时间戳/工具链版本差异，其余逐项比对）；JUnit 全绿；DEV 客户端能起、探针能通；GameTest 全绿。

### S0 结果（2026-10-12 实测完成，全部门禁通过）

实现落在**独立 worktree + `multiversion` 分支**上（`H:\MinecraftMods\Technomancy-1.20.1-wt\multiversion`），`main` 未动、工作区里未提交的 TC4R 20721→20732 WIP 也未触碰。改动文件：`settings.gradle`、`stonecutter.gradle`（新）、`build.gradle`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties`（→ 9.4.1）、`versions/<节点>/gradle.properties`（新）。**`src/` 一个字节都没改**（`git status src/` 为空）。

**与 §6 设计的两处偏离（都是有意的）：**

- `local-repo/` **留在仓库根**，没有搬进 `versions/1.20.1-forge/`。理由：它由共享脚本经 `tc4Repository` 从 `rootProject` 解析，且 TC4R 工件在两个节点间是同一份；搬进节点目录反而要每个节点各放一份。
- 控制器脚本用 **Groovy**（`stonecutter.gradle`），不是 §4 里写的 `.kts` —— 本工程本来就是 Groovy DSL，Stonecutter 的 Groovy 控制器可用（`hard_mode=true` 只压掉"有限支持"提示）。

**换到 MDG 后必须补的 5 处（都是"不报错但行为错"的坑，值得记住）：**

| # | 现象 | 根因 | 修法 |
|---|---|---|---|
| 1 | jar 里缺 `META-INF/LICENSE`、`META-INF/NOTICE` | `from(['LICENSE','NOTICE'])` 相对路径按**节点目录**解析，那里没有这两个文件 | `from([rootProject.file('LICENSE'), rootProject.file('NOTICE')])` |
| 2 | `compileTestJava` 报 100 个"程序包 com.google.gson 不存在"/"变量 Direction" | LegacyForge **没有** `unitTest {}`（那是 NeoForge 独有），`test` 源集只继承 `implementation`（重映射后的 mod jar），拿不到 Minecraft/Forge | `legacyForge { addModdingDependenciesTo sourceSets.test }` |
| 3 | `LangGuardTest` 2 个用例失败："`src\main\java\...` is not there" | 该测试按**工作目录**相对路径读源码；MDG 下工作目录是节点目录，源码在仓库根 | `tasks.named('test', Test) { workingDir = rootProject.projectDir }` |
| 4 | GameTest 启动即崩：TerraBlender 的 `@Inject` 找不到 SRG 目标 `m_258002_` | 单版本脚本手写的 `mixin.env.refMapRemappingFile=${buildDir}/createSrgToMcp/output.srg` 在 MDG 下不存在，**覆盖掉了 MDG 自己设的正确值**，于是 SRG refmap 不再重映射 | **删掉**这两行 —— MDG 的 `LegacyForgeModDevPlugin` 会自动设 `mixin.env.remapRefMap=true` 与指向自己 `moddev/artifacts/intermediateToNamed.srg` 的 `refMapRemappingFile` |
| 5 | manifest 的 `Implementation-Title` 从 `Technomancy-1.20.1` 变成 `1.20.1-forge` | `project.name` 在 Stonecutter 下是**节点名** | 改成 `"${rootProject.name}-${sc.current.version}"`，两个节点各自正确 |

**门禁实测（对照基线是"干净 checkout 的 FG6 构建"，不是旧的 `build/libs`）：**

| 门禁 | 结果 |
|---|---|
| **jar 内容一致** | **862/863 项 SHA-256 完全相同**；唯一差异是 `META-INF/MANIFEST.MF` 多出 4 个 Stonecutter 溯源属性（`Stonecutter-Plugin-Version` / `-Gradle-Version` / `-Node-Project` / `-Node-Version`）。类文件、资源、`mods.toml`、`pack.mcmeta`、mixin 配置全部逐字节一致 |
| **JUnit** | **267/267 全绿** |
| **GameTest** | **107/107 全绿**（`All 107 required tests passed`） |
| **runClient** | **启动到主菜单**（LWJGL 3.3.1 / Forge 47.4.23 / Thaumcraft 4R 初始化 / 音效引擎 / 各贴图图集 / Patchouli 271 jsons / JEI 图集，无 mixin 报错） |

**方法论提醒（第一次比对时踩过）：** 直接拿旧 `build/libs/technom-1.20.1-0.1.0-dev.jar`（10-09 从**脏工作区**构建）当基线，会得到 24 个"差异"，全部是假阳性 —— 主仓库工作区里 `*.json` 是 `i/lf w/crlf`（`.gitattributes` 要求 LF），加上基线早于后续提交（Forge 47.4.13→47.4.23、TC4R 20721→20732）和未提交的 WIP 类文件。**正确做法是另开一个干净 worktree、用旧 ForgeGradle 脚本重编一次基线**（`Technomancy-1.20.1-wt/fg6-baseline`），两个构建都来自 HEAD 的 LF checkout，剩下的差异才是真正的工具链差异。

### S1：接入 1.21.1 节点骨架

- 写 `build.1.21.1-neoforge.gradle.kts`，接上 TC4R 1.21.1 的 `local-repo`（**前提：运行时 jar 已到位**）。
- 此时 `src/` 仍是 1.20.1 形状，1.21.1 节点**必然编译失败**——这是预期的。
- **门禁**：`./gradlew buildAndCollect` 能产出两个 jar（1.21.1 那个可以是失败的构建，但管线结构成立）。

### S2：机械层（第 1 层替换）

- 写替换规则表，跑 1.21.1 节点的编译，把"只剩 53 个结构性文件报错"作为完成判据。
- 同步写资源重定位与配方 JSON 替换。
- **门禁**：1.21.1 的编译错误集合 ⊆ 那 53 个文件；1.20.1 节点**零回归**（jar 仍与 S0 一致）。

### S3：结构层（第 2/3 层）

- capability 重设计（最大的一块）、TickEvent、DistExecutor、主类构造器、网络重写、GUI overlay、`openScreen`。
- **门禁**：两个节点都 `compileJava` 通过。

### S4：运行时与 CI

- 数据包加载、GameTest、DEV 客户端探针在 1.21.1 上跑通。
- CI 改矩阵；文档（`CLAUDE.md` / `README.md` / `docs/*`）同步。
- **门禁**：两个节点在 CI 上都绿（或在依赖未就位时正确 skipped）。

---

## 11. 风险清单

按"可能性 × 影响"排序。

| # | 风险 | 可能性 | 影响 | 缓解 |
|---|---|---|---|---|
| **R1** | 1.20.1 节点从 ForgeGradle 6 换到 LegacyForge，构建行为出现差异 | 中 | **高**（动到已发货产物） | **已化解**（S0 实测）：862/863 项逐字节一致，唯一差异是 manifest 里 4 个 Stonecutter 溯源属性；JUnit 267/267、GameTest 107/107、`runClient` 到主菜单全部通过。详见 §10 S0 结果 |
| **R2** | ModDevGradle 下 **GameTest 运行类型不可用** | 低 | 中（丢 30 个 GameTest） | **已解除**：S0 实跑 `runGameTestServer`，**107/107 全绿** |
| **R3** | Stonecutter 0.9.8 的 Gradle 下限高于 8.8 | — | 中（触发 R1） | **已确认为真**：0.9.8/0.8.4 需 Gradle 9，0.7.11 是 8.8 的最后可用版本。已接受"上 Gradle 9 + LegacyForge" |
| **R4** | 1.21.1 的 TC4R **运行时 jar 缺失** | — | — | **已解除**：`1.21.1-neoforge-20736.zip` 含 4 个运行时 jar（`thaumcraft-neoforge-4.2.3.5-1.21.1-port.0.1.0-20736.jar`，18.5 MB），16 个内部类齐全，且为 **official mappings（0 个 SRG 名）**，无需重映射 |
| **R5** | `fg.deobf` → `mod*` 重映射配置的语义差异（**非传递**） | 低 | 中 | 现有代码已大量 `transitive = false`，语义吻合；逐个依赖核对 |
| **R6** | Stonecutter 默认 Kotlin DSL，本工程是 Groovy DSL | 低 | 低 | 文档明确支持 `kotlinController = false` + `centralScript = "build.gradle"` |
| **R7** | mixin refmap 在两个工具链下行为不同 | 低 | 中 | 本工程只有 1 个 mixin，且是 GTCEu/JEI 兼容 shim，1.21.1 侧大概率整体作废 |
| **R8** | 两个节点的 `src/generated/resources` 互相覆盖 | 中 | 低 | 按节点分目录 |
| **R9** | 两个 DEV 客户端端口冲突 | 高 | 低 | 探针端口按节点区分（48791 / 48792） |
| **R10** | 条件注释密度失控，源码可读性下降 | 中 | 中 | 严守三层机制：能替换的不写条件；整段不同的进 `versions/<节点>/src` |
| **R11** | 替换规则写在 `build.gradle.kts` 里被**静默忽略**；资源扩展名未注册 handler 导致替换"看着写了却不生效" | **高**（已踩） | 中（排查成本高） | **已固化**：替换一律写在 `stonecutter.gradle.kts` 的 `parameters { replacements {} }`；`json`/`mcmeta`/`toml` 等必须 `handlers { inherit(...) }` 注册。见 §10 S-1c 陷阱表 |
| **R12** | 换到 MDG 后"不报错但行为错"的配置坑（相对路径按节点目录解析、test 源集缺 Minecraft、测试工作目录、手写 `mixin.env.*` 覆盖 MDG 默认值、`project.name` 变成节点名） | **高**（S0 已全部踩到） | 中 | **已固化**：5 处修法见 §10 S0 结果表；门禁里"jar 逐字节比对 + GameTest + runClient"正是用来兜住这类静默差异的 |

---

## 12. 需要先验证的事项（清单）

按顺序做，每一项都能独立否掉后续。**S-1 已把 1/2/4/6 钉死，状态如下：**

1. ~~**Stonecutter 0.9.8 × Gradle 8.8**~~ → **已答**：0.9.8/0.8.4 需 Gradle 9，0.7.11 是 8.8 最后可用版。R1 激活。
2. ~~**ModDevGradle LegacyForge × Forge 1.20.1 × TC4R**~~ → **已全过**（S0）：编译 + reobfJar + jar 逐字节比对 + `runClient` 到主菜单 + GameTest 107/107。
3. ~~**ModDevGradle 下的 GameTest 运行类型**~~ → **已全过**（S0 实跑 107/107）。
4. ~~**Stonecutter Replacements 对 JSON / 资源路径的作用范围**~~ → **已答**：默认不处理 `json`/`mcmeta`，需 `handlers { inherit("json5", ...) }` 注册；注册后替换生效且按节点门控。见 §10 S-1c。
5. **Stonecutter 的 `versions/<节点>/` 资源覆盖语义**（`src/main/resources` 与节点资源的优先级）→ **仍待验证**（S0 未用到节点级资源；S2/S3 分叉资源时再测）。
6. ~~**1.21.1 的 TC4R 运行时 jar 是否存在、什么形态**~~ → **已答**：存在（20736 运行时 jar，official mappings，16 内部类齐全）。
7. **ModDevGradle 的 `mod*` 配置对 TC4R 这种"本地 maven + 非传递"依赖的处理** → **已全过**（S0：`modImplementation` + `transitive = false` 编译并成功进 dev 运行时，TC4R mixin/refmap 正常）。
8. **`pack.mcmeta` 在 Stonecutter 下的展开方式** → **已答**：用 `handlers { inherit("json5", "mcmeta") }` + 一条 string 替换把 `pack_format` 从 15 换到 34，实测生效；不必再用 Gradle `expand`。**S0 现状**：1.20.1 节点仍用 Gradle `expand`（与单版本行为一致），到 S2 引入替换规则时再切。

---

## 13. 本次没有做的事

- **`main` 的构建脚本与源码一个字节都没改。** S0 的全部构建改动在 worktree `H:\MinecraftMods\Technomancy-1.20.1-wt\multiversion` + 分支 `multiversion` 上；`main` 的已发货构建脚本仍是 ForgeGradle 6。本文档（规划/记录）仍提交在 `main`，与之前两篇方案文档一致。
- **没有合并 `multiversion` 到 `main`。** 按 §14，要等 S1–S4 把两个节点都打通、CI 矩阵改造完，再决定合并时机。
- **`src/` 一个字节都没改**（S0 门禁之一）。
- 没有实现 1.21.1 节点（`build.gradle` 里 `else` 分支只打一行提示），没有写任何替换规则。
- 没有评估 `compat/` 下 5 个可选模块（GTCEu / Botania / Jade / KubeJS / JEI）在双版本下的取舍——它们各自要跟 1.21.1 的版本，建议整体后置。**已知阻塞**：Botania 目前没有 1.21.1 版本。
- 工作区里未提交的 TC4R 20721→20732 升级改动未触碰（S0 的基线仍是 20721）。

## 14. 下一步

**S0 已完成，全部门禁通过**（见 §10 S0 结果）。按 §10 进 **S1：接入 1.21.1 节点骨架** —— 在 `build.gradle` 的 `else` 分支里应用 `net.neoforged.moddev`（NeoForge 节点），接上 TC4R 1.21.1 的本地工件（20736 运行时 jar），声明 1.21.1 侧的依赖版本（NeoForge 21.1.259 / curios 9.5.1 / TerraBlender 4.1.0.8 / JEI 19.57.0.451 / Jade 15.10.6 等）。此时 `src/` 仍是 1.20.1 形状，1.21.1 节点**必然编译失败**——这是预期的，S1 的门禁只是"`buildAndCollect` 能产出两个 jar（1.21.1 那个可以是失败的构建，但管线结构成立）"。

S0 的直接后果是**把 1.20.1 节点的构建工具链从 ForgeGradle 6 换成了 ModDevGradle LegacyForge**（S-1a 判定必须 Gradle 9 带来的，R1）。这次迁移动到了已发货产物，因此放在**独立分支**上做、门禁不过就不合并；现在门禁已全过（R1 化解），可以安全推进 S1。
