# Plan — desembolso del préstamo y su contabilidad

**Fecha:** 2026-09-01 · **Equipo:** CRD · EQUIPO B · **Árbitro:** `omen-saa-1-arb`
**Cierra:** el §6.1 de `PLAN-CICLO-OTORGAMIENTO.md` (el pendiente que quedó abierto al entregar el ciclo).
**Precondición:** el ciclo de otorgamiento está entregado y su gate pasó (`sql/151`, estado 1 vacío).

---

## 1. Decisión del usuario — 2026-09-01

> **«El desembolso se realiza por el módulo de TSR. Lo que debe hacer es alimentar ese módulo para
> realizar el pago. Pero a excepción del asiento de bancos (que se generaría cuando se confirme el
> desembolso del banco) se genere la contabilidad al alimentar TSR.»**
>
> Y sobre el momento del asiento: **«Parte de la contabilidad al aprobar y solo el asiento contra
> bancos al confirmar el desembolso.»** Monto **siempre completo**.

**Traducción a mecánica, y lo importante es que esto NO es un diseño nuevo:**

| Momento | Quién | Qué pasa |
|---|---|---|
| **Aprobar** (`prst/aprobar`) | **CRD** | El préstamo pasa a `VIGENTE`, se escribe el **asiento de entrega** (cartera contra socios por pagar) y **se alimenta el circuito de pago** con la orden hacia tesorería |
| **Confirmar el pago** | **TSR / CXP** | Tesorería aprueba y paga la orden; **CXP arma el asiento contra bancos** con el desglose que CRD dejó |

**El desembolso no lo ejecuta CRD.** CRD deja la orden puesta; tesorería la ve en su bandeja, le
asigna cuenta y forma de pago, y paga. Es exactamente el rol que ya cumplen la devolución de
aportes y el pago de pensión complementaria.

---

## 2. ⭐ El diseño ya estaba implícito en la plantilla contable, y eso lo valida

**La plantilla de entrega no tiene cuenta de bancos.** Verificado contra producción el 2026-09-01
(`sql/153`), la plantilla 9 (prendario) y la 13 (hipotecario) son:

```
DEBE   1.3.0x.05 / .10 / .15 / .20 / .25    cartera por tramo de plazo
DEBE   7.3.01.05                            CARTERA DE CREDITOS   (cuenta de orden)
HABER  7.4.01.05 (+ la del bien)            DOCUMENTOS EN GARANTIA (cuenta de orden)
HABER  2.3.90.90.10                         SOCIOS POR PAGAR
```

**`2.3.90.90.10 SOCIOS POR PAGAR` es la cuenta puente**, y es la pieza que hace que todo encaje:

- **Al aprobar**, se acredita: el fondo reconoce la cartera y reconoce que le debe el dinero al socio.
- **Al pagar**, tesorería la debita contra bancos: la deuda con el socio se cancela y sale la plata.

Los dos asientos son las dos mitades de la misma operación, y **la separación que pidió el usuario
es la que las plantillas ya suponían.** No hay que inventar cuentas ni forzar nada: la plantilla 34
que se creó (`sql/156`) sigue el mismo patrón y por eso tampoco lleva bancos.

---

## 3. Lo que ya existe y NO hay que construir

Verificado contra el código el 2026-09-01.

| Pieza | Dónde | Estado |
|---|---|---|
| Alimentar el circuito de pago desde CRD | `PagoProgramadoService.registrarPagoDeOrigenExterno(...)` | **Existe.** Código de CXP escrito como neutral, sin conocimiento de CRD |
| Dos consumidores en producción, para copiar el patrón | `DevolucionAporteServiceImpl:556`, `PagoPensionComplementariaServiceImpl:307` | Existen y funcionan |
| El asiento contra bancos al confirmar | **CXP lo arma solo**, desde el desglose en `PGS.DPGT` | **Existe.** Una línea DEBE por producto de pago, una línea HABER al banco por el total (§3.2 de `PLAN-DEVOLUCION-APORTES.md`) |
| Beneficiario ocasional | `BeneficiarioOcasional` + `armaBeneficiario(...)` | Existe |
| Campos de acreditación en el préstamo | `PRST.usuarioAcreditacion`, `PRST.fechaAcreditacion` | Existen, sin usar |
| La plantilla contable de quirografario | alterno **34**, `sql/156` | **Escrita, sin ejecutar** |

> ⚠️ **Consumir `PagoProgramadoService` NO es tocar CXP ni TSR.** Es un servicio diseñado para
> consumirse desde fuera, y ya tiene dos consumidores de `crd`. Este frente **no modifica un solo
> archivo de `cxp` ni de `tsr`** — respeta el alcance del equipo.

---

## 4. Lo que falta, y es poco

### 4.1 DDL — un campo en `CRD.PRST`

**`PRST` no tiene dónde guardar el id de la orden de pago.** Verificado: `grep idPagoProgramado` en
`Prestamo.java` → cero resultados. Hace falta:

```sql
ALTER TABLE CRD.PRST ADD (PRSTIDPG NUMBER);
```

Es el mismo patrón de `CRD.DVAP.DVAPIDPG` y `CRD.PGPC`. **Sin FK, a propósito**, igual que allá: CRD
no puede tener una FK dura contra el schema de pagos (§1 de `PLAN-DEVOLUCION-APORTES.md`).

⛔ **El DDL va ANTES del WAR.** Hibernate incluye toda columna `@Column` en el `SELECT`: si la
entidad mapea `PRSTIDPG` y la columna no existe, **toda lectura de `CRD.PRST` revienta con
ORA-00904** — o sea la cartera entera, no la función nueva. Es exactamente lo que tumbó la pantalla
de cobros el 2026-08-31 con `CBCRASRP`.

### 4.2 Un valor nuevo en `OrigenPagoExterno`

```java
public static final String CRD_DESEMBOLSO_PRESTAMO = "CRD_DESEMBOLSO_PRESTAMO";
```

**Es un archivo compartido y el equipo `omen-saa-2` lo modificó hoy** (`a820203`, `bb9bccb`, para
`RHH_NOMINA` y `RHH_BENEFICIO_SOCIAL`). Son constantes de **texto**, así que agregar una es aditivo
y no puede colisionar por número — pero **hay que avisar al otro árbitro antes de tocarlo**, y hacer
`git pull` inmediatamente antes de editar.

### 4.3 El producto de pago de `SOCIOS POR PAGAR` — **hay que consultarlo, no inventarlo**

El desglose que se le pasa a CXP son pares **(producto de pago, valor)**, y CXP arma el asiento
usando la cuenta del `GrupoProductoPago.planCuenta` de cada producto. Para que el asiento de bancos
salga **DEBE `2.3.90.90.10` / HABER banco**, hace falta el producto de pago cuyo grupo apunte a
`2.3.90.90.10`.

**Puede existir ya** —la devolución de aportes usa `CRD.TPAP.TPAPPRDP` para un mapeo equivalente— o
puede haber que crearlo. **Se resuelve con `sql/157` antes de escribir código**, no suponiendo.

---

## 5. Contrato

`aprobar` **no cambia de firma ni de ruta**: suma efectos. Es deliberado — el frontend ya está
construido y probado contra ese contrato, y no hay motivo para moverlo.

```
POST /rest/prst/aprobar/{id}
{ "usuario": "...", "observacion": "...", "idEmpresa": 1236, "idUsuario": 42 }
```

**Dos campos nuevos en el cuerpo**, los dos obligatorios y por el mismo motivo que en el resto del
circuito: `idEmpresa` es la empresa contable del asiento, `idUsuario` es quien registra la orden.

Devuelve el `Prestamo` actualizado, ahora con `idPagoProgramado` lleno.

### Orden de las operaciones dentro de `aprobar` — no es indistinto

**CXP primero, contabilidad y estado después.** Es la lección que dejó escrita
`DevolucionAporteServiceImpl:540`: *«un log que anuncia éxito antes de que la operación pueda fallar
es peor que no tener log»*. Si la orden de pago falla, no debe quedar ni asiento ni préstamo
aprobado.

1. Validar estado `GENERADO` y que tenga tabla (ya está).
2. **Registrar la orden de pago** (`registrarPagoDeOrigenExterno`). Si falla → `IncomeException`, no se toca nada.
3. Escribir el **asiento de entrega** con la plantilla del producto.
4. Pasar a `VIGENTE`, estampar auditoría y guardar `PRSTIDPG`.

### Qué plantilla según el producto

| Producto | Alterno | Estado |
|---|---|---|
| Prendario | 9 | existe |
| Hipotecario | 13 | existe |
| Quirografario | **34** | `sql/156`, **sin ejecutar** |
| Cualquier otro | — | **rechazar con mensaje**, no elegir una por defecto |

⛔ **Los auxiliares de las tres son POSICIONALES** (medido: `sql/153` bloque 2). El código debe
resolver la línea por su posición dentro de la plantilla, **nunca asumir que el auxiliar `n`
significa lo mismo en dos plantillas distintas.** Un auxiliar mal mapeado deja el asiento mal
clasificado **y cuadrado igual**, o sea sin ninguna señal de error.

---

## 6. Fases

| # | Qué | Quién | Bloquea a |
|---|---|---|---|
| **0** | `sql/157` — consultar el producto de pago de `2.3.90.90.10` y confirmar que `PRSTIDPG` no existe | usuario | todo |
| **1** | DDL: `ALTER TABLE CRD.PRST ADD (PRSTIDPG NUMBER)` y ejecutar `sql/156` (plantilla 34) | usuario | el WAR |
| **2** | BE: mapear `PRSTIDPG`, sumar el valor al rubro, y los tres efectos nuevos de `aprobar` | agente BE | — |
| **3** | FE: los dos campos nuevos en el cuerpo de `aprobar`, y mostrar el número de orden al aprobar | agente FE | — |
| **4** | Probar el circuito completo: aprobar → ver la orden en la bandeja de tesorería → pagar → ver los dos asientos | usuario | — |

---

## 7. Lo que NO entra

- **No se toca `cxp` ni `tsr`.** Solo se consume `PagoProgramadoService`.
- **No se construye el asiento de bancos.** Lo arma CXP al confirmar el pago, y así lo pidió el usuario.
- **No hay desembolso parcial.** Monto completo, decisión del usuario.
- **No se toca el camino de carga por Excel** ni la cartera migrada.
- **No se reabre el contrato de `aprobar`** más allá de los dos campos nuevos.

---

## 8. Avisos

| A quién | Qué |
|---|---|
| **`omen-saa-2`** | Se va a agregar `CRD_DESEMBOLSO_PRESTAMO` a `OrigenPagoExterno`, archivo que ellos tocaron hoy. Aditivo, constantes de texto, sin colisión posible — pero es su archivo también |
| **Quien opere tesorería** | Van a empezar a llegar órdenes con origen `CRD_DESEMBOLSO_PRESTAMO` a la bandeja. Nacen `POR_APROBAR` y sin cuenta de origen: tesorería asigna cuenta y forma de pago al aprobar, igual que las de devolución de aportes |

---

## 9. Pendiente abierto

**Si la orden de pago se anula o se rechaza en tesorería, el préstamo queda `VIGENTE` con un asiento
de entrega escrito y sin dinero entregado.** La devolución de aportes resolvió el caso equivalente
con contra-movimientos (§8.3 de su plan) y `pagoProgramadoService.anularPago(...)`.

**No entra en esta entrega** y se anota para no perderlo: hay que decidir si un préstamo cuya orden
se anuló vuelve a `GENERADO`, se marca de otra forma, o se deja como está con un aviso. **Es una
pregunta de negocio.**
