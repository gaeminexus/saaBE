# Prueba manual — recargar un extracto bancario

**2026-09-09.** No hay forma de armar un harness automático para esto (a diferencia de los
`.jasper`, que se pueden llenar en memoria sin Oracle): `recargar` es un EJB `@Stateless` con DAOs
inyectados por contenedor, transacción JTA real y varias tablas encadenadas (`EXBC`, `DEXB`,
`GCEX`, `GRCC`, `DTCN`, `CNCT`, `CTEB`). El repo no tiene suite de tests (ver `CLAUDE.md`), así que
esta es la única prueba real posible: correrla a mano contra una base con datos de prueba, antes de
soltar la funcionalidad a producción.

Este documento da los pasos exactos. Todas las rutas son `/rest/...` (ver `CLAUDE.md` § Capa REST).

## 0. Preparación

Necesita, ya existentes en la base de prueba:
- Una `CuentaBancaria` (`idCuentaBancaria`) con un `Banco` cuyo parser esté implementado
  (`BankStatementParserFactory`).
- Un `Periodo` contable ABIERTO (`idPeriodo`) que cubra las fechas del archivo de prueba.
- Al menos un `DetalleAsiento` sobre esa cuenta bancaria, con un monto que cuadre contra alguna
  fila del extracto de prueba (para poder conciliar en el paso 2).
- Un archivo de extracto de prueba real para esa cuenta (el mismo que ya use para probar el
  parser correspondiente).

## 1. Cargar el extracto por primera vez

```
POST /rest/exbc/importar/confirmar/{idCuentaBancaria}
Content-Type: multipart/form-data
  archivo, archivoNombre, idPeriodo, idEmpresa, usuarioCreacion
```

Anote el `codigo` del `ExtractoBancario` devuelto (`idExtractoOriginal`).

**Verificar:** `GET /rest/exbc/getId/{idExtractoOriginal}` devuelve el extracto con sus `DEXB`
(`GET /rest/dexb/selectByCriteria` filtrando por `extractoBancario.codigo`, o el listado que use la
pantalla). Anote el `codigo` de al menos una fila de detalle (`idDetalleParaConciliar`).

## 2. Conciliar una fila

```
GET /rest/cnct/cabecera/{idCuentaBancaria}/{idPeriodo}
```
Crea (si no existía) la cabecera `ConciliacionContable` — anote su `codigo` (`idConciliacion`).

```
POST /rest/cnct/conciliar
Content-Type: application/json
{
  "idCuentaBancaria": ...,
  "idPeriodo": ...,
  "idsDetalleExtracto": [idDetalleParaConciliar],
  "idsDetalleAsiento": [<el DetalleAsiento que cuadra>],
  "usuario": "prueba"
}
```
Anote el `codigo` del `GrupoConciliacionContable` devuelto (`idGrupo`).

**Verificar:** `GET /rest/cnct/grupos/{idConciliacion}` incluye `idGrupo` con `estado = 1` (Activo).

## 3. Intentar recargar con una fila conciliada → DEBE FALLAR (guarda 2)

```
POST /rest/exbc/recargar/{idCuentaBancaria}/{idPeriodo}
Content-Type: multipart/form-data
  archivo (el mismo archivo u otro), archivoNombre, idEmpresa, usuarioCreacion
```

**Esperado:** `500` con un mensaje que empieza con *"No se puede recargar: 1 movimiento(s) de este
extracto ya estan conciliados (en un grupo de conciliacion activo). Deshaga esas conciliaciones
antes de recargar."* — y que **nada se haya borrado**: `GET /rest/exbc/getId/{idExtractoOriginal}`
sigue devolviendo el extracto original sin cambios.

## 4. Deshacer la conciliación

```
POST /rest/cnct/deshacer/{idGrupo}
Content-Type: application/json
{"usuario": "prueba"}
```

**Verificar:** `GET /rest/cnct/grupos/{idConciliacion}` ya NO incluye `idGrupo` (queda con
`estado = 0`, filtrado por `selectActivosByConciliacion`). El `GRCC` sigue existiendo en la base
(no se borra) — si hay un `getId` genérico para `GrupoConciliacionContable`, confirmarlo ahí.

## 5. Intentar recargar de nuevo → DEBE FUNCIONAR

```
POST /rest/exbc/recargar/{idCuentaBancaria}/{idPeriodo}
Content-Type: multipart/form-data
  archivo, archivoNombre, idEmpresa, usuarioCreacion
```

**Esperado:** `200 OK` con un `ResumenImportacionExtracto` donde:
- `idExtractoAnterior` = `idExtractoOriginal`.
- `idExtractoCreado` = un código nuevo, distinto del original.
- `saldoInicial`/`saldoFinal`/`totalFilas`/`advertencias` corresponden al archivo recién subido.

**Verificar después:**
- `GET /rest/exbc/getId/{idExtractoOriginal}` → `404 Not Found` (la cabecera vieja ya no existe).
- `GET /rest/exbc/getId/{idExtractoCreado}` → `200`, con el extracto nuevo.
- `GET /rest/exbc/getByHash/{hash del archivo}` → devuelve el extracto nuevo (no el viejo — el
  hash del viejo desapareció con el borrado).
- El `GRCC` del paso 2 (`idGrupo`) sigue existiendo (`estado = 0`), pero sus filas `GCEX` ya no
  (se borraron en cascada junto con el `DEXB` que apuntaban). Si hay forma de listarlas
  (`GET /rest/gcex/selectByCriteria` filtrando por `grupo.codigo = idGrupo`), debería devolver una
  lista vacía.
- Si la `ConciliacionContable` de esta cuenta/período existía, sus contadores
  (`totalPendientesExtracto`, etc.) reflejan el extracto nuevo, no el viejo.
- **Subir el mismo archivo una segunda vez** con `/importar/confirmar` (no `/recargar`) debe volver
  a fallar con "Este archivo ya fue cargado previamente" — el hash del extracto RECIÉN CREADO sí
  bloquea, solo el del extracto borrado dejó de hacerlo.

## 6. El período cerrado bloquea tanto cargar como recargar

```
PUT /rest/cteb
Content-Type: application/json
{ ...el ControlExtractoBancario de esa empresa/período, con "cerrado": 1 }
```
(si no existe todavía, generarlo primero con `POST /rest/cteb/generar/{idEmpresa}/{idPeriodo}`).

```
POST /rest/exbc/recargar/{idCuentaBancaria}/{idPeriodo}
```

**Esperado:** `500` con *"El periodo '...' ya esta cerrado para conciliacion bancaria. No se
pueden cargar nuevos extractos."* (mismo mensaje que ya da `/importar/confirmar` — `recargar`
reusa el mismo chequeo). Nada debe haberse borrado: `GET /rest/exbc/getId/{idExtractoCreado}`
sigue devolviendo el extracto sin cambios.

No olvide volver a abrir el período (`"cerrado": 0`) al terminar la prueba si la base es compartida.

## 7. Opcional — el caso de la partida en tránsito

Si el escenario de prueba tiene una NC/ND del banco no registrada que quedó declarada en tránsito
sobre una fila de este extracto (ver `DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`), repetir el
paso 3 debería fallar con el mensaje de la guarda 3 ("...estan declarados como partida en transito
pendiente...") en vez del de conciliación. Este caso es más difícil de armar a mano (requiere un
cierre de conciliación completo primero) — si no hay tiempo, se puede omitir; las guardas 2, 4 y 5
ya cubren los caminos que el usuario pidió explícitamente.
