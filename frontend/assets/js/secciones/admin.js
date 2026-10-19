// Panel del ADMIN: tablero de la plataforma, usuarios (clientes y proveedores), reservas,
// pagos, facturación, devoluciones por validar, incidencias, herramientas y categorías.

import { get, post, put, patch, del, query } from '../api.js';
import { icono } from '../iconos.js';
import { $, $$, esc, badge, formularioModal, confirmar, toast, toastError, paginador, vacio, cargando } from '../ui.js';
import { encabezado, listaReservas, seccionPagos, seccionFacturas } from './comunes.js';
import { tablero } from './tablero.js';
import { seccionDevoluciones, seccionIncidencias } from './devoluciones.js';
import { seccionInventario } from './proveedor.js';

// ---------- Usuarios ----------

const CAMPOS_PROVEEDOR = `<div class="form-grid">
  <label class="field full"><span>Razón social</span><input class="input" name="razonSocial" required></label>
  <label class="field"><span>NIT</span><input class="input" name="nit" required placeholder="900123456-1"></label>
  <label class="field"><span>Ciudad</span><input class="input" name="ciudad" required></label>
  <label class="field full"><span>Dirección</span><input class="input" name="direccion" required></label>
  <label class="field"><span>Nombres del contacto</span><input class="input" name="nombres" required></label>
  <label class="field"><span>Apellidos del contacto</span><input class="input" name="apellidos" required></label>
  <label class="field"><span>Correo (usuario)</span><input class="input" type="email" name="email" required></label>
  <label class="field"><span>Teléfono</span><input class="input" name="telefono"></label>
  <label class="field full"><span>Contraseña inicial</span><input class="input" type="password" name="password" required minlength="8">
    <small class="hint">Mínimo 8 caracteres con letras y números. Compártela con el proveedor por un canal seguro.</small></label>
  <label class="field full"><span>Descripción</span><textarea class="textarea" name="descripcion"></textarea></label></div>`;

async function usuarios(main) {
  let tipo = 'clientes';
  let pagina = 0;
  main.innerHTML = `${encabezado('Usuarios', 'Clientes y proveedores de la plataforma. Desactivar un usuario le impide iniciar sesión (borrado lógico).',
    `<button class="btn btn-primary" data-nuevo-proveedor>${icono('plus')}Nuevo proveedor</button>`)}
    <div class="tabs" style="max-width:360px;margin-bottom:1rem" role="tablist">
      <button role="tab" aria-selected="true" data-tipo="clientes">Clientes</button>
      <button role="tab" aria-selected="false" data-tipo="proveedores">Proveedores</button></div>
    <div class="table-wrap" data-tabla></div><nav class="pager" data-pager></nav>`;

  async function cargar() {
    const r = await get(`/${tipo}${query({ pagina, tamanio: 15 })}`);
    const esCliente = tipo === 'clientes';
    $('[data-tabla]', main).innerHTML = r.contenido.length ? `<table>
      <thead><tr><th>${esCliente ? 'Cliente' : 'Empresa'}</th><th>${esCliente ? 'Documento' : 'NIT'}</th><th>Contacto</th><th>Ciudad</th><th>Estado</th><th></th></tr></thead>
      <tbody>${r.contenido.map((x) => `<tr>
        <td class="cell-main">${esc(esCliente ? `${x.usuario.nombres} ${x.usuario.apellidos}` : x.razonSocial)}${esCliente ? '' : `<div class="cell-sub">${esc(x.usuario.nombres)} ${esc(x.usuario.apellidos)}</div>`}</td>
        <td class="mono small">${esc(esCliente ? `${x.tipoDocumento} ${x.numeroDocumento}` : x.nit)}</td>
        <td>${esc(x.usuario.email)}<div class="cell-sub">${esc(x.usuario.telefono || '')}</div></td>
        <td>${esc(x.ciudad)}</td>
        <td>${x.usuario.activo ? badge('APROBADO', 'Activo') : badge('CANCELADA', 'Inactivo')}</td>
        <td><div class="actions">${x.usuario.activo
          ? `<button class="btn btn-danger btn-sm" data-desactivar="${x.id}" data-nombre="${esc(esCliente ? x.usuario.nombres : x.razonSocial)}">Desactivar</button>`
          : `<button class="btn btn-ok btn-sm" data-activar="${x.id}">Activar</button>`}</div></td></tr>`).join('')}</tbody></table>`
      : vacio('Sin usuarios', '');
    paginador($('[data-pager]', main), r.pagina, r.totalPaginas, (p) => { pagina = p; cargar(); });
  }

  main.addEventListener('click', async (e) => {
    const t = e.target.closest('[data-tipo]');
    if (t) {
      tipo = t.dataset.tipo;
      pagina = 0;
      $$('[data-tipo]', main).forEach((b) => b.setAttribute('aria-selected', String(b === t)));
      cargar();
    }
    if (e.target.closest('[data-nuevo-proveedor]')) {
      formularioModal({
        titulo: 'Registrar proveedor', ancho: true, textoBoton: 'Registrar', campos: CAMPOS_PROVEEDOR,
        enviar: async (d) => {
          if (!d.telefono) delete d.telefono;
          const p = await post('/proveedores', d);
          toast(`${p.razonSocial} registrado`, 'ok');
          tipo = 'proveedores';
          $$('[data-tipo]', main).forEach((b) => b.setAttribute('aria-selected', String(b.dataset.tipo === tipo)));
          cargar();
        },
      });
    }
    const des = e.target.closest('[data-desactivar]');
    const act = e.target.closest('[data-activar]');
    try {
      if (des) {
        if (!await confirmar('Desactivar usuario', `<b>${esc(des.dataset.nombre)}</b> no podrá iniciar sesión. Su historial se conserva.`, { aceptar: 'Desactivar', peligro: true })) return;
        cargando(des, true);
        await del(`/${tipo}/${des.dataset.desactivar}`);
        toast('Usuario desactivado', 'ok');
        cargar();
      }
      if (act) {
        cargando(act, true);
        await patch(`/${tipo}/${act.dataset.activar}/activar`);
        toast('Usuario activado', 'ok');
        cargar();
      }
    } catch (error) { toastError(error); cargar(); }
  });

  await cargar();
}

// ---------- Categorías ----------

async function categorias(main) {
  main.innerHTML = `${encabezado('Categorías', 'Organizan el catálogo. Desactivar una categoría la oculta para nuevas herramientas.',
    `<button class="btn btn-primary" data-nueva>${icono('plus')}Nueva categoría</button>`)}<div class="table-wrap" data-tabla></div>`;
  let lista = [];
  async function cargar() {
    lista = await get('/categorias?incluirInactivas=true');
    $('[data-tabla]', main).innerHTML = `<table><thead><tr><th>Categoría</th><th>Descripción</th><th>Estado</th><th></th></tr></thead>
      <tbody>${lista.map((c) => `<tr><td class="cell-main">${esc(c.nombre)}</td><td class="small muted">${esc(c.descripcion || '')}</td>
        <td>${c.activa ? badge('ACTIVA') : badge('INACTIVA')}</td>
        <td><div class="actions"><button class="btn btn-ghost btn-sm" data-editar="${c.id}">${icono('edit')}Editar</button>
          ${c.activa ? `<button class="btn btn-danger btn-sm" data-desactivar="${c.id}">Desactivar</button>` : `<button class="btn btn-ok btn-sm" data-activar="${c.id}">Activar</button>`}</div></td></tr>`).join('')}</tbody></table>`;
  }
  const campos = (c = {}) => `<label class="field"><span>Nombre</span><input class="input" name="nombre" required maxlength="80" value="${esc(c.nombre || '')}"></label>
    <label class="field"><span>Descripción</span><textarea class="textarea" name="descripcion" maxlength="255">${esc(c.descripcion || '')}</textarea></label>`;

  main.addEventListener('click', async (e) => {
    if (e.target.closest('[data-nueva]')) {
      formularioModal({ titulo: 'Nueva categoría', campos: campos(), enviar: async (d) => { await post('/categorias', d); toast('Categoría creada', 'ok'); cargar(); } });
    }
    const ed = e.target.closest('[data-editar]');
    if (ed) {
      const c = lista.find((x) => x.id === Number(ed.dataset.editar));
      formularioModal({ titulo: `Editar ${c.nombre}`, campos: campos(c), enviar: async (d) => { await put(`/categorias/${c.id}`, d); toast('Categoría actualizada', 'ok'); cargar(); } });
    }
    try {
      const des = e.target.closest('[data-desactivar]');
      if (des) { await del(`/categorias/${des.dataset.desactivar}`); toast('Categoría desactivada', 'ok'); cargar(); }
      const act = e.target.closest('[data-activar]');
      if (act) { await patch(`/categorias/${act.dataset.activar}/activar`); toast('Categoría activada', 'ok'); cargar(); }
    } catch (error) { toastError(error); }
  });
  await cargar();
}

export const seccionesAdmin = [
  { id: 'inicio', titulo: 'Panel', icono: 'chart', pintar: tablero },
  { id: 'usuarios', titulo: 'Usuarios', icono: 'users', pintar: usuarios },
  {
    id: 'reservas', titulo: 'Alquileres', icono: 'calendar',
    pintar: (m) => listaReservas(m, { titulo: 'Historial de alquileres', subtitulo: 'Todas las reservas de la plataforma, en cualquier estado.' }),
  },
  { id: 'pagos', titulo: 'Pagos', icono: 'card', pintar: (m) => seccionPagos(m, 'Supervisión de todos los intentos de pago.') },
  { id: 'facturas', titulo: 'Facturación', icono: 'file', pintar: (m) => seccionFacturas(m, 'Todas las facturas emitidas.') },
  {
    id: 'devoluciones', titulo: 'Devoluciones', icono: 'undo', pintar: seccionDevoluciones,
    contar: async () => (await get('/devoluciones?validada=false&tamanio=1')).totalElementos,
  },
  {
    id: 'incidencias', titulo: 'Incidencias', icono: 'alert', pintar: seccionIncidencias,
    contar: async () => (await get('/danos?estado=ABIERTO&tamanio=1')).totalElementos,
  },
  { id: 'inventario', titulo: 'Herramientas', icono: 'box', pintar: seccionInventario },
  { id: 'categorias', titulo: 'Categorías', icono: 'tag', pintar: categorias },
];
