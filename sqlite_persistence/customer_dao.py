from database import get_connection

class CustomerDAO:
    """Clase DAO para gestionar las operaciones de persistencia de la entidad Customer."""

    def insert(self, customer_id, name, phone):
        """Inserta un nuevo cliente en la base de datos usando consultas parametrizadas."""
        with get_connection() as connection:
            cursor = connection.cursor()
            cursor.execute(
                """
                INSERT INTO customer (id, name, phone)
                VALUES (?, ?, ?)
                """,
                (customer_id, name, phone)
            )
            connection.commit()
            return customer_id

    def get_by_id(self, customer_id):
        """Busca y devuelve un cliente por su ID en formato de diccionario."""
        with get_connection() as connection:
            cursor = connection.cursor()
            cursor.execute(
                """
                SELECT id, name, phone, created_at, updated_at
                FROM customer
                WHERE id = ?
                """,
                (customer_id,)
            )
            row = cursor.fetchone()
            
            if row:
                return {
                    "id": row[0],
                    "name": row[1],
                    "phone": row[2],
                    "created_at": row[3],
                    "updated_at": row[4]
                }
            return None

    def get_all(self):
        """Devuelve una lista con todos los clientes registrados."""
        with get_connection() as connection:
            cursor = connection.cursor()
            cursor.execute("SELECT id, name, phone, created_at, updated_at FROM customer")
            rows = cursor.fetchall()
            
            return [
                {
                    "id": row[0],
                    "name": row[1],
                    "phone": row[2],
                    "created_at": row[3],
                    "updated_at": row[4]
                }
                for row in rows
            ]