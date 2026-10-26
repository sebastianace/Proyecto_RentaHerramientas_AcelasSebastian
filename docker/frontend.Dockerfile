# =====================================================================
# Imagen del FRONTEND (HTML, CSS y JavaScript puro).
# No hay nada que compilar: nginx solo sirve los archivos estáticos de la carpeta frontend/.
# El navegador llama a la API en http://localhost:8080 (valor por defecto de assets/js/config.js).
# =====================================================================
FROM nginx:1.27-alpine
COPY frontend/ /usr/share/nginx/html/
EXPOSE 80
