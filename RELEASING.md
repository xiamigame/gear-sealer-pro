# Releasing

## 版本模型

- 功能版本：根 `gradle.properties` 的 `mod_version`（SemVer），当前 `0.1.0`。
- MC 版本：`adapter/<mc>/` 目录（当前只有 `1_18_2`）+ tag 后缀。
- tag：`v<mod_version>-<mc_version>`，annotated，例如 `v0.1.0-1.18.2`。tag 是版本事实来源。

## 发布顺序

1. `.\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon` 通过，工作树干净。
2. 核对 `adapter/1_18_2/build/libs/gearsealerpro-1.18.2-0.1.0.jar`：
   - 含 `common` 的 `api/`、`core/` 类与 adapter 全部类、资源；
   - `META-INF/mods.toml`：`modId=gearsealerpro`、`version` 与 `mod_version` 一致、
     `license=MIT`、依赖 `the_vault`/`forge`/`minecraft` 正确。
3. `git tag -a v0.1.0-1.18.2 -m "Gear Sealer Pro 0.1.0 for MC 1.18.2"`。
4. `git push origin main`、`git push origin v0.1.0-1.18.2`。
5. CI（`.github/workflows/release.yml`）在 self-hosted runner 上重建并创建 Release，
   资产名应为 `gearsealerpro-1.18.2-0.1.0.jar`。

## CI 前置（self-hosted runner）

- Runner 标签：`self-hosted`、`windows`、`x64`；机器需安装 JDK 17。
- 仓库变量 `THE_VAULT_JAR` 指向机器上的 `the_vault-1.18.2-3.21.6.6884.jar`
  （例如整合包的 `mods/` 路径）。该 jar 不进入仓库：它是 All Rights Reserved 且 57 MB。
- 首次运行会下载 Gradle 发行版与 Forge/MC 依赖；之后走 `~/.gradle` 缓存。

## 兼容性

- 依赖 `the_vault >= 3.21.6`；若 the_vault 升级导致 API 变化，需要重新编译并升 `mod_version`。
- 不修改原版方块与配方，可与原版 Gear Sealer 并存。
