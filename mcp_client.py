#!/usr/bin/env python3
"""
Cliente MCP PostgreSQL directo para consultas de base de datos.
Simula las herramientas MCP principales sin depender del protocolo completo.
"""
import psycopg2
import json
import sys
from datetime import datetime

class PostgreSQLMCPClient:
    def __init__(self, connection_string):
        self.conn = psycopg2.connect(connection_string)
        self.conn.autocommit = True
    
    def execute_sql(self, query):
        """Ejecuta una consulta SQL y devuelve los resultados"""
        try:
            cursor = self.conn.cursor()
            cursor.execute(query)
            
            if cursor.description:
                columns = [desc[0] for desc in cursor.description]
                rows = cursor.fetchall()
                return {
                    "success": True,
                    "columns": columns,
                    "rows": rows,
                    "row_count": len(rows)
                }
            else:
                return {
                    "success": True,
                    "message": f"Query executed successfully. Rows affected: {cursor.rowcount}",
                    "rows_affected": cursor.rowcount
                }
        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }
    
    def list_schemas(self):
        """Lista todos los esquemas de la base de datos"""
        query = """
        SELECT schema_name 
        FROM information_schema.schemata 
        WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
        ORDER BY schema_name
        """
        return self.execute_sql(query)
    
    def list_tables(self, schema='public'):
        """Lista las tablas de un esquema"""
        query = f"""
        SELECT table_name, table_type
        FROM information_schema.tables 
        WHERE table_schema = '{schema}'
        ORDER BY table_name
        """
        return self.execute_sql(query)
    
    def analyze_db_health(self):
        """Análisis básico de salud de la base de datos"""
        queries = {
            "total_tables": "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'",
            "total_clients": "SELECT COUNT(*) FROM c_bpartner WHERE iscustomer = 'Y'",
            "total_products": "SELECT COUNT(*) FROM m_product WHERE isactive = 'Y'",
            "credit_exposure": "SELECT COALESCE(SUM(creditlimit), 0) FROM c_bpartner WHERE iscustomer = 'Y'",
            "avg_credit_limit": "SELECT COALESCE(AVG(creditlimit), 0) FROM c_bpartner WHERE iscustomer = 'Y' AND creditlimit > 0"
        }
        
        results = {}
        for name, query in queries.items():
            result = self.execute_sql(query)
            if result["success"] and result["rows"]:
                results[name] = result["rows"][0][0]
            else:
                results[name] = "Error"
        
        return results

def main():
    if len(sys.argv) < 2:
        print("Uso: python3 mcp_client.py <comando> [argumentos]")
        print("Comandos: execute_sql, list_schemas, list_tables, analyze_db_health")
        return
    
    # Configuración de conexión
    conn_string = "postgresql://tad:tad@localhost:5432/openbravo"
    client = PostgreSQLMCPClient(conn_string)
    
    command = sys.argv[1]
    
    if command == "execute_sql" and len(sys.argv) > 2:
        query = sys.argv[2]
        result = client.execute_sql(query)
        print(json.dumps(result, indent=2, default=str))
    
    elif command == "list_schemas":
        result = client.list_schemas()
        print(json.dumps(result, indent=2, default=str))
    
    elif command == "list_tables":
        schema = sys.argv[2] if len(sys.argv) > 2 else 'public'
        result = client.list_tables(schema)
        print(json.dumps(result, indent=2, default=str))
    
    elif command == "analyze_db_health":
        result = client.analyze_db_health()
        print(json.dumps(result, indent=2, default=str))
    
    else:
        print(f"Comando desconocido: {command}")

if __name__ == "__main__":
    main()
