# Análisis — filtrado en servidor para las pantallas de consulta

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-08 · **Encargo:** `omen-saa-2-arb`
**Estado:** análisis puro, nada implementado todavía. Basado en
`docs/pendientes/INVENTARIO-CONSULTA-Y-ANULACION.md` (relevamiento de `omen-saa-2-fe`, §7.2), que
identificó al menos diez pantallas de `tsr`/`cxc`/`cxp` que traen el listado completo y filtran en
el navegador.

---

## 0. 🔴 El hallazgo que cambia el marco del pedido

**No hay que escribir diez endpoints nuevos.** Verificado leyendo cada `*Rest.java`, no asumido por
convención: el endpoint genérico `POST .../selectByCriteria` (el estándar de seis endpoints por
tabla que documenta `CLAUDE.md`, sobre `EntityDaoImpl.selectByCriteria`) **ya existe** para
prácticamente todas las entidades detrás de estas pantallas, y **cinco de las diez pantallas del
inventario ya lo llaman desde el frontend hoy mismo** — con un criterio real armado como
`DatosBusqueda[]` (normalmente sólo "empresa"), no con un `getAll()` a secas.

Es decir: para la mayoría de los casos, el trabajo no es de backend. Es que el frontend arme el
array de `DatosBusqueda` completo (agregando los criterios que la pantalla ya le pide al usuario)
en vez de traer todo y filtrar en JavaScript. El backend que hace falta ya está desplegado y
probado — lo usan otras pantallas del mismo módulo.

Esto **no** es válido para todos los casos: dos de las doce pantallas de este documento (**Consulta
y Gestión de Pagos**, **Egresos**) necesitan algo que `selectByCriteria` no puede expresar, y ahí sí
hay trabajo de backend real. El porqué exacto está en cada sección.

---

## 1. Tabla completa — las doce pantallas

| Pantalla | Módulo | Endpoint que la alimenta hoy | Parámetros que acepta hoy | Qué filtra el front en cliente | Qué habría que agregarle |
|---|---|---|---|---|---|
| **Consulta y Gestión de Pagos** | cxp | `GET /pgtr/listar` (explícito, ya extendido ayer) | `idEmpresa`, `estado` (repetible), `idTitular`, `idCuentaBancaria`, `origen` (repetible), `desde`, `hasta`, `texto` | **`formaPago`** (tipo de pago) — es el único de los seis filtros de la pantalla que no llegué a cubrir ayer | Agregar `formaPago` a `/pgtr/listar` (parámetro + condición `p.formaPago = :formaPago`) — columna real, `PGTRFPAG`, sin ninguna trampa como la de Egresos |
| **Egresos** | tsr | `GET /egrs/listar` (explícito) | `idEmpresa`, `estado` | Beneficiario (titular), concepto, **tipo de pago**, fecha — los cuatro | **No es un simple `@QueryParam` más.** `Egreso.formaPago` es `@Basic`/persistido en `TSR.EGRS`… **no, es `@Transient`**: se resuelve en memoria desde el `PagoProgramado` más reciente del egreso, en `EgresoServiceImpl.listar` (ya con una deuda conocida, `completaFormaPago`, `IN :ids` sin techo — ver §2 del inventario general y `docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md` §12). Filtrar por tipo de pago server-side exige un `JOIN`/subconsulta contra `PGS.PGTR`, no una columna de `TSR.EGRS`. Beneficiario/concepto/fecha sí son columnas reales (`EGRSTTLR`, `EGRSDSCR`, `EGRSFCHA`) y no tienen esa complicación |
| Consulta de Extractos Bancarios | tsr | `extractoBancarioService.getAll()` (el listado principal; el detalle de un extracto ya elegido sí usa `selectByCriteria`) | ninguno | Todo — cero filtros server-side en el listado | **Nada de backend**: `POST /exbc.../selectByCriteria` (`TSR.EXBC`) ya existe. Cambiar `getAll()` por `selectByCriteria` en el frontend, con criterios sobre `CNBCCDGO` (cuenta), `EXBCFDSD`/`EXBCFHST` (rango de fechas del extracto), `EXBCESTP` (estado) |
| Grupos de Productos | cxp | `grupoService.selectByCriteria([empresa])` | ya server, un solo criterio | posible filtro adicional en memoria sobre el resultado — no confirmado a fondo (módulo de parametrización, pocas filas, prioridad baja) | Ampliar el array de criterios en el propio componente, si hace falta |
| Datos SRI | cxp | `listadoService.selectByCriteria(criterios)` | ya server | no confirmado a fondo | Mismo patrón: revisar si el criterio ya cubre lo que el formulario ofrece |
| Bandeja Electrónica | cxp | `cargaService.selectByCriteria(criterios)` | ya server, un criterio (período) | el inventario dice "solo período" | Ampliar el array de criterios (frontend, no backend) |
| Gestión de Documentos | cxp | `cargaTxtService.selectByCriteria(...)` + `docService.selectByCriteria(...)` | ya server | El inventario general dice "filtra en cliente pese a tener 4 campos"; al leer el componente esa afirmación no se sostiene igual — ya llama `selectByCriteria`. **Punto a confirmar**: si el filtrado posterior que vio el inventario es sobre datos ya acotados por el servidor (una búsqueda de UI sobre la tabla, sin importancia) o si hay un segundo filtro real en memoria | Confirmar el punto de arriba antes de decidir si hace falta algo |
| Negociaciones | cxp | `negService.selectByCriteria(criterios)` | ya server | no confirmado a fondo | Probablemente nada |
| Anticipos de Cliente | cxc | `anticipoService.getAll()` | ninguno | Todo — cero filtros, ni servidor ni cliente | **Nada de backend**: `POST /antc/selectByCriteria` (`CBR.ANTC`) ya existe. Cambiar `getAll()` por `selectByCriteria`, con criterios sobre `TITULAR`, `FECHAANTICIPO`, `ESTADO` |
| Documentos Electrónicos | cxc | 5 llamadas `getAll()` en paralelo (`facturaService`, `ncService`, `ndService`, `retService`, `liquidacionService`), `forkJoin`, normaliza y filtra **todo** en memoria (`aplicarFiltros`: estado, autorización, cliente por texto, fecha desde/hasta) | ninguno, en las cinco | Todo: estado, autorización, cliente, fecha — en las cinco entidades | **Nada de backend**: las cinco (`Factura`/`NotaCreditoCompra`/`NotaDebitoCompra`/`RetencionV2`/`LiquidacionCompra`) tienen `selectByCriteria` confirmado. Es el caso de mayor volumen del lote (cinco servicios en paralelo con `getAll()`) y, a la vez, el más barato: sólo frontend |
| Consulta de Facturas | cxc | `facturaService.getAll()` | ninguno | Todo | **Nada de backend** — mismo `selectByCriteria` de `Factura` (`CBR.FCTR`) que usa Documentos Electrónicos. El inventario ya marca esta pantalla como candidata a **eliminar por duplicada**, no sólo a arreglar |
| Financiar Factura | cxc | `facturaService.getAll()` | ninguno | Todo | **Nada de backend** — idéntico a Consulta de Facturas |
| Liquidaciones (emitir) | cxc | `service.getAll()` para el listado principal (catálogos del formulario — grupo de producto, punto de emisión, etc. — también `getAll()`, pero son catálogos chicos, no el problema) | ninguno | Todo, sobre el listado principal | **Nada de backend**: `selectByCriteria` de `LiquidacionCompra` ya existe (confirmado junto con Factura/NC/ND/RetV2 arriba) |

---

## 2. Las dos prioritarias, en detalle

### 2.1 Consulta y Gestión de Pagos (`GET /pgtr/listar`)

Ya cubre cinco de los seis filtros de la pantalla (proveedor → `idTitular`, fecha → `desde`/`hasta`,
concepto → `texto`, estado → `estado`). **Falta uno solo: `formaPago`.**

Es una columna real y directa (`PagoProgramado.formaPago`, `PGTRFPAG`, `Long`) — agregarla es
exactamente el mismo patrón que los demás parámetros opcionales que ya tiene el método: un
`@QueryParam` más, una condición `and p.formaPago = :formaPago` más, sin ninguna trampa de las que
ya resolvimos ahí (origen polimórfico, paréntesis del `OR`, etc.). Es el ítem de backend más barato
de todo este documento.

### 2.2 Egresos (`GET /egrs/listar`)

**No es un simple parámetro más, y hay que decirlo antes de que alguien lo trate como tal.**

`Egreso.formaPago` está anotado `@Transient` en la entidad — la clase lo dice explícito en su
javadoc: *"No es una columna de TSR.EGRS: el egreso solo guarda debitoAutomatico como espejo; la
forma de pago real (incluido cheque) vive en PGS.PGTR. Lo puebla `EgresoServiceImpl.listar` en una
sola consulta por página, no se persiste."* O sea que hoy el tipo de pago se resuelve **después**
de traer los egresos, con una consulta aparte contra `PagoProgramado` por lote de ids
(`completaFormaPago`, que además tiene una deuda documentada: el `IN :ids` sin techo de más de 1000
elementos — la misma familia de riesgo que ya corregimos esta semana en
`PagoProgramadoDaoServiceImpl.selectByAsientos`, pero sin resolver acá).

Filtrar por tipo de pago en el servidor exige, como mínimo, un `JOIN` (o subconsulta `EXISTS`)
contra `PGS.PGTR` desde la consulta de `TSR.EGRS` — cambia la forma de la consulta, no es agregar un
`and` más sobre una columna propia. Los otros tres filtros (beneficiario → `EGRSTTLR`, concepto →
`EGRSDSCR`, fecha → `EGRSFCHA`) sí son columnas directas de `TSR.EGRS`, sin esta complicación.

**Recomendación para cuando se implemente:** separar el trabajo en dos — los tres filtros directos
primero (barato, mismo patrón que `/pgtr/listar`), y el de tipo de pago aparte, diseñando el `JOIN`
con cuidado de no repetir el problema del `IN` sin techo.

---

## 3. Pregunta 1 — ¿`selectByCriteria` genérico o endpoints explícitos?

**Mi opinión: los dos, y ya está decidido cuál usar en cada caso por una regla simple, no por
gusto.**

No es una elección teórica — el propio relevamiento la responde con datos: `selectByCriteria` **ya
es el patrón dominante**, desplegado y en uso en al menos ocho de las doce pantallas de este
documento. No hay que decidir "cuál instaurar", hay que decidir cuándo el explícito se justifica
frente a ese default.

**Usar `selectByCriteria` cuando el filtro es un campo directo (o una relación de un solo salto) de
una sola entidad**, que es el caso de casi todo este lote: titular, fecha, estado, concepto, número
de documento. Ahí el genérico gana en todos los ejes que importan:
- **Cero código nuevo de backend** — literalmente el caso de nueve de las doce pantallas de arriba.
- Ya tiene el cache de operadores de ayer, así que el argumento de "es más lento" ya no aplica igual
  que antes.
- Es el patrón que ya domina el código existente: sumar una décima pantalla al mismo mecanismo no
  agrega superficie nueva a mantener, extiende una que ya existe.

**Usar un endpoint explícito (el patrón de `/pgtr/listar`, `/egrs/listar`) cuando pasa alguna de
estas tres cosas** — que es exactamente por qué esos dos ya son explícitos, no por preferencia:

1. **El filtro no es una columna directa, es una regla de negocio sobre asociaciones.** El caso de
   `origen` en `PagoProgramado`: no es `p.origen = :valor`, es `p.facturaCompra is not null OR
   p.egreso is not null OR ... OR p.origenExterno = :valor` con paréntesis explícitos —
   `selectByCriteria` arma JPQL genérico campo-por-campo, no puede expresar esa traducción.
2. **El dato vive en OTRA entidad y hay que resolverlo aparte.** El caso de `formaPago` en
   `Egreso`, que es `@Transient` y sale de un `JOIN`/subconsulta contra `PagoProgramado` — otra vez,
   fuera del alcance de "filtrar por un campo de la entidad".
3. **Hace falta devolver una proyección liviana, no la entidad completa.** Ninguna de las doce
   pantallas de este documento lo necesita hoy (el precedente real es `PagoPorAprobar`, para la
   bandeja de aprobación) — lo menciono porque es el tercer motivo legítimo, no porque aplique acá.

**Contraparte, para que quede el argumento completo:** `selectByCriteria` devuelve la entidad
completa con sus relaciones — más pesado por fila que una proyección a medida — y el contrato
`DatosBusqueda[]` es más verboso de armar en Angular que query params planos. Para las doce
pantallas de este lote, ninguna maneja volúmenes que hagan de esto un problema real (a diferencia de
`/pgtr/lotes` o `/pgtr/listar`, que si tocan miles de filas). Si alguna resulta pesada en la
práctica, ahí se justifica un endpoint explícito por el motivo 3, no antes.

---

## 4. Pregunta 2 — Índices

### 4.1 Columnas candidatas, por tabla

| Tabla | Columnas para filtro | Verificado |
|---|---|---|
| `SCP.PDTR` | `PDTRALTR`, `PRBRCDGO` | Pendiente de esta sesión anterior — incluido abajo |
| `SCP.PRBR` | `PRBRALTR` | Ídem |
| `PGS.PGTR` | `PGTRTTLR` (titular), `PGTRCNBC` (cuenta origen), `PGTRFPRG` (fecha programada), `PGTRESTD` (estado), `PGTRORGN` (origen), **`PGTRFPAG`** (forma de pago, nuevo con el ítem 2.1) | Sí, entidad leída completa |
| `TSR.EGRS` | `EGRSTTLR` (titular), `EGRSFCHA` (fecha), `EGRSESTD` (estado), `EGRSDSCR` (concepto, si se busca por texto) | Sí |
| `TSR.EXBC` | `CNBCCDGO` (cuenta bancaria), `EXBCFDSD`/`EXBCFHST` (rango de fechas), `EXBCESTP` (estado) | Sí |
| `CBR.FCTR` | `COMPRADOR` (titular — FK), `FECHA`, `ESTADO` | Sí — **⚠️ esta tabla no sigue la convención de 8 caracteres del resto del sistema** (columnas `FECHA`/`ESTADO`/`COMPRADOR` a secas, no `FCTRFCHA`/`FCTRESTD`/`FCTRTTLR`) |
| `CBR.ANTC` | `TITULAR`, `FECHAANTICIPO`, `ESTADO` | Sí — mismo estilo mixto que `FCTR` (conviven `TITULAR`/`ESTADO` sin prefijo con `ANTCSALD`/`ANTCFPAG`/`ANTCREFR` que sí lo llevan) |
| `PGS.NTCC` (NotaCreditoCompra) | — | **No verificado.** Asumible el mismo estilo que `FCTC`/`FCTR` por ser documentos hermanos, pero no confirmado columna por columna — no adivinar antes de escribir el `CREATE INDEX` |
| `PGS.NTDC` (NotaDebitoCompra) | — | No verificado, mismo motivo |
| `LiquidacionCompra`, `RetencionV2` | — | No verificado |

### 4.2 Script de solo lectura

Sin `PROMPT`/`SET`/`COLUMN`; la etiqueta va como columna literal del propio `SELECT`, para que el
cliente del usuario la muestre como dato y no intente interpretarla como comando.

```sql
-- ============================================================
-- Verificacion de indices para las columnas de filtro de las
-- pantallas de consulta (tsr/cxc/cxp) y del catalogo de rubros.
-- Solo lectura. No modifica nada.
-- ============================================================

-- 1) Catalogo de rubros (pendiente de una sesion anterior)
SELECT 'INDICE POR COLUMNA - RUBROS' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  (ic.table_owner, ic.table_name, ic.column_name) IN (
         ('SCP', 'PDTR', 'PDTRALTR'),
         ('SCP', 'PDTR', 'PRBRCDGO'),
         ('SCP', 'PRBR', 'PRBRALTR')
       )
ORDER BY ic.table_name, ic.column_name, ic.column_position;

-- 2) Pagos programados (cxp) - incluye PGTRFPAG para cuando se agregue el filtro
SELECT 'INDICE POR COLUMNA - PGS.PGTR' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  ic.table_owner = 'PGS' AND ic.table_name = 'PGTR'
       AND ic.column_name IN ('PGTRTTLR', 'PGTRCNBC', 'PGTRFPRG', 'PGTRESTD', 'PGTRORGN', 'PGTRFPAG')
ORDER BY ic.column_name, ic.column_position;

-- 3) Egresos (tsr)
SELECT 'INDICE POR COLUMNA - TSR.EGRS' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  ic.table_owner = 'TSR' AND ic.table_name = 'EGRS'
       AND ic.column_name IN ('EGRSTTLR', 'EGRSFCHA', 'EGRSESTD', 'EGRSDSCR')
ORDER BY ic.column_name, ic.column_position;

-- 4) Extractos bancarios (tsr)
SELECT 'INDICE POR COLUMNA - TSR.EXBC' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  ic.table_owner = 'TSR' AND ic.table_name = 'EXBC'
       AND ic.column_name IN ('CNBCCDGO', 'EXBCFDSD', 'EXBCFHST', 'EXBCESTP')
ORDER BY ic.column_name, ic.column_position;

-- 5) Facturas de venta (cxc) - ojo, nombres de columna sin prefijo de 8 caracteres
SELECT 'INDICE POR COLUMNA - CBR.FCTR' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  ic.table_owner = 'CBR' AND ic.table_name = 'FCTR'
       AND ic.column_name IN ('COMPRADOR', 'FECHA', 'ESTADO')
ORDER BY ic.column_name, ic.column_position;

-- 6) Anticipos de cliente (cxc)
SELECT 'INDICE POR COLUMNA - CBR.ANTC' AS reporte,
       ic.table_owner, ic.table_name, ic.column_name,
       ic.index_name, ic.column_position
FROM   all_ind_columns ic
WHERE  ic.table_owner = 'CBR' AND ic.table_name = 'ANTC'
       AND ic.column_name IN ('TITULAR', 'FECHAANTICIPO', 'ESTADO')
ORDER BY ic.column_name, ic.column_position;

-- 7) Listado completo de indices de las tablas de arriba, por si el indice
--    existe compuesto y no aparece con el nombre de columna esperado
SELECT 'TODOS LOS INDICES' AS reporte,
       i.owner, i.table_name, i.index_name, i.uniqueness, i.status
FROM   all_indexes i
WHERE  (i.owner, i.table_name) IN (
         ('SCP', 'PDTR'), ('SCP', 'PRBR'),
         ('PGS', 'PGTR'), ('TSR', 'EGRS'), ('TSR', 'EXBC'),
         ('CBR', 'FCTR'), ('CBR', 'ANTC')
       )
ORDER BY i.table_name, i.index_name;
```

No lo corrí — no ejecuto SQL. Falta una segunda pasada, más corta, para confirmar las columnas de
`NTCC`/`NTDC`/`LiquidacionCompra`/`RetencionV2` antes de que ese `CREATE INDEX` se escriba; no las
adiviné acá.

---

## 5. Lo que no se resolvió en esta pasada

- **Grupos de Productos, Datos SRI, Negociaciones**: no se verificaron sus columnas a fondo (módulos
  de parametrización, poco volumen, prioridad baja según el propio inventario). El criterio que ya
  arman hoy (`selectByCriteria` con un filtro de empresa) puede alcanzar sin cambios.
- **Gestión de Documentos**: hay una contradicción entre lo que dice el inventario general ("filtra
  en cliente pese a tener 4 campos") y lo que se vio al leer el componente (ya llama
  `selectByCriteria`). Antes de tocar nada ahí, hay que confirmar si el filtrado adicional que vio
  el inventario es sobre datos ya acotados por el servidor (una búsqueda de UI sobre la tabla, sin
  importancia) o un segundo filtro real en memoria.
