// Ingreso y registro. Tras autenticarse se guarda el JWT y se consulta /auth/me para saber
// el clienteId o proveedorId del usuario (los paneles lo necesitan).

import { get, post } from '../api.js';
import { sesion } from '../sesion.js';
import { carrito } from '../carrito.js';
import { USUARIOS_DEMO, CLAVE_DEMO } from '../config.js';
import { montarLayout, $, $$, esc, cargando, mostrarErrorEnFormulario } from '../ui.js';

montarLayout({ sinPie: true });

const params = new URLSearchParams(location.search);

if (params.get('expirada')) {
  $('[data-aviso]').innerHTML = 'Tu sesión venció. Ingresa de nuevo para continuar.';
  $('[data-aviso]').classList.remove('hidden');
}

function mostrarTab(tab) {
  $$('[data-tab]').forEach((b) => b.setAttribute('aria-selected', String(b.dataset.tab === tab)));
  $$('[data-form]').forEach((f) => f.classList.toggle('hidden', f.dataset.form !== tab));
}
$$('[data-tab]').forEach((b) => b.addEventListener('click', () => mostrarTab(b.dataset.tab)));
if (location.hash === '#registro') mostrarTab('registro');

$('[data-demo]').innerHTML = USUARIOS_DEMO.map((u) => `
  <button type="button" class="demo-user" data-email="${esc(u.email)}">
    <span class="avatar">${u.rol[0]}</span>
    <span><b>${esc(u.nombre)}</b><small>${u.rol} · ${esc(u.email)}</small></span>
  </button>`).join('');

$('[data-demo]').addEventListener('click', (e) => {
  const b = e.target.closest('[data-email]');
  if (!b) return;
  const f = $('[data-form="login"]');
  f.email.value = b.dataset.email;
  f.password.value = CLAVE_DEMO;
  f.requestSubmit();
});

/** A dónde ir después de ingresar: a donde venía, al checkout si tiene carrito, o a su panel. */
function destino() {
  const volver = params.get('volver');
  if (volver && !volver.startsWith('http') && !volver.startsWith('//')) return volver; // solo rutas internas
  if (sesion.rol() === 'CLIENTE' && carrito.cantidad() > 0) return 'reserva.html';
  return 'panel.html';
}

async function completarSesion(autenticacion) {
  sesion.guardar(autenticacion);
  try {
    const perfil = await get('/auth/me');
    sesion.guardar(autenticacion, { clienteId: perfil.clienteId, proveedorId: perfil.proveedorId });
  } catch { /* el perfil se puede volver a pedir desde el panel */ }
  location.href = destino();
}

for (const form of $$('[data-form]')) {
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const boton = $('button[type="submit"]', form);
    const caja = $('[data-error]', form);
    caja.classList.add('hidden');
    $$('[aria-invalid]', form).forEach((c) => c.removeAttribute('aria-invalid'));
    const datos = Object.fromEntries(new FormData(form));
    cargando(boton, true);
    try {
      const ruta = form.dataset.form === 'login' ? '/auth/login' : '/auth/registro';
      if (!datos.telefono) delete datos.telefono;
      await completarSesion(await post(ruta, datos));
    } catch (error) {
      mostrarErrorEnFormulario(form, caja, error);
      cargando(boton, false);
    }
  });
}
