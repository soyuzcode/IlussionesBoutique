import { useEffect, useState } from 'react';
import type { Product } from '../../types';
import { productsApi } from '../../api/productsApi';

/** Productos terminados con stock que se pueden agregar a un pedido. */
export function useAvailableProducts(active = true) {
  const [products, setProducts] = useState<Product[]>([]);
  useEffect(() => {
    if (!active) return;
    productsApi
      .getAll()
      .then((all) => setProducts(all.filter((p) => p.status === 'AVAILABLE' && (p.stock ?? 0) > 0)))
      .catch(() => setProducts([]));
  }, [active]);
  return products;
}
