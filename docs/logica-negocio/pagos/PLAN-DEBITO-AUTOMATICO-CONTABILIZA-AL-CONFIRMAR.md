# El débito automático pasa a contabilizarse al confirmar, no al aprobar

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-07 · **Módulos:** `cxp` (backend) · `tsr` (pantalla)

**Pedido del usuario, textual:**

> *«Al escoger la opción de débito automático se había pedido antes que en la pantalla de aprobación
> de pagos se genere el asiento, pero ahora hay que hacer un cambio ya que para esos pagos también
> se debe registrar un número de referencia. Para conseguir esto, debemos hacer que estos pagos
> también aparezcan en la pantalla de recepción y confirmación y solo ahí se ingrese la fecha y el
> número de referencia, y en ese momento se contabilice con esa fecha, y que el número de referencia
> también se incluya en la observación del asiento.»*

---

## 1. Cómo funciona hoy — verificado

`PagoProgramadoServiceImpl.aprobar`, líneas ~1318-1325:

```java
} else if (fp == FormaPagoProgramado.DEBITO_AUTOMATICO) {
    pago.setDebitoAutomatico(1);
    pago.setFechaRespuesta(fecha);                          // ← la fecha de APROBACIÓN
    pago.setEstado(EstadoPagoProgramado.CONFIRMADO);        // ← salta a confirmado
    ...
    contabilizarSegunOrigen(pago, idUsuario);               // ← contabiliza YA
```

El débito automático **nace confirmado y contabilizado en el momento de aprobar**, con la fecha de
aprobación y sin referencia bancaria. Es lo que se pidió en su momento; lo que cambia ahora es que
esos pagos también necesitan su número de referencia.

Y la bandeja de confirmación **los excluye a propósito** hoy
(`confirmacion.component.ts`: `!this.esDebitoAutomatico(p)`), justamente porque ya venían cerrados.

---

## 2. Lo que pasa a ser

| Momento | Antes | Ahora |
|---|---|---|
| **Aprobar** | `CONFIRMADO` + asiento con la fecha de aprobación | **`REGISTRADO`**, sin asiento |
| **Recepción y confirmación** | No aparece | **Aparece**, se le teclea fecha y referencia |
| **Confirmar** | — | `CONFIRMADO` + **asiento con la fecha tecleada**, y la referencia en la observación |

**La marca `debitoAutomatico = 1` se conserva**: sigue distinguiendo el pago, y es lo que la pantalla
usa para mostrarlo como tal. Lo que cambia es *cuándo* se contabiliza, no *qué* es.

### ⛔ Por qué esto NO es sólo mover una línea

El pago pasa a existir en un estado en el que antes nunca estaba: **débito automático `REGISTRADO`,
aprobado y sin contabilizar**. Todo lo que hoy asume que un débito automático confirmado ya tiene
asiento hay que revisarlo:

- `anularPago` rechaza los `CONFIRMADO` con un mensaje específico para débito automático
  (*«es un débito automático ya ejecutado por el banco y tiene contabilidad generada»*). Con el
  cambio, un débito `REGISTRADO` **sí se puede anular** —y debe poder—, porque todavía no
  contabilizó. **Verificar que la guarda siga siendo correcta y que el mensaje no mienta.**
- La generación de archivo al banco (`POST /pgtr/lote`) **no debe incluirlos**: un débito automático
  no se transfiere, lo debita el banco por convenio. Si hoy quedaban fuera por estar `CONFIRMADO`,
  al pasar a `REGISTRADO` **podrían colarse en un lote**. Es el riesgo más caro de este cambio.

---

## 3. La referencia en la observación del asiento

`confirmarPagosManual` ya escribe `pago.setReferenciaBanco(refPago)` antes de contabilizar. Falta
que ese número llegue a la **observación del asiento**, que hoy se arma en
`contabilizarSegunOrigen` y sus tres variantes (`observacionAsiento`, líneas ~2414, ~2600, ~2686).

**Regla:** si el pago tiene `referenciaBanco`, se agrega al final de la observación en un formato
único y reconocible, del tipo `… | Ref. banco: 1009212`. **Un solo lugar de construcción**, no
repetido en las tres variantes — si mañana se agrega una cuarta, tiene que heredarlo sola.

⚠️ El asiento **se contabiliza con la fecha tecleada en la confirmación**, no con la de aprobación
ni con la del sistema. Es la mitad del pedido y es la que se nota en el balance del mes.

---

## 4. Orden y riesgos

**Sin DDL.** **El WAR va antes que el FE.**

| # | Riesgo | Mitigación |
|---|---|---|
| 1 | 🔴 **Un débito automático `REGISTRADO` se cuela en un lote de transferencias** | Excluirlos explícitamente al armar el lote, por la marca `debitoAutomatico`, no por el estado |
| 2 | 🔴 **Los débitos ya aprobados antes de este cambio** quedaron `CONFIRMADO` y contabilizados. **No hay que tocarlos ni re-contabilizarlos**: el cambio rige hacia adelante |
| 3 | 🟠 El mensaje de `anularPago` sobre débito automático puede quedar mintiendo | Revisarlo junto con el cambio |
| 4 | 🟠 La bandeja de confirmación los excluye por código | Sacar ese filtro, y que la pantalla los muestre distinguidos |
