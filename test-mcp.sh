#!/bin/bash

echo "🧪 Probando servidor MCP PostgreSQL..."
echo "======================================="

echo "1. Verificando que el servidor esté ejecutándose..."
if docker ps | grep -q postgres-mcp-server; then
    echo "✅ Servidor MCP ejecutándose"
else
    echo "❌ Servidor MCP no está ejecutándose"
    echo "Iniciando servidor..."
    docker run -d --name postgres-mcp-server -p 8000:8000 --network openbravo-erp_openbravo-net \
        -e DATABASE_URI="postgresql://tad:tad@postgres:5432/openbravo" \
        crystaldba/postgres-mcp:latest --access-mode=unrestricted --transport=sse
    sleep 3
fi

echo ""
echo "2. Verificando conectividad SSE..."
if curl -s http://localhost:8000/sse | head -1 | grep -q "event:"; then
    echo "✅ Endpoint SSE responde correctamente"
else
    echo "❌ Endpoint SSE no responde"
fi

echo ""
echo "3. Verificando logs del servidor..."
echo "Últimas líneas de logs:"
docker logs --tail 5 postgres-mcp-server

echo ""
echo "4. Probando herramientas MCP manualmente..."
echo "Enviando solicitud de lista de esquemas..."

# Crear una prueba directa del MCP usando stdio
echo "Usando cliente stdio para probar..."
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | \
docker run -i --rm --network openbravo-erp_openbravo-net \
    -e DATABASE_URI="postgresql://tad:tad@postgres:5432/openbravo" \
    crystaldba/postgres-mcp:latest --access-mode=unrestricted

echo ""
echo "🔧 Configuración completada."
echo "El servidor MCP PostgreSQL debería estar disponible en VS Code."
echo "URL: http://localhost:8000/sse"
