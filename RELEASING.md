# Releasing

## 版本模型

- 功能版本：`gradle.properties` 的 `mod_version`（SemVer），当前 `0.1.0`。
- MC 版本：单一适配（1.18.2），不建 `adapter/<mc>` 分层。
- tag：`v<mod_version>-<mc_version>`，annotated，例如 `v0.1.0-1.18.2`。tag 是版本事实来源。

## 发布顺序

1. `.\gradlew.bat build --no-daemon` 通过，工作树干净。
2. 核对 `build/libs/gearsealerpro-1.18.2-0.1.0.jar` 内的 `META-INF/mods.toml`：
   `modId=gearsealerpro`、`version` 与 `mod_version` 一致、依赖 `the_vault`/`forge`/`minecraft` 正确。
3. `git tag -a v0.1.0-1.18.2 -m "Gear Sealer Pro 0.1.0 for MC 1.18.2"`。
4. 推送由维护者决定；remote 未配置时不推送。

## 兼容性

- 依赖 `the_vault >= 3.21.6`；若 the_vault 升级导致 API 变化，需要重新编译并升 `mod_version`。
- 不修改原版方块与配方，可与原版 Gear Sealer 并存。
