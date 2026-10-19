# Diseño: que cada consulta traiga solo las tablas necesarias

**Fecha:** 2026-10-19 · **Dueño:** omen-saa-3-arb · **Ejecutores:** omen-saa-3-be (saaBE), omen-saa-3-fe (saaFE, inventario de solo lectura)

## Por qué
El 2026-10-19 producción cayó con `ORA-04036` al confirmar el pago 769: cargar un `AnticipoEmpleado`
generaba **un SELECT con 431 LEFT JOIN** (contado sobre el SQL del log). Desglose:

| Tabla | Veces | Necesaria |
|---|---|---|
| `SCP.PJRQ` (Empresa / Usuario) | 127 | casi nunca: se usa solo el código |
| `SCP.PGSP` (Jerarquia de la empresa/usuario) | 127 | no: nadie la lee |
| Cadena del asiento (`ASNT`, `MYRZ`, `PRDO`×2, `PLNT`) | ~80 | rara vez |
| Titular / persona (`TTLR`, `PSSS`) | 34 | a veces |
| Relaciones propias del pago y del anticipo | ~60 | sí |

Ya aplicado (freno de emergencia): `hibernate.max_fetch_depth=2` (`04cecc37`) — mismo resultado, en
varias consultas cortas. En curso: `@Fetch(SELECT)` en `Empresa.jerarquia` / `Usuario.jerarquia`.
Ninguno de los dos deja de *leer* tablas: solo parte la consulta. Este diseño es el que deja de leerlas.

## Restricción que manda
Las entidades JPA se serializan **directo a JSON** (Jackson, sin DTO). Hoy hay una sola relación
`LAZY` en 435 entidades. Si una relación pasa a `LAZY` sin más, Jackson revienta al serializar el proxy
de Hibernate (o lanza `LazyInitializationException` fuera de la transacción). Por eso hace falta una
fase 0 de infraestructura antes de tocar una sola relación.

## Medición de uso (verificada 2026-10-19)
- **Empresa:** el FE la lee 101 veces y **todas** son `empresa.codigo` (las 3 de `empresa.nombre`/`toString`
  son la empresa de la sesión, no la anidada). El BE: 216 `getEmpresa().getCodigo()` y 2 `getNombre()`
  (`GeneracionOrdenPagoServiceImpl:868`, dentro de transacción → carga perezosa válida).
- **Usuario anidado:** el FE solo lee `usuario.codigo` (`cierre-caja.component.ts:83`).
- `getCodigo()` sobre un proxy perezoso **no** va a la base (Hibernate conoce el id): no hay costo ni error,
  ni siquiera fuera de transacción.

## Fases

### Fase 0 — Serializador que entiende relaciones perezosas (BE, sin cambio de comportamiento)
- Dependencia `com.fasterxml.jackson.datatype:jackson-datatype-hibernate6`, versión alineada con el
  Jackson que usa hoy el despliegue (verificar en el WAR/WildFly, no suponer).
- Un `@Provider ContextResolver<ObjectMapper>` en `com.saa.ws.rest` que registre `JavaTimeModule` y
  `Hibernate6Module` con `FORCE_LAZY_LOADING=false` y `SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS=true`
  → una relación perezosa no cargada sale como `{"codigo": N}`.
- **El formato de fechas tiene que quedar idéntico al actual** (arreglos `[2026,8,10]`, ver CLAUDE.md
  §Serialización): `WRITE_DATES_AS_TIMESTAMPS` activo, aceptar ISO y arreglo en la entrada, mismo
  manejo de desconocidos/nulos que hoy. Un `ContextResolver` reemplaza la configuración por defecto:
  cualquier diferencia cambia el JSON de **todo** el sistema.
- Criterio de aceptación: como todavía nada es perezoso, el JSON de cualquier endpoint es idéntico
  byte a byte. El usuario compara en el `.3` tres respuestas (un `getId` de anticipo, un `selectByCriteria`
  de asientos, un endpoint con fechas `LocalDateTime`) antes y después.

### Fase 1 — Jerarquia de Empresa/Usuario en SELECT aparte (en curso)
`@Fetch(FetchMode.SELECT)` en los dos campos. Sin cambio de JSON.

### Fase 2 — `empresa` (y `usuario*` → `SCP.PJRQ`) perezosos en todas las entidades
- Solo después de la fase 0 desplegada y comparada.
- `@ManyToOne(fetch = FetchType.LAZY)` en todo campo que apunte a `Empresa`/`Usuario` (`SCP.PJRQ`).
  JSON resultante: `"empresa": {"codigo": 1236}` — el FE solo lee `codigo` (medido arriba).
- Riesgos a revisar por el ejecutor antes de cambiar: código del BE que lea `getEmpresa().getX()` (X ≠
  `getCodigo`) **fuera** de transacción (clases REST, hilos, `@Asynchronous`); reportes que reciban la
  entidad en vez de usar JDBC; comparaciones `empresa.equals(...)` sobre proxies.
- Grabación: el FE manda `empresa: {codigo: N}`; `merge` resuelve la referencia por id. Sin cambio.
- Corta las 127 uniones con `PJRQ` (y con ellas las 127 de `PGSP`).

### Fase 3 — Relaciones de negocio, entidad por entidad (por riesgo)
Orden (medición de cadenas EAGER sobre las 435 entidades): `AnticipoEmpleado`, `PagoProgramado`,
`DevolucionAnticipo`, `DevolucionCuotaAnticipo`, `DesgloseDetalleDeposito`, `AuxDepositoDesglose`,
`DetalleTransito`, `DetallePagoOrigenExterno`, `LiquidacionBeneficioSocial`, `OrdenBeneficioSocial`,
luego cobros de `tsr` y aplicaciones de pago.
Por cada entidad:
1. **Inventario FE (omen-saa-3-fe, solo lectura):** qué rutas anidadas lee cada pantalla del JSON de la
   entidad (p. ej. `anticipo.pagoProgramado.estado`), con archivo:línea.
2. **Inventario BE:** qué `getX().getY()` se usan y si es dentro o fuera de transacción.
3. Regla: relación con solo `.codigo` leído → `LAZY`; con campos leídos → queda `EAGER` (o se lee por
   endpoint aparte si el FE lo necesita solo en una pantalla).
4. Contrato: si una pantalla deja de recibir campos anidados, se anota en `docs/logica-negocio/{mod}/API-*.md`
   y se espeja a `saaFE/docs/{mod}/` **antes** de tocar el FE.

## Lo que no se hace
- No se cambia a `LAZY` ninguna relación sin la fase 0 desplegada.
- No se introducen DTO en este frente (sería otra migración).
- No se quita `max_fetch_depth=2`: sigue de red de seguridad.

## Reverso
Fase 0: quitar el `ContextResolver` (vuelve el mapeador por defecto). Fases 2–3: quitar `fetch = LAZY`
en el campo afectado. Ninguna fase toca la base de datos.
