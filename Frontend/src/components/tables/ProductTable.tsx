import { mockProducts } from '../../data/products';
import type { Product } from '../../data/products';

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
