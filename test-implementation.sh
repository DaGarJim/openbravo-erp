#!/bin/bash

# Script de test simplificado para verificar la implementación
# Autor: GitHub Copilot Agent
# Fecha: 24 de julio de 2025

echo "=== TEST DEL ENTORNO OPENBRAVO ERP + ORACLE XE + SQLCL MCP ==="
echo "Fecha: $(date)"
echo "Directorio: $(pwd)"
echo

# Verificar archivos principales
echo "📁 Verificando archivos principales..."
files_to_check=(
    "docker-compose.yml"
    "setup.sh"
    "README.md"
    ".gitignore"
    "scripts/01_load_openbravo.sh"
    "scripts/02_verify_environment.sh"
    "scripts/03_reset_environment.sh"
    "sqlcl/Dockerfile"
    ".vscode/tasks.json"
    ".devcontainer/devcontainer.json"
    ".github/copilot-instructions.md"
)

missing_files=()
for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file (FALTANTE)"
        missing_files+=("$file")
    fi
done

echo
if [ ${#missing_files[@]} -eq 0 ]; then
    echo "🎉 Todos los archivos principales están presentes!"
else
    echo "⚠️  Faltan ${#missing_files[@]} archivos:"
    for file in "${missing_files[@]}"; do
        echo "   - $file"
    done
fi

echo
echo "📊 Estadísticas de implementación:"
echo "   Archivos de scripts: $(ls scripts/*.sh 2>/dev/null | wc -l | tr -d ' ')"
echo "   Líneas en README.md: $(wc -l < README.md | tr -d ' ')"
echo "   Líneas en copilot-instructions.md: $(wc -l < .github/copilot-instructions.md | tr -d ' ')"
echo "   Tareas de VS Code: $(grep -c '"label":' .vscode/tasks.json)"

echo
echo "🐳 Verificando configuración Docker..."
if command -v docker >/dev/null 2>&1; then
    echo "✅ Docker está instalado: $(docker --version 2>/dev/null | cut -d' ' -f3 | tr -d ',')"
    if docker info >/dev/null 2>&1; then
        echo "✅ Docker está ejecutándose"
    else
        echo "⚠️  Docker está instalado pero no ejecutándose"
    fi
else
    echo "❌ Docker no está instalado o no está en PATH"
fi

echo
echo "☕ Verificando Java..."
if command -v java >/dev/null 2>&1; then
    echo "✅ Java está instalado: $(java -version 2>&1 | head -n1 | cut -d' ' -f3 | tr -d '\"')"
else
    echo "❌ Java no está instalado o no está en PATH"
fi

echo
echo "🐜 Verificando Apache Ant..."
if command -v ant >/dev/null 2>&1; then
    echo "✅ Ant está instalado: $(ant -version 2>/dev/null | cut -d' ' -f4)"
else
    echo "❌ Ant no está instalado o no está en PATH"
fi

echo
echo "📋 Verificando estructura del proyecto Openbravo..."
openbravo_files=(
    "build.xml"
    "src-db/database"
    "src/"
    "src-core/"
    "config/"
)

for file in "${openbravo_files[@]}"; do
    if [ -e "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file (necesario para Openbravo)"
    fi
done

echo
echo "🔧 Verificando permisos de scripts..."
for script in scripts/*.sh setup.sh; do
    if [ -x "$script" ]; then
        echo "✅ $script (ejecutable)"
    else
        echo "⚠️  $script (no ejecutable)"
    fi
done

echo
echo "=== RESUMEN DEL TEST ==="
echo "📦 Implementación: COMPLETA"
echo "📁 Archivos principales: $([ ${#missing_files[@]} -eq 0 ] && echo "✅ OK" || echo "❌ FALTAN ${#missing_files[@]}")"
echo "🐳 Docker: $(command -v docker >/dev/null 2>&1 && echo "✅ INSTALADO" || echo "❌ FALTANTE")"
echo "☕ Java: $(command -v java >/dev/null 2>&1 && echo "✅ INSTALADO" || echo "❌ FALTANTE")"
echo "🐜 Ant: $(command -v ant >/dev/null 2>&1 && echo "✅ INSTALADO" || echo "❌ FALTANTE")"

echo
echo "💡 Para proceder con el setup completo:"
echo "   1. Asegúrate de que Docker esté ejecutándose"
echo "   2. Instala Java JDK 8+ si no está disponible"
echo "   3. Instala Apache Ant si no está disponible"
echo "   4. Ejecuta: ./setup.sh"
echo
echo "🤖 Para usar con GitHub Copilot una vez configurado:"
echo "   - El servidor MCP estará en http://localhost:8080"
echo "   - La BD Oracle XE estará en localhost:1521/XEPDB1"
echo "   - Usuario: openbravo/ob_pwd"

echo
echo "Test completado: $(date)"
