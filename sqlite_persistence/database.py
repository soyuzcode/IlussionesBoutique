import sqlite3

DB_NAME = "database.db"

def get_connection():
    """Establece la conexión con SQLite y activa las llaves foráneas."""
    connection = sqlite3.connect(DB_NAME)
    connection.execute("PRAGMA foreign_keys = ON;")
    return connection

def initialize_database():
    """Crea la tabla de clientes igualando el esquema de la entidad Customer en Java."""
    with get_connection() as connection:
        cursor = connection.cursor()
        
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS customer (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        connection.commit()

if __name__ == "__main__":
    initialize_database()
    print("Base de datos inicializada con éxito.")