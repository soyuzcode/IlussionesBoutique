import React, { useState } from 'react';

interface LoginProps {
  onLoginSuccess: () => void;
}

export function Login({ onLoginSuccess }: LoginProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLoginSuccess();
  };

  return (
    <div className="flex flex-col items-center justify-center h-full p-6 bg-white box-border">
      {/* Logo Placeholder */}
      <div className="w-20 h-20 rounded-full bg-pink-100 text-pink-600 flex items-center justify-center font-bold text-base mb-4 shadow-md">
        Logo
      </div>

      {/* Título de Marca */}
      <h2 className="m-0 text-pink-600 text-2xl font-bold">Ilussiones</h2>
      <p className="mt-1 mb-6 text-gray-500 text-xs text-center">
        Tienda de Ilussiones Boutique.
      </p>

      {/* Formulario */}
      <form onSubmit={handleSubmit} className="w-full flex flex-col gap-3">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-semibold text-gray-700">Email</label>
          <input
            type="text"
            placeholder="your.email@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="p-2.5 rounded-lg border border-gray-300 text-xs outline-none focus:border-pink-500"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-semibold text-gray-700">Password</label>
          <input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="p-2.5 rounded-lg border border-gray-300 text-xs outline-none focus:border-pink-500"
          />
        </div>

        <button
          type="submit"
          className="mt-2 p-3 bg-pink-600 text-white border-none rounded-full font-bold text-sm cursor-pointer shadow-md active:scale-95 transition"
        >
          Sign In
        </button>
      </form>

      {/* Continuar con Google */}
      <div className="w-full text-center mt-6">
        <span className="text-xs text-gray-400">or continue with</span>
        <button
          type="button"
          onClick={onLoginSuccess}
          className="w-full mt-3 p-2.5 rounded-xl border border-gray-200 bg-white text-xs text-gray-700 font-semibold cursor-pointer flex items-center justify-center gap-2 hover:bg-gray-50 active:scale-95 transition shadow-sm"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
            />
          </svg>
          Google
        </button>
      </div>

      <a
        href="#forgot"
        onClick={(e) => e.preventDefault()}
        className="mt-4 text-xs text-blue-600 no-underline hover:underline"
      >
        Forgot Password?
      </a>
    </div>
  );
}

export default Login;