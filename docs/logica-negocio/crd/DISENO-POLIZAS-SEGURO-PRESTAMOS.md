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

| S10 | **Abono a capital y precancelación:** la aseguradora emite una **NOTA DE CRÉDITO** (devuelve la prima de lo que ya no se asegura). Inclusiones (préstamos nuevos): **nota de DÉBITO**. |
| S11 | Lo que no absorbe un préstamo que termina antes que la póliza **se reparte entre los DEMÁS préstamos** de la póliza. Lo distribuido suma siempre el total de la factura. |
| S12 | Dentro del préstamo: proporcional al **saldo de capital de cada cuota**. |

⚠️ **S7 toca CXP y TSR**, alcance de `omen-saa-2`: la marca en el documento, el bloqueo del pago y la
opción en su pantalla de carga. **Mensaje enviado a `omen-saa-2-arb` el 2026-10-02 con autorización del usuario.** Se espera su respuesta.

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

---

## 5. DISEÑO TÉCNICO (2026-10-02)

### 5.1 Ciclo de un documento de seguro

```
1 LISTADO_ENVIADO ─▶ 2 DOCUMENTO_REGISTRADO ─▶ 3 DISTRIBUIDO ─▶ 4 LIBERADO_A_PAGO
        │                     │                       │
        └──────────── 5 ANULADO ◀─────────────────────┘   (si estaba distribuido, se revierte en las cuotas)
```

1. **Generar el listado** (crédito). Por tipo de seguro, el sistema toma los préstamos:
   - **desgravamen:** `PRSTIDST IN (2, 11)`, de cualquier producto. El 8 queda fuera (D13 de plazo
     vencido);
   - **incendio:** lo mismo, tipo de préstamo **2 HIPOTECARIO**;
   - **prendario:** lo mismo, tipo **3 PRENDARIO**. Son los mismos códigos de `CobroPetroContableServiceImpl:98-99`.

   La **base** es el saldo de capital en desgravamen, y `PRSTVLAS` (suma asegurada) en incendio y
   prendario. ⛔ Un préstamo de incendio o prendario **sin suma asegurada** se lista marcado
   «SIN SUMA ASEGURADA» y no puede pasar al paso 2 hasta cargarla. El listado **se guarda** (la foto de
   lo enviado) y se exporta a Excel para la aseguradora o el broker.
2. **Registrar el documento** que manda la aseguradora: factura (anual), nota de débito (inclusión) o
   nota de crédito (exclusión). Se registra:
   - número de póliza, número de documento y **clave de acceso del SRI**;
   - aseguradora o broker con su RUC;
   - fecha de emisión;
   - **vigencia (inicio y fin)** y **tasa aplicada**;
   - **valor total con impuestos**.
3. **Distribuir** (vista previa y luego confirmar). Ver §5.3. Escribe en las cuotas y guarda el valor
   anterior de cada una.
4. **Liberar a pago:** se enlaza el documento de CXP por `claveAcceso` y se le quita el bloqueo. El
   pago sigue el circuito normal de CXP y Tesorería. **Endpoint de `omen-saa-2`, contrato pendiente.**
5. **Anular:** si estaba distribuido, **reversa** lo escrito en las cuotas. Si estaba liberado, primero
   vuelve a bloquear en CXP.

### 5.2 Tablas nuevas (`CRD`, nombres libres en el modelo y el registro, verificado el 2026-10-02)

| Tabla | Una fila por | Qué guarda |
|---|---|---|
| **`POSG`** — documento de seguro | factura / ND / NC | tipo de seguro (1 desgravamen, 2 incendio, 3 prendario); clase (1 factura, 2 nota de débito, 3 nota de crédito); `POSGPADR` (la factura madre, para ND/NC); aseguradora, RUC, número de póliza, número de documento, `claveAcceso` (única); emisión, vigencia; tasa; valor total; estado (§5.1); el id del documento de CXP; auditoría por paso |
| **`PSPR`** — préstamo en el documento | préstamo × documento | `PRSTCDGO`, **base enviada**, meses cubiertos, peso, **valor asignado**, cuotas en que se repartió, y la novedad (ORIGINAL / INCLUSIÓN / EXCLUSIÓN) |
| **`PSCT`** — cuota afectada | cuota × documento | `DTPRCDGO`, campo tocado (DSGR o VLSI), **valor anterior**, valor nuevo, saldo de capital usado como peso, fecha de reverso |

### 5.3 El cálculo de la distribución (S1, S2, S3, S9, S11, S12)

- Cuotas de un préstamo **dentro de la vigencia**: `fechaVencimiento ∈ [inicio, fin]`, no PAGADA (4)
  ni CANCELADA_ANTICIPADA (7). Una PARCIAL entra, pero su seguro no baja de lo ya pagado.
- `mesesCubiertos_i` = esas cuotas del préstamo. `mesesVigencia` = meses de la póliza.
- **Peso del préstamo:** `w_i = base_i × mesesCubiertos_i / mesesVigencia`. Un préstamo que termina
  antes pesa menos (S3), y lo que no absorbe se reparte solo entre los demás (S11), porque
  `valor_i = total × w_i / Σw`.
- **Dentro del préstamo:** cada cuota recibe `valor_i × saldoInicialCapital_cuota / Σ saldoInicialCapital`
  de sus cuotas de la vigencia (S12, `DTPRSICP`).
- **Redondeo:** a 2 decimales. El sobrante de la póliza va al **préstamo de mayor valor** (S9), y dentro
  de cada préstamo, a su **cuota de mayor valor**. Invariante dura: **Σ cuotas = Σ préstamos = total del
  documento**, al centavo, o no se confirma.
- **Escritura (S4, reemplaza):** desgravamen → `DTPRDSGR` (y `DTPRDSOR`/`DTPRDSFR` como hoy);
  incendio/prendario → `DTPRVLSI`. `total` y `totalConSeguro` se ajustan **por diferencia**, sin pisar
  la mora (lección del reverso de plazo vencido). Valor anterior en `PSCT`.

### 5.4 Novedades durante la vigencia (S5, S10)

- **Reporte de novedades** por rango de fechas, contra la póliza vigente de cada tipo:
  - **INCLUSIONES:** préstamos que hoy cumplen la regla y no están en ningún documento vivo de la póliza
    (los nuevos);
  - **EXCLUSIONES:** préstamos del documento que se precancelaron, abonaron y acortaron, o se declararon
    en plazo vencido.

  Se exporta para la aseguradora.
- **Nota de DÉBITO (inclusión):** se registra contra la factura madre con sus préstamos. Se distribuye
  igual (§5.3) en los préstamos incluidos, cuotas dentro de la vigencia **restante**.
- **Nota de CRÉDITO (exclusión):** se registra con sus préstamos y el valor. Reduce el seguro de las
  cuotas **que todavía existan** de esos préstamos (abono que acorta), en proporción a su saldo. En una
  precancelación las cuotas ya no existen: la nota **sólo se registra** y va a CXP.
- ⚠️ **Abono a capital dentro de una vigencia:** hoy `AbonoCapitalPrestamoServiceImpl` **recalcula** el
  desgravamen de las cuotas nuevas con la constante `1.12/1000` (`:638`) y preserva el incendio por
  número de cuota (`:479-488`). Eso **pisaría** lo distribuido por la póliza. **Propuesta del árbitro:**
  si el préstamo está en un documento vivo, el seguro pendiente de sus cuotas dentro de la vigencia se
  **re-reparte** entre las cuotas nuevas en proporción a su saldo, en vez de la constante, y la nota de
  crédito que llegue después lo reduce. Confirmar con el usuario (S13).

### 5.5 Suma asegurada del bien

- Captura manual por préstamo y **carga por Excel** (`IDAsoprep`, `suma asegurada`) sobre
  `PRST.PRSTVLAS`. Antes de grabar, la carga muestra el resultado: los que actualiza, los
  `IDAsoprep` que no existen, los que no son hipotecario/prendario, y los valores ≤ 0.
- ⚠️ `ContabilidadPrestamoServiceImpl:929` ya lee `PRSTVLAS` para las cuentas de orden de la garantía
  **al entregar el préstamo**. Cargarlo en préstamos ya entregados **no** genera ese asiento retroactivo.
  Confirmar con el usuario si debe (S14).

### 5.6 Contabilidad

**Crédito no genera asientos en este proceso.** La factura, la ND y la NC se contabilizan en CXP: el
activo de seguros pagados por anticipado (plantilla 18) y la retención. El cobro al partícipe ya
descarga ese activo. Con esto queda cobrado por la vía Petro; por las demás vías depende de los asientos
de cobro que ya existen.

### 5.6bis S13 — DECIDIDO por el usuario (2026-10-02): el abono re-reparte el seguro de la póliza

Si el préstamo tiene cuotas cubiertas por un documento vivo (`PSCT` sin `fechaReverso`, documento en
estado 3 o 4) del tipo desgravamen y/o incendio-prendario:
- antes de borrar las cuotas pendientes, `AbonoCapitalPrestamoServiceImpl` suma el seguro de póliza de
  las cuotas que va a borrar **dentro de la vigencia**;
- al crear las cuotas nuevas, ese monto se **re-reparte** entre las nuevas que caen dentro de la vigencia,
  en proporción a su `DTPRSICP`, con el sobrante a la de mayor valor. **No** se usa la constante 1,12/1000
  ni el incendio por número de cuota en esas cuotas;
- los `PSCT` de las cuotas borradas quedan con `fechaReverso` (marca de «reemplazada»), y se crean `PSCT`
  nuevos para las cuotas nuevas: `anterior` = lo que el abono les habría puesto, `nuevo` = lo
  re-repartido;
- después, la **nota de crédito** de la aseguradora por ese abono baja el seguro (§7 del contrato).

### ⛔ H82 — Las tablas de historia por cuota NO pueden tener FK a `CRD.DTPR`

El abono y el reverso de operaciones **borran** cuotas de `DTPR` y las vuelven a crear. Con la FK:
- un préstamo con declaración de plazo vencido (`DPLV`, ya en producción) **no se puede abonar ni
  reversar**: ORA-02292. Se corrige con el `sql/307`;
- `PSCT` tenía el mismo diseño y se corrigió en el `306` **antes de correrlo**.

### 5.7 Preguntas nuevas

| # | Pregunta |
|---|---|
| ~~S13~~ | **DECIDIDO: sí** (§5.6bis) |
| ~~S14~~ | **DECIDIDO (2026-10-02): NO.** Para los préstamos viejos esa contabilidad de garantía en cuentas de orden **ya se generó en su momento**. Cargar la suma asegurada sólo guarda el dato para la póliza; no genera ningún asiento. |

## 6. S15 — 2026-10-05: los préstamos con el PLAZO TERMINADO no entran al listado

**Medido con el `sql/312`**, cruzando el listado del sistema contra la factura de desgravamen de la aseguradora:
- Los 12 préstamos que el sistema listó y la aseguradora no facturó tenían **todas** sus cuotas pendientes ya
  vencidas a la fecha de corte: el plazo ya había terminado. 7 de ellos con residuos de centavos (57598:
  0,15 de capital real; el «575,27» comparado era el saldo inicial de la cuota, que no descuenta pagos).
- Los 23 que la aseguradora facturó y el sistema no listó estaban **cancelados** (estados 3 y 4) el 29 y el
  30-09: la aseguradora facturó con una lista anterior. Eso es una NC de exclusión, no un defecto.

**Decisión del usuario:** en el listado (§3 del contrato, todos los tipos) sólo entran préstamos con **al menos una
cuota no PAGADA ni CANCELADA ANTICIPADA con vencimiento > fecha de corte**. Un préstamo con el plazo terminado
queda fuera aunque siga debiendo.
