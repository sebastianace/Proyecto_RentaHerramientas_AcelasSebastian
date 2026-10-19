// Íconos e ilustraciones como un sprite SVG que se inyecta una sola vez en la página.
// Las ilustraciones de herramientas son dibujos propios (no fotos de relleno): heredan el color
// del texto (currentColor) y el acento del tema (var(--accent)), así funcionan en tema claro y oscuro.

const T = 'fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"';
const A = 'fill="var(--accent)"';
const AS = 'fill="none" stroke="var(--accent)" stroke-width="5" stroke-linecap="round"';

const ILUSTRACIONES = {
  taladro: `<g ${T}><path d="M18 30h66a14 14 0 0 1 14 14v6H18z"/><path d="M98 38h20M118 34v12M118 40h16"/><path d="M40 50l-6 36a6 6 0 0 0 6 7h14a6 6 0 0 0 6-6l4-37"/></g><rect x="22" y="34" width="26" height="8" rx="3" ${A}/>`,
  pulidora: `<g ${T}><path d="M20 58h60a10 10 0 0 0 10-10v-6a10 10 0 0 0-10-10H20z"/><path d="M90 48h20"/><path d="M52 58l-6 30h14l6-30"/></g><ellipse cx="116" cy="48" rx="6" ry="26" ${AS}/>`,
  sierra: `<g ${T}><circle cx="70" cy="50" r="34"/><circle cx="70" cy="50" r="8"/><path d="M70 12v12M70 76v12M32 50h12M96 50h12M43 23l8 8M89 69l8 8M97 23l-8 8M51 69l-8 8"/></g><circle cx="70" cy="50" r="4" ${A}/>`,
  andamio: `<g ${T}><path d="M28 94V10M112 94V10M28 36h84M28 66h84"/></g><g stroke="var(--accent)" stroke-width="4" stroke-linecap="round"><path d="M28 36l84 30M28 66l84-30"/></g>`,
  mezcladora: `<g ${T}><path d="M38 20l50-4 14 34a30 30 0 0 1-56 6z"/><path d="M46 76l-12 18M92 76l12 18"/><circle cx="34" cy="94" r="4"/><circle cx="104" cy="94" r="4"/></g><path d="M60 34l10 34" ${AS}/>`,
  compactadora: `<g ${T}><rect x="22" y="72" width="70" height="12" rx="3"/><rect x="34" y="44" width="44" height="28" rx="5"/><path d="M78 50l34-30M104 14l14 12"/></g><rect x="40" y="52" width="20" height="10" rx="2" ${A}/>`,
  generador: `<g ${T}><rect x="18" y="24" width="104" height="54" rx="6"/><path d="M30 78v12M110 78v12M18 90h104"/></g><path d="M74 32l-14 24h12l-6 18 18-26H71z" ${A}/>`,
  compresor: `<g ${T}><rect x="14" y="40" width="90" height="34" rx="17"/><path d="M104 56h22M28 74l-4 14M90 74l4 14"/><circle cx="52" cy="26" r="12"/></g><path d="M52 26l6-6" stroke="var(--accent)" stroke-width="4" stroke-linecap="round"/>`,
  hidrolavadora: `<g ${T}><rect x="16" y="32" width="44" height="44" rx="8"/><circle cx="26" cy="84" r="7"/><circle cx="52" cy="84" r="7"/><path d="M60 54c20 0 18-26 40-26h8M108 20v16"/></g><path d="M118 22l12-6M118 28h14M118 34l12 6" ${AS}/>`,
  escalera: `<g ${T}><path d="M44 94L58 8M96 94L82 8"/></g><g stroke="var(--accent)" stroke-width="5" stroke-linecap="round"><path d="M50 72h46M53 52h38M56 32h32"/></g>`,
  vibrador: `<g ${T}><rect x="12" y="28" width="36" height="28" rx="5"/><path d="M48 42c26 0 26 32 52 32h8"/><path d="M20 56v10M40 56v10"/></g><rect x="106" y="66" width="26" height="14" rx="7" ${A}/>`,
  demoledor: `<g ${T}><path d="M38 14h64"/><rect x="56" y="18" width="28" height="44" rx="6"/><path d="M70 62v24"/></g><path d="M62 84h16l-8 14z" ${A}/>`,
  soldadora: `<g ${T}><rect x="14" y="30" width="58" height="46" rx="6"/><circle cx="31" cy="47" r="6"/><circle cx="55" cy="47" r="6"/><path d="M26 64h34"/><path d="M72 62c18 0 20-28 40-28l10-10"/></g><path d="M126 8l3 7 7-1-5 6 4 6-7-2-3 7-2-7-7 1 5-6-4-6 7 2z" ${A}/>`,
  medicion: `<g ${T}><rect x="10" y="38" width="120" height="24" rx="4"/><path d="M28 38v9M46 38v6M94 38v6M112 38v9"/></g><rect x="58" y="44" width="24" height="12" rx="6" ${A}/>`,
  elevador: `<g ${T}><path d="M100 12v72M100 78h30"/><path d="M18 50h52l14 14v18H18z"/><path d="M38 50V28h22l8 22"/><circle cx="34" cy="84" r="8"/><circle cx="72" cy="84" r="8"/></g><rect x="104" y="34" width="22" height="18" rx="2" ${A}/>`,
  bomba: `<g ${T}><circle cx="52" cy="56" r="24"/><rect x="76" y="44" width="42" height="24" rx="4"/><path d="M52 32V16h26M28 56H10"/></g><path d="M52 44c-6 8-8 12-8 15a8 8 0 0 0 16 0c0-3-2-7-8-15z" ${A}/>`,
  jardineria: `<g ${T}><path d="M30 24l76 56"/><rect x="10" y="10" width="28" height="20" rx="4"/><path d="M56 42l14-12"/></g><circle cx="112" cy="84" r="10" ${AS}/>`,
  pintura: `<g ${T}><rect x="18" y="14" width="82" height="28" rx="6"/><path d="M100 28h14v26H64v14"/><rect x="58" y="68" width="12" height="26" rx="4"/></g><rect x="25" y="21" width="68" height="14" rx="4" ${A}/>`,
  carretilla: `<g ${T}><path d="M12 32h86l-14 32H34z"/><circle cx="96" cy="80" r="10"/><path d="M84 64l10 12M34 64l-6 26M98 32l28-12"/></g><path d="M24 39h64l-8 17H38z" ${A}/>`,
  reflector: `<g ${T}><rect x="42" y="10" width="56" height="40" rx="6"/><path d="M70 50v18M70 68L44 94M70 68l26 26M70 68v26"/></g><rect x="50" y="18" width="40" height="24" rx="3" ${A}/>`,
  herramienta: `<g ${T}><path d="M86 18a20 20 0 0 0-26 26L22 82l12 12 38-38a20 20 0 0 0 26-26l-12 12-12-4-4-12z"/></g><circle cx="30" cy="86" r="4" ${A}/>`,
};

// Íconos de interfaz (24×24, trazo de 2)
const U = 'fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
const ICONOS = {
  plus: `<path ${U} d="M12 5v14M5 12h14"/>`,
  minus: `<path ${U} d="M5 12h14"/>`,
  cart: `<path ${U} d="M3 4h2l2.2 10.2a2 2 0 0 0 2 1.6h7.4a2 2 0 0 0 2-1.5L20 8H6.2"/><circle cx="9.5" cy="20" r="1.4" fill="currentColor"/><circle cx="17" cy="20" r="1.4" fill="currentColor"/>`,
  search: `<g ${U}><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></g>`,
  user: `<g ${U}><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></g>`,
  users: `<g ${U}><circle cx="9" cy="8" r="4"/><path d="M2 21a7 7 0 0 1 14 0M16 4a4 4 0 0 1 0 8M22 21a7 7 0 0 0-4-6.3"/></g>`,
  logout: `<path ${U} d="M15 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 17l-5-5 5-5M5 12h11"/>`,
  sun: `<g ${U}><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></g>`,
  moon: `<path ${U} d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>`,
  close: `<path ${U} d="M6 6l12 12M18 6 6 18"/>`,
  chevron: `<path ${U} d="m6 9 6 6 6-6"/>`,
  arrow: `<path ${U} d="M5 12h14M13 6l6 6-6 6"/>`,
  back: `<path ${U} d="M19 12H5M11 18l-6-6 6-6"/>`,
  check: `<path ${U} d="m5 12 5 5L20 7"/>`,
  alert: `<g ${U}><path d="M12 3 2 20h20z"/><path d="M12 10v4M12 17h.01"/></g>`,
  info: `<g ${U}><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></g>`,
  calendar: `<g ${U}><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/></g>`,
  truck: `<g ${U}><path d="M2 6h12v10H2zM14 10h4l4 4v2h-8"/><circle cx="6" cy="18" r="2"/><circle cx="18" cy="18" r="2"/></g>`,
  undo: `<path ${U} d="M9 14 4 9l5-5M4 9h11a5 5 0 0 1 0 10h-3"/>`,
  wrench: `<path ${U} d="M14.7 6.3a4 4 0 0 0 5 5L22 14l-8 8-3-3 2.3-2.3a4 4 0 0 0-5-5L3 17V7l4-4h10z"/>`,
  chart: `<path ${U} d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>`,
  file: `<g ${U}><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6M8 13h8M8 17h5"/></g>`,
  card: `<g ${U}><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/></g>`,
  box: `<g ${U}><path d="M21 8 12 3 3 8v8l9 5 9-5z"/><path d="m3 8 9 5 9-5M12 13v8"/></g>`,
  tag: `<g ${U}><path d="M20 12 12 20l-8-8V4h8z"/><circle cx="8" cy="8" r="1.4" fill="currentColor"/></g>`,
  home: `<path ${U} d="M3 11 12 3l9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>`,
  trash: `<path ${U} d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/>`,
  edit: `<path ${U} d="M4 20h4L19 9l-4-4L4 16zM14 6l4 4"/>`,
  image: `<g ${U}><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m21 16-5-5-9 9"/></g>`,
  download: `<path ${U} d="M12 4v11M7 10l5 5 5-5M4 20h16"/>`,
  shield: `<path ${U} d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/>`,
  clock: `<g ${U}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></g>`,
  pin: `<g ${U}><path d="M12 21s-7-6-7-11a7 7 0 0 1 14 0c0 5-7 11-7 11z"/><circle cx="12" cy="10" r="2.5"/></g>`,
  inbox: `<path ${U} d="M3 13h5l1.5 3h5L16 13h5M5 5h14l2 8v6H3v-6z"/>`,
  alertCircle: `<g ${U}><circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01"/></g>`,
  layers: `<path ${U} d="m12 3 9 5-9 5-9-5zM3 13l9 5 9-5"/>`,
  filter: `<path ${U} d="M3 5h18l-7 8v6l-4 2v-8z"/>`,
  money: `<g ${U}><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M6 10v4M18 10v4"/></g>`,
};

// Palabras clave del nombre de la categoría → ilustración (más robusto que depender de los ids)
const POR_CATEGORIA = [
  ['taladro', 'taladro'], ['rotomartillo', 'taladro'], ['pulidora', 'pulidora'], ['esmeril', 'pulidora'],
  ['sierra', 'sierra'], ['cortadora', 'sierra'], ['andamio', 'andamio'], ['mezcladora', 'mezcladora'],
  ['compactadora', 'compactadora'], ['generador', 'generador'], ['compresor', 'compresor'],
  ['hidrolavadora', 'hidrolavadora'], ['escalera', 'escalera'], ['vibrador', 'vibrador'],
  ['demoledor', 'demoledor'], ['soldadora', 'soldadora'], ['medición', 'medicion'], ['medicion', 'medicion'],
  ['elevador', 'elevador'], ['montacarga', 'elevador'], ['bomba', 'bomba'], ['jardiner', 'jardineria'],
  ['pintura', 'pintura'], ['carretilla', 'carretilla'], ['transporte', 'carretilla'],
  ['iluminación', 'reflector'], ['iluminacion', 'reflector'],
];

export function montarIconos() {
  if (document.getElementById('sprite-obraya')) return;
  const simbolos = [
    ...Object.entries(ILUSTRACIONES).map(([n, d]) => `<symbol id="h-${n}" viewBox="0 0 140 100">${d}</symbol>`),
    ...Object.entries(ICONOS).map(([n, d]) => `<symbol id="i-${n}" viewBox="0 0 24 24">${d}</symbol>`),
  ].join('');
  const sprite = document.createElement('div');
  sprite.innerHTML = `<svg id="sprite-obraya" width="0" height="0" style="position:absolute" aria-hidden="true"><defs>${simbolos}</defs></svg>`;
  document.body.prepend(sprite.firstElementChild);
}

/** Ícono de interfaz. */
export function icono(nombre, clase = '') {
  return `<svg class="${clase}" aria-hidden="true"><use href="#i-${nombre}"/></svg>`;
}

/** Nombre de la ilustración que corresponde a una categoría. */
export function ilustracionDe(categoria = '') {
  const texto = String(categoria).toLowerCase();
  const encontrada = POR_CATEGORIA.find(([clave]) => texto.includes(clave));
  return encontrada ? encontrada[1] : 'herramienta';
}

/** Ilustración vectorial de una herramienta según su categoría. */
export function ilustracion(categoria, clase = '') {
  return `<svg class="${clase}" viewBox="0 0 140 100" aria-hidden="true"><use href="#h-${ilustracionDe(categoria)}"/></svg>`;
}
