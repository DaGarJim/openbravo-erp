#!/bin/bash

# Script de setup inicial completo
# Entorno de Demo: Openbravo ERP + Oracle XE + SQLcl MCP
# Autor: GitHub Copilot Agent
# Fecha: 24 de julio de 2025

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# ASCII Art para el banner
show_banner() {
    echo -e "${PURPLE}"
    cat << 'EOF'
 ___                 _                          _____ ____  ____  
/ _ \ _ __   ___ _ __| |__  _ __ __ ___   _____ | ____|  _ \|  _ \ 
| | | | '_ \ / _ \ '_ \ '_ \| '__/ _` \ \ / / _ \|  _| | |_) | |_) |
| |_| | |_) |  __/ | | |_) | | | (_| |\ V / (_) | |___|  _ <|  __/ 
 \___/| .__/ \___|_| |_.__/|_|  \__,_| \_/ \___/|_____|_| \_\_|   
      |_|                                                         
          + Oracle XE + SQLcl MCP Demo Environment
EOF
    echo -e "${NC}"
}

# Función de logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

# Función para verificar requisitos
check_requirements() {
    log "🔍 Verificando requisitos del sistema..."
    
    local missing_deps=()
    
    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        missing_deps+=("Docker")
    elif ! docker info &> /dev/null; then
        error "Docker está instalado pero no está ejecutándose."
        info "Por favor, inicia Docker Desktop o el daemon de Docker."
        exit 1
    fi
    
    # Verificar Docker Compose
    if ! docker compose version &> /dev/null; then
        missing_deps+=("Docker Compose")
    fi
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        missing_deps+=("Java JDK")
    fi
    
    # Verificar Ant
    if ! command -v ant &> /dev/null; then
        missing_deps+=("Apache Ant")
    fi
    
    # Verificar Git
    if ! command -v git &> /dev/null; then
        missing_deps+=("Git")
    fi
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        error "❌ Faltan dependencias requeridas:"
        for dep in "${missing_deps[@]}"; do
            error "   - $dep"
        done
        echo
        info "📖 Consulta el README.md para instrucciones de instalación."
        exit 1
    fi
    
    log "✅ Todos los requisitos están instalados."
    
    # Mostrar versiones
    info "📋 Versiones detectadas:"
    info "   Docker: $(docker --version | cut -d' ' -f3 | tr -d ',')"
    info "   Docker Compose: $(docker compose version --short)"
    info "   Java: $(java -version 2>&1 | head -n1 | cut -d' ' -f3 | tr -d '\"')"
    info "   Ant: $(ant -version | cut -d' ' -f4)"
    info "   Git: $(git --version | cut -d' ' -f3)"
}

# Función para compilar Openbravo
compile_openbravo() {
    log "🏗️ Compilando Openbravo ERP..."
    
    if [ ! -f "build.xml" ]; then
        error "❌ No se encontró build.xml. ¿Estás en el directorio correcto?"
        exit 1
    fi
    
    # Ejecutar ant setup
    log "Ejecutando 'ant setup'..."
    if ant setup; then
        log "✅ 'ant setup' completado exitosamente."
    else
        error "❌ Error en 'ant setup'."
        exit 1
    fi
    
    # Ejecutar ant install.source
    log "Ejecutando 'ant install.source'..."
    if ant install.source; then
        log "✅ 'ant install.source' completado exitosamente."
    else
        error "❌ Error en 'ant install.source'."
        exit 1
    fi
    
    # Verificar que se generaron archivos necesarios
    if [ -d "src-db/database" ]; then
        log "✅ Esquema de base de datos generado correctamente."
    else
        warn "⚠️  Directorio src-db/database no encontrado."
    fi
}

# Función para configurar Docker
setup_docker() {
    log "🐳 Configurando entorno Docker..."
    
    if [ ! -f "docker-compose.yml" ]; then
        error "❌ No se encontró docker-compose.yml."
        exit 1
    fi
    
    # Verificar que las imágenes base estén disponibles
    log "Descargando imagen de Oracle XE (esto puede tardar varios minutos)..."
    if docker pull container-registry.oracle.com/database/express:latest; then
        log "✅ Imagen de Oracle XE descargada."
    else
        error "❌ Error al descargar imagen de Oracle XE."
        info "Verifica tu conexión a internet y que tienes acceso a Oracle Container Registry."
        exit 1
    fi
    
    # Construir imagen SQLcl MCP
    log "Construyendo imagen SQLcl MCP..."
    if docker compose build sqlcl-mcp; then
        log "✅ Imagen SQLcl MCP construida exitosamente."
    else
        error "❌ Error al construir imagen SQLcl MCP."
        exit 1
    fi
    
    # Iniciar servicios
    log "Iniciando servicios Docker..."
    if docker compose up -d; then
        log "✅ Servicios Docker iniciados."
    else
        error "❌ Error al iniciar servicios Docker."
        exit 1
    fi
    
    # Esperar que los servicios estén listos
    log "⏳ Esperando que los servicios se inicialicen..."
    sleep 30
    
    # Verificar estado de contenedores
    if docker ps | grep -q "openbravo-oracle-xe"; then
        log "✅ Contenedor Oracle XE ejecutándose."
    else
        error "❌ Contenedor Oracle XE no está ejecutándose."
        docker logs openbravo-oracle-xe | tail -20
        exit 1
    fi
}

# Función para cargar esquema
load_schema() {
    log "📋 Cargando esquema Openbravo en Oracle..."
    
    if [ ! -f "scripts/01_load_openbravo.sh" ]; then
        error "❌ Script de carga no encontrado: scripts/01_load_openbravo.sh"
        exit 1
    fi
    
    # Hacer el script ejecutable por si acaso
    chmod +x scripts/01_load_openbravo.sh
    
    # Ejecutar script de carga
    if ./scripts/01_load_openbravo.sh; then
        log "✅ Esquema Openbravo cargado exitosamente."
    else
        error "❌ Error al cargar esquema Openbravo."
        exit 1
    fi
}

# Función para verificar entorno
verify_environment() {
    log "✅ Verificando entorno completo..."
    
    if [ ! -f "scripts/02_verify_environment.sh" ]; then
        error "❌ Script de verificación no encontrado: scripts/02_verify_environment.sh"
        exit 1
    fi
    
    # Hacer el script ejecutable por si acaso
    chmod +x scripts/02_verify_environment.sh
    
    # Ejecutar verificación
    if ./scripts/02_verify_environment.sh; then
        log "✅ Verificación completada exitosamente."
        return 0
    else
        error "❌ La verificación encontró problemas."
        return 1
    fi
}

# Función para mostrar resumen final
show_summary() {
    echo
    log "🎉 ¡SETUP COMPLETADO EXITOSAMENTE!"
    echo
    info "📊 Tu entorno Openbravo ERP + Oracle XE + SQLcl MCP está listo."
    echo
    info "🔗 Conexiones disponibles:"
    info "   Oracle XE:     sqlplus openbravo/ob_pwd@//localhost:1521/XEPDB1"
    info "   Oracle EM:     http://localhost:5500/em"
    info "   SQLcl MCP:     http://localhost:8080"
    echo
    info "🤖 Para usar con GitHub Copilot:"
    info "   1. Abre VS Code en este directorio"
    info "   2. Asegúrate de que GitHub Copilot esté habilitado"
    info "   3. Usa comandos en lenguaje natural como:"
    info "      - 'Muéstrame los 5 clientes con mayor límite de crédito'"
    info "      - '¿Cuántos productos tenemos registrados?'"
    info "      - 'Dame un resumen de clientes por organización'"
    echo
    info "🛠️ Comandos útiles:"
    info "   Verificar entorno:     ./scripts/02_verify_environment.sh"
    info "   Reset suave:           ./scripts/03_reset_environment.sh --soft"
    info "   Reset completo:        ./scripts/03_reset_environment.sh --full"
    info "   Ver logs Oracle:       docker logs -f openbravo-oracle-xe"
    info "   Ver logs SQLcl:        docker logs -f openbravo-sqlcl-mcp"
    echo
    info "📖 Para más información, consulta el README.md"
    echo
}

# Función para manejo de errores
cleanup_on_error() {
    error "❌ Setup falló. Limpiando recursos..."
    
    # Parar contenedores si están ejecutándose
    docker compose down 2>/dev/null || true
    
    # Mostrar logs para debugging
    echo
    error "🔍 Logs de debugging:"
    if docker ps -a | grep -q "openbravo-oracle-xe"; then
        error "--- Logs Oracle XE ---"
        docker logs openbravo-oracle-xe | tail -20
    fi
    
    if docker ps -a | grep -q "openbravo-sqlcl-mcp"; then
        error "--- Logs SQLcl MCP ---"
        docker logs openbravo-sqlcl-mcp | tail -20
    fi
    
    echo
    error "Para más ayuda:"
    error "  1. Revisa el README.md"
    error "  2. Ejecuta './scripts/02_verify_environment.sh' para diagnosticar"
    error "  3. Usa './scripts/03_reset_environment.sh --full' para limpiar completamente"
    
    exit 1
}

# Función principal
main() {
    # Configurar trap para cleanup en caso de error
    trap cleanup_on_error ERR
    
    show_banner
    
    log "🚀 Iniciando setup completo del entorno Openbravo ERP + Oracle XE + SQLcl MCP"
    log "Fecha: $(date)"
    log "Directorio: $(pwd)"
    echo
    
    # Estimar tiempo
    info "⏱️  Tiempo estimado: 15-30 minutos (dependiendo de la velocidad de descarga)"
    echo
    
    # Ejecutar pasos del setup
    check_requirements
    echo
    
    compile_openbravo
    echo
    
    setup_docker
    echo
    
    load_schema
    echo
    
    if verify_environment; then
        show_summary
    else
        warn "⚠️  El setup se completó pero la verificación encontró algunos problemas."
        warn "Revisa los mensajes anteriores y ejecuta:"
        warn "  ./scripts/02_verify_environment.sh"
        warn "para más detalles."
    fi
}

# Verificar que estamos en el directorio correcto
if [ ! -f "build.xml" ] || [ ! -f "docker-compose.yml" ]; then
    error "❌ Este script debe ejecutarse desde el directorio raíz del proyecto openbravo-erp."
    error "Directorio actual: $(pwd)"
    error "Archivos esperados: build.xml, docker-compose.yml"
    exit 1
fi

# Ejecutar función principal
main "$@"
