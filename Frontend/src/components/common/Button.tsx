import React from 'react';

interface ButtonProps {
  label: string;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'outline';
}

export function Button({ label, onClick, variant = 'primary' }: ButtonProps) {
  let backgroundColor = '#D81B60';
  let color = '#FFFFFF';
  let border = 'none';

  if (variant === 'secondary') {
    backgroundColor = '#FCE4EC';
    color = '#D81B60';
  } else if (variant === 'outline') {
    backgroundColor = 'transparent';
    color = '#D81B60';
    border = '1px solid #D81B60';
  }

  return (
    <button
      onClick={onClick}
      style={{
        padding: '10px 20px',
        borderRadius: '20px',
        border,
        backgroundColor,
        color,
        fontWeight: '600',
        fontSize: '0.9rem',
        cursor: 'pointer',
        boxShadow: variant === 'primary' ? '0 2px 4px rgba(216, 27, 96, 0.25)' : 'none',
        transition: 'all 0.2s ease',
      }}
    >
      {label}
    </button>
  );
}

export default Button;