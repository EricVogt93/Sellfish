#!/usr/bin/env bash
# SessionStart-Hook für Claude Code on the web.
# Wärmt die Build-Caches, damit Tests/Linter in der Session sofort laufen.
set -euo pipefail

echo "[setup] Bewerbungsatze – Umgebung vorbereiten"

# --- Backend: Maven-Dependencies vorab laden (nur wenn Maven da ist) ---
if command -v mvn >/dev/null 2>&1 && [ -f backend/pom.xml ]; then
  echo "[setup] Maven dependency:go-offline …"
  (cd backend && mvn -B -q dependency:go-offline >/dev/null 2>&1) || \
    echo "[setup] Hinweis: go-offline unvollständig (Netzwerk?). Build lädt bei Bedarf nach."
fi

# --- Frontend: npm-Dependencies installieren ---
if command -v npm >/dev/null 2>&1 && [ -f frontend/package.json ]; then
  echo "[setup] npm install (frontend) …"
  (cd frontend && (npm ci >/dev/null 2>&1 || npm install >/dev/null 2>&1)) || \
    echo "[setup] Hinweis: npm install fehlgeschlagen (Netzwerk?)."
fi

echo "[setup] Fertig."
