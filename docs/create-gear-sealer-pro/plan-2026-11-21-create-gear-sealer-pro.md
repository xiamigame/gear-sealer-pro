<!-- LOCK id=L1 date=2026-11-21 plan=P1 -->
> 🔒 **L1 已锁定（2026-11-21）** —— 本段只读。确认内容：整体执行；允许 `danger-full-access` 跑 gradlew；**不**自动复制进整合包（用户自拷）；允许 `git init -b main` + 首次提交。

# 计划 P1 · Gear Sealer Pro（可自选密封词条 / 可指定替换目标）

- 日期：2026-11-21
- feature：`create-gear-sealer-pro`
- 会话：`session-8e06cba8-ece8-40f3-aae0-60014e5e4dd0`（label `2026-11-21-create-gear-sealer-pro`）
- 状态：**待确认（G1）**

## 目标

在 `D:\Project\gear-sealer-pro` 交付一个 Forge 1.18.2 模组 `gearsealerpro`，注册方块 `gearsealerpro:vault_sealer_pro`（显示名 `Gear Sealer Pro` / 中文「装备封印台 Pro」），行为为原版 Gear Sealer 的升级版：

1. 密封消耗与原版**逐项相同**（Vorpal Focus 数量、Vault Gold、Painite，全部直接调用 `VaultSealerCost.of(data)`）。
2. 封印时由玩家**选择要密封的词条**（从该装备可用的密封词条池中选），而不是随机抽。
3. 封印时由玩家**选择替换哪一条现有词条**，或者选择「新增词条」（等于原版 `ADD_VORPAL` 分支）。
4. 封印后装备同样被锁定为 corrupted（`VaultGearModifierHelper.setGearCorrupted`），语义与原版一致。
5. 外观与原版可区分（附魔感），且不复制 the_vault 的贴图/模型文件。

可验证：`gradlew build` 产出 `build/libs/gearsealerpro-1.18.2-0.1.0.jar`，jar 内 `mods.toml` 的 `modId=gearsealerpro`、`version=0.1.0`、依赖 `the_vault`；静态核对密封路径的成本与锁定调用与原版一致。

## 背景与约束

### 已确认的原版事实（反编译 `the_vault-1.18.2-3.21.6.6884.jar`）

- 「Gear Sealer」是**方块** `the_vault:vault_sealer`（中文「装备封印台」），不是物品。
- 槽位：Vault Gold（0）、Vorpal Focus（1）、Painite（2）、装备输入、产物输出（`VaultSealerContainer`）。
- 成本 `VaultSealerCost.of(data)`：`vorpalFocus` 按稀有度 1/2/3/4/5(Omega,Chaotic)/10(Unique,Special)，`vaultGold = 等级×6`，`painite = 等级×10`。
- 封印走 `ModGearModifications.CORRUPT_GEAR` → `CorruptGearModification.doModification`：
  - `ADD_VORPAL`（权重 1）：+1 词条槽并插入随机密封词条；
  - `REPLACE_WITH_VORPAL`（权重 9）：**随机**挑一条 `canBeModified()` 的前缀/后缀，**随机**抽一条密封词条替换；
  - 词条池 = `cfg.getModifierGroup(ModifierAffixTagGroup.CORRUPTED_IMPLICIT)`；为空时退回 generic 组（排除 `noLegendary` 标签）；
  - 结束后写 `CORRUPTED` 分类并 `setGearCorrupted` 锁定。
- 需要的类/方法均为 public：`VaultGearTierConfig.getConfig/getModifierGroup/getGenericGroupsFulfilling`、`ModifierTierGroup.getModifiersForLevel/getIdentifier`、`ModifierOutcome.makeModifier`、`VaultGearData.addModifierFirst/removeModifier/createOrReplaceAttributeValue/getModifiers`、`VaultGearModifier.getDisplay`、`VaultGearModifierHelper.setGearCorrupted`。

### 工程约束

- 目标环境：Wold's Vaults 0.34.1，MC **1.18.2**，Forge **40.3.11**，the_vault **3.21.6.6884**。
- 依赖方向：本 mod → the_vault，**仅 `compileOnly`**（运行期由整合包提供），不打包 the_vault 任何类或资源。
- 资源：方块/物品模型用 `"parent": "the_vault:block/modifier_workbench"`（运行时由 the_vault 提供），不复制其贴图。
- 构建：只走官方链路（Gradle 官方发行版、Forge 官方 maven、官方插件门户），不改 `distributionUrl`、不用本地镜像/离线缓存/仓库内 `GRADLE_USER_HOME`。
- 网络实测（2026-11-21）：`services.gradle.org`、`piston-meta.mojang.com`、`libraries.minecraft.net`、`repo.spongepowered.org`、`plugins.gradle.org` HTTPS 正常；`maven.minecraftforge.net` HTTPS **时通时断**（成功一次 28s，失败为 connect timeout）。构建可能因此失败，失败即按约束停下报告，不换源。
- 许可证：the_vault 为 `All rights reserved`。本 mod 仅本地自用、不对外分发、不反编译其代码到本仓库（只调用 public API）。
- 仓库现状：`D:\Project\gear-sealer-pro` 不是 git 仓库，只有 `AGENTS.md` 与本计划文档。

## 范围

1. 单工程 Forge 1.18.2 模组工程（`skill` §2：单 MC 版本、单 loader、小体量 → 不做 common/adapter 分层）。
2. 方块 + 方块实体 + 菜单 + 客户端界面 + 网络包 + 服务端密封逻辑。
3. 语言文件 `en_us` / `zh_cn`。
4. 合成配方：原版 `the_vault:vault_sealer` + `minecraft:netherite_ingot` → `gearsealerpro:vault_sealer_pro`。
5. 外观：紫色附魔感（见 P7，含退路）。
6. 工程文档：`README.md`（基线声明）、`RELEASING.md`、`.gitignore`。

## 非目标

- 不修改、不替换原版 `the_vault:vault_sealer` 的行为与配方。
- 不调整密封词条池、数值、权重、成本公式（除「谁被选中」由随机改为自选）。
- 不做多 MC 版本 / 多 loader 支持，不建 `common` + `adapter` 分层。
- 不提供服务器配置项、不改 datapack 配置。
- 不发布到 CurseForge/Modrinth，不推送远端，不做 CI。
- 不自动启动游戏客户端做运行验证（由用户手动进游戏验收）。

## 步骤

1. [ ] **P0 预检**：确认 JDK17 路径、Gradle 官方发行版可下载、官方 MDK zip 可下载
   `https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.3.11/forge-1.18.2-40.3.11-mdk.zip`。
   失败即停止并报告（URL、错误、退出码）。（回滚点：无写入）
2. [ ] **P1 脚手架**：解压 MDK 到仓库根；`gradle.properties` 改 `mod_id=gearsealerpro`、`mod_name=Gear Sealer Pro`、`mod_version=0.1.0`、`mod_group_id=com.gearsealerpro`、`mod_license=All Rights Reserved`、`mod_authors=...`；`build.gradle` 固定 `minecraft '1.18.2'`、`forge '40.3.11'`、`mappings official 1.18.2`、ForgeGradle `[5.1,5.2)`；Mod 主类改名并注册 `IEventBus` 构造入口。（回滚点：`git checkout`/删目录）
3. [ ] **P1b 构建验证**：`.\gradlew.bat build --no-daemon` 跑通空模板 → 基线声明写入 `README.md`。（回滚点：同上）
4. [ ] **P2 依赖接入**：把 `the_vault-1.18.2-3.21.6.6884.jar` 复制到 `libs/`（`.gitignore` 排除），`build.gradle` 加 `compileOnly files('libs/the_vault-...jar')`；写一个只引用 `VaultSealerCost` 的空类验证编译与 reobf 通过。（回滚点：移除依赖块）
5. [ ] **P3 方块与容器**：
   - `GearSealerProBlock`（水平朝向、`noOcclusion`、复用 `the_vault:block/modifier_workbench` 模型、`use` 打开菜单、破坏时掉落内容物）；
   - `GearSealerProTileEntity`（`OverSizedInventory` 4 槽 + `gearInput` + `output`，NBT 存取同原版字段名）；
   - `GearSealerProMenu extends OverSizedSlotContainer`（槽位坐标/过滤器照抄 `VaultSealerContainer`）；
   - 注册 `BLOCKS / ITEMS / BLOCK_ENTITIES / MENUS`。
   （回滚点：删除 4 个类与注册项）
6. [ ] **P4 密封逻辑（服务端，核心）**：
   - `SealOption(ResourceLocation id, AffixType type, Component display)`；
   - `SealerLogic.options(stack)` 复刻 `getVorpalOptions`（CORRUPTED_IMPLICIT 优先、generic 兜底、`ProficiencyStep.isModifierAllowed` 过滤）；
   - `SealerLogic.apply(menu, player, optionId, targetIndexOrAdd)`：校验（`isModifiable`、槽位、成本、选择合法）→ 扣 Vorpal Focus / Vault Gold（含背包金币兜底 `CoinDefinition`）/ Painite（逐条照抄 `VaultSealerRequestModificationMessage.apply`）→ 生成词条（`ModifierOutcome.makeModifier` + `addCategory(CORRUPTED)`）→ 替换指定词条或新增槽位 → `data.write` → `setGearCorrupted` → 写入产物槽。
   （回滚点：单文件，可整体回退）
7. [ ] **P5 网络**：`SimpleChannel`（`gearsealerpro:main`，协议版本 1）三个包：
   - `C→S RequestSealOptions`（打开界面 / 装备槽变化时）；
   - `S→C SyncSealOptions`（选项列表 + 当前装备可替换词条列表 + 状态、成本）；
   - `C→S CommitSeal`（选项 id + 目标索引或 ADD）。
   服务端对所有入参做二次校验（不信任客户端）。
   （回滚点：删除网络包与注册）
8. [ ] **P6 界面**：`GearSealerProScreen extends AbstractContainerScreen`，176×196 自绘面板：
   - 顶部：4 个材料槽 + 装备槽 + 产物槽；
   - 左栏「密封词条」滚动列表（`ObjectSelectionList`，显示词条 tooltip 风格名称）；
   - 右栏「替换目标」列表：固定首项「新增词条（+1 槽）」，其余为装备现有可改前缀/后缀；
   - 底部：成本行（金币 / painite / vorpal，红绿显示是否足够）+ `Seal` 按钮（条件不满足时禁用）。
   （回滚点：界面类独立）
9. [ ] **P7 外观**：先试真·附魔 glint（`IClientItemExtensions#getCustomRenderer` + 渲染一次带 foil 的副本）；**若 30 分钟内不通，退回**：自制半透明紫色覆盖层贴图 + 双层模型（不复制 the_vault 贴图）。
10. [ ] **P8 配方与本地化**：`data/gearsealerpro/recipes/vault_sealer_pro.json`（原版封印台 + 下界合金锭）；`assets/gearsealerpro/lang/{en_us,zh_cn}.json`；创造模式物品栏加入方式（若 the_vault 物品栏不可写则建独立 tab）。
11. [ ] **P9 构建与验收**：`.\gradlew.bat build --no-daemon` → 检查 jar（`unzip -l` 等价：列条目）、`mods.toml` 展开内容、lang/data/model 是否齐全；记录证据。
12. [ ] **P10 文档与提交**：`README.md` 基线声明、`RELEASING.md`、`.gitignore`；`git init -b main` + 首次提交（`feat: gear sealer pro 0.1.0`）；计划锁定 + 追加执行记录。

## 影响面

- 仅新增：`D:\Project\gear-sealer-pro` 仓库内容。
- 整合包：**不改动**；若用户同意，最后把 jar 复制到 `...\versions\Wold's Vaults - Vault Hunters Expansion\mods\`（写 E 盘，需沙箱批准，单列在确认点）。
- 网络：构建期从官方源下载 Gradle 发行版、Forge/MC 依赖（可能数百 MB）。

## 风险与回滚

| 风险 | 概率 | 应对 |
|---|---|---|
| `maven.minecraftforge.net` 抖动导致下载失败 | 高 | 停下，报告命令/URL/错误；不改源、不改 URL、不用镜像（skill §6）。重试由用户决定 |
| MDK/Gradle 与 JDK 版本不匹配 | 中 | 固定 JDK17 + 官方 MDK 自带 wrapper |
| the_vault API 与 3.21.6.6884 绑定 | 中 | `compileOnly` 固定该 jar；版本不符时明确报错 |
| GUI/密封逻辑无法自动验证 | 高 | 只做编译+静态核对，游戏内验证交用户；如实报告 |
| 引用 the_vault 模型导致资源缺失 | 低 | 模型走 `parent`，缺 the_vault 时方块显示紫黑格，可接受 |
| 沙箱写入限制（`~/.gradle`、E 盘） | 中 | Gradle 运行需 `danger-full-access`，逐次申请批准 |

回滚：整个仓库为新增内容；`git init` 后未提交前可直接删目录；提交后可 `git reset`/删分支。

## 验收标准

1. `.\gradlew.bat build --no-daemon` 退出码 0，产物 `build/libs/gearsealerpro-1.18.2-0.1.0.jar` 存在。
2. jar 内含：`META-INF/mods.toml`（`modId=gearsealerpro`、`version=0.1.0`、依赖 `the_vault`/`forge [40,)`/`minecraft [1.18.2]`）、`com/gearsealerpro/**` 类、`assets/gearsealerpro/lang/{en_us,zh_cn}.json`、`assets/gearsealerpro/blockstates|models`、`data/gearsealerpro/recipes/vault_sealer_pro.json`。
3. 静态核对：密封成本仅来自 `VaultSealerCost.of(data)`；密封后调用 `setGearCorrupted`；替换/新增两条路径都会消费材料。
4. `README.md` 含基线声明一行（MC/Forge/FG/Gradle/JDK/mappings + 验证命令与日期）。
5. 未做：游戏内行为验证（明确列为遗留，由用户验收）。

## 需要的确认点

1. **本计划整体确认**（G2）后开始写代码。
2. 是否允许在构建阶段以 `danger-full-access` 运行 `gradlew`（Gradle 需写 `~/.gradle` 缓存，沙箱只允许写工作区）。
3. 是否允许最后把 jar 复制进整合包 `mods/` 目录（写 E 盘，沙箱外）。
4. 是否允许 `git init -b main` 并做首次提交（remote 由你决定，未指示不添加）。

### A1 · 2026-11-21 · 提出人：agent

- **类型**：执行记录
- **关联**：L1（P0–P9 执行，P10 部分）
- **变更**：
  - 官方 MDK（`forge-1.18.2-40.3.11-mdk.zip`）落地为工程；`mod_id=gearsealerpro`、`mod_version=0.1.0`、`mod_group_id=com.gearsealerpro`。
  - 新增 16 个类：主类、方块、方块实体（4 材料槽 + 装备 + 产物，沿用 `OverSizedInventory`）、菜单、密封逻辑、3 个网络包、界面与客户端注册。
  - 资源：blockstate/物品模型（`parent` 指向 `the_vault:block/modifier_workbench`）、`en_us`/`zh_cn`、配方（原版封印台 + 下界合金锭）、README、RELEASING、.gitignore。
  - 证据：`.\gradlew.bat build --no-daemon` → `BUILD SUCCESSFUL`；产物 `build/libs/gearsealerpro-1.18.2-0.1.0.jar`（55 KB）。
  - jar 内条目已核对：`META-INF/mods.toml`、全部 `com/gearsealerpro/*.class`、`assets/gearsealerpro/{blockstates,models,lang}`、`data/gearsealerpro/recipes/vault_sealer_pro.json`、`pack.mcmeta`。
  - `mods.toml` 展开核对：`modId="gearsealerpro"`、`version="0.1.0"`、依赖 `forge [40,)`、`minecraft [1.18.2,1.19)`、`the_vault [3.21.6,)`（ordering AFTER）。
- **与计划的实现偏离**（均已在本文件正文未改动的前提下于代码中处理）：
  1. 1.18.2 没有 `Component.literal/translatable`，改用 `TextComponent`/`TranslatableComponent`。
  2. 1.18.2 的 `GuiComponent` 未暴露 `enableScissor`，列表改为按像素宽度截断文本 + 悬停 tooltip 显示完整词条。
  3. 1.18.2 的 `SimpleChannel` 没有 `sendTo(MSG, Player)` 重载，改用 `sendTo(msg, player.connection.connection, PLAY_TO_CLIENT)`。
  4. 产物名改为 `<mod_id>-<mc>-<version>.jar`（skill §3）。
  5. 构建期按用户明确指示走本机代理（`127.0.0.1:7890`，以进程环境变量传入，未写入仓库）；skill §6 的"不加 `-D` 代理参数"在此被用户指令覆盖。
- **未做**：
  - P7 附魔感外观（glint）：真 glint 需要自定义 BEWLR，而 `ItemRenderer` 内部会重复派发；自制覆盖层需要额外贴图并与 parent 模型叠加，风险大于收益。按需求"太麻烦就不需要"处理，保留为后续可选项。
  - 游戏内运行验证（开界面、选词条、封印、成本扣除、锁定）——agent 无法自动进游戏，列为遗留，由用户验收。
  - `git tag`（本次只做首次提交，未打 tag）。
- **影响**：仓库首次成型；整合包 `mods/` 未改动（按用户选择，jar 由用户自行复制）。
- **生效**：2026-11-21
- **状态**：ACTIVE

### A2 · 2026-11-21 · 提出人：user

- **类型**：修订（破坏性变更：Java 主包名）
- **关联**：L1 第 3 条（mod_group_id）与 A1
- **变更**：
  - 按 skill §1 第 4 项与 §1.1，主包名改由用户确认：`space.xiami.projects.gearsealerpro`
    （GroupId `space.xiami.projects` + mod `gearsealerpro`）。
  - `mod_group_id` 改为 `space.xiami.projects.gearsealerpro`（与主包名一致）；`mod_id` 保持 `gearsealerpro`。
  - 源码目录 `src/main/java/com/gearsealerpro/` → `src/main/java/space/xiami/projects/gearsealerpro/`，
    16 个类的 `package`/`import` 全部重写，无 `com.gearsealerpro` 残留。
- **证据**：
  - `.\gradlew.bat build --no-daemon` → `BUILD SUCCESSFUL`。
  - 产物 `build/libs/gearsealerpro-1.18.2-0.1.0.jar`：52 条目，`space/xiami/projects/gearsealerpro/*` 31 条，
    `com/gearsealerpro*` 0 条。
  - `gradle.properties`：`mod_group_id == 主包名`，`mod_id=gearsealerpro` 不变，`mods.toml` 的 modId 与资源命名空间不受影响。
- **影响**：包名是公开契约（import、AT、资源路径、Maven 坐标）；当前未发布、无外部消费者，风险为零。
- **生效**：2026-11-21
- **状态**：ACTIVE
