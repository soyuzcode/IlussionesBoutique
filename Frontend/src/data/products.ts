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
