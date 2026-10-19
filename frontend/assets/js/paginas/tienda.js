// Página principal: catálogo público con filtros, categorías y disponibilidad por fechas.

import { get, query } from '../api.js';
import { TARJETAS_PRUEBA } from '../config.js';
import { carrito } from '../carrito.js';
import { ilustracion } from '../iconos.js';
import {
  montarLayout, $, $$, esc, cop, visual, debounce, paginador, vacio, agregarAlCarrito, toastError, hoy,
} from '../ui.js';

montarLayout({ activa: 'catalogo' });

const TAMANIO = 12;
const url = new URLSearchParams(location.search);
const filtros = {
  texto: url.get('texto') || '',
  categoriaId: url.get('categoriaId') || '',
  ciudad: url.get('ciudad') || '',
  precioMaximo: url.get('precioMaximo') || '',
  desde: url.get('desde') || '',
  hasta: url.get('hasta') || '',
  pagina: Number(url.get('pagina') || 0),
};
let categorias = [];

// ---------- Datos de apoyo (categorías, ciudades, cifras del hero) ----------

async function cargarApoyo() {
  const [cats, todas] = await Promise.all([get('/categorias'), get('/herramientas?tamanio=100')]);
  categorias = cats.filter((c) => c.activa !== false);
  const ciudades = [...new Set(todas.contenido.map((h) => h.proveedor.ciudad))].sort((a, b) => a.localeCompare(b, 'es'));

  const opcionesCat = categorias.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('');
  const opcionesCiudad = ciudades.map((c) => `<option>${esc(c)}</option>`).join('');
  $$('select[name="categoriaId"], select[data-f="categoriaId"]').forEach((s) => s.insertAdjacentHTML('beforeend', opcionesCat));
  $$('select[name="ciudad"], select[data-f="ciudad"]').forEach((s) => s.insertAdjacentHTML('beforeend', opcionesCiudad));

  const [b1, b2, b3] = $$('[data-hero-stats] b');
  b1.textContent = todas.totalElementos;
  b2.textContent = categorias.length;
  b3.textContent = ciudades.length;

  // Destacadas: una herramienta de cada una de las 4 primeras categorías con más oferta
  const destacadas = [];
  const vistas = new Set();
  for (const h of todas.contenido) {
    if (!vistas.has(h.categoria.id)) { vistas.add(h.categoria.id); destacadas.push(h); }
    if (destacadas.length === 4) break;
  }
  $('[data-hero-art]').innerHTML = destacadas.map((h) => `
    <a class="hero-tile" href="herramienta.html?id=${h.id}">
      ${ilustracion(h.categoria.nombre)}
      <small>${esc(h.categoria.nombre)}</small><strong>desde ${cop(h.precioPorDia)}/día</strong>
    </a>`).join('');

  $('[data-categorias]').innerHTML = categorias.map((c) => `
    <button class="cat-pill" data-cat="${c.id}" aria-pressed="${String(c.id) === filtros.categoriaId}">
      ${ilustracion(c.nombre)}${esc(c.nombre)}</button>`).join('');

  $('[data-tarjetas]').innerHTML = TARJETAS_PRUEBA.map((t) => `
    <div class="test-card"><span class="mono">${t.numero}</span><span class="badge ${t.tipo}">${t.resultado}</span></div>`).join('');
}

// ---------- Filtros ----------

function sincronizarControles() {
  $$('[data-f]').forEach((el) => { el.value = filtros[el.dataset.f] || (el.type === 'range' ? 500000 : ''); });
  const f = $('[data-buscador]');
  ['texto', 'categoriaId', 'ciudad', 'desde', 'hasta'].forEach((k) => { f.elements[k].value = filtros[k]; });
  $('[data-precio-valor]').textContent = filtros.precioMaximo ? cop(filtros.precioMaximo) : 'Sin límite';
  $$('[data-cat]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.cat === String(filtros.categoriaId))));
  ['[data-f="desde"]', '[name="desde"]', '[data-f="hasta"]', '[name="hasta"]'].forEach((s) => { $(s).min = hoy(); });

  const nombreCat = categorias.find((c) => String(c.id) === String(filtros.categoriaId))?.nombre;
  const chips = [
    filtros.texto && ['texto', `“${filtros.texto}”`],
    nombreCat && ['categoriaId', nombreCat],
    filtros.ciudad && ['ciudad', filtros.ciudad],
    filtros.precioMaximo && ['precioMaximo', `≤ ${cop(filtros.precioMaximo)}`],
    filtros.desde && filtros.hasta && ['fechas', `${filtros.desde} → ${filtros.hasta}`],
  ].filter(Boolean);
  $('[data-chips]').innerHTML = chips.map(([k, t]) => `<button class="chip" data-quitar="${k}">${esc(t)} <span class="x">✕</span></button>`).join('');
}

function aplicar(cambios, { recargar = true } = {}) {
  Object.assign(filtros, cambios, { pagina: cambios.pagina ?? 0 });
  if (filtros.desde && filtros.hasta && filtros.hasta < filtros.desde) filtros.hasta = filtros.desde;
  if (filtros.desde && filtros.hasta) carrito.fijarFechas(filtros.desde, filtros.hasta);
  const q = query({ ...filtros, pagina: filtros.pagina || '' });
  history.replaceState(null, '', `${location.pathname}${q}${location.hash}`);
  sincronizarControles();
  if (recargar) cargarCatalogo();
}

// ---------- Catálogo ----------

function esqueletos() {
  return Array.from({ length: 8 }, () => `
    <div class="card is-loading"><div class="thumb skeleton"></div>
    <div class="card-body"><div class="skeleton" style="height:12px;width:40%"></div><div class="skeleton" style="height:22px;width:85%"></div><div class="skeleton" style="height:12px;width:60%"></div></div></div>`).join('');
}

function tarjeta(h) {
  return `
    <article class="card">
      <a class="card-link" href="herramienta.html?id=${h.id}">
        <div class="thumb">
          <span class="badge ${h.unidadesOperativas <= 2 ? 'warn' : 'ok'}" data-disp="${h.id}">${h.unidadesOperativas <= 2 ? `Últimas ${h.unidadesOperativas}` : 'Disponible'}</span>
          ${visual(h.imagenPrincipal, h.categoria.nombre, h.nombre)}
        </div>
        <div class="card-body">
          <span class="card-cat">${esc(h.categoria.nombre)}</span>
          <h3>${esc(h.nombre)}</h3>
          <div class="card-meta"><span>${esc(h.proveedor.ciudad)}</span><span>${esc(h.marca || '')}</span></div>
          <div class="card-meta"><span>${esc(h.proveedor.razonSocial)}</span></div>
        </div>
      </a>
      <div class="card-body" style="padding-top:0;flex:0">
        <div class="card-foot">
          <div class="price"><b>${cop(h.precioPorDia)}</b> <small>/ día</small></div>
          <button class="add-btn" data-agregar="${h.id}" aria-label="Agregar ${esc(h.nombre)} a mi reserva"><svg><use href="#i-plus"/></svg></button>
        </div>
      </div>
    </article>`;
}

let herramientasPagina = [];
let peticion = 0;

async function cargarCatalogo() {
  const id = ++peticion;
  const grid = $('[data-grid]');
  grid.innerHTML = esqueletos();
  try {
    const pagina = await get(`/herramientas${query({
      texto: filtros.texto, categoriaId: filtros.categoriaId, ciudad: filtros.ciudad,
      precioMaximo: filtros.precioMaximo, pagina: filtros.pagina, tamanio: TAMANIO,
    })}`);
    if (id !== peticion) return;
    herramientasPagina = pagina.contenido;
    $('[data-resultado]').textContent = `${pagina.totalElementos} herramienta${pagina.totalElementos === 1 ? '' : 's'} disponible${pagina.totalElementos === 1 ? '' : 's'} para alquilar`;
    grid.innerHTML = pagina.contenido.length
      ? pagina.contenido.map(tarjeta).join('')
      : vacio('No encontramos herramientas', 'Prueba con otra categoría, otra ciudad o un precio máximo mayor.', 'herramienta',
        '<button class="btn btn-ghost" data-limpiar style="margin-top:1rem">Limpiar filtros</button>');
    paginador($('[data-pager]'), pagina.pagina, pagina.totalPaginas, (p) => {
      aplicar({ pagina: p });
      $('#catalogo').scrollIntoView({ behavior: 'smooth' });
    });
    if (filtros.desde && filtros.hasta) marcarDisponibilidad(id);
  } catch (error) {
    grid.innerHTML = vacio('No se pudo cargar el catálogo', error.message);
    $('[data-resultado]').textContent = '';
  }
}

/** Con fechas elegidas, consulta la disponibilidad real de cada herramienta de la página. */
function marcarDisponibilidad(id) {
  herramientasPagina.forEach(async (h) => {
    try {
      const d = await get(`/herramientas/${h.id}/disponibilidad${query({ fechaInicio: filtros.desde, fechaFin: filtros.hasta })}`);
      if (id !== peticion) return;
      const b = $(`[data-disp="${h.id}"]`);
      if (!b) return;
      const n = d.unidadesDisponibles;
      b.className = `badge ${n === 0 ? 'bad' : n <= 2 ? 'warn' : 'ok'}`;
      b.textContent = n === 0 ? 'Sin cupo en tus fechas' : `${n} libre${n === 1 ? '' : 's'} en tus fechas`;
    } catch { /* la tarjeta conserva su insignia */ }
  });
}

// ---------- Eventos ----------

$('[data-buscador]').addEventListener('submit', (e) => {
  e.preventDefault();
  const f = Object.fromEntries(new FormData(e.target));
  aplicar(f);
  $('#catalogo').scrollIntoView({ behavior: 'smooth' });
});

const porTexto = debounce((v) => aplicar({ texto: v }), 350);
$('.filters').addEventListener('input', (e) => {
  const campo = e.target.dataset.f;
  if (campo === 'texto') porTexto(e.target.value.trim());
  if (campo === 'precioMaximo') {
    const v = Number(e.target.value);
    $('[data-precio-valor]').textContent = v >= 500000 ? 'Sin límite' : cop(v);
  }
});
$('.filters').addEventListener('change', (e) => {
  const campo = e.target.dataset.f;
  if (!campo || campo === 'texto') return;
  if (campo === 'precioMaximo') aplicar({ precioMaximo: Number(e.target.value) >= 500000 ? '' : e.target.value });
  else aplicar({ [campo]: e.target.value });
});

document.addEventListener('click', async (e) => {
  const cat = e.target.closest('[data-cat]');
  if (cat) {
    aplicar({ categoriaId: cat.dataset.cat === String(filtros.categoriaId) ? '' : cat.dataset.cat });
    $('#catalogo').scrollIntoView({ behavior: 'smooth' });
  }
  if (e.target.closest('[data-limpiar]')) aplicar({ texto: '', categoriaId: '', ciudad: '', precioMaximo: '', desde: '', hasta: '' });
  const quitar = e.target.closest('[data-quitar]');
  if (quitar) aplicar(quitar.dataset.quitar === 'fechas' ? { desde: '', hasta: '' } : { [quitar.dataset.quitar]: '' });
  const agregar = e.target.closest('[data-agregar]');
  if (agregar) {
    const h = herramientasPagina.find((x) => x.id === Number(agregar.dataset.agregar));
    if (h && await agregarAlCarrito(h)) window.abrirCarrito?.();
  }
});

// ---------- Inicio ----------

try {
  await cargarApoyo();
} catch (error) {
  toastError(error);
}
aplicar({ pagina: filtros.pagina });
