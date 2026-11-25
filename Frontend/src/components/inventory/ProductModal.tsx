import { useState, type FormEvent } from 'react';
import type { Product, ProductInput, ProductStatus, ProductType } from '../../types';
import { PRODUCT_STATUS_LABELS, PRODUCT_TYPE_LABELS } from './productLabels';

interface ProductModalProps {
  product?: Product | null;
  onClose: () => void;
  onSave: (data: ProductInput) => Promise<void>;
}

const inputClass = 'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';

export function ProductModal({ product, onClose, onSave }: ProductModalProps) {
  const [name, setName] = useState(product?.name ?? '');
  const [type, setType] = useState<ProductType>(product?.type ?? 'DRESS');
  const [price, setPrice] = useState(product ? String(product.price) : '');
  const [stock, setStock] = useState(product ? String(product.stock ?? 0) : '1');
  const [materials, setMaterials] = useState(product?.materials ?? '');
  const [status, setStatus] = useState<ProductStatus | ''>(
    product?.status === 'DISCONTINUED' ? 'DISCONTINUED' : '',
  );
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const priceValue = Number(price);
    const stockValue = Number(stock);
    if (name.trim().length < 2) return setError('El nombre debe tener al menos 2 caracteres.');
    if (!Number.isFinite(priceValue) || priceValue < 0) return setError('El precio no puede ser negativo.');
    if (!Number.isInteger(stockValue) || stockValue < 0) return setError('El stock debe ser un número entero mayor o igual a 0.');

    setSaving(true);
    setError('');
    try {
      await onSave({
        name: name.trim(),
        type,
        price: priceValue,
        stock: stockValue,
        materials: materials.trim(),
        ...(status ? { status } : {}),
      });
    } catch {
      setError('No se pudo guardar el producto.');
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <form onSubmit={handleSubmit} noValidate className="bg-white w-full max-w-md rounded-2xl p-5 space-y-3 shadow-xl text-xs">
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 uppercase tracking-wider">{product ? 'Editar producto' : 'Agregar producto'}</h3>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 font-bold p-1 cursor-pointer">✕</button>
        </div>
        <label className="block">
          <span className="text-gray-400 text-[10px] uppercase font-bold">Nombre</span>
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} className={inputClass} />
        </label>
        <div className="grid grid-cols-2 gap-2">
          <label className="block">
            <span className="text-gray-400 text-[10px] uppercase font-bold">Categoría</span>
            <select value={type} onChange={(e) => setType(e.target.value as ProductType)} className={inputClass}>
              {Object.entries(PRODUCT_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="text-gray-400 text-[10px] uppercase font-bold">Estado</span>
            <select value={status} onChange={(e) => setStatus(e.target.value as ProductStatus | '')} className={inputClass}>
              <option value="">Automático</option>
              {Object.entries(PRODUCT_STATUS_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="text-gray-400 text-[10px] uppercase font-bold">Precio (USD)</span>
            <input type="number" min="0" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} className={inputClass} />
          </label>
          <label className="block">
            <span className="text-gray-400 text-[10px] uppercase font-bold">Stock</span>
            <input type="number" min="0" step="1" value={stock} onChange={(e) => setStock(e.target.value)} className={inputClass} />
          </label>
        </div>
        <label className="block">
          <span className="text-gray-400 text-[10px] uppercase font-bold">Materiales / descripción</span>
          <input value={materials} onChange={(e) => setMaterials(e.target.value)} className={inputClass} />
        </label>
        {error && <p role="alert" className="bg-red-50 border border-red-200 text-red-600 font-semibold p-2.5 rounded-xl">{error}</p>}
        <div className="flex gap-2">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-bold cursor-pointer">Cancelar</button>
          <button type="submit" disabled={saving} className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl font-bold cursor-pointer disabled:opacity-50">
            {saving ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default ProductModal;
