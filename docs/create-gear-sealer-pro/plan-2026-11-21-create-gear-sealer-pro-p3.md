<!-- LOCK id=L2 date=2026-11-21 plan=P3 -->
> 🔒 **L2 已锁定（2026-11-21）** —— 本段只读。确认内容：按 P3 重构为 common + adapter/1_18_2；CI 采用 self-hosted runner；远端由用户创建。


## 计划 P3 · 重构为 common + adapter、MIT 许可、远端仓库与 tag CI

- 日期：2026-11-21
- 依据：skill §1.2（工程形态选 A）、§3（目录与依赖规则）、§4（tag/发布）、§7（验收）
- 状态：**待确认（G1）**

### 目标

1. 把当前单工程重构为 `common`（纯 JVM，零 MC import）+ `adapter/1_18_2`（唯一接触 MDK），产出仍是单 jar `gearsealerpro-1.18.2-0.1.0.jar`。
2. 元数据按用户确认：`mod_license=MIT`、`mod_authors=xiami`（`mod_id`/`mod_name`/`mod_version` 不变）。
3. 建立 GitHub 远端仓库 `xiamigame/gear-sealer-pro`（SSH 已验证可用），推送 `main`。
4. 增加 `.github/workflows/release.yml`：tag `v*` 触发构建 + 发布 Release（挂载 jar）。
5. 打 annotated tag `v0.1.0-1.18.2`。

### 背景与约束

- skill §3：`common` 不得出现 `net.minecraft` / `net.minecraftforge` / `com.mojang` / `org.lwjgl`；`adapter → common` 单向依赖；两模块同一主包前缀 `space.xiami.projects.gearsealerpro`。
- 既有 P1 已锁定的行为不得改变：成本、词条池、替换/新增语义、锁定逻辑。
- `the_vault` 只 `compileOnly`，不打包、不提交仓库（`libs/` 已 gitignore）。
- 当前 16 个类全部直接依赖 MC/the_vault，可抽到 `common` 的只有纯逻辑；重构必须保证行为不变。

### 范围

**A. 模块划分**

```text
gear-sealer-pro/
├─ settings.gradle              # include 'common', 'adapter:1_18_2'
├─ build.gradle                 # 聚合；-Padapter=1_18_2（默认 1_18_2）
├─ gradle.properties            # 只放 mod_*（id/name/version/group/license/authors/description）
├─ common/
│  ├─ build.gradle              # java-library，Java 17 toolchain，零 MC 依赖，JUnit5
│  └─ src/main/java/space/xiami/projects/gearsealerpro/{api,core}
│  └─ src/test/java/space/xiami/projects/gearsealerpro/core
├─ adapter/1_18_2/
│  ├─ gradle.properties         # minecraft_version / forge_version / mappings
│  ├─ build.gradle              # FG [6.0,6.2)、compileOnly the_vault、合并 common 输出
│  └─ src/main/{java,resources} # 方块/BE/菜单/网络/界面/SealerLogic/注册/资源
└─ .github/workflows/release.yml
```

**B. common 内容（纯 JVM）**

| 类 | 职责 |
|---|---|
| `api/SealOptionKey` | 记录：`String id` + `boolean prefix` |
| `api/SealTargetKey` | 记录：`int index` + `boolean prefix` |
| `api/SealRequest` | 记录：`SealOptionKey` + `int targetIndex`（-1 = 新增） |
| `core/SelectionModel` | 选择状态机：选中词条/目标、前后缀切换、滚动夹取、按类型过滤视觉索引 |
| `core/PanelLayout` | 面板尺寸、列表矩形、行高、按钮坐标（纯 int 常量与计算） |
| `core/CostView` | 消耗行的 `have/need` 与是否足量判断 |

**C. adapter 内容**

方块、方块实体、菜单、三个网络包、`SealerLogic`（the_vault API）、界面（只做渲染 + 事件转发给 `SelectionModel`）、注册类、`mods.toml`、lang、blockstate/model、配方。

**D. 构建接线**

- `adapter/1_18_2/build.gradle`：`implementation project(':common')`；`jar { from(project(':common').sourceSets.main.output) }` 合并 common 类后再 `reobfJar`。
- 资源与 `mods.toml` 的 `${mod_*}` 由 adapter 的 `processResources` 从根 `gradle.properties` 展开。
- 产物名保持 `<mod_id>-<mc>-<mod_version>.jar`。

**E. CI**

`.github/workflows/release.yml`：`on.push.tags: v*` → JDK 17 → `./gradlew :adapter:1_18_2:build --no-daemon` → `softprops/action-gh-release` 挂 jar。

**F. 发布**

`git remote add origin git@github.com:xiamigame/gear-sealer-pro.git` → push main → `git tag -a v0.1.0-1.18.2` → push tag → 核对 Actions 与 Release 资产。

### 非目标

- 不改密封行为、成本公式、词条池、界面布局观感。
- 不新增 MC 版本 adapter（只 `1_18_2`）。
- 不做 Fabric/NeoForge 支持。
- 不做附魔外观。

### 步骤

1. [ ] 建 `common/`：`build.gradle`（java-library + JUnit5）、`api`/`core` 五个类、2 个单测（`SelectionModel` 滚动夹取与类型过滤、`CostView` 足量判断）。
2. [ ] 建 `adapter/1_18_2/`：`gradle.properties`、`build.gradle`（FG + the_vault + common 合并），迁移 16 个类到 `...gearsealerpro.{block,menu,network,registry,client,logic}`，迁移 `src/main/resources`。
3. [ ] 改根 `settings.gradle` / `build.gradle` / `gradle.properties`（元数据 MIT / xiami，去掉 MC 相关属性）。
4. [ ] 界面改用 `core/PanelLayout` 与 `core/SelectionModel`（渲染逻辑不变）。
5. [ ] 构建：`.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon` 通过；核对 jar 内含 common 类、`mods.toml`、资源；`common` 零 MC import（grep 验证）。
6. [ ] 元数据：`mod_license=MIT`、`mod_authors=xiami`，重建核对 `mods.toml` 展开结果。
7. [ ] 写 `.github/workflows/release.yml`。
8. [ ] 建远端 → push → tag → push tag → 核对 CI/Release。
9. [ ] 更新 README（形态 common + adapter、包名、基线、构建命令）、RELEASING（tag/CI 流程）、追加执行记录 A3。

### 影响面

- 仓库目录结构变化（旧 `src/` 删除）；构建命令变为 `:adapter:1_18_2:build`。
- 对外产物不变：jar 名、mod_id、包名、方块注册名、语言键、配方。
- 远端仓库为**对外可见**动作，需单独确认（见确认点 2、3）。

### 风险与回滚

| 风险 | 应对 |
|---|---|
| 重构引入行为回归 | 逻辑代码原样搬运，只把选择状态与布局常量外提；构建后核对类清单与 jar 内容 |
| `common` 误引 MC | 构建后 grep `common/src` 中的 `net.minecraft`/`net.minecraftforge`/`com.mojang`/`org.lwjgl`，必须为 0 |
| 单 jar 合并漏掉 common 类 | 展开 jar 检查 `core/` `api/` 类存在 |
| CI 构建缺 `the_vault` jar | **未决**，见确认点 2 |
| 重构失败 | 在分支 `refactor/common-adapter` 上做，未合并前 `main` 保持可用 |

### 验收标准

1. `:common:test` 通过，`common` 零 MC import。
2. `:adapter:1_18_2:build` 通过，产物 `build/libs/gearsealerpro-1.18.2-0.1.0.jar` 含 common + adapter 类与全部资源。
3. `mods.toml` 展开正确：`modId=gearsealerpro`、`version=0.1.0`、`license=MIT`、`authors=xiami`、依赖不变。
4. `main` 推到远端；annotated tag `v0.1.0-1.18.2` 存在；CI 对该 tag 成功并产出 Release（若 CI 方案可执行）。
5. `git status` 干净；README/RELEASING 与实际一致。

### 需要的确认点

1. **本计划整体确认**（含在 `refactor/common-adapter` 分支上重构）。
2. **CI 如何拿到 `the_vault` jar**（仓库不能提交该 jar，它是 All Rights Reserved、57 MB）：
   - A. **self-hosted runner**：在你本机注册 GitHub Actions runner，`libs/` 现成可用，CI 真正用真实依赖构建（推荐，需你注册一次）；
   - B. GitHub-hosted + 仓库内提交一份「仅签名」的 API stub 模块（不含实现，约 15 个方法）：CI 能编译，但验证的是桩不是真 the_vault；
   - C. GitHub-hosted 只跑 `common` 单测；jar 本地构建后手动上传 Release（CI 不产出 jar）。
3. **远端仓库创建方式**：你已在 GitHub 建好空仓库 `xiamigame/gear-sealer-pro` 并告诉我；或给我一个有 `repo` 权限的 PAT 由我创建（token 不入库、不写日志）。SSH 推送已可用。

### A3 · 2026-11-21 · 提出人：user

- **类型**：执行记录（P3：工程形态重构 + 发布前置）
- **关联**：L2（P3）
- **变更**：
  - 重构为 `common` + `adapter/1_18_2`；`common` 仅含 `api`（`SealOptionKey`/`SealTargetKey`/`SealRequest`）
    与 `core`（`SelectionModel`/`PanelLayout`/`CostView`）及 JUnit5 单测；界面改用这些纯 JVM 类型。
  - `adapter/1_18_2` 承接原 16 个类与全部资源，`implementation project(':common')`，
    `jar { from(common output) }` 合并后 `reobfJar`，产物仍为单 jar。
  - 元数据：`mod_license=MIT`、`mod_authors=xiami`（`mod_id`/`mod_name`/包名/注册名/语言键不变）。
  - 新增 `.github/workflows/release.yml`（self-hosted runner，tag `v*` → 构建 → Release）。
  - 远端 `git@github.com:xiamigame/gear-sealer-pro.git`，推送 `main` 与 annotated tag `v0.1.0-1.18.2`。
- **证据**：
  - `.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon` → `BUILD SUCCESSFUL`。
  - 产物 `adapter/1_18_2/build/libs/gearsealerpro-1.18.2-0.1.0.jar`（66 KB，60 条目）：
    `api/`+`core/` 类 8 个、adapter 类 39 个、`iskallia/*` 类 0 个。
  - `common/src` 中 `net.minecraft|net.minecraftforge|com.mojang|org.lwjgl` 引用数 = 0。
  - `mods.toml` 展开：`license="MIT"`、`authors="xiami"`、`modId="gearsealerpro"`、`version="0.1.0"`、
    依赖 `forge [40,)` / `minecraft [1.18.2,1.19)` / `the_vault [3.21.6,)`。
  - 远端：`refs/heads/main` = `0dd14f3`；`refs/tags/v0.1.0-1.18.2` 为 annotated tag（指向同一提交）。
- **未做 / 待用户**：
  - CI 尚未真正执行：需要你在本机注册 self-hosted runner（标签 `self-hosted,windows,x64`），
    并在仓库设置里加变量 `THE_VAULT_JAR` 指向 the_vault jar 路径；在此之前 tag 触发的 job 会排队。
  - 游戏内验收仍由你手动完成。
- **影响**：仓库结构变更；产物名、mod id、包名、方块注册名、语言键、配方不变。
- **生效**：2026-11-21
- **状态**：ACTIVE

### A4 · 2026-11-21 · 提出人：agent

- **类型**：补丁
- **关联**：A3（CI 部分）
- **变更**：`release.yml` 增加一步「tag 与 `mod_version` + adapter 目录一致性校验」（提交 `ca21561`）。
  依据 skill 审计脚本的规则 `workflow validates adapter directory and mod_version`。
- **证据**：`audit-mod-repo.ps1` → `0 fail, 0 warn`（24 项 PASS）。
- **影响**：仅 CI 行为；不影响产物。
- **注意**：已推送的 tag `v0.1.0-1.18.2` 指向 `0dd14f3`（该提交的 workflow 还没有这一步校验），
  按"不复用/不改写已发布 tag"的规则**没有**移动 tag；校验从下一个 tag 起生效。
- **状态**：ACTIVE

### A5 · 2026-11-21 · 提出人：user

- **类型**：热修（patch，0.1.0 → 0.1.1）
- **关联**：A1/A3（`mods.toml` 依赖声明）
- **问题**：整合包启动崩溃 `crash-2026-11-21_18.41.47-fml.txt`：
  `Mod gearsealerpro requires the_vault 3.21.6 or above / Currently, the_vault is 1.18.2-3.21.6.6884`。
- **根因**：The Vault 的 mod 版本号是 `<mc>-<mod version>`（`1.18.2-3.21.6.6884`），
  Forge 用 Maven 版本语义比较，把它解析为主版本 1，因此 `[3.21.6,)` 永远不满足。
- **变更**：
  - `adapter/1_18_2/src/main/resources/META-INF/mods.toml`：`the_vault` 的
    `versionRange` 由 `[3.21.6,)` 改为 `[1.18.2-3.21.6,)`，并加注释说明前缀原因。
  - `mod_version` 0.1.0 → 0.1.1；tag `v0.1.1-1.18.2`。
- **证据**（用 Maven `VersionRange` 实测，与 Forge 同一套实现）：
  - `[3.21.6,)` → 对 `1.18.2-3.21.6.6884` 为 **false**；
  - `[1.18.2-3.21.6,)` → true；对 `1.18.2-3.21.6.7000`、`1.18.2-3.22.0.1` 也为 true。
  - 构建后 jar 内 `mods.toml` 展开：`version="0.1.1"`、`the_vault` `versionRange="[1.18.2-3.21.6,)"`。
- **未验证**：游戏内启动（需你替换 jar 后实测）。
- **影响**：仅加载期依赖检查；产物名 `gearsealerpro-1.18.2-0.1.1.jar`。
- **生效**：2026-11-21
- **状态**：ACTIVE

### A6 · 2026-11-21 · 提出人：user（截图反馈 5 项）

- **类型**：补丁（UI / 交互，0.1.1 → 0.1.2）
- **关联**：A5；用户截图与 5 条反馈
- **根因与变更**：
  1. **词条重复、选中一个两个都亮**：`VaultGearModifier.AffixType` 的枚举顺序是
     `IMPLICIT(0) / PREFIX(1) / SUFFIX(2)`，旧代码用 `type == 0` 判前缀，导致前后缀两条
     都落进"后缀"侧的过滤结果。改为「词条只按 id 去重，条目携带 side 位掩码
     （1=前缀 / 2=后缀）」。
  2. **前后缀为什么分**：封印词条最终必须落进前缀或后缀槽。**替换**时由目标词条决定，
     只有**新增词条**时才需要选槽位 —— 因此前缀/后缀切换改为只在新增模式显示。
  3. **只显示最低档数值**：`VaultGearModifier#getDisplay(..., displayDetail)` 的第 4 个参数
     改为 `true`，文案会追加配置区间（如 `(1 - 3)`）。样例值仍用固定种子生成。
  4. **装备 tooltip 消失**：`super.render()` 先画 tooltip，随后自绘面板把它盖住；
     改为在自绘结束后对 `hoveredSlot` 重绘一次 tooltip。
  5. **面板太窄**：`common/PanelLayout` 宽度 176 → **240**，左右列表各 110，
     槽位坐标同步重排（`GearSealerProMenu`），并保持与 common 布局常量一致。
  6. 附带：选择替换目标后，若当前词条仍允许该侧则保留选择（不再无条件清空）。
- **证据**：
  - `.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon` → `BUILD SUCCESSFUL`（含新增的两个 SelectionModel 单测）。
  - 产物 `adapter/1_18_2/build/libs/gearsealerpro-1.18.2-0.1.2.jar`；
    `mods.toml` 展开：`version="0.1.2"`、`license="MIT"`、`authors="xiami"`、
    `the_vault` `versionRange="[1.18.2-3.21.6,)"`。
  - tag `v0.1.2-1.18.2` 已推送。
- **未验证**：游戏内实际观感与交互（需用户替换 jar 后确认）。
- **遗留**：`getDisplay(..., true)` 标注为 `@OnlyIn(Dist.CLIENT)`，服务端计算列表时调用它
  仅保证**集成服务器（单人）**可用；专用服务器需要改成服务端算 id/范围、客户端拼文案。
- **状态**：ACTIVE

### A7 · 2026-11-21 · 提出人：user（“强行启动会怎样”）

- **类型**：补丁（稳定性，0.1.2 → 0.1.3）
- **关联**：A6 遗留第 1 条
- **事实**（字节码核对）：
  - `VaultGearModifier.getDisplay(data, type, stack, boolean)` 的 4 参重载**无注解**，方法体只是转调 5 参重载；
  - 5 参重载 `getDisplay(..., boolean, boolean)` 带 `@OnlyIn(Dist.CLIENT)`；
  - 专用服务器上 Forge `RuntimeDistCleaner` 会删掉该 5 参方法，4 参方法里的 `invokevirtual` 因此抛
    `NoSuchMethodError`（`LinkageError`，不是 `RuntimeException`）。
- **原行为**：`SealerLogic.display()/targets()` 只 `catch (RuntimeException)`，兜不住该 Error →
  异常冒到网络包处理 → 玩家断线；能开界面、能放方块，但只要放装备就炸；存档不受影响。
- **变更**：两处 catch 改为 `RuntimeException | LinkageError`，降级为
  `Component.literal(<modifier id>)`；`targets()` 同理。
- **效果**：
  - 单人/局域网：行为不变（仍显示数值 + 区间）；
  - 专用服务器：不再崩，列表显示 `the_vault:<modifier_id>`，无区间；封印/替换/新增逻辑仍可用。
- **证据**：`.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon` → `BUILD SUCCESSFUL`；
  产物 `adapter/1_18_2/build/libs/gearsealerpro-1.18.2-0.1.3.jar`。
- **未验证**：没有真实专用服务器环境实测（无服务器可起），结论基于 Forge 的 DistCleaner 语义与字节码。
- **遗留**：专用服务器上的可读文案仍需"服务端发 id、客户端拼显示"的改造（约 1–2 小时 + 服务器实测）。
- **状态**：ACTIVE
