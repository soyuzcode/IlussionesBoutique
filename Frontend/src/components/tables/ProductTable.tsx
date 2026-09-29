import React from 'react';

export interface Product {
  id: number;
  nombre: string;
  categoria: string;
  stock: number;
  estado: 'Disponible' | 'Agotado';
}

export const mockProducts: Product[] = [
  { id: 1, nombre: 'Vestido Novia Elegante', categoria: 'Novias', stock: 12, estado: 'Disponible' },
  { id: 2, nombre: 'Vestido Quinceañera Rosa', categoria: 'Quince', stock: 25, estado: 'Disponible' },
  { id: 3, nombre: 'Accesorio Tocado Cristal', categoria: 'Accesorios', stock: 0, estado: 'Agotado' },
  { id: 4, nombre: 'Vestido Noche Rojo', categoria: 'Fiesta', stock: 8, estado: 'Disponible' },
  { id: 5, nombre: 'Velos de Seda Mantilla', categoria: 'Accesorios', stock: 15, estado: 'Disponible' },
];

interface ProductTableProps {
  products?: Product[];
}

export function ProductTable({ products = mockProducts }: ProductTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-100 bg-white shadow-sm">
      <table className="w-full text-left text-xs border-collapse">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-100 text-gray-700 font-semibold">
            <th className="p-2.5">Producto</th>
            <th className="p-2.5">Categoría</th>
            <th className="p-2.5">Stock</th>
            <th className="p-2.5">Estado</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {products.map((p) => (
            <tr key={p.id} className="hover:bg-pink-50/40 transition">
              <td className="p-2.5 font-bold text-gray-800">{p.nombre}</td>
              <td className="p-2.5 text-gray-500">{p.categoria}</td>
              <td className="p-2.5 text-gray-700 font-semibold">{p.stock}</td>
              <td className="p-2.5">
                <span
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                    p.estado === 'Disponible'
                      ? 'bg-pink-100 text-pink-700'
                      : 'bg-red-100 text-red-700'
                  }`}
                >
                  {p.estado}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ProductTable;