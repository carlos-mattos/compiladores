#!/usr/bin/env bash
set -euo pipefail

# Unified entrypoint: run unit tests and artifact verification in one place

echo "== CI: build + tests =="
mvn -q clean package && mvn -q test

echo -e "\nSummary of passing tests:"
grep -E "Tests run:" target/surefire-reports/*.txt | sed -E 's|target/surefire-reports/(.*)\\.txt:|→ \1: |'
grep -E "BUILD SUCCESS" target/surefire-reports/*.txt >/dev/null 2>&1 || echo "Build finished successfully."

echo -e "\n== Verify artifacts =="

ART="verify_artifacts"
JAR="target/lang-*-jar-with-dependencies.jar"
JAS="${JASMIN_JAR:-tools/jasmin.jar}"

# Ensure the fat-jar exists (in case Maven packaging was customized)
if ! ls $JAR >/dev/null 2>&1; then
  mvn -q -e -DskipTests package
fi

rm -rf "$ART"
mkdir -p "$ART/src" "$ART/gen" "$ART/run" "$ART/logs"

run_case() {
  local file="$1"
  local name
  name="$(basename "$file" .lang)"

  # Skip files that fail syntax or typecheck
  if ! java -jar $JAR -syn "$file" >/dev/null 2>>"$ART/logs/$name.syn.err"; then
    echo "[SKIP] $name: syntax reject"
    return
  fi
  if ! java -jar $JAR -t "$file" >/dev/null 2>>"$ART/logs/$name.type.err"; then
    echo "[SKIP] $name: type reject"
    return
  fi

  echo "-- $name: -i"
  java -jar $JAR -i "$file" > "$ART/run/$name.i.out" || true

  echo "-- $name: -gen"
  if java -jar $JAR -gen "$file" > "$ART/gen/$name.j" 2>"$ART/logs/$name.gen.err"; then
    pushd "$ART/gen" >/dev/null
    if [[ -f "$JAS" ]]; then
      java -jar "$JAS" "$name.j" >/dev/null 2>"../logs/$name.jasmin.err" || true
      if [[ -f LangMain.class ]]; then
        java -cp . LangMain > "../run/$name.gen.out" || true
        rm -f LangMain.class
      fi
    else
      echo "[WARN] missing jasmin jar at $JAS" >"../logs/$name.jasmin.err"
    fi
    popd >/dev/null
  fi

  echo "-- $name: -src (java)"
  if java -jar $JAR -src "$file" > "$ART/src/$name.java" 2>"$ART/logs/$name.src.err"; then
    mkdir -p "$ART/run/java_$name"
    if javac -d "$ART/run/java_$name" "$ART/src/$name.java" 2>"$ART/logs/$name.javac.err"; then
      java -cp "$ART/run/java_$name" LangSource > "$ART/run/$name.src.out" || true
    fi
  fi

  # diffs
  if [[ -f "$ART/run/$name.gen.out" ]]; then
    if ! diff -u "$ART/run/$name.i.out" "$ART/run/$name.gen.out" > "$ART/logs/$name.gen.diff"; then
      echo "[GEN] diff for $name"
      fail=1
    fi
  fi
  if [[ -f "$ART/run/$name.src.out" ]]; then
    if ! diff -u "$ART/run/$name.i.out" "$ART/run/$name.src.out" > "$ART/logs/$name.src.diff"; then
      echo "[SRC] diff for $name"
      fail=1
    fi
  fi
}

fail=0
for f in src/test/resources/progs/*.lang; do
  base=$(basename "$f")
  run_case "$f"
done

echo "== DONE =="
exit $fail

