# Revisión del motor de amortización antes del otorgamiento

**Fecha:** 2026-08-31 · **Equipo:** CRD · EQUIPO B (ciclo del crédito y seguros) · **Escrito por:** árbitro `omen-saa-1`
**Gate:** ninguna tarea de otorgamiento que genere una tabla real arranca antes de cerrar §3.

> **Qué es esto.** `ALCANCE-EQUIPOS-CRD.md` §"Equipo 3" ordena revisar los 10 defectos de la
> auditoría del motor (`PLAN-SIMULADORES-PRESTAMOS.md` §5.1) **antes de generar la primera tabla de
> amortización real**. Esta revisión no repite la auditoría: verifica, línea por línea contra el
> código del 2026-08-31, **cuáles quedaron efectivamente corregidos**, y agrega lo que aparece al
> mirar el mismo código con la pregunta "¿y si mañana esto genera cartera real?".
>
> El `PLAN-SIMULADORES-PRESTAMOS.md` §11.2 declara los 10 cerrados. **No lo están: dos siguen
> abiertos**, y hay cinco defectos nuevos que esa auditoría no podía ver porque solo miró la
> matemática de la calculadora, no el camino que va de la calculadora a `CRD.DTPR`.

---

## 1. Lo que sí quedó corregido — verificado, no asumido

Ocho de los diez están corregidos dentro de `CalculadoraAmortizacionServiceImpl`, y
`PrestamoServiceImpl.generarAmortizacion` **delega de verdad** (línea 197:
`calculadoraAmortizacionService.calcular(params)`). No hay motor duplicado sobreviviente en
`PrestamoServiceImpl`.

| Defecto | Verificado en | Cómo se confirmó |
|---|---|---|
| **D1** | `CalculadoraAmortizacionServiceImpl.calcularFrancesa` | `cuota = redondear(capitalCuota + interes)`, y el proporcional entra a `interes` cuando `i == 1 && mesesGracia == 0`. El capital se sigue derivando de `interesBase` (sin proporcional), como pedía la decisión 9. |
| **D2 / D3 / D9** | `calcularFrancesa` / `calcularAlemana` | Última cuota: `capitalCuota = redondear(saldoAntes)` **sin condición de magnitud**. `saldoCapital` redondeado en cada paso. `cuotaFija`/`capitalFijo` redondeados una sola vez arriba. |
| **D4** | `agregarCuotaCero` | `interes = redondear(capital * tasaDiaria * diasReales)` con `diasReales = DAYS.between(fechaInicio, ultimoDiaMesSiguiente)`. |
| **D6 / D7** | `fila(...)` + `construirDetalle` | Los invariantes se cumplen **por construcción**, no por un cálculo paralelo: `total = cuota + desgravamen + seguro` en `fila`, y `saldoInicialCapital = capital + saldoCapital` en `PrestamoServiceImpl:242` (con `saldoOtros = 0`). |
| **D8** | `calcular` | `diasMesInicial = DAYS.between(fechaInicioLocal, ultimoDiaMesInicio)`, sin el `+1`. |
| **Casos borde §5.2** | `validar` / `validarNingunaCuotaEnCero` | `plazo > 600` → rechazo duro; cuota regular en $0,00 → `MONTO_INSUFICIENTE`. |

**Esta parte está bien y no hay que volver a tocarla.** El resto del documento es lo que falta.

---

## 2. Los dos que siguen abiertos

### D5 — `DTPRSLDO` significa dos cosas distintas según quién la escribió · **ABIERTO**

`PLAN-SIMULADORES-PRESTAMOS.md` §11.2 lo despacha con *"No aplica acá: es de mapeo, se trata en
`construirDetalle`"*. **Y en `construirDetalle` no se trató.**

```java
// PrestamoServiceImpl:220-221  <- el generador
detalle.setSaldoCapital(saldoCapital);
detalle.setSaldo(saldoCapital);          // <- capital pendiente
```

Contra los otros seis escritores del repo, que sin excepción escriben **importe pendiente por
cobrar**:

| Escritor | Línea | Qué escribe |
|---|---|---|
| `MotorPagoPrestamoServiceImpl` | `:218`, `:447`, `:682` | `max(0, totalConMoraIV - totalPagado)` |
| `AbonoCapitalPrestamoServiceImpl` | `:775` | `total` de la cuota |
| `CargaArchivoPetroServiceImpl` | `:1724`, `:1756`, `:1863`, `:2462`, `:3029` | `max(0, totalCuota - totalPagadoCuota)` |
| `PrestamoServiceImpl` (camino Excel) | `:381` | capital pendiente — **el otro lado del mismo defecto** |

**Por qué no ha hecho daño hasta hoy:** la cartera es migrada y entró por el camino Excel o por
Petro; y en cuanto una cuota recibe su primer pago, el motor sobrescribe `DTPRSLDO` con su propia
semántica. La incoherencia dura desde que se genera la tabla hasta el primer pago.

**Por qué importa el día del otorgamiento:** un crédito nuevo vive semanas en ese hueco. Todo lo que
lea `DTPRSLDO` en ese tramo lee capital pendiente donde el resto del sistema pone total por cobrar.

**Radio de daño, medido:** en Java casi no hay lectores (`AbonoCapitalPrestamoServiceImpl:828` y
`ProcesoPagoPrestamoServiceImpl:1471`, ambos copiando a/desde `HistDetallePrestamo`). En el frontend
hay dos, y son de presentación: `prestamo-detalle-dialog` (columna «Saldo» y su exportación CSV) y
`afectacion-financiera-cuotas-dialog`. **El cobro no lo usa**: la pantalla de cobros personales
recalcula desde componentes (`SaldoPrestamoService.saldoPendienteDe`). Así que el riesgo es de
reporte y de exportación, no de plata mal cobrada — **severidad media, tal como la puso la
auditoría**, ni más ni menos.

**Decisión del árbitro:** gana la semántica de los seis, no la del uno. `detalle.setSaldo(total)` en
el generador. **Nada retroactivo** (la decisión 13 del plan sigue vigente): no se toca ninguna fila
ya persistida, ni las del camino Excel que ya están en producción.

### D10 — la mitad que falta, y es la única que hace daño HOY · **MEDIO ABIERTO**

La mitad hacia adelante está hecha y verificada: `PrestamoServiceImpl.saveSingle:98-105` deriva
`interesNominal` de `tasa` en cada guardado.

**La mitad hacia atrás no existe.** El proceso de mora de las 02:00 sigue leyendo `PRSTINNM` sobre
la cartera migrada:

```java
// ProcesoMoraPrestamoServiceImpl:328-342
if (tasaNominal <= 0.0) {
    tasaNominal = TASA_POR_DEFECTO;   // 9.0
    // ...ADVERTENCIA: prestamo N sin interesNominal (PRSTINNM); se usa el default silencioso...
```

El comentario del código dice que *"esto solo debería activarse en préstamos guardados ANTES del
fix"* — y **todos** los préstamos de producción fueron guardados antes del fix, porque el fix nunca
se desplegó y porque nada vuelve a guardar un préstamo migrado que nadie edita. Se agregó la traza,
que es útil, y se dejó el número equivocado.

No hay ningún script de backfill en `docs/logica-negocio/crd/sql/`. Se escribió acá:
**`sql/96_BACKFILL_PRSTINNM_DESDE_PRSTTSAA.sql`**.

#### ⚠️ Medido contra la base — y el resultado cambia la conclusión

El bloque de diagnóstico de `sql/96` se corrió contra la base local el **2026-08-31** (5.664
préstamos, 362.762 cuotas — la cartera migrada completa). **Lo que se esperaba encontrar no estaba:**

| Medición | Resultado |
|---|---|
| Préstamos que caen al default del 9 % | **7** de 5.664 |
| De esos, vivos (`PRSTIDST` 2, 8, 11) | **4** |
| De esos, con `PRSTTSAA` utilizable para el backfill | **0** — los 7 tienen `PRSTTSAA = 0` |
| `PRSTINNM` distinto de `PRSTTSAA` donde ambos existen | **0** |
| Distribución de `PRSTTSAA` en la cartera viva | **9 % en 1.305 de 1.311**; el resto: cuatro en 0, uno en 10, uno en 13 |

**Tres conclusiones, y las tres corrigen lo que este documento decía antes:**

1. **El `UPDATE` de `sql/96` no toca ni una fila.** Exige `PRSTTSAA > 0` y los 7 candidatos tienen la
   tasa en 0. El script queda como red de seguridad —**producción puede tener otros números y el
   bloque 0 hay que volver a correrlo allá**— pero acá es un no-op. No es urgente.
2. **La migración sí llenó `PRSTINNM`.** El supuesto de que "todos los préstamos fueron guardados
   antes del fix, así que todos caen al default" era razonable y **es falso**: 5.657 de 5.664 tienen
   la tasa nominal correcta y coincidiendo exactamente con `PRSTTSAA`.
3. **El default silencioso de 9 % coincide con la tasa real de casi toda la cartera.** Con 1.305 de
   1.311 préstamos vivos al 9 %, aunque el default se disparara daría el número correcto. La
   severidad "alta" que le puso la auditoría era correcta *a priori* —un default silencioso es una
   bomba— pero **el daño real medido es mucho menor de lo que se temía**.

#### Lo que sí queda, y es lo único que hay que resolver

**Cuatro préstamos vivos sin ninguna tasa**, que el backfill deliberadamente no puede arreglar
porque no hay de dónde sacar el número:

| Préstamo | Estado | Monto | Plazo | Inicio | Mora ya devengada al 9 % |
|---|---|---|---|---|---|
| 8157 | 2 · VIGENTE | 6.389,38 | 84 | 2019-08-05 | 0,00 |
| 8078 | 8 · PLAZO VENCIDO | 24.966,46 | 25 | 2019-08-05 | **2.854,39** |
| 8085 | 11 · EN MORA | 55.217,22 | 165 | 2012-06-29 | 108,50 (saldo 31,99) |
| 8307 | 11 · EN MORA | 4.001,25 | 60 | 2005-02-22 | **5.876,45** |

Más tres cancelados (8420, 8434, 8440), que no devengan y no interesan.

**La exposición real de D10 son esos ~8.700 de mora sobre dos préstamos**, calculada al 9 % sobre
créditos cuya tasa nadie sabe. Es una decisión de negocio, no un script: **¿qué tasa tienen esos
cuatro?**

⚠️ **Observación aparte, no es de D10 pero salió en la misma consulta:** los siete tienen
`PRSTSLCP = PRSTMNSL` — saldo de capital exactamente igual al monto original. En un préstamo de
**2005** y otro de **2012** eso no puede ser real. Huelen a basura de migración más que a créditos
vivos. Verificar antes de asignarles una tasa: puede que lo que corresponda sea darlos de baja.

---

## 3. Los cinco que la auditoría no podía ver — GATE del otorgamiento

La auditoría de §5.1 miró la aritmética. Estos aparecen al mirar **el camino de la calculadora a la
base**, que es justamente el tramo que el otorgamiento va a estrenar.

### N1 — `generarTablaAmortizacion` no es idempotente · **BLOQUEANTE**

`PrestamoServiceImpl:121-166` valida el préstamo, genera los detalles y los guarda:

```java
for (DetallePrestamo detalle : detalles) {
    detallePrestamoDaoService.save(detalle, detalle.getCodigo());   // codigo == null -> INSERT
}
```

**No borra la tabla anterior. No verifica que no exista una. No hay guarda de estado.** Cada llamada
inserta un juego completo de cuotas nuevas.

Y el botón está a un clic, hoy, en producción:
`saaFE/.../forms/prestamo/prestamo-edit/prestamo-edit.component.ts:480` — sin diálogo de
confirmación y sin comprobar si el préstamo ya tiene cuotas. Dos clics = **tabla duplicada**; el
préstamo pasa a deber el doble y `actualizarCamposPrestamo` recalcula `PRSTTTCP`/`PRSTTTPR` sobre la
lista recién generada, así que la cabecera **no delata** la duplicación.

**Medido contra la base el 2026-08-31: cero cuotas duplicadas** en 362.762 filas de `CRD.DTPR` (el
bloque `0.b` de `sql/96` no devolvió ninguna fila). O sea, **nadie apretó todavía el botón dos
veces** — la premisa de la decisión 14 del plan de simuladores queda confirmada por medición, no por
suposición. El defecto es riesgo futuro, no daño acumulado, y sigue siendo el más peligroso de los
revisados porque **la primera vez que pase no se va a notar**.

Es el defecto más peligroso de todos los revisados, y no está en la lista de 10. Corregir **antes**
de cualquier otra cosa del frente:

- Rechazar si el préstamo ya tiene cuotas, salvo que se pida regenerar explícitamente.
- Regenerar = borrar las cuotas anteriores **solo si ninguna tiene pagos asociados** (`PGPR`); si
  alguna los tiene, rechazo duro. **Todo-o-nada**: se recorren TODAS las cuotas antes de borrar
  ninguna. Borrar las que no tienen pagos y frenar en la primera que sí dejaría la tabla partida a
  la mitad, que es peor que no hacer nada.
- El FE confirma con diálogo antes de regenerar, diciendo cuántas cuotas se van a reemplazar.

#### ⚠️ Pendiente abierto en la guarda — dos agujeros conocidos, a propósito

La guarda implementada usa `pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo`, y ese método
tiene dos problemas para este uso:

1. **Filtra los pagos anulados** (`anulado IS NULL OR anulado = 0`). Una cuota cuyos pagos fueron
   todos anulados por un reverso pasa como "sin pagos", se borra, y las filas anuladas de `CRD.PGPR`
   quedan apuntando a un `DTPRCDGO` inexistente. Un pago anulado **sigue teniendo la FK**.
2. **Se traga la excepción y devuelve lista vacía** si la consulta falla
   (`catch (Exception e) { return new ArrayList<>(); }`). Es la convención de la casa para los bucles
   por lotes de Petro y ahí está bien; acá significa que **ante un error la guarda concluye "no hay
   pagos" y borra la tabla**. Una guarda que ante un fallo dice "adelante" no es una guarda.

**Los detectó la pregunta del árbitro del EQUIPO A** sobre qué pasa con sus `PagoPrestamo`, que
tienen FK a las cuotas. La solución es un método `countByIdDetallePrestamo(Long) throws Throwable`
que cuente **todos** los pagos, anulados incluidos, y que **no** atrape la excepción.

**Lo escribe el EQUIPO A, no nosotros**: `CRD.PGPR` es su área y la guarda existe para proteger sus
FK. Está listo en su working tree, sin commitear — o sea, todavía invisible acá. Cuando llegue a
`origin`, el cambio de nuestro lado es una línea. **Hasta entonces la guarda queda con los dos
agujeros, a propósito, sin TODO en el código**: nada está desplegado y un medio-arreglo es peor que
un pendiente anotado.

### N2 — la corrección de D1 contaminó `PRSTVLCT` · **ALTA**

`actualizarCamposPrestamo:274-277` toma como valor de cuota **la primera cuota con `numeroCuota > 0`**:

```java
if (valorCuota == 0.0 && detalle.getNumeroCuota() > 0 && detalle.getCuota() != null) {
    valorCuota = detalle.getCuota();
}
```

Antes de D1 eso daba la cuota fija francesa. **Después de D1, la cuota 1 incluye el interés
proporcional del mes inicial**, así que `PRSTVLCT` queda por encima de lo que el socio va a pagar
todos los meses. Sobre el ejemplo de la propia auditoría (10.000 al 12 % a 12 meses) son ~56 de más,
en un campo que se presenta como "valor de la cuota".

Lo leen **nueve reportes de `crd`**: `RPRT_TBLA_ACML`, `CSPR`, `CSPT`, `ESDI`, `JBPR`, `JBPT`,
`PNCM`, `RMJP`, `RNCP` (todos `P.PRSTVLCT AS VALOR_CUOTA`).

Inofensivo hoy porque ninguna tabla de producción salió de este generador. Se vuelve visible en los
nueve reportes el día del primer crédito otorgado. **La corrección de D1 fue correcta; lo que quedó
sin ajustar es el campo derivado.**

Corrección: tomar la cuota **representativa**, no la primera — la de `numeroCuota == 2` cuando el
plazo lo permite, en francesa. En alemana la cuota es decreciente y `PRSTVLCT` no significa nada:
dejar la primera regular y **decirlo en el javadoc**, no inventar un promedio.

### N3 — el generador real escribe los seguros en `0.00`, siempre · **ALTA, y es decisión de negocio**

```java
// PrestamoServiceImpl:194-197
params.setDesgravamenPorCuota(0.0);
params.setSeguroIncendioPorCuota(0.0);
```

El simulador **sí** los cobra: es la decisión 6 del plan de simuladores, que existe textualmente
porque *"sin los dos seguros el simulador mostraría una cuota menor que la que el socio va a pagar de
verdad"*. Con N3 abierto pasa exactamente eso, un paso más adelante: **el simulador que el socio
firma no coincide con la tabla que el sistema genera.**

`PRST` ya tiene `PRSTVLAS` (valor asegurado), `PRSTTSIN` (tasa seguro incendio) y `PRSTPRIN` (prima),
así que los datos de entrada existen. Falta decidir de dónde sale cada uno — y eso enlaza con el
tercer frente de este mismo equipo (seguros por pólizas), que es quien va a crear el hecho
administrativo detrás de esos importes. **Ver la pregunta abierta en §7.**

### N4 — `cobrarSegurosEnCuotaCero` NO EXISTE · **MEDIA, dependiente de N3**

La decisión 16 del plan de simuladores dice que si la cuota 0 de gracia cobra o no los seguros *"lo
decide el usuario al simular"*, vía un campo nuevo `ParametrosAmortizacion.cobrarSegurosEnCuotaCero`
y una casilla en la pantalla.

**Ese campo no existe.** Verificado el 2026-08-31 con `grep` sobre `ParametrosAmortizacion`,
`CalculadoraAmortizacionServiceImpl` y `SimulacionPrestamoServiceImpl`: cero coincidencias. La
casilla tampoco está en `forms/simulador-credito`. La decisión se declaró tomada y **nunca se
implementó, ni en BE ni en FE**. `agregarCuotaCero` cobra desgravamen y seguro siempre, sin
condición.

Al menos no hay una casilla mintiendo en la pantalla. Y con la decisión del usuario del 2026-08-31
(§8) el punto queda cerrado por otra vía: la cuota 0 cobra desgravamen, y el seguro de incendio es
0 hasta que existan las pólizas. **No se implementa el flag** — si algún día el negocio quiere una
gracia sin desgravamen, se vuelve a abrir.

### N5 — el estado de la cuota se escribe del catálogo equivocado · **BAJA, barata de arreglar**

```java
// PrestamoServiceImpl:249-250
detalle.setEstado(Long.valueOf(Estado.ACTIVO));
detalle.setIdEstado(Long.valueOf(Estado.ACTIVO));
```

`Estado.ACTIVO = 1`. El motor de pagos razona con **otro** catálogo, `EstadoCuotaPrestamo`
(`MotorPagoPrestamoServiceImpl:194`, `:455`, `:698`, `:714`), donde `PENDIENTE = 1`.

**Funciona por coincidencia numérica.** El día que alguien reordene `Estado`, las cuotas recién
generadas nacen con un estado que el motor de pagos no reconoce. Cambiar a
`EstadoCuotaPrestamo.PENDIENTE` mientras se está en el archivo; no cambia ningún dato, cambia de
dónde sale el 1. Respetar `SINCRONIZACION-DTPRIDST-DTPRESTD.md`: `DTPRESTD` es la fuente,
`DTPRIDST` la espeja.

---

## 4. Corrección de alcance: el otorgamiento NO es un proyecto entero

`LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md` §"El otorgamiento de créditos es un proyecto entero" dice
que *"no hay solicitud, ni evaluación, ni aprobación, ni otorgamiento, ni desembolso"*. Contra el
código, eso es más pesimista de lo que corresponde, y la diferencia cambia el diseño:

| Pieza | Estado real | Evidencia |
|---|---|---|
| Entidad de la solicitud | **Existe: es `CRD.PRST`** | `Prestamo` ya trae `usuarioAprobacion`/`fechaAprobacion`, `fechaAdjudicacion`, `usuarioRechazo`/`fechaRechazo`, `usuarioLegalizacion`/`fechaLegalizacion`, `usuarioAcreditacion`/`fechaAcreditacion`, `estadoOperacion`, `montoLiquidacion` |
| Máquina de estados | **Existe** | rubro `EstadoPrestamo`: `GENERADO(1)`, `PENDIENTE_DE_APROBACION(6)`, `RECHAZADO(7)`, `VIGENTE(2)`, `CANCELADO_POR_NOVACION(5)` |
| Niveles de aprobación por monto | **Existe** | `CRD.CRDT` (`CreditoMontoAprobacion`): `montoMinimo`, `montoMaximo`, `idProceso` |
| Pantalla de alta + generación de tabla | **Existe** | `forms/prestamo/prestamo-edit` → `POST /rest/prst/generarTablaAmortizacion/{id}/{cuota0}` |
| Documentos del crédito | **Existe** | `DocumentoCreditoServiceImpl`, `AdjuntoServiceImpl` |
| Evaluación de capacidad | Parcial | `BandaProductoServiceImpl`, `ClasificadorBandaServiceImpl` |
| Desembolso a tesorería | **No existe** | — |
| Asiento de entrega | **No existe** la plantilla de quirografario | decisión D7 del alcance |

**Consecuencia práctica:** el frente es *"convertir un alta manual sin gobierno en un ciclo con
estados, aprobación y desembolso"*, no un greenfield. Y sobre todo: **no hace falta reservar una
tabla nueva de 4 letras para la solicitud**, que era lo que `REGISTRO-RESERVAS-EQUIPOS.md` §3 daba
por probable para este frente. No consumo rango `PRBR`/`PDTR` todavía.

⚠️ **Corolario incómodo:** si `prestamo-edit` ya crea préstamos y genera tablas, la premisa de la
decisión 14 del plan de simuladores —*"ninguna tabla de `CRD.DTPR` en producción salió de ese
generador"*— se sostiene únicamente sobre que **nadie ha usado ese botón todavía**, no sobre que el
camino no exista. Con N1 abierto, el primer uso puede duplicar una tabla. Confirmar contra la base
antes de dar por buena la premisa (`sql/96`, bloque de diagnóstico 3).

---

## 5. Aviso al árbitro del EQUIPO A

**Rebajado el 2026-08-31 después de medir contra la base.** El aviso original decía que `sql/96`
cambiaba la mora nocturna de la cartera migrada. **No es así: el `UPDATE` no toca ninguna fila en
local** (§2). Queda una sola cosa que sí les afecta, y es menor:

- **`PrestamoServiceImpl.saveSingle` ya deriva `interesNominal` de `tasa` en cada guardado.** Es
  código nuestro, todavía sin desplegar. Cuando el WAR suba, cualquier flujo del equipo A que guarde
  un `Prestamo` va a escribir `PRSTINNM` aunque no lo mande. Es lo que se quiere —y en la práctica no
  cambia nada, porque las dos columnas ya coinciden en las 5.664 filas donde ambas existen— pero
  conviene que lo sepan.

Si en producción el bloque 0 de `sql/96` diera números distintos a los de local, el aviso vuelve a
subir de tono y hay que coordinarlo con ellos antes de ejecutar.

---

## 6. Orden de trabajo

| # | Qué | Quién | Bloquea a |
|---|---|---|---|
| 1 | **N1 caso A** — idempotencia de `generarTablaAmortizacion` (BE + guarda en el FE) | BE, luego FE | todo el frente |
| 2 | **D5** + **N2** + **N5** — mapeo de `construirDetalle` y `actualizarCamposPrestamo` | BE | la primera tabla real |
| 2b | **N3** — desgravamen por fórmula, incendio en 0 (decisión U1) | BE, mismo cambio que el 2 | la primera tabla real |
| 3 | **`sql/96`** — correr el **bloque 0** en producción. El `UPDATE` es no-op en local; ejecutarlo solo si allá los números difieren | usuario | nada |
| 3b | **Los 4 préstamos vivos sin tasa** (§2): decidir tasa o baja. Exposición ~8.700 de mora al 9 % | usuario / negocio | nada |
| 5 | Ciclo de otorgamiento sobre `PRST` + `EstadoPrestamo` + `CRDT` | BE + FE | — |
| 6 | Desembolso a tesorería y asiento de entrega (§3.8 del levantamiento contable, plantillas 9/13 + quirografario, decisión D7) | BE + FE | — |
| 7 | **N1 caso B** — regeneración parcial preservando cuotas pagadas (decisión U2) | BE, **en el frente de reestructuración** | — |

**Sí se compila acá.** `mvn -q compile` funciona (Maven 3.9.8 / JDK 21) — verificado el 2026-08-31,
contra lo que decía `CLAUDE.md`, ya corregido. Toda entrega de este frente se compila antes de darse
por cerrada. El despliegue sigue siendo por Eclipse.

**Estado al 2026-08-31, tras actualizar a `79059b2`:** `mvn -q compile` **pasa limpio** con las cinco
correcciones de este documento aplicadas. Primera entrega de este equipo verificada por compilador y
no por lectura.

> **Lección de proceso, que costó dos mensajes:** el primer `mvn -q compile` falló por un import de
> `LocalDate` en `CobroCreditoServiceImpl` y se reportó como *«`main` está roto»*. **Era un checkout
> viejo**: el arreglo ya estaba en `origin/main`, tres commits adelante. **`git fetch` antes de
> concluir que un archivo ajeno está roto** — un fallo de compilación puede ser tuyo, de otro equipo,
> o de nadie.

---

## 7. Preguntas abiertas para el usuario

1. ~~**N3** — ¿de dónde salen el desgravamen y el seguro de incendio de un crédito nuevo?~~
   **RESUELTA — ver §8, decisión U1.**
2. ~~**N1** — ¿se permite regenerar la tabla de un préstamo?~~ **RESUELTA — ver §8, decisión U2.**
3. **Aprobación — ¿cuántos niveles?** `CRD.CRDT` modela rangos de monto contra `idProceso`, lo que
   sugiere que el sistema origen tenía niveles. Falta saber cuántos y quién firma cada uno. **No
   bloquea** los puntos 1-3 del orden de trabajo; sí bloquea el punto 5.

---

## 8. Decisiones del usuario — 2026-08-31

### U1 — Seguros en el generador real (cierra N3 y N4)

**El desgravamen se calcula con la fórmula del simulador. El seguro de incendio queda en 0 hasta que
se cargue la póliza.**

En términos de `ParametrosAmortizacion`, para `PrestamoServiceImpl.generarAmortizacion`:

| Parámetro | Valor | Por qué |
|---|---|---|
| `calcularDesgravamenSobreSaldo` | `true` | Es la fórmula del simulador: `saldo * 1.12 / 1000` sobre el saldo de capital **antes** de amortizar cada cuota (`FACTOR_DESGRAVAMEN_SOBRE_SALDO`, confirmada por el usuario el 2026-08-27) |
| `desgravamenPorCuota` | `0.0` | Queda sin uso cuando el flag de arriba está en `true`; se manda en cero para no dejar un valor muerto que confunda |
| `seguroIncendioPorCuota` | `0.0` | **Decisión explícita, no un pendiente olvidado**: el importe de incendio no se cobra mientras no exista la póliza que lo respalde |

**Consecuencia buscada:** la tabla que genera el sistema coincide con la simulación que el socio
firma, que es lo que N3 estaba rompiendo.

**Consecuencia colateral, aceptada:** la cuota 0 de gracia cobra desgravamen sobre el capital
completo (`agregarCuotaCero`), porque durante la gracia el capital está íntegramente expuesto. No se
implementa `cobrarSegurosEnCuotaCero` (ver N4).

**Enganche con el tercer frente:** cuando exista el modelo de pólizas, el seguro de incendio deja de
ser 0 y pasa a salir de la inscripción del préstamo en la póliza. Es un cambio de origen del dato,
no de la tabla: `ParametrosAmortizacion` ya acepta `seguroIncendioPorCuota` y
`seguroPorNumeroCuota`. **Los créditos otorgados antes de ese día nacen sin seguro de incendio en su
tabla** — hay que decidir entonces si se inscriben retroactivamente, que es justamente una de las
dudas abiertas del frente de seguros.

### U2 — Regeneración de la tabla (cierra N1, parcialmente)

**«Sí, pero solo se afectan las cuotas que no estén pagadas.»**

La regla es absoluta y se implementa entera desde el primer día: **ninguna cuota con pago vigente se
toca nunca**, ni se borra ni se reescribe.

Lo que sí se separa en dos entregas es *hasta dónde llega* la regeneración:

| Caso | Qué hace | Cuándo |
|---|---|---|
| **A — el préstamo no tiene ningún pago** | Se borra la tabla entera y se regenera desde los parámetros del préstamo | **Ahora**, es lo que desbloquea el otorgamiento |
| **B — el préstamo tiene cuotas pagadas** | Se preservan las pagadas y se re-amortiza el tramo pendiente sobre el saldo de capital restante | **En el frente de reestructuración**, ver abajo |

**Por qué B no va ahora, y no es una rebaja del alcance.** Re-amortizar el tramo pendiente sobre el
saldo restante, respetando el calendario original de vencimientos, **es exactamente la máquina de la
reestructuración**, y ya existe una tercera copia de esa matemática en
`AbonoCapitalPrestamoServiceImpl` (que corta, copia a `HistDetallePrestamo` y reconstruye la cola —
`:193-199`). Escribirla una cuarta vez para el caso B, y después una quinta para la
reestructuración, es literalmente el riesgo por el que `ALCANCE-EQUIPOS-CRD.md` puso otorgamiento y
reestructuración en el mismo equipo: *"dos equipos tocando el motor de amortización en paralelo es
cómo se rompe la cartera en silencio"*. Acá serían dos entregas del mismo equipo, que es apenas
menos malo.

**Mientras tanto el usuario no queda sin salida:** el caso B **rechaza con un mensaje que nombra la
cuota pagada que lo impide**, en vez de fallar en silencio o corromper la tabla. Y el caso A cubre
el escenario real del otorgamiento, que es corregir un error de digitación antes del desembolso,
cuando todavía no hay ni un pago.
