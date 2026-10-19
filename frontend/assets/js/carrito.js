// Carrito de reserva, guardado en el navegador. Regla del negocio que se respeta desde aquí:
// una reserva es de UN solo proveedor, así que no se mezclan herramientas de proveedores distintos.

const CLAVE = 'obraya.carrito';

/** Fecha local en formato ISO (toISOString usa UTC: en Bogotá, después de las 7 p. m. daría mañana). */
export function fechaIso(d) {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

export function hoyMas(dias) {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return fechaIso(d);
}

function vacio() {
  return { fechaInicio: hoyMas(1), fechaFin: hoyMas(3), items: [] };
}

function leer() {
  try {
    const c = JSON.parse(localStorage.getItem(CLAVE));
    if (!c || !Array.isArray(c.items)) return vacio();
    // Fechas ya pasadas se corren a mañana
    if (c.fechaInicio < hoyMas(0)) Object.assign(c, { fechaInicio: hoyMas(1), fechaFin: hoyMas(3) });
    return c;
  } catch {
    return vacio();
  }
}

function guardar(c) {
  try { localStorage.setItem(CLAVE, JSON.stringify(c)); } catch { /* nada */ }
  window.dispatchEvent(new CustomEvent('carrito', { detail: c }));
}

export const carrito = {
  obtener: leer,

  cantidad() {
    return leer().items.reduce((s, i) => s + i.cantidad, 0);
  },

  /** Devuelve 'agregado' o 'otro-proveedor' (si el carrito ya tiene herramientas de otro proveedor). */
  agregar(h, cantidad = 1, { reemplazar = false } = {}) {
    let c = leer();
    if (c.items.length && c.items[0].proveedorId !== h.proveedor.id) {
      if (!reemplazar) return 'otro-proveedor';
      c = { ...c, items: [] };
    }
    const existente = c.items.find((i) => i.herramientaId === h.id);
    if (existente) {
      existente.cantidad += cantidad;
    } else {
      c.items.push({
        herramientaId: h.id, nombre: h.nombre, precioPorDia: Number(h.precioPorDia), cantidad,
        categoria: h.categoria?.nombre || '', imagen: h.imagenPrincipal || null,
        proveedorId: h.proveedor.id, proveedor: h.proveedor.razonSocial, ciudad: h.proveedor.ciudad,
      });
    }
    guardar(c);
    return 'agregado';
  },

  cambiarCantidad(herramientaId, delta) {
    const c = leer();
    const item = c.items.find((i) => i.herramientaId === herramientaId);
    if (!item) return;
    item.cantidad += delta;
    if (item.cantidad <= 0) c.items = c.items.filter((i) => i !== item);
    guardar(c);
  },

  quitar(herramientaId) {
    const c = leer();
    c.items = c.items.filter((i) => i.herramientaId !== herramientaId);
    guardar(c);
  },

  fijarFechas(fechaInicio, fechaFin) {
    const c = leer();
    c.fechaInicio = fechaInicio;
    c.fechaFin = fechaFin < fechaInicio ? fechaInicio : fechaFin;
    guardar(c);
  },

  vaciar() {
    const c = leer();
    guardar({ ...c, items: [] });
  },

  /** Cuerpo para POST /reservas/cotizacion y POST /reservas. */
  pedido(notas) {
    const c = leer();
    return {
      fechaInicio: c.fechaInicio,
      fechaFin: c.fechaFin,
      herramientas: c.items.map((i) => ({ herramientaId: i.herramientaId, cantidad: i.cantidad })),
      ...(notas ? { notas } : {}),
    };
  },
};
