import type { FC } from 'react';

import logo from '../../assets/logo.jpg';

interface ServerWakeScreenProps {
  /** `connecting`: primer intento; `waking`: el servidor tarda; `failed`: se agotaron los intentos. */
  phase: 'connecting' | 'waking' | 'failed';
  onRetry: () => void;
}

const MESSAGES: Record<ServerWakeScreenProps['phase'], { title: string; detail: string }> = {
  connecting: { title: 'Conectando…', detail: 'Recuperando tu sesión.' },
  waking: {
    title: 'Despertando el servidor…',
    detail: 'El servidor se apaga cuando no se usa. La primera carga puede tardar hasta un minuto.',
  },
  failed: {
    title: 'El servidor no responde',
    detail: 'Revisa tu conexión o vuelve a intentarlo en unos segundos.',
  },
};

export const ServerWakeScreen: FC<ServerWakeScreenProps> = ({ phase, onRetry }) => {
  const { title, detail } = MESSAGES[phase];

  return (
    <div className="min-h-screen w-full bg-white flex items-center justify-center p-6" role="status" aria-live="polite">
      <div className="w-full max-w-sm text-center space-y-5">
        <img src={logo} alt="Logo Ilussiones Boutique" className="w-24 h-24 mx-auto rounded-full object-cover shadow-sm" />
        {phase !== 'failed' && (
          <div className="mx-auto w-10 h-10 rounded-full border-4 border-pink-200 border-t-pink-600 animate-spin" aria-hidden="true" />
        )}
        <div className="space-y-1">
          <h1 className="text-lg font-bold text-gray-800">{title}</h1>
          <p className="text-xs sm:text-sm text-gray-500">{detail}</p>
        </div>
        {phase === 'failed' && (
          <button
            type="button"
            onClick={onRetry}
            className="px-6 py-2.5 bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs sm:text-sm rounded-full shadow-md transition cursor-pointer"
          >
            Reintentar
          </button>
        )}
      </div>
    </div>
  );
};

export default ServerWakeScreen;
