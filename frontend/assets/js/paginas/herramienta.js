// Detalle de una herramienta: galería, ficha técnica, cotización en vivo y calendario de disponibilidad.

import { get, post, query, urlMedia, esImagenReal } from '../api.js';
import { carrito, fechaIso, hoyMas } from '../carrito.js';
import { icono, ilustracion } from '../iconos.js';
import {
  montarLayout, $, $$, esc, cop, badge, visual, debounce, vacio, agregarAlCarrito, hoy,
} from '../ui.js';

montarLayout({ activa: 'catalogo' });

const id = Number(new URLSearchParams(location.search).get('id'));
const contenido = $('[data-contenido]');
const DIAS_CALENDARIO = 35;
let herramienta;
let cantidad = 1;
const estado = { desde: carrito.obtener().fechaInicio, hasta: carrito.obtener().fechaFin };

async function iniciar() {
  try {
    herramienta = await get(`/herramientas/${id}`);
  } catch (error) {
    contenido.innerHTML = `<div style="margin-top:3rem">${vacio('Herramienta no encontrada', error.message, 'herramienta',
      '<a class="btn btn-primary" href="index.html#catalogo" style="margin-top:1rem">Volver al catálogo</a>')}</div>`;
    return;
  }
  const h = herramienta;
  document.title = `${h.nombre} · ObraYa`;
  const imagenes = h.imagenes.filter((i) => esImagenReal(i.url));
  const disponibleParaAlquilar = h.estado === 'ACTIVA' && h.unidadesOperativas > 0;

  contenido.innerHTML = `
    <nav class="breadcrumb" aria-label="Ruta"><a href="index.html">Inicio</a><span>/</span>
      <a href="index.html?categoriaId=${h.categoria.id}#catalogo">${esc(h.categoria.nombre)}</a><span>/</span><span>${esc(h.nombre)}</span></nav>
    <div class="product">
      <div>
        <div class="gallery-main thumb" data-principal>${imagenes.length
          ? `<img src="${esc(urlMedia(imagenes.find((i) => i.principal)?.url || imagenes[0].url))}" alt="${esc(h.nombre)}">`
          : ilustracion(h.categoria.nombre)}</div>
        ${imagenes.length > 1 ? `<div class="gallery-thumbs" role="group" aria-label="Fotos">${imagenes.map((img, i) => `
          <button data-img="${esc(urlMedia(img.url))}" aria-pressed="${i === 0}" aria-label="Foto ${i + 1}"><img src="${esc(urlMedia(img.url))}" alt=""></button>`).join('')}</div>` : ''}

        <section class="panel-box" style="margin-top:1.5rem">
          <div class="row between" style="margin-bottom:.9rem">
            <h3>Disponibilidad</h3>
            <div class="legend"><span><i style="background:var(--ok)"></i>Libre</span><span><i style="background:var(--warn)"></i>Pocas unidades</span><span><i style="background:var(--bad)"></i>Sin cupo</span></div>
          </div>
          <p class="small muted" style="margin-bottom:.8rem">Próximas 5 semanas. Toca un día de inicio y uno de fin para elegir tus fechas.</p>
          <div class="calendar" data-calendario aria-label="Calendario de disponibilidad"></div>
        </section>

        <section style="margin-top:1.5rem">
          <h3 style="margin-bottom:.6rem">Descripción</h3>
          <p class="muted">${esc(h.descripcion || 'Sin descripción.')}</p>
        </section>
      </div>

      <aside class="buy-box">
        <div class="row"><span class="eyebrow">${esc(h.categoria.nombre)}</span>${h.estado !== 'ACTIVA' ? badge(h.estado) : ''}</div>
        <h1>${esc(h.nombre)}</h1>
        <p class="muted">${icono('pin', 'ico')} ${esc(h.proveedor.razonSocial)} · ${esc(h.proveedor.ciudad)}</p>
        <div class="price-big">${cop(h.precioPorDia)} <small>por día · IVA aparte</small></div>
        <dl class="specs">
          <div><dt>Marca</dt><dd>${esc(h.marca || '—')}</dd></div>
          <div><dt>Modelo</dt><dd>${esc(h.modelo || '—')}</dd></div>
          <div><dt>Unidades operativas</dt><dd>${h.unidadesOperativas} de ${h.stockTotal}</dd></div>
          <div><dt>Estado</dt><dd>${h.unidadesFueraServicio ? `${h.unidadesFueraServicio} en reparación` : 'Todas operativas'}</dd></div>
        </dl>

        <div class="panel-box stack" style="gap:.9rem">
          <div class="dates">
            <label class="field"><span>Desde</span><input class="input" type="date" data-desde min="${hoy()}" value="${estado.desde}"></label>
            <label class="field"><span>Hasta</span><input class="input" type="date" data-hasta min="${estado.desde}" value="${estado.hasta}"></label>
          </div>
          <div class="row between">
            <span class="label">Cantidad</span>
            <div class="qty"><button data-menos aria-label="Menos">−</button><output data-cantidad>1</output><button data-mas aria-label="Más">+</button></div>
          </div>
          <div data-cotizacion></div>
          ${disponibleParaAlquilar ? `
          <button class="btn btn-primary btn-lg btn-block" data-agregar>${icono('cart')}Agregar a mi reserva</button>
          <button class="btn btn-dark btn-block" data-reservar>Reservar ahora ${icono('arrow')}</button>`
          : '<div class="notice warn">Esta herramienta no está disponible para alquiler en este momento.</div>'}
        </div>
        <div class="row small muted" style="gap:1.2rem">
          <span class="row" style="gap:.35rem">${icono('shield', 'ico')}Proveedor verificado</span>
          <span class="row" style="gap:.35rem">${icono('file', 'ico')}Factura en PDF</span>
        </div>
      </aside>
    </div>
    <section class="section">
      <div class="section-head"><h2>También en ${esc(h.categoria.nombre)}</h2></div>
      <div class="grid" data-relacionadas></div>
    </section>`;

  conectarEventos();
  cotizar();
  pintarCalendario();
  cargarRelacionadas();
}

// ---------- Cotización en vivo ----------

const cotizar = debounce(async () => {
  const caja = $('[data-cotizacion]');
  if (!caja) return;
  caja.innerHTML = '<div class="quote-line"><span>Calculando…</span><span class="spinner"></span></div>';
  try {
    const q = await post('/reservas/cotizacion', {
      fechaInicio: estado.desde, fechaFin: estado.hasta, herramientas: [{ herramientaId: id, cantidad }],
    });
    const libres = q.lineas[0].unidadesDisponibles;
    caja.innerHTML = `
      <div class="notice ${libres >= cantidad ? 'ok' : 'bad'}">${icono(libres >= cantidad ? 'check' : 'alert')}
        <div>${libres >= cantidad ? `${libres} unidad${libres === 1 ? '' : 'es'} libre${libres === 1 ? '' : 's'} en esas fechas`
          : `Solo hay ${libres} unidad${libres === 1 ? '' : 'es'} libre${libres === 1 ? '' : 's'} en esas fechas`}</div></div>
      <div class="quote-line"><span>${cop(herramienta.precioPorDia)} × ${cantidad} × ${q.dias} ${q.dias === 1 ? 'día' : 'días'}</span><span class="num">${cop(q.subtotal)}</span></div>
      <div class="quote-line"><span>IVA (19 %)</span><span class="num">${cop(q.iva)}</span></div>
      <div class="quote-line total"><span>Total</span><span class="num">${cop(q.total)}</span></div>`;
  } catch (error) {
    caja.innerHTML = `<div class="notice bad">${icono('alertCircle')}<div>${esc(error.message)}</div></div>`;
  }
}, 250);

// ---------- Calendario de disponibilidad ----------

let disponibilidadPorDia = {};

async function pintarCalendario() {
  const cal = $('[data-calendario]');
  const inicio = new Date(`${hoy()}T00:00:00`);
  inicio.setDate(inicio.getDate() - ((inicio.getDay() + 6) % 7)); // la semana empieza el lunes
  const dias = Array.from({ length: DIAS_CALENDARIO }, (_, i) => {
    const d = new Date(inicio);
    d.setDate(d.getDate() + i);
    return fechaIso(d);
  });
  const futuros = dias.filter((d) => d >= hoy());

  // Una consulta por día (en grupos de 7 para no saturar el servidor)
  for (let i = 0; i < futuros.length; i += 7) {
    await Promise.all(futuros.slice(i, i + 7).map(async (d) => {
      try {
        const r = await get(`/herramientas/${id}/disponibilidad${query({ fechaInicio: d, fechaFin: d })}`);
        disponibilidadPorDia[d] = r.unidadesDisponibles;
      } catch { disponibilidadPorDia[d] = null; }
    }));
    dibujar(cal, dias);
  }
}

function dibujar(cal, dias) {
  const operativas = herramienta.unidadesOperativas;
  cal.innerHTML = ['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((d) => `<span class="dow">${d}</span>`).join('') +
    dias.map((d) => {
      const fecha = new Date(`${d}T00:00:00`);
      const n = disponibilidadPorDia[d];
      let clase = 'pasado';
      if (d >= hoy()) clase = n === undefined ? '' : n === null ? '' : n === 0 ? 'lleno' : n <= Math.max(1, Math.floor(operativas / 3)) ? 'poco' : 'libre';
      if (d === estado.desde || d === estado.hasta) clase += ' sel';
      else if (d > estado.desde && d < estado.hasta) clase += ' rango';
      const titulo = n === undefined || d < hoy() ? '' : `${n} disponible(s)`;
      return `<button class="day ${clase}" data-dia="${d}" ${d < hoy() || n === 0 ? 'disabled' : ''} title="${titulo}">
        ${fecha.getDate()}${fecha.getDate() === 1 || d === dias[0] ? `<small>${fecha.toLocaleDateString('es-CO', { month: 'short' })}</small>`
          : n !== undefined && d >= hoy() ? `<small>${n ?? ''}</small>` : ''}</button>`;
    }).join('');
}

let eligiendoFin = false;

function elegirDia(d) {
  if (!eligiendoFin || d < estado.desde) {
    estado.desde = d;
    estado.hasta = d;
    eligiendoFin = true;
  } else {
    estado.hasta = d;
    eligiendoFin = false;
  }
  sincronizarFechas();
}

function sincronizarFechas() {
  $('[data-desde]').value = estado.desde;
  $('[data-hasta]').value = estado.hasta;
  $('[data-hasta]').min = estado.desde;
  carrito.fijarFechas(estado.desde, estado.hasta);
  const cal = $('[data-calendario]');
  const dias = $$('[data-dia]', cal).map((b) => b.dataset.dia);
  if (dias.length) dibujar(cal, dias);
  cotizar();
}

// ---------- Eventos ----------

function conectarEventos() {
  $('[data-desde]').addEventListener('change', (e) => {
    estado.desde = e.target.value || hoyMas(1);
    if (estado.hasta < estado.desde) estado.hasta = estado.desde;
    sincronizarFechas();
  });
  $('[data-hasta]').addEventListener('change', (e) => {
    estado.hasta = e.target.value < estado.desde ? estado.desde : e.target.value;
    sincronizarFechas();
  });
  $('[data-mas]').addEventListener('click', () => {
    cantidad = Math.min(cantidad + 1, herramienta.unidadesOperativas || 1);
    $('[data-cantidad]').textContent = cantidad;
    cotizar();
  });
  $('[data-menos]').addEventListener('click', () => {
    cantidad = Math.max(1, cantidad - 1);
    $('[data-cantidad]').textContent = cantidad;
    cotizar();
  });
  $('[data-calendario]').addEventListener('click', (e) => {
    const b = e.target.closest('[data-dia]');
    if (b && !b.disabled) elegirDia(b.dataset.dia);
  });
  $('[data-agregar]')?.addEventListener('click', async () => {
    if (await agregarAlCarrito(herramienta, cantidad)) window.abrirCarrito?.();
  });
  $('[data-reservar]')?.addEventListener('click', async () => {
    if (await agregarAlCarrito(herramienta, cantidad)) location.href = 'reserva.html';
  });
  $$('[data-img]').forEach((b) => b.addEventListener('click', () => {
    $('[data-principal]').innerHTML = `<img src="${b.dataset.img}" alt="${esc(herramienta.nombre)}">`;
    $$('[data-img]').forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
  }));
}

async function cargarRelacionadas() {
  try {
    const r = await get(`/herramientas${query({ categoriaId: herramienta.categoria.id, tamanio: 5 })}`);
    const otras = r.contenido.filter((h) => h.id !== id).slice(0, 4);
    $('[data-relacionadas]').innerHTML = otras.length ? otras.map((h) => `
      <a class="card card-link" href="herramienta.html?id=${h.id}">
        <div class="thumb">${visual(h.imagenPrincipal, h.categoria.nombre, h.nombre)}</div>
        <div class="card-body"><span class="card-cat">${esc(h.proveedor.ciudad)}</span><h3>${esc(h.nombre)}</h3>
          <div class="card-foot"><div class="price"><b>${cop(h.precioPorDia)}</b> <small>/ día</small></div></div></div>
      </a>`).join('') : '<p class="muted">No hay más herramientas en esta categoría.</p>';
  } catch { $('[data-relacionadas]').innerHTML = ''; }
}

iniciar();
