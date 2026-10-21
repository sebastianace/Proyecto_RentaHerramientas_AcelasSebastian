// Panel del PROVEEDOR: su negocio (reportes), solicitudes, reservas con entrega y devolución,
// inventario (CRUD de herramientas con fotos), devoluciones, incidencias, pagos, facturas y perfil.

import { api, get, post, put, patch, query, urlMedia, esImagenReal } from '../api.js';
import { sesion } from '../sesion.js';
import { icono } from '../iconos.js';
import {
  $, esc, cop, visual, formularioModal, modal, confirmar, toast, toastError, vacio,
} from '../ui.js';
import { encabezado, listaReservas, seccionPagos, seccionFacturas, refrescar } from './comunes.js';
import { tablero } from './tablero.js';
import { seccionDevoluciones, seccionIncidencias } from './devoluciones.js';

// ---------- Inventario ----------

let categorias = null;
async function opcionesCategoria(seleccionada) {
  categorias ||= await get('/categorias');
  return categorias.filter((c) => c.activa !== false)
    .map((c) => `<option value="${c.id}" ${c.id === seleccionada ? 'selected' : ''}>${esc(c.nombre)}</option>`).join('');
}

async function camposHerramienta(h = null) {
  return `<div class="form-grid">
    <label class="field full"><span>Nombre</span><input class="input" name="nombre" maxlength="150" required value="${esc(h?.nombre || '')}" placeholder="Rotomartillo SDS Plus 800W"></label>
    <label class="field"><span>Categoría</span><select class="select" name="categoriaId">${await opcionesCategoria(h?.categoria.id)}</select></label>
    <label class="field"><span>Precio por día (COP)</span><input class="input" type="number" name="precioPorDia" min="1000" step="500" required value="${h ? Number(h.precioPorDia) : ''}"></label>
    <label class="field"><span>Marca</span><input class="input" name="marca" maxlength="80" value="${esc(h?.marca || '')}"></label>
    <label class="field"><span>Modelo</span><input class="input" name="modelo" maxlength="80" value="${esc(h?.modelo || '')}"></label>
    <label class="field"><span>Unidades en stock</span><input class="input" type="number" name="stockTotal" min="0" required value="${h?.stockTotal ?? 1}"></label>
    ${h ? `<label class="field"><span>Fuera de servicio</span><input class="input" type="number" name="unidadesFueraServicio" min="0" value="${h.unidadesFueraServicio}"></label>` : ''}
    <label class="field full"><span>Descripción</span><textarea class="textarea" name="descripcion" maxlength="2000" placeholder="Estado, accesorios incluidos, recomendaciones de uso…">${esc(h?.descripcion || '')}</textarea></label>
  </div>`;
}

function numeros(d) {
  return {
    ...d, categoriaId: Number(d.categoriaId), precioPorDia: Number(d.precioPorDia), stockTotal: Number(d.stockTotal),
    ...(d.unidadesFueraServicio !== undefined ? { unidadesFueraServicio: Number(d.unidadesFueraServicio) } : {}),
  };
}

function modalImagenes(h, alCambiar) {
  const m = modal({ titulo: `Fotos · ${esc(h.nombre)}`, ancho: true, cuerpo: '<div data-fotos></div>', alCerrar: alCambiar });
  const caja = $('[data-fotos]', m.el);

  function pintar() {
    const reales = h.imagenes.filter((i) => esImagenReal(i.url));
    caja.innerHTML = `
      <p class="muted small">Hasta 8 fotos JPG, PNG o WEBP de máximo 5 MB. La foto principal es la que se ve en el catálogo.</p>
      <div class="img-grid" style="margin:.8rem 0">${reales.map((i) => `
        <div class="img-tile ${i.principal ? 'principal' : ''}"><img src="${esc(urlMedia(i.url))}" alt="">
          <div class="actions">${i.principal ? '<button disabled>Principal</button>' : `<button data-principal="${i.id}">Hacer principal</button>`}
            <button data-borrar="${i.id}" aria-label="Eliminar foto">✕</button></div></div>`).join('')}</div>
      ${reales.length ? '' : '<div class="notice">Esta herramienta aún no tiene fotos reales; en el catálogo se muestra su ilustración.</div>'}
      <label class="drop" data-drop style="display:block;margin-top:.8rem">${icono('image', 'ico')} <b>Haz clic o arrastra una foto aquí</b>
        <input type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" data-archivo></label>`;
  }

  async function subir(archivo) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    const principal = !h.imagenes.some((i) => esImagenReal(i.url));
    caja.querySelector('[data-drop]').innerHTML = '<span class="spinner"></span> Subiendo…';
    try {
      h = await api(`/herramientas/${h.id}/imagenes${query({ principal })}`, { metodo: 'POST', formulario: datos });
      toast('Foto subida', 'ok');
    } catch (error) { toastError(error); }
    pintar();
  }

  caja.addEventListener('change', (e) => { if (e.target.matches('[data-archivo]') && e.target.files[0]) subir(e.target.files[0]); });
  caja.addEventListener('dragover', (e) => { e.preventDefault(); caja.querySelector('[data-drop]')?.classList.add('over'); });
  caja.addEventListener('dragleave', () => caja.querySelector('[data-drop]')?.classList.remove('over'));
  caja.addEventListener('drop', (e) => { e.preventDefault(); if (e.dataTransfer.files[0]) subir(e.dataTransfer.files[0]); });
  caja.addEventListener('click', async (e) => {
    const p = e.target.closest('[data-principal]');
    const b = e.target.closest('[data-borrar]');
    try {
      if (p) h = await patch(`/herramientas/${h.id}/imagenes/${p.dataset.principal}/principal`);
      if (b && await confirmar('Eliminar foto', '¿Eliminar esta foto? El archivo se borra del servidor.', { aceptar: 'Eliminar', peligro: true })) {
        h = await api(`/herramientas/${h.id}/imagenes/${b.dataset.borrar}`, { metodo: 'DELETE' });
      }
      if (p || b) pintar();
    } catch (error) { toastError(error); }
  });
  pintar();
}

export async function seccionInventario(main) {
  const esAdmin = sesion.rol() === 'ADMIN';
  const proveedorId = sesion.perfil()?.proveedorId;
  main.innerHTML = `${encabezado(esAdmin ? 'Herramientas' : 'Mi inventario',
    esAdmin ? 'Todas las herramientas registradas en la plataforma.' : 'Tus herramientas: precios, unidades, estado y fotos.',
    esAdmin ? '' : `<button class="btn btn-primary" data-nueva>${icono('plus')}Nueva herramienta</button>`)}
    <div class="table-wrap" data-tabla></div>`;

  let herramientas = [];
  async function cargar() {
    const r = await get(`/herramientas/inventario${query({ tamanio: 100 })}`);
    herramientas = r.contenido;
    $('[data-tabla]', main).innerHTML = herramientas.length ? `<table>
      <thead><tr><th></th><th>Herramienta</th>${esAdmin ? '<th>Proveedor</th>' : ''}<th class="right">Precio / día</th><th class="right">Operativas</th><th>Estado</th><th></th></tr></thead>
      <tbody>${herramientas.map((h) => `<tr>
        <td style="width:76px"><div class="line-item" style="padding:0;border:0;display:block;background:none"><div class="mini">${visual(h.imagenPrincipal, h.categoria.nombre, h.nombre)}</div></div></td>
        <td><a class="cell-main" href="herramienta.html?id=${h.id}">${esc(h.nombre)}</a><div class="cell-sub">${esc(h.categoria.nombre)} · ${esc(h.marca || '')} ${esc(h.modelo || '')}</div></td>
        ${esAdmin ? `<td class="small">${esc(h.proveedor.razonSocial)}</td>` : ''}
        <td class="right num cell-main">${cop(h.precioPorDia)}</td>
        <td class="right">${h.unidadesOperativas}/${h.stockTotal}${h.unidadesFueraServicio ? `<div class="cell-sub">${h.unidadesFueraServicio} en reparación</div>` : ''}</td>
        <td><select class="select" style="width:auto;padding:.35rem .5rem;font-size:.82rem" data-estado="${h.id}" aria-label="Estado de ${esc(h.nombre)}">
          ${['ACTIVA', 'EN_MANTENIMIENTO', 'INACTIVA'].map((e) => `<option value="${e}" ${e === h.estado ? 'selected' : ''}>${e === 'EN_MANTENIMIENTO' ? 'Mantenimiento' : e.charAt(0) + e.slice(1).toLowerCase()}</option>`).join('')}</select></td>
        <td><div class="actions"><button class="btn btn-ghost btn-sm" data-fotos="${h.id}">${icono('image')}Fotos</button>
          <button class="btn btn-ghost btn-sm" data-editar="${h.id}">${icono('edit')}Editar</button></div></td></tr>`).join('')}</tbody></table>`
      : vacio('Tu inventario está vacío', 'Registra tu primera herramienta para empezar a recibir reservas.', 'taladro');
  }

  main.addEventListener('click', async (e) => {
    if (e.target.closest('[data-nueva]')) {
      formularioModal({
        titulo: 'Nueva herramienta', ancho: true, textoBoton: 'Registrar herramienta', campos: await camposHerramienta(),
        enviar: async (d) => {
          const h = await post('/herramientas', { ...numeros(d), proveedorId });
          toast(`${h.nombre} registrada. Ahora agrégale fotos.`, 'ok');
          await cargar();
          modalImagenes(h, cargar);
        },
      });
    }
    const editar = e.target.closest('[data-editar]');
    if (editar) {
      const h = herramientas.find((x) => x.id === Number(editar.dataset.editar));
      formularioModal({
        titulo: `Editar ${h.nombre}`, ancho: true, campos: await camposHerramienta(h),
        enviar: async (d) => { await put(`/herramientas/${h.id}`, numeros(d)); toast('Cambios guardados', 'ok'); cargar(); },
      });
    }
    const fotos = e.target.closest('[data-fotos]');
    if (fotos) modalImagenes(herramientas.find((x) => x.id === Number(fotos.dataset.fotos)), cargar);
  });
  main.addEventListener('change', async (e) => {
    const sel = e.target.closest('[data-estado]');
    if (!sel) return;
    try {
      await patch(`/herramientas/${sel.dataset.estado}/estado`, { estado: sel.value });
      toast(`Estado actualizado: ${sel.selectedOptions[0].textContent}`, 'ok');
    } catch (error) { toastError(error); cargar(); }
  });

  await cargar();
}

// ---------- Perfil ----------

async function perfil(main) {
  const id = sesion.perfil()?.proveedorId;
  const p = await get(`/proveedores/${id}`);
  main.innerHTML = `${encabezado('Mi empresa', 'Estos datos aparecen en tus facturas como emisor.',
    `<button class="btn btn-primary" data-editar>${icono('edit')}Editar datos</button>`)}
    <div class="box"><dl class="specs">
      <div><dt>Razón social</dt><dd>${esc(p.razonSocial)}</dd></div><div><dt>NIT</dt><dd>${esc(p.nit)}</dd></div>
      <div><dt>Contacto</dt><dd>${esc(p.usuario.nombres)} ${esc(p.usuario.apellidos)}</dd></div><div><dt>Correo</dt><dd>${esc(p.usuario.email)}</dd></div>
      <div><dt>Teléfono</dt><dd>${esc(p.usuario.telefono || '—')}</dd></div><div><dt>Ciudad</dt><dd>${esc(p.ciudad)}</dd></div>
      <div><dt>Dirección</dt><dd>${esc(p.direccion)}</dd></div><div><dt>Descripción</dt><dd>${esc(p.descripcion || '—')}</dd></div>
    </dl></div>`;
  $('[data-editar]', main).addEventListener('click', () => formularioModal({
    titulo: 'Editar datos de la empresa', ancho: true,
    campos: `<div class="form-grid">
      <label class="field full"><span>Razón social</span><input class="input" name="razonSocial" data-solo="empresa" value="${esc(p.razonSocial)}" required></label>
      <label class="field"><span>Nombres del contacto</span><input class="input" name="nombres" data-solo="letras" value="${esc(p.usuario.nombres)}" required></label>
      <label class="field"><span>Apellidos del contacto</span><input class="input" name="apellidos" data-solo="letras" value="${esc(p.usuario.apellidos)}" required></label>
      <label class="field"><span>Teléfono</span><input class="input" name="telefono" data-solo="telefono" inputmode="numeric" maxlength="10" placeholder="3001234567" value="${esc(p.usuario.telefono || '')}"></label>
      <label class="field"><span>Ciudad</span><input class="input" name="ciudad" data-solo="letras" value="${esc(p.ciudad)}" required></label>
      <label class="field full"><span>Dirección</span><input class="input" name="direccion" value="${esc(p.direccion)}" required></label>
      <label class="field full"><span>Descripción</span><textarea class="textarea" name="descripcion">${esc(p.descripcion || '')}</textarea></label></div>`,
    enviar: async (d) => { await put(`/proveedores/${id}`, d); toast('Datos actualizados', 'ok'); refrescar(); },
  }));
}

const contarPendientes = async () => (await get('/reservas?estado=PENDIENTE&tamanio=1')).totalElementos;

export const seccionesProveedor = [
  { id: 'inicio', titulo: 'Mi negocio', icono: 'chart', pintar: tablero },
  {
    id: 'solicitudes', titulo: 'Solicitudes', icono: 'inbox', contar: contarPendientes,
    pintar: (m) => listaReservas(m, { titulo: 'Solicitudes por aprobar', subtitulo: 'Al aprobar, las unidades quedan apartadas y el cliente tiene 24 h para pagar.', soloEstado: 'PENDIENTE' }),
  },
  {
    id: 'reservas', titulo: 'Reservas', icono: 'calendar',
    pintar: (m) => listaReservas(m, { titulo: 'Reservas', subtitulo: 'Registra la entrega de las pagadas y la devolución de las que están en alquiler.' }),
  },
  { id: 'inventario', titulo: 'Inventario', icono: 'box', pintar: seccionInventario },
  { id: 'devoluciones', titulo: 'Devoluciones', icono: 'undo', pintar: seccionDevoluciones },
  { id: 'incidencias', titulo: 'Daños', icono: 'alert', pintar: seccionIncidencias },
  { id: 'facturas', titulo: 'Facturación', icono: 'file', pintar: (m) => seccionFacturas(m, 'Facturas emitidas a tus clientes.') },
  { id: 'pagos', titulo: 'Pagos recibidos', icono: 'card', pintar: (m) => seccionPagos(m, 'Pagos de tus reservas, aprobados y rechazados.') },
  { id: 'perfil', titulo: 'Mi empresa', icono: 'user', pintar: perfil },
];
