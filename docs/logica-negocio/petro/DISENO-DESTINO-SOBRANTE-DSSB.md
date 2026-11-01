# Destino del sobrante y devolución en la carga Petro/ARCH (B8): tabla `CRD.DSSB`

**Estado: DISEÑO, pendiente de 3 decisiones del usuario (§4).** omen-saa-1-arb, 2026-11-01. La
propuesta la levantó omen-saa-1-be leyendo el código. El árbitro la revisó.
Relacionados: `DISENO-AFECTACION-MANUAL-V2.md` (§3.2 sobrante del partícipe, §7 U3 decidida, U2,
U4) y `PLAN-CORRECCIONES-CARGA-2026-11-01.md` B8.

## 1. Problema
Hoy, todo lo que una carga no puede aplicar queda como **novedad bloqueante**. Pasa con:
- el excedente del motor;
- el aporte que no cabe en ningún mes;
- el seguro de incendio sin destino;
- el parcial de ARCH.

El operador lo reparte a mano, por monto, con AVPC. El usuario pidió poder elegir **un destino por
partícipe** sin repartir montos: siguiente cuota, aporte o devolución.

**AVPC no sirve para esto:** cuelga de una novedad y exige un monto. Se mantiene como está para los
casos raros.

## 2. Datos: `CRD.DSSB`, una fila por carga y partícipe (nombre reservado el 2026-11-01)
| Columna | Tipo | Qué |
|---|---|---|
| DSSBCDGO | NUMBER PK | secuencia `CRD.SQ_DSSBCDGO` |
| CRARCDGO | NUMBER NOT NULL | FK `CRD.CRAR` (la carga) |
| ENTDCDGO | NUMBER NOT NULL | FK `CRD.ENTD` (el partícipe) |
| DSSBTPDS | NUMBER NOT NULL | 1 siguiente cuota · 2 aporte · 3 devolución (rubro de constantes nuevo `DestinoSobrante`) |
| PRSTCDGO | NUMBER NULL | FK `CRD.PRST`, solo con destino 1. Null = la cuota pendiente más antigua de un préstamo vigente o en mora; el plazo vencido no entra (U2) |
| TPAPCDGO | NUMBER NULL | FK `CRD.TPAP`, solo con destino 2: 9 jubilación, 11 cesantía |
| DSSBVLAP | NUMBER(18,2) NULL | lo que realmente se aplicó; lo escribe el proceso |
| DSSBPGTR | NUMBER NULL | pago programado de Tesorería, sin FK, como `PRSTIDPG` (solo destino 3) |
| DSSBASNT | NUMBER NULL | asiento que llevó el valor a socios por pagar (para que el reproceso lo anule) |
| DSSBESTD | NUMBER NOT NULL | 1 activo, 0 anulado |
| DSSBOBSR, DSSBUSAR, DSSBFCRG, +modificación | | auditoría, patrón de la casa |

Restricciones:
- **CHECK** de coherencia entre destino y columnas.
- **Un único destino activo por (carga, partícipe):** índice único con función sobre `DSSBESTD = 1`.
- Índice por `CRARCDGO`.

## 3. Comportamiento
1. **Cuándo actúa:** el paso de sobrante corre **después** de la aplicación automática y de las AVPC. Si
   el partícipe tiene un DSSB activo, en vez de la novedad bloqueante:
   - **destino 1:** el motor aplica a ese préstamo;
   - **destino 2:** reparte en el tipo de aporte por devengo (B2);
   - **destino 3:** registra la devolución.

   El sobrante exacto recién se conoce al procesar; en la vista previa no.
2. **Validaciones:**
   - un DSSB activo cuenta como «con destino» para las novedades de sobrante;
   - el control recibido = aplicado suma también las devoluciones.
3. **Contabilidad:** dentro del asiento de aplicación, una línea DEBE puente / HABER **línea nueva de la
   plantilla 21, aux1 61** `CrdLineaAsiento.DEVOLUCION_DESCUENTO_PARTICIPE`. Hoy esa línea apunta a
   `2.3.90.90.10`.
   - La puente es el aux1 3 si el dinero entró por aportes, o el aux1 4 si entró por préstamos.
   - **Las puentes tienen que seguir quedando en cero (D-024).**
   - Si falta la línea, el proceso se detiene con mensaje claro.
4. **Tesorería:** `registrarPagoDeOrigenExterno` con el origen nuevo
   `OrigenPagoExterno.CRD_DEVOLUCION_DESCUENTO` e `idOrigen = DSSBCDGO`.
   - El producto de pago se resuelve desde la cuenta de la línea 61, como en los desembolsos.
   - Se paga en el mismo mes o acumulado (U3).
5. **U4:** `validarAporteAH` deja de marcar APORTE_MONTO_INCONSISTENTE cuando el reparto por vigencias
   absorbe el monto.
6. **Frontend:** en la vista previa, una pantalla de destino por partícipe que marca a los que pueden
   tener sobrante. Necesita contrato de API antes de empezar.
7. **Resolutor de seguimiento en cxp** (`PagoProgramadoServiceImpl` ~4016): es del **equipo 2, que
   está abajo**. Sin el resolutor el pago funciona igual; solo falta ver el estado del documento
   desde el seguimiento. Queda pendiente para el equipo 2.

## 4. Decisiones
**Las toma el árbitro (técnicas):**
- **D1:** un destino por partícipe y carga, no por fila. El sobrante es del partícipe (§3.2 V2).
- **D2:** la devolución va dentro del asiento de aplicación; así se anula junto con la carga.
- **D5:** aux1 61, confirmado con `crd/sql/355`.

**Las toma el usuario:**
- **DU1. Reproceso de una carga cuya devolución ya pagó Tesorería.** *Recomendado:* la devolución queda
  viva y no se repite. El reproceso anula y rehace lo demás. Si todavía no está pagada, se anula
  junto con la carga.
- **DU2. Destino por defecto cuando el operador no eligió ninguno.** Hoy queda como novedad bloqueante.
  *Recomendado:* que siga así. Nada se devuelve ni se aplica sin que alguien lo decida.
- **DU3. Alcance de la primera entrega.** *Recomendado:* backend y frontend completos, sin el
  resolutor de cxp hasta que vuelva el equipo 2.

## 5. Orden
`355` (verificación) → DDL `357` + semilla de la línea 61 (árbitro) → backend → contrato de API →
frontend → EAR. El DDL y la semilla van antes del EAR.
