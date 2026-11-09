# DISEÑO — Estado APROBADO: el préstamo es vigente recién cuando tesorería desembolsa

omen-saa-1-arb · 2026-11-08 · **Aprobado por el usuario** («El estado aprobado estaría bien», 2026-11-08), con las propuestas de §4 y §5 tal como están (asiento de entrega al desembolso, anular en APROBADO, reverso, tabla sin recalcular). Ejecutores: omen-saa-1-be y omen-saa-1-fe.
Base: mapa de aprobación/desembolso (agente, 2026-11-08) y `BARRIDO-ESTADO-GENERADO-COMO-VIGENTE.md`.

## 1. Pedido (palabras del usuario)

> «Pasa a vigente cuando se desembolsa el dinero, los estados serían generado, aprobado y vigente. Ya se sabe que
> cuando lo aprueban están por hacer el desembolso»
> «Todos estos generados si no están desembolsados o vigentes, no se los debe tomar en cuenta en ningún lado, el
> préstamo es vigente desde que se entrega el dinero»

## 2. Hoy (verificado)

`aprobarConNiveles` (`PrestamoServiceImpl` ~424-603, una transacción), en el último nivel: número Asoprep → orden de
pago en CXP (`PGS.PGTR` origen `CRD_DESEMBOLSO_PRESTAMO`, estado 0 POR_APROBAR; no en reestructuración) → asiento de
entrega (`contabilizarEntrega`, fechado con `fechaInicio`) o, en sustitución, `contabilizarSustitucion` +
`cancelarOriginal` (original → 5/13, cuotas → 7) → **estado 2 VIGENTE**, `fechaAprobacion`, `PRSTIDPG`, `PRSTASEN`.
Tesorería: PGTR 0 → 1 REGISTRADO → 2 EN_ARCHIVO → **3 CONFIRMADO** (4 puntos: cheque en `aprobar`, respuesta del
banco, confirmación manual, orden creada confirmada), todos pasan por `contabilizarSegunOrigen` →
`contabilizarPagoOrigenExterno` (D socios por pagar / H banco, fecha PGTRFRSP). **Ningún punto toca el préstamo**
(dicho en el código, `PagoProgramadoServiceImpl` ~2566). Precedente de callback: `RHH_ANTICIPO_EMPLEADO`
(~3502 al confirmar, ~3711 al revertir).

## 3. Estados nuevos

`6 PENDIENTE → 1 GENERADO → 14 APROBADO → 2 VIGENTE` (14 está libre en `rubros/EstadoPrestamo`).
- **APROBADO** = aprobado por todos los niveles, número Asoprep asignado, orden de pago emitida, **sin dinero
  entregado**. No cuenta en ningún lado (cartera, Petro, mora, pagos, deuda, móvil, certificados).
- Reestructuración (no hay orden de pago, no sale dinero): sigue pasando a **VIGENTE al aprobar**, como hoy.

## 4. Qué se mueve de la aprobación al desembolso

| Paso | Hoy | Nuevo |
|---|---|---|
| Niveles, validaciones, número Asoprep, orden CXP | al aprobar | al aprobar (igual) |
| Estado | VIGENTE al aprobar | **APROBADO** al aprobar; **VIGENTE al confirmarse el pago** |
| Asiento de entrega (D cartera / H socios por pagar) | al aprobar, fecha `fechaInicio` | **al desembolsar**, en la misma transacción y ANTES del asiento del pago, fecha = PGTRFRSP. Así mayor y módulo arrancan la cartera el mismo día |
| Novación: asiento de sustitución + cancelar original (5) | al aprobar | **al desembolsar** (el partícipe no queda con el viejo cancelado y el nuevo sin vigencia). El VS se fija al aprobar (se guarda) y se usa ese |
| `fechaDesembolso` | — | se guarda (PGTRFRSP); campo nuevo o reuso — el ejecutor verifica si ya existe columna; si hace falta DDL, va en el script |
| Tabla de amortización | — | **no se recalcula** (pagaré y tabla firmados al aprobar; la validación Petro al aprobar ya cubre la primera cuota) — pendiente de confirmación del usuario |

**Hook:** en `contabilizarPagoOrigenExterno` (o un punto único al que lleguen los 4 caminos de CONFIRMADO), para
origen `CRD_DESEMBOLSO_PRESTAMO`: `prestamoService.activarPorDesembolso(idPrestamo, fechaDesembolso, usuario)` en la
misma transacción: exige estado 14; asiento de entrega (o sustitución + cancelar original); estado 2; auditoría. Si
el préstamo no está en 14 (préstamos aprobados antes del EAR, ya VIGENTES), **no hace nada** (compatibilidad).

## 5. Anulaciones y reversos

- **Anular un préstamo APROBADO** (desembolso no hecho, o pago rechazado por el banco): permitido; anula la orden
  CXP si no está confirmada; no hay asiento que revertir; préstamo → 12. (Hoy anular solo admite 2/11.)
- **Regla del árbitro (2026-11-08) para cuando entre la aprobación y el desembolso cambió el original (novación):** el
  préstamo nuevo SIEMPRE pasa a VIGENTE al confirmarse el pago (el dinero ya salió; dejarlo en 14 descuadraría socios por
  pagar). Si el original sigue novable se cancela con el VS RECALCULADO (el real); si difiere del VS fijado al aprobar (el que
  se descontó del neto), la diferencia es plata cobrada de más o de menos al partícipe: advertencia en la respuesta de tesorería
  + observación del préstamo con los dos valores, para regularizar a mano (no se crea devolución automática). Si el original ya
  no es novable (cuota vencida, cobro en trámite…): el nuevo pasa a VIGENTE con su asiento de entrega normal, el original queda
  como está, en socios por pagar queda a favor del partícipe el VS descontado, y la advertencia dice que es caso manual.
- **Pago rechazado por el banco** (PGTR 4): el préstamo queda APROBADO; crédito lo anula y genera otro, o tesorería
  reemite (el ejecutor reporta si existe reemisión; no se crea en este cambio).
- **Revertir un pago CONFIRMADO de desembolso** (`revertirPagoConfirmado`): si el préstamo no tiene pagos
  aplicados → revierte el asiento de entrega y vuelve a APROBADO; si es novación o tiene pagos → se rechaza el
  reverso con mensaje (caso manual). El ejecutor confirma que esto encaja con el precedente RHH.

## 6. «No cuenta en ningún lado» (1, 6, 7, 14)

- Aplicar `BARRIDO-ESTADO-GENERADO-COMO-VIGENTE.md` §A y §A' completo, con lista blanca {2, 8, 10, 11}; el 14 queda
  fuera por construcción.
- Listas negras que hay que extender con 14: `ESTADOS_FUERA_DE_CARTERA "0,1,6,7,12"` (`ModoCorteCartera:62`),
  G46 `{0,1,6,7,12}`, `contabilizarDesembolsoHistorico` (rechaza 0/1/6/7/12), `ProcesoCargaPetroServiceImpl`
  terminal {3,4,5}, `ProcesoMoraPrestamoServiceImpl` terminal {3,4,5}.
- Flujo: guarda de regenerar tabla (hoy bloquea 2/7) **debe bloquear 14**; `saveSingle` NO debe tratar 14 como
  editable (borraría el número Asoprep).
- Nombres/etiquetas: `nombreEstadoPrestamo`, `DevolucionAporteRest`, `ResolutorDesembolsoPrestamo` (sumar 12, 13,
  14), `RPRT_TBLA_ACML`, `RPRT_INFR_DVAP`, `CertificadoServiceImpl`.

## 7. Catálogo (script 371, lo revisa el árbitro antes de entregar)

- `CRD.ESPS`: fila APROBADO con `ESPSCDEX = 14` (la usan filtros del FE).
- Rubro SCP de estados de préstamo: fila 14 como en `325_DDL_ANULACION_PRESTAMO.sql` (documental si el rubro no se
  encuentra).
- Columna de fecha de desembolso si no existe.
- Control previo de solo lectura: préstamos VIGENTES con orden de desembolso NO confirmada (aprobados y no pagados
  al momento del EAR) — se listan para decidir; el código nuevo no los toca.

## 8. Frontend

`prestamo-edit` (enum/nombres/clases con APROBADO, chip; anular también en APROBADO; textos de «aprobado» →
«aprobado, pendiente de desembolso»; imprimir permitido), `catalogos-pago.ts` lista blanca, `participe-dash`
(borrar `recalcularEstadoPrestamo`), mapas de estado listados en el mapa del agente.

## 9. Documentación

`crd/API-CICLO-OTORGAMIENTO.md`, `crd/DISENO-REESTRUCTURACION-NOVACION.md` (momento de cancelar el original),
contrato de CXP/pagos programados (callback nuevo), `petro/REGLAS-*` si cambian listas de estados en asoprep.
