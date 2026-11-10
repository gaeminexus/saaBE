# Ajuste de firmas: solicitud de crédito y pagaré (equipo omen-saa-3, 2026-11-10)

Pedido del usuario (textual): *«en la solicitud de créditos, cuando se trate de préstamos quirografarios o
emergentes, no incluir la firma del cónyuge, y en la sección donde firma el personal de ASOPREP, no poner los
nombres de usuarios que aprueba o ingresa, solo los textos para que se realice firma física»*.

Los dos reportes salen juntos en el PDF del otorgamiento (`rprt/generarLote`: solicitud + pagaré + tabla). Son de
`omen-saa-1` (commits `b9f3602c`, `5966881a`, `56f6df22`, `4054b858`, `bebb1fc4`); este ajuste es puntual y no cambia
nada más de su diseño (`DISENO-IMPRESOS-OTORGAMIENTO.md`, `DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md`).

## A1 — Sin cónyuge en quirografarios y emergentes (`rep/crd/RPRT_PRST_PGRE.jrxml`)

- La solicitud (`RPRT_PRST_SLCT`) **no** tiene firma de cónyuge; la única está en el **pagaré** (banda de firmas,
  «FIRMA DEL CONYUGE», controlada por `V_HAY_CONYUGE`). Ahí se aplica.
- Criterio: **código Petro del producto**, `CRD.PRDC.PRDCCDPT IN ('PQ','PE')` — el mismo que usa la regla del cupo
  (`EvaluacionOtorgamientoServiceImpl`), no el nombre del producto ni `TPPRCDGO`. El query expone
  `CASE WHEN TRIM(pr.PRDCCDPT) IN ('PQ','PE') THEN 1 ELSE 0 END AS SIN_CONYUGE`.
- `V_HAY_CONYUGE` pasa a ser falso cuando `SIN_CONYUGE = 1`. Eso quita **la firma y también la mención del cónyuge
  en el texto legal** (`V_CONYUGE_TXT`, «…, y {cónyuge}, cédula…»): un pagaré que nombra al cónyuge como obligado
  pero no lleva su firma queda incoherente. El resto del texto legal no cambia.
- Hipotecarios, prendarios y demás productos: sin cambio (con cónyuge registrado, sale como hoy).

## A2 — Firmas del personal de ASOPREP sin nombres de usuario (`rep/crd/RPRT_PRST_SLCT.jrxml`)

- Última banda del `detail` (sección 7): se **quitan** los tres `textField` con `USUARIO_ELABORA`, `REVISADO_POR` y
  `APROBADOR`. Quedan las tres líneas de firma y los textos «Elaborado por — Asistente de Crédito», «Revisado por —
  Jefe de Crédito» y «Aprobado por — Gerencia General / CAD», en la misma posición, para firma física.
- Se retiran también los tres `<field>` y sus columnas del `<query>` (la subconsulta de `REVISADO_POR` incluida):
  no se imprimen en ningún otro lugar.
- No cambian: la firma del partícipe (sección 5, lleva su nombre y cédula, no es personal de ASOPREP), «Nivel de
  aprobación», fechas, montos ni el pie «Impreso por» (`P_USUARIO`, IM15).

## Verificación

`compilar-jasper.bat` + `verificar-fill-jasper.bat` de los dos, y un PDF de muestra mirado (no basta el fill).
Sin endpoint ni FE nuevos.
