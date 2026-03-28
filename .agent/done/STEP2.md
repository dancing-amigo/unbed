# STEP2: ドメインモデルと状態遷移を固める

## 目的
- MVP の中核であるアラーム状態遷移を UI や OS 実装から独立して定義する。
- 将来の歩数解除に差し替えても破綻しないモデルを先に作る。

## 実装内容
- `AlarmConfig`, `AlarmSession`, `QrConfig` を定義する。
- `AlarmState` として `idle`, `scheduled`, `ringing`, `scanning_qr`, `snoozed_waiting_release`, `cleared` を定義し、単発と繰り返しの両方で成立する遷移にする。
- `ReleaseConditionType` として少なくとも `manual_release` を定義する。
- 将来用に `step_count_release` を enum または sealed class で予約する。
- `QR success -> snoozed_waiting_release -> manual release or re-ring` の状態遷移ルールをユースケース化する。
- 単発アラームでは `cleared -> idle`、繰り返しでは `cleared -> scheduled` になるルールを定義する。
- 再鳴動上限 10 回と `manual_release`, `rering_cap_reached`, `superseded_by_next_schedule` の終了理由を定義する。
- 不正な遷移を防ぐドメイン関数を定義する。

## 完了条件
- 状態遷移図とコードの表現が一致している。
- UI なしでもアラームセッションの進行をシミュレートできる。
- 単発と繰り返しで終端が変わること、再鳴動上限で終了することが表現できる。
- 将来の解除条件差し替えポイントが明示されている。

## 注意点
- 「停止」ではなく「解除条件達成による完了」を中心概念にする。
- Android 固有クラスをドメイン層に持ち込まない。
