#!/usr/bin/env sh
set -e
for d in "$HOME"/.gradle/wrapper/dists/gradle-8.4-bin/*; do
  if [ -x "$d/gradle-8.4/bin/gradle" ]; then exec "$d/gradle-8.4/bin/gradle" "$@"; fi
done
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "[mo.co hub] Gradle 8.4 não encontrado. Faça o Gradle Sync no Android Studio e tente novamente." >&2
exit 1
