# Pendientes — segunda ola (después del devengo de aportes)

**Fecha:** 2026-08-27
**Por qué están aquí:** son pedidos que **no comparten tabla, proceso ni pantalla** con
`PLAN-APORTES-DEVENGO-CONTRATOS.md`. Meterlos en esa ola sólo alargaría el ciclo sin ganar nada.
La clasificación completa de los diez pedidos está en §7 de ese plan.

**Nada de esto está implementado ni analizado a fondo.** Lo que sigue es la pista de arranque, no el
diseño.

---

## 1. Saldo de capital mal leído — pedidos 6 y 8 · **se arreglan juntos**

Dos síntomas, una raíz aparente:

- **Pedido 6:** en la pantalla de cobro de valores para cubrir con cuenta individual, el saldo de
  capital no cuadra con el real: no toma el de la mínima cuota no pagada ni cancelada anticipada.
- **Pedido 8:** la simulación de crédito para reestructura no toma el capital a reestructurar de esa
  misma cuota.

### Por dónde empezar

`CierreCarteraDaoServiceImpl` ya dejó documentado el hallazgo que probablemente explica los dos:
**`DTPRCPPG` no sirve para saber qué se pagó** — es resto de la migración; vale igual que `DTPRCPTL`
en 50.853 de 59.147 cuotas pendientes sin fecha de pago, y 0 en 10.593 pagadas, con `DTPRFCPG` nulo
en 11.163 de ellas. Por eso el motor de pagos, la generación Petro y el cierre de cartera **reconstruyen
el saldo desde `CRD.PGPR`**, no desde las columnas de la cuota.

Hipótesis a verificar antes de tocar nada: estas dos pantallas siguen leyendo `DTPRSLCP` /
`DTPRCPPG` de `DTPR` en vez de reconstruir desde `PGPR`, y además no filtran por la **mínima cuota con
`DTPRESTD NOT IN (4, 7)`**.

> Recordatorio de `CLAUDE.md`: en `CRD.DTPR` el estado vigente es **`DTPRESTD`**, no `DTPRIDST`.
> Filtrar por la columna equivocada devuelve resultados vacíos o silenciosamente incorrectos.

### ⚠ Orden obligatorio

**El pedido 6 hay que volver a medirlo DESPUÉS de la Fase 1 de la primera ola.** Su lado de "cuenta
individual" es el saldo de aportes, y ese saldo cambia de valor cuando se corrija
`valor = lo recibido`. Medir la diferencia antes es perseguir un síntoma que puede haber desaparecido
solo — o peor, "corregir" el lado de capital para compensar un error del lado de aportes.

---

## 2. Simuladores — pedidos 2 y 3

- **Pedido 2:** la simulación de crédito nuevo debe calcular el seguro de desgravamen de cada cuota
  como `capital × 1.12 / 1000`.
- **Pedido 3:** todos los reportes de simulaciones deben mostrar el signo `$` en los valores
  monetarios.

### Notas

El invariante del total de cuota ya está fijado y **no debe romperse**:
`CalculadoraAmortizacionServiceImpl:300` calcula `total = cuota + desgravamen + seguroIncendio`, con el
mismo significado que `DTPRTTLL`. Si la simulación empieza a calcular el desgravamen, tiene que
alimentar ese mismo invariante y no un cálculo paralelo, o la simulación y el préstamo real dejarán de
coincidir.

**Confirmado por el usuario el 2026-08-27:** en `capital × 1.12 / 1000`, `capital` es el **saldo de
capital de cada cuota**, no el capital amortizado en ella.

En una simulación de crédito **nuevo** no hay pagos, así que el saldo es el proyectado por la propia
calculadora (`CuotaProyectada.saldoCapital`) y no hay que reconstruir nada desde `CRD.PGPR` — eso sólo
aplica a los pedidos 6 y 8, sobre préstamos reales. Queda un detalle a fijar contra el código al
implementar: si `saldoCapital` de la fila es el saldo **antes** o **después** de amortizar esa cuota.
Elegir mal corre todo el cuadro un periodo.

Contexto vigente del proyecto: `PLAN-SIMULADORES-PRESTAMOS.md` (la auditoría del motor encontró 10
defectos y las reglas de negocio ya están decididas). Revisar si los pedidos 2 y 8 ya están cubiertos
ahí antes de abrir trabajo nuevo.

El pedido 3 es sólo frontend y no depende de nada.

---

## 3. Sigue esperando: limpieza de aportes duplicados

`sql/61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql` está escrito y **espera que el usuario corra A0, A2 y
A6**. No es de esta ola ni de la segunda: es un saneamiento de datos que arranca cuando haya
resultados. La primera ola sí **cierra la causa** (Fase 1.4).

Y aparte, sin analizar: si A0 confirma cargas reprocesadas, **los pagos de préstamo de esas cargas
también se aplicaron dos veces**. Mismo diagnóstico, sobre `CRD.PGPR`.
