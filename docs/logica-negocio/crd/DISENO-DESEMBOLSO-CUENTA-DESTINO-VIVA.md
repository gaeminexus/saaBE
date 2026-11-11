# Orden de desembolso con la cuenta del socio siempre al día (equipo omen-saa-3, 2026-11-11)

Pedido del usuario: *«corrijamos de una vez para que no vuelva a suceder»* (préstamo 8530, orden 1120 en la
bandeja de Aprobación de pagos con «Sin cuenta de destino» aunque el socio sí tenía cuenta; se reparó a mano con
`crd/sql/e3-18-desembolso-sin-cuenta-destino.sql`).

## Causa

`PrestamoServiceImpl.aprobar` (omen1, `5b6b09fb`) copia **una sola vez**, al aprobar, banco / tipo / número de la
cuenta activa única del socio (`CRD.CNBP`) a la orden (`PGS.PGTR.PGTRBFBC/PGTRBFTP/PGTRBFCT`,
`armaBeneficiarioPrestamo`). Es una foto:

- Si la cuenta se registra o corrige **después** de aprobar (o el préstamo se aprobó con un EAR anterior a
  `5b6b09fb`), la orden queda sin cuenta para siempre.
- `motivoSinCuentaActivaUnica` solo cuenta las activas: una cuenta activa **sin banco o sin tipo** pasa la
  validación y la orden nace incompleta.
- La bandeja solo ofrece «Asignar cuenta» a pagos con titular de TSR; el socio no lo es → sin salida en pantalla.

## Corrección (solo BE, sin SQL, sin FE)

### D1 — La validación al aprobar exige cuenta COMPLETA
`CuentaBancariaParticipeServiceImpl.motivoSinCuentaActivaUnica`: con exactamente una activa, además debe tener
banco (`BEXTCDGO`), tipo (`CNBPTPCN`) y número (`CNBPNMRO` no vacío). Si no: *«La cuenta bancaria activa del socio
no tiene banco / tipo de cuenta / número; complétela antes de aprobar»* (nombrando lo que falta). Lo usan el aprobar
del préstamo (`CUENTA_DESTINO_NO_VALIDA`) y `NivelAprobacionServiceImpl` (aviso), los dos quedan coherentes.

### D2 — Cambiar la cuenta del socio actualiza sus órdenes de desembolso pendientes
Tras **toda** escritura de `CRD.CNBP` de un socio (`saveSingle` —las dos sobrecargas pasan por la de un
argumento—, `save(List)`, `crearConCertificado`, `remove`), en la misma transacción:

1. Órdenes afectadas: `PGS.PGTR` con `PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'`, `PGTRESTD = POR_APROBAR (0)`, y
   `PGTRIDOR` = un préstamo del socio en estado **APROBADO (14)** con `PRSTIDPG` = esa orden.
   **Nunca** se toca una orden en REGISTRADO / EN_ARCHIVO / CONFIRMADO / RECHAZADO / ANULADO (ya salió o va al banco).
2. Con la cuenta activa única **completa** → se copian banco, tipo y número (`TRIM`).
   Sin ella (0 activas, 2+, o incompleta) → se **vacían** los tres campos: la bandeja avisa «Sin cuenta de destino»
   y el aprobar por transferencia rechaza, como corresponde, en vez de mandar plata a una cuenta que el socio ya
   desactivó.
3. `System.out.println` por orden tocada (orden, préstamo, antes → después, sin el número completo de cuenta: los
   últimos 4 dígitos).
4. Si falla la actualización de la orden, falla el guardado de la cuenta (misma transacción): no queda la cuenta
   nueva con la orden vieja.

La escritura sobre `PGS.PGTR` va por un método nuevo de CXP, no por SQL suelto desde CRD:
`PagoProgramadoService.actualizarBeneficiarioOcasional(Long idPago, Long idBancoExterno, Long tipoCuenta,
String numeroCuenta)` — solo si el pago está en POR_APROBAR, es de origen externo y no tiene titular
(`PGTRTTLR`/`cuentaDestino` nulos); si no, `IncomeException`. Nombre y cédula del beneficiario no cambian.
La búsqueda de las órdenes (préstamos 14 del socio con `PRSTIDPG`) va en el DAO de préstamo o de pago, con
`@NamedQuery`/JPQL en el DAO, no en el service.

### D3 — Lo que no cambia
- El aprobar del préstamo sigue copiando la cuenta igual (D2 solo la mantiene al día).
- Devolución de aportes: tiene su propio «cambiar cuenta» (`DevolucionAporteServiceImpl` ~1382); fuera de alcance.
- La bandeja y el FE: sin cambio; el aviso desaparece solo al guardar la cuenta del socio.

## Verificación
- `mvn -q compile`.
- Lectura del árbitro: los cinco puntos de escritura llaman a la sincronización; filtro por estado 0 y préstamo 14.
- Humo (lectura, con un caso real): socio con préstamo APROBADO y orden sin cuenta → registrar/editar su cuenta en la
  ficha del partícipe → la bandeja muestra la cuenta. Sin pagos de prueba.

## Autoría
Toca `5b6b09fb` de omen1 (aprobar) indirectamente y `CuentaBancariaParticipeServiceImpl`; se le avisa a omen1 con
el visto bueno del usuario.
