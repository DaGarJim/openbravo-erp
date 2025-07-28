#!/bin/bash

echo "🧪 PRUEBA RÁPIDA DE LA DEMO"
echo "=========================="

# Verificar Docker
echo "📦 Verificando Docker..."
docker --version
echo ""

# Verificar contenedores
echo "🐳 Contenedores activos:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# Verificar PostgreSQL si está corriendo
if docker ps | grep -q "openbravo-postgres"; then
    echo "🗄️ Probando PostgreSQL..."
    docker exec openbravo-postgres psql -U tad -d openbravo -c "SELECT 'Demo funcionando!' as status;"
    echo ""
    
    echo "📊 Consulta de ejemplo - Top clientes:"
    docker exec openbravo-postgres psql -U tad -d openbravo -c "
        SELECT 
            value as codigo,
            name as cliente,
            creditlimit as credito
        FROM c_bpartner 
        WHERE iscustomer = 'Y' 
        ORDER BY creditlimit DESC 
        LIMIT 3;
    "
    echo ""
else
    echo "❌ Contenedor PostgreSQL no está corriendo"
    echo "🚀 Para iniciar: docker compose up -d"
    echo ""
fi

echo "✅ Verificación completa"
