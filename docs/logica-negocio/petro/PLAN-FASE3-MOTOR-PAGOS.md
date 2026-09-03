# Rediseño de la prelación de la fase 3 de la carga Petro

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** plan aprobado, pendiente de implementar

> **Decisión del usuario, 2026-09-02:** *«Respecto a los decidibles, que se rediseñe la prelación de la fase 3»*.
> El disparador fue que la carga 449 dejó $1.284,42 de mora sin cobrar y 279 cuotas en PARCIAL.

---

## 1. Qué se descubrió midiendo, y por qué cambia el plan

La brecha de **$2.906,52** entre lo repartido y lo aplicado quedó localizada con datos, no con
argumentos. Tres scripts, tres respuestas:

| Script | Qué preguntó | Resultado |
|---|---|---|
| `sql/170` | ¿La brecha está en las cuotas? | **No.** 1.092 cuotas, brecha **0,01**. `DTPRTTLL` − mora = 275.464,51; capital+interés+desgravamen+seguro = 275.464,50. Las cuotas cuadran perfecto. |
| `sql/171` bloque 1 | ¿De qué ruta sale? | **`PAGO_NORMAL`: brecha 0.** `AFECTACION_MANUAL`: 285 pagos, **brecha 2.906,52**. Los 2.906,52 están íntegros ahí. |
| `sql/171` bloque 2 | ¿Cuál de los dos defectos? | El **(B)**, con diferencia. Caso extremo: pago 306016, préstamo 7973, cuota 10 — se afectaron **$529,47** y la cuota sólo debía **$0,68** de capital. Los **$528,79** restantes se grabaron en `PGPRVLRR` y **no fueron a ningún lado**. |

**La causa raíz no es un cálculo mal hecho: es que la afectación manual no tiene cascada.**
Aplica a UNA cuota y lo que no cabe se descarta —
`aplicarAfectacionManualConRegistroPago:3152` lo imprime (`⚠️ Excedente no aplicado`) y después
graba `valorTotalAfectar` **completo** como total del pago. El dinero desaparece del desglose pero
no del total, y ahí nace el descuadre exacto que contabilidad no acepta.

> **Hipótesis mía descartada, y queda escrita para no volver a perseguirla:** sostuve durante media
> jornada que `calcularSaldosRealesCuota` calcula el total de dos formas distintas según la cuota
> tenga o no pagos previos, y que en la cartera migrada esas dos formas no coinciden. Las dos ramas
> existen y siguen siendo feas, pero **el `sql/170` prueba que no producen este descuadre**. También
> queda descartado el commit `a09732f` (arreglo del seguro por deducción algebraica): bajó el seguro
> grabado de 1.124,28 a 893,49 y **subió la brecha 230,79, exactamente lo mismo**. Hay que revertirlo.

---

## 2. La decisión de fondo: no se escribe una prelación nueva, se converge en la que ya existe

**`MotorPagoPrestamoServiceImpl` ya es el rediseño que el usuario pidió**, escrito el 2026-08-14
según `crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md` §6. Su propio javadoc dice para qué nació:

> *«Código NUEVO: es una copia adaptada del comportamiento de `CargaArchivoPetroServiceImpl`
> (calcularSaldosRealesCuota, procesarPagoCuota, procesarExcedenteASiguienteCuota,
> verificarYActualizarEstadoPrestamo, crearRegistroPago), **extendida con mora e interés vencido y
> con la prelación de 6 componentes**. Aquel servicio NO se modifica; **la convergencia de ambos es
> una fase futura**.»*

**Esa fase futura es ésta.** Escribir una segunda prelación dentro de la carga Petro dejaría al
sistema con dos motores de pago que divergirían al primer cambio de negocio.

### La prelación de 6 componentes (`MotorPagoPrestamoServiceImpl:367`)

Orden confirmado por negocio el 2026-08-14:

1. Seguro de incendio
2. Seguro de desgravamen
3. **Interés de mora**
4. Interés vencido *(hoy siempre 0: ningún proceso lo alimenta)*
5. Interés ordinario
6. Capital

Primero los seguros, después la deuda vieja, después el interés corriente, por último el capital.

### Y ya trae la invariante que cierra la brecha

```java
if (restante > TOLERANCIA) {
    // Solo ocurre con datos legacy donde DTPRTTLL no cuadra con la suma de los
    // componentes. Se registra el desfase y se imputa SOLO lo que los componentes
    // pudieron absorber, para que PGPR siga cumpliendo "los componentes suman el valor";
    // el sobrante vuelve a la cascada.
    montoAplicar = totalAplicado;
}
```

**«Los componentes suman el valor» es exactamente el invariante que la carga Petro viola hoy.**
Y `aplicarPago(idPrestamo, valor, ctx)` cascada sola: imputa a la mínima cuota pendiente, y sólo
cuando la agota pasa a la siguiente — que es literalmente la regla que pidió el usuario
(*«para poder pasar a la siguiente cuota debe terminar de pagar la cuota anterior»*). Lo que sobra
al final del préstamo no se descarta: vuelve en `ResultadoAplicacionPago.excedenteNoAplicado`.

### Lo que se resuelve de un solo movimiento

| | Hoy | Con el motor |
|---|---|---|
| Mora | nunca se cobra (H de `sql/167`) | se cobra, posición 3 de la prelación |
| Brecha total vs componentes | $2.906,52 | imposible por construcción |
| **H19** — dinero descartado en silencio | dos `return` mudos | `excedenteNoAplicado` devuelto al llamador |
| **H20** — cuotas salteadas | medido, préstamo 4456 saltó diez | cascada desde la mínima pendiente |
| Cuotas PARCIAL y sigue de largo | 279 cuotas | sólo queda PARCIAL la última que el dinero no alcanzó |
| Dos implementaciones de lo mismo (**P14**) | dos motores | uno |

---

## 3. ⛔ Prerrequisito duro: las líneas de mora de la plantilla

**Correr `sql/172` ANTES de subir el WAR.** No es una verificación de prolijidad.

`CobroPetroContableServiceImpl:850-861` contabiliza la mora por tipo de préstamo y, si no encuentra
la línea en la plantilla, **no la saltea: lanza `IncomeException`** — que es
`@ApplicationException(rollback = true)`. Si falta UNA línea para UN tipo de préstamo, el asiento de
aplicación revienta y **se revierte la carga entera** después de los 20+ minutos de proceso.

Hoy ese `throw` es inalcanzable porque `moraPorTipo` siempre queda vacío. **El momento en que la
fase 3 empiece a cobrar mora es exactamente el momento en que se vuelve alcanzable.** Misma trampa
que el `.jasper` faltante: compila, pasa revisión, entra al commit y revienta con el usuario adelante.

Si el bloque 1 del `172` devuelve filas → **no se sube**, se crean primero esas líneas.

---

## 4. Qué se cambia, exactamente

Todo dentro de `ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java`, salvo los dos puntos
marcados.

### 4.1 Trazabilidad de la carga — **el riesgo de integración crítico**

`ContextoPago` **no tiene** el id de la carga, y `MotorPagoPrestamoServiceImpl.crearRegistroPago`
**nunca hace** `pago.setCargaArchivo(...)`.

`PGPR.CRARCDGO` es de lo que cuelga **todo el asiento de aplicación**: `contabilizarAplicacion`
agrupa los pagos por carga. Si se migra la fase 3 al motor sin esto, los pagos quedan con
`CRARCDGO` nulo, el asiento sale **vacío** y el descuadre pasa de $2.906,52 a $351.927,95 sin un
solo error en el log.

- Agregar `Long idCargaArchivo` a `com.saa.ejb.crd.service.dto.ContextoPago` (+ getter/setter).
- En `MotorPagoPrestamoServiceImpl.crearRegistroPago`, resolver la carga y hacer
  `pago.setCargaArchivo(...)` cuando `ctx.getIdCargaArchivo() != null`. Cuando es null, comportamiento
  idéntico al de hoy — ningún otro llamador del motor se entera.

### 4.2 Fecha de efecto

`ctx.setFechaPago(fechaService.ultimoDiaMesAnioLocal(mes, anio).atTime(23,59,59))`, con el mes y año
de afectación de la carga. **Sin fallback a `now()`**: si vienen null, gritar. Es el criterio ya
fijado el 2026-09-02 (H21) y del que depende la clasificación por banda contable.

### 4.3 Ruta normal (fase 3)

`procesarPagoCuota` + `procesarExcedenteASiguienteCuota` → una sola llamada a
`motorPagoPrestamoService.aplicarPago(idPrestamo, montoDescontado, ctx)`.

El seguro de incendio deja de viajar como parámetro suelto: el motor lo toma del saldo real de la
cuota, que es lo que el commit `a09732f` intentaba lograr por el camino difícil.

### 4.4 Afectación manual — **el cambio de comportamiento que hay que declarar**

`aplicarAfectacionManualConRegistroPago` pasa a usar `aplicarPago` con el `valorAfectar` de AVPC.

**Lo que AVPC define sigue siendo del operador: a qué préstamo y cuánto.** Lo que cambia es que el
desglose interno (capital/interés/desgravamen) lo decide la prelación en vez del formulario.

**Por qué es la única opción coherente, y no una preferencia mía:**

- AVPC **no tiene campo de seguro de incendio ni de mora**. El propio código lo dice en `:3197`:
  *«Cuota tiene seguro de incendio pero NO se puede afectar manualmente (campo no existe en tabla
  AVPC)»*. Un desglose digitado **nunca puede estar completo**, así que nunca puede sumar el total.
- El desglose digitado es justamente lo que produce la brecha: el total se graba entero y los
  componentes no lo cubren.
- Las bandas contables se clasifican por componente. Un desglose digitado a mano manda plata a la
  banda equivocada sin ningún aviso.

> ⚠️ **Decidible pendiente, no bloqueante.** El 2026-09-01 el usuario dijo *«debes procesarlas según
> se haya ingresado en la tabla de ajuste de novedades. Eso lo define el usuario»*. Eso resolvía
> **a qué destino y por cuánto** — que se respeta igual. Este plan avanza asumiendo que **no**
> cubría el reparto interno entre componentes de una misma cuota. Si el usuario quiere que el
> desglose digitado mande, hay que agregar los campos de seguro y mora a AVPC primero, y aun así el
> total tendría que validarse contra la suma.

### 4.5 El excedente ya no se descarta nunca

`aplicarPago` devuelve `excedenteNoAplicado`. Cuando sea > 0,01:

- **No** grabarlo en ningún `PGPRVLRR`.
- Registrar novedad **bloqueante** con el monto y el préstamo, para que el operador lo distribuya en
  la pantalla de afectación.

Es la regla del usuario del 2026-09-02: *«todo el dinero recibido se debe repartir para que la
contabilidad cuadre»*. Dinero sin destino **es** el descuadre; que bloquee es el punto.

### 4.6 Guarda de invariante

En el `crearRegistroPago` de la carga (si sobrevive alguno) y en el del motor: si
`|valor − Σ componentes| > 0,01`, **lanzar**. Es lo que habría hecho imposible este defecto y las
otras cuatro semanas que costó encontrarlo.

---

## 5. Qué NO se toca

- **`totalBaseCuota:888`** deja de tener sentido cuando el motor manda (el motor usa saldos reales
  por componente, mora incluida). No borrarlo a ciegas: verificar que no quede ningún otro llamador.
- **Fase 1 y fase 2** de la carga: validaciones y novedades. Fuera de alcance.
- **`CobroPetroContableServiceImpl`**: ya contabiliza mora (`moraPorTipo`, línea `AUX1 = 20`). No
  necesita cambios — necesita que existan las líneas (§3).
- **Los demás llamadores del motor**: abono a capital, precancelación, reverso. `idCargaArchivo` es
  opcional y en null se comportan igual que hoy.

---

## 6. Verificación antes de dar por entregado

1. `mvn -q compile` — es la única verificación automática que existe en este repo.
2. `sql/172` bloque 1 vacío.
3. Reprocesar la 449 sobre el respaldo y correr `sql/171`: **`BRECHA = 0` en las dos filas**.
4. `sql/167` bloque 3 (regla de no saltear cuotas): **0 filas**.
5. `sql/167` bloque 4: `MORA_COBRADA` > 0 — hoy da 0.
6. El asiento de aplicación debe traer línea de mora y cuadrar contra el de reparto.

---

## 7. Documentación obligatoria en el mismo cambio

Regla de `CLAUDE.md`: todo cambio en `CargaArchivoPetroServiceImpl` actualiza su documento.

- `docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md` — reescribir la fase 3: prelación de 6
  componentes, cascada, mora, excedente que bloquea.
- `docs/logica-negocio/petro/REGLAS-GENERALES-PETRO.md` — la mora ahora se cobra en la carga.
- `docs/logica-negocio/crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md` — la convergencia dejó de ser
  «fase futura»; anotar que la carga Petro es llamadora del motor.
