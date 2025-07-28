#!/bin/bash

# ============================================================================
# Script de Tests Rápido - Openbravo ERP + Oracle XE + SQLcl MCP
# ============================================================================

set -euo pipefail

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging con timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "${BLUE}🚀 Demo Rápido - Openbravo ERP + Oracle XE + SQLcl MCP${NC}"

# 1. Verificar Docker
log "${YELLOW}🔍 Verificando Docker...${NC}"
if ! docker --version > /dev/null 2>&1; then
    log "${RED}❌ Docker no está instalado o no está en el PATH${NC}"
    exit 1
fi

# 2. Levantar contenedores
log "${YELLOW}🐳 Levantando contenedores Docker...${NC}"
docker compose up -d

# 3. Esperar a que Oracle esté listo
log "${YELLOW}⏳ Esperando a que Oracle XE esté listo...${NC}"
timeout=300
counter=0
while [ $counter -lt $timeout ]; do
    if docker exec openbravo-oracle-xe sqlplus -L -S sys/oracle@XEPDB1 as sysdba <<< "SELECT 'Oracle is ready' FROM dual;" > /dev/null 2>&1; then
        log "${GREEN}✅ Oracle XE está listo!${NC}"
        break
    fi
    sleep 5
    counter=$((counter + 5))
    echo -n "."
done

if [ $counter -ge $timeout ]; then
    log "${RED}❌ Timeout esperando Oracle XE${NC}"
    exit 1
fi

# 4. Ejecutar script de carga básica
log "${YELLOW}📊 Ejecutando script de carga básica...${NC}"
if [ -f "./scripts/01_load_openbravo.sh" ]; then
    ./scripts/01_load_openbravo.sh
else
    log "${YELLOW}⚠️  Script de carga no encontrado, creando esquema mínimo...${NC}"
    
    # Crear esquema básico directamente
    docker exec openbravo-oracle-xe sqlplus -L -S sys/oracle@XEPDB1 as sysdba <<EOF
-- Create openbravo user
CREATE USER openbravo IDENTIFIED BY ob_pwd;
GRANT CONNECT, RESOURCE, DBA TO openbravo;
GRANT UNLIMITED TABLESPACE TO openbravo;

-- Basic test table
CREATE TABLE openbravo.test_table (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100),
    created_date DATE DEFAULT SYSDATE
);

INSERT INTO openbravo.test_table (id, name) VALUES (1, 'Demo Record');
COMMIT;

SELECT 'Schema created successfully' FROM dual;
QUIT;
EOF
fi

# 5. Verificar SQLcl MCP
log "${YELLOW}🔧 Verificando SQLcl MCP...${NC}"
if docker ps | grep -q "openbravo-sqlcl-mcp"; then
    log "${GREEN}✅ SQLcl MCP está en ejecución${NC}"
    
    # Test endpoint
    if curl -f -s http://localhost:8080/health > /dev/null; then
        log "${GREEN}✅ Endpoint MCP responde correctamente${NC}"
    else
        log "${YELLOW}⚠️  Endpoint MCP no responde (puede estar iniciando)${NC}"
    fi
else
    log "${YELLOW}⚠️  SQLcl MCP no está ejecutándose${NC}"
fi

# 6. Mostrar estado final
log "${BLUE}📋 Estado del entorno:${NC}"
echo
echo "🐳 Contenedores Docker:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo
echo "🔗 URLs de acceso:"
echo "   • Oracle XE: localhost:1521/XEPDB1"
echo "   • SQLcl MCP: http://localhost:8080"
echo
echo "🔑 Credenciales:"
echo "   • Oracle sys: sys/oracle (as sysdba)"
echo "   • Oracle openbravo: openbravo/ob_pwd"
echo
echo "🧪 Test de conexión:"
echo "   docker exec openbravo-oracle-xe sqlplus openbravo/ob_pwd@XEPDB1"

log "${GREEN}✅ Demo rápido completado!${NC}"
