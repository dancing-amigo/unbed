# Unbed Product Design

## 1. 推奨技術スタック

### 方針
- MVPは Android を最優先し、OS制約の強いアラーム・通知・ロック画面表示・QRスキャンを安定実装する。
- 将来の iOS 展開を見据え、`apps/android` と `apps/ios` を並列管理できるモノレポ構成にする。
- ただし、アラーム発火やバックグラウンド挙動は OS 依存が大きいため、最初から全面クロスプラットフォーム化はしない。
- 共通化するのはドメインモデル、ユースケース、設定値、将来の API 契約までに留める。

### MVP 推奨スタック
- Android UI: Kotlin + Jetpack Compose
- Android architecture: MVVM + UseCase/Repository 分離
- Android async: Kotlin Coroutines + Flow
- Local storage: Room
- Alarm scheduling: `AlarmManager` + exact alarm permission 対応
- Background recovery: `BroadcastReceiver` + `BOOT_COMPLETED` で再登録
- Foreground behavior: Full-screen intent notification + Alarm screen Activity
- QR scanning: CameraX + ML Kit Barcode Scanning
- Dependency injection: Hilt
- Logging/analytics: Firebase Analytics + Crashlytics または当面はローカルログのみ
- Build/tooling: Gradle Kotlin DSL, ktlint, detekt, JUnit, MockK, Turbine

### 将来 iOS を見据えた構成
- iOS app: Swift + SwiftUI
- iOS architecture: MVVM + UseCase/Repository
- Shared domain contract: `packages/domain-spec` に仕様と状態遷移を集約
- Shared backend contract: 将来 API を追加する場合は `packages/contracts` を追加

### この構成を推奨する理由
- Android のアラーム実装はネイティブ機能への依存が強く、Flutter/React Native で始めるより Kotlin ネイティブの方が MVP の確実性が高い。
- 一方で、将来の iOS 追加に備えてフォルダーとドメイン定義を先に分離しておくことで、コードの再整理コストを下げられる。
- 将来の歩数計測やユーザー固有 QR 導入時も、ドメイン層の状態遷移を保ったまま各 OS 実装を増やせる。

## 2. 推奨フォルダー構成

```text
unbed/
├─ .agent/
│  └─ docs/
│     └─ product-design.md
├─ apps/
│  ├─ android/
│  │  ├─ app/
│  │  ├─ core/
│  │  │  ├─ model/
│  │  │  ├─ database/
│  │  │  ├─ notifications/
│  │  │  ├─ alarms/
│  │  │  ├─ qr/
│  │  │  └─ utils/
│  │  ├─ feature/
│  │  │  ├─ onboarding/
│  │  │  ├─ alarm-settings/
│  │  │  ├─ ringing/
│  │  │  ├─ snooze-release/
│  │  │  └─ qr-setup/
│  │  ├─ domain/
│  │  └─ build-logic/
│  └─ ios/
│     └─ README.md
├─ packages/
│  ├─ domain-spec/
│  │  ├─ state-machine.md
│  │  ├─ entities.md
│  │  └─ glossary.md
│  └─ contracts/
│     └─ README.md
├─ backend/
│  └─ README.md
├─ scripts/
│  └─ README.md
└─ README.md
```

### フォルダー設計の意図
- `apps/android`: Android 実装本体。MVP はここが中心。
- `apps/ios`: 将来追加する iOS 実装の受け皿。初期はプレースホルダーでよい。
- `packages/domain-spec`: OS 非依存の状態遷移、用語、エンティティ定義を格納する場所。
- `packages/contracts`: 将来の認証、ユーザー固有 QR、同期 API 用の契約定義置き場。
- `backend`: 現時点では未使用でも、将来の QR 発行やユーザー管理で追加しやすくする。

### Android 内の責務分割
- `core/model`: Entity, enum, value object
- `core/database`: Room schema, DAO
- `core/alarms`: `AlarmManager`, receiver, reboot re-scheduler
- `core/notifications`: notification channel, full-screen intent
- `core/qr`: camera, barcode parser, QR validation
- `feature/alarm-settings`: 時刻、曜日、鳴動設定 UI
- `feature/ringing`: 発火中画面、QR スキャン導線
- `feature/snooze-release`: MVP の手動解除 UI、将来の歩数解除置換ポイント
- `feature/qr-setup`: 固定 QR 表示、利用ガイド

## 3. プロダクト全体像

### プロダクトの目的
Unbed は「起床時に物理的な移動を伴う解除条件を課すことで、ベッドから出る行動を強制する」起床支援プロダクトである。

### コア体験
1. ユーザーは起床時刻を設定する。
2. 指定時刻にアラームが鳴り、ロック画面上でも解除導線が表示される。
3. ユーザーはベッドから離れた場所の QR をスキャンする。
4. QR に成功しても即終了せず、スヌーズ状態へ移行する。
5. ユーザーは追加の解除条件を満たして初めてその回のアラームを完全終了できる。

### MVP 時点の解除モデル
- QR スキャン成功で `ringing -> snoozed_waiting_release`
- 10分後に再びアラーム発火
- ユーザーがアプリ内の「解除完了」操作を行うまで、再発火と QR スキャンを繰り返す
- 再鳴動は 1 セッションあたり最大 10 回までとし、上限到達時はその回のアラームを強制終了する
- 進行中セッションと次の通常アラーム時刻が競合した場合は、進行中セッションを終了して新しい通常セッションへ切り替える
- MVP では解除完了条件は手動操作
- 将来はこの手動操作を「100歩到達」で置き換える

### 将来像
- ユーザー固有 QR の発行
- 歩数計測によるスヌーズ解除
- 曜日別・複数アラーム・難易度設定
- 起床成功率や解除までの時間の可視化
- 端末間同期、アカウント連携
- iOS 対応

### 中長期のプロダクト価値
- 単なる目覚ましではなく、起床完了までを管理する行動変容アプリ
- アラーム停止ではなく、起床達成をゴールにする設計
- 解除条件の差し替えにより、QR、歩数、位置情報、NFC などへ拡張可能

## 4. MVP仕様

### MVP のスコープ
- Android 単独リリース
- 完全ローカル保存
- 固定 QR 1種
- 単一アラーム管理を前提
- 単発アラームと曜日指定の繰り返しの両方に対応
- 曜日指定の繰り返し対応
- QR 成功後に 10 分スヌーズ
- 手動解除完了するまで再鳴動

### 機能要件

#### 4.1 初回セットアップ
- 通知権限の要求
- カメラ権限の要求
- 正確なアラーム権限が必要な端末では案内表示
- バッテリー最適化除外の案内
- 固定 QR の表示と、ベッドから離れた場所へ設置する説明
- 初回セットアップ完了前はアラーム設定画面へ進めない

#### 4.2 アラーム設定
- 起床時刻を設定できる
- 曜日ごとの繰り返しを設定できる
- 曜日未選択の場合は単発アラームとして扱う
- アラームの有効/無効を切り替えできる
- 設定内容は端末ローカルに保存される

#### 4.3 アラーム発火
- アプリを閉じていても設定時刻にアラームが鳴る
- 通知と全画面表示で起床を促す
- 発火中は QR スキャン以外で通常停止できない

#### 4.4 QR スキャン
- 発火中画面から QR スキャンへ遷移できる
- 固定 QR と一致した場合のみ成功
- 不一致ならアラームは継続
- スキャン成功でアラーム音は一旦停止し、`snoozed_waiting_release` に遷移する

#### 4.5 スヌーズと解除
- QR 成功後は 10 分のスヌーズタイマーを開始する
- スヌーズ中にアプリ内の「解除完了」操作が可能
- 解除完了した場合、その回のアラームは終了する
- 解除完了しないまま 10 分経過した場合、再度アラームが鳴る
- 再発火後も QR スキャンが必要
- 再鳴動は 1 セッションあたり最大 10 回までとする
- 上限到達時はその回のアラームを強制終了し、次回の通常予約のみ残す
- 進行中セッションより次の通常アラーム時刻が先に来た場合は、進行中セッションを終了して新しい通常アラームを優先する

#### 4.6 QR の用意
- アプリ内で固定 QR を表示できる
- ユーザーが印刷または別端末表示で利用できる
- 固定 QR の値は MVP では全ユーザー共通のハードコード値を使う

#### 4.7 再起動対応
- 端末再起動後も次回アラームを再登録する
- 再起動時に進行中スヌーズがあれば復元を試みる

### 非機能要件

#### 4.8 信頼性
- 設定済みアラームは OS 制約下で可能な限り高確率で発火する
- 再起動後の復元に対応する
- 状態不整合時は安全側に倒し、必要なら再発火させる
- セッション競合時は古い進行中セッションより次の通常アラームを優先する

#### 4.9 UX
- 3タップ以内でアラーム設定できる
- 初回セットアップで必要権限と注意点を説明する
- ロック画面上でも迷わず QR スキャン導線に入れる

#### 4.10 セキュリティ/不正耐性
- MVP は固定 QR のため複製耐性は低い
- この制約を仕様上明示し、将来のユーザー固有 QR で改善する
- カメラ故障や OS 都合で QR 解除不能なケースに対する救済導線は MVP では用意しない

#### 4.10.1 実装時に明示する既知制約
- 固定 QR は全ユーザー共通であり、本人性は保証しない
- カメラ権限がない場合は解除フローに入れず、権限再付与が必要
- `manual_release` は暫定であり、将来は `step_count_release` に置き換える
- 端末ベンダー固有の電池最適化により発火時刻が遅延する可能性がある
- 実機検証では lock screen, exact alarm, reboot, timezone change を最低限確認対象に含める

#### 4.11 保守性
- 状態遷移と UI を分離する
- 「解除条件」を差し替え可能な設計にする
- 歩数解放に差し替えても画面遷移を大きく変えない

#### 4.12 対応環境
- Android 10 以上を初期対象とする
- カメラ搭載端末を前提とする
- 歩数機能は MVP 非対象

### 状態遷移モデル

```text
idle
  -> scheduled
scheduled
  -> ringing
ringing
  -> scanning_qr
scanning_qr
  -> ringing                (QR 不一致 / キャンセル)
  -> snoozed_waiting_release (QR 一致)
snoozed_waiting_release
  -> cleared                (アプリ内で解除完了)
  -> ringing                (10分経過)
  -> idle                   (単発アラームで上限到達)
  -> scheduled              (繰り返しアラームで上限到達)
cleared
  -> idle                   (単発アラームのその回が終了)
  -> scheduled              (次回繰り返し分を待機)
```

### 状態遷移の補足ルール
- `repeatDays` が空の場合は単発アラームとして扱う
- `snoozed_waiting_release -> ringing` の再遷移ごとに再鳴動回数を加算する
- 再鳴動回数が 10 回に達したらそのセッションを終了し、単発なら `idle`、繰り返しなら `scheduled` へ進む
- 進行中セッションがある状態で次の通常アラーム時刻へ達した場合は、既存セッションを終了扱いにして新しい通常セッションを開始する

### MVP の主要データモデル
- `AlarmConfig`
  - `id`
  - `time`
  - `repeatDays`
  - `enabled`
  - `soundType` = fixed
- `AlarmSession`
  - `sessionId`
  - `alarmId`
  - `scheduledAt`
  - `state`
  - `qrValidatedAt`
  - `snoozeUntil`
  - `releasedAt`
  - `snoozeCycleCount`
  - `sessionEndedReason`
- `QrConfig`
  - `mode` = fixed
  - `fixedValue` = app constant
- `SessionEndedReason`
  - `manual_release`
  - `rering_cap_reached`
  - `superseded_by_next_schedule`
- `SoundConfig`
  - MVP では 1 種類固定とし、設定 UI は提供しない

## 5. フェーズ分割した実装計画

### Phase 1: プロジェクト基盤・フォルダー構成
- モノレポのルート構成を作る
- Android app モジュール、core、feature、domain の分割を行う
- `packages/domain-spec` に状態遷移と用語を定義する
- CI 用に lint/test の最低限を用意する

### Phase 2: アラーム設定機能
- 単一アラームのデータモデルを実装する
- 時刻設定、曜日設定、有効/無効切替 UI を作る
- Room 保存と ViewModel を接続する

### Phase 3: バックグラウンドでも鳴るアラーム機能
- `AlarmManager` で次回発火を予約する
- `BroadcastReceiver` で発火処理を受ける
- 通知チャネルと full-screen intent を実装する
- 再起動時の再登録を実装する

### Phase 4: QR 読み取りによる一時停止
- 発火中画面を実装する
- CameraX + ML Kit で QR スキャンを実装する
- 全ユーザー共通の固定 QR 定数との検証ロジックを実装する
- QR 一致で `snoozed_waiting_release` に遷移し、10分後再発火を予約する

### Phase 5: MVP 仕上げ
- スヌーズ中の「解除完了」画面を実装する
- 解除完了がない場合の再鳴動ループと上限到達時の強制終了を仕上げる
- 初回セットアップ、権限導線、QR 表示画面を実装し、セットアップ完了まで利用をロックする
- エラーハンドリング、ログ、テストを追加する

### Phase 6: 将来機能
- 手動解除を歩数解除へ置換する
- `SensorManager` または Health Connect を用いた歩数検知を追加する
- 固定 QR をユーザー固有 QR に置き換える
- 複数アラーム、履歴、分析、認証、同期、iOS 版追加へ拡張する

## 6. 今後の拡張ポイント

### 解除条件エンジン化
- 現在の解除条件は `manual_release`
- 将来 `step_count >= 100` に差し替える
- さらに `location`, `nfc`, `task completion` を追加可能にする

### QR の進化
- 固定 QR からユーザー固有 QR へ変更
- 時限トークン付き QR やバックエンド発行へ拡張

### データ同期
- ローカルのみからアカウント連携へ移行
- 起床履歴、成功率、設定同期を追加

### プラットフォーム拡張
- `packages/domain-spec` を元に iOS 実装を追加
- iOS では OS 制約上、Android と完全同等体験にならない可能性を許容する

## 7. 開発時の主なリスクや論点

### Android 特有の制約
- exact alarm 権限が必要な端末がある
- バッテリー最適化やメーカー独自制御で発火遅延の可能性がある
- full-screen intent の挙動は OS バージョンや通知設定に影響される
- 通知無効化時は体験が大きく劣化する

### UX/仕様上の論点
- 手動解除ボタンをユーザーがベッド上で押せるため、MVP の抜け道になる
- 固定 QR は撮影・複製で回避可能
- 再鳴動が強すぎるとアンインストール要因になる
- カメラ権限拒否時は停止導線が成立しない
- カメラ故障や OS 制約で QR 遷移に失敗した場合、MVP では救済導線がない

### 実装上の論点
- 状態復元を厳密にしないと `ringing` と `snoozed` が競合する
- 再起動、時刻変更、タイムゾーン変更時の再スケジュールが必要
- 10分スヌーズ中にアプリが kill された場合の復元ルールが必要
- 進行中セッションと次の通常アラームが競合したときの終了優先順位をコードに固定する必要がある

### 将来拡張時の論点
- 歩数カウントは端末差分と OS 制約が大きい
- iOS は Android と同等のアラーム保証が難しい
- バックエンド導入時にローカル設計と整合を取る必要がある

## 推奨する意思決定
- MVP は Android ネイティブで開発する
- 単一アラームで始める
- 解除条件は状態遷移としてモデリングする
- 手動解除は MVP の一時措置として明記し、将来 `100歩` に差し替える
- 固定 QR、カメラ必須、再鳴動上限 10 回を MVP 制約として明記する
- ユーザー向けには「必ず起こす」ではなく「起床行動を促進する」表現で設計する
