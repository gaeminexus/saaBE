# DISEÑO — el número de referencia del banco: en el asiento del anticipo y en la conciliación

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-22 · **Módulos:** `pagos` (cxp) · `tsr` · `cnt`

**Pedido del usuario, textual:**

> *«Cuando se paga un anticipo a proveedor en el asiento contable de egreso no está incluyendo el
> número de referencia que tienen el resto de egresos (exactamente el texto de Ref). Y ese número
> también debe mostrarse en el grid derecho de conciliación bancaria.»*

---

## 0. El pedido son DOS defectos distintos, y sólo uno es del anticipo

Medido contra el código el 2026-09-22, antes de escribir una línea:

| | Qué | Alcance real |
|---|---|---|
| **A** | El asiento del pago de anticipo a proveedor no lleva la referencia | **Sólo el anticipo.** Los otros cuatro caminos sí la llevan |
| **B** | El grid derecho de conciliación no muestra la referencia | **TODOS los egresos.** No es del anticipo: hoy no se ve para ninguno |

**El B no se arregla arreglando el A.** Son causas independientes y se atienden por separado.

## 1. Defecto A — el anticipo es el único de los cinco que no aplica el helper

`PagoProgramadoServiceImpl.contabilizarSegunOrigen` (`:2707`) reparte en cinco caminos. Cuatro
componen la observación del asiento y la pasan por el helper `conReferenciaBanco(...)` (`:3756`),
que agrega `" | Ref. banco: " + referencia` cuando `PGTR.referenciaBanco` no está vacía:

| Camino | Método | ¿Aplica `conReferenciaBanco`? |
|---|---|:--:|
| Egreso de tesorería | `contabilizarPagoEgreso` | ✅ `:2851` |
| Origen externo | `contabilizarPagoOrigenExterno` | ✅ `:3120` |
| Caja chica | `contabilizarPagoCajaChica` | ✅ `:3205` |
| Anticipo a **empleado** | `contabilizarPagoAnticipoEmpleado` | ✅ `:3289` |
| **Anticipo a proveedor** | **`contabilizarPagoAnticipo`** (`:3672`) | ❌ **NO** |

**Por qué se escapó, y no es un olvido cualquiera:** los otros cuatro arman la observación *en*
`PagoProgramadoServiceImpl` y después llaman al generador de asientos. El del anticipo **no arma
ninguna observación**: delega el asiento entero en otro servicio —

```java
Asiento asiento = anticipoProveedorService.contabilizarAnticipoConfirmado(
        anticipo.getId(), idCuentaBancaria, fecha, idUsuario, notaCheque);   // :3688
```

— y el único texto que le pasa es `notaCheque`, que **vale `null` cuando el pago es por
transferencia** (`:3683`). O sea: con cheque el asiento recibe algo; por transferencia, nada.

> **Lo que hay que llevarse:** el helper existía, tenía cuatro llamadores y aun así el quinto camino
> quedó afuera — porque es el único que **no arma su propia observación**. Un helper protege al que
> lo llama; el que delega la construcción del texto a otro servicio queda fuera de su alcance sin
> que nada lo señale. Es la familia del §29: *contar los casos antes de arreglar el que se tiene en
> la mano* — acá la cuenta dio 5, y el código cubría 4.

### 1.1 El `MovimientoBanco` del anticipo SÍ la lleva

Ojo con esto, porque explica por qué el defecto no saltaba: en el mismo método, `:3699`, el
movimiento bancario se crea con `" | Ref: " + nvl(pago.getReferenciaBanco(), "")`. **La referencia
estaba a tres líneas de distancia del asiento que no la recibe.**

### 1.2 ⚠️ Dos textos distintos conviven hoy, y hay que elegir a propósito

| Dónde | Texto |
|---|---|
| Observación del **asiento** (los cuatro caminos, vía `conReferenciaBanco`) | `" | Ref. banco: X"` |
| Descripción del **MovimientoBanco** (los cinco caminos) | `" | Ref: X"` |
| Aplicación de pago de factura (`AplicacionPagoCxpServiceImpl:702,719,740`) | `" | Ref: X"` |

El pedido dice *«exactamente el texto de Ref»*. **Este diseño usa `conReferenciaBanco`**, o sea
`" | Ref. banco: "`, porque es el texto que tienen **los otros asientos de egreso**, que es contra
lo que el usuario está comparando. Unificar los dos formatos es un cambio aparte y más grande: toca
cinco caminos y los textos ya grabados. → **Decisión del usuario, no bloquea este frente.**

## 2. Defecto B — el grid derecho muestra la LÍNEA, y la referencia vive en la CABECERA

`conciliacion-contable.component.html:352-380`, panel **«Contabilidad (Detalle Asiento)»**, pinta
`fila.descripcion` — que es `DetalleAsiento.descripcion`, **la línea del asiento**.

Y la referencia **no está en la línea**: `AsientoContableServiceImpl.generarAsientoEgresoTesoreria`
(`:958-962`) arma las dos líneas con `"Egreso tesorería: " + concepto` (+ `" | Cta Banco: N"` en la
del banco) y pasa `observaciones` —el texto que trae el `" | Ref. banco: "`— a `generarAsiento(...)`,
que lo graba en la **cabecera** (`Asiento.observaciones`).

**Conclusión: hoy la referencia no se ve en ese grid para NINGÚN egreso**, no sólo para el anticipo.

### 2.1 ✅ Y se arregla en el frontend solo, sin tocar el backend

`saaFE/src/app/modules/tsr/model/detalle-asiento-conciliacion.ts` **ya declara
`asiento.observaciones`**, y el backend lo devuelve: `GrupoConciliacionAsientoDaoService.selectPendientes`
retorna `DetalleAsiento`, cuyo `@ManyToOne asiento` es EAGER y se serializa entero.

**El dato ya está en el navegador y no se pinta** — el mismo hallazgo que la ficha de documentos de
CXP (§49.2). Por eso:

- **No hay endpoint nuevo, ni cambio de contrato, ni DDL.**
- **Y arregla también el pasado:** todos los asientos de egreso ya contabilizados tienen su
  `" | Ref. banco: "` en la observación. En cuanto el grid la muestre, aparece la referencia de los
  históricos sin tocar un solo dato. *(Excepto los anticipos ya pagados — ver §4.)*

## 3. Qué se hace

### BE-1 — el asiento del anticipo a proveedor lleva la referencia

**Archivo:** `src/main/java/com/saa/ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java`,
`contabilizarPagoAnticipo` (`:3672-3711`).

La nota que hoy se pasa como `notaCheque` pasa a ser **una nota compuesta**: lo del cheque (si lo
hay) **más** la referencia del banco, usando el helper que ya existe.

- Se construye con `conReferenciaBanco(notaBase, pago)`, **sin duplicar la lógica**: si
  `PGTR.referenciaBanco` viene vacía o nula, el helper devuelve el texto sin tocar y el
  comportamiento es exactamente el de hoy.
- **No se cambia la firma** de `contabilizarAnticipoConfirmado`: sigue recibiendo un solo `String`.
  El parámetro deja de llamarse sólo «nota de cheque» **en el javadoc**, que hay que corregir.
- Con cheque **y** referencia, el texto lleva los dos.
- **No se toca** el `MovimientoBanco` de `:3699`, que ya está bien.

⛔ **Verificar antes de programar** (instrucción permanente): abrir
`AnticipoProveedorServiceImpl.contabilizarAnticipoConfirmado` y comprobar **qué hace con ese
parámetro** — si lo concatena a la observación de la cabecera, a la descripción de las líneas, o a
las dos. **El diseño asume que va a la observación de la cabecera, como en los otros cuatro
caminos.** Si resultara que va a la línea, o que se ignora cuando es nulo de una forma que rompa,
**reportarlo y detenerse**: cambia dónde queda el texto y hay que decidirlo, no improvisarlo.

### FE-1 — el grid derecho muestra la referencia

**Archivo:** `saaFE/src/app/modules/tsr/forms/generales/conciliacion-contable/conciliacion-contable.component.html`
(panel «Contabilidad (Detalle Asiento)», `:352-380`) y su `.ts`/`.scss`.

1. Extraer la referencia de `fila.asiento?.observaciones` con un getter en el componente, buscando
   el marcador `Ref. banco:` **y también** `Ref:` (los dos formatos del §1.2 existen en datos ya
   grabados), y quedándose con lo que sigue hasta el próximo `|` o el fin del texto.
2. Mostrarla como una pieza propia de la fila —un chip o un `<span class="fila-ref">`—, **no
   concatenada dentro de la descripción**, para que se pueda leer de un vistazo al cuadrar contra
   el extracto de la izquierda.
3. **Si no hay referencia, no se muestra nada**: sin placeholder, sin guion. Un asiento que no es de
   pago (uno manual, una nómina) no tiene por qué tenerla, y un campo vacío se lee como «falta un
   dato». *(Es la distinción del §2 de `PLAN-CONSULTA-DOCUMENTOS-V2`: un campo que **no aplica** se
   oculta; uno que aplica y está vacío se muestra vacío. Acá no aplica.)*
4. **No se toca el panel izquierdo** (Extracto Bancario) ni la lógica de selección, suma, diferencia
   o conciliación. Sólo se agrega la pieza a la fila del panel derecho.

## 4. Lo que esto NO arregla, y es decisión del usuario

**Los anticipos a proveedor YA pagados.** Sus asientos están grabados sin la referencia: el BE-1
corrige de acá en adelante, y el FE-1 no puede mostrar lo que nunca se escribió.

Se puede completar con un script de datos —`PGS.PGTR.referenciaBanco` existe y el pago apunta a su
anticipo, así que el dato está— pero **eso es reescribir la observación de asientos contables ya
emitidos**, y eso lo decide el usuario, igual que el `e2-62` con los períodos declarados. Si lo
pide, el árbitro escribe el script con control previo y posterior.

## 5. Criterio de aceptación

1. Pagar un anticipo a proveedor **por transferencia**, con referencia cargada → el asiento generado
   tiene `| Ref. banco: <número>` en su observación, igual que un egreso de tesorería.
2. El mismo pago **con cheque** → la observación tiene la nota del cheque **y** la referencia.
3. Pagar un anticipo **sin** referencia → la observación queda exactamente como hoy, sin un `|`
   colgando ni un `Ref. banco:` vacío.
4. Abrir Conciliación Contable: las filas del panel derecho que vienen de un pago muestran su
   referencia, **incluidas las de asientos anteriores a este cambio**.
5. Las filas que no vienen de un pago no muestran nada en ese lugar.
6. La suma, la diferencia y el botón de conciliar siguen funcionando igual.
