# Reglas de la migración de apertura — RRHH

**Cliente:** ASOPREP-FCPC · **Fecha de corte:** 31-dic-2025 · **Documento vigente desde:** 2026-08-19

Este documento es la referencia del proceso de carga de saldos iniciales del módulo de RRHH.
Cubre el formato del archivo, qué materializa cada tipo de saldo y cómo se revierte.

> **Regla de mantenimiento:** cualquier cambio en `MigracionRhhServiceImpl`, en el formato del
> archivo o en el mapa de materialización debe actualizar este documento en el mismo cambio.

---

## 1. Los cuatro pasos

El proceso está deliberadamente partido en cuatro, para que nada se materialice sin revisión previa.

| Paso | Endpoint | Qué hace | Qué toca |
|---|---|---|---|
| 1 · Cargar | `POST /rest/slap/cargar` | Lee el archivo e inserta filas en `RHH.SLAP` | Solo `SLAP` |
| 2 · Validar | `GET /rest/slap/validar?idEmpresa=&fechaCorte=` | Contrasta los `SLAP` contra el maestro | Nada |
| 3 · Aplicar | `POST /rest/slap/aplicar` | Materializa en las tablas operativas | `MPLD`, `SLDV`, `ACMN`, `DSRC`, `CTDS` |
| 4 · Revertir | `POST /rest/slap/revertir` | Deshace la materialización | Las mismas |

**Aplicar rechaza la ejecución si validar devuelve alguna inconsistencia.** No hay forma de
materializar saltándose la validación, y es intencional.

**Aplicar es idempotente.** Un `SLAP` con `SLAPAPLC = 'S'` se salta. Volver a ejecutar el paso 3
no duplica nada.

### Cuerpos de petición

```jsonc
// POST /rest/slap/cargar  — multipart/form-data
//   archivo          : el CSV
//   idEmpresa        : código de la empresa
//   fechaCorte       : yyyy-MM-dd
//   usuarioRegistro  : usuario de sesión

// POST /rest/slap/aplicar  y  POST /rest/slap/revertir  — application/json
{ "idEmpresa": 1, "fechaCorte": "2025-12-31", "usuarioRegistro": "mvaca" }
```

`cargar`, `aplicar` y `revertir` devuelven un número; `validar` devuelve `List<String>`, y la
lista vacía significa que se puede aplicar.

---

## 2. Formato del archivo

CSV delimitado por **punto y coma**, codificado en **UTF-8**, con **una línea de cabecera** que
se salta. Fechas en `dd/MM/yyyy`. Decimales con punto o coma, ambos se aceptan.

```
identificacion;tipoSaldo;valor;dias;fecha;anio;numeroCuotas;numeroReferencia;observacion
```

| # | Columna | Tipo | Obligatoria |
|---|---|---|---|
| 1 | `identificacion` | texto | **Siempre** |
| 2 | `tipoSaldo` | entero, código alterno del rubro 211 | **Siempre** |
| 3 | `valor` | decimal | Según tipo |
| 4 | `dias` | decimal | Según tipo |
| 5 | `fecha` | `dd/MM/yyyy` | Según tipo |
| 6 | `anio` | entero | Según tipo |
| 7 | `numeroCuotas` | entero | Solo préstamos |
| 8 | `numeroReferencia` | texto | No |
| 9 | `observacion` | texto | No |

**Una línea mal formada aborta toda la carga.** Se acumulan todos los errores y se devuelven
juntos, con el número de línea. Entra el archivo entero o no entra nada.

### Ejemplo

```
identificacion;tipoSaldo;valor;dias;fecha;anio;numeroCuotas;numeroReferencia;observacion
1712345678;1;;;15/06/2025;;;;Ingreso por cambio de administracion
1712345678;2;;12.5;;2025;;;
1712345678;3;1450.00;;;2025;;;
1712345678;6;2400.00;;;2025;12;QG-889012;
```

---

## 3. Qué materializa cada tipo de saldo

Los tipos son códigos alternos del rubro `RHH_TIPO_SALDO_APERTURA` (211).

| Tipo | Nombre | Destino | Campos que usa |
|---|---|---|---|
| 1 | `ANTIGUEDAD` | `MPLD.MPLDFCIN` | `fecha` |
| 2 | `VACACIONES_PENDIENTES` | Un `SLDV` con `SLDVAPRT='S'` | `dias`, `anio` |
| 3 | `DECIMO_TERCERO_ACUMULADO` | `ACMN` tipo `BASE_DECIMO_TERCERO` | `valor`, `anio` |
| 4 | `DECIMO_CUARTO_ACUMULADO` | `ACMN` tipo `BASE_DECIMO_CUARTO` | `valor`, `anio` |
| 5 | `FONDOS_DE_RESERVA_ACUMULADOS` | `ACMN` tipo `BASE_FONDOS_DE_RESERVA` | `valor`, `anio` |
| 6 | `PRESTAMO_IESS` | `DSRC` tipo quirografario + sus `CTDS` | `valor`, `numeroCuotas` |
| 7 | `PRESTAMO_INTERNO` | `DSRC` tipo préstamo interno + sus `CTDS` | `valor`, `numeroCuotas` |
| 8 | `IR_RETENIDO_EN_EL_ANIO` | `ACMN` tipo `RETENCION_IR` | `valor`, `anio` |

Los `ACMN` de apertura se crean **sin período de nómina** (`PRDNCDGO` nulo) y anclados al mes de
la fecha de corte. Todos llevan su bandera de apertura (`ACMNAPRT`, `SLDVAPRT`, `DSRCAPRT`) en
`'S'`, para distinguirlos de los que genera el proceso normal.

### Generación de cuotas de préstamo

`valorCuota = saldo / numeroCuotas`, redondeado a dos decimales con `RedondeoNomina`. Los
vencimientos son mensuales a partir de la fecha de corte. **La última cuota absorbe la diferencia
de redondeo**, de modo que la suma de cuotas iguale exactamente el saldo. Todas nacen en
`RhhEstadoCuotaDescuento.PENDIENTE` con `CTDSVLDS = 0`.

**El concepto de nómina con el que se descontará el préstamo no viene en el archivo:** sale de
`CPNMROLM`, el rol que el catálogo declara (rubro 221). El tipo de saldo se traduce al rol
—`PRESTAMO_IESS` → `PRESTAMO_QUIROGRAFARIO`, `PRESTAMO_INTERNO` → `PRESTAMO_INTERNO`— y se busca
el concepto que lo tenga asignado.

---

## 4. Validaciones

| # | Comprueba |
|---|---|
| 1 | Que exista **un y solo un** empleado con esa identificación en la empresa |
| 2 | Que el tipo de saldo esté informado y corresponda a un detalle del rubro 211 |
| 3 | Que ni el valor ni los días sean negativos |
| 4 | Que estén los campos obligatorios del tipo (ver la tabla de arriba) |
| 5 | Que exista un concepto de `RHH.CPNM` con el rol del motor que corresponde al préstamo |
| 6 | Que no haya duplicados de `(identificación, tipoSaldo, año)` dentro del corte |

Una identificación que devuelve más de un empleado se reporta como inconsistencia, no se resuelve
eligiendo uno.

---

## 5. Reversión

Cada materialización graba **`SLAPRFTB`** (nombre de la tabla destino) y **`SLAPRFID`** (id del
registro creado). La reversión los lee y deshace exactamente lo que se hizo:

| `SLAPRFTB` | Qué deshace |
|---|---|
| `RHH.MPLD` | Pone `MPLDFCIN` en nulo |
| `RHH.SLDV` | Borra el saldo de vacaciones |
| `RHH.ACMN` | Borra el acumulado |
| `RHH.DSRC` | Borra primero las `CTDS` hijas y después la cabecera |

Un `SLAP` aplicado sin rastro (`SLAPRFTB` o `SLAPRFID` nulos) **no se revierte automáticamente**:
se registra en el log para revisión manual, porque adivinar qué borrar sería peor que no hacerlo.

Tras revertir, el saldo queda con `SLAPAPLC = 'N'` y sus campos de rastro limpios, listo para
volver a aplicarse.

---

## 6. Los dos huecos que tenía este proceso, ya cerrados

**Ambos se resolvieron el 2026-08-19.** Se dejan documentados porque explican por qué el
formato del archivo cambió respecto de la primera versión.

### 6.1 `SLAP` no tenía dónde indicar el concepto de nómina — resuelto por rol

`RHH.DSRC.CPNMCDGO` es `NOT NULL`, pero `SLAP` no tiene esa columna. La primera versión
pedía una columna `codigoConcepto` en el archivo y la guardaba en `SLAPOBSR` con el prefijo
`CPNM=`: funcionaba y no quemaba nada, pero era un rodeo.

**Se descartó añadir `SLAP.CPNMCDGO`.** En su lugar el concepto se resuelve por
**`CPNM.CPNMROLM`** (rubro 221), que es la misma vía por la que el motor de nómina localiza
sus conceptos. `aplicarSaldosApertura` traduce el tipo de saldo al rol y busca el concepto
que lo tenga asignado. La columna `codigoConcepto` desapareció del archivo, y con ella
`extraeCodigoConcepto`.

### 6.2 `CTDSESTD` no tenía rubro — resuelto con el 222

Los estados de la cuota vivían solo en un comentario del DDL, incumpliendo la regla 2 del
maestro. Ahora son el rubro **222 `RHH_ESTADO_CUOTA_DESCUENTO`** (PENDIENTE, DESCONTADA,
PARCIAL, ANULADA), con su interfaz `com.saa.rubros.RhhEstadoCuotaDescuento`.

**No se reutilizó `EstadoCuotaPrestamo`**: pertenece a CRD y describe otro dominio.

## 7. Verificación de la carga

Con 18–25 empleados el corte se revisa a mano. Consultas de control:

```sql
-- Resumen de lo cargado por tipo
SELECT SLAPTPSL, COUNT(*), SUM(SLAPVLOR), SUM(SLAPDIAS)
  FROM RHH.SLAP WHERE SLAPFCCR = DATE '2025-12-31'
 GROUP BY SLAPTPSL ORDER BY 1;

-- Saldos que no se pudieron enlazar con un empleado
SELECT SLAPCDGO, SLAPIDNT, SLAPTPSL
  FROM RHH.SLAP WHERE SLAPFCCR = DATE '2025-12-31' AND MPLDCDGO IS NULL;

-- Rastro de la materializacion
SELECT SLAPRFTB, COUNT(*) FROM RHH.SLAP
 WHERE SLAPFCCR = DATE '2025-12-31' AND SLAPAPLC = 'S'
 GROUP BY SLAPRFTB;

-- Cuadre de cada prestamo: la suma de cuotas debe igualar el saldo
SELECT d.DSRCCDGO, d.DSRCSLDD, SUM(c.CTDSTTAL) AS SUMA_CUOTAS,
       d.DSRCSLDD - SUM(c.CTDSTTAL) AS DIFERENCIA
  FROM RHH.DSRC d, RHH.CTDS c
 WHERE c.DSRCCDGO = d.DSRCCDGO AND d.DSRCAPRT = 'S'
 GROUP BY d.DSRCCDGO, d.DSRCSLDD
HAVING d.DSRCSLDD - SUM(c.CTDSTTAL) <> 0;   -- debe devolver cero filas
```
