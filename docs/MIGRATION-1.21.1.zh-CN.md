# 迁移可行性评估：Technomancy 1.20.1（Forge 47）→ 1.21.1（NeoForge 21.1）

核对日期：2026-10-12。本文是一次**只读评估**的记录：只统计迁移面、核对 API 是否存在，**没有**改动任何源码，也**没有**尝试编译或运行 1.21.1 目标。所有数字都可用第 1 节的命令复现。

---

## 0. 结论

**可行，且 TC4R 依赖侧几乎是零成本**；工作量集中在 Forge → NeoForge 的平台层，量级是"机械但面广"，不是"重新移植"。

| 维度 | 判断 | 依据 |
|---|---|---|
| TC4R（Thaumcraft 4 Reborn） | **基本无变化** | 本工程引用的 73 个 `dev.tc4port.*` 类，72 个在 1.21.1 源码里同名同签名存在；唯一缺失的 `worldgen.BiomeAuraProfile` 被 `worldgen.BiomeAuraValues` 取代，三个静态方法签名完全一致 |
| Forge → NeoForge | **机械，但触及 103/304 个文件** | 55 个 distinct `net.minecraftforge.*` import：32 个纯改包名，9 个改了类名，13 个被删除（需改写调用点），1 个同名但 API 全换 |
| 原版 1.20.1 → 1.21.1 | **机械，可脚本化** | 数据包目录单数化、配方 `result.item`→`result.id`、`ItemStack` NBT→DataComponents、`BlockEntity` 存读档签名、`ResourceLocation` 构造器 |
| 依赖可用性 | **有硬缺口** | 提供的 `1.21.1-neoforge-20736-dev.zip` **只有 `-api.jar` 与 `-sources.jar`，没有运行时 jar**；本工程依赖的 16 个 TC4R 内部类不在 api jar 里（见 §1.3） |
| 可选联动（GTCEu / Botania / Jade / KubeJS / JEI） | **需要逐个跟版本** | 这些模块都直接调对应 mod 的 API，各自要等 1.21.1 线（JEI 15.x → 19.x、GTCEu 7.x → 1.21.1 线、Botania 的 capability 类改名等） |

一句话：**TC4R 的 1.21.1 移植把最难的一环（魔法侧的 API 稳定性）解决了**，本工程要付的是"平台层机械改造 + 数据包格式"的代价。真正的阻塞项不是 API，而是 1.21.1 运行时依赖的分发形态。

---

## 1. 评估输入与可复现性

### 1.1 输入

| 输入 | 位置 | 备注 |
|---|---|---|
| 本工程 | `Technomancy-1.20.1` @ `main` `725d771` | 工作区另有未提交的 TC4R 20721→20732 升级改动，本次评估未触碰 |
| TC4R 1.21.1 分发包 | `D:\Downloads\1.21.1-neoforge-20736-dev.zip`（20,272,309 B，6 个 jar） | 解压到 `/tmp/tc4r1211` |
| TC4R 1.20.1 基线 | `local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20721/` | 含完整运行时 jar（19.2 MB / 6185 项）、api jar（376 项）、sources jar |
| NeoForge API 判据 | Gradle 缓存 `net.neoforged:neoforge:21.1.235` 的 **sources jar** 与 universal jar | 952 个 Java 文件，可逐类核对，不依赖记忆 |

### 1.2 复现命令

```bash
# 本工程的 TC4R import 清单
grep -rhoE '^import (static )?dev\.tc4port\.[A-Za-z0-9_.]+;' src/main/java src/test/java \
  | sed 's/^import \(static \)\?//; s/;$//' | sort -u

# 本工程的 Forge import 清单（distinct）
grep -rhoE '^import (static )?net\.minecraftforge\.[A-Za-z0-9_.]+;' src/main/java src/test/java \
  | sed 's/^import \(static \)\?//; s/;$//' | sort -u

# 逐个核对 1.21.1 是否存在（把 sources jar 解压成目录后）
while read -r fqcn; do p=$(echo "$fqcn" | tr '.' '/'); [ -f "$p.java" ] || echo "MISSING $fqcn"; done < /tmp/tc_imports.txt
```

### 1.3 两个必须先说的坑

1. **不能拿 api jar 判"类是否还在"。** 用 `-api.jar` 比对会把 16 个类误判为"1.21.1 已删除"——它们本来就**不在任何版本的 api jar 里**（1.20.1 的 api jar 同样没有 `registry.TCBlocks`）。判定必须用 **sources jar**。本文的所有 TC4R 结论都基于 sources jar。

2. **1.21.1 分发包缺运行时 jar。** 20736 包内容：

   | 文件 | 类型 |
   |---|---|
   | `tc/…-api.jar` | 356 个类，**只有 `dev.tc4port.thaumcraft.api.**` 与 `…network.**`**（manifest：`Platform-API-Usage: compileOnly`） |
   | `tc/…-sources.jar` | 1570 个 `.java` + 3672 个资源文件，**0 个 `.class`** |
   | `fm/…-sources.jar`、`tm/…-sources.jar`、`tt/…-api.jar`、`tt/…-sources.jar` | 同上 |

   对照 1.20.1 的 `local-repo`：那里除了 api/sources 还有 `thaumcraft-forge-0.1.0-20721.jar`（19.2 MB），**内部类在这个 jar 里**。本工程有 16 个 import 属于内部类（§1.4），因此**没有运行时 jar 就无法编译，更无法运行**。这是评估中唯一的硬阻塞，且是分发问题而非技术问题——需要拿到 1.21.1 的完整构建产物，或按 `local-repo` 的 maven 布局自建一份。

### 1.4 本工程对 TC4R 的耦合面

| 类别 | 数量 | 说明 |
|---|---|---|
| `dev.tc4port.thaumcraft.api.**` | 57 | 公开 API，1.21.1 全覆盖 |
| `dev.tc4port.thaumcraft.*`（内部） | 16 | `registry.TCBlocks`/`TCFluids`、`block.entity.*`（7 个）、`research.ResearchCatalog`/`EntryDefinition`/`PageDefinition`、`worldgen.TCBiomes`/`BiomeAuraProfile`、`block.TaintSpreadLogic`、`common.WandInteractionTarget`、`nativeimpl.mixin.FurnaceAccessor` |

---

## 2. TC4R 侧：1.21.1 的 API 漂移

### 2.1 逐类核对结果

对 73 个 import 在 1.21.1 sources jar（1570 个 `.java`）中查找：

- **72 个存在且路径不变。**
- **1 个缺失**：`dev.tc4port.thaumcraft.worldgen.BiomeAuraProfile`。

### 2.2 唯一的破坏性改动

| 1.20.1 | 1.21.1 | 差异 |
|---|---|---|
| `worldgen.BiomeAuraProfile` | `worldgen.BiomeAuraValues` | 类改名 |

三个静态方法**签名逐字相同**，调用点无需改语义：

| 方法 | 1.20.1 | 1.21.1 |
|---|---|---|
| `averageAura` | `int averageAura(Holder<Biome>)` | 同 |
| `randomAspect` | `@Nullable AspectId randomAspect(Holder<Biome>, RandomSource)` | 同 |
| `greatwoodChance` | `float greatwoodChance(Holder<Biome>)` | 同 |

本工程只有 **1 处调用**：`common/tiles/nodes/NodeFabricatorBlockEntity.java:501`（外加 1 行 import）。改一个标识符即可。

> 实现层换了后端：1.20.1 走 `WorldgenDefinitionCatalog` 快照 + `StableWeightedSelector`，1.21.1 走 NeoForge 原生 DataMap（`TCRegistryDataMaps.BIOME_AURA`）。对调用方不可见。

### 2.3 其余漂移（不影响本工程，但说明 1.21.1 TC4R 是"真移植"而非换壳）

1.21.1 新增：`GameplayActionApi`、`GameplayEventApi`、`WarpEventApi`、`EntityBalanceApi`、`ChampionNumbers`、`EssentiaStorageApi`、`ArcaneBoreApi`、`VisAmuletItem`、`BiomeAuraDefinition`、`NativeItemStateDeclarations`，以及一批客户端视觉类型（`BeamSettings`、`FocusProjectileVisual`、`LightningSettings`、`ScreenEffect`）。
1.21.1 移除：`NativeItemClientExtensions`、`NativeFluidClientExtensions`、`RenderTypeBlendPlatform`、`ItemStateKey`、`RegistryDataMapKey`（被原生 DataMap 取代）、`BlockHarvest`、`GameEventHooks`、`NativeEnchantments`、`OwnedItemStack`、`MessageCodec`、`PlayerMovementApi`、`RecipeInputView`、`ArmorWalkingAnimation`。

这些名字**一个都没出现在本工程的 import 列表里**——这正是"依赖侧零成本"的证据。

### 2.4 依赖声明（1.21.1）

从 20736 的 `META-INF/neoforge.mods.toml` 读出：

| 依赖 | 版本 | 必要性 |
|---|---|---|
| neoforge | `[21.1.248,)` | required |
| minecraft | `[1.21.1]` | required |
| curios | `[9.5.1,)` | required |
| terrablender | `[4.1.0.8,)` | required |
| jade | `[15.10.5,)` | optional（CLIENT） |
| craftingtweaks | `[21.1.1,)` | optional |

本工程 1.20.1 侧已声明 curios / terrablender，迁移时改成上表版本即可。

---

## 3. 平台侧：Forge 47 → NeoForge 21.1

### 3.1 总量

| 指标 | 值 |
|---|---|
| 主源码 | 268 个 Java / 32,804 行 |
| 测试 | 36 个 Java / 4,718 行 |
| 引用 `net.minecraftforge` 的文件 | **103 / 304** |
| distinct `net.minecraftforge.*` import | **55** |

### 3.2 分桶（55 个 import）

| 桶 | 数量 | 含义 |
|---|---|---|
| A. 纯改包名 | **32** | 类名、方法签名都不变，`sed` 可做 |
| B. 改类名（语义改名） | **9** | 需要一起改调用点的名字 |
| C. 同名但 API 全换 | **1** | `PacketDistributor` |
| D. 已删除，需改写 | **13** | 调用点必须重写，不能只改 import |

### 3.3 A 桶：纯改包名（32）

`net.minecraftforge.` → `net.neoforged.`，其余路径基本不变；少数落到 `neoforged.bus` / `neoforged.api` / `neoforged.neoforgespi`。

| Forge | NeoForge |
|---|---|
| `api.distmarker.Dist` | `net.neoforged.api.distmarker.Dist` |
| `eventbus.api.IEventBus` / `SubscribeEvent` / `EventPriority` | `net.neoforged.bus.api.*` |
| `fml.common.Mod` / `fml.ModList` / `fml.config.ModConfig` | `net.neoforged.fml.*` |
| `fml.event.lifecycle.FMLClientSetupEvent` / `FMLCommonSetupEvent` | `net.neoforged.fml.event.lifecycle.*` |
| `forgespi.language.IModFileInfo` | `net.neoforged.neoforgespi.language.IModFileInfo` |
| `registries.DeferredRegister` | `net.neoforged.neoforge.registries.DeferredRegister` |
| `items.IItemHandler` / `ItemStackHandler` / `SlotItemHandler` / `ItemHandlerHelper` | `net.neoforged.neoforge.items.*` |
| `energy.IEnergyStorage` | `net.neoforged.neoforge.energy.IEnergyStorage` |
| `fluids.FluidStack` / `FluidType` / `capability.IFluidHandler` / `capability.templates.FluidTank` / `capability.wrappers.FluidBucketWrapper` | `net.neoforged.neoforge.fluids.*` |
| `client.extensions.common.IClientItemExtensions` / `IClientFluidTypeExtensions` | `net.neoforged.neoforge.client.extensions.common.*` |
| `client.event.RegisterColorHandlersEvent` / `EntityRenderersEvent` / `RenderLevelStageEvent` | `net.neoforged.neoforge.client.event.*` |
| `event.AddReloadListenerEvent` / `event.entity.EntityJoinLevelEvent` / `event.entity.living.LivingDeathEvent` / `event.entity.player.PlayerEvent` / `event.level.BlockEvent` | `net.neoforged.neoforge.event.*` |
| `common.SoundActions` | `net.neoforged.neoforge.common.SoundActions` |
| `gametest.GameTestHolder` / `PrefixGameTestTemplate` | `net.neoforged.neoforge.gametest.*` |

**已验证**：以上目标路径在 `neoforge-21.1.235-sources.jar` 中逐个存在。

**已确认无额外工作量的两个点**：

- `IClientItemExtensions` 的 18 个文件走的是 `Item#initializeClient(Consumer)`。该方法在 NeoForge 21.1 **仍然存在**（`ClientExtensionsManager` 在初始化时仍然 `BuiltInRegistries.ITEM.forEach(item -> item.initializeClient(...))`），所以这 18 个文件**只需改 import**。NeoForge 另提供的 `RegisterClientExtensionsEvent#registerItem(...)` 是可选的替代写法，不必强改。
- `BlockEntityType.Builder.of(...).build(null)` 在 1.21.1 **未变**（TC4R 20736 自己也这么写），10 处注册点不用动。

### 3.4 B 桶：改类名（9）

| Forge | NeoForge | 影响文件 |
|---|---|---|
| `common.MinecraftForge` | `neoforged.neoforge.common.NeoForge` | 5 |
| `common.ForgeConfigSpec` | `neoforged.neoforge.common.ModConfigSpec` | 1 |
| `common.extensions.IForgeMenuType` | `neoforged.neoforge.common.extensions.IMenuTypeExtension` | 1 |
| `registries.RegistryObject` | `neoforged.neoforge.registries.DeferredHolder` | 10 |
| `registries.ForgeRegistries` | 原版注册表走 `net.minecraft.core.registries.BuiltInRegistries`；NeoForge 自有注册表走 `NeoForgeRegistries` | 2 |
| `fluids.ForgeFlowingFluid` | `neoforged.neoforge.fluids.BaseFlowingFluid` | 2 |
| `event.entity.living.LivingHurtEvent` | `neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent` | 3 |
| `client.event.RegisterGuiOverlaysEvent` | `neoforged.neoforge.client.event.RegisterGuiLayersEvent` | 1 |
| `network.NetworkEvent` | `neoforged.neoforge.network.handling.IPayloadContext` | 2 |

**注意 `LivingHurtEvent`**：不只是改名。1.21 里伤害事件被拆分/重新归类，`LivingIncomingDamageEvent` 只在伤害**即将结算**时触发，`setCanceled` 的语义与 1.20.1 的 `LivingHurtEvent` 不完全等价，要逐处核对（3 个文件）。这是 B 桶里唯一需要**语义复核**而非纯替换的项。

**`RegistryObject` → `DeferredHolder` 的泛型形态不同**：`DeferredHolder<R, T>` 是双泛型，`RegistryObject<T>` 是单泛型。10 个文件里的字段声明要补第一个类型参数（多数是 `<?>` 或具体注册表类型）。这是机械的，但 `sed` 做不了，需要编译错误驱动逐个修。

### 3.5 C 桶：同名但 API 全换（1）

`net.minecraftforge.network.PacketDistributor` → `net.neoforged.neoforge.network.PacketDistributor`，包名只差前缀，**但静态方法全换**：

| 1.20.1 | 1.21.1 |
|---|---|
| `PacketDistributor.PLAYER.with(() -> player)` | `sendToPlayer(player, payload)` |
| `PacketDistributor.NEAR.with(() -> new TargetPoint(...))` | `sendToPlayersNear(level, x, y, z, radius, dimension, payload)` |
| `PacketDistributor.ALL.noArg()` | `sendToAllPlayers(payload)` |
| — | `sendToPlayersInDimension` / `sendToPlayersTrackingEntity(AndSelf)` / `sendToPlayersTrackingChunk` / `sendToServer` |

本工程只有 1 个文件（`common/network/TechnomNetwork.java`）用，且只有 2 个发送点。

### 3.6 D 桶：已删除（13）——真正的工作量在这里

| Forge（已删） | NeoForge 替代 | 本工程受影响文件 |
|---|---|---|
| `common.util.LazyOptional` | **无**。capability 查询直接返回 `@Nullable T` | **23** |
| `common.capabilities.Capability` | `ItemCapability` / `BlockCapability` / `EntityCapability` | **22** |
| `common.capabilities.ForgeCapabilities` | `neoforged.neoforge.capabilities.Capabilities`（`EnergyStorage.BLOCK/ITEM`、`ItemHandler.BLOCK/ITEM`、`FluidHandler.BLOCK/ITEM`） | **20** |
| `common.capabilities.ICapabilityProvider` | 无（改为在 `RegisterCapabilitiesEvent` 里注册工厂） | 3 |
| `event.AttachCapabilitiesEvent` | 无（同上） | 2 |
| `event.TickEvent` | 拆成 `event.tick.PlayerTickEvent.Pre/Post`、`ServerTickEvent`、`LevelTickEvent`、`client.event.ClientTickEvent` | 4 |
| `fml.DistExecutor` | **已从 loader 移除**（全缓存搜索无 `net/neoforged/fml/DistExecutor.class`）。改用 `@EventBusSubscriber(value = Dist.CLIENT)` 或 `FMLEnvironment.dist` 判断 | 6 |
| `fml.javafmlmod.FMLJavaModLoadingContext` | **已移除**（`javafmlmod` 包只剩 `FMLModContainer` / `FMLJavaModLanguageProvider` / `AutomaticEventSubscriber`）。主类改为 `@Mod` 构造器注入 `IEventBus` / `ModContainer` / `Dist` | 1 |
| `client.gui.overlay.IGuiOverlay` | 原版 `net.minecraft.client.gui.LayeredDraw.Layer` | 1 |
| `network.simple.SimpleChannel` | 整套换：`RegisterPayloadHandlersEvent` + `PayloadRegistrar` + `CustomPacketPayload` + `StreamCodec` | 1 |
| `network.NetworkRegistry` | 同上 | 1 |
| `network.NetworkDirection` | 同上 | 1 |
| `network.NetworkHooks` | `ServerPlayer#openMenu(...)` | 1（`ProcessorBlock.java:61`） |

#### 3.6.1 capability 是最大的一块

本工程围绕 `LazyOptional` 建了自己的抽象层：

- `common/energy/EnergyProtocolExtension.java:17` — `<T> LazyOptional<T> getCapability(Capability<T>, @Nullable Direction)`
- `common/energy/MachineEnergy.java:79` — 该接口的实现，`getCapability` / `invalidateCaps` / 存读档全转发
- 各机器 BlockEntity 重写 `public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)`，模式高度统一：`LazyOptional<T> view = energy.getCapability(cap, side); return view.isPresent() ? view : super.getCapability(cap, side);`
- 邻居查询：`neighbour.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).orElse(null)`

NeoForge 的模型是**注册式**，不是**询问式**：

```java
// 在 mod bus 上注册（对应 1.20.1 的 AttachCapabilitiesEvent + ICapabilityProvider）
event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TechnomBlockEntities.X.get(),
        (be, side) -> be.energyView(side));
event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> ..., TechnomItems.Y.get());

// 查询侧（对应 LazyOptional 链）
IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side); // @Nullable
```

也就是说 `MachineEnergy` / `EnergyProtocolExtension` 这套抽象需要**重写成"注册 + 可空查询"两半**，而不是逐行替换。这是整个迁移里唯一需要重新设计（而非重写名字）的局部，估计波及 §3.6 中 23+22+20 的文件集合。

> 参考实现：TC4R 20736 的 `registry/TCCapabilities.java` 已经把 `MachineCapabilityRegistrar` 映射到 `RegisterCapabilitiesEvent#registerBlockEntity/registerItem`，可以直接当模板。

#### 3.6.2 TickEvent

4 个文件，全是 `if (event.phase != TickEvent.Phase.END) return;` 的写法 → 直接换成订阅 `.Post` 事件，判断条件整段删掉。纯机械。

#### 3.6.3 DistExecutor

6 个文件。主类 `Technomancy.java:79` 用它加载客户端初始化；两个网络包用它做客户端分发；`RitualTomeItem` 用它开客户端 UI。
NeoForge 里 `DistExecutor` 整个类不存在，标准做法是：

- 客户端初始化 → 拆出 `@EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)` 的类（本工程 `client/coils/CoilClient.java:25` 已经在用这个写法，可以照抄）；
- 网络包里的 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` → 在 `IPayloadContext#enqueueWork` 里判 `context.player().level().isClientSide` 或直接把逻辑放进 client-only 的 handler 注册。

### 3.7 主类构造器

`Technomancy.java:41` 已经是 `Technomancy(FMLJavaModLoadingContext context)`（注释里甚至写了 "`@Deprecated(forRemoval = true, since = "1.21.1")`"——作者当时就在为 1.21.1 铺路）。改成：

```java
public Technomancy(IEventBus modBus, ModContainer container) { ... }
```

`context.registerConfig(ModConfig.Type.COMMON, SPEC)` → `container.registerConfig(ModConfig.Type.COMMON, SPEC)`；`context.getModEventBus()` → 直接就是参数。**1 个文件，几行。**

---

## 4. 原版侧：1.20.1 → 1.21.1

| 项 | 1.20.1 现状 | 1.21.1 要求 | 规模 |
|---|---|---|---|
| `ResourceLocation` 构造器 | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` | **47 处 / 36 文件**（已有 5 个文件用了新写法，说明是渐进替换中的半成品） |
| `ItemStack` 数据 | `getOrCreateTag()` / `getTag()` / `setTag()` | `DataComponents`（`DataComponentType` + `get`/`set`） | **13 处 / 9 文件** |
| `BlockEntity` 存读档 | `saveAdditional(CompoundTag)` / `loadAdditional(CompoundTag)` | `saveAdditional(CompoundTag, HolderLookup.Provider)` / `loadAdditional(CompoundTag, HolderLookup.Provider)` | **58 个方法 / 30 文件** |
| `pack.mcmeta` | `"pack_format": 15` | `34` | 1 文件 |
| `mods.toml` | `src/main/resources/META-INF/mods.toml` | `neoforge.mods.toml`；`[[dependencies]]` 里 `modId="forge"` → `"neoforge"`；`mandatory=true` → `type="required"` | 1 文件 |
| mixin 注册 | manifest `MixinConfigs`（1.20.1 Forge 没有 `[[mixins]]`） | `neoforge.mods.toml` 的 `[[mixins]] config="technom.mixins.json"` | 1 文件 |
| 配方序列化 | `RecipeSerializer` 的 JSON 反序列化 | 1.20.5+ 的 codec/`MapCodec` + `RecipeInput` | 1 个自定义配方类型 |
| 菜单 | `MenuType` + `IForgeMenuType.create(...)` | `IMenuTypeExtension.create(...)` | 1 文件 |

**验证过的"其实没变"**：`BlockEntityType.Builder.build(null)` 未变（TC4R 20736 同写法）；方块/物品模型 JSON 的 `render_type` 未变；方块掉落表内容格式未变（`rolls: 1` 仍被 IntProvider 接受）。

**1 个 mixin**：`mixin/GtceuJeiFluidHelperMixin.java` 是给 GTCEu 7.5.3 + JEI 15.x 打的兼容补丁。1.21.1 侧 GTCEu 和 JEI 都是新版本，这个 shim 大概率**整体作废**（要么不需要，要么目标方法名全变）。属于"删掉重写"而非"改包名"。

---

## 5. 资源与数据包

### 5.1 数据包目录单数化（1.21 的破坏性改名）

用 TC4R 20736 sources jar 里的**实际资源树**作为判据（3672 个资源文件），1.21.1 的目录是单数：

| 本工程（1.20.1） | 文件数 | 1.21.1 目标 |
|---|---|---|
| `data/technom/recipes/` | 70 | `data/technom/recipe/` |
| `data/technom/loot_tables/` | 40 | `data/technom/loot_table/` |
| `data/technom/structures/` | 2 | `data/technom/structure/` |
| `data/technom/tags/items/` | 4 | `data/technom/tags/item/` |
| `data/minecraft/tags/blocks/` | 2 | `data/minecraft/tags/block/` |
| `data/thaumcraft/tags/items/` | 1 | `data/thaumcraft/tags/item/` |

TC4R 20736 自己的树里同时存在 `loot_table/`、`recipe/`、`structure/`、`tags/item/`、`tags/block/`、`dimension/`、`damage_type/`、`enchantment/`、`loot_modifiers/` —— 单数化是确定的。

**不受影响的**：
- `data/technom/tags/worldgen/biome/`（3 个文件）—— `tags/worldgen/biome` 与 `tags/worldgen/structure` 在 20736 里**同样是复数**，两个版本一致。
- `data/thaumcraft/data_maps/item/wand_rods.json` —— NeoForge 原生 DataMap 的目录名同样是 `data_maps/<registry>/`，与 1.20.1 TC4R 的模拟实现一致。
- `data/technom/thaumcraft/{focus_catalog,object_aspects,research}/` —— TC4R 自定义数据包注册表，20736 侧路径**完全相同**（`data/thaumcraft/thaumcraft/{focus_catalog,object_aspects,research}`）。
- `data/technom/technomancy/essentia_fuel/` —— 本工程自定义目录（`EssentiaFuelLoader.DIRECTORY`），MC 不校验，原样保留。

### 5.2 配方 `result` 字段

| | 1.20.1 | 1.21.1 |
|---|---|---|
| 写法 | `"result": { "item": "technom:biome_morpher", "count": 1 }` | `"result": { "id": "technom:advanced_alchemical_construct", "count": 4 }` |

**68 / 70 个配方**用了 `result.item`。`key` 里的 `{"item": ...}` / `{"tag": ...}` 不变。脚本可做（`json.load` → 改 key → dump）。

### 5.3 其它资源

- `pack.mcmeta`：`pack_format` 15 → 34（TC4R 20736 用的就是 34）。
- `assets/`：blockstates / models / lang 未发现格式破坏性变化。`assets/minecraft/atlases/blocks.json` 这类 NeoForge 侧写法不变。
- 本工程有 `data/technom/thaumcraft/research/*.json`（研究条目）—— 需按 20736 的 `ResearchCatalog` 反序列化器复核字段（1.21.1 的研究 JSON 里 `research` 字段在 TC4R 自带配方中写作不带命名空间的 `"ADVALCHEMYFURNACE"`，而本工程写作 `"technom:BIOMORPHER"`；这是**需要核对**的一项，不能默认兼容）。

---

## 6. 构建、运行与 CI

| 项 | 1.20.1 现状 | 1.21.1 目标 |
|---|---|---|
| 插件 | `net.minecraftforge.gradle` 6.0.54 | NeoGradle 或 ModDevGradle（缓存里已有 `moddev-gradle-2.0.141`） |
| 映射 | `official` / `1.20.1` | NeoForge 的 `parchment` 或官方 mapping |
| Java | 17 | 21（1.21.1 要求） |
| GameTest run | `property 'forge.enabledGameTestNamespaces'` | `property 'neoforge.enabledGameTestNamespaces'` |
| mixin refmap 属性 | `mixin.env.remapRefMap` / `mixin.env.refMapRemappingFile` | NeoForge 下名称与默认值不同，需重配 |
| `local-repo` + `verifyLocalDependencies` | 校验 `local-repo/DEPENDENCIES.json` 的 SHA-256 | 需要**重新生成一份 1.21.1 的 maven 布局与 DEPENDENCIES.json**（当前 zip 是扁平 6 jar，不是 maven 布局） |
| CI | `.github/workflows/build.yml`，`vars.TC4R_ARCHIVE_URL` 门控 | 逻辑可整体复用，只需改解压后的期望路径（现在是 `test -d local-repo/dev/tc4port`） |

**CI 的可复用性**：现有工作流的"依赖不可再分发 → 用仓库变量门控、默认绿（job skipped 而非 failed）"设计在 1.21.1 上仍然成立，`forge-setup` 复合 action 只需改解压目标与校验路径。

---

## 7. 风险与未知项

按严重度排序：

1. **【阻塞】1.21.1 运行时 jar 缺失。** 提供的 zip 只有 api + sources。本工程 16 个内部类 import 必须靠运行时 jar。**这是能不能开工的前提，不是工作量问题。**
2. **【高】capability 抽象层需要重设计。** `MachineEnergy` / `EnergyProtocolExtension` 的 `LazyOptional` 模型在 NeoForge 没有对应物，必须改成"注册 + 可空查询"。这是唯一需要设计决策的局部（其余都是机械替换）。
3. **【中】可选联动模块要逐个跟版本。** `compat/` 共 20 个文件：GTCEu 9（651 行）、Jade 2（273 行）、Botania 2（121 行）、KubeJS 2（126 行）、JEI 4（495 行）。其中：
   - **JEI** 从 15.x → 19.x（缓存里是 `jei-1.21.1-neoforge-19.21.1.312`），插件 API 有变动，495 行要复核；
   - **Botania** 的 `BotaniaForgeCapabilities.MANA_RECEIVER` 在 1.21.1 改名/换包，且能力查询模型同步变化；
   - **GTCEu** 需要 1.21.1 线，且 §4 那个 JEI shim mixin 大概率作废；
   - **Jade** 插件 API 在 1.21.1 有变动。
   这些**都可以先整模块摘掉**（本工程已用 `-PwithGtceu` / `-PwithBotania` 等开关隔离），先让本体过编译与 GameTest，再逐个补。
4. **【中】`LivingHurtEvent` → `LivingIncomingDamageEvent` 的语义差异。** 3 个文件，要确认取消时机是否仍符合原意。
5. **【低】`DeferredHolder` 双泛型。** 10 个文件，编译错误驱动即可。
6. **【低】研究 JSON 的 `research` 字段格式。** 需对 20736 的 `ResearchCatalog` 反序列化器实测一次。
7. **【未知】GameTest 基础设施。** 36 个测试文件、24 个 `@GameTestHolder`。包名改起来是机械的，但 NeoForge 的 GameTest 运行器与结构模板路径（`structures/` → `structure/`）要实测。本工程有 `project_dev_test_world_superflat` 那个超平坦世界的已知坑（地形只到 y=-64..-61），换版本后要重新确认。
8. **【未知】没有做过任何编译尝试。** 本文所有"存在性"结论来自源码 jar 的静态核对；**没有**验证过方法签名级别的兼容（例如某个类的某个方法在 1.21.1 是否改了参数）。

---

## 8. 阶段建议

按"先让它编译，再让它跑，再让它全绿"的顺序：

| 阶段 | 内容 | 交付判据 |
|---|---|---|
| **M0 依赖** | 拿到 1.21.1 的**运行时 jar**；按 maven 布局建 `local-repo`；生成 `DEPENDENCIES.json` | `verifyLocalDependencies` 通过 |
| **M1 工具链** | NeoGradle/ModDevGradle + Java 21 + `neoforge.mods.toml` + `pack.mcmeta` 34 | 空壳能 `runClient` 到标题界面 |
| **M2 平台层** | A 桶 32 项 `sed`；B 桶 9 项；C 桶 1 项；D 桶 13 项（capability 重设计 + TickEvent + DistExecutor + 网络重写 + 主类构造器） | `compileJava` 通过 |
| **M3 原版层** | `ResourceLocation` 47 处；`BlockEntity` 存读档 58 处；`ItemStack` NBT 13 处；配方 codec | `compileJava` + `processResources` 通过 |
| **M4 数据包** | 目录单数化（119 个文件改名）；`result.item`→`result.id`（68 个文件）；研究 JSON 复核 | `/reload` 无错误，数据包加载日志干净 |
| **M5 运行时** | DEV 客户端 + Rosetta 桥（端口 48791）逐条走查；GameTest 迁移 | S0–S5 的既有验收项在 1.21.1 上复跑 |
| **M6 可选联动** | JEI → Botania → Jade → GTCEu → KubeJS，逐个按开关恢复 | 各开关组合下启动无崩溃 |

M2 是最大的一块（103 个文件里绝大多数），但**规则明确、错误可被编译器穷举**。M4 是纯脚本。M6 可以整体推迟，不阻塞主体。

---

## 9. 本次没有做的事

- 没有修改任何源码、构建脚本或资源文件。
- 没有尝试 `compileJava`、`runClient`、GameTest 或任何运行时验证——本机没有 1.21.1 的工程骨架，也没有 TC4R 1.21.1 的运行时 jar。
- 没有核对方法级签名（只核对了类级存在性）。
- 没有评估 1.21.1 与 1.20.1 在**行为**上的差异（例如 1.21 的配方书、附魔、战利品表内部结构），只在格式层面做了核对。
- 没有触碰工作区里未提交的 TC4R 20721→20732 升级改动（`gradle.properties`、`local-repo/DEPENDENCIES.json`、两个 jar 相关 BlockEntity）。
