// Piezas que comparten los tres paneles: lista de reservas con acciones según el rol,
// modales de pago / entrega / devolución, y tablas de pagos y facturas.
// Las acciones que se muestran dependen del rol, pero quien decide si se permiten es el backend.

import { get, post, patch, query } from '../api.js';
import { sesion } from '../sesion.js';
import { TARJETAS_PRUEBA } from '../config.js';
import { icono, ilustracion } from '../iconos.js';
import {
  $, $$, esc, cop, fecha, fechaHora, badge, modal, confirmar, formularioModal, cargando, toast, toastError,
  descargar, paginador, vacio, mostrarErrorEnFormulario,
} from '../ui.js';

export const refrescar = () => window.refrescarSeccion?.();

export function encabezado(titulo, subtitulo = '', acciones = '') {
  return `<div class="page-head"><div><h1>${esc(titulo)}</h1>${subtitulo ? `<p>${subtitulo}</p>` : ''}</div>
    ${acciones ? `<div class="row">${acciones}</div>` : ''}</div>`;
}

export function kpi(etiqueta, valor, nota = '', { icon = 'chart', destacado = false } = {}) {
  return `<div class="kpi ${destacado ? 'accent' : ''}"><span class="label">${icono(icon)}${esc(etiqueta)}</span>
    <b>${valor}</b>${nota ? `<small>${nota}</small>` : ''}</div>`;
}

/** Tiempo restante para pagar una reserva aprobada. */
export function plazo(fechaLimite) {
  const ms = new Date(fechaLimite) - new Date();
  if (ms <= 0) return 'Plazo vencido';
  const h = Math.floor(ms / 36e5);
  const min = Math.floor((ms % 36e5) / 6e4);
  return `Vence en ${h} h ${min} min`;
}

// ---------- Reservas ----------

const FLUJO = ['PENDIENTE', 'APROBADA', 'PAGADA', 'ENTREGADA', 'DEVUELTA'];
const NOMBRES_FLUJO = ['Solicitud', 'Aprobada', 'Pagada', 'Entregada', 'Devuelta'];
const FINALES_MALOS = { RECHAZADA: 1, CANCELADA: 1, VENCIDA: 2 };

function lineaDeTiempo(estado) {
  const fallo = FINALES_MALOS[estado];
  const actual = fallo ?? FLUJO.indexOf(estado);
  return `<div class="timeline">${NOMBRES_FLUJO.map((n, i) => {
    let clase = i < actual ? 'done' : i === actual ? (fallo ? 'fail' : 'done now') : '';
    if (fallo && i === actual) return `<div class="tl-step fail">${badge(estado)}</div>`;
    if (fallo && i > actual) clase = '';
    return `<div class="tl-step ${clase}">${n}</div>`;
  }).join('')}</div>`;
}

function acciones(r, rol, factura) {
  const b = [];
  const esGestor = rol === 'PROVEEDOR' || rol === 'ADMIN';
  if (rol === 'CLIENTE' && r.estado === 'APROBADA') b.push(`<button class="btn btn-primary" data-accion="pagar">${icono('card')}Pagar ${cop(r.total)}</button>`);
  if (esGestor && r.estado === 'PENDIENTE') {
    b.push(`<button class="btn btn-ok" data-accion="aprobar">${icono('check')}Aprobar</button>`);
    b.push(`<button class="btn btn-danger" data-accion="rechazar">Rechazar</button>`);
  }
  if (esGestor && r.estado === 'PAGADA') b.push(`<button class="btn btn-primary" data-accion="entregar">${icono('truck')}Registrar entrega</button>`);
  if (esGestor && r.estado === 'ENTREGADA') b.push(`<button class="btn btn-primary" data-accion="devolver">${icono('undo')}Registrar devolución</button>`);
  if ((rol === 'CLIENTE' || rol === 'ADMIN') && ['PENDIENTE', 'APROBADA'].includes(r.estado)) b.push(`<button class="btn btn-ghost" data-accion="cancelar">Cancelar reserva</button>`);
  if (factura) b.push(`<button class="btn btn-ghost" data-accion="factura" data-factura="${factura.id}" data-numero="${esc(factura.numero)}">${icono('download')}Factura ${esc(factura.numero)}</button>`);
  return b.join('');
}

function tarjetaReserva(r, rol, factura) {
  const contraparte = rol === 'CLIENTE' ? `${esc(r.proveedor.razonSocial)} · ${esc(r.proveedor.ciudad)}` : esc(r.cliente.nombreCompleto);
  const unidades = r.detalles.reduce((s, d) => s + d.cantidad, 0);
  return `
  <article class="res" data-reserva="${r.id}">
    <div class="res-head" data-toggle role="button" tabindex="0" aria-expanded="false">
      <div class="res-icons">${r.detalles.slice(0, 3).map((d) => `<span>${ilustracion(d.herramienta)}</span>`).join('')}</div>
      <div><div class="row" style="gap:.5rem"><b class="mono">${esc(r.codigo)}</b>${badge(r.estado)}
        ${r.estado === 'APROBADA' && r.fechaLimitePago ? `<span class="badge warn">${icono('clock', 'ico')}${plazo(r.fechaLimitePago)}</span>` : ''}</div>
        <div class="cell-sub">${fecha(r.fechaInicio)} → ${fecha(r.fechaFin)} · ${r.dias} ${r.dias === 1 ? 'día' : 'días'} · ${unidades} unidad${unidades === 1 ? '' : 'es'} · ${contraparte}</div></div>
      <div class="res-total num">${cop(r.total)}</div>
      <span class="res-chev">${icono('chevron', 'ico')}</span>
    </div>
    <div class="res-body">
      ${lineaDeTiempo(r.estado)}
      <div class="table-wrap"><table><thead><tr><th>Herramienta</th><th class="right">Cant.</th><th class="right">Precio/día</th><th class="right">Días</th><th class="right">Subtotal</th></tr></thead>
        <tbody>${r.detalles.map((d) => `<tr><td><a href="herramienta.html?id=${d.herramientaId}" class="cell-main">${esc(d.herramienta)}</a></td>
          <td class="right">${d.cantidad}</td><td class="right num">${cop(d.precioPorDia)}</td><td class="right">${d.dias}</td><td class="right num">${cop(d.subtotal)}</td></tr>`).join('')}
          <tr><td colspan="4" class="right muted">Subtotal · IVA 19 %</td><td class="right num">${cop(r.subtotal)} · ${cop(r.iva)}</td></tr></tbody></table></div>
      ${r.motivoRechazo ? `<div class="notice bad">${icono('alertCircle')}<div><b>Motivo del rechazo:</b> ${esc(r.motivoRechazo)}</div></div>` : ''}
      ${r.notas ? `<div class="notice">${icono('info')}<div><b>Notas:</b> ${esc(r.notas)}</div></div>` : ''}
      ${rol !== 'CLIENTE' ? `<p class="small muted">Cliente: <b>${esc(r.cliente.nombreCompleto)}</b> · ${esc(r.cliente.email)} · ${esc(r.cliente.telefono || '')}</p>` : ''}
      <div data-logistica></div>
      <div class="row">${acciones(r, rol, factura)}</div>
    </div>
  </article>`;
}

/** Entrega y devolución de una reserva (se cargan al abrir la tarjeta). */
async function cargarLogistica(r, caja) {
  if (!['ENTREGADA', 'DEVUELTA'].includes(r.estado) || caja.dataset.cargada) return;
  caja.dataset.cargada = '1';
  const partes = [];
  try {
    const e = await get(`/reservas/${r.id}/entrega`);
    partes.push(`<div class="notice ok">${icono('truck')}<div><b>Entregada</b> el ${fechaHora(e.fechaEntrega)} · ${e.tipo === 'DOMICILIO' ? `a domicilio: ${esc(e.direccion)}` : 'recogida en sede'}${e.observaciones ? ` · ${esc(e.observaciones)}` : ''}</div></div>`);
  } catch { /* sin entrega registrada (datos antiguos) */ }
  if (r.estado === 'DEVUELTA') {
    try {
      const d = await get(`/reservas/${r.id}/devolucion`);
      partes.push(`<div class="notice ${d.estadoEquipo === 'CON_DANOS' ? 'warn' : 'ok'}">${icono('undo')}<div><b>Devuelta</b> el ${fechaHora(d.fechaDevolucion)} · ${badge(d.estadoEquipo)} ${d.validada ? badge('APROBADO', 'Validada por el admin') : badge('PENDIENTE', 'Por validar')}
        ${d.danos.map((x) => `<div style="margin-top:.35rem">${badge(x.severidad)} ${esc(x.herramienta)}: ${esc(x.descripcion)} · ${cop(x.costoEstimado)} · ${badge(x.estado)}</div>`).join('')}</div></div>`);
    } catch { /* sin devolución registrada */ }
  }
  caja.innerHTML = partes.join('');
}

/**
 * Lista paginada de reservas con filtros por estado. Sirve para los tres roles:
 * el backend ya devuelve solo las reservas que le corresponden a cada uno.
 */
export async function listaReservas(contenedor, { estadoInicial = '', titulo = 'Reservas', subtitulo = '', soloEstado = null } = {}) {
  const rol = sesion.rol();
  const estados = ['', 'PENDIENTE', 'APROBADA', 'PAGADA', 'ENTREGADA', 'DEVUELTA', 'RECHAZADA', 'CANCELADA', 'VENCIDA'];
  let estado = soloEstado || estadoInicial;
  let pagina = 0;
  let facturas = new Map();

  contenedor.innerHTML = `${encabezado(titulo, subtitulo)}
    ${soloEstado ? '' : `<div class="chips" data-filtro style="margin-bottom:1rem">${estados.map((e) => `<button class="chip" data-estado="${e}" aria-pressed="${e === estado}">${e ? esc(e.charAt(0) + e.slice(1).toLowerCase()) : 'Todas'}</button>`).join('')}</div>`}
    <div class="res-list" data-lista></div><nav class="pager" data-pager></nav>`;

  try {
    const f = await get('/facturas?tamanio=100');
    facturas = new Map(f.contenido.map((x) => [x.reserva.id, x]));
  } catch { /* sin facturas */ }

  let reservas = [];
  async function cargar() {
    const lista = $('[data-lista]', contenedor);
    lista.innerHTML = '<div class="skeleton" style="height:76px"></div>'.repeat(4);
    const r = await get(`/reservas${query({ estado, pagina, tamanio: 10 })}`);
    reservas = r.contenido;
    lista.innerHTML = reservas.length ? reservas.map((x) => tarjetaReserva(x, rol, facturas.get(x.id))).join('')
      : vacio(estado ? `No hay reservas ${estado.toLowerCase()}s` : 'Aún no hay reservas',
        rol === 'CLIENTE' ? 'Explora el catálogo y solicita tu primera reserva.' : 'Cuando lleguen solicitudes aparecerán aquí.', 'carretilla',
        rol === 'CLIENTE' ? '<a class="btn btn-primary" href="index.html#catalogo" style="margin-top:1rem">Ir al catálogo</a>' : '');
    paginador($('[data-pager]', contenedor), r.pagina, r.totalPaginas, (p) => { pagina = p; cargar(); });
    if (reservas.length === 1 || location.search.includes('abrir')) abrir($('.res', lista));
  }

  function abrir(tarjeta) {
    if (!tarjeta) return;
    const abierta = tarjeta.classList.toggle('open');
    $('[data-toggle]', tarjeta).setAttribute('aria-expanded', String(abierta));
    const r = reservas.find((x) => x.id === Number(tarjeta.dataset.reserva));
    if (abierta) cargarLogistica(r, $('[data-logistica]', tarjeta));
  }

  contenedor.addEventListener('click', async (e) => {
    const chip = e.target.closest('[data-estado]');
    if (chip) {
      estado = chip.dataset.estado;
      pagina = 0;
      $$('[data-estado]', contenedor).forEach((c) => c.setAttribute('aria-pressed', String(c === chip)));
      cargar();
      return;
    }
    if (e.target.closest('[data-toggle]')) { abrir(e.target.closest('.res')); return; }
    const boton = e.target.closest('[data-accion]');
    if (!boton) return;
    const r = reservas.find((x) => x.id === Number(boton.closest('.res').dataset.reserva));
    await ejecutar(boton.dataset.accion, r, boton);
  });
  contenedor.addEventListener('keydown', (e) => {
    if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('[data-toggle]')) { e.preventDefault(); abrir(e.target.closest('.res')); }
  });

  await cargar();
}

async function ejecutar(accion, r, boton) {
  try {
    if (accion === 'pagar') return modalPago(r);
    if (accion === 'entregar') return modalEntrega(r);
    if (accion === 'devolver') return modalDevolucion(r);
    if (accion === 'factura') return await descargar(`/facturas/${boton.dataset.factura}/pdf`, `${boton.dataset.numero}.pdf`);
    if (accion === 'aprobar') {
      if (!await confirmar('Aprobar reserva', `Al aprobar <b>${esc(r.codigo)}</b> las unidades quedan apartadas y el cliente tiene 24 horas para pagar.`, { aceptar: 'Aprobar' })) return;
      cargando(boton, true);
      await patch(`/reservas/${r.id}/aprobar`);
      toast(`Reserva ${r.codigo} aprobada`, 'ok');
    }
    if (accion === 'rechazar') {
      return formularioModal({
        titulo: `Rechazar ${r.codigo}`, textoBoton: 'Rechazar reserva',
        campos: '<label class="field"><span>Motivo (lo verá el cliente)</span><textarea class="textarea" name="motivo" required maxlength="300" placeholder="Ej: el equipo está en mantenimiento esas fechas"></textarea></label>',
        enviar: async ({ motivo }) => {
          await patch(`/reservas/${r.id}/rechazar`, { motivo });
          toast(`Reserva ${r.codigo} rechazada`, 'ok');
          refrescar();
        },
      });
    }
    if (accion === 'cancelar') {
      if (!await confirmar('Cancelar reserva', `¿Seguro que quieres cancelar <b>${esc(r.codigo)}</b>? Esta acción no se puede deshacer.`, { aceptar: 'Sí, cancelar', peligro: true })) return;
      cargando(boton, true);
      await patch(`/reservas/${r.id}/cancelar`);
      toast(`Reserva ${r.codigo} cancelada`, 'ok');
    }
    refrescar();
  } catch (error) {
    cargando(boton, false);
    toastError(error);
  }
}

// ---------- Pago ----------

function modalPago(r) {
  const m = formularioModal({
    titulo: `Pagar ${r.codigo}`, textoBoton: `Pagar ${cop(r.total)}`,
    campos: `
      <div class="notice">${icono('shield')}<div>Pasarela <b>simulada</b>: los datos de la tarjeta solo viajan al servidor para "cobrar" y <b>nunca se guardan</b>.</div></div>
      <div class="quote-line total" style="border:0;margin:0;padding:0"><span>Total a pagar</span><span class="num">${cop(r.total)}</span></div>
      <label class="field"><span>Método de pago</span><select class="select" name="metodo">
        <option value="TARJETA_CREDITO">Tarjeta de crédito</option><option value="TARJETA_DEBITO">Tarjeta débito</option>
        <option value="PSE">PSE</option><option value="NEQUI">Nequi</option></select></label>
      <div class="stack" data-tarjeta>
        <label class="field"><span>Número de tarjeta</span><input class="input mono" name="numeroTarjeta" inputmode="numeric" autocomplete="cc-number" placeholder="4242 4242 4242 4242" maxlength="23"></label>
        <div class="stack" style="gap:.35rem"><span class="label">Tarjetas de prueba (clic para usar)</span>
          ${TARJETAS_PRUEBA.map((t) => `<button type="button" class="test-card" style="background:var(--card);border-color:var(--line);color:var(--ink)" data-usar="${t.numero}"><span class="mono">${t.numero}</span><span class="badge ${t.tipo}">${t.resultado}</span></button>`).join('')}</div>
      </div>
      <label class="field"><span>Titular</span><input class="input" name="titular" autocomplete="cc-name" value="${esc(sesion.nombre().toUpperCase())}" required></label>`,
    enviar: async (datos) => {
      if (!datos.metodo.startsWith('TARJETA')) delete datos.numeroTarjeta;
      const resultado = await post(`/reservas/${r.id}/pagos`, datos);
      if (!resultado.aprobado) {
        // El intento queda registrado en el historial; el modal sigue abierto para reintentar
        const error = new Error(`${resultado.mensaje}. La reserva sigue aprobada: puedes intentar con otro medio de pago.`);
        throw error;
      }
      const factura = resultado.pago.factura;
      const ok = modal({
        titulo: 'Pago aprobado',
        cuerpo: `<div class="notice ok">${icono('check')}<div>Tu reserva <b>${esc(r.codigo)}</b> quedó <b>pagada</b>. Referencia: <span class="mono">${esc(resultado.pago.referenciaTransaccion)}</span></div></div>
          <p>Se generó la factura <b>${esc(factura.numero)}</b> automáticamente.</p>`,
        pie: `<button class="btn btn-ghost" data-cerrar>Cerrar</button><button class="btn btn-primary" data-pdf>${icono('download')}Descargar factura</button>`,
        alCerrar: refrescar,
      });
      $('[data-pdf]', ok.el).addEventListener('click', () => descargar(`/facturas/${factura.id}/pdf`, `${factura.numero}.pdf`).catch(toastError));
    },
  });
  const form = $('form', m.el);
  const tarjeta = $('[data-tarjeta]', m.el);
  form.metodo.addEventListener('change', () => tarjeta.classList.toggle('hidden', !form.metodo.value.startsWith('TARJETA')));
  form.numeroTarjeta.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ');
  });
  tarjeta.addEventListener('click', (e) => {
    const b = e.target.closest('[data-usar]');
    if (b) form.numeroTarjeta.value = b.dataset.usar;
  });
}

// ---------- Entrega y devolución (PROVEEDOR / ADMIN) ----------

function modalEntrega(r) {
  const m = formularioModal({
    titulo: `Entrega de ${r.codigo}`, textoBoton: 'Registrar entrega',
    campos: `
      <label class="field"><span>Tipo de entrega</span><select class="select" name="tipo">
        <option value="DOMICILIO">A domicilio</option><option value="RECOGIDA_EN_SEDE">El cliente recoge en la sede</option></select></label>
      <label class="field" data-direccion><span>Dirección de entrega</span><input class="input" name="direccion" maxlength="200" placeholder="Cra 27 # 36-14, obra Torre Norte"></label>
      <label class="field"><span>Observaciones</span><textarea class="textarea" name="observaciones" maxlength="500" placeholder="Accesorios entregados, estado del equipo…"></textarea></label>`,
    enviar: async (datos) => {
      await post(`/reservas/${r.id}/entrega`, datos);
      toast(`Entrega de ${r.codigo} registrada`, 'ok');
      refrescar();
    },
  });
  const form = $('form', m.el);
  form.tipo.addEventListener('change', () => $('[data-direccion]', m.el).classList.toggle('hidden', form.tipo.value !== 'DOMICILIO'));
}

function filaDano(r) {
  return `<div class="dano-row" data-dano>
    <label class="field"><span>Herramienta</span><select class="select" data-k="herramientaId">${r.detalles.map((d) => `<option value="${d.herramientaId}">${esc(d.herramienta)}</option>`).join('')}</select></label>
    <label class="field"><span>Severidad</span><select class="select" data-k="severidad"><option value="LEVE">Leve</option><option value="MODERADO">Moderado</option><option value="GRAVE">Grave (sale de servicio)</option></select></label>
    <label class="field"><span>Costo estimado</span><input class="input" type="number" min="0" step="1000" data-k="costoEstimado" placeholder="0"></label>
    <button type="button" class="icon-btn" data-quitar-dano aria-label="Quitar daño">${icono('trash')}</button>
    <label class="field full"><span>Descripción del daño</span><input class="input" data-k="descripcion" maxlength="500" required></label>
  </div>`;
}

export function modalDevolucion(r) {
  const m = modal({
    titulo: `Devolución de ${esc(r.codigo)}`, ancho: true,
    cuerpo: `<form id="form-devolucion" class="stack" novalidate>
      <div class="form-grid">
        <label class="field"><span>Estado del equipo</span><select class="select" name="estadoEquipo">
          <option value="BUENO">Buen estado</option><option value="DESGASTE_NORMAL">Desgaste normal</option><option value="CON_DANOS">Con daños</option></select></label>
        <label class="field"><span>Observaciones</span><input class="input" name="observaciones" maxlength="500"></label>
      </div>
      <div class="stack hidden" data-danos-caja>
        <div class="row between"><h3>Daños encontrados</h3><button type="button" class="btn btn-ghost btn-sm" data-agregar-dano>${icono('plus')}Agregar daño</button></div>
        <div class="notice warn">${icono('alert')}<div>Un daño <b>grave</b> saca de servicio una unidad de la herramienta hasta que el administrador resuelva la incidencia.</div></div>
        <div class="stack" data-danos></div>
      </div>
      <div class="notice bad hidden" data-error></div></form>`,
    pie: `<button class="btn btn-ghost" data-cerrar>Cancelar</button><button class="btn btn-primary" type="submit" form="form-devolucion">Confirmar devolución</button>`,
  });
  const form = $('form', m.el);
  const caja = $('[data-danos-caja]', m.el);
  const lista = $('[data-danos]', m.el);
  form.estadoEquipo.addEventListener('change', () => {
    const conDanos = form.estadoEquipo.value !== 'BUENO';
    caja.classList.toggle('hidden', !conDanos);
    if (form.estadoEquipo.value === 'CON_DANOS' && !lista.children.length) lista.insertAdjacentHTML('beforeend', filaDano(r));
  });
  m.el.addEventListener('click', (e) => {
    if (e.target.closest('[data-agregar-dano]')) lista.insertAdjacentHTML('beforeend', filaDano(r));
    if (e.target.closest('[data-quitar-dano]')) e.target.closest('[data-dano]').remove();
  });
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const boton = $('button[type="submit"]', m.el);
    const danos = form.estadoEquipo.value === 'BUENO' ? [] : $$('[data-dano]', lista).map((fila) => {
      const d = Object.fromEntries($$('[data-k]', fila).map((c) => [c.dataset.k, c.value]));
      return { ...d, herramientaId: Number(d.herramientaId), costoEstimado: d.costoEstimado ? Number(d.costoEstimado) : 0 };
    });
    cargando(boton, true);
    try {
      await post(`/reservas/${r.id}/devolucion`, { estadoEquipo: form.estadoEquipo.value, observaciones: form.observaciones.value, danos });
      m.cerrar();
      toast(`Devolución de ${r.codigo} confirmada${danos.length ? ` con ${danos.length} daño(s) reportado(s)` : ''}`, 'ok');
      refrescar();
    } catch (error) {
      mostrarErrorEnFormulario(form, $('[data-error]', m.el), error);
    } finally {
      cargando(boton, false);
    }
  });
}

// ---------- Cambiar contraseña (todos los roles) ----------

export function modalCambiarPassword() {
  formularioModal({
    titulo: 'Cambiar mi contraseña', textoBoton: 'Cambiar contraseña',
    campos: `
      <label class="field"><span>Contraseña actual</span><input class="input" type="password" name="passwordActual" autocomplete="current-password" required></label>
      <label class="field"><span>Contraseña nueva</span><input class="input" type="password" name="passwordNueva" autocomplete="new-password" minlength="8" required>
        <small class="hint">Mínimo 8 caracteres, con al menos una letra y un número.</small></label>
      <label class="field"><span>Repite la contraseña nueva</span><input class="input" type="password" name="confirmacion" autocomplete="new-password" required></label>`,
    enviar: async (d) => {
      if (d.passwordNueva !== d.confirmacion) {
        throw Object.assign(new Error('Las contraseñas nuevas no coinciden'), { errores: { confirmacion: 'Las contraseñas no coinciden' } });
      }
      await post('/auth/cambiar-password', { passwordActual: d.passwordActual, passwordNueva: d.passwordNueva });
      toast('Tu contraseña se actualizó correctamente', 'ok');
    },
  });
}

// ---------- Pagos y facturas ----------

export async function seccionPagos(main, subtitulo) {
  let pagina = 0;
  main.innerHTML = `${encabezado('Pagos', subtitulo)}<div class="table-wrap" data-tabla></div><nav class="pager" data-pager></nav>`;
  async function cargar() {
    const p = await get(`/pagos${query({ pagina, tamanio: 15 })}`);
    $('[data-tabla]', main).innerHTML = p.contenido.length ? `<table>
      <thead><tr><th>Fecha</th><th>Reserva</th><th>${sesion.rol() === 'CLIENTE' ? 'Proveedor' : 'Cliente'}</th><th>Método</th><th>Referencia</th><th>Estado</th><th class="right">Monto</th><th>Factura</th></tr></thead>
      <tbody>${p.contenido.map((x) => `<tr>
        <td>${fechaHora(x.fechaPago)}</td><td class="mono">${esc(x.reserva.codigo)}</td>
        <td>${esc(sesion.rol() === 'CLIENTE' ? x.reserva.proveedor : x.reserva.cliente)}</td>
        <td>${esc(x.metodo.replace('_', ' ').toLowerCase())}</td><td class="mono small">${esc(x.referenciaTransaccion)}</td>
        <td>${badge(x.estado)}</td><td class="right num cell-main">${cop(x.monto)}</td>
        <td>${x.factura ? `<button class="btn-text small" data-pdf="${x.factura.id}" data-numero="${esc(x.factura.numero)}">${icono('download', 'ico')} ${esc(x.factura.numero)}</button>` : '<span class="muted">—</span>'}</td>
      </tr>`).join('')}</tbody></table>` : vacio('Sin pagos todavía', 'Aquí verás cada intento de pago, aprobado o rechazado.', 'reflector');
    paginador($('[data-pager]', main), p.pagina, p.totalPaginas, (n) => { pagina = n; cargar(); });
  }
  main.addEventListener('click', (e) => {
    const b = e.target.closest('[data-pdf]');
    if (b) descargar(`/facturas/${b.dataset.pdf}/pdf`, `${b.dataset.numero}.pdf`).catch(toastError);
  });
  await cargar();
}

export async function seccionFacturas(main, subtitulo) {
  let pagina = 0;
  main.innerHTML = `${encabezado('Facturas', subtitulo)}<div class="table-wrap" data-tabla></div><nav class="pager" data-pager></nav>`;
  async function cargar() {
    const f = await get(`/facturas${query({ pagina, tamanio: 15 })}`);
    $('[data-tabla]', main).innerHTML = f.contenido.length ? `<table>
      <thead><tr><th>Número</th><th>Emisión</th><th>Reserva</th><th>Proveedor</th><th>Cliente</th><th class="right">Subtotal</th><th class="right">IVA</th><th class="right">Total</th><th>Estado</th><th></th></tr></thead>
      <tbody>${f.contenido.map((x) => `<tr>
        <td class="mono cell-main">${esc(x.numero)}</td><td>${fecha(x.fechaEmision)}</td><td class="mono">${esc(x.reserva.codigo)}</td>
        <td>${esc(x.emisor.razonSocial)}</td><td>${esc(x.cliente.nombreCompleto)}</td>
        <td class="right num">${cop(x.subtotal)}</td><td class="right num">${cop(x.iva)}</td><td class="right num cell-main">${cop(x.total)}</td>
        <td>${badge(x.estado)}</td>
        <td><button class="btn btn-ghost btn-sm" data-pdf="${x.id}" data-numero="${esc(x.numero)}">${icono('download')}PDF</button></td></tr>`).join('')}</tbody></table>`
      : vacio('Sin facturas todavía', 'Las facturas se generan solas cuando se aprueba un pago.', 'reflector');
    paginador($('[data-pager]', main), f.pagina, f.totalPaginas, (n) => { pagina = n; cargar(); });
  }
  main.addEventListener('click', (e) => {
    const b = e.target.closest('[data-pdf]');
    if (b) {
      cargando(b, true);
      descargar(`/facturas/${b.dataset.pdf}/pdf`, `${b.dataset.numero}.pdf`).catch(toastError).finally(() => cargando(b, false));
    }
  });
  await cargar();
}
