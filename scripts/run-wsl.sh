#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v java >/dev/null 2>&1; then
  echo "[ERRO] JDK 17/21 não encontrado no Linux/WSL."
  echo "Instale com: sudo apt-get update && sudo apt-get install -y openjdk-21-jdk"
  exit 1
fi

if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
  echo "[AVISO] DISPLAY e WAYLAND_DISPLAY não definidos. Verifique se o WSLg está ativo no Windows 11."
fi

chmod +x ./gradlew
echo "[Controle de Poço 2] Iniciando interface Desktop (Linux/WSL)..."
exec ./gradlew :desktopApp:run --console=plain "$@"
