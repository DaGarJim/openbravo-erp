#!/bin/bash

# Script de prueba rápida para tests de Openbravo + PostgreSQL
# Autor: GitHub Copilot Agent

set -e

echo "🧪 PROBANDO TESTS DE OPENBRAVO + POSTGRESQL"
echo "==========================================="
echo ""

# Verificar contenedores
echo "📦 Estado de contenedores:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(postgres|Names)" || echo "No hay contenedores corriendo"
echo ""

# Verificar conectividad PostgreSQL
echo "🗄️ Conectividad PostgreSQL:"
if docker exec openbravo-postgres pg_isready -U tad -d openbravo 2>/dev/null; then
    echo "✅ PostgreSQL está disponible"
    
    echo ""
    echo "📊 Consultas de tests:"
    echo ""
    
    # Consulta 1: Listar tablas
    echo "1️⃣ Tablas disponibles:"
    docker exec openbravo-postgres psql -U tad -d openbravo -c "\dt" 2>/dev/null || echo "❌ Error al listar tablas"
    echo ""
    
    # Consulta 2: Clientes con mayor crédito
    echo "2️⃣ Top 5 clientes por límite de crédito:"
    docker exec openbravo-postgres psql -U tad -d openbravo -c "
        SELECT 
            value as codigo_cliente,
            name as nombre_cliente,
            creditlimit as limite_credito
        FROM c_bpartner 
        WHERE iscustomer = 'Y' 
        ORDER BY creditlimit DESC 
        LIMIT 5;
    " 2>/dev/null || echo "❌ Error en consulta de clientes"
    echo ""
    
    # Consulta 3: Productos
    echo "3️⃣ Productos disponibles:"
    docker exec openbravo-postgres psql -U tad -d openbravo -c "
        SELECT value as codigo, name as producto FROM m_product WHERE isactive = 'Y';
    " 2>/dev/null || echo "❌ Error en consulta de productos"
    echo ""
    
    # Consulta 4: Organizaciones
    echo "4️⃣ Organizaciones:"
    docker exec openbravo-postgres psql -U tad -d openbravo -c "
        SELECT name as organizacion FROM ad_org WHERE isactive = 'Y';
    " 2>/dev/null || echo "❌ Error en consulta de organizaciones"
    echo ""
    
    echo "🎉 Tests listos para usar con GitHub Copilot!"
    echo ""
    echo "✨ Ejemplos de consultas que puedes hacer:"
    echo '  • "Muéstrame los 5 clientes con mayor límite de crédito"'
    echo '  • "Dame un resumen de productos disponibles"'
    echo '  • "Actualiza el límite de crédito del cliente BE-001 a 40000"'
    echo '  • "Genera un reporte de socios de negocio por organización"'
    echo ""
    
else
    echo "❌ PostgreSQL no está disponible"
    echo ""
    echo "🔧 Para iniciar los tests:"
    echo "  docker compose up -d"
    echo "  ./scripts/01_load_openbravo_postgres.sh"
    echo ""
fi
