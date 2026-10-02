# DISEÑO — Que el saldo global de anticipos (TSR.PRCC tipo 2) no se vuelva a descuadrar

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-02 por `omen-saa-2-arb`, antes de despachar.
**Origen:** estado del equipo §58, `e2-80`. Pedido del usuario: *«corrige las causas en el código»*.

## 1. Qué es ese saldo y quién lo usa

`TSR.PRCC.PRCCSLIN` (`PersonaCuentaContable.saldoInicial`), en la cuenta **tipo 2 (anticipos)**, es un
**saldo global por titular** que se mueve en paralelo al saldo de cada anticipo:

- sube al confirmar un anticipo (`actualizarSaldoInicialPrcc`);
- baja en cada cruce (`AplicacionPagoCxpServiceImpl.aplicaCruces`, y su par en CxC);
- baja en cada devolución;
- se repone al reversar o anular.

Los cruces y las devoluciones **validan contra él**. Medido el 2026-10-02: `getSaldoInicial()` **solo
lo leen flujos de anticipos**. En las cuentas de otros tipos (1 facturas, etc.) no lo usa nadie.

## 2. Las dos causas del descuadre (seis proveedores en el `e2-80`)

1. **`actualizarSaldoInicialPrcc` se traga el error.** Está en `AnticipoProveedorServiceImpl:1140-1172`
   y, **idéntico**, en `AnticipoClienteServiceImpl:~965-995`. Si no encuentra la cuenta, hace `return`.
   Si algo falla, el `catch (Throwable)` solo imprime. El anticipo queda CONFIRMADO y el saldo global
   no sube. Llamadores: proveedor `:406` (confirmar) y `:492` (anular); cliente `:293`, `:435` y `:605`.
2. **Titulares → «Editar saldo inicial»** (`titulares-v2.component.ts:811`) pisa ese saldo con
   cualquier número, vía `PUT /prcc` → `PersonaCuentaContableServiceImpl.saveSingle`.

## 3. El arreglo

### 3.1 Backend — `actualizarSaldoInicialPrcc`, en proveedor Y en cliente

- **Sin `try/catch`.**
- **Sin cuenta → `IncomeException`:** *«El {proveedor|cliente} '{nombre}' no tiene cuenta contable de
  anticipos (Tipo 2, Rol: {Proveedor|Cliente}) configurada para la empresa {id}: no se puede
  {confirmar|anular} el anticipo. Configúrela en Tesorería → Persona → Cuentas Contables.»*
- Efecto: si el saldo global no se puede mover, **la operación entera no se hace**. Es preferible a un
  anticipo confirmado con el global descuadrado.
- ⚠️ **Verificar antes de cambiar**, y si se cumple, **parar y reportar**: si confirmar un anticipo se
  dispara **dentro de la confirmación de un lote bancario**, que un pago lance una excepción **no puede
  abortar los otros pagos del lote**. El ejecutor busca los llamadores de la confirmación del anticipo
  (desde `PagoProgramadoServiceImpl`, `contabilizarSegunOrigen`) y reporta el comportamiento
  transaccional, con archivo y línea.

### 3.2 Backend — `PersonaCuentaContableServiceImpl`: el saldo de anticipos no se edita a mano

En `saveSingle` y en `save(List)`, si la cuenta **es o pasa a ser tipo 2**:

- **Edición** (`codigo` no nulo, y la fila persistida es tipo 2): se ignora el `saldoInicial` que manda
  el cliente y se conserva el persistido. Si venía distinto, traza
  `"⚠ PRCC {id}: saldoInicial de anticipos ignorado ({enviado}); se conserva {persistido}"`. **No es
  error**: el resto de los campos se graba normal.
- **Alta** (o una fila de otro tipo que pasa a ser tipo 2): `saldoInicial` = **la suma de los saldos de
  los anticipos CONFIRMADOS de ese titular y empresa** para el rol de la cuenta:
  - rol proveedor (`RolPersona.PROVEEDOR`): `AnticipoProveedorDaoService.sumaSaldoDisponible(idTitular, idEmpresa)`;
  - rol cliente: el equivalente de `AnticipoClienteDaoService`;
  - otro rol: 0.

  Así, la cuenta que se crea tarde (el caso de JLCONTROL) nace cuadrada.
- El rol de la cuenta sale de `personaRol.rubroRolPersonaH`.
  ⚠️ **Hay que cargar el `PersonaRol` por id**: el payload del FE trae solo `{ codigo }`.

`PersonaCuentaContableServiceImpl` es de `tsr`, que se comparte con `lap-saa-1`. Antes de editar:
`git status` y `git log -3`.

### 3.3 Frontend — Titulares

En `titulares-v2.component.html:~332`, el botón **«Editar saldo inicial» no se muestra para la cuenta
tipo 2**. En su lugar va un ícono `lock`, con el tooltip *«El saldo de anticipos lo lleva el sistema: se
mueve con los anticipos, los cruces y las devoluciones.»* Para los otros tipos queda como está.

## 4. Lo que NO cambia

Los cruces, las devoluciones, los reversos y la anulación, que ya mueven el saldo bien. Los datos de los
seis proveedores: el `e2-81` cuadra dos, y los otros cuatro esperan a medirse.
