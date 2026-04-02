#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080/treat}"

echo "[STEP 1] C1/C2 登录与就诊人"
echo "请按 scripts/testing/c1-c2-api-checklist.md 执行并导出 PATIENT_TOKEN/DOCTOR_TOKEN"

echo "[STEP 2] C3/C4 排班与挂号"
echo "请按 scripts/testing/c3-c4-api-checklist.md 执行并导出 APPOINT_TOKEN/registrationId"

echo "[STEP 3] C5/C6 问诊与聊天"
echo "请按 scripts/testing/c5-c6-api-checklist.md 执行并导出 roomId"

echo "[STEP 4] C7/C8 病历与处方"
echo "请按 scripts/testing/c7-c8-api-checklist.md 执行"

echo "[STEP 5] C9/C10 + D/E"
echo "请按 scripts/testing/c9-c10-d-e-checklist.md 执行"

echo "回放顺序完成。请将输出记录到 test-execution-log.md"
