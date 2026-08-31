# Plan — enviar el excedente de la carga Petro a un aporte

**Fecha:** 2026-08-30 · **Módulo:** CRD / ASOPREP · Escrito por el árbitro `saabe-4b`
**Estado:** diseño. **No construido.**

---

## 0. El requerimiento

Cuando la carga Petro trae **más dinero del esperado** para un partícipe, la pantalla de novedades
hoy permite **aplicar ese excedente a otro préstamo**. Falta que permita también **enviarlo a un
aporte de jubilación o cesantía**.

Regla del usuario: **según cuáles tenga el partícipe.** Si tiene los dos, que se pueda elegir; si
tiene uno solo, que se pueda enviar a ese.

Y: **la contabilidad tiene que generarse correctamente** cuando se active.

---

## 1. Lo que hay hoy, verificado en código

`CRD.AVPC` (`AfectacionValoresParticipeCarga`) es **exclusivamente de préstamos**:

| Campo | Qué es |
|---|---|
| `NVPCCDGO` | La novedad que originó el excedente (padre) |
| `PRSTCDGO` | El préstamo al que se afecta |
| `DTPRCDGO` | **La cuota** a afectar |
| `AVPCVAFA` | Valor total a afectar |
| `AVPCCPAF` / `AVPCINAF` / `AVPCDGAF` | El desglose: capital, interés, desgravamen |

**No hay ningún campo de tipo de aporte.** Y en
`CargaArchivoPetroServiceImpl` (línea ~1359) la aplicación arranca con:

```java
if (afectacion.getDetallePrestamo() == null) {
    continue;
}
```

⚠️ **Una afectación sin cuota se descarta EN SILENCIO.** Ese `continue` es hoy una guarda
defensiva; con este cambio pasa a ser **el punto donde se bifurca** entre préstamo y aporte. Si se
agrega el campo del aporte sin tocar esa línea, **el excedente se guardaría y no se aplicaría
nunca**, sin ningún error — el dinero quedaría registrado como afectado y no llegaría a la cuenta
del socio.

---

## 2. El antecedente contable: ya está levantado

`LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` **§3.7 (Cobro en exceso)** anota tres opciones
decididas con contabilidad:

> **① se lo devuelve ✓, ② se afecta al préstamo, ③ se aplica a cuenta individual.**

**Este requerimiento es la opción ③**, que nunca se construyó. La ② es la que existe hoy.

No hay que inventar el asiento: es el mismo movimiento que un aporte registrado —
**H a la cuenta de aporte del tipo que corresponda** (`2.1.01.05.01` cesantía,
`2.1.02.05.01` jubilación), contra el debe que ya usa el reparto de Petro.

⚠️ **La cuenta sale de la plantilla alterno 21**, por el `aux1` semántico:
**50 = cesantía, 51 = jubilación, 52 = adicional**. Verificado contra `CNT.DTPL`. **No escribir
cuentas en el código** — es la misma regla que ya rige en el resto del módulo.

---

## 3. Qué significa "los que tenga el partícipe" — RESUELTO

**Decisión del usuario (2026-08-30):** *"basta con que se lo haya incluido en el archivo Petro de
ese mes para que se le descuente AH. La misma condición que se usa para incluirlo."*

**Esa condición existe y tiene un solo dueño:**

```java
VigenciaContratoService.esperadoPorEntidad(idEntidad, idTipoAporte, mes)
```

La regla, del javadoc: **contrato activo** (desempate por mayor código) **+ vigencia en estado
ACTIVO cuyo rango cubra el último día del mes**. Devuelve el monto esperado, o **0** si no hay
vigencia. Es la misma que usa `GeneracionArchivoPetroServiceImpl` para decidir a quién incluir en
el archivo — vía `esperadoEnLotePorFilial`, que es la versión en bloque de la **misma regla**.

**Aporta a ese tipo, ese mes, quien devuelve > 0.** Eso ofrece la pantalla.

⚠️ **NO reimplementar la regla ni leer `CNTR` directo.** El javadoc lo dice explícitamente:
*"Quien cambie la regla de selección de vigencia la cambia acá, no en el llamador"*. Los campos
`CNTRPRAI`/`CNTRPRAJ`/`CNTRMNAJ`/`CNTRMNAC` del contrato son **espejo** de la vigencia, no la
fuente — leerlos sería exactamente el defecto de "el dato no viene de donde parece".

### Dos consecuencias que se siguen de la regla

1. **Quien aportaba antes y ya no, queda excluido**: su vigencia no cubre ese mes. Coherente con
   el criterio del usuario — si no se le descontó en el archivo de ese mes, tampoco recibe
   excedente ahí.
2. **La regla se evalúa POR MES**, con el mes de la carga que se está procesando, no con la
   situación de hoy. Al reprocesar una carga vieja se ofrecen los tipos que estaban vigentes
   **entonces**. Es lo correcto, pero conviene saberlo antes de que sorprenda.

---

## 4. Diseño

### 4.1 Modelo

Agregar a `CRD.AVPC` la columna **`TPAPCDGO`** (FK a `CRD.TPAP`), nullable.

Una fila de afectación pasa a ser **o de préstamo o de aporte**, nunca las dos:

- **De préstamo:** `PRSTCDGO` + `DTPRCDGO` presentes, `TPAPCDGO` null. Con su desglose
  capital/interés/desgravamen.
- **De aporte:** `TPAPCDGO` presente, `PRSTCDGO`/`DTPRCDGO` null. **Sin desglose** — un aporte no
  tiene capital ni interés: solo `AVPCVAFA`.

Un `CHECK` que garantice la exclusividad, igual que se hizo en `DCBC` con `idPrestamo` XOR
`idTipoAporte`.

### 4.2 Aplicación

En `CargaArchivoPetroServiceImpl`, **reemplazar el `continue` por la bifurcación**:

- Con cuota → lo de siempre.
- Con tipo de aporte → **registrar el aporte por el camino que ya existe** (`AporteService`), con
  el período de devengo de la carga y el tipo de movimiento que corresponda.

⚠️ **No reimplementar el registro de aportes.** Ya existe, ya está probado, y ya resuelve el
período de devengo, la glosa y el enlace a la carga (`CRARCDGO`).

⚠️ **El tipo de movimiento importa:** el rubro 235 distingue de dónde viene cada aporte. Este no es
un aporte mensual normal — **decidir si usa uno existente o necesita uno nuevo**, y si es nuevo,
reservarlo en `REGISTRO-RESERVAS-EQUIPOS.md` antes de usarlo.

### 4.3 Contabilidad

La línea del haber sale de la plantilla 21 por `aux1` 50/51/52 (§2). El **helper compartido**
`ContabilizacionIndividualCreditoService.lineaAporteRegistrado` —construido el 2026-08-30 para
`CBCRASN2`— **ya resuelve exactamente esa línea**. Usarlo, no escribir una segunda resolución de
la misma cuenta.

### 4.4 Pantalla

Donde hoy se elige el préstamo destino, ofrecer también **jubilación / cesantía**, con los tipos
que correspondan según §3, y **mostrando cuánto tiene hoy en cada uno** — el operador está
decidiendo a qué cuenta del socio va la plata.

Que quede claro en pantalla que **el excedente a un aporte no baja ninguna deuda**: aumenta el
saldo del socio. Es la misma distinción que ya se aplicó en el comprobante y en el cobro mixto.

---

## 5. El reparto y su cuadre — decisión del usuario (2026-08-30)

**Un mismo excedente SÍ se puede repartir** entre un préstamo y un aporte. Son filas distintas de
`AVPC` colgando de la misma novedad, y el modelo de §4.1 ya lo soporta.

**Pero la suma de lo repartido tiene que ser EXACTAMENTE el excedente.** Palabras del usuario:
*"ni más ni menos, o si no no hay cómo procesar"*.

### Dónde se valida, y por qué en los dos lados

**1. En la pantalla**, para que el operador no pueda confirmar un reparto que no cuadra — con el
indicador de repartido/total que ya se usa en precancelación y en el acuerdo.

**2. ⛔ Y en el PROCESO del archivo, que es la que de verdad protege.** Antes de aplicar, verificar
que **cada novedad con excedente lo tenga repartido al 100%** (tolerancia $0.01). Si alguna no
cuadra, **el proceso no corre** y el mensaje dice qué partícipe y cuánto falta o sobra.

**Por qué no alcanza con la pantalla:** las afectaciones se guardan por novedad y se aplican
**después**, en el proceso del archivo. Entre los dos momentos alguien puede editar, borrar una
fila, o dejar una novedad a medio repartir y procesar igual.

⚠️ **Y hoy ese hueco pierde dinero en silencio.** La aplicación arranca con
`if (afectacion.getDetallePrestamo() == null) continue;` — una afectación sin destino **se saltea
sin avisar**. Con el reparto parcial permitido, esa línea convierte un reparto incompleto en plata
que se registró como afectada y nunca llegó a ningún lado. **El control de cuadre al procesar es
lo que cierra eso**, y es la razón por la que el requisito del usuario no es una comodidad de
pantalla sino una condición para poder procesar.

---

## 6. Lo que queda por decidir

1. **Si el excedente a aporte lleva un tipo de movimiento propio** en el rubro 235 (hoy:
   `APORTE_MENSUAL`, `AJUSTE_MANUAL`, `DEVOLUCION`, `PAGO_PRESTAMO`, `REVERSO`, `MIGRADO`,
   `JUBILACION`), o reusa uno existente. **Lo propone el backend al construir**; si hace falta uno
   nuevo, se reserva en `REGISTRO-RESERVAS-EQUIPOS.md` antes de usarlo.
