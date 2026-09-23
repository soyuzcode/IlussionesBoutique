import Navbar from './components/layout/Navbar';
import Button from './components/common/Button';

export function App() {
  return (
    <div style={{ fontFamily: 'sans-serif', minHeight: '100vh', margin: 0 }}>
      <Navbar />
      <main style={{ padding: '2rem' }}>
        <h1>Panel Principal</h1>
        <p>Bienvenido al sistema de gestión de Ilusiones Boutique.</p>

        <div style={{ display: 'flex', gap: '10px', marginTop: '1rem' }}>
          <Button label="Nuevo Producto" onClick={() => alert('Crear producto')} />
          <Button label="Ver Ventas" variant="secondary" onClick={() => alert('Ver ventas')} />
        </div>
      </main>
    </div>
  );
}

export default App;