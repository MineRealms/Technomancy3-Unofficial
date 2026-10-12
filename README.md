# Technomancy Unofficial — Forge 1.20.1

> Magic and science. What can go wrong?

**Technomancy 是 Thaumcraft 4 的科技向扩展。** 它把 TC4 的**源质（essentia）**和**灵气节点（vis）**接上工业能源：用源质和节点发电、用电力驱动注魔与炼金、把源质当流体在管道里搬运、把节点当电池充放，最后连祭坛的不稳定度、炼金炉的火候和群系本身都能用电来改。

上游版本用 RF（Thermal Expansion / CoFH）作能源。本工程把它换成 **Forge Energy**，并可选接 **GTCEu 的 EU** —— 所有机器同时提供两种能力，接哪条线都行。

这是该模组的**非官方 1.20.1 移植版**：基于官方 Forge MDK 重写实现，对接 **TC4R（Thaumcraft 4 Reborn）**。玩法基准是上游 `37bf9a5` 中实际注册的路径，贴图、模型、数值与行为都按上游对齐；上游处于注释状态、从未注册的内容（精华炮、火系 T3 仪式、精华重构器等）不在范围内。移植已完成，没有遗留的"待迁移"项——逐条状态见[功能矩阵](docs/FEATURE_MATRIX.zh-CN.md)，验收证据见[验证记录](docs/VALIDATION.zh-CN.md)。

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
| **Botania** | 选装。四台机器（花卉发电机、魔力制造器、植物净化器、魔力交换器）+ 魔力流体与桶 + Lexicon 词条；缺席时核心照常启动 |
| **JEI** | 源质燃料值类别 + 38 条用途页 |
| **Jade** | 15 个方块实体的提示行 |
| **KubeJS** | 暴露一个只读的 `Technom` 全局，供脚本查询 |

**明确不做**：Blood Magic；Thermal Expansion / CoFH RF 及其专属配方分支（由 FE / EU 取代）。

## 需求

| 项目 | 版本 |
|---|---|
| Minecraft / Java | 1.20.1 / 17 |
| Forge / ForgeGradle / Gradle | 47.4.23 / 6.0.54 / 8.8 |
| 模组 ID / Java 包 | `technom` / `theflogat.technomancy` |
| TC4R | `0.1.0-20721`，必需 |
| GTCEu | `7.5.3`，可选运行时 |
| Curios / TerraBlender | `5.14.1+1.20.1` / `1.20.1-3.0.1.10` |

版本事实以 `gradle.properties` 为准。运行时元数据严格限定当前 TC4R / GTCEu 核对版本，不宣称自动支持其他版本。

## 构建

需要 JDK 17。使用仓库自带 Gradle Wrapper，不需要另装 Gradle。

```powershell
Set-Location 'H:\MinecraftMods\Technomancy-1.20.1'
.\tools\bootstrap-local-deps.ps1
.\gradlew.bat build --console=plain --no-daemon
```

TC4R 是**用户本地提供的构建产物**，不随仓库分发，所以第一步必须先把它导入 `local-repo/`，方式见 [local-repo/README.md](local-repo/README.md)。其他依赖由 Gradle 下载。成品路径：`build/libs/technom-1.20.1-0.1.0-dev.jar`。

```powershell
# 开发运行时
.\gradlew.bat runClient
.\gradlew.bat runServer

# 同一工程加入 GTCEu；不改变编译 API 和发布功能集
.\gradlew.bat runClient -PwithGtceu=true

# IDE / 数据生成
.\gradlew.bat genIntellijRuns
.\gradlew.bat runData
```

代理配置放在本机的 Gradle 用户配置中，项目不保存本机 JDK 路径或代理端口。网络环境确实需要镜像时可以传 `-PmavenCentralMirror=https://maven.aliyun.com/repository/public`。

## 开发与测试

```powershell
.\gradlew.bat build --console=plain --no-daemon                                  # JUnit
.\gradlew.bat runGameTestServer --console=plain --no-daemon                      # GameTest，默认运行时
.\gradlew.bat runGameTestServer -PwithGtceu=true --console=plain --no-daemon
.\gradlew.bat runGameTestServer -PwithBotania=false --console=plain --no-daemon  # Botania 缺席安全
.\gradlew.bat runGameTestServer -PwithJade=false --console=plain --no-daemon     # Jade 缺席安全
python tools\validate_technom_data.py                                            # 数据包校验
```

`validate_technom_data.py` 读的是**上一次** `runGameTestServer` 写出的注册表清单，所以要在带 Botania 的那次运行之后跑，否则 Botania 配方里的物品会被误报成"未注册"。

**客户端实机探针**：服务端与 GameTest 看不到模型、blockstate 和贴图图集，这部分要拉真客户端跑。用 RosettaRemoteDebugBridge（桥端口 **48791**）：

```powershell
# 非阻塞地把 dev 客户端拉成独立进程（带 GTCEu，quick-join "New World"）
Start-Process -FilePath .\tools\start_client_detached.cmd -ArgumentList '"New World"','"true"' -WorkingDirectory $PWD

# 等日志出现 "Remote bridge listening on"，再对已运行的客户端跑探针
python .\tools\client_probe.py probes\client --attach    # PASS/FAIL，退出码 = 失败数
```

`tools/client_probe.py <探针> [--attach]`：每个探针是一段 Java 方法体，经桥在客户端线程执行并自带 PASS/FAIL；`--attach` 只跑探针，不开也不关客户端。

### 持续集成

`.github/workflows/build.yml` 在 push / PR 上跑三个 job：`validate`（数据包校验，不需要 TC4R）、`build`（JUnit + `reobfJar` + 成品 jar 检查）、`gametest`（三种运行时的 `runGameTestServer`）。

后两个 **需要仓库变量 `TC4R_ARCHIVE_URL`**（可选密钥 `TC4R_ARCHIVE_TOKEN`）：TC4R 不可再分发，托管 runner 取不到，未配置时它们显示为 **skipped 而不是失败**。归档内容必须与 `local-repo/DEPENDENCIES.json` 一致，`verifyLocalDependencies` 会逐个校验 SHA-256。

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
