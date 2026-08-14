# REGLAS DE GENERACIÓN — ARCHIVO DE DESCUENTOS HACIA LA EMPRESA

**Documento consolidado y verificado contra el código fuente al 2026-08-13.**
Contexto general, catálogos y endpoints: ver [REGLAS-GENERALES-PETRO.md](REGLAS-GENERALES-PETRO.md).
Fuente de verdad: `com.saa.ejb.crd.serviceImpl.GeneracionArchivoPetroServiceImpl` (`@Stateless`).

---

## 1. Flujo y ciclo de vida (GNAP)

```
POST /rest/gnap/crearCabecera?mes&anio&codigoFilial&usuario   → GNAP estado 0 (PENDIENTE)
POST /rest/gnap/generarArchivo/{codigo}                       → recopila datos, crea DTGA/PDGA/CXPG,
                                                                 escribe el TXT → estado 1 (GENERADO)
GET  /rest/gnap/descargarArchivo/{codigo}?usuario=            → estampa fechaDescarga/usuarioDescarga
POST marcarEnviado / marcarProcesado                          → estado 2 (ENVIADO) → 3 (PROCESADO)
POST anular (solo desde estado 1)                             → estado 0, motivo en observaciones
DELETE /rest/gnap/eliminar/{codigo}?usuario=                  → borra todo y libera el periodo
```

Reglas del ciclo de vida:
- **Duplicados**: no puede existir más de una generación por **mes + año + filial**
  (`buscarPorPeriodo` al crear la cabecera; UNIQUE en BD).
- **`generarArchivo` solo corre con estado 0 o null** ("ya fue procesada" en otro caso). Anular
  (1→0) es la vía para regenerar un periodo.
- **Marca de descarga**: la PRIMERA descarga estampa `fechaDescarga`/`usuarioDescarga` **antes** de
  enviar el binario; descargas posteriores no la sobreescriben (auditoría). Si el usuario cancela
  la descarga la marca queda igual (deliberado).
- **Eliminación** (`eliminarGeneracion`, transaccional): prohibida si ya fue **descargada**
  (`fechaDescarga != null`), **ENVIADA (2)** o **PROCESADA (3)**. Borra de abajo hacia arriba con
  DELETEs masivos JPQL (las FK no tienen cascada en JPA): `CXPG → PDGA → DTGA → GNAP`, y borra el
  TXT del disco **al final** (si algo falla antes, rollback y el TXT sigue junto a sus registros).
  Que el TXT ya no exista en disco no es error (`archivoEliminado: false`).
- La **filial es obligatoria**: sin ella no se sabe qué partícipes entran ni con qué formato sale
  el archivo (`obtenerCodigoFilial` corta el proceso).

## 2. Qué se cobra (recopilación de datos)

Orden de productos en el archivo y en los detalles: **AH → HS → PE → PH → PQ → PP**.
Orden de partícipes dentro de cada producto: por **rol** (filial Petrocomercial) o por
**número de identificación** (otras filiales).

Filtro de identificador según filial (`condicionIdentificadorFilial`):
- Petrocomercial: `rolPetroComercial IS NOT NULL AND > 0`.
- Otras: `numeroIdentificacion IS NOT NULL AND LENGTH(TRIM(...)) > 0` (en Oracle `''` es NULL).
Un partícipe sin el identificador de su filial **no entra en la generación**.

### 2.1 Aportes (producto AH) — `recopilarAportes`
- Universo: `HistorialSueldo` con **estado 99** de entidades de la filial en estado
  **ACTIVO (1)** o **ACTIVO_EN_MORA (8)**; se toma el historial más reciente por entidad
  (`ORDER BY entidad, fechaIngreso DESC`, primero de cada grupo).
- Monto normal: `montoJubilacion + montoCesantia` (guardados por separado para el desglose).
- **Morosos** (`calcularMesesACobrarMorosos`): a los ACTIVO_EN_MORA se les cobra la deuda
  acumulada: `meses = MONTHS_BETWEEN(último aporte tipos 9/11 anterior al periodo, periodo)`,
  mínimo 1, **sin tope**; el monto de jubilación y cesantía se multiplica por esos meses.
  Ejemplo: último aporte abril, generando agosto → 4 meses (mayo–agosto).
  Si no registra ningún aporte previo, no se puede calcular la deuda → se cobra 1 mes (con log).
- Solo entran líneas con monto > 0.

### 2.2 Préstamos — `recopilarPrestamos`
- Universo (una sola query): **TODAS** las cuotas con `fechaVencimiento <= fin del mes generado`
  (la del mes y las atrasadas) donde:
  - préstamo `idEstado IN (GENERADO=1, VIGENTE=2, EN_MORA=11)`;
  - entidad en **ACTIVO (1)** y de la filial;
  - `producto.codigoPetro IS NOT NULL`;
  - cuota `(estado IS NULL OR estado NOT IN (4 PAGADA, 7 CANCELADA_ANTICIPADA))` — el `IS NULL`
    es necesario porque `NOT IN` contra NULL descarta la fila.
- **Se cobra el SALDO pendiente, no el valor original**: lo pagado se obtiene de `PGPR` con una
  query agregada por bloques de 500 cuotas (`obtenerPagosPorCuota`); si esa consulta falla, el
  proceso **se corta** (mejor no generar que cobrar dos veces).
  `saldoCuota = Σ max(0, componente − pagado)` sobre capital, interés, mora, interés vencido y
  desgravamen — **sin** seguro de incendio, que viaja aparte.
  > **Desde el 2026-08-14 el archivo cobra mora de verdad.** La fórmula ya incluía el término
  > `mora`, pero `DTPRMRAA` valía siempre 0 porque nada lo escribía. Ahora lo alimenta el proceso
  > diario (`docs/logica-negocio/crd/PROCESO-DIARIO-INTERES-MORA.md`), que corre a las 02:00 y
  > acumula la mora de las cuotas vencidas. **No hubo cambios de código en esta clase**: el monto
  > enviado crece solo. `DTPRINVN` (interés vencido) sigue en 0.
- Cuotas con saldo ≤ 0.01 se omiten (pagadas aunque el estado diga otra cosa).
- **Una línea por préstamo**, con la suma de los saldos de sus cuotas; cada cuota queda registrada
  en `CXPG` (número y valor). Un partícipe con dos préstamos del mismo producto genera dos líneas
  en Petrocomercial; en el formato ARCH la columna del producto lleva la suma.
- **Seguro de incendio (PH/PP)**: `saldoSeguro = max(0, valorSeguroIncendio − seguroPagado)` de
  cada cuota se acumula **por entidad** en una línea del producto `HS`, con CXPG por cuota que
  registra el préstamo de origen.

### 2.3 Persistencia del detalle
- `DTGA`: un registro por producto con `totalRegistros` y `totalMonto`.
- `PDGA`: una fila por línea (entidad, préstamo o null, rol, producto, `montoEnviado`,
  `numeroLinea` global, estado 1 = ENVIADO).
- `CXPG`: para préstamos y HS, una fila por cuota sumada; para AH, **dos filas por partícipe**:
  tipoAporte **9** (jubilación) y **11** (cesantía), solo si su monto > 0. En BD el aporte se
  guarda sumado en PDGA y el desglose AC/AJ vive únicamente en CXPG.
- Cabecera GNAP actualizada al final: estado 1, `totalRegistros` (filas PDGA — en ARCH no coincide
  con las líneas del archivo), `totalMontoEnviado`, nombre y ruta del archivo.

## 3. Formatos del archivo TXT

Ruta: `{user.home}/archivos_petrocomercial/`. Nombre:
`DESCUENTOS ASOPREP {MES} {AÑO}.txt` (Petrocomercial), `DESCUENTOS ARCH {MES} {AÑO}.txt` (ARCH),
`DESCUENTOS FILIAL {n} {MES} {AÑO}.txt` (otras). El nombre lleva la filial porque todas las
generaciones comparten carpeta.

### 3.1 Filial 1 (Petrocomercial) — posicional 55 caracteres
Una línea por partícipe-producto, bloques en el orden AH, HS, PE, PH, PQ, PP:

| Pos (1-based) | Long | Contenido |
|---|---|---|
| 1–5 | 5 | rol del partícipe, `%5d` (justificado a la derecha con espacios) |
| 6–9 | 4 | constante `JRNN` |
| 10–17 | 8 | `00000000` |
| 18–25 | 8 | fecha proceso = último día del mes generado, `YYYYMMDD` |
| 26–38 | 13 | monto × 10.000, `%013d` (`round(monto * 10000)`) |
| 39 | 1 | constante `1` |
| 40–53 | 14 | `00000000000000` |
| 54–55 | 2 | código de producto |

Ejemplo: ` 9689JRNN00000000202506300000000450300100000000000000AH` (45.03 del producto AH).

### 3.2 Filial 2 (ARCH) — plano por columnas, separador `;`
```
ASOPREP
Fecha: AGOSTO 2026
IDENTIFICACION;RAZON SOCIAL;AC;AJ;PE;PH;HS;PQ;PP;TOTAL
0923456789;GOMEZ RUIZ MARIA ELENA;25.00;35.00;95.20;0.00;0.00;0.00;0.00;155.20
TOTALES;2 PARTICIPES;50.00;70.00;95.20;180.45;12.30;0.00;0.00;407.95
```
- **Una línea por partícipe** (consolida todos sus productos); orden de columnas de préstamos:
  `PE;PH;HS;PQ;PP`.
- El aporte va separado: `AC` = cesantía, `AJ` = jubilación (desde el desglose de la recopilación,
  o desde CXPG al regenerar).
- Valores con dos decimales y punto (`Locale.US`), sin multiplicador; producto sin valor = `0.00`.
- `TOTAL` = todo lo que se le descuenta al partícipe en el periodo.
- Última fila `TOTALES` con los totales por columna (la segunda celda es el conteo de partícipes).
- Filas ordenadas por identificación; se limpian `;` y saltos de línea de la razón social.

## 4. Regenerar y consultar

- `regenerarArchivo(codigo)`: reconstruye el TXT **desde la BD** (DTGA→PDGA; para AH recupera el
  desglose jubilación/cesantía desde CXPG — si una línea no tiene CXPG, todo el valor se reporta
  como jubilación para no perderlo). Mismo orden y formato que la generación original.
- `obtenerDetalle` / `obtenerEstadisticas`: cabecera + detalles por producto, con totales de
  aportes vs préstamos.
- Descripciones de producto en el código: AH "Aportes Voluntarios / Ahorro", HS "Seguro",
  PE "Préstamo Emergente", PH "Préstamo Hipotecario", PQ "Préstamo Quirografario",
  PP "Préstamo Personal".

## 5. Relación con el proceso de carga

- La generación cobra **saldos** (incluidas cuotas PARCIALES y atrasadas); la carga aplica lo
  descontado empezando siempre por la **menor cuota pendiente**. Ambos usan `PGPR` como fuente de
  lo ya pagado, por lo que no se contradicen.
- La mora de aportes es un ciclo cerrado: la carga marca **ACTIVO_EN_MORA** (dos periodos sin
  aporte AH) y lo revierte a ACTIVO cuando llega un pago; la generación cobra a los morosos la
  deuda acumulada (meses × aporte mensual).
- El seguro de incendio sale como producto `HS` (saldo por entidad) y regresa como registro `HS`
  que la carga suma al PH/PP correspondiente.
