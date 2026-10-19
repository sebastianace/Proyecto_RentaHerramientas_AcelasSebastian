// Tablero de reportes. El mismo código sirve al PROVEEDOR y al ADMIN: el backend decide el alcance
// (un proveedor siempre recibe solo sus datos). El ADMIN ve además el comparativo entre proveedores.

import { get, query } from '../api.js';
import { sesion } from '../sesion.js';
import { icono } from '../iconos.js';
import { $, $$, esc, cop, copCorto, numero, fecha, badge, etiquetaEstado, hoy } from '../ui.js';
import { columnas, barrasH } from '../graficas.js';
import { encabezado, kpi } from './comunes.js';

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

function periodos() {
  const h = new Date(`${hoy()}T00:00:00`);
  const iso = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const menos = (dias) => { const d = new Date(h); d.setDate(d.getDate() - dias); return iso(d); };
  return [
    { id: 'anio', nombre: 'Este año', desde: `${h.getFullYear()}-01-01` },
    { id: '90', nombre: 'Últimos 90 días', desde: menos(89) },
    { id: '30', nombre: 'Últimos 30 días', desde: menos(29) },
    { id: 'mes', nombre: 'Este mes', desde: iso(new Date(h.getFullYear(), h.getMonth(), 1)) },
  ];
}

export async function tablero(main) {
  const esAdmin = sesion.rol() === 'ADMIN';
  let periodo = periodos()[0];

  main.innerHTML = `
    ${encabezado(esAdmin ? 'Panel de la plataforma' : 'Mi negocio',
      esAdmin ? 'Ingresos, uso del inventario, rentabilidad y alertas de toda la plataforma.' : 'Cómo le va a tu inventario: ingresos, herramientas más pedidas y tus mejores clientes.',
      `<div class="chips" data-periodos>${periodos().map((p) => `<button class="chip" data-periodo="${p.id}" aria-pressed="${p.id === periodo.id}">${p.nombre}</button>`).join('')}</div>`)}
    <div data-contenido></div>`;

  async function cargar() {
    const caja = $('[data-contenido]', main);
    caja.innerHTML = '<div class="kpis">' + '<div class="skeleton" style="height:110px"></div>'.repeat(4) + '</div>';
    const q = query({ desde: periodo.desde, hasta: hoy() });
    const [panel, rentabilidad, inventario, proveedores] = await Promise.all([
      get(`/reportes/panel${q}`),
      get(`/reportes/rentabilidad${q}`),
      get('/reportes/inventario'),
      esAdmin ? get(`/reportes/ingresos/proveedores${q}`) : Promise.resolve([]),
    ]);
    const r = panel.resumen;
    const alertas = inventario.filter((h) => h.alerta);
    const enReservas = Object.entries(r.reservasPorEstado);

    caja.innerHTML = `
      <div class="kpis">
        ${kpi('Ingresos netos', copCorto(r.ingresosNetos), `${cop(r.ingresosBrutos)} con IVA`, { icon: 'money', destacado: true })}
        ${kpi('Pagos aprobados', numero(r.pagosAprobados), `${r.tasaAprobacionPagos}% de aprobación · ${r.pagosRechazados} rechazados`, { icon: 'card' })}
        ${kpi('Ticket promedio', copCorto(r.ticketPromedio), `${numero(r.facturasEmitidas)} facturas emitidas`, { icon: 'file' })}
        ${kpi('Clientes activos', numero(r.clientesActivos), 'Con al menos un alquiler pagado', { icon: 'users' })}
        ${kpi('Inventario', `${numero(r.unidadesTotales - r.unidadesFueraServicio)}/${numero(r.unidadesTotales)}`, `${r.herramientas} herramientas · ${r.unidadesFueraServicio} en reparación`, { icon: 'box' })}
        ${kpi('Pendientes de gestión', numero(r.incidenciasAbiertas + r.devolucionesPorValidar), `${r.incidenciasAbiertas} incidencias · ${r.devolucionesPorValidar} devoluciones por validar`, { icon: 'alert' })}
      </div>

      <div class="cards-3" style="margin-top:1rem">
        <div class="box"><div class="box-head"><div><h3>Ingresos netos por mes</h3><p class="small muted">Pagos aprobados, sin IVA</p></div></div><div data-g-ingresos></div></div>
        <div class="box"><div class="box-head"><h3>Reservas por estado</h3></div><div data-g-estados></div></div>
      </div>

      <div class="cards-2" style="margin-top:1rem">
        <div class="box"><div class="box-head"><div><h3>Herramientas más alquiladas</h3><p class="small muted">Número de alquileres en el periodo</p></div></div><div data-g-top></div></div>
        ${esAdmin
          ? '<div class="box"><div class="box-head"><div><h3>Ingresos por proveedor</h3><p class="small muted">Top 8, ingresos netos</p></div></div><div data-g-proveedores></div></div>'
          : '<div class="box"><div class="box-head"><h3>Clientes frecuentes</h3></div><div data-clientes></div></div>'}
      </div>

      ${esAdmin ? '<div class="box" style="margin-top:1rem"><div class="box-head"><h3>Clientes frecuentes</h3></div><div data-clientes></div></div>' : ''}

      <div class="box" style="margin-top:1rem">
        <div class="box-head"><div><h3>Rentabilidad por herramienta</h3><p class="small muted">Utilidad = ingresos − costo estimado de daños. Ocupación = unidades-día alquiladas ÷ capacidad del periodo.</p></div></div>
        <div class="table-wrap"><table><thead><tr><th>Herramienta</th>${esAdmin ? '<th>Proveedor</th>' : ''}<th class="right">Ingresos</th><th class="right">Daños</th><th class="right">Utilidad</th><th class="right">Margen</th><th class="right">Ocupación</th></tr></thead>
          <tbody>${rentabilidad.filter((x) => Number(x.ingresos) > 0 || x.danos > 0).slice(0, 10).map((x) => `<tr>
            <td class="cell-main">${esc(x.herramienta)}</td>${esAdmin ? `<td class="small">${esc(x.proveedor)}</td>` : ''}
            <td class="right num">${cop(x.ingresos)}</td><td class="right num">${x.danos ? `${cop(x.costoDanos)} <span class="muted small">(${x.danos})</span>` : '—'}</td>
            <td class="right num cell-main">${cop(x.utilidad)}</td><td class="right num">${x.margen}%</td><td class="right num">${x.ocupacion}%</td></tr>`).join('')
            || `<tr><td colspan="7" class="muted">Sin alquileres en el periodo.</td></tr>`}</tbody></table></div>
      </div>

      <div class="box" style="margin-top:1rem">
        <div class="box-head"><div><h3>Disponibilidad y estado hoy</h3><p class="small muted">${alertas.length} herramienta(s) requieren atención</p></div>
          ${esAdmin ? '<a class="btn-text small" href="#inventario">Ver inventario completo</a>' : '<a class="btn-text small" href="#inventario">Gestionar inventario</a>'}</div>
        ${alertas.length ? `<div class="table-wrap"><table><thead><tr><th>Herramienta</th>${esAdmin ? '<th>Proveedor</th>' : ''}<th>Estado</th><th class="right">Operativas</th><th class="right">Comprometidas hoy</th><th class="right">Libres hoy</th><th>Alerta</th></tr></thead>
          <tbody>${alertas.slice(0, 8).map((h) => `<tr><td class="cell-main">${esc(h.herramienta)}</td>${esAdmin ? `<td class="small">${esc(h.proveedor)}</td>` : ''}
            <td>${badge(h.estado)}</td><td class="right">${h.operativas}/${h.stock}</td><td class="right">${h.comprometidasHoy}</td><td class="right">${h.disponiblesHoy}</td>
            <td><span class="badge warn">${esc(h.alerta)}</span></td></tr>`).join('')}</tbody></table></div>`
          : `<div class="notice ok">${icono('check')}<div>Todo el inventario está operativo y con unidades libres hoy.</div></div>`}
      </div>`;

    columnas($('[data-g-ingresos]', caja), panel.ingresosPorMes.map((m) => {
      const [a, mes] = m.mes.split('-');
      return { etiqueta: `${MESES[Number(mes) - 1]} ${a.slice(2)}`, valor: Number(m.ingresosNetos), detalle: `${m.pagos} pago(s)` };
    }), { formato: cop, formatoEje: copCorto, nombreValor: 'Ingresos netos' });

    barrasH($('[data-g-estados]', caja), enReservas.map(([e, n]) => ({ etiqueta: etiquetaEstado(e), valor: n })),
      { formato: (n) => `${n}`, vacio: 'Sin reservas creadas en el periodo.' });

    barrasH($('[data-g-top]', caja), panel.masAlquiladas.map((h) => ({ etiqueta: h.herramienta, sub: h.categoria, valor: h.vecesAlquilada })),
      { formato: (n) => `${n} alquiler${n === 1 ? '' : 'es'}`, vacio: 'Sin alquileres en el periodo.' });

    if (esAdmin) {
      barrasH($('[data-g-proveedores]', caja), proveedores.filter((p) => Number(p.ingresosNetos) > 0).slice(0, 8)
        .map((p) => ({ etiqueta: p.proveedor, sub: p.ciudad, valor: Number(p.ingresosNetos) })), { formato: copCorto });
    }

    $('[data-clientes]', caja).innerHTML = panel.clientesFrecuentes.length ? `<div class="table-wrap"><table>
      <thead><tr><th>Cliente</th><th>Ciudad</th><th class="right">Alquileres</th><th class="right">Total</th><th>Último</th></tr></thead>
      <tbody>${panel.clientesFrecuentes.map((c) => `<tr><td><span class="cell-main">${esc(c.cliente)}</span><div class="cell-sub">${esc(c.email)}</div></td>
        <td>${esc(c.ciudad)}</td><td class="right">${c.reservas}</td><td class="right num">${cop(c.totalGastado)}</td><td>${fecha(c.ultimoAlquiler)}</td></tr>`).join('')}</tbody></table></div>`
      : '<p class="muted small">Sin clientes en el periodo.</p>';
  }

  $('[data-periodos]', main).addEventListener('click', (e) => {
    const b = e.target.closest('[data-periodo]');
    if (!b) return;
    periodo = periodos().find((p) => p.id === b.dataset.periodo);
    $$('[data-periodo]', main).forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
    cargar();
  });

  await cargar();
}
