// Sesión del usuario: el JWT y los datos básicos se guardan en localStorage.
// El token NO se decodifica para decidir permisos: el rol que se usa aquí solo sirve para mostrar
// el panel correcto. Quien de verdad autoriza es el backend, que valida la firma en cada petición.

const CLAVE = 'obraya.sesion';

function leer() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) || null;
  } catch {
    return null;
  }
}

export const sesion = {
  guardar(autenticacion, perfil = null) {
    const datos = {
      token: autenticacion.token,
      expiraEn: autenticacion.expiraEn,
      usuario: autenticacion.usuario,
      perfil,
    };
    try { localStorage.setItem(CLAVE, JSON.stringify(datos)); } catch { /* modo privado: la sesión dura lo que la pestaña */ }
    this._memoria = datos;
  },

  datos() {
    const d = this._memoria || leer();
    if (!d) return null;
    if (d.expiraEn && new Date(d.expiraEn) <= new Date()) {
      this.cerrar();
      return null;
    }
    return d;
  },

  token() { return this.datos()?.token || null; },
  usuario() { return this.datos()?.usuario || null; },
  perfil() { return this.datos()?.perfil || null; },
  activa() { return !!this.token(); },

  /** Rol principal para elegir el panel: ADMIN > PROVEEDOR > CLIENTE. */
  rol() {
    const roles = this.usuario()?.roles || [];
    return ['ADMIN', 'PROVEEDOR', 'CLIENTE'].find((r) => roles.includes(r)) || null;
  },

  nombre() {
    const u = this.usuario();
    return u ? `${u.nombres} ${u.apellidos}` : '';
  },

  iniciales() {
    const u = this.usuario();
    return u ? (u.nombres[0] + (u.apellidos[0] || '')).toUpperCase() : '';
  },

  cerrar() {
    this._memoria = null;
    try { localStorage.removeItem(CLAVE); } catch { /* nada */ }
  },

  /** Si no hay sesión (o el rol no sirve) manda al login y recuerda a dónde volver. */
  exigir(roles = null) {
    const rol = this.rol();
    if (!rol || (roles && !roles.includes(rol))) {
      const volver = location.pathname.split('/').pop() + location.search + location.hash;
      location.href = `ingresar.html?volver=${encodeURIComponent(volver)}`;
      return false;
    }
    return true;
  },
};
