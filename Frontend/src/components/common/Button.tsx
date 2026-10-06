import React from 'react';

interface ButtonProps {
  label: string;
  onClick?: () => void;
  variant?: 'primary' | 'secondary';
}

export function Button({ label, onClick, variant = 'primary' }: ButtonProps) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: '8px 16px',
        borderRadius: '6px',
        border: 'none',
        cursor: 'pointer',
        fontWeight: 'bold',
        backgroundColor: variant === 'primary' ? '#2563EB' : '#E2E8F0',
        color: variant === 'primary' ? '#FFFFFF' : '#1E293B',
      }}
    >
      {label}
    </button>
  );
}

export default Button;