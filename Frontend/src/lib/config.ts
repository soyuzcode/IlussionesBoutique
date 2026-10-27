const DEFAULT_API_BASE_URL =
  'http://ilussionesboutique-production.up.railway.app/api';

const configuredBaseUrl = import.meta.env.VITE_API_URL?.trim();

export const API_BASE_URL = (
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl
    : DEFAULT_API_BASE_URL
).replace(/\/+$/, '');
