# Plan de estudio de 24 horas

El 39.5 % de la nota (66 de 167 puntos) es **explicar el código**. El resto se gana con el proyecto ya construido,
pero también te lo van a preguntar. Este plan reparte el tiempo según ese peso: **16 horas de estudio, 7 de sueño y
1 de descansos.** Dormir no es opcional: la memoria se consolida durmiendo.

**Materiales:** [`mapa-del-codigo.md`](mapa-del-codigo.md) (dónde está cada cosa) ·
[`flujos.md`](flujos.md) (qué pasa en cada operación) · [`guia-examen.md`](guia-examen.md) (preguntas con respuesta
modelo) · el código abierto en VS Code.

**Método para cada bloque (siempre igual):**
1. Lee el flujo en `flujos.md`.
2. Abre los archivos que nombra y sigue el recorrido con `Ctrl+clic`.
3. **Explícalo en voz alta sin mirar**, como si el profesor estuviera al frente.
4. Responde las preguntas de la guía con tus palabras **antes** de leer la respuesta modelo.
5. Escribe en un papel lo que no supiste explicar y repásalo al final del día.

También puedes escribirme **"modo examen"**: elijo un archivo, te pregunto qué hace y por qué está en esa capa, y te corrijo.

---

## Día 1

| Hora | Bloque | Qué estudiar | Rúbrica |
|---|---|---|---|
| 0:00–1:30 | **La arquitectura** | Las 3 capas y por qué las dependencias apuntan hacia adentro. Recorre un caso: `ReservaController` → `GestionReservasUseCase` → `GestionReservasService` → `Reserva` → `ReservaRepository` → `ReservaRepositoryAdapter`. Puertos de entrada vs. de salida. Por qué entidad JPA ≠ agregado | ARQ (10) + Dominio |
| 1:30–3:00 | **Seguridad y JWT** | Flujo 1 de `flujos.md`. Lee `SecurityConfig`, `JwtTokenProvider`, `JwtValidationFilter`, `UsuarioAutenticado`. Qué contiene el token, cómo se firma y por qué no se puede falsificar el rol. 401 vs. 403. BCrypt. Stateless y CSRF | SEG (10) + JWT (9) |
| 3:00–3:15 | Descanso | — | — |
| 3:15–5:00 | **Reservas y disponibilidad** | Flujos 3 y 4. `Reserva`, `RangoFechas`, `DetalleReserva` (precio congelado), `unidadesComprometidas`, el bloqueo `FOR UPDATE` y por qué se ordena por id. El job de vencimiento | RES (10) |
| 5:00–6:30 | **Pagos y facturación** | Flujo 5. Por qué se bloquea la reserva y se verifica ANTES de cobrar. Pago rechazado vs. aprobado. `PasarelaPagoPort` y cómo se cambiaría por Wompi. Por qué no se guarda la tarjeta. Cómo se arma el PDF | PAG (10) |
| 6:30–7:15 | Almuerzo | — | — |
| 7:15–8:30 | **Entregas, devoluciones y daños** | Flujo 6. Reglas de `Entrega`, `Devolucion`, `ReporteDano`. Daño GRAVE → `sacarDeServicio()`. Quién valida y por qué hay defensa en profundidad | CRUD (10) + RES |
| 8:30–9:45 | **Reportes** | Flujo 7. Por qué SQL nativo, CQRS ligero, ingresos brutos vs. netos, cómo se calcula la ocupación, por qué el proveedor solo ve lo suyo | REP (10) |
| 9:45–10:00 | Descanso | — | — |
| 10:00–11:00 | **Base de datos** | Diagrama `docs/diagrama-relacional.png`: las 15 tablas y sus relaciones. Flyway: por qué no se editan las migraciones. Índices de V5. Enums con `VARCHAR` + `CHECK`. Seeds solo en `dev` | BD (8 + 8) |
| 11:00–12:00 | **Repaso activo 1** | Responde en voz alta las preguntas de las fases 0 a 4 de `guia-examen.md` | Dominio |
| 12:00–19:00 | **Dormir** | 7 horas | — |

## Día 2

| Hora | Bloque | Qué estudiar | Rúbrica |
|---|---|---|---|
| 19:00–20:00 | **Repaso activo 2** | Preguntas de las fases 5 a 9 + lo que anotaste en el papel ayer | Dominio |
| 20:00–21:00 | **Swagger y pruebas** | Abre Swagger y explica un endpoint con sus errores `ProblemDetail`. Explica los 3 tipos de prueba: dominio (sin Spring), casos de uso (Mockito), integración (MockMvc + MySQL, rollback). Corre `mvn test` | DOC (7) + Dominio |
| 21:00–22:00 | **Frontend** | `api.js` (JWT, 401), `sesion.js` (por qué el rol del navegador no da permisos), `esc()` y XSS, el carrito y la cotización en vivo | Presentación |
| 22:00–22:15 | Descanso | — | — |
| 22:15–23:15 | **Demo completa** | Haz el flujo en el frontend como si lo presentaras: cliente reserva → proveedor aprueba → cliente paga (0000 y luego 4242) → descarga factura → proveedor entrega y recibe con daño → admin valida y mira el panel. Cronométralo (máximo 10 min) | Todo |
| 23:15–24:00 | **Simulacro "modo examen"** | Pídeme "modo examen" y responde 10 preguntas seguidas sin mirar | Dominio |

---

## Lo que no puede fallar (si solo tienes 1 hora)

1. Dibujar las 3 capas y decir qué va en cada una.
2. Explicar cómo se firma y se valida el JWT y por qué cambiar el rol no funciona.
3. Explicar la disponibilidad: operativas − comprometidas (APROBADA/PAGADA/ENTREGADA que se cruzan) y el `FOR UPDATE`.
4. El flujo de pago en 5 pasos: bloquear → verificar → cobrar → registrar → confirmar y facturar.
5. Qué es un puerto y un adaptador, con un ejemplo (`PasarelaPagoPort` / `PasarelaPagoSimuladaAdapter`).
6. Por qué hay transacciones sin `@Transactional` en los casos de uso (`UseCaseConfig`).
7. Git Flow: `feature/*` → PR a `develop` → `release/1.0.0` → `main` + tag.
