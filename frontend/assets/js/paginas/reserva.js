// Checkout: revisa el carrito, cotiza en el servidor y envía la solicitud de reserva (queda PENDIENTE).

import { post } from '../api.js';
import { sesion } from '../sesion.js';
import { carrito } from '../carrito.js';
import { icono } from '../iconos.js';
import {
  montarLayout, $, esc, cop, fecha, visual, debounce, vacio, cargando, toastError, hoy,
} from '../ui.js';

montarLayout();

const contenido = $('[data-contenido]');
let cotizacionValida = false;

function pintar() {
  const c = carrito.obtener();
  if (!c.items.length) {
    contenido.innerHTML = vacio('Tu reserva está vacía', 'Explora el catálogo y agrega las herramientas que necesitas.', 'carretilla',
      '<a class="btn btn-primary" href="index.html#catalogo" style="margin-top:1rem">Ir al catálogo</a>');
    return;
  }
  const rol = sesion.rol();
  contenido.innerHTML = `
    <div class="page-head"><div><span class="eyebrow">Paso 1 de 3 · Solicitud</span><h1 style="margin-top:.5rem">Tu reserva</h1>
      <p>Con <b>${esc(c.items[0].proveedor)}</b> · ${esc(c.items[0].ciudad)}</p></div>
      <a class="btn btn-ghost" href="index.html#catalogo">${icono('plus')}Agregar más herramientas</a></div>
    <div class="product" style="grid-template-columns:1.3fr .7fr">
      <div class="stack">
        <div class="box">
          <div class="box-head"><h3>Herramientas</h3><button class="btn-text small" data-vaciar>Vaciar</button></div>
          <div class="stack" style="gap:.6rem" data-lineas>${c.items.map((i) => `
            <div class="line-item" style="grid-template-columns:80px 1fr auto auto">
              <a class="mini" style="width:80px;height:62px" href="herramienta.html?id=${i.herramientaId}">${visual(i.imagen, i.categoria, i.nombre)}</a>
              <div><b>${esc(i.nombre)}</b><small>${esc(i.categoria)} · ${cop(i.precioPorDia)} / día</small>
                <div data-aviso="${i.herramientaId}"></div></div>
              <div class="qty"><button data-menos="${i.herramientaId}" aria-label="Menos">−</button><output>${i.cantidad}</output><button data-mas="${i.herramientaId}" aria-label="Más">+</button></div>
              <div class="stack" style="gap:.2rem;align-items:end"><b class="num" data-subtotal="${i.herramientaId}">—</b>
                <button class="btn-text small" data-quitar="${i.herramientaId}">Quitar</button></div>
            </div>`).join('')}</div>
        </div>
        <div class="box stack">
          <h3>Fechas y notas</h3>
          <div class="form-grid">
            <label class="field"><span>Desde</span><input class="input" type="date" data-desde min="${hoy()}" value="${c.fechaInicio}"></label>
            <label class="field"><span>Hasta</span><input class="input" type="date" data-hasta min="${c.fechaInicio}" value="${c.fechaFin}"></label>
            <label class="field full"><span>Notas para el proveedor (opcional)</span>
              <textarea class="textarea" data-notas maxlength="500" placeholder="Ej: entregar en la portería de la obra, horario de recepción…"></textarea></label>
          </div>
          <p class="small muted">Los días se cuentan completos: del ${fecha(c.fechaInicio)} al ${fecha(c.fechaFin)} incluidos.</p>
        </div>
      </div>
      <aside class="buy-box">
        <div class="box stack" style="gap:.6rem">
          <h3>Resumen</h3>
          <div data-cotizacion><div class="quote-line"><span>Calculando…</span><span class="spinner"></span></div></div>
          ${rol === 'CLIENTE' ? `<button class="btn btn-primary btn-lg btn-block" data-enviar>Solicitar reserva ${icono('arrow')}</button>`
            : rol ? '<div class="notice warn">Las reservas se hacen con una cuenta de cliente.</div>'
              : `<a class="btn btn-primary btn-lg btn-block" href="ingresar.html?volver=reserva.html">Ingresa para solicitar ${icono('arrow')}</a>
                 <a class="btn btn-ghost btn-block" href="ingresar.html?volver=reserva.html#registro">Crear cuenta gratis</a>`}
        </div>
        <div class="box stack" style="gap:.8rem">
          <h3>¿Qué sigue?</h3>
          <div class="timeline">
            <div class="tl-step now">Solicitud</div><div class="tl-step">Aprobación</div><div class="tl-step">Pago</div><div class="tl-step">Entrega</div>
          </div>
          <p class="small muted">Tu solicitud queda <b>pendiente</b>. Cuando el proveedor la apruebe, las unidades quedan apartadas y tienes <b>24 horas</b> para pagar en línea. La factura se genera automáticamente.</p>
        </div>
      </aside>
    </div>`;
  cotizar();
}

const cotizar = debounce(async () => {
  const caja = $('[data-cotizacion]');
  if (!caja) return;
  cotizacionValida = false;
  try {
    const q = await post('/reservas/cotizacion', carrito.pedido());
    let faltan = false;
    q.lineas.forEach((l) => {
      const sub = $(`[data-subtotal="${l.herramientaId}"]`);
      if (sub) sub.textContent = cop(l.subtotal);
      const aviso = $(`[data-aviso="${l.herramientaId}"]`);
      if (aviso) {
        faltan ||= l.cantidad > l.unidadesDisponibles;
        aviso.innerHTML = l.cantidad > l.unidadesDisponibles
          ? `<span class="badge bad" style="margin-top:.35rem">Solo ${l.unidadesDisponibles} libre(s) en esas fechas</span>`
          : `<span class="badge ok" style="margin-top:.35rem">${l.unidadesDisponibles} libre(s)</span>`;
      }
    });
    cotizacionValida = !faltan;
    caja.innerHTML = `
      <div class="quote-line"><span>${fecha(q.fechaInicio)} → ${fecha(q.fechaFin)}</span><span>${q.dias} ${q.dias === 1 ? 'día' : 'días'}</span></div>
      <div class="quote-line"><span>Subtotal</span><span class="num">${cop(q.subtotal)}</span></div>
      <div class="quote-line"><span>IVA (19 %)</span><span class="num">${cop(q.iva)}</span></div>
      <div class="quote-line total"><span>Total</span><span class="num">${cop(q.total)}</span></div>
      ${faltan ? `<div class="notice bad" style="margin-top:.6rem">${icono('alert')}<div>Ajusta las cantidades o las fechas: no hay suficientes unidades.</div></div>` : ''}`;
  } catch (error) {
    caja.innerHTML = `<div class="notice bad">${icono('alertCircle')}<div>${esc(error.message)}</div></div>`;
  }
  const enviar = $('[data-enviar]');
  if (enviar) enviar.disabled = !cotizacionValida;
}, 250);

async function enviar(boton) {
  cargando(boton, true, 'Enviando…');
  try {
    const reserva = await post('/reservas', carrito.pedido($('[data-notas]').value.trim()));
    carrito.vaciar();
    contenido.innerHTML = `
      <div class="box" style="max-width:640px;margin:2rem auto;text-align:center;padding:2.5rem">
        <div class="logo-mark" style="width:64px;height:64px;margin:0 auto 1.2rem;border-radius:16px">${icono('check')}</div>
        <span class="eyebrow">Solicitud enviada</span>
        <h1 style="margin:.6rem 0">${esc(reserva.codigo)}</h1>
        <p class="muted">Le avisamos a <b>${esc(reserva.proveedor.razonSocial)}</b>. Cuando apruebe tu reserva podrás pagarla desde tu panel.</p>
        <div class="timeline" style="margin:2rem 0"><div class="tl-step done now">Solicitud</div><div class="tl-step">Aprobación</div><div class="tl-step">Pago</div><div class="tl-step">Entrega</div></div>
        <div class="quote-line total" style="border:0"><span>Total</span><span class="num">${cop(reserva.total)}</span></div>
        <div class="row" style="justify-content:center;margin-top:1.5rem">
          <a class="btn btn-primary" href="panel.html#reservas">Ver mis reservas</a>
          <a class="btn btn-ghost" href="index.html#catalogo">Seguir explorando</a></div>
      </div>`;
  } catch (error) {
    toastError(error);
    cargando(boton, false);
  }
}

contenido.addEventListener('click', (e) => {
  const b = e.target.closest('button');
  if (!b) return;
  if (b.dataset.mas) carrito.cambiarCantidad(Number(b.dataset.mas), 1);
  if (b.dataset.menos) carrito.cambiarCantidad(Number(b.dataset.menos), -1);
  if (b.dataset.quitar) carrito.quitar(Number(b.dataset.quitar));
  if (b.hasAttribute('data-vaciar')) carrito.vaciar();
  if (b.hasAttribute('data-enviar')) enviar(b);
});
contenido.addEventListener('change', (e) => {
  if (e.target.matches('[data-desde], [data-hasta]')) carrito.fijarFechas($('[data-desde]').value, $('[data-hasta]').value);
});
window.addEventListener('carrito', () => {
  if (!$('[data-lineas]')) return;
  const notas = $('[data-notas]')?.value;
  pintar();
  if (notas) $('[data-notas]').value = notas;
});

pintar();
