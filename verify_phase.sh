#!/bin/bash
set -e

echo "🚀 Iniciando verificación de Fase..."

# Paso 1: Verificar el formato y estilo (opcional, si hay ktlint o similar)
# ./gradlew ktlintCheck

# Paso 2: Ejecutar tests unitarios de módulos críticos
echo "🧪 Corriendo tests unitarios..."
./gradlew testDebugUnitTest --continue

# Paso 3: Verificar compilación completa del proyecto
echo "🔨 Construyendo el proyecto (Sync + Build)..."
./gradlew assembleDebug

# Paso 4: (Opcional) Correr lint
echo "🧹 Ejecutando Lint..."
./gradlew lintDebug

echo "✅ Verificación exitosa. El código de la Fase está listo para Handoff."
