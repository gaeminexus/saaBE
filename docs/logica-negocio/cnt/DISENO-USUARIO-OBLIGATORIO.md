# Usuario obligatorio en todo proceso que grabe: sin usuario válido, no se hace nada

**Estado: DISEÑO, fase U-1 (inventario) despachada.** omen-saa-1-arb, 2026-11-01.

## 1. Decisión del usuario (2026-11-01, textual)
> «Asientos sin usuario se deben rechazar y el proceso que los ejecute no se debe realizar, por ejemplo,
> si se hace un pago a factura pero el asiento no tiene usuario, entonces el pago no se debe realizar, y
> así en todos los procesos. Por eso para eso lo primero que se debe validar en los procesos es si el
> usuario existe.»

## 2. Qué significa
1. **Validar al empezar, no al final.** Todo proceso que vaya a grabar algo (pago, cobro, documento,
   carga, cierre, nómina, anulación…) valida el usuario **como primera instrucción**, antes de tocar
   la base. Si no es válido, el proceso no corre y responde un error claro. Así no queda nada a medias.
2. **«El usuario existe»** = no nulo, no vacío y **corresponde a un usuario activo del sistema**
   (`com.saa.model.scp.Usuario`). Un texto inventado, `'SYSTEM'` o `'SISTEMA'` no pasa, salvo que exista
   como usuario real.
3. **Se acaban los rellenos.** Todos los `x != null ? x : "SISTEMA"` del backend se eliminan, en todos
   los módulos: el rastreo del B6 (`B6-LLAMADORES-USUARIO-DEL-ASIENTO.md`) encontró ~40 en cxc, cxp,
   tsr y rhh, y los de crd y cnt. El frontend ya no rellena desde F1 (`6e4e92d`).
4. **Defensa central.** Además, `AsientoContableServiceImpl.grabarAsiento` (único punto por donde pasa
   la creación) y la anulación rechazan un usuario inválido. Es la red de seguridad, no el control
   principal: si se llega ahí sin usuario, el proceso ya grabó cosas antes; por eso el punto 1.
5. **Procesos sin persona** (timers, recepción automática del SRI, reproceso por lotes): se listan en
   el inventario. Cada uno necesita una decisión: o lo lanza una persona (y lleva su usuario), o se crea
   un **usuario técnico real** en el sistema (activo, con nombre propio) que lo identifique. Hoy ningún
   timer de crd/cnt crea asientos.

## 3. Fases
- **U-1 (solo lectura):** inventario por módulo de cada proceso que graba:
  - punto de entrada (REST);
  - de dónde saldría el usuario (DTO, entidad, query);
  - si el FE ya lo manda;
  - los rellenos a quitar.

  Además: diseño del helper único de validación, y la lista de procesos sin persona. Base: el rastreo
  del B6.
- **U-2:** helper + crd, cnt y asoprep. Incluye B6-b: la carga con el usuario de quien procesa.
- **U-3:** cxc y cxp. **U-4:** tsr, rhh, pagos y sri. Cada uno con su contrato de API cuando el FE
  tenga que mandar el usuario.
- **U-5:** la defensa central de `grabarAsiento` se **activa al final**. Activarla antes rompería los
  procesos que todavía llegan sin usuario.

## 4. Coordinación
cxc, cxp, tsr y rhh tienen trabajo de los equipos 2 y 3, que **están abajo**. El usuario decidió la regla
y esos módulos están en el alcance del equipo 1. Antes de tocar un archivo: `git status` y su historial, y
dejar el aviso en `docs/equipos/saa-1.md` para cuando vuelvan.

## 5. Helper (hecho en B6-b, 2026-11-01)
`com.saa.basico.ejb.UsuarioValidacionService.validar(nombre)` y `UsuarioInvalidoException` (rollback). Un
usuario es válido si no es nulo, no está vacío y existe `Usuario.nombre` (`SCP.PJRQ.PJRQNMBR`).
- **Verificado por el árbitro:** es la misma búsqueda que hace el login (`UsuarioDaoServiceImpl.validaUsuarioSucursal`
  → `selectByNombre`, después de `pr_vlda_usro_clve`), y el FE guarda el nombre en MAYÚSCULAS. Un
  usuario que entró al sistema siempre pasa.
- **Límite:** `Usuario` no mapea ninguna columna de estado, así que hoy «activo» = «existe». El estado
  lo controla el procedimiento del login. Si hiciera falta exigir el estado en cada proceso, hay que
  saber en qué columna o tabla vive.

## 6. ⛔ El reproceso no se puede romper (2026-11-01)
El reproceso del cuadre corre con el código de `main`.
- Graba como «SISTEMA»:
  - el modo reproceso de la carga (B6-a/B6-b);
  - los cargadores históricos C11/C13 (`ctx.usuario`), que se conservan hasta que el reproceso llegue a
    producción.
- Si «SISTEMA» no existe como `Usuario` real, **la defensa central (U-5) tumba el reproceso**.
- **Antes de U-5 hay que decidir cómo se identifica:**
  - un usuario técnico real en `SCP.PJRQ` (recomendado: uno con nombre propio, p. ej. `REPROCESO`);
  - o el usuario de la persona que lo lanza.
- Se coordina con el árbitro del cuadre, con la confirmación del usuario.

## 7. Resultado del inventario U-1 (`INVENTARIO-USUARIO-OBLIGATORIO.md`) y decisión del árbitro
- Los cargadores históricos graban con el usuario real 38 («usuario 1», decisión del coordinador del
  cuadre, 28-oct). **Pasan la validación sin cambios.**
- **El reproceso de la carga Petro** graba «SISTEMA» y «SAA_REPROCESO», pero lo lanza una persona y ya
  recibe `solicitud.usuario` validado. **Decisión: graba con ese usuario.** Es la regla del usuario:
  el que ejecuta.
  - Hay que avisar al árbitro del cuadre, con la confirmación del usuario: cambia el usuario de los
    asientos del reproceso, no los montos.
- Ningún timer crea asientos. Queda abierta la **recepción automática del SRI** (`FacturaServiceImpl:2482`).
