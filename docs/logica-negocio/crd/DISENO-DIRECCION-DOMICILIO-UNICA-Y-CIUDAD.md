# DISEÑO — Una sola dirección de domicilio del socio (pantalla = reportes) y ciudad por combo

omen-saa-1-arb · 2026-11-16 · Pedido del usuario: «Necesitamos que en la solicitud de crédito siempre salga la dirección
que aparece en la pantalla de edición de datos. Además es necesario ingresar la ciudad escogiéndola de un combo.»
Origen: reclamo de un socio, cuya solicitud imprimió una dirección distinta de la que muestra «Editar Información de
Partícipe». Ejecutores: omen-saa-1-be y omen-saa-1-fe. Script: `crd/sql/379_DDL_DIRECCION_CIUDAD.sql`.

## 1. Lo que hay hoy: tres reglas distintas para elegir la dirección del socio en `CRD.DRCC`
| Dónde | Regla |
|---|---|
| Pantalla `entidad-participe-info.component.ts:511` | `getByParent` (JPQL **sin ORDER BY**) → la **primera** con `porDefecto = 1`, si no la primera. Sin filtrar estado, trabajo ni `CRD.DRTR`. En la práctica, la más antigua. |
| `RPRT_PRST_SLCT` (solicitud) y `RPRT_PRST_PGRE` (pagaré) | activa, no trabajo, no en `CRD.DRTR`; `DRCCPRDF` desc y **`DRCCCDGO` desc** (la más nueva). |
| `ContratoAdhesionParametros.direccionDomiciliaria` (contrato de adhesión, Anexo I) | activa y no trabajo; la primera por defecto en el orden de `getByParent`. |

Un socio con dos direcciones marcadas por defecto ve una en pantalla y otra en la solicitud y el pagaré. La pantalla, al
guardar, actualiza la que muestra y la marca por defecto, pero **no desmarca las otras**.

## 2. Regla única (decisión del usuario: manda la pantalla)
**Domicilio vigente** de una entidad = entre sus `CRD.DRCC` activas (`NVL(DRCCIDST,1)=1`), no de trabajo
(`NVL(DRCCTRBJ,0)<>1`) y que no estén en `CRD.DRTR`: la de `DRCCPRDF = 1` de **menor `DRCCCDGO`**; si ninguna está por
defecto, la de menor `DRCCCDGO`. (Menor código = la que hoy muestra la pantalla, que es la que el personal ha venido
corrigiendo. El bloque 0 del 379 lo confirma con datos.)

- **BE:** un solo punto: `DireccionDaoService.selectDomicilioVigente(Long idEntidad)` (JPQL/SQL con la regla) y
  `GET /rest/drcc/domicilioVigente/{idEntidad}` (204 si no tiene). `getByParent` pasa a tener ORDER BY
  (`porDefecto desc, codigo asc`) por coherencia.
- **Al guardar** una dirección de domicilio con `porDefecto = 1` (`DireccionServiceImpl`, alta o edición), las demás
  domiciliarias activas de la misma entidad quedan con `DRCCPRDF = 0`, en la misma transacción. Así hay una sola.
- **Reportes:** `RPRT_PRST_SLCT` y `RPRT_PRST_PGRE` cambian el subselect a la regla (`DRCCCDGO` **asc**). Con el
  `.jasper` recompilado y el fill verificado (CLAUDE.md; el pagaré y la solicitud usan `NumeroALetrasUtil`: compilar con
  esa clase en el classpath).
- **Contrato de adhesión:** `ContratoAdhesionParametros` usa `selectDomicilioVigente`.
- **Otros que escriben `CRD.DRCC`** (app móvil, actualización de datos, migraciones): el BE los lista. Si alguno crea una
  dirección nueva por defecto en vez de actualizar la vigente, se reporta al árbitro antes de cambiarlo (puede ser
  la «actualización de datos» que menciona el socio).

## 3. Texto de «Dirección domicilio» en la solicitud
Lo que muestra la pantalla, en este orden y saltando lo vacío:
`{calle principal} {número} y {calle secundaria}, {barrio}, {descripción/referencia}, {CIUDAD}`.
El campo del `.jrxml` se estira (`textAdjust="StretchHeight"`) para que no se corte. El pagaré conserva su columna
`CIUDAD`, ahora resuelta como en §4.

## 4. Ciudad por combo
- Hoy la ciudad solo se alcanza por `DRCC.PRRQCDGO → CRD.PRRQ.CDDDCDGO → CRD.CDDD` (parroquia), y la pantalla no pide
  parroquia ni ciudad.
- **DDL (379):** `CRD.DRCC.CDDDCDGO NUMBER` FK a `CRD.CDDD(CDDDCDGO)`, nullable, con índice. Respaldo de la ciudad
  desde la parroquia donde exista. Entidad `Direccion`: `@ManyToOne Ciudad ciudad` (`CDDDCDGO`).
- **Ciudad efectiva** en reportes y contrato: `NVL(DRCC.CDDDCDGO, PRRQ.CDDDCDGO)`.
- **FE, pestaña Dirección:** combo «Ciudad» con búsqueda, desde `ciudad.service` (`CRD.CDDD`), mostrando
  «CIUDAD (PROVINCIA)», ordenado por nombre. **Obligatorio al guardar** la dirección. También en la edición rápida
  (`entidad-edit`) si ahí se edita la dirección.
- Si el catálogo `CRD.CDDD` está vacío o incompleto (bloque 0 del 379), primero se carga; sin eso el combo no sirve.

## 5. Datos existentes
Los socios con más de una domiciliaria por defecto salen en el bloque 0. Cuando el BE esté desplegado, guardar la
dirección en la pantalla deja una sola. Si son muchos, un script aparte (380) los normaliza con la regla de §2, con
respaldo; se decide con el resultado del bloque 0.

## 6. Verificación
`mvn -q compile`, `npx ng build`; `compilar-jasper.bat` + `verificar-fill-jasper.bat` de los dos reportes, y un PDF
real por el mismo camino del servicio, buscando la dirección y la ciudad. Prueba con el socio del reclamo: la solicitud
debe mostrar la dirección de la pantalla.

## 7. Etiqueta de la capacidad de pago (mismo reporte, pedido del usuario 2026-11-16)
En `RPRT_PRST_SLCT.jrxml` (~línea 423), sección de capacidad de pago: «Ingreso mensual (rol)» pasa a **«Ingreso neto
(rol)»**. Solo cambia la etiqueta; el valor no cambia. Va en la misma recompilación del `.jasper`.
