// Gráficas en SVG/HTML puro (sin librerías). Todas son de una sola serie, así que usan un solo color
// (--chart) y no necesitan leyenda: el título de la caja dice qué se grafica.
// Especificaciones: columnas de máximo 24px con 4px redondeados en el extremo y planas en la base,
// cuadrícula de 1px, etiqueta solo en el valor más alto, tooltip al pasar el mouse y vista de tabla.

import { esc } from './ui.js';

let tip;
function tooltip() {
  if (!tip) {
    tip = document.createElement('div');
    tip.className = 'chart-tip';
    tip.setAttribute('role', 'status');
    document.body.append(tip);
  }
  return tip;
}

function mostrarTip(e, html) {
  const t = tooltip();
  t.innerHTML = html;
  t.classList.add('on');
  const x = Math.min(e.clientX + 14, window.innerWidth - t.offsetWidth - 8);
  t.style.left = `${x}px`;
  t.style.top = `${e.clientY - t.offsetHeight - 12}px`;
}

const ocultarTip = () => tip?.classList.remove('on');

/** Valores "redondos" para las marcas del eje Y. */
function marcas(maximo, cantidad = 4) {
  if (maximo <= 0) return [0];
  const paso = 10 ** Math.floor(Math.log10(maximo / cantidad));
  const multiplo = [1, 2, 2.5, 5, 10].find((m) => maximo / (paso * m) <= cantidad) || 10;
  const salto = paso * multiplo;
  return Array.from({ length: Math.ceil(maximo / salto) + 1 }, (_, i) => i * salto);
}

/**
 * Gráfica de columnas. datos: [{ etiqueta, valor, detalle? }].
 * formatoEje: texto corto para el eje; formato: texto completo para tooltip y tabla.
 */
export function columnas(contenedor, datos, { formato, formatoEje = formato, nombreValor = 'Valor' }) {
  if (!datos.length) {
    contenedor.innerHTML = '<p class="muted small">Sin datos en el periodo.</p>';
    return;
  }
  const ancho = 640;
  const alto = 240;
  const m = { arriba: 22, derecha: 8, abajo: 26, izquierda: 64 };
  const ticks = marcas(Math.max(...datos.map((d) => d.valor)));
  const tope = ticks[ticks.length - 1] || 1;
  const banda = (ancho - m.izquierda - m.derecha) / datos.length;
  const barra = Math.min(24, banda * 0.55);
  const y = (v) => m.arriba + (alto - m.arriba - m.abajo) * (1 - v / tope);
  const base = y(0);
  const iMax = datos.reduce((im, d, i) => (d.valor > datos[im].valor ? i : im), 0);

  const rejilla = ticks.map((t) => `
    <line class="grid-line" x1="${m.izquierda}" x2="${ancho - m.derecha}" y1="${y(t)}" y2="${y(t)}"/>
    <text x="${m.izquierda - 8}" y="${y(t) + 4}" text-anchor="end">${esc(formatoEje(t))}</text>`).join('');

  const barras = datos.map((d, i) => {
    const cx = m.izquierda + banda * i + banda / 2;
    const x = cx - barra / 2;
    const top = y(d.valor);
    const h = base - top;
    const r = Math.min(4, h); // 4px redondeados arriba, base recta
    const camino = h <= 0 ? '' : `M${x},${base} V${top + r} Q${x},${top} ${x + r},${top} H${x + barra - r} Q${x + barra},${top} ${x + barra},${top + r} V${base} Z`;
    return `
      <path class="bar" d="${camino}"/>
      ${i === iMax ? `<text class="value" x="${cx}" y="${top - 7}" text-anchor="middle">${esc(formatoEje(d.valor))}</text>` : ''}
      <text x="${cx}" y="${alto - 8}" text-anchor="middle">${esc(d.etiqueta)}</text>
      <rect class="hit" x="${m.izquierda + banda * i}" y="${m.arriba}" width="${banda}" height="${base - m.arriba}" data-i="${i}"/>`;
  }).join('');

  contenedor.innerHTML = `
    <svg class="chart" viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Gráfica de columnas">${rejilla}${barras}</svg>
    <details class="small" style="margin-top:.5rem"><summary class="muted" style="cursor:pointer">Ver como tabla</summary>
      <div class="table-wrap" style="margin-top:.5rem"><table><thead><tr><th>Periodo</th><th class="right">${esc(nombreValor)}</th></tr></thead>
      <tbody>${datos.map((d) => `<tr><td>${esc(d.etiqueta)}</td><td class="right num">${esc(formato(d.valor))}</td></tr>`).join('')}</tbody></table></div>
    </details>`;

  contenedor.querySelectorAll('.hit').forEach((r) => {
    const d = datos[Number(r.dataset.i)];
    r.addEventListener('mousemove', (e) => mostrarTip(e, `<b>${esc(formato(d.valor))}</b>${esc(d.etiqueta)}${d.detalle ? ` · ${esc(d.detalle)}` : ''}`));
    r.addEventListener('mouseleave', ocultarTip);
  });
}

/** Barras horizontales (rankings). datos: [{ etiqueta, valor, sub? }]. */
export function barrasH(contenedor, datos, { formato, vacio = 'Sin datos en el periodo.' }) {
  if (!datos.length) {
    contenedor.innerHTML = `<p class="muted small">${esc(vacio)}</p>`;
    return;
  }
  const max = Math.max(...datos.map((d) => d.valor)) || 1;
  contenedor.innerHTML = `<div class="hbars">${datos.map((d) => `
    <div class="hbar" title="${esc(d.etiqueta)}: ${esc(formato(d.valor))}">
      <span>${esc(d.etiqueta)}${d.sub ? ` <span class="muted small">· ${esc(d.sub)}</span>` : ''}</span>
      <span>${esc(formato(d.valor))}</span>
      <div class="track"><div class="fill" style="width:${Math.max(2, (d.valor / max) * 100)}%"></div></div>
    </div>`).join('')}</div>`;
}
