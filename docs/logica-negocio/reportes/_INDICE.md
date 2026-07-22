# Índice — Lógica de Negocio de Reportes

> Este directorio contiene la lógica de negocio de todos los reportes G (SBS) y reportes de cartera (internos).
> **Cada archivo cubre exactamente un reporte.** Leer solo el archivo del reporte que se va a modificar.

## Reglas de mantenimiento
1. **Si se modifica la lógica de un G → actualizar el MD de ese G.**
2. **Si se modifica la lógica de un reporte de cartera → actualizar su MD.**
3. Los reportes de cartera SIEMPRE comparten la misma lógica base que su G equivalente.
   Si cambia G42 → revisar CPRM.md. Si cambia G44 → revisar CJBM.md. Si cambia G48 → revisar CCPM.md.
4. La lógica compartida entre G48 y CCPM (grupos, calificación, provisión) está en `_LOGICA_COMPARTIDA_CREDITO.md`.
   Si cambia esa lógica → actualizar ese archivo Y el MD del G/reporte afectado.

---

## Archivos de contexto (leer solo cuando se necesite contexto general)
| Archivo | Contenido |
|---|---|
| `_GENERAL.md` | Orquestador, estados EJRC/EJRD, patrón estándar para crear un G nuevo |
| `_LOGICA_COMPARTIDA_CREDITO.md` | Grupos de cuotas, regla del último día del mes, calificación, porcentajes de provisión — compartido entre G48 y CCPM |

## Reportes G (SBS)
| Archivo | Reporte | Descripción |
|---|---|---|
| `G40.md` | G40 CreditoG40 | Información general del fondo (solo junio y diciembre) |
| `G41.md` | G41 ParticipeActivoG41 | Partícipes activos nuevos |
| `G42.md` | G42 SaldoCuentaG42 | Saldos de cuenta individual |
| `G43.md` | G43 ParticipeCesanteG43 | Partícipes cesantes del mes |
| `G44.md` | G44 ParticipeJubiladoG44 | Partícipes jubilados |
| `G45.md` | G45 NuevoParticipeG45 | Datos ampliados de nuevos partícipes |
| `G46.md` | G46 NuevoPrestamoG46 | Nuevos préstamos del mes |
| `G47.md` | G47 NovacionG47 | Novaciones del mes |
| `G48.md` | G48 SaldoOperacionG48 | Saldo de operaciones de crédito |
| `G49.md` | G49 CancelacionG49 | Cancelaciones del mes |
| `G50.md` | G50 GaranteG50 | Garantes |
| `G51.md` | G51 GarantiaRealG51 | Garantías reales |

## Reportes de Cartera (internos)
| Archivo | Reporte | G equivalente |
|---|---|---|
| `CPRM.md` | CPRM CreditoParticipesMensual | G42 |
| `CJBM.md` | CJBM CreditoJubiladosMensual | G44 |
| `CCPM.md` | CCPM CreditoCuotasPrestamosMensual | G48 |
