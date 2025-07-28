# Instrucciones para Agente de Codificación IA – Openbravo ERP + PostgreSQL + MCP

## Descripción del Proyecto

Este proyecto utiliza un fork del ERP Openbravo ([DaGarJim/openbravo-erp](https://github.com/DaGarJim/openbravo-erp)) para mostrar cómo un agente de codificación (como GitHub Copilot en modo agente) puede interactuar con una base de datos PostgreSQL de forma segura y automatizada usando el protocolo **MCP** (Model Context Protocol).

## 🎯 Estado Actual: IMPLEMENTADO ✅

El entorno está **completamente implementado** con:

- ✅ **docker-compose.yml** - Orquestación de PostgreSQL y servidor MCP
- ✅ **mcp_client.py** - Cliente MCP Python para interacciones con la base de datos
- ✅ **scripts/01_load_openbravo_postgres.sh** - Script de carga automática de esquema PostgreSQL
- ✅ **scripts/02_verify_environment_postgres.sh** - Script de verificación completa
- ✅ **scripts/03_reset_environment_postgres.sh** - Script de reset y limpieza
- ✅ **.vscode/tasks.json** - Tareas preconfiguradas para VS Code (PostgreSQL)
- ✅ **README.md completo** - Documentación detallada
- ✅ **Esquema básico Openbravo** - Tablas y datos de ejemplo incluidos en PostgreSQL

## Objetivo del Agente

Eres un agente de codificación con acceso a una base de datos PostgreSQL que contiene un esquema básico de Openbravo ERP. Tu objetivo es:

* **SIEMPRE usar el cliente MCP** (`mcp_client.py`) para todas las interacciones con la base de datos
* **NUNCA ejecutar SQL directamente** con psql, sqlplus o comandos directos
* Consultar y analizar datos usando SQL de forma natural a través del protocolo MCP
* Realizar actualizaciones controladas con auditoría automática  
* Generar reportes y análisis de datos empresariales
* Mantener integridad de datos y seguir mejores prácticas

## 🚨 REGLAS OBLIGATORIAS PARA EL AGENTE

### ✅ SIEMPRE HACER:
1. **Usar exclusivamente el cliente MCP**: `/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py`
2. **Verificar el entorno antes de comenzar** con las validaciones correspondientes
3. **Estructurar respuestas en formato JSON** cuando use el cliente MCP
4. **Verificar cambios** después de actualizaciones con consultas de confirmación
5. **Usar transacciones seguras** y auditoría automática

### ❌ NUNCA HACER:
1. **Ejecutar psql directamente** (`psql postgresql://...`)
2. **Usar comandos SQL directos** en terminal sin el cliente MCP
3. **Omitir validaciones** del entorno antes de operar
4. **Hacer cambios** sin verificación posterior
5. **Ignorar mensajes de error** del cliente MCP

---

## 🚀 Procedimiento de Arranque de la Demo

### Paso 1: Validación Inicial del Entorno
```bash
# Verificar que Docker esté ejecutándose
docker --version

# Verificar estado de contenedores existentes
docker ps -a
```

### Paso 2: Configuración del Entorno Python
```bash
# Configurar entorno virtual Python (automático con herramientas VS Code)
# El sistema creará automáticamente: /Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python

# Instalar dependencias requeridas (automático)
# psycopg2-binary se instalará automáticamente cuando sea necesario
```

### Paso 3: Levantar la Base de Datos PostgreSQL
```bash
# Usar la tarea de VS Code recomendada:
# "🐘 Setup PostgreSQL (Recomendado)"
docker compose up -d

# Verificar que PostgreSQL esté ejecutándose
docker ps | grep postgres
```

### Paso 4: Cargar el Esquema Openbravo
```bash
# Usar la tarea de VS Code:
# "📊 Cargar esquema PostgreSQL"
./scripts/01_load_openbravo_postgres.sh

# Este script:
# - Crea la base de datos 'openbravo' si no existe
# - Carga el esquema básico con tablas principales
# - Inserta datos de ejemplo (clientes, productos, organizaciones)
# - Configura la auditoría MCP
```

### Paso 5: Verificación del Entorno
```bash
# Usar la tarea de VS Code:
# "🔍 Verificar entorno PostgreSQL"
./scripts/02_verify_environment_postgres.sh

# Este script verifica:
# - Conectividad a PostgreSQL
# - Existencia de tablas principales
# - Datos de ejemplo cargados
# - Cliente MCP funcionando
```

### Paso 6: Prueba del Cliente MCP
```bash
# Probar conectividad del cliente MCP
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py analyze_db_health

# Resultado esperado:
# {
#   "total_tables": 6,
#   "total_clients": 5,
#   "total_products": 2,
#   "credit_exposure": "125000.00",
#   "avg_credit_limit": "25000.000000000000"
# }
```

### ✅ Validaciones de Éxito

El entorno está correctamente configurado si:

1. **PostgreSQL responde**: `docker ps` muestra contenedor `openbravo-postgres` en estado `Up`
2. **Base de datos existe**: El script de verificación completa sin errores
3. **Tablas cargadas**: `analyze_db_health` devuelve métricas válidas
4. **Cliente MCP funciona**: Las consultas JSON se ejecutan correctamente
5. **Datos de ejemplo presentes**: Se muestran 5 clientes y 2 productos

### 🔧 Troubleshooting

#### Error: "ModuleNotFoundError: No module named 'psycopg2'"
```bash
# Solución: Configurar entorno Python y instalar dependencias
# (Se hace automáticamente con las herramientas de VS Code)
```

#### Error: "Connection refused" a PostgreSQL
```bash
# Verificar estado del contenedor
docker ps -a | grep postgres

# Reiniciar si es necesario
docker compose restart postgres
```

#### Error: "Database does not exist"
```bash
# Ejecutar script de carga
./scripts/01_load_openbravo_postgres.sh
```

### 📋 Ejemplos de Uso del Cliente MCP

#### ✅ Ejemplo Correcto - Análisis de salud:
```bash
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py analyze_db_health
```

#### ✅ Ejemplo Correcto - Consulta específica:
```bash
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py execute_sql "SELECT COUNT(*) FROM c_bpartner WHERE iscustomer = 'Y'"
```

#### ✅ Ejemplo Correcto - Listar tablas:
```bash
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py list_tables
```

#### ❌ Ejemplo INCORRECTO - No usar SQL directo:
```bash
# NUNCA HACER ESTO:
psql postgresql://tad:tad@localhost:5432/openbravo -c "SELECT * FROM c_bpartner"
```

---

## Componentes Clave

* **`docker-compose.yml`** – Orquestación de PostgreSQL y contenedores MCP
* **`mcp_client.py`** – Cliente MCP Python para todas las interacciones con la base de datos
* **`scripts/01_load_openbravo_postgres.sh`** – Script automatizado de carga de esquema PostgreSQL
* **`scripts/02_verify_environment_postgres.sh`** – Script de verificación completa del entorno
* **`scripts/03_reset_environment_postgres.sh`** – Script de reset y limpieza del entorno
* **`.vscode/tasks.json`** – Tareas preconfiguradas para desarrollo PostgreSQL
* **`README.md`** – Documentación completa y guías de uso

---

## Flujo de Datos MCP

1. **Docker Compose** levanta PostgreSQL en puerto 5432
2. **Script de carga** crea base de datos `openbravo` y esquema básico con datos de ejemplo
3. **Cliente MCP Python** (`mcp_client.py`) se conecta a PostgreSQL usando psycopg2
4. **GitHub Copilot** usa EXCLUSIVAMENTE el cliente MCP para todas las consultas y actualizaciones
5. **Auditoría automática** registra cambios en tabla `mcp_operations_log`

### 🔄 Interacción Correcta con MCP

#### ✅ CORRECTO - Usar cliente MCP:
```bash
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py execute_sql "SELECT * FROM c_bpartner LIMIT 5"
```

#### ❌ INCORRECTO - SQL directo:
```bash
psql postgresql://tad:tad@localhost:5432/openbravo -c "SELECT * FROM c_bpartner LIMIT 5"
```

---

## 🔍 Validación del Entorno

### Lista de Verificación para Demo

Para considerar el entorno **completamente operativo**:

- [ ] **PostgreSQL ejecutándose**: `docker ps` muestra `openbravo-postgres` con estado `Up`
- [ ] **Base de datos creada**: Script de carga ejecutado sin errores
- [ ] **Tablas presentes**: 6 tablas principales cargadas (`AD_CLIENT`, `AD_ORG`, `C_BPARTNER`, `M_PRODUCT`, `DEMO_STATUS`, `MCP_OPERATIONS_LOG`)
- [ ] **Datos de ejemplo**: 5 socios de negocio y 2 productos disponibles
- [ ] **Cliente MCP funcional**: `analyze_db_health` devuelve métricas válidas
- [ ] **Entorno Python**: Virtual environment configurado con psycopg2-binary
- [ ] **Conectividad verificada**: Scripts de verificación completan exitosamente

### Comandos de Validación Rápida

```bash
# 1. Verificar containers
docker ps | grep postgres

# 2. Verificar entorno completo
./scripts/02_verify_environment_postgres.sh

# 3. Probar cliente MCP
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py analyze_db_health

# 4. Verificar datos básicos
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py execute_sql "SELECT COUNT(*) as total_clientes FROM c_bpartner WHERE iscustomer = 'Y'"
```

### Métricas Esperadas de Validación

Al ejecutar `analyze_db_health`, debe devolver:

```json
{
  "total_tables": 6,
  "total_clients": 5,
  "total_products": 2,
  "credit_exposure": "140000.00",
  "avg_credit_limit": "28000.000000000000"
}
```

*Nota: Los valores pueden variar si se han realizado actualizaciones durante la demo.*

---

## Setup del Entorno (Para referencia)

### Opción A: Setup Automatizado (Recomendado)
```bash
# Usar las tareas de VS Code en este orden:
# 1. "🐘 Setup PostgreSQL (Recomendado)"
# 2. "📊 Cargar esquema PostgreSQL"  
# 3. "🔍 Verificar entorno PostgreSQL"
```

### Opción B: Setup Manual
```bash
# 1. Levantar PostgreSQL
docker compose up -d

# 2. Cargar esquema
./scripts/01_load_openbravo_postgres.sh

# 3. Verificar entorno
./scripts/02_verify_environment_postgres.sh

# 4. Probar cliente MCP
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py analyze_db_health
```

---

## 🗄️ Esquema de Base de Datos

### Tablas Principales

| Tabla | Descripción | Campos Clave |
|-------|-------------|--------------|
| `AD_CLIENT` | Clientes/Empresas | `AD_CLIENT_ID`, `NAME` |
| `AD_ORG` | Organizaciones | `AD_ORG_ID`, `AD_CLIENT_ID`, `NAME` |
| `C_BPARTNER` | Socios de Negocio (Clientes/Proveedores) | `C_BPARTNER_ID`, `VALUE`, `NAME`, `CREDITLIMIT`, `ISCUSTOMER` |
| `M_PRODUCT` | Productos | `M_PRODUCT_ID`, `VALUE`, `NAME`, `DESCRIPTION` |
| `DEMO_STATUS` | Estado de la Demo | `STATUS`, `LAST_UPDATE` |
| `MCP_OPERATIONS_LOG` | Auditoría MCP | `ID`, `OPERATION_TYPE`, `TABLE_NAME`, `EXECUTED_AT` |

### Datos de Ejemplo Incluidos

- **2 Clientes**: System (0) y F&B US, Inc. (23C59575B9CF467C9620760EB255B389)
- **2 Organizaciones**: System (0) y F&B US, Inc. (19404EAD144C49A0AF37D54377CF452D)  
- **5 Socios de Negocio**: BE-001, AL-001, QA-001, HE-001, TR-001 con límites de crédito variables
- **2 Productos**: COLA-001 (Coca Cola), WATER-001 (Bottled Water)

---

## 🔧 Configuración de la Base de Datos PostgreSQL

**Conexión:**
- **Host**: localhost
- **Puerto**: 5432  
- **Base de datos**: openbravo
- **Usuario**: tad
- **Contraseña**: tad

**Cadena de conexión completa:**
```
postgresql://tad:tad@localhost:5432/openbravo
```

---

## 🌐 Cliente MCP PostgreSQL

**Configuración:**
- **Archivo**: `mcp_client.py`
- **Funciones disponibles**: `execute_sql`, `list_schemas`, `list_tables`, `analyze_db_health`
- **Conexión**: Automática a PostgreSQL local
- **Uso**: `/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python mcp_client.py <comando>`

---

## 📋 Casos de Uso para el Agente

### 1. Consultas de Análisis

**Ejemplo**: *"Muéstrame los 5 clientes con mayor límite de crédito"*

```sql
SELECT 
    value as codigo_cliente,
    name as nombre_cliente,
    creditlimit as limite_credito
FROM c_bpartner 
WHERE iscustomer = 'Y' 
    AND creditlimit > 0
ORDER BY creditlimit DESC 
LIMIT 5;
```

### 2. Actualizaciones Controladas

**Ejemplo**: *"Aumenta el límite de crédito del cliente BE-001 a 20000"*

```sql
-- Verificar valor actual
SELECT name, creditlimit FROM c_bpartner WHERE value = 'BE-001';

-- Actualizar con comentario
UPDATE c_bpartner 
SET creditlimit = 20000,
    updated = NOW()
WHERE value = 'BE-001';

-- Verificar cambio
SELECT name, creditlimit FROM c_bpartner WHERE value = 'BE-001';
```

### 3. Reportes Ejecutivos

**Ejemplo**: *"Dame un resumen de clientes por rango de crédito"*

```sql
SELECT 
    CASE 
        WHEN creditlimit >= 30000 THEN 'Premium (30K+)'
        WHEN creditlimit >= 20000 THEN 'Gold (20K-30K)'
        WHEN creditlimit >= 15000 THEN 'Silver (15K-20K)'
        ELSE 'Standard (<15K)'
    END AS categoria_cliente,
    COUNT(*) as total_clientes,
    TO_CHAR(AVG(creditlimit), 'L999,999,990.00') as credito_promedio,
    TO_CHAR(SUM(creditlimit), 'L999,999,990.00') as exposicion_total
FROM c_bpartner 
WHERE iscustomer = 'Y' AND creditlimit > 0
GROUP BY 
    CASE 
        WHEN creditlimit >= 30000 THEN 'Premium (30K+)'
        WHEN creditlimit >= 20000 THEN 'Gold (20K-30K)'
        WHEN creditlimit >= 15000 THEN 'Silver (15K-20K)'
        ELSE 'Standard (<15K)'
    END
ORDER BY AVG(creditlimit) DESC;
```

### 4. Auditoría y Trazabilidad

**Ejemplo**: *"Muéstrame los últimos cambios en la tabla de clientes"*

```sql
SELECT 
    TO_CHAR(executed_at, 'DD/MM/YYYY HH24:MI:SS') as fecha_hora,
    executed_by as usuario,
    operation_type as operacion,
    table_name as tabla
FROM mcp_operations_log 
WHERE table_name = 'c_bpartner'
ORDER BY executed_at DESC 
LIMIT 10;
```

---

## 🛡️ Mejores Prácticas y Convenciones

### Consultas Seguras
- ✅ Siempre usar `WHERE` clauses apropiadas para evitar full table scans
- ✅ Usar `LIMIT n` para limitar resultados grandes
- ✅ Formatear números con `TO_CHAR` para mejor legibilidad
- ✅ Incluir campos de auditoría (`CREATED`, `UPDATED`) en SELECT cuando sea relevante

### Actualizaciones Responsables  
- ✅ Siempre verificar datos ANTES de actualizar con SELECT
- ✅ Usar transacciones explícitas (BEGIN/COMMIT/ROLLBACK)
- ✅ Actualizar campos de auditoría (`UPDATED`, `UPDATEDBY`)
- ✅ Verificar resultados DESPUÉS de actualizar
- ⚠️ **NUNCA** ejecutar UPDATE/DELETE sin WHERE clause

### Análisis de Datos
- ✅ Usar funciones agregadas (COUNT, SUM, AVG) para resúmenes
- ✅ Implementar categorización con CASE statements
- ✅ Incluir formateo de monedas y números
- ✅ Proporcionar contexto empresarial en los resultados

---

## ⚙️ Comandos de Gestión del Entorno

### Verificación Rápida
```bash
./scripts/02_verify_environment_postgres.sh
```

### Reset del Entorno
```bash
# Reset suave (mantiene datos)
./scripts/03_reset_environment_postgres.sh --soft

# Reset completo (DESTRUYE DATOS)  
./scripts/03_reset_environment_postgres.sh --full
```

### Tareas de VS Code Disponibles
- � **Setup PostgreSQL (Recomendado)**
- � **Cargar esquema PostgreSQL**  
- 🔍 **Verificar entorno PostgreSQL**
- 🔄 **Reset suave PostgreSQL**
- �️ **Conectar a PostgreSQL (psql)**

---

## 🚨 Manejo de Errores Comunes

### Error de Conexión PostgreSQL
```sql
-- Verificar conectividad
SELECT 'Conexión exitosa a PostgreSQL como ' || current_user || ' en base de datos ' || current_database();
```

### Error en Consultas
```sql
-- Verificar existencia de tabla
SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'c_bpartner';

-- Verificar estructura de tabla
SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'c_bpartner';
```

### Problemas con Cliente MCP
```bash
# Verificar entorno Python
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python --version

# Verificar dependencias
/Users/degr/Documents/GitHub/openbravo-erp/.venv/bin/python -c "import psycopg2; print('psycopg2 OK')"
```

---

## 📊 Métricas y KPIs de Negocio

Cuando generes reportes, considera estos KPIs empresariales:

- **Exposición total de crédito**: `SUM(creditlimit)`
- **Cliente promedio**: `AVG(creditlimit)`  
- **Distribución por categoría**: Segmentación por rangos de crédito
- **Clientes activos vs inactivos**: `WHERE isactive = 'Y'`
- **Mix de productos**: Análisis de `M_PRODUCT`
- **Actividad de auditoría**: Frecuencia de cambios en `DBTOOLS_MCP_LOG`

---

## 🔄 Flujo de Trabajo Recomendado

1. **Entender el Requerimiento**: Interpretar la consulta en lenguaje natural
2. **Verificar Contexto**: Confirmar que las tablas y datos existen
3. **Construir Consulta**: Escribir SQL optimizado y seguro
4. **Ejecutar y Verificar**: Revisar resultados y formato
5. **Proporcionar Contexto**: Explicar resultados en términos empresariales
6. **Documentar Cambios**: Si hay actualizaciones, registrar el impacto

---

## 🎯 Objetivos del Agente

Al interactuar con este entorno, el agente debe:

- ✅ Demostrar capacidades de análisis de datos empresariales
- ✅ Mantener integridad y seguridad de datos
- ✅ Proporcionar insights de negocio valiosos
- ✅ Seguir mejores prácticas de SQL y base de datos
- ✅ Ser transparente sobre limitaciones y suposiciones
- ✅ Facilitar el aprendizaje y la comprensión del usuario

---

**Notas Finales**: Este entorno está optimizado para demostrar las capacidades de agentes de IA trabajando con sistemas ERP reales. Todos los datos son de ejemplo y el entorno es seguro para experimentación y aprendizaje.

---

## Componentes Clave

* **`openbravo-erp/`** – Código fuente del ERP con módulos, base de datos y lógica.
* **`docker-compose.yml`** – Orquestación de Oracle XE y servidor SQLcl MCP.
* **`scripts/01_load_openbravo.sh`** – Script de carga de esquema Openbravo en Oracle.
* **`sqlcl/`** – Dockerfile mínimo para exponer SQLcl MCP desde contenedor o host.
* **`config/Openbravo.properties`** – Configuración de conexión a Oracle.
* **`build/sql/oracle/`** – Esquema SQL generado por `ant install.source`.

---

## Flujo de Datos

1. **Openbravo ERP** genera esquema y datos base mediante `ant install.source`.
2. **Oracle XE (Docker)** expone la base de datos en `localhost:1521/XEPDB1`.
3. **SQLcl** lanza el servidor MCP y expone herramientas como `runSQL`.
4. **Copilot (modo agente)** descubre el esquema y consulta / modifica usando herramientas MCP.

---

## Compilación y Carga de Esquema

```bash
git clone https://github.com/DaGarJim/openbravo-erp.git
cd openbravo-erp
ant setup
ant install.source
cp build/sql/oracle/openbravo_schema.sql ../scripts/
```

Luego se arranca el entorno:

```bash
docker compose up -d
```

Y se carga el esquema:

```bash
./scripts/01_load_openbravo.sh
```

---

## 📊 Métricas y KPIs de Negocio

Cuando generes reportes, considera estos KPIs empresariales:

- **Exposición total de crédito**: `SUM(creditlimit)`
- **Cliente promedio**: `AVG(creditlimit)`  
- **Distribución por categoría**: Segmentación por rangos de crédito
- **Clientes activos vs inactivos**: `WHERE isactive = 'Y'`
- **Mix de productos**: Análisis de `M_PRODUCT`
- **Actividad de auditoría**: Frecuencia de cambios en `MCP_OPERATIONS_LOG`

---

## 🔄 Flujo de Trabajo Recomendado

1. **Entender el Requerimiento**: Interpretar la consulta en lenguaje natural
2. **Verificar Contexto**: Confirmar que las tablas y datos existen
3. **Construir Consulta**: Escribir SQL optimizado y seguro
4. **Ejecutar y Verificar**: Revisar resultados y formato
5. **Proporcionar Contexto**: Explicar resultados en términos empresariales
6. **Documentar Cambios**: Si hay actualizaciones, registrar el impacto

---

## 🎯 Objetivos del Agente

Al interactuar con este entorno, el agente debe:

- ✅ Demostrar capacidades de análisis de datos empresariales
- ✅ Mantener integridad y seguridad de datos
- ✅ Proporcionar insights de negocio valiosos
- ✅ Seguir mejores prácticas de SQL y base de datos
- ✅ Ser transparente sobre limitaciones y suposiciones
- ✅ Facilitar el aprendizaje y la comprensión del usuario

---

**Notas Finales**: Este entorno está optimizado para demostrar las capacidades de agentes de IA trabajando con sistemas ERP reales. Todos los datos son de ejemplo y el entorno es seguro para experimentación y aprendizaje.