#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

chmod +x ./gradlew

echo "==> 1/3 Executando testes (:core:test e :desktopApp:test)..."
./gradlew :core:test :desktopApp:test --no-daemon --stacktrace

echo "==> 2/3 Gerando Uber JAR Linux (Compose Desktop)..."
./gradlew :desktopApp:packageUberJarForCurrentOS --no-daemon --stacktrace

if [[ "${1:-}" == "--package" ]]; then
  echo "==> 3/3 Gerando pacote Linux distributable / .deb..."
  ./gradlew :desktopApp:createDistributable :desktopApp:packageDeb --no-daemon --stacktrace
else
  echo "==> 3/3 Gerando imagem distributable Linux (use --package para gerar também .deb)..."
  ./gradlew :desktopApp:createDistributable --no-daemon --stacktrace
fi

echo "[OK] Build Linux/WSL concluído em desktopApp/build/compose/"
