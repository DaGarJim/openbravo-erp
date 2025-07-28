#!/bin/bash

# Script de setup completo para Openbravo ERP + PostgreSQL + MCP
# Autor: GitHub Copilot Agent  
# Fecha: 28 de julio de 2025
# Migrado de Oracle a PostgreSQL para mejor compatibilidad

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Banner ASCII
show_banner() {
    echo -e "${CYAN}"
    cat << 'EOF'
 ██████╗ ██████╗ ███████╗███╗   ██╗██████╗ ██████╗  █████╗ ██╗   ██╗ ██████╗ 
██╔═══██╗██╔══██╗██╔════╝████╗  ██║██╔══██╗██╔══██╗██╔══██╗██║   ██║██╔═══██╗
██║   ██║██████╔╝█████╗  ██╔██╗ ██║██████╔╝██████╔╝███████║██║   ██║██║   ██║
██║   ██║██╔═══╝ ██╔══╝  ██║╚██╗██║██╔══██╗██╔══██╗██╔══██║╚██╗ ██╔╝██║   ██║
╚██████╔╝██║     ███████╗██║ ╚████║██████╔╝██║  ██║██║  ██║ ╚████╔╝ ╚██████╔╝
 ╚═════╝ ╚═╝     ╚══════╝╚═╝  ╚═══╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝   ╚═════╝ 
                                                                              
         🐘 PostgreSQL + 🤖 MCP Server + 📊 Tests Environment
                    ⚡ SETUP AUTOMATIZADO COMPLETO ⚡
EOF
    echo -e "${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}                    SETUP OPENBRAVO + POSTGRESQL + MCP                     ${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════${NC}"
    echo ""
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
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

success() {
    echo -e "${GREEN}✅${NC} $1"
}

# Variables globales de estado
DOCKER_OK=false
OPENBRAVO_COMPILED=false
POSTGRES_OK=false
SCHEMA_OK=false
MCP_OK=false

# Función para verificar requisitos del sistema
check_requirements() {
    log "🔍 Verificando requisitos del sistema..."
    
    local missing_deps=()
    
    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        missing_deps+=("docker")
    else
        if ! docker info &> /dev/null; then
            error "Docker está instalado pero no está ejecutándose"
            exit 1
        fi
        success "Docker está instalado y funcionando"
        DOCKER_OK=true
    fi
    
    # Verificar Docker Compose
    if ! command -v docker &> /dev/null || ! docker compose version &> /dev/null; then
        missing_deps+=("docker-compose")
    else
        success "Docker Compose está disponible"
    fi
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        missing_deps+=("java")
    else
        local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        success "Java está instalado (versión: $java_version)"
    fi
    
    # Verificar Ant
    if ! command -v ant &> /dev/null; then
        missing_deps+=("ant")
    else
        local ant_version=$(ant -version 2>&1 | head -n 1 | awk '{print $4}')
        success "Apache Ant está instalado (versión: $ant_version)"
    fi
    
    # Verificar psql (cliente PostgreSQL)
    if ! command -v psql &> /dev/null; then
        warn "psql no está instalado. Se instalará automáticamente si es necesario."
    else
        success "Cliente PostgreSQL (psql) está instalado"
    fi
    
    # Verificar Git
    if ! command -v git &> /dev/null; then
        missing_deps+=("git")
    else
        success "Git está instalado"
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        error "Faltan dependencias: ${missing_deps[*]}"
        echo ""
        echo "📋 Instrucciones de instalación:"
        echo ""
        for dep in "${missing_deps[@]}"; do
            case $dep in
                docker)
                    echo "  🐳 Docker: https://docs.docker.com/get-docker/"
                    ;;
                docker-compose)
                    echo "  🐙 Docker Compose: https://docs.docker.com/compose/install/"
                    ;;
                java)
                    echo "  ☕ Java: brew install openjdk@11 (macOS) o apt install openjdk-11-jdk (Ubuntu)"
                    ;;
                ant)
                    echo "  🐜 Apache Ant: brew install ant (macOS) o apt install ant (Ubuntu)"
                    ;;
                git)
                    echo "  📚 Git: https://git-scm.com/downloads"
                    ;;
            esac
        done
        echo ""
        exit 1
    fi
    
    success "Todos los requisitos están satisfechos"
}

# Función para compilar Openbravo
compile_openbravo() {
    log "🏗️ Compilando Openbravo ERP..."
    
    # Verificar si ya está compilado
    if [ -f "config/Openbravo.properties" ] && [ -d "build/classes" ]; then
        info "Openbravo ya está compilado. Saltando compilación..."
        OPENBRAVO_COMPILED=true
        return 0
    fi
    
    # Copiar configuración PostgreSQL
    if [ ! -f "config/Openbravo.properties" ]; then
        if [ -f "config/Openbravo.properties.postgres" ]; then
            log "Copiando configuración de PostgreSQL..."
            cp config/Openbravo.properties.postgres config/Openbravo.properties
        else
            log "Creando configuración básica de PostgreSQL..."
            cat > config/Openbravo.properties << 'EOF'
# Configuración básica para PostgreSQL
bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://localhost:5432/openbravo
bbdd.sid=openbravo
bbdd.systemUser=postgres
bbdd.systemPassword=tad
bbdd.user=tad
bbdd.password=tad
context.name=/openbravo
source.path=/Users/degr/Documents/GitHub/openbravo-erp
deploy.mode=class
EOF
        fi
    fi
    
    # Ejecutar ant setup
    log "Ejecutando 'ant setup'..."
    if ant setup; then
        success "ant setup completado"
    else
        error "Falló ant setup"
        return 1
    fi
    
    # Ejecutar ant install.source
    log "Ejecutando 'ant install.source'..."
    if ant install.source; then
        success "ant install.source completado"
        OPENBRAVO_COMPILED=true
    else
        error "Falló ant install.source"
        return 1
    fi
    
    success "Compilación de Openbravo completada"
}

# Función para iniciar servicios Docker
start_docker_services() {
    log "🐳 Iniciando servicios Docker..."
    
    # Verificar si docker-compose.yml existe
    if [ ! -f "docker-compose.yml" ]; then
        error "docker-compose.yml no encontrado"
        return 1
    fi
    
    # Iniciar servicios
    log "Levantando contenedores..."
    if docker compose up -d; then
        success "Contenedores iniciados correctamente"
    else
        error "Error al iniciar contenedores"
        return 1
    fi
    
    # Esperar que PostgreSQL esté listo
    log "Esperando a que PostgreSQL esté listo..."
    local max_attempts=60
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec openbravo-postgres pg_isready -U tad -d openbravo &> /dev/null; then
            success "PostgreSQL está listo!"
            POSTGRES_OK=true
            break
        fi
        
        warn "Intento $attempt/$max_attempts - PostgreSQL no está listo, esperando 5 segundos..."
        sleep 5
        ((attempt++))
        
        if [ $attempt -gt $max_attempts ]; then
            error "PostgreSQL no está disponible después de $max_attempts intentos"
            return 1
        fi
    done
}

# Función para cargar esquema
load_schema() {
    log "📊 Cargando esquema de Openbravo..."
    
    # Verificar que el script existe
    if [ ! -f "scripts/01_load_openbravo_postgres.sh" ]; then
        error "Script de carga no encontrado: scripts/01_load_openbravo_postgres.sh"
        return 1
    fi
    
    # Ejecutar script de carga
    if ./scripts/01_load_openbravo_postgres.sh; then
        success "Esquema cargado correctamente"
        SCHEMA_OK=true
    else
        error "Error al cargar esquema"
        return 1
    fi
}

# Función para verificar servidor MCP
check_mcp_server() {
    log "🤖 Verificando servidor MCP..."
    
    local mcp_url="http://localhost:8080"
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if command -v curl &> /dev/null; then
            if curl -s -f "$mcp_url/health" > /dev/null 2>&1 || curl -s "$mcp_url" > /dev/null 2>&1; then
                success "Servidor MCP está respondiendo en $mcp_url"
                MCP_OK=true
                return 0
            fi
        else
            # Sin curl, verificar solo el puerto
            if nc -z localhost 8080 2>/dev/null; then
                success "Puerto MCP (8080) está accesible"
                MCP_OK=true
                return 0
            fi
        fi
        
        warn "Intento $attempt/$max_attempts - MCP Server no responde, esperando 3 segundos..."
        sleep 3
        ((attempt++))
    done
    
    warn "Servidor MCP no está disponible (esto es opcional para tests básicos)"
    return 0
}

# Función para ejecutar verificación final
final_verification() {
    log "🔍 Ejecutando verificación final..."
    
    if [ -f "scripts/02_verify_environment_postgres.sh" ]; then
        ./scripts/02_verify_environment_postgres.sh
    else
        warn "Script de verificación no encontrado, ejecutando verificación básica..."
        
        # Verificación básica manual
        echo "📊 Verificación básica:"
        echo ""
        
        # Contenedores
        echo "🐳 Estado de contenedores:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(postgres|mcp|Names)"
        echo ""
        
        # Conectividad PostgreSQL
        echo "🗄️ Conectividad PostgreSQL:"
        if PGPASSWORD=tad psql -h localhost -p 5432 -U tad -d openbravo -c "SELECT version();" 2>/dev/null | head -1; then
            success "PostgreSQL está accesible"
        else
            error "PostgreSQL no está accesible"
        fi
        echo ""
    fi
}

# Función para mostrar resumen final
show_final_summary() {
    echo ""
    echo -e "${MAGENTA}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${MAGENTA}║                    SETUP COMPLETADO                          ║${NC}"
    echo -e "${MAGENTA}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    echo "📋 Estado de componentes:"
    [ "$DOCKER_OK" = true ] && success "Docker" || error "Docker"
    [ "$OPENBRAVO_COMPILED" = true ] && success "Openbravo compilado" || error "Openbravo compilado"
    [ "$POSTGRES_OK" = true ] && success "PostgreSQL" || error "PostgreSQL"
    [ "$SCHEMA_OK" = true ] && success "Esquema cargado" || error "Esquema cargado"
    [ "$MCP_OK" = true ] && success "Servidor MCP" || warn "Servidor MCP (opcional)"
    
    echo ""
    echo "🔗 Información de conexión:"
    echo "  📦 Base de datos: openbravo"
    echo "  🌐 PostgreSQL: localhost:5432"
    echo "  👤 Usuario: tad / Contraseña: tad"
    echo "  🤖 MCP Server: http://localhost:8080"
    echo ""
    echo "🚀 Comandos útiles:"
    echo "  psql postgresql://tad:tad@localhost:5432/openbravo    # Conectar a PostgreSQL"
    echo "  docker compose logs -f                               # Ver logs"
    echo "  ./scripts/02_verify_environment_postgres.sh          # Verificar entorno"
    echo "  ./scripts/03_reset_environment_postgres.sh --soft    # Reset suave"
    echo ""
    
    if [ "$DOCKER_OK" = true ] && [ "$POSTGRES_OK" = true ] && [ "$SCHEMA_OK" = true ]; then
        echo -e "${GREEN}🎉 ENTORNO LISTO PARA USAR CON GITHUB COPILOT${NC}"
        echo ""
        echo "✨ Casos de uso disponibles:"
        echo '  • "Muéstrame los 5 clientes con mayor límite de crédito"'
        echo '  • "Dame un resumen de productos disponibles"'
        echo '  • "Actualiza el límite de crédito del cliente BE-001 a 40000"'
        echo '  • "Genera un reporte de socios de negocio por organización"'
        echo ""
        return 0
    else
        echo -e "${RED}⚠️ SETUP INCOMPLETO - REVISA LOS ERRORES ANTERIORES${NC}"
        echo ""
        return 1
    fi
}

# Función para cleanup en caso de error
cleanup_on_error() {
    error "Setup interrumpido. Ejecutando limpieza..."
    
    # Parar contenedores si están corriendo
    docker compose down 2>/dev/null || true
    
    echo ""
    echo "🔧 Para reintentar el setup:"
    echo "  ./setup-postgres.sh"
    echo ""
    echo "🗑️ Para limpiar completamente:"
    echo "  ./scripts/03_reset_environment_postgres.sh --full"
    echo ""
}

# Función principal
main() {
    show_banner
    
    # Trap para cleanup en caso de error
    trap cleanup_on_error ERR
    
    # Verificar que estamos en el directorio correcto
    if [[ ! -f "docker-compose.yml" ]] || [[ ! -f "build.xml" ]]; then
        error "Este script debe ejecutarse desde el directorio raíz del proyecto Openbravo"
        exit 1
    fi
    
    # Ejecutar pasos del setup
    check_requirements
    compile_openbravo
    start_docker_services
    load_schema
    check_mcp_server
    final_verification
    show_final_summary
    
    # Si llegamos aquí, todo fue exitoso
    trap - ERR
}

# Ejecutar función principal con manejo de errores
if ! main "$@"; then
    exit 1
fi
