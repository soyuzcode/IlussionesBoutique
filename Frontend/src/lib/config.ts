const DEFAULT_API_BASE_URL =
  'https://ilussionesboutique-production.up.railway.app';

const configuredBaseUrl = import.meta.env.VITE_API_URL?.trim();

export const API_BASE_URL = (
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl
    : DEFAULT_API_BASE_URL
).replace(/\/+$/, '');
