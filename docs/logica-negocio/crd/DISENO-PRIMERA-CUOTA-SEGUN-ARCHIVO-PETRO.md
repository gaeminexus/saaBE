# DISEÑO — La primera cuota cae en el mes de inicio si el archivo Petro de ese mes no se ha generado

omen-saa-1-arb · 2026-11-08 · Pedido del usuario. Ejecutor: omen-saa-1-be.

## 1. Pedido (palabras del usuario)

> «Lo que se quiere hacer es que si ya se generó el archivo petro, entonces la cuota va desde el siguiente mes,
> caso contrario va en el mismo mes»

## 2. Hoy (verificado en `CalculadoraAmortizacionServiceImpl`, main `bfe59ca7`)

- La cuota 1 vence SIEMPRE a fin del mes siguiente a `fechaInicio`; su interés = interés del mes
  (`saldo × tasaMensual`) + proporcional de los días reales del mes de inicio (`interesesMesInicial`, D1/D8).
  Ej. real: inicio 09/11, cuota 1 al 31/12 con $107,73 + $77,00 (21 días de noviembre).
- La cuota 0 (gracia) vence a fin del mes siguiente con el interés de los días reales (`agregarCuotaCero`).
- Usan la calculadora: `PrestamoServiceImpl` (tabla real) y `SimulacionPrestamoServiceImpl` (simuladores).
- El archivo Petro (`GeneracionArchivoPetroServiceImpl.recopilarPrestamos`) cobra toda cuota con vencimiento
  ≤ fin del mes generado, de préstamos en estado **1 GENERADO**, 2 VIGENTE y 11 EN MORA. Ciclo GNAP:
  0 PENDIENTE → 1 GENERADO → 2 ENVIADO → 3 PROCESADO; anular vuelve a 0; una cabecera por mes + año + filial.

## 3. Regla nueva

**«Archivo del mes generado»** = existe `GNAP` del mes y año de `fechaInicio` y de la **filial del partícipe**
con estado **1, 2 o 3**. (Estado 0 o sin cabecera = no generado.)

| Archivo del mes de inicio | Cuota 1 vence | Interés de la cuota 1 |
|---|---|---|
| No generado | **fin del mes de inicio** | solo los días reales del mes de inicio (`capital × tasaDiaria × días`) — sin el mes completo |
| Generado | fin del mes siguiente (como hoy) | como hoy: mes completo + proporcional |

- El resto de cuotas se corre igual (cuota i = fin del mes de inicio + i − 1 en el caso nuevo). El capital de cada
  cuota se sigue derivando como hoy (`cuotaFija − interesBase`); en el caso nuevo la cuota 1 solo cambia el
  interés que se suma. Francesa y alemana por igual. La última cuota sigue absorbiendo el saldo.
- **Días = 0** (inicio el último día del mes): se usa la regla de hoy (mes siguiente).
- **Cuota 0 (gracia)**: en el caso nuevo vence a fin del mes de inicio con el interés de los días reales; la cuota 1
  al mes siguiente. El ejecutor verifica que el corrimiento quede consistente en los dos tipos.
- Implementación sugerida (el ejecutor puede proponer otra): un `Boolean primeraCuotaEnMesInicio` en
  `ParametrosAmortizacion` (null/false = comportamiento de hoy, para no tocar abono a capital ni nada más que use la
  calculadora); quien llama decide el valor con un método único, p. ej.
  `archivoPetroGenerado(LocalDate fechaInicio, Long codigoFilial)`, en un servicio de crd (consulta a GNAP).
- **Préstamo sin filial** o partícipe sin filial: regla de hoy (mes siguiente) y log.
- **Simuladores**: si el simulador conoce al partícipe, la misma regla; si no (sin partícipe), la de hoy. Que el
  resultado diga en qué mes cae la primera cuota.

## 4. ⛔ Trampa: la tabla se genera un día y se aprueba otro

La tabla se genera con el préstamo en 6/1 y se aprueba días después. Entre medio puede generarse el archivo, y
con §4-bis el archivo ya no toma préstamos en estado 1: la cuota 1 de un préstamo no aprobado nunca queda en él.

**Al aprobar** (`aprobarConNiveles`, en cada nivel, antes de registrar la aprobación): si la cuota 1 vence en el
mes de inicio y el GNAP de ese mes y filial está en 1/2/3 → 422: «El archivo Petro de <mes> ya se generó; regenere
la tabla de amortización (la primera cuota pasará a <mes siguiente>)». Regenerar la tabla ya invalida las
aprobaciones por niveles (B4) — es lo correcto. Como respaldo, si la cuota 1 sí aparece en `CXPG` de esa
generación (préstamos que estaban en 1 antes de este cambio), se deja aprobar.

## 4-bis. El archivo Petro deja de cobrar préstamos en estado 1 GENERADO (decisión del usuario, 2026-11-08)

> «Es correcto, los generados no los debe incluir»

- `GeneracionArchivoPetroServiceImpl.recopilarPrestamos` (~1408): `ESTADOS_PRESTAMO_A_COBRAR` queda en
  2 VIGENTE y 11 EN_MORA; ajustar el `println` de filtros (~1399). Nada más en esa clase.
- Actualizar en el mismo cambio `docs/logica-negocio/petro/REGLAS-GENERACION-PETRO.md` §2.2 (regla obligatoria
  del CLAUDE.md).
- `CargaArchivoPetroServiceImpl:6631` (aplica pagos a préstamos en estado GENERADO) **no se toca**: el ejecutor
  solo reporta qué hace ahí, por si deja de tener sentido.
- Antes del EAR el usuario corre un conteo de solo lectura de préstamos en estado 1 con cuotas ya vencidas o
  pagadas (préstamos «vivos» mal marcados que dejarían de cobrarse). Lo arma el árbitro.

## 5. Fuera de alcance / no tocar

- El resto de `GeneracionArchivoPetroServiceImpl` (fuera de §4-bis) no cambia.
- Préstamos ya generados o aprobados: conservan su tabla.
- Abono a capital, reestructuración, novación: solo heredan la regla si llaman a la calculadora con el flag; por
  defecto se quedan como hoy. El ejecutor reporta qué caminos de novación/reestructuración generan la tabla y si
  deben llevar la regla (por defecto sí, si generan la tabla por el mismo `PrestamoServiceImpl`).

## 6. Casos a revisar uno por uno

1. Inicio 09/11, archivo de noviembre no generado → cuota 1 al 30/11 con 21 días de interés; cuota 2 al 31/12.
2. Inicio 09/11, archivo de noviembre generado (estado 1, 2 o 3) → como hoy (cuota 1 al 31/12).
3. Archivo anulado (vuelve a 0) → cuenta como no generado.
4. Inicio 30/11 → mes siguiente.
5. Con cuota 0, francesa y alemana.
6. Tabla generada antes del archivo, préstamo en estado 1 al generar el archivo → aprueba normal (cuota en CXPG).
7. Tabla generada antes del archivo, cuota no en CXPG → aprobar da 422; regenerar → cuota 1 al mes siguiente.
8. Otra filial con su propio GNAP: se mira solo el de la filial del partícipe.
9. Simulador sin partícipe → regla de hoy.

## 7. Documentación

Actualizar en el mismo cambio `crd/API-CICLO-OTORGAMIENTO.md` (regla de la primera cuota y el 422 al aprobar) y
`crd/PLAN-SIMULADORES-PRESTAMOS.md` (nota sobre D1: el proporcional no se suma cuando la cuota 1 cae en el mes de
inicio).
