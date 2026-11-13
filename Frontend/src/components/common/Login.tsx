import { useState } from 'react';
import type { FC, FormEvent } from 'react';

import { authApi } from '../../api/authApi';
import { ApiError } from '../../api/httpClient';
import { saveSession } from '../../lib/authSession';
import { auditEvent } from '../../lib/audit';
import logo from '../../assets/logo.jpg';
import type { AuthUser } from '../../types';

interface LoginProps {
  onLoginSuccess?: (user: AuthUser) => void;
}

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z]{2,}$/;
const INVALID_CREDENTIALS_MESSAGE = 'User or password wrong';
const UNAVAILABLE_MESSAGE =
  'Could not reach the server. Check your connection and try again.';

export const Login: FC<LoginProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [showForgot, setShowForgot] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetError, setResetError] = useState('');
  const [resetSent, setResetSent] = useState(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (submitting) return;

    auditEvent({
      eventType: 'UI_LOGIN_SUBMITTED',
      entityType: 'AUTH',
      metadata: { AUTH_METHOD: 'PASSWORD' },
    });

    setError('');

    if (!email.trim() || !password) {
      setError(INVALID_CREDENTIALS_MESSAGE);
      return;
    }

    setSubmitting(true);
    try {
      // El backend valida las credenciales y emite el token. El frontend no
      // compara contraseñas ni decide por su cuenta si el acceso es válido.
      const result = await authApi.login(email.trim().toLowerCase(), password);
      saveSession(result.token, result.user);
      onLoginSuccess?.(result.user);
    } catch (err) {
      // Mismo mensaje para usuario inexistente y contraseña incorrecta.
      const isRejectedCredentials =
        err instanceof ApiError && (err.status === 400 || err.status === 401);
      setError(
        isRejectedCredentials ? INVALID_CREDENTIALS_MESSAGE : UNAVAILABLE_MESSAGE,
      );
    } finally {
      setSubmitting(false);
    }
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
            disabled={submitting}
            className="w-full py-3.5 bg-pink-600 hover:bg-pink-700 disabled:bg-pink-300 disabled:cursor-not-allowed text-white font-bold text-xs sm:text-sm rounded-full shadow-md hover:shadow-lg transition active:scale-98 cursor-pointer mt-2"
          >
            {submitting ? 'Signing In…' : 'Sign In'}
          </button>
        </form>

        <div className="text-center space-y-4 pt-2">
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
