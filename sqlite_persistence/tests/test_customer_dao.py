import unittest
import sqlite3
import sys
import os
from unittest.mock import patch

# Agrega la ruta superior para importar los módulos correctamente
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from customer_dao import CustomerDAO

class TestCustomerDAO(unittest.TestCase):
    """Pruebas unitarias para verificar el funcionamiento del DAO de clientes."""

    def setUp(self):
        """Se ejecuta antes de cada prueba para preparar una base de datos en memoria."""
        self.connection = sqlite3.connect(":memory:")
        self.connection.execute("PRAGMA foreign_keys = ON;")
        
        # Creamos la tabla customer en la BD temporal en memoria
        self.connection.execute("""
            CREATE TABLE IF NOT EXISTS customer (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self.connection.commit()
        
        self.customer_dao = CustomerDAO()

    def tearDown(self):
        """Se ejecuta al finalizar cada prueba para cerrar la conexión."""
        self.connection.close()

    @patch("customer_dao.get_connection")
    def test_insert_and_get_by_id(self, mock_get_connection):
        """Prueba que se pueda insertar un cliente y recuperarlo por su ID."""
        # Hacemos que el DAO use nuestra conexión en memoria durante la prueba
        mock_get_connection.return_value = self.connection

        customer_id = "uuid-test-123"
        name = "María López"
        phone = "7123-4567"

        # 1. Probar inserción
        inserted_id = self.customer_dao.insert(customer_id, name, phone)
        self.assertEqual(inserted_id, customer_id)

        # 2. Probar obtención por ID
        customer = self.customer_dao.get_by_id(customer_id)
        self.assertIsNotNone(customer)
        self.assertEqual(customer["name"], name)
        self.assertEqual(customer["phone"], phone)

    @patch("customer_dao.get_connection")
    def test_get_all_customers(self, mock_get_connection):
        """Prueba que devuelva todos los clientes registrados."""
        mock_get_connection.return_value = self.connection

        # Insertamos un par de clientes de prueba
        self.customer_dao.insert("id-1", "Carlos Gomez", "7000-0001")
        self.customer_dao.insert("id-2", "Sofia Rivas", "7000-0002")

        # Obtenemos todos
        customers = self.customer_dao.get_all()
        self.assertEqual(len(customers), 2)
        self.assertEqual(customers[0]["name"], "Carlos Gomez")
        self.assertEqual(customers[1]["name"], "Sofia Rivas")

if __name__ == "__main__":
    unittest.main()