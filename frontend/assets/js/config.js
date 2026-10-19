// Configuración del frontend.
// La URL de la API se puede cambiar sin tocar código: abre cualquier página con ?api=http://localhost:8081
// y queda guardada en el navegador (útil si el backend corre en otro puerto).

const CLAVE_API = 'obraya.api';

function leerApi() {
  try {
    const desdeUrl = new URLSearchParams(location.search).get('api');
    if (desdeUrl) localStorage.setItem(CLAVE_API, desdeUrl);
    return localStorage.getItem(CLAVE_API);
  } catch {
    return null;
  }
}

export const API_URL = (leerApi() || 'http://localhost:8080').replace(/\/$/, '');
export const MARCA = 'ObraYa';
export const IVA = 0.19;

/** Tarjetas de la pasarela simulada (se muestran como ayuda en el pago). */
export const TARJETAS_PRUEBA = [
  { numero: '4242 4242 4242 4242', resultado: 'Aprobada', tipo: 'ok' },
  { numero: '4000 0000 0000 0000', resultado: 'Rechazada · fondos insuficientes', tipo: 'bad' },
  { numero: '4111 1111 1111 1111', resultado: 'Rechazada por el banco', tipo: 'bad' },
];

/** Usuarios de los datos de prueba (clave de todos: Renta2026*). */
export const USUARIOS_DEMO = [
  { rol: 'CLIENTE', nombre: 'Juan Pablo Ardila', email: 'juanpablo.ardila@gmail.com' },
  { rol: 'PROVEEDOR', nombre: 'Herramientas del Oriente', email: 'carlos.rueda@herroriente.com' },
  { rol: 'ADMIN', nombre: 'Administrador', email: 'admin@rentaherramientas.com' },
];
export const CLAVE_DEMO = 'Renta2026*';
