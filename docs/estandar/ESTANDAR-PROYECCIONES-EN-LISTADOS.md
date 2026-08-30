# Estándar: los endpoints de listado devuelven proyecciones, no entidades

**Vigente desde:** 2026-08-27. Aplica a todo endpoint REST nuevo que devuelva una lista.

## La regla

**Todo endpoint nuevo que devuelva una lista devuelve una proyección — nunca la entidad JPA
completa.** Si el endpoint necesita datos de una relación (`empresa`, `titular`, `asiento`,
`usuario`…), se **aplanan** los dos o tres campos que hagan falta (ej. `proveedor`,
`identificacion`) directamente en la proyección — **no se anida el objeto completo**.

`getAll` genérico (el que expone la entidad tal cual, vía `EntityDao.selectAll`) se reserva para
**catálogos chicos**: tablas de referencia con pocas decenas de filas donde el peso nunca importa
(tipos, estados, grupos, formas de pago…). No es el patrón por defecto para nada que pueda crecer
a cientos o miles de filas.

## Por qué — dos incidentes reales, mismo mes

1. **`GET /fctc/sustentoPendiente`** devolvía `List<FacturaCompra>` completa: **536 KB para 131
   filas**, porque cada `FacturaCompra` arrastra `empresa` (con su jerarquía), `titular`,
   `usuario` y `asiento`. Se corrigió devolviendo `FacturaSustentoPendiente` — 7 campos planos
   (`id`, `numero`, `fecha`, `proveedor`, `identificacion`, `total`, `iva`, `sustentoSugerido`).
   Ver `docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md`.

2. **`GET /asnt/getAll`** era el fallback de `listado-asientos.component.ts` cuando el filtro por
   criterios fallaba: el frontend bajaba **los 1.784 asientos de todas las empresas** (~4 MB) y
   filtraba en el cliente — cruzando empresas, exactamente el mismo riesgo de fuga de datos entre
   compañías que se acababa de cerrar del lado del backend en otro cambio de esta sesión. Se
   agregó `GET /asnt/resumen/{idEmpresa}/{idPeriodo}`, que devuelve `AsientoResumen` (código,
   número, fecha, glosa, estado, total debe, total haber) filtrado por empresa y período, para
   que el fallback deje de necesitar `getAll`. Ver
   `docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` (donde se detectó el
   patrón por primera vez, en el barrido que motivó este estándar).

En ningún caso el problema era la cantidad de datos que el usuario necesitaba ver — era que la
serialización arrastraba todo el grafo JPA por cada fila.

## Cómo construir una proyección en este proyecto

1. **DTO plano** en el paquete `model` del módulo correspondiente (`com.saa.model.{mod}`), sin
   anotaciones JPA, `implements Serializable`, con getters/setters — mismo patrón que
   `FacturaSustentoPendiente`, `AsientoResumen`, o los DTO de
   `docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` (`PendienteExtractoTransito`,
   `GrupoConciliadoResumen`, etc.).
2. **Constructor que reciba exactamente los campos de la proyección**, en el mismo orden y tipo
   que la consulta — lo usa la expresión JPQL `select new com.saa.model.{mod}.MiProyeccion(...)`.
   Mantener también el constructor vacío, para deserialización/uso general.
3. **La consulta va en el DAO** (`{Entidad}DaoServiceImpl`), como cualquier otra query
   personalizada de este proyecto — con `select new ...` en vez de `select e`. Si la proyección
   necesita agregar (sumas, conteos), agruparlas ahí mismo con `group by` en vez de traer las
   filas crudas y sumar en Java.
4. Si la relación pesada es una colección (ej. líneas de un asiento) y la entidad principal **no**
   tiene mapeada la colección inversa, arrancar la consulta `FROM` la entidad de detalle y navegar
   hacia la cabecera (`d.asiento.codigo`, `d.asiento.empresa.codigo`…) — no hace falta mapear una
   colección nueva solo para esto. Ver `AsientoDaoServiceImpl.selectResumenPorEmpresaPeriodo` como
   ejemplo.
5. El endpoint REST recibe y expone directamente la lista de la proyección — no hay paso
   intermedio de "traer la entidad y mapearla a mano" salvo que la consulta con `select new` no
   sea viable (por ejemplo, si el mapeo depende de lógica de negocio que no es expresable en
   JPQL, como `sustentoSugerido` en `FacturaSustentoPendiente` — ahí se arma la proyección en el
   *service*, no en el DAO, pero sigue siendo una proyección, nunca la entidad).

## Lo que NO se tocó (relevado, no arreglado)

El barrido que motivó este estándar encontró varios `getAll` existentes con el mismo problema,
que **no se tocaron** porque no estaban en uso (verificado contra el frontend) o eran de bajo
impacto — quedan documentados aquí para que no se repita el patrón al tocarlos, no como pendiente
urgente:

| Endpoint | Entidad | Motivo para no tocar ahora |
|---|---|---|
| `GET /pgtr/getAll` | `PagoProgramado` | Sin uso: ningún componente del frontend lo invoca (~900 KB–1 MB estimado, 123 filas, con `FacturaCompra` completa anidada además de `empresa`) |
| `GET /aplp/getAll` | `AplicacionPagoCxp` | Sin uso, mismo motivo (~800 KB–1 MB, 148 filas) |
| `GET /fctc/getAll` | `FacturaCompra` | Sin uso confirmado del `getAll` genérico (536 KB medido si se llamara) — el endpoint específico que sí se usaba (`sustentoPendiente`) ya se corrigió |
| `GET /dcxp/getAll` | `DocumentoCxp` | Bajo impacto (~350–450 KB, 258 filas) |
| `GET /mvcb/getAll` | `MovimientoBanco` | Bajo impacto (~300–370 KB, 122 filas) |
| `POST /cnct/transito/anular/{idCierre}` | `Conciliacion` | Una sola fila (no el problema de payload de una lista) pero mismo patrón estructural — reutilizar un campo de texto existente (`motivoAnulacion`) para el autor de la anulación, en vez de agregar columna, siguió el mismo criterio de "no tocar lo que no hace falta" |

**Optimizar un endpoint sin uso es trabajo tirado y cada cambio es riesgo** — si alguno de estos
se vuelve a necesitar, aplicar esta guía en ese momento, no antes.
