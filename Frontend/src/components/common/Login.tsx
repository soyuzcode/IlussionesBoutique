import React, { useState } from 'react';

interface LoginProps {
  onLoginSuccess?: () => void;
}

export const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!email.trim() || !password.trim()) {
      setErrorMsg('User or password wrong');
      return;
    }

    if (onLoginSuccess) {
      onLoginSuccess();
    }
  };

  return (
    <div className="min-h-screen w-full bg-white flex flex-col justify-center items-center p-6 sm:p-10">
      <div className="w-full max-w-md mx-auto space-y-6">
        
        {/* LOGO OFICIAL - CÍRCULO CON BORDES Y TIPOGRAFÍA DE LA BOUTIQUE */}
        <div className="flex flex-col items-center text-center space-y-3">
          <div className="w-36 h-36 rounded-full bg-[#fde8f0] border-4 border-dashed border-pink-400 p-2 flex items-center justify-center shadow-sm">
            <div className="w-full h-full rounded-full bg-white flex flex-col items-center justify-center p-2 text-center shadow-inner">
              <span className="font-serif italic text-2xl font-bold text-gray-800 tracking-tight leading-tight">
                Ilussiones Boutique
              </span>
              <span className="text-[10px] text-pink-400 font-light mt-0.5 tracking-wider">
                Una gran historia
              </span>
            </div>
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-700 mt-2">Bienvenida</h1>
            <p className="text-xs text-gray-400">Ingresa tus credenciales para acceder</p>
          </div>
        </div>

        {/* BANNER DE ERROR EN CASO DE CAMPOS VACÍOS O INCORRECTOS */}
        {errorMsg && (
          <div className="bg-red-50 border-l-4 border-red-500 p-3.5 rounded-r-xl text-xs text-red-700 font-semibold flex items-center gap-2 shadow-xs animate-fade-in">
            <span className="text-base">⚠️</span>
            <span>{errorMsg}</span>
          </div>
        )}

        {/* FORMULARIO */}
        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (errorMsg) setErrorMsg(null);
              }}
              placeholder="your.email@example.com"
              className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 focus:ring-2 focus:ring-pink-200 outline-none transition"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (errorMsg) setErrorMsg(null);
              }}
              placeholder="••••••••"
              className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 focus:ring-2 focus:ring-pink-200 outline-none transition"
            />
          </div>

          <button
            type="submit"
            className="w-full py-3.5 bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs sm:text-sm rounded-full shadow-md hover:shadow-lg transition active:scale-98 cursor-pointer mt-2"
          >
            Sign In
          </button>
        </form>

        <div className="text-center space-y-4 pt-2">
          <p className="text-xs text-gray-400 font-medium">or continue with</p>
          <button
            type="button"
            onClick={onLoginSuccess}
            className="w-full py-3 border border-gray-200 rounded-xl font-semibold text-xs sm:text-sm text-gray-700 hover:bg-gray-50 flex items-center justify-center gap-2 transition cursor-pointer"
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

          <a href="#forgot" className="block text-xs font-semibold text-blue-500 hover:underline">
            Forgot Password?
          </a>
        </div>
      </div>
    </div>
  );
};

export default Login;