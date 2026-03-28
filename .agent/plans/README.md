# Unbed Implementation Plan

## Completed Updates

- STEP1 completed: monorepo skeleton, Android Gradle baseline, and repo scripts were initialized.
- STEP2 completed: core alarm entities, release condition abstraction, and state machine were implemented.
- STEP3 completed: Room schema, repository bridge, and next-trigger calculation were added.
- STEP4 completed: Compose alarm settings UI now saves locally and reschedules the next alarm.
- STEP5 completed: AlarmManager scheduling, receiver dispatch, and re-schedule conflict handling were connected.
- STEP6 completed: high-priority alarm notification, full-screen ringing activity, and playback control were added.
- STEP7 completed: CameraX and ML Kit QR scanning now validate only the fixed MVP QR marker.
- STEP8 completed: QR success enters snooze, manual release clears the session, and re-ring scheduling stays active.
- STEP9 completed: onboarding now gates alarm usage until permissions, device settings, and fixed QR preparation are done.
- STEP10 completed: boot and clock-change recovery now restores active sessions safely and re-registers future alarms.

## STEP1: リポジトリ基盤を作る
- `.agent/docs` と整合するモノレポ骨格を作る
- `apps/android`, `apps/ios`, `packages/domain-spec`, `backend`, `scripts` を用意する
- Android 用の Gradle Kotlin DSL 構成を初期化する
- lint, format, test の最低限の実行基盤を入れる

## STEP2: ドメインモデルと状態遷移を固める
- `AlarmConfig`, `AlarmSession`, `QrConfig` を定義する
- 単発と繰り返しの両方を含む `idle`, `scheduled`, `ringing`, `scanning_qr`, `snoozed_waiting_release`, `cleared` の状態遷移を実装する
- 「QR成功で即終了」ではなく「QR成功でスヌーズ開始、解除完了まで再鳴動」のルールをコード上の中心に置く
- 再鳴動上限 10 回と終了理由をドメインルールとして定義する
- 将来の `manual_release` から `step_count_release` への差し替えポイントを定義する

## STEP3: ローカル保存と設定管理を実装する
- Room の schema と DAO を実装する
- 単一アラーム前提の保存ロジックを作る
- 起床時刻、曜日繰り返し、有効/無効の保存を実装する
- `AlarmSession` に再鳴動回数と終了理由を保持する
- 次回発火時刻計算は「通常アラーム予約」と「進行中セッションの再発火」を区別して扱う

## STEP4: アラーム設定UIを作る
- Jetpack Compose で設定画面を実装する
- 時刻選択、曜日選択、有効/無効切替を実装する
- 保存時に次回アラーム予約まで接続する
- 将来の複数アラーム対応に耐える UI 構造にしておく

## STEP5: Androidの発火基盤を実装する
- `AlarmManager` で次回アラームを予約する
- exact alarm が必要な端末向けに権限案内を実装する
- `BroadcastReceiver` で発火イベントを受け取る
- アプリ未起動時でも動く通知基盤を作る
- 進行中セッションの再発火予約と通常アラーム予約の競合ルールを実装する

## STEP6: 発火中の通知と全画面アラーム画面を作る
- Notification Channel を作成する
- Full-screen intent でロック画面上のアラーム画面を表示する
- アラーム音とバイブ制御を実装する
- 発火中は通常の停止手段を出さない

## STEP7: QRスキャン機能を実装する
- CameraX と ML Kit で QR スキャンを実装する
- 全ユーザー共通の固定 QR 定数との一致判定を実装する
- 不一致時は発火状態を維持する
- 一致時は音を止めて `snoozed_waiting_release` に遷移する

## STEP8: スヌーズと解除完了ループを実装する
- QR 成功時に 10 分後の再発火を予約する
- スヌーズ中の状態表示画面を作る
- アプリ内の「解除完了」操作を実装する
- 解除完了しなければ 10 分後に再度 `ringing` に戻す
- 再発火後は再度 QR スキャンを必須にする
- 再鳴動上限到達時の予約キャンセルとセッション終了処理を入れる

## STEP9: 初回セットアップとQR準備導線を作る
- 通知権限、カメラ権限の案内を作る
- バッテリー最適化除外と exact alarm の説明を入れる
- 固定 QR の表示画面を作る
- ユーザーに「ベッドから離れた場所へ設置する」オンボーディングを実装する
- 初回セットアップ完了まで利用開始できないゲートを入れる

## STEP10: 再起動復元と異常系を固める
- `BOOT_COMPLETED` で次回アラームを再登録する
- スヌーズ中だった場合の復元ルールを実装する
- 時刻変更、タイムゾーン変更、権限拒否時の挙動を定義する
- 状態不整合が起きた場合は安全側に倒して再発火できるようにする

## STEP11: テストと品質担保を入れる
- 状態遷移のユニットテストを追加する
- 次回発火時刻計算のテストを追加する
- QR 一致/不一致の判定テストを追加する
- 再起動復元とスヌーズ再発火のテストを追加する
- 主要画面の UI テストを追加する

## STEP12: MVP仕上げ
- 文言、エラー表示、権限拒否時の導線を整える
- ログとクラッシュ収集を入れる
- 実機で端末差分を確認する
- MVP の制約として「固定QR」「カメラ必須」「解除不能時の救済導線なし」「再鳴動上限あり」「手動解除は暫定」「Android中心」を明文化する

## STEP13: 将来拡張の準備を残す
- `manual_release` を `step_count_release` に差し替えやすい interface にする
- `packages/domain-spec` に解除条件 abstraction を整理する
- ユーザー固有 QR とバックエンド導入のための `contracts` 境界を残す
- iOS 実装時に共有できるドメイン仕様を文書化する
