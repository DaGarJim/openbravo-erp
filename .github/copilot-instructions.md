# Instrucciones para Agente de Codificación IA – Openbravo ERP + Oracle + SQLcl MCP

## Descripción del Proyecto

Este proyecto utiliza un fork del ERP Openbravo ([DaGarJim/openbravo-erp](https://github.com/DaGarJim/openbravo-erp)) para mostrar cómo un agente de codificación (como GitHub Copilot en modo agente) puede interactuar con una base de datos Oracle de forma segura y automatizada usando el protocolo **MCP** (Mission Control Protocol) de Oracle SQLcl.

## 🎯 Estado Actual: IMPLEMENTADO ✅

El entorno está **completamente implementado** con:

- ✅ **docker-compose.yml** - Orquestación de Oracle XE y SQLcl MCP
- ✅ **sqlcl/Dockerfile** - Imagen para servidor SQLcl MCP  
- ✅ **scripts/01_load_openbravo.sh** - Script de carga automática de esquema
- ✅ **scripts/02_verify_environment.sh** - Script de verificación completa
- ✅ **scripts/03_reset_environment.sh** - Script de reset y limpieza
- ✅ **.vscode/tasks.json** - Tareas preconfiguradas para VS Code
- ✅ **.devcontainer/** - Configuración para VS Code Dev Containers
- ✅ **README.md completo** - Documentación detallada
- ✅ **Esquema básico Openbravo** - Tablas y datos de ejemplo incluidos

## Objetivo del Agente

Eres un agente de codificación con acceso a una base de datos Oracle XE que contiene un esquema básico de Openbravo ERP. Tu objetivo es:

* Consultar y analizar datos usando SQL de forma natural
* Realizar actualizaciones controladas con auditoría automática  
* Generar reportes y análisis de datos empresariales
* Mantener integridad de datos y seguir mejores prácticas

---

## Componentes Clave

* **`docker-compose.yml`** – Orquestación de Oracle XE y servidor SQLcl MCP
* **`sqlcl/Dockerfile`** – Imagen optimizada para SQLcl con servidor MCP integrado
* **`scripts/01_load_openbravo.sh`** – Script automatizado de carga de esquema Openbravo en Oracle
* **`scripts/02_verify_environment.sh`** – Script de verificación completa del entorno
* **`scripts/03_reset_environment.sh`** – Script de reset y limpieza del entorno
* **`.vscode/tasks.json`** – Tareas preconfiguradas para desarrollo
* **`README.md`** – Documentación completa y guías de uso

---

## Flujo de Datos

1. **Docker Compose** levanta Oracle XE en puerto 1521 y SQLcl MCP en puerto 8080
2. **Script de carga** crea usuario `openbravo` y esquema básico con datos de ejemplo
3. **SQLcl MCP** expone herramientas como `runSQL`, `describeSchema`, `listTables`
4. **GitHub Copilot** usa herramientas MCP para interactuar con la base de datos
5. **Auditoría automática** registra cambios en tabla `DBTOOLS_MCP_LOG`

---

## Setup del Entorno (Para referencia)

El entorno se configura ejecutando:

```bash
# Setup completo automatizado
ant setup && ant install.source
docker compose up -d
./scripts/01_load_openbravo.sh
./scripts/02_verify_environment.sh
```

O usando VS Code con la tarea **"🚀 Setup completo del entorno"**.

---

## 🗄️ Esquema de Base de Datos

### Tablas Principales

| Tabla | Descripción | Campos Clave |
|-------|-------------|--------------|
| `AD_CLIENT` | Clientes/Empresas | `AD_CLIENT_ID`, `NAME` |
| `AD_ORG` | Organizaciones | `AD_ORG_ID`, `AD_CLIENT_ID`, `NAME` |
| `C_BPARTNER` | Socios de Negocio (Clientes/Proveedores) | `C_BPARTNER_ID`, `VALUE`, `NAME`, `CREDITLIMIT`, `ISCUSTOMER` |
| `M_PRODUCT` | Productos | `M_PRODUCT_ID`, `VALUE`, `NAME`, `DESCRIPTION` |
| `DBTOOLS_MCP_LOG` | Auditoría MCP | `LOG_ID`, `LOG_TIME`, `USERNAME`, `OPERATION`, `TABLE_NAME` |

### Datos de Ejemplo Incluidos

- **2 Clientes**: System (0) y F&B US, Inc. (23C59575B9CF467C9620760EB255B389)
- **2 Organizaciones**: System (0) y F&B US, Inc. (19404EAD144C49A0AF37D54377CF452D)  
- **5 Socios de Negocio**: BE-001, AL-001, QA-001, HE-001, TR-001 con límites de crédito variables
- **2 Productos**: COLA-001 (Coca Cola), WATER-001 (Bottled Water)

---

## 🔧 Configuración de la Base de Datos Oracle

**Conexión:**
- **Host**: localhost
- **Puerto**: 1521  
- **Servicio**: XEPDB1
- **Usuario**: openbravo
- **Contraseña**: ob_pwd

**Cadena de conexión completa:**
```
sqlplus openbravo/ob_pwd@//localhost:1521/XEPDB1
```

---

## 🌐 Servidor SQLcl MCP

**Configuración:**
- **URL**: http://localhost:8080
- **Herramientas disponibles**: `runSQL`, `describeSchema`, `listTables`, etc.
- **Estado**: Verificar con `curl http://localhost:8080/health`

---

## 📋 Casos de Uso para el Agente

### 1. Consultas de Análisis

**Ejemplo**: *"Muéstrame los 5 clientes con mayor límite de crédito"*

```sql
SELECT 
    value as codigo_cliente,
    name as nombre_cliente,
    TO_CHAR(creditlimit, 'L999,999,990.00') as limite_credito
FROM c_bpartner 
WHERE iscustomer = 'Y' 
    AND creditlimit > 0
ORDER BY creditlimit DESC 
FETCH FIRST 5 ROWS ONLY;
```

### 2. Actualizaciones Controladas

**Ejemplo**: *"Aumenta el límite de crédito del cliente BE-001 a 20000"*

```sql
-- Verificar valor actual
SELECT name, creditlimit FROM c_bpartner WHERE value = 'BE-001';

-- Actualizar con comentario
UPDATE c_bpartner 
SET creditlimit = 20000,
    updated = SYSDATE,
    updatedby = USER
WHERE value = 'BE-001';

COMMIT;

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
    TO_CHAR(log_time, 'DD/MM/YYYY HH24:MI:SS') as fecha_hora,
    username as usuario,
    operation as operacion,
    rows_affected as filas_afectadas
FROM dbtools_mcp_log 
WHERE table_name = 'C_BPARTNER'
ORDER BY log_time DESC 
FETCH FIRST 10 ROWS ONLY;
```

---

## 🛡️ Mejores Prácticas y Convenciones

### Consultas Seguras
- ✅ Siempre usar `WHERE` clauses apropiadas para evitar full table scans
- ✅ Usar `FETCH FIRST n ROWS ONLY` para limitar resultados grandes
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
./scripts/02_verify_environment.sh
```

### Reset del Entorno
```bash
# Reset suave (mantiene datos)
./scripts/03_reset_environment.sh --soft

# Reset completo (DESTRUYE DATOS)  
./scripts/03_reset_environment.sh --full
```

### Tareas de VS Code Disponibles
- 🚀 **Setup completo del entorno**
- 🐳 **Levantar entorno Docker**  
- 📋 **Cargar esquema Openbravo**
- ✅ **Verificar entorno**
- 🔄 **Reset suave (reiniciar)**
- 📊 **Conectar a Oracle XE (sqlplus)**

---

## 🚨 Manejo de Errores Comunes

### Error de Conexión Oracle
```sql
-- Verificar conectividad
SELECT 'Conexión exitosa a ' || USER || '@' || SYS_CONTEXT('USERENV', 'DB_NAME') FROM DUAL;
```

### Error en Consultas
```sql
-- Verificar existencia de tabla
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'C_BPARTNER';

-- Verificar estructura de tabla
DESCRIBE C_BPARTNER;
```

### Problemas con Servidor MCP
```bash
# Verificar estado del servidor
curl -f http://localhost:8080/health

# Ver logs del contenedor
docker logs openbravo-sqlcl-mcp
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

## Configuración de la Base de Datos Oracle

El archivo `Openbravo.properties` debe contener la configuración para Oracle:

```properties
bbdd.rdbms=ORACLE
bbdd.driver=oracle.jdbc.OracleDriver
bbdd.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
bbdd.userid=openbravo
bbdd.password=ob_pwd
```

---

## Ejecución del Servidor SQLcl MCP

Desde el host o contenedor, se lanza:

```bash
sql -save ob_mcp -savepwd openbravo/ob_pwd@//localhost:1521/XEPDB1
sql -mcp
```

Esto expone automáticamente herramientas como `runSQL`, `describeSchema`, `listTables`, etc., disponibles para agentes MCP como Copilot.

---

## Tareas para el Agente de Codificación

### Preparación del Entorno

1. Verificar que `docker compose up -d` levanta Oracle y SQLcl MCP.
2. Ejecutar script de carga `scripts/01_load_openbravo.sh`.
3. Probar conectividad con SQLcl usando alias `ob_mcp`.

### Casos de Uso a Implementar

(Se incluirán aparte en archivos `usecase-<id>.md`, ver sección final).

### Consultas y Actualizaciones

El agente debe ser capaz de:

* Consultar datos, p.ej.:
  ```sql
  SELECT name, creditlimit FROM c_bpartner WHERE bpartner_id = 'C0001';
  ```

* Actualizar datos:
  ```sql
  UPDATE c_bpartner SET creditlimit = 25000 WHERE bpartner_id = 'C0001';
  COMMIT;
  ```

* Verificar logs de auditoría:
  ```sql
  SELECT * FROM DBTOOLS$MCP_LOG ORDER BY log_time DESC FETCH FIRST 5 ROWS ONLY;
  ```

* Ejecutar rollback (si procede):
  ```sql
  ROLLBACK;
  ```

---

## Convenciones y Buenas Prácticas

* Usar `ant smartbuild` para reconstrucciones rápidas.
* Mantener los cambios de esquema mediante `ant export.database`.
* Añadir tests JUnit si se amplía la lógica de negocio.
* Evitar modificar directamente el núcleo (`src-core/`), usar módulos en `modules/`.

---

## Validación del Entorno

Para considerar el entorno correctamente configurado:

- [ ] Oracle XE se inicia correctamente (`docker ps`).
- [ ] SQLcl MCP responde en el puerto esperado (`sql -mcp` logs OK).
- [ ] Copilot puede listar tablas y describir esquema (`describeSchema`).
- [ ] Se puede ejecutar una consulta básica (`runSQL`).
- [ ] Se puede actualizar un valor y verificar el resultado.
- [ ] El log de auditoría contiene las operaciones anteriores.

---

## Archivos y Directorios Importantes

* `openbravo-erp/`
* `scripts/01_load_openbravo.sh`
* `docker-compose.yml`
* `sqlcl/Dockerfile`
* `config/Openbravo.properties`
* `build/sql/oracle/openbravo_schema.sql`

---

## Casos de Uso

Los siguientes casos de uso se definirán individualmente en archivos del tipo:

* `usecase-001-consulta-clientes.md`
* `usecase-002-modificacion-precios.md`

Cada uno contendrá:

- Descripción funcional
- Consulta esperada
- Acción esperada
- Resultado verificable

---

## Notas Finales

Este archivo debe permanecer actualizado como guía de referencia para agentes de codificación automáticos (como GitHub Copilot en modo planificación / agente). Está optimizado para uso con herramientas de IA en entornos reproducibles.

```bash
# Para ejecutar todo el entorno:
docker compose up -d
./scripts/01_load_openbravo.sh
sql -mcp
```

Copilot podrá usar `runSQL`, `describeSchema`, etc., vía REST MCP para razonar sobre la BD y actuar.