import React from 'react';

export function Navbar() {
  return (
    <nav
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '1rem 2rem',
        backgroundColor: '#1E293B',
        color: '#FFFFFF',
      }}
    >
      <h2 style={{ margin: 0, color: '#FFFFFF' }}>Ilusiones Boutique</h2>
      <ul
        style={{
          display: 'flex',
          gap: '20px',
          listStyle: 'none',
          margin: 0,
          padding: 0,
        }}
      >
        <li>Inicio</li>
        <li>Productos</li>
        <li>Ventas</li>
      </ul>
    </nav>
  );
}

export default Navbar;