import React from 'react';

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export function Navbar({ activeTab, setActiveTab }: NavbarProps) {
  const navItems = [
    { id: 'Inicio', label: 'Inicio', icon: '🏠' },
    { id: 'Clientes', label: 'Clientes', icon: '👥' },
    { id: 'Pedidos', label: 'Pedidos', icon: '🧵' },
    { id: 'Inventario', label: 'Inventario', icon: '📦' },
  ];

  return (
    <nav className="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around py-2.5 z-50 rounded-b-[24px]">
      {navItems.map((item) => {
        const isActive = activeTab === item.id;
        return (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`flex flex-col items-center gap-0.5 bg-transparent border-none cursor-pointer text-[10px] font-semibold transition ${
              isActive ? 'text-pink-600' : 'text-gray-400 hover:text-pink-600'
            }`}
          >
            <span className="text-xl">{item.icon}</span>
            {item.label}
          </button>
        );
      })}
    </nav>
  );
}

export default Navbar;