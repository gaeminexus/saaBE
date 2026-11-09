# DISEÑO — Bloquear la novación de un original con otra novación en curso, y dashboard de cartera viva + solicitudes

omen-saa-1-arb · 2026-11-09 · Decisiones del usuario (después del EAR aprobado-1109; van en el EAR siguiente).
Ejecutores: omen-saa-1-be y omen-saa-1-fe.

## 1. Novación en curso
> «Novación en curso: seguimos tu recomendación»

Hoy un original con una novación en estado 14 APROBADO (o 1/6, aún sin aprobar) sigue saliendo como sustituible; se
podría armar otra novación del mismo original (el desembolso la detectaría como «caso manual»).
- `SustitucionPrestamoServiceImpl` (evaluarOriginal / lista de sustituibles / validaciones de guardar y aprobar): un
  original NO es sustituible si existe otro préstamo con `PRSTPROR = original` en estado **1, 6 o 14** (excluido el
  préstamo que se está editando). Motivo: «El préstamo X ya tiene la novación/reestructuración Y en trámite (estado
  …)». Afecta también al simulador de novación (usa evaluarOriginal).
- Reestructuración: misma regla (un original con una reestructuración en 1/6 no admite otra).

## 2. Dashboard de préstamos
> «Dashboard, solo cartera viva, pero una sección para ver las solicitudes, cuántas recibidas, aprobadas, rechazadas, etc.»

- Resumen por estado (`EntidadDaoServiceImpl.selectResumenPrestamosPorEstado` / `selectResumenConsolidadoPorEstado`,
  FE `prestamo-dash`): **solo cartera viva** `PRSTIDST IN (2, 8, 10, 11)`.
- **Sección nueva «Solicitudes»** en el mismo dashboard, con filtro de rango de fechas (por fecha de registro del
  préstamo, `PRSTFCRG`; por defecto el mes en curso) y, si el dashboard ya filtra por filial/producto, los mismos
  filtros. Cifras (cantidad y monto):
  - Recibidas: todas las registradas en el rango (cualquier estado).
  - En trámite: 6 PENDIENTE + 1 GENERADO.
  - Aprobadas pendientes de desembolso: 14.
  - Desembolsadas: con fecha de desembolso (PRSTFCAC) en el rango, o hoy vigentes/canceladas que nacieron en el rango
    (el ejecutor propone la definición exacta y la reporta).
  - Rechazadas: 7. Anuladas: 12.
  - Opcional si es barato: desglose por producto y tiempo promedio registro → aprobación → desembolso.
- Endpoint nuevo de solo lectura (una consulta agregada, sin hidratar entidades), contrato que el BE reporta al FE.
- Docs: API del dashboard que corresponda.
