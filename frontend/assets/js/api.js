// Cliente HTTP de la API. Toda petición pasa por aquí: agrega el JWT, convierte los errores
// ProblemDetail (RFC 9457) del backend en ApiError y maneja la sesión vencida (401).

import { API_URL } from './config.js';
import { sesion } from './sesion.js';

export class ApiError extends Error {
  constructor(status, problema) {
    super(problema?.detail || problema?.title || `Error HTTP ${status}`);
    this.status = status;
    this.titulo = problema?.title || 'Error';
    this.errores = problema?.errores || null; // errores por campo de @Valid (400)
  }
}

export async function api(ruta, { metodo = 'GET', cuerpo, formulario, blob = false } = {}) {
  const headers = { Accept: 'application/json' };
  const token = sesion.token();
  if (token) headers.Authorization = `Bearer ${token}`;

  let body;
  if (formulario) {
    body = formulario; // multipart: el navegador pone el Content-Type con el boundary
  } else if (cuerpo !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/v1${ruta}`, { method: metodo, headers, body });
  } catch {
    throw new ApiError(0, {
      title: 'Sin conexión con el servidor',
      detail: `No se pudo conectar con la API en ${API_URL}. Verifica que el backend esté corriendo.`,
    });
  }

  if (respuesta.status === 401 && token) {
    // El token venció o fue rechazado: se cierra la sesión y se pide iniciar de nuevo
    sesion.cerrar();
    const volver = location.pathname.split('/').pop() + location.search + location.hash;
    location.href = `ingresar.html?expirada=1&volver=${encodeURIComponent(volver)}`;
    throw new ApiError(401, { title: 'Sesión vencida', detail: 'Tu sesión venció. Inicia sesión de nuevo.' });
  }

  if (!respuesta.ok) {
    let problema = null;
    try { problema = await respuesta.json(); } catch { /* sin cuerpo */ }
    throw new ApiError(respuesta.status, problema);
  }

  if (blob) return respuesta.blob();
  if (respuesta.status === 204) return null;
  const texto = await respuesta.text();
  return texto ? JSON.parse(texto) : null;
}

export const get = (ruta) => api(ruta);
export const post = (ruta, cuerpo) => api(ruta, { metodo: 'POST', cuerpo });
export const put = (ruta, cuerpo) => api(ruta, { metodo: 'PUT', cuerpo });
export const patch = (ruta, cuerpo) => api(ruta, { metodo: 'PATCH', cuerpo });
export const del = (ruta) => api(ruta, { metodo: 'DELETE' });

/** Arma una query string ignorando los valores vacíos. */
export function query(params) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

/** Las imágenes subidas vienen como /media/...; las de ejemplo, como URL completa. */
export function urlMedia(url) {
  if (!url) return null;
  return /^https?:\/\//.test(url) ? url : `${API_URL}${url}`;
}

/** Las imágenes de los datos de prueba son placeholders con texto: se reemplazan por la ilustración. */
export function esImagenReal(url) {
  return !!url && !url.includes('placehold.co');
}
