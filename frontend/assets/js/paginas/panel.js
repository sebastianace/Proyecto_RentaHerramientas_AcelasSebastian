// Panel por rol. Cada rol tiene su menú; cada opción es una "sección" (una función que pinta
// el contenido principal). La navegación usa el hash de la URL (#reservas, #inventario…),
// así los botones atrás/adelante del navegador funcionan y cada vista se puede enlazar.

import { get } from '../api.js';
import { sesion } from '../sesion.js';
import { icono } from '../iconos.js';
import { montarLayout, $, esc, vacio } from '../ui.js';
import { seccionesCliente } from '../secciones/cliente.js';
import { seccionesProveedor } from '../secciones/proveedor.js';
import { seccionesAdmin } from '../secciones/admin.js';

if (sesion.exigir()) {
  montarLayout({ activa: 'panel', sinPie: true });
  iniciar();
}

async function iniciar() {
  // El perfil (clienteId / proveedorId) se guarda al ingresar; si falta, se pide de nuevo
  if (!sesion.perfil()) {
    try {
      const p = await get('/auth/me');
      sesion.guardar(sesion.datos(), { clienteId: p.clienteId, proveedorId: p.proveedorId });
    } catch { /* las secciones que lo necesiten mostrarán el error */ }
  }

  const rol = sesion.rol();
  const secciones = { CLIENTE: seccionesCliente, PROVEEDOR: seccionesProveedor, ADMIN: seccionesAdmin }[rol];
  const nombresRol = { CLIENTE: 'Cliente', PROVEEDOR: 'Proveedor', ADMIN: 'Administrador' };

  $('[data-side]').innerHTML = `
    <div class="side-user"><span class="avatar">${esc(sesion.iniciales())}</span>
      <div><b>${esc(sesion.nombre())}</b><span class="badge accent">${nombresRol[rol]}</span></div></div>
    <span class="side-label">Menú</span>
    <nav>${secciones.map((s) => `<a href="#${s.id}" data-nav="${s.id}">${icono(s.icono)}${esc(s.titulo)}<span class="count hidden" data-count="${s.id}"></span></a>`).join('')}</nav>
    <span class="side-label">Accesos</span>
    <nav><a href="index.html">${icono('home')}Ir a la tienda</a></nav>`;

  async function mostrar() {
    const id = location.hash.slice(1) || secciones[0].id;
    const seccion = secciones.find((s) => s.id === id) || secciones[0];
    document.querySelectorAll('[data-nav]').forEach((a) => {
      if (a.dataset.nav === seccion.id) a.setAttribute('aria-current', 'page');
      else a.removeAttribute('aria-current');
    });
    document.title = `${seccion.titulo} · ObraYa`;
    const main = $('[data-main]');
    // Cada sección recibe un contenedor NUEVO: así los eventos de la sección anterior se descartan con él
    // y no se acumulan listeners al navegar (una acción no se ejecuta dos veces).
    const vista = document.createElement('div');
    vista.innerHTML = '<div class="stack"><div class="skeleton" style="height:42px;width:40%"></div><div class="skeleton" style="height:140px"></div><div class="skeleton" style="height:280px"></div></div>';
    main.replaceChildren(vista);
    try {
      await seccion.pintar(vista);
    } catch (error) {
      vista.innerHTML = vacio('No se pudo cargar esta sección', error.message);
    }
    main.focus({ preventScroll: true });
    window.scrollTo({ top: 0 });
  }

  window.addEventListener('hashchange', mostrar);
  window.refrescarSeccion = mostrar;
  mostrar();

  // Contadores del menú (ej. solicitudes pendientes), sin bloquear la carga
  secciones.filter((s) => s.contar).forEach(async (s) => {
    try {
      const n = await s.contar();
      const el = document.querySelector(`[data-count="${s.id}"]`);
      if (n > 0) { el.textContent = n; el.classList.remove('hidden'); }
    } catch { /* sin contador */ }
  });
}
