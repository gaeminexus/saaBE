# Plan: cuentas contables escritas en el código → plantilla o parámetro

**omen-saa-1-arb, 2026-11-01.** Regla del usuario: «todas esas cuentas deben poder ser parametrizables
desde una plantilla». Auditoría de solo lectura de BE (todo `src/main/java`), FE y los 58 `.jrxml`.
Los hallazgos ALTA y los de `ControlCierreServiceImpl` los verificó el árbitro en la línea.

**Resultado:** el patrón de plantilla está bien adoptado (Petro, pensión, nómina, IVA por `PGS.TSRI`,
estado financiero por rubro 250). Los literales están concentrados en 4 archivos del BE y 1 del FE.
**Ningún `.jrxml` filtra por cuenta fija.**

## Ítems (en orden de despacho)

| # | Sev. | Dónde | Literal | Qué hace | Se mueve a |
|---|---|---|---|---|---|
| P1 | MEDIA, fácil | `ControlCierreServiceImpl:67, 75-76` | `1.4.05.05`, `2.3.02.05`, `1.4.05.10`, `2.3.02.10` | Controles D-024 de las puentes | Las líneas que **ya existen**: `CrdLineaAsiento` aux 1-4 de la plantilla de aplicación Petro. Sin semilla |
| P2 | MEDIA, fácil | `ControlCierreServiceImpl:90-105` | `SUBSTR(DTASCNTA,1,1)` = 1…5 | Ecuación contable del cierre | Rubro 250 `CntParametroEstadoFinanciero.GRUPO_*`, **que ya existe** y que `EstadoFinancieroOficialServiceImpl:188-204` ya lee. Sin semilla |
| P3 | ALTA | `CierreCarteraServiceImpl:1426-1438` (`cuentaQueAbsorbe`) | `1.3.01.25`, `1.3.05.25`, `1.3.09.25`, `grupo+".25"` | ⑨ ajuste de bandas: **contabiliza** la diferencia del grupo en esa cuenta | Plantilla 37 (cierre de cartera): 3 líneas nuevas `AJUSTE_BANDA_ABSORBE_Q/P/H`, con semilla |
| P4 | ALTA | `CierreCarteraServiceImpl:1312-1313, 1400` y `ControlCierreServiceImpl:185, 205, 413-414` | `1.3.`, `1.3.99`, rangos 01-04/05-08/09-12 | Alcance de cartera y agrupación Q/P/H | La provisión `1.3.99` = línea `PROVISION_CARTERA` (83), que **ya existe**. Las bandas por grupo, desde `CRD.BNDP` por producto. Requiere diseño: no hay catálogo Q/P/H. **Va después de P3, con su propio diseño** |
| P5 | ALTA | `AsientoContableServiceImpl:97` (se usa en 3129 y 3781) | `4.8.90.90.35` | Línea de redondeo SRI en el asiento de compra CXP. Además busca la cuenta **sin filtrar por empresa** | Línea nueva de redondeo en la plantilla de compras CXP, como hace RHH. Con semilla |
| P6 | ALTA | `CargaHistoricaDocumentoServiceImpl:298-299, 1137, 1258-1259` | `2.3.90.10`, `1.4.90.10`, `1.9.01.90`, `4.8.90.90.35` | Cargador histórico: crea la cuenta por cobrar o por pagar **permanente** del titular | Parámetro o línea de plantilla de la carga histórica. Con semilla. Para el redondeo, la misma línea que P5 |
| P7 | MEDIA | `CargaHistoricaNominaPasoServiceImpl:103` | `4.3.` | Centro de costo para las líneas de gasto de nómina histórica | Las líneas `RhhLineaAsiento.GASTO_*`, que **ya existen** |
| P8 | MEDIA | `ControlCierreServiceImpl:400` | `2.1.` | Control B17, cuenta 21 contra la G42 | Línea nueva en el rubro 250 o en la plantilla de aportes. Con semilla |
| P9 | MEDIA, indirecto | `PagoPensionComplementariaServiceImpl:129, 152` | ids de producto 516/517 | Productos de pago de jubilados. No son cuentas: la cuenta sale del producto | Rubro o parámetro. Sin prisa |
| P10 | MEDIA, FE | `saaFE rrh/model/liquidacion-externa.ts:76-94` | 8 cuentas sugeridas | Liquidación de excolaboradores: si hay una sola coincidencia, **la asigna sola** | Las cuentas sugeridas desde la plantilla de liquidación o un endpoint. Requiere contrato de API. **Módulo rrh** |
| — | BAJA | ~20 mensajes y logs (BE) y 7 tooltips (FE) | | Solo texto | Que el mensaje muestre la cuenta leída de la plantilla, al tocar cada archivo |

## Reglas para el ejecutor
- **Si falta la línea de plantilla o el parámetro, para.** Sin cuenta de respaldo en el código: detiene
  el proceso con un mensaje claro (patrón de `204a7950`).
- **Semillas:** cada línea nueva la escribe el árbitro (patrón `crd/sql/347`) y el usuario la corre
  **antes** del EAR. Hoy las plantillas de producción ya tienen esas cuentas. La semilla tiene que
  dejar **la misma cuenta que hoy está en el código**, para que el resultado contable no cambie.
- **Un ítem por commit,** con `mvn -q compile`.

## Lo que falta decidir
- **P4 y P10** necesitan diseño propio y van al final.
- **P6:** es un cargador de migración que ya se corrió. ¿Se vuelve a usar? Si no, puede quedarse como
  está, documentado.

## Avance
- **P1+P2** `f3abf5a0`: sin semilla. Verificación `cnt/sql/c20-01`.
- **P3+P5**: hallazgo, ninguno de los dos asientos usaba plantilla.
  - **P3:** usa la plantilla 37, aux1 85, aux2 = tipo de préstamo. El modo arranque (`madre + ".25"`)
    queda sin cambio por ahora. Propuesta: derivar la cuenta de la banda de más días de cada madre
    en `CRD.BNDP`; antes hay que verificarlo en la base.
  - **P5:** plantilla nueva alterno 38, reservada en el registro §2c, aux1 1.
  - **⛔ Semilla `crd/sql/356` ANTES del EAR que los traiga.**
- **P8**: control B17_21_VS_G42 lee el detalle 16 nuevo del rubro 250
  (`CntParametroEstadoFinanciero.CUENTA_APORTES_PARTICIPES`). Semilla `cnt/sql/c20-02` (valor `2.1`).
  Sin ella, el control queda en advertencia. La comparación contra `CRD.CTAP` se descartó: no cubre
  los tipos patronales 3, 13 y 14 que suma la G42.
- **P3, modo arranque**: verificación `crd/sql/358`, de solo lectura. Se decide con su salida.
- **Texto pendiente**: el detalle del control B17_OFICIAL (~477) menciona `2.1`, `1.3` y `1.3.99`;
  va con P4.

## Prerrequisitos de base del próximo EAR (en orden)
1. `crd/sql/356`: semillas de P3 y P5. **Si falta, el EAR rompe el cierre de cartera y las compras con
   redondeo.**
2. `cnt/sql/c20-02` (detalles 16 y 17 del rubro 250). **También es obligatorio desde P4-A:** sin el
   detalle 17, el cierre de cartera se detiene y los controles de cartera quedan en advertencia.
3. `cnt/sql/c20-01`: verificación.
- **P4** (propuesta de omen-saa-1-be; decisiones técnicas del árbitro):
  - **A, raíz `1.3`:** detalle 17 del rubro 250 (`CUENTA_CARTERA_CREDITOS`), semilla agregada al
    `c20-02`. Mismo resultado de hoy. **Se implementa ya.**
  - **B, provisión:** las cuentas de la línea 83 de la plantilla 37. **C, grupo Q/P/H:** el tipo de
    préstamo del producto de cada banda (`BNDP → CBPR → PRDC.TPPRCDGO`). **No se implementan hasta
    que `crd/sql/359` confirme que el resultado no cambia** (consultas 1 y 3 todas IGUAL/EN_PLANTILLA,
    2 y 4 en cero). Si no da, se quedan como están, documentadas.
  - Una cuenta sin grupo derivable: advertencia, no se ajusta (igual que P3).
  - El modo arranque queda fuera (`crd/sql/358`).
- **P4-A** hecho: raíz de cartera desde el detalle 17. Helper único `ParametroContableService` (cnt) para el rubro 250. Con `1.3` cargado, el resultado es idéntico. Quedan escritos hasta P4-B/C: `1.3.99` y los rangos.
- **P4-B** hecho (implementado, pendiente de commit): la provisión sale de las cuentas distintas de las líneas aux1 83 de la plantilla 37 de la empresa (`ParametroContableService.cuentasProvisionCartera`). El ⑨ y los controles excluyen esas cuentas EXACTAS (no el prefijo `1.3.99`); `B17_1399_VS_G48` suma esas mismas cuentas. DAO nuevo `CierreCarteraDaoService.selectSaldoContablePorCuentaExcluyendo(…, List<String>, …)` (la de un prefijo se queda). Sin la plantilla o la línea 83: el cierre se detiene con `IncomeException` y los controles quedan en ADVERTENCIA (`CARTERA_SIN_PROVISION`, `B17_13_VS_G48`). Ya no queda `1.3.99` en cálculos ni mensajes. Los rangos 01-04/05-08/09-12 siguen hasta P4-C.
- **`crd/sql/359` corrido en producción (2026-11-01):**
  - (1) las 51 cuentas de banda dan IGUAL. Aparece `1.3.12.00` en una banda: queda en H, sin
    problema.
  - (2) solo `1.3.07`, saldo 0 con 2 movimientos (una madre usada y reversada).
  - (3) `1.3.99.05/.10/.15` todas EN_PLANTILLA.
  - (4) vacía (confirmado por el usuario): todos los productos con banda tienen tipo de préstamo.
  - **→ P4-B y P4-C aprobados.** Una cuenta sin grupo con saldo 0 se ignora sin advertencia; con
    saldo distinto de 0, advertencia.
- **`crd/sql/358` corrido en producción (2026-11-01):** las 10 madres dan IGUAL, con una sola fila
  y su `.25` existente. `1.3.12` tiene 6 bandas, con `1.3.12.00` como banda 1, y la de más días
  sigue siendo la `.25`. **→ Aprobado P3-arranque:** la cuenta que absorbe en modo arranque es la
  banda de mayor número (`BNDPNMRO`) de cada madre en `CRD.BNDP`. Sin literal y sin semilla.
- **`cnt/sql/c20-01` corrido en producción (2026-11-01):** rubro 250, detalles 8-12 = 5/4/1/2/3; plantilla
  alterno 1, aux1 1-4 = 1.4.05.05 / 1.4.05.10 / 2.3.02.05 / 2.3.02.10. **P1 y P2 sin semilla, confirmado.**
- **`cnt/sql/c20-02` corrido en producción (2026-11-01):** PDTR 1534 (16 = 2.1) y 1535 (17 = 1.3).
