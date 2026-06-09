# WHOOP 5 Android Historical Sync Notes

Working branch: `whoop5-android-history-sync`

Date: 2026-06-09

Hardware under test:

- WHOOP 5.0 / MG band, advertised as `WHOOP 5AG0081917`
- Android debug package `com.noop.whoop.debug`
- Local Goose reference checkout: `/Users/mtajchert/coding/tmp/goose`

## Goal

Bring Android WHOOP 5.0/MG closer to full local history support. Live HR already works, but Today,
Sleep, Recovery, and similar tabs need historical body packets to populate deeper metrics.

Do not open a PR until this is validated on real hardware.

## What Changed

- Added a narrow WHOOP 5 command policy so Android only sends commands we are actively validating:
  `TOGGLE_REALTIME_HR`, `RUN_HAPTICS_PATTERN`, `GET_DATA_RANGE`, `SEND_HISTORICAL_DATA`, and
  `HISTORICAL_DATA_RESULT`.
- Added a historical-sync start policy so sync only starts when connected, bonded, session-ready, and
  not already backfilling.
- Added a family-aware historical frame classifier:
  - WHOOP 5 metadata and historical stream offsets use the Puffin envelope offset.
  - Plain WHOOP 5 `EVENT` frames are not treated as historical progress.
  - WHOOP 5 `HISTORY_END` ack data is extracted from the Puffin offset.
- Updated WHOOP 5 frame parsing:
  - `METADATA` decodes at Puffin offsets.
  - `EVENT` decodes at Puffin offsets.
  - `COMMAND_RESPONSE` decodes Goose-style response fields: command, sequence, result.
- Updated the WHOOP 5 Puffin command frame builder to match Goose:
  - Inner command payload is padded to a 4-byte boundary before CRC32.
  - This fixed the previously ignored historical commands.
- Added tests for:
  - WHOOP 5 command allowlist and empty historical payload policy.
  - Historical sync start gating.
  - WHOOP 5 metadata/event parsing and history-end ack extraction.
  - Goose-compatible exact bytes for empty `GET_DATA_RANGE` and `SEND_HISTORICAL_DATA`.

## Goose Findings Used

Relevant Goose files:

- `GooseSwift/GooseBLEClient.swift`
- `GooseSwift/GooseBLEClient+HistoricalCommands.swift`
- `GooseSwift/GooseBLEClient+HistoricalHandlers.swift`
- `GooseSwift/GooseBLEClient+Parsing.swift`

Important Goose behavior:

- `GET_DATA_RANGE` command `34`, payload `[]`.
- `SEND_HISTORICAL_DATA` command `22`, payload `[]`.
- `HISTORICAL_DATA_RESULT` command `23`, default payload `[1,0,0,0,0,0,0,0,0]`, but history-end
  ack uses `[1] + end_data`.
- `buildV5CommandFrame` builds `[type=35, sequence, command] + payload`, pads that inner payload to
  a 4-byte boundary, then calculates CRC32 over the padded bytes.
- Goose waits for `GET_DATA_RANGE` command response before `SEND_HISTORICAL_DATA`; Android currently
  sends `SEND_HISTORICAL_DATA` after a short delay.

Known exact Goose-compatible frames now covered by tests:

```text
GET_DATA_RANGE seq=1 empty payload:
aa0108000001e67123012200dbf3b335

SEND_HISTORICAL_DATA seq=2 empty payload:
aa0108000001e6712302160075bedf8c
```

## Hardware Validation So Far

After installing the padded-frame build, the band accepted the historical command sequence:

```text
23:26:59 WHOOP 5/MG: CLIENT_HELLO acked
23:27:01 -> GET_DATA_RANGE payload= (puffin)
23:27:01 COMMAND_RESPONSE GET_DATA_RANGE seq=2 result=PENDING
23:27:01 COMMAND_RESPONSE GET_DATA_RANGE seq=2 result=SUCCESS
23:27:01 -> SEND_HISTORICAL_DATA payload= (puffin)
23:27:02 COMMAND_RESPONSE SEND_HISTORICAL_DATA seq=3 result=SUCCESS
```

The band then streamed many metadata chunks:

```text
Backfill: inbound METADATA ... meta_type=HISTORY_START
Backfill: inbound METADATA ... meta_type=HISTORY_END ... trim_cursor=...
-> HISTORICAL_DATA_RESULT payload=...
Backfill: acked chunk trim=...
COMMAND_RESPONSE HISTORICAL_DATA_RESULT ... result=SUCCESS
```

This validates that:

- `CLIENT_HELLO` still works with the current subscribe-first ordering.
- The padded Puffin historical command frames are accepted.
- `GET_DATA_RANGE` command responses are now parsed correctly.
- `SEND_HISTORICAL_DATA` is sent and accepted.
- `HISTORY_END` acknowledgement payloads are accepted by the band.

## Current Gap

The sync still does not populate deeper app data.

Packet-type summary from the successful backfill attempt:

```text
CONSOLE_LOGS       5827
EVENT              1504
METADATA            268
type54              165
COMMAND_RESPONSE    141
```

No packet logs were seen for:

```text
HISTORICAL_DATA                 type 47
HISTORICAL_IMU_DATA_STREAM      type 52
```

Database snapshot after the sync:

```text
hrSample         7
event            1
rrInterval       0
battery          0
spo2Sample       0
skinTempSample   0
respSample       0
gravitySample    0
dailyMetric      0
sleepSession     0
```

So the command path is working, but Android is not yet receiving or decoding the expected historical
body records that feed Today/Sleep/Recovery.

## Suspicious Packet Type

During backfill, Android sees many `type54` frames, for example:

```text
aa0114000102a070360d02002a55ab696f5d040001ed0100743800d9
aa0114000102a070361102003355ab69ad07040001ef01006ffb84f5
```

Goose's visible `V5PacketType` enum currently defines historical body packets as `47` and `52`; it
does not obviously name packet type `54`. `type54` may be a WHOOP 5-specific stream we need to
classify/decode, or it may be unrelated telemetry that happens during backfill.

## Next Steps

## 2026-06-10 Hardware Note

After repeated Android `status=133` failures on the first protected GATT operation, the tested
WHOOP 5.0 band succeeded when NOOP connected directly to Android's already-bonded WHOOP device
instead of scan-only reconnecting.

Observed sequence:

```text
Connecting directly to bonded WHOOP 5.0 / MG WHOOP 5AG0081917
WHOOP 5/MG: CLIENT_HELLO acked
Subscribed fd4b0003...
Subscribed fd4b0004...
Subscribed fd4b0005...
Subscribed fd4b0007...
-> GET_DATA_RANGE
COMMAND_RESPONSE GET_DATA_RANGE result=PENDING
COMMAND_RESPONSE GET_DATA_RANGE result=SUCCESS
-> SEND_HISTORICAL_DATA
COMMAND_RESPONSE SEND_HISTORICAL_DATA result=SUCCESS
-> HISTORICAL_DATA_RESULT ... result=SUCCESS
```

The first direct connect attempt can still time out with `Confirmed write failed: status=133`.
The automatic reconnect now reuses the bonded device and the second attempt reached the working
Puffin session. This is still a hardware-tested workaround, not a final root-cause fix.

## 2026-06-10 Follow-up: Range-Gated Transfer and No-Trim Trial

Implemented and hardware-tested two safer history-sync behaviors:

```text
GET_DATA_RANGE is sent first.
SEND_HISTORICAL_DATA is sent only after GET_DATA_RANGE SUCCESS.
WHOOP 5 HISTORY_END trim ACK is skipped while bodyPackets=0.
```

Two-minute hardware result:

```text
GET_DATA_RANGE attempt 1: no response before idle retry.
GET_DATA_RANGE attempt 2: PENDING, then SUCCESS.
SEND_HISTORICAL_DATA: SUCCESS.
HISTORY_END trim=4512 repeated.
Trim ACK skipped each time because bodyPackets=0.
Final summary: timeout, bodyPackets=0, counts=COMMAND_RESPONSE=5, CONSOLE_LOGS=9, EVENT=1, METADATA=6, REALTIME_DATA=16.
unknown samples=none.
```

Goose comparison:

- Goose also gates `SEND_HISTORICAL_DATA` on a valid `GET_DATA_RANGE SUCCESS`.
- Goose treats `GET_DATA_RANGE PENDING` as pending and waits for a final response.
- Goose has an `acknowledgeHistoricalDataResult` switch.
- When that switch is disabled, Goose suppresses ACKs only after historical packets were received;
  for metadata-only history, Goose still sends the result ACK and records it as metadata-only.
- NOOP's current no-trim trial is stricter: it suppresses WHOOP 5 ACKs until body packets appear.
  Hardware showed the strap then repeats the same HISTORY_END cursor and eventually times out.

## Next Steps

1. Resolve why this hardware run reports zero historical data packets.
   - The command and metadata sequence is correct, but the strap console reports:
     `PullStats: Data: 0, Events: 82`.
   - Check whether the app must set/sync the strap clock before history, because the firmware repeatedly
     logs `RTC timestamp ... is invalid; not saving data to flash`.
   - Check whether the requested history window is empty on this band, or whether data packets require
     an additional WHOOP 5 command/subscription from Goose/OpenWhoop.
   - Re-run after the band has worn history and compare `PullStats` across runs.

2. Pull the WHOOP 5 raw backfill capture for offline analysis.
   - Android now writes each WHOOP 5 backfill frame to app-private JSONL while a backfill session is
     active: `files/whoop5-backfill-capture.jsonl`.
   - Pull it after a hardware run with:
     `adb exec-out run-as com.noop.whoop.debug cat files/whoop5-backfill-capture.jsonl > whoop5-backfill-capture.jsonl`
   - The file is bounded to 20,000 lines and is overwritten at the start of each WHOOP 5 backfill
     session.

### 2026-06-10 raw capture result

Pulled local artifact: `whoop5-backfill-capture.jsonl` (not committed; hardware evidence only).

The capture contained 89 frames:

```text
COMMAND_RESPONSE 7
CONSOLE_LOGS     64
EVENT            8
METADATA         9
REALTIME_DATA    1
UNKNOWN          0
```

Command sequence was correct:

```text
GET_DATA_RANGE          PENDING
GET_DATA_RANGE          SUCCESS
SEND_HISTORICAL_DATA    SUCCESS
HISTORICAL_DATA_RESULT  SUCCESS x4
```

Metadata sequence was also correct:

```text
HISTORY_START trim=82
HISTORY_END   trim=4517
HISTORY_START trim=82
HISTORY_END   trim=4525
HISTORY_START trim=82
HISTORY_END   trim=4533
HISTORY_START trim=82
HISTORY_END   trim=4535
HISTORY_COMPLETE
```

The decisive firmware console line was:

```text
BLE: PullStats: Data: 0, Events: 82, Bytes: 6120, Seconds: 3.530, Brate: 1733.7, Prate: 23.2
```

This means the strap accepted the history request and sent event/metadata history, but reported zero
data packets for that transfer. That explains why Today/Sleep/Recovery stayed empty: there were no
type-47/type-52/K-packet body records to persist in this run.

Parser follow-up:

- Android now decodes WHOOP 5 `CONSOLE_LOGS.console_text`.
- Android now preserves WHOOP 5 `EVENT` payload bytes as `event_payload_hex` and `event_payload_u8`.
- Tests use real frames from this hardware capture.

### 2026-06-10 GATT-safe clock capture result

After moving WHOOP 5 clock sync behind the Puffin CCCD subscription drain, logcat confirmed the
commands were accepted by the strap:

```text
BLE_CMD: Command Set Clock {1781047476, 0}
BLE_CMD: Command Get Clock {1781047476, 327}
```

This changed the history behavior materially: the strap emitted real `HISTORICAL_DATA` frames.

```text
COMMAND_RESPONSE 3
CONSOLE_LOGS     31
EVENT            6
HISTORICAL_DATA  2
METADATA         6
REALTIME_DATA    70
```

The session then timed out because Android suppressed the first `HISTORY_END` ACK after seeing body
packets:

```text
Backfill: suppress trim ack=4557 for WHOOP5; bodyPackets=2
Backfill: WHOOP 5/MG summary reason=timeout attempts=1 bodyPackets=2
```

Next implementation step: ACK WHOOP 5 `HISTORY_END` even when body packets were seen, matching the
normal Goose/default transfer flow. The raw JSONL capture is retained locally before ACKing so we have
evidence for parser work.

### 2026-06-10 ACK-enabled capture result

After enabling WHOOP 5 `HISTORY_END` ACKs even after body packets, the transfer advanced through
multiple trim cursors and reached `HISTORY_COMPLETE`.

Pulled local artifact: `whoop5-backfill-capture-after-ack-enabled.jsonl` (not committed).

```text
COMMAND_RESPONSE 8
CONSOLE_LOGS     48
EVENT            8
HISTORICAL_DATA  246
METADATA         11
REALTIME_DATA    1
```

ACK responses were accepted for five history chunks:

```text
HISTORICAL_DATA_RESULT SUCCESS seq=6
HISTORICAL_DATA_RESULT SUCCESS seq=7
HISTORICAL_DATA_RESULT SUCCESS seq=8
HISTORICAL_DATA_RESULT SUCCESS seq=9
HISTORICAL_DATA_RESULT SUCCESS seq=10
```

Metadata progressed:

```text
HISTORY_START trim=82
HISTORY_END   trim=4558
HISTORY_START trim=82
HISTORY_END   trim=4562
HISTORY_START trim=82
HISTORY_END   trim=4566
HISTORY_START trim=82
HISTORY_END   trim=4570
HISTORY_START trim=82
HISTORY_END   trim=4574
HISTORY_COMPLETE
```

This validates the current transfer sequencing on real WHOOP 5 hardware:

1. `CLIENT_HELLO`
2. subscribe Puffin notifications
3. `SET_CLOCK` / `GET_CLOCK`
4. `GET_DATA_RANGE`
5. `SEND_HISTORICAL_DATA`
6. ACK each `HISTORY_END`

Remaining blocker: Android captures the WHOOP 5 `HISTORICAL_DATA` frames but does not yet decode those
body packets into the persisted health streams that Today/Sleep/Recovery consume.

3. Keep command sequencing aligned with Goose.
   - Android now waits for `GET_DATA_RANGE SUCCESS` before sending `SEND_HISTORICAL_DATA`.
   - The 2026-06-10 capture confirms this sequence on hardware.

4. Decode WHOOP 5 `HISTORICAL_DATA` body packets.
   - ACK-enabled hardware run produced 246 `HISTORICAL_DATA` frames and `HISTORY_COMPLETE`.
   - Compare the 124-byte frames against Goose's K-packet parser and add Android decode/persistence.

5. Once body records decode and persist, validate UI data flow.
   - Confirm rows appear in `hrSample`, `rrInterval`, `skinTempSample`, `respSample`, and
     `gravitySample` as applicable.
   - Confirm Today/Sleep/Recovery move from "No Data" on-device without a WHOOP export import.

6. Only after successful real-hardware validation, prepare a PR/MR.

## Verification Run

Commands run successfully after current changes:

```text
./gradlew :app:testFullDebugUnitTest
./gradlew :app:assembleFullDebug
adb install -r android/app/build/outputs/apk/full/debug/app-full-debug.apk
```

The final installed build produced the successful command-response evidence above.
