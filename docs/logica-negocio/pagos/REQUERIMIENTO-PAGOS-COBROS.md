# Requerimiento de Negocio — Pagos (CXP) y Cobros (CXC) de Facturas

> **Qué es este documento:** la explicación de negocio del proceso de pagos y cobros,
> tal como la definió el usuario en la sesión de análisis del 2026-08-07. Sin detalle
> de implementación. El plan técnico correspondiente está en
> `PLAN-TECNICO-PAGOS-COBROS.md` (misma carpeta).
> Los puntos aún no definidos están marcados como **PENDIENTE**.

---

## 1. El problema de fondo

En el sistema, los documentos generan su contabilidad en el momento correcto:

- Las **facturas de venta** (módulo CXC) se emiten al SRI desde el sistema y, cuando el
  SRI las autoriza, se genera su asiento contable.
- Las **facturas de compra** (módulo CXP) se cargan al sistema desde los archivos XML
  descargados de la página del SRI (cotejados contra un TXT), y al registrarse se genera
  su asiento contable.

El problema aparece con los documentos que **abonan o modifican** esas facturas
(retenciones, notas de crédito, notas de débito): cuando se emiten o se cargan, **su
asiento contable sí se genera** — es decir, contablemente ya se reflejó que el saldo con
ese proveedor o cliente cambió — **pero la factura no se entera**. A nivel del módulo, la
factura sigue mostrando su valor original, sin ningún registro de que ya se le abonó
parte del valor.

**Consecuencia:** los saldos de las cuentas contables y los saldos "según el módulo"
pueden diferir. El objetivo de este requerimiento es que cada abono quede registrado
en la vida de la factura, en el mismo momento en que se genera su contabilidad, de modo
que ambos saldos nunca difieran.

**Regla central acordada:** el registro del pago/abono y la generación del asiento
contable ocurren **juntos y de forma atómica** — si el abono no se puede registrar
correctamente, el asiento no se genera (y viceversa). Además, si el documento que abona
referencia una factura que **no existe en el sistema**, el proceso completo se **bloquea**
(en emisión: antes de firmar y enviar al SRI; en carga: antes de registrar nada).

**Regla de arquitectura:** toda la lógica vive en los servicios del backend. La base de
datos solo almacena: no se usan triggers ni procedimientos que calculen saldos o estados.
El estado de pago de cada factura y el saldo de anticipos los calcula y graba el backend
cada vez que se crea o se reversa un abono.

---

## 2. Los dos flujos y en qué se diferencian

| | **CXP — Pagos** | **CXC — Cobros** |
|---|---|---|
| Documento base | Factura de compra: nos la emite el proveedor, la **cargamos** de los XML del SRI | Factura de venta: la **emitimos** nosotros al SRI |
| Qué representa | Lo que **debemos pagar** | Lo que **nos deben pagar** |
| Retención que la abona | La **emitimos nosotros** (comprobante de retención electrónico) | Nos la **emite el cliente** y la cargamos junto con los demás documentos del SRI |
| NC/ND que la modifican | Las emite el **proveedor** y las cargamos | Las **emitimos nosotros** |
| Dinero | **Sale** de nuestras cuentas bancarias (transferencias enviadas) | **Entra** a nuestras cuentas bancarias (transferencias recibidas) |

El concepto de abono es el mismo en ambos lados; cambian el sentido del dinero y quién
emite cada documento.

---

## 3. Formas de abonar una factura

### 3.1 Retenciones

A una factura **siempre** se le emite una retención que reduce su valor.

- **Pago de factura de compra (CXP):** cuando el proveedor nos emite una factura y la
  cargamos, nosotros le emitimos una retención (desde CXC, que es donde vive la emisión
  de comprobantes). Esa retención referencia a la factura en los campos que exige el SRI
  (número de documento sustento) — **no** mediante un vínculo interno del sistema, sino
  por el número del documento. Al autorizarse la retención se genera su asiento: la
  contabilidad ya muestra que le debemos menos al proveedor. Ese mismo valor debe quedar
  registrado como **abono a la factura de compra**.
- **Cobro de factura de venta (CXC):** el cliente nos emite una retención sobre nuestra
  factura; esa retención llega en la carga de documentos del SRI (proceso de carga de
  CXP, porque es un documento recibido). Trae el número de nuestra factura de venta. Al
  registrarse genera su asiento y debe registrar el **abono a la factura de venta**.

### 3.2 Notas de crédito y de débito

- Una **nota de crédito** que afecta a una factura **reduce** su valor: equivale a un
  abono. (En CXP: el proveedor nos la emite y la cargamos. En CXC: la emitimos nosotros.)
- Una **nota de débito** hace lo contrario: **aumenta** el valor de la factura afectada.
- Hoy, en ambos casos, la contabilidad ya se afecta al generarse el asiento de la NC/ND,
  pero la factura no registra el movimiento — exactamente el problema descrito en §1.
- Con este requerimiento, en el momento de generar el asiento de la NC/ND se registra
  también el movimiento sobre la factura (abono si NC, incremento si ND).

### 3.3 Cruce con anticipos

Los titulares (en su rol de cliente o proveedor) pueden tener **saldo de anticipos**:
dinero entregado o recibido por adelantado, aún no cruzado con ninguna factura.

- El saldo se lleva en el campo `saldoInicial` de la tabla PersonaCuentaContable (que
  vincula al titular con su cuenta contable por tipo). Ese campo nació para arrastrar el
  **saldo inicial migrado del sistema anterior**, y desde ahí llevar el control de
  anticipos en este sistema.
- **Decisión tomada:** ese campo pasa a entenderse como **saldo actual** de anticipos.
  El saldo migrado se documenta creando un registro en las tablas de anticipos con la
  marca "saldo inicial migrado", **sin generar contabilidad** (ese saldo ya estaba
  contabilizado en el sistema anterior).
- Las tablas de anticipos registran cada **entrada** (cada anticipo entregado/recibido);
  las **salidas** son los cruces contra facturas.
- **El cruce es por valor, no por anticipo individual:** si el titular tiene 3 anticipos
  de $100, el usuario puede cruzar $225 contra una factura; el sistema valida que el
  saldo alcance, registra la salida de $225 y el saldo queda en $75. No interesa "de qué
  anticipos" salió el valor — el historial de entradas y salidas explica cómo se llegó al
  saldo.
- El cruce genera su propio asiento contable (cuentas y tipo de asiento: **PENDIENTE** —
  el usuario indicará qué cuentas y qué tipo de asiento usar al implementar).

### 3.4 Transferencias

**CXP — pagos salientes (el flujo más largo):**
1. En una pantalla se registra el pago de una factura: se selecciona la factura y la
   forma de pago; si es transferencia, se escoge la **cuenta bancaria propia** desde la
   que se pagará (tabla de cuentas bancarias de tesorería). La cuenta **destino** del
   proveedor ya está registrada a nivel de titular (CuentaBancariaTitular).
2. Los pagos registrados aparecen en un **listado**, donde se selecciona cuáles sí se van
   a ejecutar y cuáles no.
3. Para los seleccionados se genera un **archivo TXT** con formato específico de la
   entidad financiera (formato: **PENDIENTE** — el usuario lo entregará), que se carga al
   banco.
4. El banco responde confirmando cuáles transferencias **sí** se ejecutaron y cuáles
   **no**. Para las confirmadas, **en ese momento** se genera la contabilidad y se
   registra el abono a la factura. Las no ejecutadas quedan **en seguimiento** (pueden
   reintentarse o anularse).
- Una factura puede pagarse con **muchos pagos parciales** o uno solo.

**Nota (2026-08-11):** los anticipos (a proveedores o a clientes) **no** pasan por este
proceso de aprobación/archivo/respuesta del banco. Siguen su propio flujo, ya existente:
se registran y contabilizan en el mismo momento. Este circuito de lote/TXT/confirmación
es exclusivo de los pagos y cobros **de facturas** por transferencia.

**CXC — cobros entrantes (más simple):**
- Una pantalla permite registrar el pago de una factura por parte de un cliente: valor
  recibido, fecha y **número de transferencia**, y la cuenta bancaria nuestra donde se
  recibió. Se valida que la transferencia se recibió, se abona el saldo de la factura y
  se genera la contabilidad en ese momento. También admite pagos parciales múltiples.

### 3.5 Qué ofrece cada pantalla

**Decisión tomada:** las aplicaciones por **retención, nota de crédito y nota de débito
son automáticas** — ocurren cuando se emite/carga el documento y se genera su asiento.
Las pantallas de pago/cobro de tesorería solo ofrecen dos opciones: **transferencia** y
**cruce de anticipo**.

---

## 4. Reversión y anulación

- Todo pago/abono debe poder **anularse o revertirse**, pidiendo un **motivo**.
- La reversión anula los asientos contables relacionados y **restaura los saldos**
  afectados: el saldo de la factura, el saldo de anticipos si fue un cruce, y el
  movimiento bancario si fue una transferencia.
- Si se anula un **documento** (una factura, una retención, una NC/ND), sus abonos
  asociados se revierten también, de manera que el estado de pago de la factura se
  recalcule correctamente.
- Para pagos por transferencia aún no confirmados por el banco, la anulación simplemente
  los saca del flujo (no hay contabilidad que revertir, porque la contabilidad solo se
  genera al confirmarse).

---

## 5. Pendientes por definir

| # | Pendiente | Necesario para |
|---|---|---|
| 1 | **Formato del archivo TXT** que se carga a la entidad financiera, y formato del archivo/mecanismo de **respuesta** del banco | Pagos CXP por transferencia |
| ~~2~~ | ~~Tipos de asiento para los movimientos de tesorería~~ — **DEFINIDO**: salidas de dinero a proveedores (pago por transferencia y cruce de anticipo) usan el tipo de asiento de **egresos**; entradas de dinero de clientes (cobro por transferencia y cruce de anticipo) usan el de **ingresos** | — |
| 3 | **Código de institución financiera** de los bancos externos (los bancos de los titulares hoy no tienen código IFI, que el archivo bancario seguramente exige) | Archivo TXT bancario |
| 4 | **Rubros de MovimientoBanco** para clasificar los movimientos de "pago a proveedores" y "cobro de clientes" | Movimientos bancarios de tesorería |
