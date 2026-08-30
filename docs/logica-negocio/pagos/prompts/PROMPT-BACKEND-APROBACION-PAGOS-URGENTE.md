# PROMPT — **BACKEND** — Cerrar Aprobación de Pagos (urgente) + origen CXC

> **Eres el agente de BACKEND** del repositorio `saaBE`. Trabajas en paralelo con un agente de
> FRONTEND que toca `saaFE`, y con otro equipo de 3 agentes que trabaja **solo en el módulo
> `crd`**. **No edites nada de `crd`** (paquetes `com.saa.ejb.crd`, `com.saa.model.crd`,
> `com.saa.ws.rest.crd`, ni sus DAOs) — ese módulo lo cierra el otro equipo. Tu alcance es `cxp`,
> `tsr`, `rhh`, `cxc` (solo lo que se pide aquí).
>
> **No compilas ni despliegas.** `mvn` no está en el PATH; el usuario compila en Eclipse. No
> intentes verificar con `javac`/`mvn`; entrega el código y dilo explícitamente.
>
> **No tocas SQL ni lo ejecutas.** Si necesitas una columna o tabla que no existe, repórtalo y
> detente — el árbitro escribe el DDL.
>
> **Documentos obligatorios antes de escribir código** (léelos completos, en este orden):
> 1. `docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md` — diseño y estado real (§7).
> 2. `docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` §7bis — de dónde sale
>    el saldo bancario real (decisión ya tomada, no la reabras).
> 3. `CLAUDE.md` — capas, convenciones y trampas del repositorio.
>
> Reporta cada ítem apenas lo termines, con el formato: `ÍTEM <n> — COMPLETADO | BLOQUEADO`, y una
> línea de qué archivos tocaste. No esperes a terminar todo para reportar.

---

## Contexto — por qué esto es urgente

Producción tiene desplegado el código que ya NO pide cuenta bancaria al registrar un pago (nace
`POR_APROBAR`), pero la columna `PGS.PGTR.PGTRCNBC` sigue `NOT NULL` en la base — el usuario ya
tiene el DDL para corregirlo y lo va a correr por su cuenta (`sql/01-aprobacion-pagos.sql`,
existente, tú no lo tocas). **Tu trabajo no es esa corrección** — es terminar lo que falta del
ciclo para que la pantalla de aprobación funcione completa: validar disponibilidad de saldo, y
agregar un origen nuevo (CXC) que el negocio pidió hoy.

---

## ÍTEM 1 — Validar disponibilidad de saldo (desbloqueado, decisión ya tomada)

`PagoProgramadoServiceImpl.validaDisponibilidad` (línea ~1260) es un no-op a propósito — estaba
esperando que se decidiera de dónde sale el saldo real de una cuenta bancaria. **Ya se decidió**
(`DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` §7bis, 2026-08-27): **de la contabilidad**, vía
`PlanCuentaService.saldoCuentaFechaEmpresa(idEmpresa, idCuenta, fecha)` — **no** de
`CuentaBancariaService.saldoSegunMovimientosBanco` (antes `obtieneSaldoFecha`), que solo cubre
entre 1% y 5% del movimiento real. No reabras esta decisión.

Implementa:

```java
private void validaDisponibilidad(Long idCuentaBancaria, double totalAprobado, LocalDate fecha)
        throws Throwable {
    CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuentaBancaria);
    if (cuenta == null || cuenta.getPlanCuenta() == null) {
        throw new IncomeException("La cuenta bancaria no tiene plan de cuenta asociado; no se puede validar disponibilidad.");
    }
    Double saldoContable = planCuentaService.saldoCuentaFechaEmpresa(
        cuenta.getEmpresa().getCodigo(), cuenta.getPlanCuenta().getCodigo(), fecha);
    double comprometido = sumaPagosComprometidos(idCuentaBancaria, fecha); // ver abajo
    double disponibleReal = (saldoContable != null ? saldoContable : 0.0) - comprometido;
    if (totalAprobado > disponibleReal + 0.01) {
        throw new IncomeException(String.format(
            "Saldo insuficiente. Disponible: %.2f (saldo %.2f - comprometido %.2f). Total a aprobar: %.2f.",
            disponibleReal, saldoContable, comprometido, totalAprobado));
    }
}
```

- **`sumaPagosComprometidos`**: suma de `PagoProgramado.valor` de esa `idCuentaBancaria` en estado
  `REGISTRADO(1)` o `EN_ARCHIVO(2)` — pagos ya aprobados que todavía no se confirman contra el
  banco. Sin esto, dos aprobaciones seguidas pasan la validación cada una por su lado y juntas
  sobregiran la cuenta (§3.3 del plan, es imprescindible, no opcional). Ponlo en
  `PagoProgramadoDaoService` con JavaDoc, sin `selectAll()`.
- Inyecta `PlanCuentaService` en `PagoProgramadoServiceImpl` (mismo patrón que
  `ConciliacionCierreServiceImpl`, que ya lo hace para el mismo propósito).
- La excepción la debe capturar `aprobar()` tal como ya hace con el resto de validaciones
  (`IncomeException` → 400, mismo estilo del módulo).

## ÍTEM 2 — `GET /pgtr/disponibilidad/{idCuenta}`

Del plan §4 Fase 1, marcado "no implementado en esta fase" — impleméntalo ahora, usando el mismo
cálculo del ítem 1 (refactoriza para no duplicar la fórmula):

```
GET /rest/pgtr/disponibilidad/4?fecha=2026-08-28
```
`fecha` opcional (vacío = hoy). Respuesta 200:
```json
{ "idCuentaBancaria": 4, "saldo": 2714031.22, "comprometido": 3750.00, "disponible": 2710281.22 }
```
400 si la cuenta no existe o no tiene `planCuenta`.

## ÍTEM 3 — Nuevo origen externo: CXC (devolución a cliente)

El negocio pidió hoy que las devoluciones de dinero a clientes (`CXC.AnticipoCliente` con
`saldo > 0`) también entren al circuito único de aprobación de pagos, igual que ya lo hacen
`CRD_DEVOLUCION_APORTE`, `TSR_CAJA_CHICA` y `RHH_ANTICIPO_EMPLEADO`. Sigue el mismo patrón,
**cópialo de `AnticipoEmpleadoServiceImpl` (RHH), es el más simple de los tres precedentes**:

1. En `com.saa.rubros.OrigenPagoExterno`, agrega:
   ```java
   /**
    * Devolucion de saldo a favor de un cliente, originada en CXC.AnticipoCliente.
    * PGTRIDOR lleva el CXC.AnticipoCliente.id correspondiente.
    */
   public static final String CXC_DEVOLUCION_CLIENTE = "CXC_DEVOLUCION_CLIENTE";
   ```
   **Sin DDL**: `PGTRORGN`/`PGTRIDOR` son genéricas (string opaco + id), ya soportan cualquier
   origen sin tocar el esquema — confirma esto leyendo el JavaDoc de la propia interfaz antes de
   pedir una columna que no hace falta.
2. En `com.saa.ejb.cxc.service.AnticipoClienteService` (o el service que corresponda — verifica el
   nombre exacto, cópialo de cómo está armado el resto de `cxc`), agrega un método
   `solicitarDevolucion(Long idAnticipo, Double valor, String usuario)` que:
   - Valida que el anticipo existe, `estado` es el vigente (verifica primero cuál es la columna de
     estado real de `AnticipoCliente` contra el código, no lo asumas — puede repetir la trampa de
     "dos columnas de estado" que describe `CLAUDE.md`), y `saldo >= valor`.
   - Llama `pagoProgramadoService.registrarPagoDeOrigenExterno(OrigenPagoExterno.CXC_DEVOLUCION_CLIENTE, idAnticipo, idEmpresa, /* idCuentaBancariaOrigen */ null, beneficiario, concepto, valor, fecha, usuario)`
     — exactamente la firma de 3 argumentos nula-cuenta que usa `AnticipoEmpleadoServiceImpl:202`.
     El beneficiario es el `Titular` del anticipo; el concepto describe "Devolución anticipo
     cliente #<id>".
   - **No descuenta el `saldo` del anticipo todavía** — eso se hace cuando el pago se **confirma**
     (mismo patrón que revisa `DevolucionAporteServiceImpl` para su reconciliación en CRD — pero
     **no copies código de `crd`, solo el patrón**: busca el equivalente conceptual en cómo CXC ya
     resuelve sus propios estados). Si no encuentras un hook de confirmación de pago que CXC pueda
     escuchar, repórtalo como bloqueante en vez de inventar un mecanismo nuevo.
3. Expón un endpoint REST mínimo, `POST /rest/antc/solicitarDevolucion` con
   `{idAnticipo, valor, usuario}`, mismo estilo `catch (Throwable e)` del resto del módulo.

**No implementes reconciliación automática de saldo del anticipo en este ítem si no encuentras
dónde engancharla** — repórtalo y sigue con lo demás; es preferible un origen que registra bien el
pago y deja el descuento del saldo como pendiente explícito, a inventar un mecanismo sin precedente
en el código.

## ÍTEM 4 — Barrido de verificación (rápido, antes de dar todo por cerrado)

Confirma que **todavía funcionan sin cambio de comportamiento** los tres orígenes externos
existentes (`registrarPagoDeOrigenExterno` desde CRD, TSR caja chica, RHH anticipo) después de tus
cambios en `PagoProgramadoServiceImpl` — no deberías haber tocado esos caminos, pero repórtalo
explícitamente.

---

## Reglas de la casa

- Español en código, comentarios y commits.
- Los métodos de service/REST empiezan con la línea de traza `System.out.println`.
- REST: `catch (Throwable e)` → `Response.status(INTERNAL_SERVER_ERROR).entity("Error ...: " + e.getMessage())`.
- Usa las interfaces de `com.saa.rubros`, nunca literales.
- Prohibido `selectAll()` en DAOs de este trabajo.
- Si cualquier ítem depende de un dato o columna que no existe, **repórtalo y detente** — no
  inventes DDL ni lo ejecutes.
