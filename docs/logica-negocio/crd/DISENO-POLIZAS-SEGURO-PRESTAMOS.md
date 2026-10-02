# DISEÑO — Pólizas de seguro de préstamos (desgravamen e incendio): factura, prorrateo a cuotas y pago

**Equipo:** `omen-saa-1` (CRD · equipo B) · **Abierto:** 2026-10-02 · **Estado:** ⛔ PROCESO RELEVADO, NO DISEÑADO.
Faltan las respuestas del §4. Reemplaza a la pantalla maqueta `crd/forms/asignacion-seguros`, que se
retiró del menú el 2026-09-21 por simular la asignación (lote U4).

---

## 1. El proceso, como lo explicó el usuario (2026-10-02)

### 1.1 Desgravamen
1. Se saca un **listado de todos los préstamos VIGENTES y EN MORA**. Lo importante es el **saldo de
   capital** de cada préstamo. Hoy ese listado sale de la pantalla **de saldo insoluto**.
2. Ese listado **se envía a la aseguradora**.
3. La aseguradora **aplica su tasa** y envía **una factura por el valor ANUAL** del seguro de todos los
   préstamos del listado.
4. La factura **se registra en crédito** con:
   - el **valor total de la factura, impuestos incluidos**;
   - la **fecha de inicio y la fecha de fin** de la póliza;
   - los **préstamos incluidos**;
   - la **tasa aplicada**.
5. Ese valor total **se distribuye entre todos los préstamos, de forma proporcional, y a sus cuotas**:
   se **prorratea en las cuotas que caen dentro del año de vigencia**.
6. La factura **pasa por el pago normal de facturas**, CXP y luego TSR, **pero se paga DESPUÉS de que
   sus valores se hayan distribuido en las cuotas**.

### 1.2 Incendio
Es el mismo proceso, con tres diferencias:
- Se registra la **SUMA ASEGURADA DEL BIEN HIPOTECADO** de cada préstamo. **No es el valor del
  préstamo.**
  - Se ingresa **en crédito**, préstamo por préstamo, o se **carga desde un Excel** con sólo dos
    columnas: `IDAsoprep` y `suma asegurada`.
- El listado va al **broker**, que emite la factura. De ahí sigue igual que desgravamen.
- La factura de incendio **lleva más impuestos** que la de desgravamen. **Igual se prorratea el valor
  TOTAL** a las cuotas dentro de la vigencia de todos los préstamos incluidos.

---

## 2. Lo que ya existe y sirve (verificado contra el código, 2026-10-02)

- **Las cuotas ya tienen dónde va el seguro:** `DTPRDSGR` (desgravamen) y `DTPRVLSI` (incendio). Se
  cobran en pagos, cruces y Petro con la prelación incendio → desgravamen → mora → … El valor que se
  prorratee ahí **se cobra solo**, sin tocar el motor.
- **`CRD.PRST.PRSTVLAS` (`valorAsegurado`)** existe, está mapeada y nadie la escribe. Es el lugar
  natural de la **suma asegurada del bien**. ⚠️ La contabilidad ya la lee para las cuentas de orden de
  la garantía (`ContabilidadPrestamoServiceImpl:929`): cargarla tiene ese efecto, y hay que confirmar
  que es el deseado.
- **Listado de saldo insoluto:** la pantalla «Repote Valores Insolutos» toma `saldoInicialCapital` de
  la cuota del mes. Sirve de referencia, pero el listado que se manda a la aseguradora **tiene que
  quedar GUARDADO con la póliza**: es la base del prorrateo y lo que se le reclama después.
- **Pagar a un proveedor desde crédito por CXP ya tiene precedente:** el seguro médico de jubilados
  genera su orden al proveedor (`generarOrdenPagoProveedorSeguro`), y la devolución de aportes y el
  sepelio usan productos de pago de `PGS`.
- **Contabilidad de seguros hoy** (`ESTADO-EQUIPO-SEGUROS.md` §1.9): la compra de la póliza entra por
  CXP al activo de seguros pagados por anticipado (plantilla alterno 18, cuentas `1.4.90.90.10`
  desgravamen y `1.4.90.15.02/.03/.06`), y **el cobro al partícipe descarga ese activo** sólo vía Petro.
- **El desgravamen de los préstamos NUEVOS** se calcula hoy con una constante, `1.12/1000` sobre el
  saldo (`CalculadoraAmortizacionServiceImpl:59`), al crear el préstamo. El proceso nuevo lo
  reemplaza en las cuotas que cubre una póliza.
- **Plazo vencido (D13 del diseño de plazo vencido):** un préstamo en 8 **ya no lleva seguro**. Queda
  FUERA del listado.

---

## 3. Lo que hay que construir (borrador, sujeto al §4)

1. **Póliza / factura** (tabla nueva): tipo de seguro, aseguradora o broker, número de póliza, número
   de factura, vigencia (inicio y fin), tasa, valor total con impuestos, estado (REGISTRADA →
   DISTRIBUIDA → ENVIADA A PAGO → PAGADA / ANULADA) y el enlace a CXP.
2. **Préstamos de la póliza** (tabla nueva): por préstamo, la **base** con que se envió (saldo de
   capital, o suma asegurada para incendio), la **parte del valor total** que le tocó y el número de
   cuotas en las que se repartió.
3. **Detalle por cuota** (tabla nueva o columnas): qué valor de póliza se puso en cada cuota, y el que
   tenía antes, para poder **reversar** una distribución mal hecha o una póliza anulada.
4. **Suma asegurada del bien:** captura manual y **carga por Excel** (`IDAsoprep`, `suma asegurada`)
   sobre `PRSTVLAS`.
5. **Pantalla nueva** que reemplaza la maqueta:
   - generar y exportar el listado;
   - registrar la factura con su vigencia y tasa;
   - ver la distribución antes de confirmar;
   - confirmar;
   - enviar a pago.
6. **Envío a CXP** de la factura, **sólo** cuando la distribución quedó confirmada.

---

## 3bis. Decisiones del usuario — 2026-10-02. NO re-litigar

| # | Decisión |
|---|---|
| S1 | El reparto entre préstamos es **proporcional a la base enviada**: el **saldo de capital** en desgravamen y la **suma asegurada** en incendio. |
| S2 | Dentro del préstamo, el reparto entre sus cuotas de la vigencia es **proporcional**, no en partes iguales. ⇒ Lectura del árbitro, por confirmar: proporcional al **saldo de capital de cada cuota**, que baja mes a mes, como hoy el desgravamen. |
| S3 | Un préstamo que termina antes que la póliza carga **sólo la proporción de los meses que la póliza le cubre**. |
| S4 | El valor de la póliza **REEMPLAZA** el desgravamen / incendio que esas cuotas tenían. |
| S5 | **Préstamos nuevos durante la vigencia entran a la póliza vigente**, pero la novedad se envía a la aseguradora, que emite una **nota de débito**. Ese valor es el que se reparte en el préstamo nuevo. **Abono a capital y precancelación también se notifican** a la aseguradora, que emite un documento (ver §4, S10). |
| S6 | Como S5. |
| S7 | **Integración con CXP:** la factura le llega a crédito por mail, pero contabilidad puede descargar el XML y cargarlo en CXP primero. Hace falta: (a) **marcar** la factura como «de seguros» para que **no se pague hasta que crédito la distribuya**; (b) si ya estaba en CXP y crédito la sube, el sistema **la reconoce, avisa y la enlaza**, no la duplica; (c) **al cargarla desde CXP**, la opción de **marcarla de seguros** para que entre al proceso; (d) puede llevar **retención**. ⇒ Árbitro: CXP ya tiene clave de unicidad por `claveAcceso` (`DocumentoCxp`), y esa es la llave del enlace. |
| S8 | El **prendario** sigue el mismo proceso que incendio. |
| S9 | El centavo que sobra del reparto va **al préstamo de mayor valor**. |

⚠️ **S7 toca CXP y TSR**, alcance de `omen-saa-2`: la marca en el documento, el bloqueo del pago y la
opción en su pantalla de carga. Lo coordina el árbitro **con autorización del usuario**.

## 4. ⛔ Lo que falta decidir

| # | Pregunta | Por qué importa |
|---|---|---|
| S1 | **Base del reparto entre préstamos:** ¿proporcional al **saldo de capital enviado** (desgravamen) y a la **suma asegurada** (incendio)? | Define cuánto le toca a cada préstamo |
| S2 | **Reparto dentro del préstamo:** ¿en **partes iguales** entre sus cuotas dentro de la vigencia, o proporcional al saldo de cada cuota (que baja mes a mes)? | El desgravamen hoy baja con el saldo; un valor igual por cuota cambia eso |
| S3 | **Préstamo que termina antes que la póliza** (por ejemplo, le quedan 4 cuotas y la póliza es de 12 meses): ¿su parte anual se carga entera en esas 4, o sólo la proporción de los meses que cubre? | Cambia lo que paga ese partícipe |
| S4 | **¿El valor de la póliza REEMPLAZA el desgravamen/incendio que hoy tienen esas cuotas?** | Si no, se cobra dos veces |
| S5 | **Préstamos nuevos durante la vigencia:** ¿entran a la póliza vigente (inclusión), esperan a la renovación, o siguen con el 1,12 por mil hasta entonces? | El generador de préstamos nuevos tiene que saber qué hacer |
| S6 | **Préstamo que sale antes** (precancela o abona y acorta): ¿qué pasa con el seguro de las cuotas que ya no existen? ¿Se le reclama a la aseguradora? | Hoy se calcula y se pierde (`ESTADO-EQUIPO-SEGUROS.md` §1.8) |
| S7 | **CXP:** ¿crédito **crea** el documento por pagar (proveedor = aseguradora o broker, valor total, y las retenciones las pone CXP), o CXP registra la factura y crédito la **enlaza**? | Define la integración con otro equipo (`omen-saa-2`) |
| S8 | **¿Seguro prendario?** Las cuentas existen (`1.4.90.15.03`). ¿Mismo proceso que incendio, con la suma asegurada del bien prendado? | Si no, la pantalla tiene dos tipos y no tres |
| S9 | ~~Redondeo~~ → contestada, §3bis | — |
| **S10** | **Abono a capital / precancelación:** dijiste que la aseguradora emite una **nota de débito**. Lo normal sería una **nota de CRÉDITO** (devuelve la prima de lo que ya no se asegura). ¿Es de crédito? ¿El sistema genera el listado de novedades (inclusiones y exclusiones) para mandarle a la aseguradora? | Define si es un cobro o una devolución, y qué reporte hay que hacer |
| **S11** | **S3, el remanente:** si a un préstamo que termina en 4 meses le toca sólo 4/12 de su parte, los 8/12 restantes de la factura **¿se reparten entre los demás préstamos, o quedan como diferencia** a reclamar a la aseguradora (nota de crédito)? | Si nadie lo absorbe, lo distribuido no suma el total de la factura |
| **S12** | **S2:** ¿«proporcional» es al **saldo de capital de cada cuota**? | El reparto dentro del préstamo |
