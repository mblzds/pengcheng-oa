#!/usr/bin/env bash
# Local verification matrix for the active development branch.
# Usage:
#   bash scripts/verify-local.sh        # fast checks
#   bash scripts/verify-local.sh --full # full Maven tests + frontend build

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-fast}"

run() {
  echo
  echo "========== $* =========="
  "$@"
}

cd "$ROOT"

run git diff --check

if [[ "$MODE" == "--full" ]]; then
  run mvn -q test
else
  run mvn -q -pl pengcheng-common,pengcheng-infra/pengcheng-db,pengcheng-infra/pengcheng-pay,pengcheng-core/pengcheng-system,pengcheng-core/pengcheng-realty -am test
fi

(
  cd "$ROOT/pengcheng-ui"
  run npm run typecheck
  if [[ "$MODE" == "--full" ]]; then
    run npm run build
  fi
)

(
  cd "$ROOT/pengcheng-uniapp"
  run npm run verify
)

if [[ -f "$ROOT/pengcheng-infra/pengcheng-sms/src/test/java/com/pengcheng/sms/SmsServiceTest.java" ]]; then
  run mvn -q -pl pengcheng-infra/pengcheng-sms dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=/tmp/pengcheng-sms-test.cp
  run java -cp "$ROOT/pengcheng-infra/pengcheng-sms/target/test-classes:$ROOT/pengcheng-infra/pengcheng-sms/target/classes:$ROOT/pengcheng-core/pengcheng-system/target/classes:$(cat /tmp/pengcheng-sms-test.cp)" com.pengcheng.sms.SmsServiceTest
fi

if [[ -f "$ROOT/pengcheng-infra/pengcheng-push/src/test/java/com/pengcheng/push/PushServiceTest.java" ]]; then
  run mvn -q -pl pengcheng-infra/pengcheng-push dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=/tmp/pengcheng-push-test.cp
  run java -cp "$ROOT/pengcheng-infra/pengcheng-push/target/test-classes:$ROOT/pengcheng-infra/pengcheng-push/target/classes:$(cat /tmp/pengcheng-push-test.cp)" com.pengcheng.push.PushServiceTest
fi

echo
echo "Verification completed: $MODE"
