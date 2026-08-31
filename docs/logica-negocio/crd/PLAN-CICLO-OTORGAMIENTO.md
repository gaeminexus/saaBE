# Plan — ciclo de otorgamiento de créditos

**Fecha:** 2026-08-31 · **Equipo:** CRD · EQUIPO B · **Árbitro:** `omen-saa-1-arb`
**Precondición:** `REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md` — el gate del motor está cerrado.

> **Qué cambia respecto del plan anterior.** `LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md` describía el
> otorgamiento como *"un proyecto entero"*: solicitud, evaluación, aprobación con niveles,
> otorgamiento, desembolso. Con las decisiones del usuario del 2026-08-31 (§1) y lo que ya existe en
> el código (§2), el frente se reduce a **poner una máquina de estados sobre una pantalla de alta que
> ya funciona**. No hay tabla nueva, no hay motor nuevo, no hay bandeja de aprobación.

---

## 1. Decisiones del usuario — 2026-08-31

### U3 — Sin niveles de aprobación

**«Solo el proceso en pantalla ya debe permitir realizarlo.»**

No hay bandeja, ni ruteo por monto, ni segundo firmante. La aprobación es **un paso más de la misma
pantalla**, hecho por quien la opera.

⛔ **Consecuencia concreta: `CRD.CRDT` (`CreditoMontoAprobacion`) NO se usa.** Esa tabla modela
rangos `montoMinimo`/`montoMaximo` contra un `idProceso`, o sea niveles de aprobación por monto —
existe porque el sistema origen los tenía. **Queda viva en la base y sin cablear.** Se anota acá
para que el próximo que la encuentre no asuma que hay que llenarla: es deuda del sistema anterior,
no un pendiente de este frente.

### U4 — La tabla se genera con la solicitud y se congela al aprobar

La tabla de amortización **se genera antes de aprobar**, y **se puede regenerar todas las veces que
haga falta mientras el préstamo no esté aprobado**. Al aprobar, **se congela para siempre**: no se
regenera nunca más, ni aunque no tenga ningún pago.

Es la guarda que ya se construyó (defecto N1), pero **atada al estado además de a los pagos**:

| Estado | ¿Se puede regenerar? | Por qué |
|---|---|---|
| `PENDIENTE_DE_APROBACION` (6) | **Sí**, con confirmación | Es el momento de corregir un error de digitación |
| `GENERADO` (1) | **Sí**, con confirmación | Ídem: todavía no se aprobó nada |
| `VIGENTE` (2) | **NO**, nunca | Congelada por U4, aunque no tenga pagos |
| `RECHAZADO` (7) | **NO** | No se retoca lo que se rechazó |
| Cualquiera, con pagos | **NO** | Guarda preexistente de N1, se mantiene |

**Las dos guardas conviven y se verifican las dos.** La de estado es la que manda en el ciclo nuevo;
la de pagos sigue siendo necesaria porque la cartera migrada entró por otro camino y ya está en
`VIGENTE` con pagos aplicados.

### U5 — El desembolso es un paso aparte

La pantalla llega **hasta dejar el préstamo aprobado y `VIGENTE`**. El desembolso hacia tesorería es
un proceso propio, posterior, con su propio asiento. `PRST` ya tiene `usuarioAcreditacion` y
`fechaAcreditacion` para registrar cuándo se acreditó.

**Se mantiene separado quien aprueba de quien paga**, que es el control que importa cuando no hay
niveles de aprobación (U3). Es lo único que queda cubriendo esa función.

---

## 2. Lo que ya existe y no hay que construir

Verificado contra el código el 2026-08-31.

| Pieza | Dónde | Estado |
|---|---|---|
| Entidad de la solicitud | `CRD.PRST` (`Prestamo`) | **Existe.** Trae `usuarioAprobacion`, `fechaAprobacion`, `usuarioRechazo`, `fechaRechazo`, `usuarioLegalizacion`, `fechaLegalizacion`, `usuarioAcreditacion`, `fechaAcreditacion`, `fechaAdjudicacion`, `estadoOperacion`, `montoLiquidacion` |
| Máquina de estados | rubro `EstadoPrestamo` | **Existe.** `GENERADO(1)`, `VIGENTE(2)`, `PENDIENTE_DE_APROBACION(6)`, `RECHAZADO(7)`, y los de cierre |
| Alta + generación de tabla | `forms/prestamo/prestamo-edit` → `POST /rest/prst/generarTablaAmortizacion/{id}/{c0}` | **Existe y funciona**, ya con la guarda de regeneración |
| Motor de amortización | `CalculadoraAmortizacionServiceImpl` | **Existe, auditado y corregido** |
| Documentos del crédito | `DocumentoCreditoServiceImpl`, `AdjuntoServiceImpl` | Existe |
| Evaluación por bandas | `BandaProductoServiceImpl`, `ClasificadorBandaServiceImpl` | Existe, no se toca en esta entrega |

⚠️ **`PRSTIDST` es el estado vigente, no `ESPSCDGO`.** Ver `CLAUDE.md` §"qué columna lleva realmente
el estado": `ESPSCDGO` es la FK al catálogo `CRD.ESPS` y **no** el estado operativo. Toda transición
de este plan escribe `PRSTIDST`.

---

## 3. La máquina de estados

```
    [ alta en pantalla ]
             │
             ▼
   PENDIENTE_DE_APROBACION (6)  ──── generar tabla ────►  GENERADO (1)
             │                                                │  ▲
             │                                                │  └── regenerar
             │                                                │      (con confirmación)
             │                                                │
             └──────────── rechazar ──────┐      ┌── aprobar ─┘
                                          ▼      ▼
                                   RECHAZADO (7)   VIGENTE (2)
                                                        │
                                                        ▼
                                            [ desembolso — proceso aparte, U5 ]
```

**Reglas de transición, todas verificadas en el servicio y no solo en la pantalla:**

1. **Un préstamo nuevo creado desde la pantalla nace en `PENDIENTE_DE_APROBACION (6)`.**
   Hoy `saveSingle` pone `Estado.ACTIVO` cuando el código es nulo; eso pasa a ser el estado 6 del
   catálogo correcto.
2. **Generar la tabla lleva de 6 a `GENERADO (1)`.** Regenerar deja el estado en 1.
3. **No se puede aprobar sin tabla.** Aprobar exige estado `GENERADO (1)`: sin cuotas no hay nada
   que aprobar. Desde 6 se rechaza el intento.
4. **Aprobar** → `VIGENTE (2)`, estampa `usuarioAprobacion` y `fechaAprobacion`. A partir de acá la
   tabla está congelada (U4).
5. **Rechazar** → `RECHAZADO (7)`, estampa `usuarioRechazo` y `fechaRechazo`. Se puede rechazar
   desde 6 o desde 1.
6. **Las dos son terminales para este frente.** De `VIGENTE` en adelante manda el ciclo de cobros,
   que ya existe y no se toca.

**La tabla de un préstamo rechazado NO se borra.** Queda como evidencia de qué se le ofreció al
socio. Es inerte: el proceso de mora filtra `PRSTIDST IN (2,8,10,11)` y el 7 no está ahí, así que
nada la procesa.

### ⚠️ Lo que NO hay que romper: la cartera migrada

`prestamo-edit` es hoy la pantalla con la que se registran los préstamos **migrados**, y su tabla
entra por `cargarTablaAmortizacionDesdeExcel`, no por el generador.

- **El camino de Excel no se toca en esta entrega.** Ni su guarda de estado, ni su semántica de
  `DTPRSLDO` (ver D5). Cambiarlo sería retroactivo sobre cartera que ya está en producción.
- Un préstamo migrado que ya está en `VIGENTE` **queda con la tabla congelada** por la regla de U4,
  que es exactamente lo que se quiere.
- **Pendiente conocido, no de esta entrega:** el camino de Excel puede sobrescribir la tabla de un
  préstamo aprobado. Es un hueco preexistente; se anota para no perderlo, no se arregla acá.

---

## 4. Contrato REST

Dos endpoints nuevos en `PrestamoRest` (`@Path("prst")`), en el estilo de la casa: `catch (Throwable)`
y `Response.status(INTERNAL_SERVER_ERROR).entity("Error ...: " + e.getMessage())`.

| Método | Path | Cuerpo | Hace |
|---|---|---|---|
| `POST` | `/aprobar/{id}` | `{ "usuario": "..." , "observacion": "..." }` | 1 → 2, estampa `usuarioAprobacion`/`fechaAprobacion` |
| `POST` | `/rechazar/{id}` | `{ "usuario": "...", "observacion": "..." }` | 6 ó 1 → 7, estampa `usuarioRechazo`/`fechaRechazo` |

Los dos devuelven el `Prestamo` actualizado. Los dos **validan el estado de origen en el servicio** y
rechazan con un mensaje que nombra el estado actual si la transición no aplica — nunca confían en que
la pantalla haya deshabilitado el botón.

`generarTablaAmortizacion` suma la guarda de estado descrita en U4, **además** de la de pagos que ya
tiene.

---

## 5. Alcance de esta entrega

### Entra
- Las dos transiciones (aprobar / rechazar) con sus endpoints, sus validaciones de estado y sus
  campos de auditoría.
- El estado inicial correcto al crear (`PENDIENTE_DE_APROBACION`) y la transición a `GENERADO`.
- La guarda de estado en la regeneración (U4).
- La pantalla: botones de aprobar y rechazar, con confirmación, y el estado visible.

### No entra
- **Desembolso y su asiento** (U5): proceso aparte, toca tesorería y contabilidad.
- **Niveles de aprobación** (U3): descartados, `CRD.CRDT` sin usar.
- **Evaluación de capacidad de pago por bandas**: existe, se conecta después.
- **El camino de carga por Excel**: intacto.
- La plantilla contable de quirografario (decisión D7 del alcance): va con el desembolso.

---

## 5.b ⛔ RIESGO ABIERTO — `GENERADO (1)` puede estar ocupado por la cartera migrada

**Sin resolver al 2026-08-31. Hay que mirar la base antes de desplegar esto.**

El ciclo nuevo le da a `PRSTIDST = 1` el significado *"tiene tabla, falta aprobar"*, y sobre ese
estado habilita **aprobar** y **rechazar**. Pero el 1 ya venía usándose para otra cosa:

- `PrestamoServiceImpl.saveSingle` ponía `Estado.ACTIVO`, que **también vale 1** — es el mismo
  defecto de catálogo cruzado que N5, ahora del lado del préstamo.
- `prestamo-edit.component.ts:432` manda `idEstado: 1` al dar de alta, y **esa es la pantalla con la
  que se registró la cartera migrada**.

**Si hay préstamos migrados sentados en `PRSTIDST = 1`, el ciclo nuevo los trata como solicitudes
pendientes**, y a un operador le van a aparecer los botones de aprobar y rechazar sobre créditos
vivos. Las dos transiciones hacen daño:

- **Aprobar** lo mueve a `VIGENTE (2)`, y el proceso de mora de las 02:00 filtra
  `PRSTIDST IN (2,8,10,11)`: un préstamo que hoy no devenga **empezaría a devengar**.
- **Rechazar** lo mueve a `RECHAZADO (7)`, que **no** está en ese filtro: un préstamo vivo
  **dejaría** de devengar y saldría de la cartera en silencio.

Ninguna de las dos avisa, porque desde el punto de vista del código son transiciones válidas.

**La consulta que lo resuelve** — el conteo por estado de toda la cartera, que hasta ahora nunca se
sacó (la medición de D10 solo contó los estados 2, 8, 10 y 11):

```sql
SELECT  p.PRSTIDST                                  AS ESTADO,
        COUNT(*)                                    AS PRESTAMOS,
        MIN(TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD'))       AS INICIO_MAS_VIEJO,
        MAX(TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD'))       AS INICIO_MAS_NUEVO,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM CRD.DTPR d
                              WHERE d.PRSTCDGO = p.PRSTCDGO)
                 THEN 1 ELSE 0 END)                 AS CON_TABLA,
        SUM(NVL(p.PRSTTTPG,0))                      AS TOTAL_PAGADO
FROM    CRD.PRST p
GROUP   BY p.PRSTIDST
ORDER   BY p.PRSTIDST;
```

**Cómo leerla:** si el estado 1 sale con **0 préstamos**, no hay riesgo y el ciclo entra tal cual. Si
sale con préstamos que tienen tabla y `TOTAL_PAGADO > 0`, son cartera viva mal etiquetada y **hay que
separarlos antes de desplegar** — o migrándolos al estado que les corresponda, o agregando una
condición extra a las dos transiciones (por ejemplo, exigir `PRSTFCRG` posterior a la fecha de
arranque del ciclo nuevo).

**Hasta que esa consulta se corra, este frente no se despliega.**

---

## 6. Pendientes abiertos

1. **El asiento de entrega del préstamo** — §3.8 del levantamiento contable, plantillas **9**
   (prendario) y **13** (hipotecario); la de **quirografario no existe y hay que crearla** con el
   mismo patrón (decisión D7). Va junto con el desembolso, no acá. **Toca contabilidad: avisar al
   equipo A antes de empezar.**
2. **`countByIdDetallePrestamo`** — lo escribe el equipo A; cuando llegue a `origin`, la guarda de
   pagos cambia una línea y se cierran los dos agujeros documentados en
   `REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md` §3.
3. **El camino de Excel puede pisar la tabla de un préstamo aprobado** (§3). Preexistente.
4. **`estadoOperacion` (`PRSTESOP`)** — existe en `PRST` y no se sabe qué significa. No se toca
   hasta saberlo.
