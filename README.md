# Gear Sealer Pro

Minecraft Forge 1.18.2 模组，为 **Wold's Vaults / Vault Hunters 3rd Edition** 增加一个
「Gear Sealer Pro / 装备封印台 Pro」方块。

原版 Gear Sealer 封印时是**随机**挑一条能被替换的词条、再从密封词条池里**随机**抽一条替换上去。
Gear Sealer Pro 把这两个随机换成玩家自选：

- 左栏选「密封词条」（该装备实际可用的密封词条池，前缀/后缀都可）；
- 右栏选「替换目标」（装备现有可改词条），或选「新增词条（+1 槽）」；
- 点「封印」执行。

**消耗与原版完全一致**：Vorpal Focus 数量、Vault Gold、Painite 全部来自 the_vault 的
`VaultSealerCost`；封印后装备同样被锁定为 corrupted、不可再修改。

## 兼容与基线

```text
MC 1.18.2 | Forge 40.3.11 | ForgeGradle [6.0,6.2) | Gradle 8.8 (wrapper) | JDK 17 | mappings official 1.18.2
包名 space.xiami.projects.gearsealerpro（GroupId space.xiami.projects + mod gearsealerpro）
形态 common + adapter（adapter/1_18_2 是唯一接触 MDK 的模块，产物为合并后的单 jar）
依赖：the_vault 3.21.6.6884（Wold's Vaults 0.34.1），compileOnly，运行期由整合包提供
验证：.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon 通过（2026-11-21）
```

## 仓库结构

| 目录 | 内容 |
|---|---|
| `common/` | 纯 JVM，零 Minecraft/Forge 依赖：`api`（密封请求值对象）、`core`（选择状态机、面板布局、消耗视图）+ JUnit5 单测 |
| `adapter/1_18_2/` | 唯一接触 Forge/MDK 的模块：方块、方块实体、菜单、网络、界面、密封逻辑、注册、`mods.toml`、lang、模型与配方 |
| `.github/workflows/release.yml` | tag `v*` 触发的发布流水线（self-hosted runner） |

## 构建

前置：把目标整合包里的 `the_vault-1.18.2-3.21.6.6884.jar` 复制到本仓库 `libs/`
（`libs/` 不入库，属于本地依赖）。

```powershell
.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon
```

产物：`adapter/1_18_2/build/libs/gearsealerpro-1.18.2-0.2.0.jar`，放进整合包 `mods/` 即可。
构建只使用官方 Gradle 发行版、官方 Forge maven 与官方插件门户，不使用镜像或离线缓存。

## 多人联机

服务端只下发「词条 id + 可占用的侧别」与材料/成本状态，**列表文案由客户端自己拼**：
The Vault 的 `getDisplay` 是 `@OnlyIn(CLIENT)`，专用服务器上会被剥掉，所以服务端不能碰它。
因此专用服务器、局域网、单人三种环境行为一致，客户端必须有同一个 mod（`mods.toml` 默认
`MATCH_VERSION`）。

## 获得方式

工作台合成：原版 `the_vault:vault_sealer` 放在中间，下方放一个 `minecraft:netherite_ingot`
（两行竖排）。创造模式物品栏位于「装饰性方块」。

## 已知限制

- 未在游戏内实测（仅编译 + 静态核对）；首次进游戏请先用**复制出来的测试装备**验证。
- 界面为代码绘制，未做像素级美化。
- 方块模型直接引用 the_vault 的 `block/modifier_workbench`，因此外观与原版相同；
  附魔感外观（glint）未实现，见 `docs/create-gear-sealer-pro/` 的追加记录 A1。
- 密封词条列表显示的是样例数值（显示用随机种子固定），实际数值在封印时按原版权重重新滚。
