# Technomancy Unofficial — Forge 1.20.1 / NeoForge 1.21.1

> Magic and science. What can go wrong?

**Technomancy 是 Thaumcraft 4 的科技向扩展。** 它把 TC4 的**源质（essentia）**和**灵气节点（vis）**接上工业能源：用源质和节点发电、用电力驱动注魔与炼金、把源质当流体在管道里搬运、把节点当电池充放，最后连祭坛的不稳定度、炼金炉的火候和群系本身都能用电来改。

上游版本用 RF（Thermal Expansion / CoFH）作能源。本工程把它换成 **Forge Energy**，并可选接 **GTCEu 的 EU** —— 所有机器同时提供两种能力，接哪条线都行。

这是该模组的**非官方现代移植版**，对接 **TC4R（Thaumcraft 4 Reborn）**，同一套主要源码分别构建 **Minecraft 1.20.1 Forge** 和 **1.21.1 NeoForge**，不是一个 JAR 通用所有版本。玩法基准是上游 `37bf9a5` 中实际注册的路径；上游处于注释状态、从未注册的内容（精华炮、火系 T3 仪式、精华重构器等）不在范围内。1.20.1 的功能迁移已完成，1.21.1 的运行验证进度及限制见[多版本状态](docs/MULTIVERSION-STATUS.zh-CN.md)。Botania 联动当前仅在 1.20.1 提供。

## 特色

### 源质与能量互换

- **Essentia Dynamo（源质发电机）** —— 烧源质发电。燃料值表是数据驱动的（`data/technom/technomancy/essentia_fuel/`），可以用数据包改写。
- **Node Dynamo（节点发电机）** —— 直接从灵气节点抽 vis 发电，把节点当电池用。
- **Hippie Dynamo（花卉发电机）** —— 接 Botania 的魔力池，把 Mana 转成电。
- **Energy Condenser（能量凝聚器）** —— 耗电把原始要素聚合成复合要素。
- **Mana Fabricator（魔力制造器）** —— 反过来，用电产 Mana。
- **Mana Exchanger（魔力交换器）** —— Mana 与电双向互换，配套可放置的魔力流体与魔力桶。

### 源质物流

- **Quantum Jar（量子罐）** —— 640 点单要素，可贴标签锁定内容，带液面与标签渲染。
- **Quantum Reservoir（量子储库）** —— 256 点，六面双向。
- **Essentia Coil（源质线圈）** —— 无线源质传输；**Item Coil（物品线圈）** 与 **Coil Coupler（谐频器）** 把同一套连接模型用在物品搬运上。
- **Essentia Fusor（源质融合器）** —— 两进一出，按 TC4R 的组合规则合成要素。
- **Creative Jar（创造罐）** —— 创造模式专用，无限输出任意要素。

### 加工链

- **Ignis Incinerator** 与 **Mana Purifier** —— TC 与 Botania 两条源质净化线，共用一套多阶段纯矿加工链（铁 / 金 / 铜 × 6 阶段）。
- **Advanced Deconstruction Table（高级分解台）** —— 拆物品还原成要素，奖励记在放置者名下。
- **Eldritch Consumer（邪术吞噬器）** —— 吃生物、掉落物或方块换要素，带面板动画。

### 灵气节点

- **Node Fabricator（节点制造器）** —— 成对多方块：用精华加电力给已有节点充能，或者干脆**凭空造一个新节点**（按法杖右键启动仪式，类型与修正按上游规则级联）。
- **Fusion Focus（融合焦点）** —— 吸收、合并、搬运、再造节点，节点只会存在于世界或焦点其中之一，不会复制也不会丢失。

### 法杖与工具

- **Energized Wand Core（充能杖芯）** —— 用背包里的电给法杖补魔力。
- **Technoturges Scepter Core（Technoturge 权杖）** —— 三种杖帽组合，容量与折扣按上游。
- **Pen（书写笔）** 与 **Pen Core** —— 右键两张桌子组成研究桌。
- **Potency Gem（增幅宝石）** —— 装机后吞吐 ×4，效率不变。
- 材料线：**Neutronized Metal / Gear**、**Enchanted Coil**，以及 Botania 的 **Mana Coil**、**Manasteel Gear**。

### 仪式

**16 个仪式**全部到位：净化、流水、塌陷、黑洞各 T1–T3，火系 T1/T2，外加 **Extraction** 与 **Fountain of Existence**。配套五色 **Ritual Core（仪式核心）**、五色 **Crystal（水晶）**、**Basalt（玄武岩）** 与 **Ritual Tome（仪式手册）**（八页签、可翻页、能直接查配方）。

### Existence

一整套以"存在"为货币的自动化：**Fountain of Existence（存在喷泉）** 产能，**Existence Burner（存在燃烧器）** 烧生物换产能，**Existence Pylon（存在之塔）** 三阶传输与均衡，**Crop Accelerator / Harvester / Sealer（催熟 / 收割 / 封印）** 三变体消费。**Existence Gem（存在宝石）** 是这一整套的核心材料。玩家的亲和等级与 Existence 进度有独立的 HUD 与网络同步。

### 深层 TC4 玩法

- **Flux Lamp（注魔稳定灯）** —— 在真实注魔祭坛旁边降低不稳定度，副产淤泥。
- **Electric Bellows（电动风箱）** —— 吹奥术炼金炉或原版熔炉，用电买火候。
- **Biome Morpher（生态转换器）** —— 用电把已加载的群系改成魔法森林 / 阴森 / 污染之地。
- **Fake Light（假空气光源）** —— 仪式结构用的隐藏光源。

### 宝物

**Fire Gem**、**Power Plate**、**Golden Wing** 三件宝物，由稀有的宝物村民携带；带在身上有受击与摧毁副作用，也能用 Extraction 仪式取走。

### 联动

| 模组 | 关系 |
|---|---|
| **TC4R (Thaumcraft 4 Reborn)** | **必需**。玩法本体，源质 / 要素 / 节点 / 仪式 / 注魔 API 都来自它 |
| **GTCEu** | 选装运行时。九台耗能机器按各自耗电推导电压档位，三台发电机输出 EU；不装时全部退回 FE |
| **Botania** | **仅 1.20.1** 选装。四台机器（花卉发电机、魔力制造器、植物净化器、魔力交换器）与 Lexicon 词条；Botania 官方的 1.21.1 移植仍在 `1.21.1-porting` 分支开发、尚无正式发布，因此 1.21.1 暂不打包联动代码与配方，核心照常运行 |
| **JEI** | 源质燃料值类别 + 38 条用途页 |
| **Jade** | 15 个方块实体的提示行 |
| **KubeJS** | 暴露一个只读的 `Technom` 全局，供脚本查询 |

**明确不做**：Blood Magic；Thermal Expansion / CoFH RF 及其专属配方分支（由 FE / EU 取代）。

## 需求与构建目标

| 目标 | Minecraft / Java | Loader | TC4R | GTCEu（选装） |
|---|---|---|---|---|
| `1.20.1-forge` | 1.20.1 / 17 | Forge 47.4.23 | 20721 | 7.5.3 |
| `1.21.1-neoforge` | 1.21.1 / 21 | NeoForge 21.1.259 | 20736 | 7.0.2 |

模组 ID 为 `technom`。版本事实以 `versions/<目标>/gradle.properties` 为准，元数据限定核对过的版本，不宣称其它小版本、同版本另一加载器或跨版本世界降级兼容。

## 构建

用 **JDK 21 启动 Gradle**，并提供 JDK 17 toolchain 构建 Forge 目标。仓库自带 Gradle 9.4.1 Wrapper、Stonecutter 0.9.8 和 ModDevGradle 2.0.148，无需安装系统 Gradle。

TC4R 是用户私下提供的前置，不随仓库或 Technomancy JAR 分发。将两目标的运行时 JAR/POM 导入 `local-repo/`；旧目标还需要其清单列出的 API/sources。清单为 `local-repo/DEPENDENCIES.json` 与 `local-repo/DEPENDENCIES.1.21.1-neoforge.json`，构建会验证每个 SHA-256。旧版导入工具为 `tools/bootstrap-local-deps.ps1`，不要用它覆盖新版清单。

```powershell
# 一条命令：分别编译、JUnit、检查 JAR，再收集两份成品
.\gradlew.bat buildAndCollect --console=plain --no-daemon

# 也可只构建一个目标
.\gradlew.bat :1.20.1-forge:build --console=plain --no-daemon
.\gradlew.bat :1.21.1-neoforge:build --console=plain --no-daemon
```

成功后：

- `build/distributions/technom-1.20.1-forge-0.1.0-dev.jar`
- `build/distributions/technom-1.21.1-neoforge-0.1.0-dev.jar`

各节点原始发布产物在 `versions/<目标>/build/libs/`；Forge 发布 JAR 经过 `reobfJar`。`buildAndCollect` 只有两个目标都成功才更新收集目录；失败后其中可能仍有旧产物，不应把旧文件当成当次成功。

```powershell
# 开发运行：始终指定目标，避免意外启动两份实例
.\gradlew.bat :1.20.1-forge:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
.\gradlew.bat :1.21.1-neoforge:runServer
.\gradlew.bat :1.21.1-neoforge:runClient -PwithGtceu=true -PwithKubejs=true
```

运行目录按目标隔离；NeoForge 的目录带 `-1.21.1` 后缀。代理/JDK 绝对路径只放本机配置，不提交到工程。

## 开发与测试

```powershell
.\gradlew.bat :1.20.1-forge:runGameTestServer --console=plain --no-daemon
.\gradlew.bat :1.21.1-neoforge:runGameTestServer --console=plain --no-daemon
.\gradlew.bat :1.21.1-neoforge:runGameTestServer -PwithGtceu=true --console=plain --no-daemon
.\gradlew.bat :1.20.1-forge:runGameTestServer -PwithBotania=false --console=plain --no-daemon

# 检查实际目标资源；--require-inventory 要求已有同目标的 GameTest 注册表清单
python tools/validate_technom_data.py --node 1.20.1-forge --require-inventory
python tools/validate_technom_data.py --node 1.21.1-neoforge --require-inventory
```

Forge 数据检查应紧跟**带 Botania 的默认 GameTest**运行，以免误用无 Botania 的注册表清单。NeoForge 清单单独写入 `run-gametest-1.21.1/`。`-PwithGtceu`、`-PwithJade` 和 `-PwithKubejs` 只调整开发运行时；Botania 的 1.21.1 编译隔离与这些运行时开关不同。

### 客户端探针

`tools/client_probe.py` 复用 RosettaRemoteDebugBridge，必须部署**对应 MC/loader** 的 dev-mapped 桥，不能把 Forge 1.20.1 桥直接放进 NeoForge。自动化只操作隔离测试世界，不对玩家存档跑铺方块探针。

```powershell
python tools/client_probe.py probes/client --node 1.20.1-forge --attach
python tools/client_probe.py <适配目标的探针> --node 1.21.1-neoforge --attach
```

默认端口分别为 48791 / 48792，可用 `--port` 覆盖；启动运行时要传相同的 `-PclientProbePort`。已有探针含 Forge 专有引用，不因为脚本支持节点选择就宣称全部 NeoForge 探针已适配。实测状态见 VALIDATION / 多版本状态文档。

### 持续集成

`.github/workflows/build.yml` 包含规范源数据检查、两个节点的构建/JUnit/JAR 检查，以及分节点可选运行时 GameTest 矩阵。构建与 GameTest 需要 `TC4R_ARCHIVE_URL`（可选密钥 `TC4R_ARCHIVE_TOKEN`），私有归档必须包含两个目标各自校验清单所需的前置；没有配置时明确跳过，而不是宣称验证通过。CI 仅上传 Technomancy 产物和测试报告。

## 工程文档

- [总工程指导](docs/ENGINEERING_GUIDE.zh-CN.md)：架构、FE/EU 规则、TC4 API、数据持久化、里程碑与验收要求。
- [功能迁移矩阵](docs/FEATURE_MATRIX.zh-CN.md)：逐条功能的来源、阶段与状态。
- [验证记录](docs/VALIDATION.zh-CN.md)：实际执行过的检查、未验证边界、GameTest 批次与结果。
- [源码基线与取舍](docs/SOURCE_BASELINE.zh-CN.md)：原版 / 1.12 分支关系、可吸收改动和旧代码问题。
- [兼容与联动分析](docs/COMPAT.zh-CN.md)：原版 compat 清单、目标整合包环境、TC4R 版本差异、Thaumic Energistics 结论。
- [Botania 规格与对齐清单](docs/BOTANIA.zh-CN.md)：四台 Botania 机器的上游精确数值、已知偏差与未验证项。
- [Claude/agent 交接入口](CLAUDE.md)：当前进度、下一步、读文件顺序与不可改动方向。
- [官方 MDK 来源与校验](docs/MDK_PROVENANCE.json)。

## 来源与许可

新代码与文档采用 [Apache-2.0](LICENSE)；MDK 原始许可保存在 `LICENSES/`。

上游 Technomancy 由 Democretes、theflogat、Mordenkainen 及贡献者创作（<https://github.com/Mordenkainen/Technomancy>）；移植另参考了 Technomancy-2。旧代码、美术与第三方依赖的来源分别记录在 [NOTICE](NOTICE) 与源码基线文档中。TC4R 的 JAR/POM 由本地导入，既不提交 Git，也不打进本模组的成品。
