# STEP1: リポジトリ基盤を作る

## 目的
- Android MVP 開発を開始できる最小のモノレポ基盤を整える。
- 将来の iOS 追加、共通仕様管理、バックエンド追加に耐えるルート構成を先に固定する。

## 実装内容
- ルートに `apps/android`, `apps/ios`, `packages/domain-spec`, `backend`, `scripts` を作成する。
- Android プロジェクトを Gradle Kotlin DSL で初期化する。
- `settings.gradle.kts`, ルート `build.gradle.kts`, `gradle/libs.versions.toml` を整備する。
- Android の target/min SDK 方針を確定する。
- `ktlint`, `detekt`, `JUnit` を最低限導入する。
- ルート `README.md` にプロジェクト概要と起動手順の枠を作る。

## 完了条件
- Android プロジェクトが同期可能である。
- ルート構成が `.agent/docs/product-design.md` と一致している。
- lint/test のプレースホルダー実行コマンドが定義されている。

## 注意点
- iOS はまだ実装しないが、受け皿ディレクトリは必ず作る。
- クロスプラットフォーム共通化はこの段階では行わない。
