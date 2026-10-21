// Página del enlace de recuperación: lee el token de la URL (?token=...) y envía la contraseña nueva.

import { post } from '../api.js';
import { icono } from '../iconos.js';
import { montarLayout, $, cargando, mostrarErrorEnFormulario } from '../ui.js';

montarLayout({ sinPie: true });

const token = new URLSearchParams(location.search).get('token');
const form = $('[data-form]');
const caja = $('[data-error]', form);

if (!token) {
  form.innerHTML = `<div class="notice bad">${icono('alertCircle')}<div><b>Enlace incompleto.</b><br>
    Abre el enlace completo que llegó a tu correo o solicita uno nuevo.</div></div>
    <a class="btn btn-primary btn-block" href="ingresar.html">Volver a ingresar</a>`;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  caja.classList.add('hidden');
  if (form.password.value !== form.confirmacion.value) {
    mostrarErrorEnFormulario(form, caja, { errores: { confirmacion: 'Las contraseñas no coinciden' } });
    return;
  }
  const boton = $('button[type="submit"]', form);
  cargando(boton, true);
  try {
    await post('/auth/restablecer', { token, password: form.password.value });
    form.innerHTML = `<div class="notice ok">${icono('check')}<div><b>¡Listo! Tu contraseña se actualizó.</b><br>
      Ya puedes ingresar con tu contraseña nueva. El enlace dejó de funcionar.</div></div>
      <a class="btn btn-primary btn-lg btn-block" href="ingresar.html">Ingresar</a>`;
  } catch (error) {
    mostrarErrorEnFormulario(form, caja, error);
    cargando(boton, false);
  }
});
