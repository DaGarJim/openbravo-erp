# Dockerfile para SQLcl con servidor MCP
FROM oraclelinux:8-slim

# Instalar dependencias básicas
RUN microdnf update -y && \
    microdnf install -y \
    java-11-openjdk-headless \
    wget \
    unzip \
    curl \
    && microdnf clean all

# Configurar variables de entorno para Java
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk
ENV PATH=$PATH:$JAVA_HOME/bin

# Crear usuario no-root para SQLcl
RUN useradd -m -u 1000 sqlcl

# Crear directorio de trabajo
WORKDIR /app

# Descargar e instalar SQLcl (versión 23.4)
# Nota: En un entorno real, sería mejor tener el archivo localmente
RUN wget -O sqlcl.zip https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip && \
    unzip sqlcl.zip && \
    rm sqlcl.zip && \
    mv sqlcl sqlcl-install && \
    chmod +x sqlcl-install/bin/sql && \
    ln -s /app/sqlcl-install/bin/sql /usr/local/bin/sql

# Copiar archivos de configuración
COPY config/ /app/config/
COPY scripts/ /app/scripts/

# Crear script de inicio
RUN cat > /app/start-sqlcl-mcp.sh << 'EOF'
#!/bin/bash

# Esperar a que Oracle esté disponible
echo "Esperando que Oracle XE esté disponible..."
while ! nc -z ${ORACLE_HOST:-oracle-xe} ${ORACLE_PORT:-1521}; do
  echo "Oracle no está listo aún, esperando..."
  sleep 5
done

echo "Oracle XE disponible, configurando conexión..."

# Configurar alias de conexión
sql /nolog << SQL_EOF
set feedback off
set verify off
save ob_mcp replace
connect ${ORACLE_USER:-openbravo}/${ORACLE_PASSWORD:-ob_pwd}@//${ORACLE_HOST:-oracle-xe}:${ORACLE_PORT:-1521}/${ORACLE_SERVICE:-XEPDB1}
save ob_mcp replace savepwd
exit
SQL_EOF

# Iniciar servidor MCP
echo "Iniciando servidor SQLcl MCP en puerto ${MCP_PORT:-8080}..."
sql -mcp -port ${MCP_PORT:-8080} -host 0.0.0.0 ob_mcp
EOF

RUN chmod +x /app/start-sqlcl-mcp.sh

# Crear script de healthcheck
RUN cat > /app/healthcheck.sh << 'EOF'
#!/bin/bash
curl -f http://localhost:${MCP_PORT:-8080}/health > /dev/null 2>&1
EOF

RUN chmod +x /app/healthcheck.sh

# Cambiar propietario
RUN chown -R sqlcl:sqlcl /app

# Cambiar a usuario no-root
USER sqlcl

# Exponer puerto MCP
EXPOSE 8080

# Comando de inicio
CMD ["/app/start-sqlcl-mcp.sh"]
