import { useState } from 'react';
import type { FC, FormEvent } from 'react';

import { auditEvent } from '../../lib/audit';
import logo from '../../assets/logo.jpg';
import type { AuthUser } from '../../types';

interface LoginProps {
  onLoginSuccess?: (user: AuthUser) => void;
}

const MOCK_USERS = [{ email: 'admin@ilussiones.com', password: '123456', name: 'Admin' }];
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z]{2,}$/;
const ERROR_MESSAGE = 'User or password wrong';

export const Login: FC<LoginProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const [showForgot, setShowForgot] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetError, setResetError] = useState('');
  const [resetSent, setResetSent] = useState(false);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    auditEvent({
      eventType: 'UI_LOGIN_SUBMITTED',
      entityType: 'AUTH',
      metadata: { AUTH_METHOD: 'PASSWORD' },
    });

    const cleanEmail = email.trim().toLowerCase();

    if (!cleanEmail || !password.trim() || !EMAIL_REGEX.test(cleanEmail)) {
      setError(ERROR_MESSAGE);
      return;
    }

    const user = MOCK_USERS.find((u) => u.email === cleanEmail && u.password === password);
    if (!user) {
      setError(ERROR_MESSAGE);
      return;
    }

    setError('');
    onLoginSuccess?.({ name: user.name, email: user.email });
  };

  const handleGoogleLogin = () => {
    auditEvent({
      eventType: 'UI_LOGIN_SUBMITTED',
      entityType: 'AUTH',
      metadata: { AUTH_METHOD: 'GOOGLE' },
    });

    // Simulado: el login real con Google requiere autenticación en el backend (p. ej. Supabase Auth)
    onLoginSuccess?.({ name: 'Google User', email: '' });
  };

  const handleReset = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!EMAIL_REGEX.test(resetEmail.trim())) {
      setResetError('Enter a valid email address.');
      return;
    }
    // TODO: conectar con la recuperación real (p. ej. supabase.auth.resetPasswordForEmail)
    setResetError('');
    setResetSent(true);
  };

  const closeForgot = () => {
    setShowForgot(false);
    setResetEmail('');
    setResetError('');
    setResetSent(false);
  };

  return (
    <div className="min-h-screen w-full bg-white flex flex-col justify-center items-center p-6 sm:p-10">
      <div className="w-full max-w-md mx-auto space-y-6">
        <div className="flex flex-col items-center text-center space-y-3">
          <img
            src={logo}
            alt="Logo Ilussiones Boutique"
            className="w-24 h-24 sm:w-28 sm:h-28 rounded-full object-cover shadow-sm"
          />
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-pink-600 tracking-tight">Ilussiones</h1>
            <p className="text-xs sm:text-sm text-gray-500 mt-1">Tienda de Ilussiones Boutique.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4 pt-2">
          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setError('');
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
                setError('');
              }}
              placeholder="••••••••"
              className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 focus:ring-2 focus:ring-pink-200 outline-none transition"
            />
          </div>

          {error && (
            <div
              role="alert"
              className="bg-red-50 border border-red-200 text-red-600 text-xs sm:text-sm font-semibold p-3 rounded-xl text-center"
            >
              {error}
            </div>
          )}

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
            onClick={handleGoogleLogin}
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

          <button
            type="button"
            onClick={() => setShowForgot(true)}
            className="block mx-auto text-xs font-semibold text-blue-500 hover:underline cursor-pointer bg-transparent border-none"
          >
            Forgot Password?
          </button>
        </div>
      </div>

      {showForgot && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <form onSubmit={handleReset} noValidate className="bg-white w-full max-w-sm rounded-2xl p-5 space-y-4 shadow-xl">
            <div className="flex justify-between items-center border-b border-gray-100 pb-2">
              <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">Reset password</h3>
              <button
                type="button"
                onClick={closeForgot}
                className="text-gray-400 hover:text-gray-600 text-xs font-bold p-1 cursor-pointer"
              >
                ✕
              </button>
            </div>

            {resetSent ? (
              <>
                <p className="text-xs sm:text-sm text-gray-600">
                  If the email is registered, you will receive instructions to reset your password.
                </p>
                <button
                  type="button"
                  onClick={closeForgot}
                  className="w-full py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Back to login
                </button>
              </>
            ) : (
              <>
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Email</label>
                  <input
                    type="email"
                    value={resetEmail}
                    onChange={(e) => {
                      setResetEmail(e.target.value);
                      setResetError('');
                    }}
                    placeholder="your.email@example.com"
                    className="w-full p-3 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none"
                  />
                </div>
                {resetError && (
                  <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl text-center">
                    {resetError}
                  </p>
                )}
                <button
                  type="submit"
                  className="w-full py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Send instructions
                </button>
              </>
            )}
          </form>
        </div>
      )}
    </div>
  );
};

export default Login;
