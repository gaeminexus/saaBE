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

**Cambio del 2026-08-27 (Fase 4 del plan de devengo de aportes).** `recopilarAportes` ahora
es un despachador detrás del flag `CRD_GENERACION_POR_FALTANTE` (rubro 242, detalle único,
`ConfiguracionGeneracionAportesService.porFaltanteActiva()`, **APAGADO por defecto** —
catálogo en `docs/logica-negocio/crd/sql/70_CATALOGO_RUBRO_GENERACION_POR_FALTANTE.sql`,
`GET/PUT /rest/cnfg/generacionPorFaltanteAh`):

```
si porFaltanteActiva() -> recopilarAportesPorFaltante   (camino nuevo)
si no                  -> recopilarAportesPorHistorialSueldo (camino viejo, intacto)
```

#### Camino viejo — `recopilarAportesPorHistorialSueldo` (el de siempre, sigue activo por defecto)
- Universo: `HistorialSueldo` con **estado 99** de entidades de la filial en estado
  **ACTIVO (1)** o **ACTIVO_EN_MORA (8)**; se toma el historial más reciente por entidad
  (`ORDER BY entidad, fechaIngreso DESC`, primero de cada grupo).
- Monto normal: `montoJubilacion + montoCesantia` (guardados por separado para el desglose).
- **Morosos** (`calcularMesesACobrarMorosos`, **`@Deprecated`** desde esta fase — ver más
  abajo por qué desaparece): a los ACTIVO_EN_MORA se les cobra la deuda acumulada:
  `meses = MONTHS_BETWEEN(último aporte tipos 9/11 anterior al periodo, periodo)`,
  mínimo 1, **sin tope**; el monto de jubilación y cesantía se multiplica por esos meses.
  Ejemplo: último aporte abril, generando agosto → 4 meses (mayo–agosto).
  Si no registra ningún aporte previo, no se puede calcular la deuda → se cobra 1 mes (con log).
- Solo entran líneas con monto > 0.

#### Camino nuevo — `recopilarAportesPorFaltante` (detrás del flag, apagado por defecto)
- Universo: `Entidad` (no `HistorialSueldo`) en estado ACTIVO/ACTIVO_EN_MORA de la filial,
  con el mismo filtro de identificador (`condicionIdentificadorFilial`) que el resto de la
  generación.
- Fórmula, por partícipe y tipo de aporte (9 jubilación, 11 cesantía), sumando cada mes
  desde el **piso 2025-06-01** (D11, mismo valor que `CargaArchivoPetroServiceImpl.ALCANCE_MINIMO_DEVENGO`,
  replicado localmente porque esa constante es privada y vive en otro paquete) hasta el
  periodo que se genera:
  ```
  a cobrar = Σ max(0, esperado(m,tipo) − aportado(m,tipo))
  ```
  - `esperado(m,tipo)`: `VigenciaContratoService.esperadoEnLotePorFilial` — ver más abajo
    por qué es esa forma y no `esperadoPorEntidad` dentro del bucle.
  - `aportado(m,tipo)`: `AportadoGeneracionDaoService.sumAportadoPorEntidadPeriodoTipo`, una
    sola consulta para toda la filial (no una por partícipe), usando el mismo PERIODO
    EFECTIVO de `PeriodoEfectivoAporteSql` (no el devengo a secas).
- **`calcularMesesACobrarMorosos` y `AporteDaoService.selectUltimaFechaAportePorEntidad`
  desaparecen conceptualmente**: cobrar el faltante mes a mes ya cubre morosos (un mes sin
  aporte simplemente acumula faltante, sin límite), anticipos (un mes ya cubierto de más no
  vuelve a cobrarse) y devoluciones (bajan `aportado`, así que el faltante reaparece solo),
  sin ningún caso especial. `calcularMesesACobrarMorosos` quedó `@Deprecated` (sigue en uso
  desde el camino viejo mientras el flag esté apagado); `selectUltimaFechaAportePorEntidad`
  vive en `AporteDaoService`, en manos de otro agente en esta ola — pendiente que ese agente
  lo marque `@Deprecated` cuando le corresponda.
- **Por qué `esperado` se carga EN BLOQUE, y no se resuelve llamando al servicio partícipe
  por partícipe** (corregido 2026-08-27, tras medir con los números reales de esta base):
  la primera versión de este camino llamaba a `esperadoPorEntidad` dentro del doble bucle
  (partícipe × mes × tipo), dos consultas internas cada vez (contrato activo + vigencia
  vigente). Con ~1.650 partícipes × ~14 meses desde el piso × 2 tipos, eso son **del orden de
  92.000 consultas en una sola generación**, contra un timeout de WildFly de 15 minutos para
  este proceso — **no es un riesgo hipotético "a escala real", revienta la primera vez que
  alguien encienda el flag**. **No es solo un patrón de acceso más rápido**: la instrucción
  de reusar `VigenciaContratoService` sin otra vía era sobre la regla de negocio (que el
  esperado salga de un único sitio), no sobre cómo se llama a la base — así que la solución
  correcta no era optimizar la llamada, era mover la regla a donde ya vivía.
  - `VigenciaContratoService` ganó `esperadoEnLotePorFilial(codigoFilial, desde, hasta)`:
    UNA consulta (`VigenciaContratoDaoService.selectVigentesPorFilial`, nativa con
    `ROW_NUMBER()` para el desempate de contrato activo — mismo criterio que
    `ContratoDaoServiceImpl.selectActivoPorEntidad`) trae todas las vigencias vigentes de la
    filial, y el servicio resuelve la cobertura mes a mes en memoria a un mapa
    `"idEntidad|idTipoAporte|mes" -> monto`.
  - **Es la MISMA regla que `esperadoPorEntidad`, una sola implementación**: ambos métodos
    viven en `VigenciaContratoService`/`VigenciaContratoDaoService`, la capa dueña de la
    regla de qué vigencia aplica a qué mes. `GeneracionArchivoPetroServiceImpl` sólo consume
    el mapa con `getOrDefault(...)` — no vuelve a decidir qué vigencia cubre qué mes.
  - **Primera versión de esta corrección (ya reemplazada):** reimplementó la regla en JPQL
    local dentro de `GeneracionArchivoPetroServiceImpl.cargarVigenciasVigentesPorFilial`,
    creyendo por error que `VigenciaContrato*` estaba vetado para este agente en esta fase
    — la restricción real de la Fase 4 es sobre `Aporte*`/`EntidadDaoServiceImpl`/
    `PeriodoEfectivoAporteSql` (territorio de la Fase 5), no sobre contratos, que este mismo
    agente escribió en la Fase 3. Tener la regla en dos sitios es el mismo defecto que ya se
    corrigió una vez extrayendo `PeriodoEfectivoAporteSql`: el día que alguien cambie sólo
    una copia, la generación cobra con un criterio distinto al que muestra la pantalla, sin
    ningún error visible. **No la vuelvas a duplicar** si en algún momento hace falta tocar
    esto de nuevo.
  - **Con este cambio, `recopilarAportesPorFaltante` corre en 3 consultas totales por
    generación** (entidades de la filial, aportado en bloque, vigencias en bloque vía
    `VigenciaContratoService`), independientemente de cuántos partícipes o meses cubra — es
    la diferencia entre un proceso que corre en segundos y uno que no termina. **No
    "simplificar" esto de vuelta a una llamada por partícipe**: parece más simple de leer,
    pero reintroduce el reventón del timeout.
- **Antes de encender el flag**, correr
  `docs/logica-negocio/crd/sql/71_COMPARACION_GENERACION_VIEJO_VS_NUEVO.sql` (solo lectura):
  compara, para el próximo periodo, lo que cobraría cada camino por partícipe, con un
  resumen por tramos de diferencia. Solo es significativa **después** de migrar
  `CRD.VGCN` (`64_MIGRACION_CONTRATOS_VIGENCIAS.sql` de la Fase 3): mientras esa tabla esté
  vacía, el camino nuevo cobra 0 para todos y la comparación no dice nada.

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
- **`nombreFilial` (pedido 4, 2026-08-27):** `GeneracionArchivoPetro.getNombreFilial()` es
  un getter derivado (`@Transient`) que expone `filial.getNombre()` como campo plano. No
  hizo falta un JOIN nuevo en ninguna consulta: `filial` ya se trae EAGER (`@ManyToOne` sin
  `fetch` explícito), así que todos los endpoints existentes (`getAll`, `getId`,
  `porFilial`, y el objeto embebido en `obtenerDetalle`) ya lo traían con una sola consulta;
  solo faltaba exponerlo como campo plano para el frontend.

## 5. Relación con el proceso de carga

- La generación cobra **saldos** (incluidas cuotas PARCIALES y atrasadas); la carga aplica lo
  descontado empezando siempre por la **menor cuota pendiente**. Ambos usan `PGPR` como fuente de
  lo ya pagado, por lo que no se contradicen.
- La mora de aportes es un ciclo cerrado: la carga marca **ACTIVO_EN_MORA** (dos periodos sin
  aporte AH) y lo revierte a ACTIVO cuando llega un pago; la generación cobra a los morosos la
  deuda acumulada (meses × aporte mensual).
- El seguro de incendio sale como producto `HS` (saldo por entidad) y regresa como registro `HS`
  que la carga suma al PH/PP correspondiente.
