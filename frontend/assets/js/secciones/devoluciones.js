// Devoluciones e incidencias (daños). El PROVEEDOR reporta daños de sus devoluciones;
// el ADMIN valida devoluciones y avanza las incidencias ABIERTO → EN_REVISION → RESUELTO.

import { get, post, patch, query } from '../api.js';
import { sesion } from '../sesion.js';
import { icono } from '../iconos.js';
import { $, $$, esc, cop, fechaHora, badge, formularioModal, confirmar, toast, toastError, paginador, vacio, cargando } from '../ui.js';
import { encabezado } from './comunes.js';

export async function seccionDevoluciones(main) {
  const esAdmin = sesion.rol() === 'ADMIN';
  let filtro = esAdmin ? 'false' : '';
  let pagina = 0;
  let devoluciones = [];

  main.innerHTML = `${encabezado('Devoluciones', esAdmin
    ? 'Revisa el estado en que volvieron los equipos y valida cada devolución.'
    : 'Equipos que te devolvieron. Si encuentras un daño después, repórtalo aquí.')}
    <div class="chips" style="margin-bottom:1rem" data-filtros>
      <button class="chip" data-v="" aria-pressed="${filtro === ''}">Todas</button>
      <button class="chip" data-v="false" aria-pressed="${filtro === 'false'}">Por validar</button>
      <button class="chip" data-v="true" aria-pressed="${filtro === 'true'}">Validadas</button></div>
    <div class="table-wrap" data-tabla></div><nav class="pager" data-pager></nav>`;

  async function cargar() {
    const d = await get(`/devoluciones${query({ validada: filtro, pagina, tamanio: 15 })}`);
    devoluciones = d.contenido;
    $('[data-tabla]', main).innerHTML = devoluciones.length ? `<table>
      <thead><tr><th>Reserva</th><th>Cliente</th>${esAdmin ? '<th>Proveedor</th>' : ''}<th>Recibida</th><th>Equipo</th><th>Daños</th><th>Validación</th><th></th></tr></thead>
      <tbody>${devoluciones.map((x) => `<tr>
        <td class="mono cell-main">${esc(x.reserva.codigo)}</td><td>${esc(x.reserva.cliente)}</td>${esAdmin ? `<td class="small">${esc(x.reserva.proveedor)}</td>` : ''}
        <td>${fechaHora(x.fechaDevolucion)}<div class="cell-sub">por ${esc(x.recibidaPor)}</div></td>
        <td>${badge(x.estadoEquipo)}${x.diasRetraso > 0 ? `<div class="cell-sub" style="color:var(--bad)">Tardía: ${x.diasRetraso} día(s) · multa ${cop(x.multa)}</div>` : ''}</td>
        <td>${x.danos.length ? x.danos.map((dn) => `<div class="small">${badge(dn.severidad)} ${esc(dn.herramienta)} · ${cop(dn.costoEstimado)}</div>`).join('') : '<span class="muted">—</span>'}</td>
        <td>${x.validada ? `${badge('APROBADO', 'Validada')}<div class="cell-sub">${esc(x.validadaPor)} · ${fechaHora(x.fechaValidacion)}</div>` : badge('PENDIENTE', 'Por validar')}</td>
        <td><div class="actions">
          ${esAdmin && !x.validada ? `<button class="btn btn-ok btn-sm" data-validar="${x.id}">${icono('check')}Validar</button>` : ''}
          ${x.estadoEquipo !== 'BUENO' ? `<button class="btn btn-ghost btn-sm" data-dano="${x.id}">${icono('plus')}Reportar daño</button>` : ''}
        </div></td></tr>`).join('')}</tbody></table>`
      : vacio('No hay devoluciones', filtro === 'false' ? 'Todas las devoluciones están validadas.' : 'Cuando se registren devoluciones aparecerán aquí.', 'carretilla');
    paginador($('[data-pager]', main), d.pagina, d.totalPaginas, (p) => { pagina = p; cargar(); });
  }

  main.addEventListener('click', async (e) => {
    const chip = e.target.closest('[data-v]');
    if (chip) {
      filtro = chip.dataset.v;
      pagina = 0;
      $$('[data-v]', main).forEach((c) => c.setAttribute('aria-pressed', String(c === chip)));
      cargar();
    }
    const validar = e.target.closest('[data-validar]');
    if (validar) {
      const dv = devoluciones.find((x) => x.id === Number(validar.dataset.validar));
      if (!await confirmar('Validar devolución', `Confirmas que revisaste la devolución de <b>${esc(dv.reserva.codigo)}</b> (${esc(dv.estadoEquipo.toLowerCase().replace('_', ' '))}).`, { aceptar: 'Validar' })) return;
      cargando(validar, true);
      try {
        await patch(`/devoluciones/${dv.id}/validar`);
        toast(`Devolución de ${dv.reserva.codigo} validada`, 'ok');
        cargar();
      } catch (error) { toastError(error); cargando(validar, false); }
    }
    const dano = e.target.closest('[data-dano]');
    if (dano) modalDano(devoluciones.find((x) => x.id === Number(dano.dataset.dano)), cargar);
  });

  await cargar();
}

async function modalDano(devolucion, alTerminar) {
  const reserva = await get(`/reservas/${devolucion.reserva.id}`);
  formularioModal({
    titulo: `Reportar daño · ${devolucion.reserva.codigo}`, textoBoton: 'Reportar daño',
    campos: `<div class="form-grid">
      <label class="field"><span>Herramienta</span><select class="select" name="herramientaId">${reserva.detalles.map((d) => `<option value="${d.herramientaId}">${esc(d.herramienta)}</option>`).join('')}</select></label>
      <label class="field"><span>Severidad</span><select class="select" name="severidad"><option value="LEVE">Leve</option><option value="MODERADO">Moderado</option><option value="GRAVE">Grave (sale de servicio)</option></select></label>
      <label class="field full"><span>Descripción</span><textarea class="textarea" name="descripcion" maxlength="500" required></textarea></label>
      <label class="field"><span>Costo estimado (COP)</span><input class="input" type="number" min="0" step="1000" name="costoEstimado" value="0"></label></div>`,
    enviar: async (d) => {
      await post(`/devoluciones/${devolucion.id}/danos`, { ...d, herramientaId: Number(d.herramientaId), costoEstimado: Number(d.costoEstimado || 0) });
      toast('Daño reportado', 'ok');
      alTerminar();
    },
  });
}

const SIGUIENTE = { ABIERTO: 'EN_REVISION', EN_REVISION: 'RESUELTO' };

export async function seccionIncidencias(main) {
  const esAdmin = sesion.rol() === 'ADMIN';
  let estado = esAdmin ? 'ABIERTO' : '';
  let severidad = '';
  let pagina = 0;
  let reportes = [];

  main.innerHTML = `${encabezado('Incidencias por daños', esAdmin
    ? 'Gestiona cada reporte: ABIERTO → EN REVISIÓN → RESUELTO. Resolver un daño grave devuelve la unidad reparada al inventario.'
    : 'Daños reportados en tus devoluciones y en qué estado está su gestión.')}
    <div class="row" style="margin-bottom:1rem">
      <div class="chips" data-estados>${['', 'ABIERTO', 'EN_REVISION', 'RESUELTO'].map((e) => `<button class="chip" data-e="${e}" aria-pressed="${e === estado}">${e ? esc(e.replace('_', ' ').toLowerCase()) : 'Todas'}</button>`).join('')}</div>
      <select class="select" style="width:auto" data-severidad aria-label="Severidad"><option value="">Toda severidad</option><option value="LEVE">Leve</option><option value="MODERADO">Moderado</option><option value="GRAVE">Grave</option></select>
    </div>
    <div class="table-wrap" data-tabla></div><nav class="pager" data-pager></nav>`;

  async function cargar() {
    const r = await get(`/danos${query({ estado, severidad, pagina, tamanio: 15 })}`);
    reportes = r.contenido;
    $('[data-tabla]', main).innerHTML = reportes.length ? `<table>
      <thead><tr><th>#</th><th>Herramienta</th><th>Reserva</th><th>Descripción</th><th>Severidad</th><th class="right">Costo</th><th>Estado</th><th></th></tr></thead>
      <tbody>${reportes.map((x) => `<tr>
        <td class="muted">${x.id}</td><td class="cell-main">${esc(x.herramienta)}<div class="cell-sub">${esc(x.reserva.proveedor)}</div></td>
        <td class="mono">${esc(x.reserva.codigo)}<div class="cell-sub">${esc(x.reserva.cliente)}</div></td>
        <td style="max-width:280px">${esc(x.descripcion)}<div class="cell-sub">Reportado por ${esc(x.reportadoPor)} · ${fechaHora(x.creadoEn)}</div></td>
        <td>${badge(x.severidad)}</td><td class="right num">${cop(x.costoEstimado)}</td><td>${badge(x.estado)}</td>
        <td>${esAdmin && SIGUIENTE[x.estado] ? `<button class="btn btn-ghost btn-sm" data-avanzar="${x.id}">${SIGUIENTE[x.estado] === 'RESUELTO' ? 'Marcar resuelto' : 'Pasar a revisión'} ${icono('arrow')}</button>` : ''}</td>
      </tr>`).join('')}</tbody></table>`
      : vacio('Sin incidencias', 'No hay reportes de daño con ese filtro.', 'demoledor');
    paginador($('[data-pager]', main), r.pagina, r.totalPaginas, (p) => { pagina = p; cargar(); });
  }

  main.addEventListener('click', async (e) => {
    const chip = e.target.closest('[data-e]');
    if (chip) {
      estado = chip.dataset.e;
      pagina = 0;
      $$('[data-e]', main).forEach((c) => c.setAttribute('aria-pressed', String(c === chip)));
      cargar();
    }
    const b = e.target.closest('[data-avanzar]');
    if (b) {
      const x = reportes.find((r) => r.id === Number(b.dataset.avanzar));
      const siguiente = SIGUIENTE[x.estado];
      const extra = siguiente === 'RESUELTO' && x.severidad === 'GRAVE' ? '<br><br>Como es un daño <b>grave</b>, la unidad reparada vuelve a estar disponible para alquilar.' : '';
      if (!await confirmar('Actualizar incidencia', `La incidencia de <b>${esc(x.herramienta)}</b> pasará a <b>${siguiente.replace('_', ' ').toLowerCase()}</b>.${extra}`, { aceptar: 'Confirmar' })) return;
      cargando(b, true);
      try {
        await patch(`/danos/${x.id}/estado`, { estado: siguiente });
        toast('Incidencia actualizada', 'ok');
        cargar();
      } catch (error) { toastError(error); cargando(b, false); }
    }
  });
  $('[data-severidad]', main).addEventListener('change', (e) => { severidad = e.target.value; pagina = 0; cargar(); });

  await cargar();
}
