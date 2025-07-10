#!/usr/bin/env bash
set -euo pipefail

mvn -q clean package && mvn -q test

echo -e "\nResumo dos testes aprovados:"
grep -E "Tests run:" target/surefire-reports/*.txt | sed -E 's|target/surefire-reports/(.*)\\.txt:|→ \1: |'
grep -E "BUILD SUCCESS" target/surefire-reports/*.txt >/dev/null 2>&1 || echo "Build finalizado com sucesso." 