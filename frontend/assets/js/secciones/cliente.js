// Panel del CLIENTE: resumen, reservas (pagar, cancelar, factura), pagos, facturas y perfil.

import { get, put } from '../api.js';
import { sesion } from '../sesion.js';
import { icono, ilustracion } from '../iconos.js';
import { $, esc, cop, fecha, badge, formularioModal, toast } from '../ui.js';
import { encabezado, kpi, plazo, listaReservas, seccionPagos, seccionFacturas, refrescar } from './comunes.js';

async function inicio(main) {
  const r = await get('/reservas?tamanio=100');
  const reservas = r.contenido;
  const porPagar = reservas.filter((x) => x.estado === 'APROBADA');
  const activas = reservas.filter((x) => ['APROBADA', 'PAGADA', 'ENTREGADA'].includes(x.estado));
  const pagadas = reservas.filter((x) => ['PAGADA', 'ENTREGADA', 'DEVUELTA'].includes(x.estado));
  const invertido = pagadas.reduce((s, x) => s + Number(x.total), 0);
  const proxima = reservas.filter((x) => ['PAGADA', 'APROBADA'].includes(x.estado))
    .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio))[0];

  main.innerHTML = `
    ${encabezado(`Hola, ${sesion.usuario().nombres}`, 'Este es el resumen de tus alquileres.',
      `<a class="btn btn-primary" href="index.html#catalogo">${icono('plus')}Nueva reserva</a>`)}
    <div class="kpis">
      ${kpi('Por pagar', porPagar.length, porPagar.length ? 'Tienes 24 h desde la aprobación' : 'Nada pendiente', { icon: 'card', destacado: porPagar.length > 0 })}
      ${kpi('Alquileres activos', activas.length, 'Aprobados, pagados o en obra', { icon: 'truck' })}
      ${kpi('Completados', reservas.filter((x) => x.estado === 'DEVUELTA').length, 'Equipos ya devueltos', { icon: 'check' })}
      ${kpi('Total invertido', cop(invertido), `${pagadas.length} reserva(s) pagada(s)`, { icon: 'money' })}
    </div>
    ${porPagar.length ? `<div class="box" style="margin-top:1rem;border-color:var(--accent)">
      <div class="box-head"><h3>Acción requerida</h3></div>
      <div class="stack" style="gap:.6rem">${porPagar.map((x) => `
        <div class="row between" style="padding:.7rem 0;border-top:1px solid var(--line)">
          <div><b class="mono">${esc(x.codigo)}</b> · ${esc(x.proveedor.razonSocial)}<div class="cell-sub">${plazo(x.fechaLimitePago)}</div></div>
          <div class="row"><b class="num">${cop(x.total)}</b><a class="btn btn-primary btn-sm" href="#reservas">Pagar ahora</a></div>
        </div>`).join('')}</div></div>` : ''}
    <div class="cards-3" style="margin-top:1rem">
      <div class="box">
        <div class="box-head"><h3>Últimas reservas</h3><a class="btn-text small" href="#reservas">Ver todas</a></div>
        ${reservas.length ? `<div class="table-wrap"><table><thead><tr><th>Código</th><th>Proveedor</th><th>Fechas</th><th>Estado</th><th class="right">Total</th></tr></thead>
          <tbody>${reservas.slice(0, 6).map((x) => `<tr><td class="mono cell-main">${esc(x.codigo)}</td><td>${esc(x.proveedor.razonSocial)}</td>
            <td>${fecha(x.fechaInicio, { day: 'numeric', month: 'short' })} → ${fecha(x.fechaFin, { day: 'numeric', month: 'short' })}</td>
            <td>${badge(x.estado)}</td><td class="right num">${cop(x.total)}</td></tr>`).join('')}</tbody></table></div>`
          : '<p class="muted">Aún no tienes reservas.</p>'}
      </div>
      <div class="box">
        <div class="box-head"><h3>Tu próximo alquiler</h3></div>
        ${proxima ? `<div class="stack" style="gap:.6rem">
          <div class="thumb" style="aspect-ratio:16/9;border-radius:var(--r)">${ilustracion(proxima.detalles[0].herramienta)}</div>
          <b>${esc(proxima.detalles.map((d) => d.herramienta).join(', '))}</b>
          <span class="muted small">${fecha(proxima.fechaInicio)} → ${fecha(proxima.fechaFin)} · ${esc(proxima.proveedor.razonSocial)}</span>
          ${badge(proxima.estado)}</div>` : '<p class="muted">No tienes alquileres próximos.</p>'}
      </div>
    </div>`;
}

async function perfil(main) {
  const id = sesion.perfil()?.clienteId;
  const c = await get(`/clientes/${id}`);
  main.innerHTML = `${encabezado('Mi perfil', 'Tus datos de contacto y de facturación.',
    `<button class="btn btn-primary" data-editar>${icono('edit')}Editar datos</button>`)}
    <div class="box"><dl class="specs">
      <div><dt>Nombre</dt><dd>${esc(c.usuario.nombres)} ${esc(c.usuario.apellidos)}</dd></div>
      <div><dt>Correo</dt><dd>${esc(c.usuario.email)}</dd></div>
      <div><dt>Documento</dt><dd>${esc(c.tipoDocumento)} ${esc(c.numeroDocumento)}</dd></div>
      <div><dt>Teléfono</dt><dd>${esc(c.usuario.telefono || '—')}</dd></div>
      <div><dt>Dirección</dt><dd>${esc(c.direccion)}</dd></div>
      <div><dt>Ciudad</dt><dd>${esc(c.ciudad)}</dd></div>
    </dl></div>`;
  $('[data-editar]', main).addEventListener('click', () => formularioModal({
    titulo: 'Editar mis datos',
    campos: `<div class="form-grid">
      <label class="field"><span>Nombres</span><input class="input" name="nombres" value="${esc(c.usuario.nombres)}" required></label>
      <label class="field"><span>Apellidos</span><input class="input" name="apellidos" value="${esc(c.usuario.apellidos)}" required></label>
      <label class="field"><span>Teléfono</span><input class="input" name="telefono" value="${esc(c.usuario.telefono || '')}"></label>
      <label class="field"><span>Ciudad</span><input class="input" name="ciudad" value="${esc(c.ciudad)}" required></label>
      <label class="field full"><span>Dirección</span><input class="input" name="direccion" value="${esc(c.direccion)}" required></label></div>`,
    enviar: async (datos) => {
      await put(`/clientes/${id}`, datos);
      toast('Datos actualizados', 'ok');
      refrescar();
    },
  }));
}

export const seccionesCliente = [
  { id: 'inicio', titulo: 'Resumen', icono: 'home', pintar: inicio },
  {
    id: 'reservas', titulo: 'Mis reservas', icono: 'calendar',
    pintar: (m) => listaReservas(m, { titulo: 'Mis reservas', subtitulo: 'Paga las aprobadas, descarga tus facturas y sigue cada alquiler.' }),
    contar: async () => (await get('/reservas?estado=APROBADA&tamanio=1')).totalElementos,
  },
  { id: 'pagos', titulo: 'Pagos', icono: 'card', pintar: (m) => seccionPagos(m, 'Historial de todos tus intentos de pago.') },
  { id: 'facturas', titulo: 'Facturas', icono: 'file', pintar: (m) => seccionFacturas(m, 'Descarga tus facturas en PDF.') },
  { id: 'perfil', titulo: 'Mi perfil', icono: 'user', pintar: perfil },
];
