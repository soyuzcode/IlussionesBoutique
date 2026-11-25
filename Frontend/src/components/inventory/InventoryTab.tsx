import { useEffect, useMemo, useState } from 'react';
import { productsApi } from '../../api/productsApi';
import type { Product, ProductInput } from '../../types';
import ProductTable from '../tables/ProductTable';
import { LOW_STOCK_THRESHOLD, PRODUCT_TYPE_LABELS } from './productLabels';
import ProductModal from './ProductModal';

export function InventoryTab() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [editing, setEditing] = useState<Product | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    productsApi
      .getAll()
      .then((result) => {
        if (active) setProducts(result);
      })
      .catch(() => {
        if (active) setError('No se pudo cargar el inventario. Verifica que el servidor esté activo.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return products.filter(
      (p) =>
        (!typeFilter || p.type === typeFilter) &&
        (!term || (p.name ?? '').toLowerCase().includes(term) || (p.materials ?? '').toLowerCase().includes(term)),
    );
  }, [products, search, typeFilter]);

  const totalUnits = products.reduce((sum, p) => sum + (p.stock ?? 0), 0);
  const lowStock = products.filter((p) => p.status !== 'DISCONTINUED' && (p.stock ?? 0) <= LOW_STOCK_THRESHOLD);

  const replace = (updated: Product) =>
    setProducts((current) => current.map((p) => (p.productId === updated.productId ? updated : p)));

  const handleAdjust = async (product: Product, delta: number) => {
    setBusyId(product.productId);
    try {
      replace(await productsApi.adjustStock(product.productId, delta));
    } catch {
      setError('No se pudo actualizar el stock.');
    } finally {
      setBusyId(null);
    }
  };

  const handleSave = async (data: ProductInput) => {
    if (editing) {
      replace(await productsApi.update(editing.productId, data));
    } else {
      const created = await productsApi.create(data);
      setProducts((current) => [...current, created].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '')));
    }
    setEditing(null);
    setIsCreating(false);
  };

  const handleDelete = async (product: Product) => {
    if (!window.confirm(`¿Eliminar "${product.name}" del inventario?`)) return;
    try {
      await productsApi.remove(product.productId);
      setProducts((current) => current.filter((p) => p.productId !== product.productId));
    } catch {
      setError('No se pudo eliminar. Puede que el producto esté asociado a un pedido.');
    }
  };

  return (
    <section className="w-full space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-sm sm:text-base font-bold text-gray-800">📦 Inventario</h2>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="py-2 px-3 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer"
        >
          + Agregar producto
        </button>
      </div>

      <div className="grid grid-cols-3 gap-2 text-center">
        <div className="bg-gray-50 border border-gray-100 rounded-xl p-3">
          <p className="text-lg font-bold text-gray-800">{products.length}</p>
          <p className="text-[10px] uppercase font-bold text-gray-400">Productos</p>
        </div>
        <div className="bg-gray-50 border border-gray-100 rounded-xl p-3">
          <p className="text-lg font-bold text-gray-800">{totalUnits}</p>
          <p className="text-[10px] uppercase font-bold text-gray-400">Unidades</p>
        </div>
        <div className={`border rounded-xl p-3 ${lowStock.length ? 'bg-red-50 border-red-100' : 'bg-gray-50 border-gray-100'}`}>
          <p className={`text-lg font-bold ${lowStock.length ? 'text-red-600' : 'text-gray-800'}`}>{lowStock.length}</p>
          <p className="text-[10px] uppercase font-bold text-gray-400">Stock bajo</p>
        </div>
      </div>

      <div className="flex gap-2">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar producto..."
          className="flex-1 p-2.5 text-xs border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500"
        />
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="p-2.5 text-xs border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500"
        >
          <option value="">Todas</option>
          {Object.entries(PRODUCT_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
      </div>

      {error && <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl">{error}</p>}

      {loading ? (
        <p className="text-center py-8 text-xs text-gray-400">Cargando inventario...</p>
      ) : filtered.length === 0 ? (
        <p className="text-center py-8 text-xs text-gray-400">No hay productos.</p>
      ) : (
        <ProductTable
          products={filtered}
          busyId={busyId}
          onAdjustStock={handleAdjust}
          onEdit={setEditing}
          onDelete={handleDelete}
        />
      )}

      {(isCreating || editing) && (
        <ProductModal
          key={editing?.productId ?? 'new'}
          product={editing}
          onClose={() => {
            setEditing(null);
            setIsCreating(false);
          }}
          onSave={handleSave}
        />
      )}
    </section>
  );
}

export default InventoryTab;
