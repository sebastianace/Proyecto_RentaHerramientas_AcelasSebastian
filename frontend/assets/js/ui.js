// Piezas de interfaz compartidas por todas las páginas: formato, notificaciones, modales,
// insignias de estado y el layout común (encabezado, pie y carrito lateral).

import { MARCA } from './config.js';
import { sesion } from './sesion.js';
import { carrito } from './carrito.js';
import { api, post, urlMedia, esImagenReal } from './api.js';
import { montarIconos, icono, ilustracion } from './iconos.js';

// ---------- Formato ----------

/** Escapa HTML. TODO dato que viene de la API pasa por aquí antes de ir a innerHTML (evita XSS). */
export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const formatoCop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
export const cop = (n) => formatoCop.format(Number(n || 0));
export const numero = (n) => new Intl.NumberFormat('es-CO').format(Number(n || 0));
export const copCorto = (n) => {
  const v = Number(n || 0);
  if (v >= 1e6) return `$${(v / 1e6).toLocaleString('es-CO', { maximumFractionDigits: 1 })} M`;
  if (v >= 1e3) return `$${Math.round(v / 1e3).toLocaleString('es-CO')} mil`;
  return cop(v);
};

export function fecha(iso, opciones = { day: 'numeric', month: 'short', year: 'numeric' }) {
  if (!iso) return '—';
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso);
  return d.toLocaleDateString('es-CO', opciones);
}

export function fechaHora(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });
}

export function diasEntre(inicio, fin) {
  return Math.round((new Date(`${fin}T00:00:00`) - new Date(`${inicio}T00:00:00`)) / 864e5) + 1; // inclusivo
}

export function debounce(fn, ms = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

export const $ = (sel, raiz = document) => raiz.querySelector(sel);
export const $$ = (sel, raiz = document) => [...raiz.querySelectorAll(sel)];

// ---------- Imagen o ilustración de una herramienta ----------

export function visual(imagen, categoria, alt = '') {
  return esImagenReal(imagen)
    ? `<img src="${esc(urlMedia(imagen))}" alt="${esc(alt)}" loading="lazy">`
    : ilustracion(categoria);
}

// ---------- Estados ----------

const ESTADOS = {
  PENDIENTE: ['warn', 'Pendiente'],
  APROBADA: ['info', 'Aprobada · por pagar'],
  PAGADA: ['accent', 'Pagada'],
  ENTREGADA: ['info', 'En alquiler'],
  DEVUELTA: ['ok', 'Devuelta'],
  RECHAZADA: ['bad', 'Rechazada'],
  CANCELADA: ['', 'Cancelada'],
  VENCIDA: ['bad', 'Vencida'],
  APROBADO: ['ok', 'Aprobado'],
  RECHAZADO: ['bad', 'Rechazado'],
  ABIERTO: ['bad', 'Abierto'],
  EN_REVISION: ['warn', 'En revisión'],
  RESUELTO: ['ok', 'Resuelto'],
  ACTIVA: ['ok', 'Activa'],
  EN_MANTENIMIENTO: ['warn', 'Mantenimiento'],
  INACTIVA: ['', 'Inactiva'],
  EMITIDA: ['ok', 'Emitida'],
  ANULADA: ['bad', 'Anulada'],
  LEVE: ['info', 'Leve'],
  MODERADO: ['warn', 'Moderado'],
  GRAVE: ['bad', 'Grave'],
  BUENO: ['ok', 'Buen estado'],
  DESGASTE_NORMAL: ['info', 'Desgaste normal'],
  CON_DANOS: ['bad', 'Con daños'],
};

export function badge(estado, texto = null) {
  const [tono, etiqueta] = ESTADOS[estado] || ['', estado];
  return `<span class="badge ${tono}">${esc(texto || etiqueta)}</span>`;
}

export const etiquetaEstado = (estado) => (ESTADOS[estado] || [null, estado])[1];

// ---------- Notificaciones ----------

export function toast(mensaje, tipo = 'ok', titulo = null) {
  let caja = $('.toasts');
  if (!caja) {
    caja = document.createElement('div');
    caja.className = 'toasts';
    caja.setAttribute('role', 'status');
    caja.setAttribute('aria-live', 'polite');
    document.body.append(caja);
  }
  const t = document.createElement('div');
  t.className = `toast ${tipo}`;
  t.innerHTML = `${icono(tipo === 'ok' ? 'check' : tipo === 'bad' ? 'alertCircle' : 'info')}
    <div>${titulo ? `<b>${esc(titulo)}</b>` : ''}${esc(mensaje)}</div>`;
  caja.append(t);
  setTimeout(() => { t.classList.add('saliendo'); setTimeout(() => t.remove(), 300); }, tipo === 'bad' ? 6000 : 3500);
}

/** Muestra un error de la API de forma legible. */
export function toastError(error) {
  toast(error.message, 'bad', error.titulo && error.titulo !== error.message ? error.titulo : null);
}

// ---------- Modales ----------

export function modal({ titulo, cuerpo, pie = '', ancho = false, alCerrar = null }) {
  const fondo = document.createElement('div');
  fondo.className = 'modal-backdrop';
  fondo.innerHTML = `
    <div class="modal ${ancho ? 'wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
      <div class="modal-head"><h3 id="modal-titulo">${titulo}</h3>
        <button class="icon-btn" data-cerrar aria-label="Cerrar">${icono('close')}</button></div>
      <div class="modal-body">${cuerpo}</div>
      ${pie ? `<div class="modal-foot">${pie}</div>` : ''}
    </div>`;
  const anterior = document.activeElement;
  const cerrar = () => {
    fondo.remove();
    document.removeEventListener('keydown', alTeclear);
    anterior?.focus?.();
    alCerrar?.();
  };
  const alTeclear = (e) => { if (e.key === 'Escape') cerrar(); };
  fondo.addEventListener('click', (e) => {
    if (e.target === fondo || e.target.closest('[data-cerrar]')) cerrar();
  });
  document.addEventListener('keydown', alTeclear);
  document.body.append(fondo);
  setTimeout(() => $('input, select, textarea, button:not([data-cerrar])', fondo)?.focus(), 50);
  return { el: fondo, cerrar };
}

export function confirmar(titulo, texto, { aceptar = 'Confirmar', peligro = false } = {}) {
  return new Promise((resolver) => {
    let respondido = false;
    const m = modal({
      titulo: esc(titulo),
      cuerpo: `<p>${texto}</p>`,
      pie: `<button class="btn btn-ghost" data-cerrar>Cancelar</button>
            <button class="btn ${peligro ? 'btn-danger' : 'btn-primary'}" data-ok>${esc(aceptar)}</button>`,
      alCerrar: () => { if (!respondido) resolver(false); },
    });
    $('[data-ok]', m.el).addEventListener('click', () => { respondido = true; m.cerrar(); resolver(true); });
  });
}

/**
 * Modal con un formulario. `enviar(datos, form)` debe devolver una promesa; si falla, el error se muestra
 * dentro del modal (con los errores por campo del 400) y el modal sigue abierto.
 */
export function formularioModal({ titulo, campos, textoBoton = 'Guardar', ancho = false, enviar }) {
  const m = modal({
    titulo: esc(titulo), ancho,
    cuerpo: `<form id="form-modal" class="stack" novalidate>${campos}<div class="notice bad hidden" data-error></div></form>`,
    pie: `<button class="btn btn-ghost" data-cerrar>Cancelar</button>
          <button class="btn btn-primary" type="submit" form="form-modal">${esc(textoBoton)}</button>`,
  });
  const form = $('#form-modal', m.el);
  const boton = $('button[type="submit"]', m.el);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const caja = $('[data-error]', form);
    caja.classList.add('hidden');
    $$('[aria-invalid]', form).forEach((c) => c.removeAttribute('aria-invalid'));
    cargando(boton, true);
    try {
      await enviar(Object.fromEntries(new FormData(form)), form);
      m.cerrar();
    } catch (error) {
      mostrarErrorEnFormulario(form, caja, error);
    } finally {
      cargando(boton, false);
    }
  });
  return m;
}

// Algunos errores de la API vienen de una regla que combina campos (ej. el documento según su tipo):
// se muestran debajo del campo que el usuario debe corregir.
const CAMPO_DEL_ERROR = { numeroDocumentoValido: 'numeroDocumento' };

/** Muestra el error de la API: el mensaje de cada campo debajo de su casilla y un resumen arriba del botón. */
export function mostrarErrorEnFormulario(form, caja, error) {
  $$('.field-error', form).forEach((e) => e.remove());
  if (error.errores) {
    const sinCampo = [];
    Object.entries(error.errores).forEach(([clave, mensaje]) => {
      const campo = form.elements[CAMPO_DEL_ERROR[clave] || clave];
      if (campo?.setAttribute) {
        campo.setAttribute('aria-invalid', 'true');
        const aviso = Object.assign(document.createElement('small'), { className: 'field-error', textContent: mensaje });
        (campo.closest('.field') || campo.parentElement).append(aviso);
      } else {
        sinCampo.push(mensaje);
      }
    });
    caja.innerHTML = `${icono('alertCircle')}<div><b>Revisa los campos marcados en rojo.</b>${sinCampo.map((m) => `<br>${esc(m)}`).join('')}</div>`;
  } else {
    caja.innerHTML = `${icono('alertCircle')}<div>${esc(error.message)}</div>`;
  }
  caja.classList.remove('hidden');
}

// ---------- Filtros de escritura ----------
// Impiden teclear caracteres que no corresponden (ej. números en un nombre). La API valida lo mismo:
// el frontend ayuda al usuario, pero la regla de verdad está en el backend.
const FILTROS = {
  letras: (v) => v.replace(/[^\p{L} .'-]/gu, '').slice(0, 80),
  numeros: (v) => v.replace(/\D/g, ''),
  telefono: (v) => v.replace(/\D/g, '').slice(0, 10),
  nit: (v) => v.replace(/[^\d-]/g, '').slice(0, 12),
  categoria: (v) => v.replace(/[^\p{L} ,-]/gu, '').slice(0, 80),
  empresa: (v) => v.replace(/[^\p{L}\d .,&'-]/gu, '').slice(0, 150),
};

function filtroDocumento(form) {
  const tipo = form?.elements?.tipoDocumento?.value;
  if (tipo === 'PASAPORTE') return (v) => v.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 12);
  if (tipo === 'NIT') return FILTROS.nit;
  return (v) => v.replace(/\D/g, '').slice(0, 10); // CC y CE: solo números
}

function aplicarFiltro(campo) {
  const tipo = campo.dataset?.solo;
  if (!tipo) return;
  const filtro = tipo === 'documento' ? filtroDocumento(campo.form) : FILTROS[tipo];
  if (!filtro) return;
  const limpio = filtro(campo.value);
  if (limpio !== campo.value) {
    campo.value = limpio;
    campo.classList.add('rechazo');
    setTimeout(() => campo.classList.remove('rechazo'), 400);
  }
  campo.removeAttribute('aria-invalid');
  campo.closest('.field')?.querySelector('.field-error')?.remove();
}

let filtrosActivos = false;
/** Se activa una sola vez por página y funciona también en los modales (delegación de eventos). */
export function activarFiltros() {
  if (filtrosActivos) return;
  filtrosActivos = true;
  document.addEventListener('input', (e) => aplicarFiltro(e.target));
  document.addEventListener('change', (e) => {
    if (e.target.name === 'tipoDocumento') {
      const doc = e.target.form?.elements?.numeroDocumento;
      if (doc) aplicarFiltro(doc);
    }
  });
}

export function cargando(boton, activo, texto = null) {
  if (!boton) return;
  if (activo) {
    boton.dataset.texto = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = `<span class="spinner"></span>${texto ? ` ${esc(texto)}` : ''}`;
  } else if (boton.dataset.texto) {
    boton.disabled = false;
    boton.innerHTML = boton.dataset.texto;
  }
}

/** Descarga un archivo protegido (el navegador no manda el JWT en un enlace normal). */
export async function descargar(ruta, nombre) {
  const blob = await api(ruta, { blob: true });
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement('a'), { href: url, download: nombre });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function paginador(contenedor, pagina, totalPaginas, alCambiar) {
  if (totalPaginas <= 1) { contenedor.innerHTML = ''; return; }
  const botones = [];
  const desde = Math.max(0, Math.min(pagina - 2, totalPaginas - 5));
  for (let i = desde; i < Math.min(totalPaginas, desde + 5); i++) botones.push(i);
  contenedor.innerHTML = `
    <button data-p="${pagina - 1}" ${pagina === 0 ? 'disabled' : ''} aria-label="Anterior">‹</button>
    ${botones.map((i) => `<button data-p="${i}" ${i === pagina ? 'aria-current="page"' : ''}>${i + 1}</button>`).join('')}
    <button data-p="${pagina + 1}" ${pagina >= totalPaginas - 1 ? 'disabled' : ''} aria-label="Siguiente">›</button>`;
  contenedor.onclick = (e) => {
    const b = e.target.closest('button[data-p]');
    if (b && !b.disabled) alCambiar(Number(b.dataset.p));
  };
}

export function vacio(titulo, texto = '', ilustracionCategoria = 'herramienta', accion = '') {
  return `<div class="empty">${ilustracion(ilustracionCategoria)}<h3>${esc(titulo)}</h3><p>${esc(texto)}</p>${accion}</div>`;
}

// ---------- Tema claro / oscuro ----------

function aplicarTema(tema) {
  document.documentElement.dataset.theme = tema;
  try { localStorage.setItem('obraya.tema', tema); } catch { /* nada */ }
  const b = $('[data-tema]');
  if (b) b.innerHTML = icono(tema === 'dark' ? 'sun' : 'moon');
}

function temaInicial() {
  try {
    const guardado = localStorage.getItem('obraya.tema');
    if (guardado) return guardado;
  } catch { /* nada */ }
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}
document.documentElement.dataset.theme = temaInicial();

// ---------- Layout común ----------

export function montarLayout({ activa = '', sinPie = false } = {}) {
  montarIconos();
  activarFiltros();
  const rol = sesion.rol();
  const encabezado = document.createElement('div');
  encabezado.innerHTML = `
    <div class="topbar"><div class="wrap">
      <span>Entrega a domicilio o recogida en sede · <b>Pago seguro en línea</b></span>
      <span>Factura electrónica automática en PDF</span>
    </div></div>
    <header class="site-header"><div class="wrap">
      <a class="logo" href="index.html" aria-label="${MARCA}, inicio">
        <span class="logo-mark"><svg viewBox="0 0 140 100"><use href="#h-taladro"/></svg></span><span>Obra<em>Ya</em></span></a>
      <nav class="nav-links" aria-label="Principal">
        <a href="index.html#catalogo" ${activa === 'catalogo' ? 'aria-current="page"' : ''}>Catálogo</a>
        <a href="index.html#como-funciona">Cómo funciona</a>
        ${rol ? `<a href="panel.html" ${activa === 'panel' ? 'aria-current="page"' : ''}>Mi panel</a>` : ''}
      </nav>
      <div class="header-actions">
        <button class="icon-btn" data-tema aria-label="Cambiar tema claro u oscuro"></button>
        ${rol !== 'ADMIN' && rol !== 'PROVEEDOR' ? `
        <button class="icon-btn" data-abrir-carrito aria-label="Mi reserva" style="position:relative">
          ${icono('cart')}<span class="cart-count" data-cart-count></span></button>` : ''}
        ${rol ? `
          <a class="user-chip" href="panel.html" title="${esc(sesion.nombre())}">
            <span class="avatar">${esc(sesion.iniciales())}</span><span>${esc(sesion.usuario().nombres)}</span></a>
          <button class="icon-btn" data-salir aria-label="Cerrar sesión" title="Cerrar sesión">${icono('logout')}</button>`
          : `<a class="btn btn-dark" href="ingresar.html">${icono('user')}Ingresar</a>`}
      </div>
    </div></header>`;
  document.body.prepend(...encabezado.children);

  if (!sinPie) {
    const pie = document.createElement('footer');
    pie.className = 'site-footer';
    pie.innerHTML = `
      <div class="wrap">
        <div><a class="logo" href="index.html"><span class="logo-mark"><svg viewBox="0 0 140 100"><use href="#h-taladro"/></svg></span><span>Obra<em>Ya</em></span></a>
          <p style="margin-top:.8rem;max-width:22rem">Alquiler de herramientas y equipos de construcción. Proveedores verificados, disponibilidad real y factura al instante.</p></div>
        <div><h4>Plataforma</h4><ul><li><a href="index.html#catalogo">Catálogo</a></li><li><a href="index.html#como-funciona">Cómo funciona</a></li><li><a href="reserva.html">Mi reserva</a></li></ul></div>
        <div><h4>Tu cuenta</h4><ul><li><a href="ingresar.html">Ingresar</a></li><li><a href="ingresar.html#registro">Crear cuenta</a></li><li><a href="panel.html">Mi panel</a></li></ul></div>
        <div><h4>Contacto</h4><ul><li>Bucaramanga, Colombia</li><li>soporte@obraya.co</li><li>Lun–Sáb 7:00–18:00</li></ul></div>
      </div>
      <div class="wrap footer-bottom">© ${new Date().getFullYear()} ${MARCA} · Proyecto académico Campuslands · Spring Boot + JavaScript puro</div>`;
    document.body.append(pie);
  }

  aplicarTema(document.documentElement.dataset.theme);
  $('[data-tema]').addEventListener('click', () => aplicarTema(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'));
  $('[data-salir]')?.addEventListener('click', () => { sesion.cerrar(); location.href = 'index.html'; });

  if ($('[data-abrir-carrito]')) montarCarrito();
}

// ---------- Carrito lateral con cotización en vivo ----------

function montarCarrito() {
  const contenedor = document.createElement('div');
  contenedor.innerHTML = `
    <div class="scrim" data-scrim></div>
    <aside class="drawer" aria-label="Mi reserva" aria-hidden="true">
      <div class="drawer-head"><h3>Mi reserva</h3><button class="icon-btn" data-cerrar-carrito aria-label="Cerrar">${icono('close')}</button></div>
      <div class="drawer-body" data-items></div>
      <div class="drawer-foot" data-pie></div>
    </aside>`;
  document.body.append(...contenedor.children);
  const drawer = $('.drawer');
  const scrim = $('[data-scrim]');

  const abrir = () => { drawer.classList.add('on'); scrim.classList.add('on'); drawer.setAttribute('aria-hidden', 'false'); pintar(); };
  const cerrar = () => { drawer.classList.remove('on'); scrim.classList.remove('on'); drawer.setAttribute('aria-hidden', 'true'); };
  $('[data-abrir-carrito]').addEventListener('click', abrir);
  $('[data-cerrar-carrito]').addEventListener('click', cerrar);
  scrim.addEventListener('click', cerrar);
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrar(); });
  window.abrirCarrito = abrir;

  const contador = () => {
    const n = carrito.cantidad();
    const el = $('[data-cart-count]');
    el.textContent = n || '';
    el.dataset.n = n;
  };

  let ultimaCotizacion = 0;
  const cotizar = debounce(async () => {
    const pie = $('[data-pie]');
    const c = carrito.obtener();
    if (!c.items.length) return;
    const id = ++ultimaCotizacion;
    try {
      const q = await post('/reservas/cotizacion', carrito.pedido());
      if (id !== ultimaCotizacion) return; // llegó una respuesta vieja: se ignora
      const faltantes = q.lineas.filter((l) => l.cantidad > l.unidadesDisponibles);
      $('[data-cotizacion]', pie).innerHTML = `
        ${faltantes.map((l) => `<div class="notice warn">${icono('alert')}<div>De <b>${esc(l.nombre)}</b> hay ${l.unidadesDisponibles} disponible(s) en esas fechas.</div></div>`).join('')}
        <div class="quote-line"><span>${q.dias} ${q.dias === 1 ? 'día' : 'días'} · subtotal</span><span class="num">${cop(q.subtotal)}</span></div>
        <div class="quote-line"><span>IVA (19 %)</span><span class="num">${cop(q.iva)}</span></div>
        <div class="quote-line total"><span>Total</span><span class="num">${cop(q.total)}</span></div>`;
    } catch (error) {
      if (id === ultimaCotizacion) $('[data-cotizacion]', pie).innerHTML = `<div class="notice bad">${icono('alertCircle')}<div>${esc(error.message)}</div></div>`;
    }
  }, 300);

  function pintar() {
    contador();
    const c = carrito.obtener();
    const items = $('[data-items]');
    const pie = $('[data-pie]');
    if (!c.items.length) {
      items.innerHTML = vacio('Tu reserva está vacía', 'Agrega herramientas desde el catálogo con el botón +.', 'carretilla',
        '<a class="btn btn-primary" href="index.html#catalogo" style="margin-top:1rem">Ver catálogo</a>');
      pie.innerHTML = '';
      return;
    }
    items.innerHTML = `<p class="small muted">Proveedor: <b>${esc(c.items[0].proveedor)}</b> · ${esc(c.items[0].ciudad)}</p>` +
      c.items.map((i) => `
      <div class="line-item">
        <div class="mini">${visual(i.imagen, i.categoria, i.nombre)}</div>
        <div><b>${esc(i.nombre)}</b><small>${cop(i.precioPorDia)} / día</small></div>
        <div class="stack" style="gap:.3rem;align-items:end">
          <div class="qty"><button data-menos="${i.herramientaId}" aria-label="Quitar una">−</button><output>${i.cantidad}</output><button data-mas="${i.herramientaId}" aria-label="Agregar una">+</button></div>
          <button class="btn-text small" data-quitar="${i.herramientaId}">Quitar</button>
        </div>
      </div>`).join('');
    pie.innerHTML = `
      <div class="dates">
        <label class="field"><span>Desde</span><input class="input" type="date" data-desde value="${c.fechaInicio}" min="${c.fechaInicio < hoy() ? c.fechaInicio : hoy()}"></label>
        <label class="field"><span>Hasta</span><input class="input" type="date" data-hasta value="${c.fechaFin}" min="${c.fechaInicio}"></label>
      </div>
      <div data-cotizacion><div class="quote-line"><span>Calculando…</span><span class="spinner"></span></div></div>
      <a class="btn btn-primary btn-lg btn-block" href="reserva.html">Continuar con la reserva ${icono('arrow')}</a>
      <p class="small muted" style="text-align:center">El proveedor aprueba tu solicitud y luego tienes 24 h para pagar.</p>`;
    cotizar();
  }

  $('[data-items]').addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    if (b.dataset.mas) carrito.cambiarCantidad(Number(b.dataset.mas), 1);
    if (b.dataset.menos) carrito.cambiarCantidad(Number(b.dataset.menos), -1);
    if (b.dataset.quitar) carrito.quitar(Number(b.dataset.quitar));
  });
  $('[data-pie]').addEventListener('change', (e) => {
    if (e.target.matches('[data-desde], [data-hasta]')) {
      carrito.fijarFechas($('[data-desde]').value, $('[data-hasta]').value);
    }
  });
  window.addEventListener('carrito', () => { if (drawer.classList.contains('on')) pintar(); else contador(); });
  window.addEventListener('storage', (e) => { if (e.key === 'obraya.carrito') contador(); });
  contador();
}

export const hoy = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

/** Agrega al carrito; si hay herramientas de otro proveedor, pregunta antes de reemplazarlas. */
export async function agregarAlCarrito(herramienta, cantidad = 1) {
  if (sesion.rol() === 'PROVEEDOR' || sesion.rol() === 'ADMIN') {
    toast('Las reservas se hacen con una cuenta de cliente.', 'info');
    return false;
  }
  let resultado = carrito.agregar(herramienta, cantidad);
  if (resultado === 'otro-proveedor') {
    const actual = carrito.obtener().items[0].proveedor;
    const ok = await confirmar('¿Empezar una reserva nueva?',
      `Cada reserva es de un solo proveedor. Tu reserva actual es con <b>${esc(actual)}</b> y esta herramienta es de <b>${esc(herramienta.proveedor.razonSocial)}</b>.`,
      { aceptar: 'Reemplazar reserva' });
    if (!ok) return false;
    resultado = carrito.agregar(herramienta, cantidad, { reemplazar: true });
  }
  toast(`${herramienta.nombre} agregada a tu reserva`, 'ok');
  return true;
}
