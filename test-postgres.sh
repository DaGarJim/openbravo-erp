#!/bin/bash

# Script de prueba simple para PostgreSQL
echo "🐘 Probando PostgreSQL básico..."

# Limpiar entorno
echo "Limpiando entorno anterior..."
docker compose down --volumes 2>/dev/null || true

# Iniciar solo PostgreSQL
echo "Iniciando PostgreSQL..."
docker compose up -d postgres

# Esperar que PostgreSQL esté listo
echo "Esperando PostgreSQL..."
sleep 10

# Verificar conexión usando docker exec
echo "Probando conexión..."
docker exec openbravo-postgres psql -U tad -d openbravo -c "SELECT version();"

# Crear tabla de prueba
echo "Creando tabla de prueba..."
docker exec openbravo-postgres psql -U tad -d openbravo -c "
CREATE TABLE IF NOT EXISTS test_table (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"

# Insertar datos de prueba
echo "Insertando datos de prueba..."
docker exec openbravo-postgres psql -U tad -d openbravo -c "
INSERT INTO test_table (name) VALUES 
('Test 1'),  
('Test 2'),
('Test 3');
"

# Consultar datos
echo "Consultando datos..."
docker exec openbravo-postgres psql -U tad -d openbravo -c "SELECT * FROM test_table;"

echo "✅ Prueba PostgreSQL completada!"
