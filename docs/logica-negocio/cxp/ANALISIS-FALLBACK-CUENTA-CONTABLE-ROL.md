# Análisis: titulares afectados por el fallback de cuenta contable sin filtro de rol

**Fecha:** 2026-08-28. **Motivo:** antes de estricter el chequeo de cuenta contable por rol
(factura de venta, carga automática de CxP, liquidaciones de compra emitidas — ver
`AsientoContableService.existeCuentaConRolEstricto`), hay que saber a quién bloquea el primer
día en producción y parametrizarlo antes.

## 1. El problema, medido

`PersonaCuentaContableDaoServiceImpl.selectByTitularRolTipoCuenta` cae a un fallback "sin
filtro de rol" cuando no encuentra una fila `TSR.PRCC` con el rol exacto pedido (`TSR.PRRL
.PRRLRZZA` = 1 Cliente / 2 Proveedor). Contado contra la base local (copia de producción):

- **87** titulares tienen al menos una cuenta contable (`PRCCTPOO=1`, Facturas) configurada.
- **61** la tienen **sólo** bajo rol Proveedor (2).
- **24** la tienen **sólo** bajo rol Cliente (1).
- **2** la tienen bajo **ambos** roles.

Es decir: **85 de 87** titulares con cuenta configurada disparan el fallback en cuanto se
usan en el rol contrario al que tienen tagueado — y el fallback les da silenciosamente la
cuenta equivocada en vez de fallar.

```sql
-- Verificación de los conteos de arriba (ejecutar y comparar contra 87/61/24/2)
SELECT
    COUNT(*)                                                              AS total_titulares,
    SUM(CASE WHEN solo_proveedor = 1 THEN 1 ELSE 0 END)                   AS solo_proveedor,
    SUM(CASE WHEN solo_cliente   = 1 THEN 1 ELSE 0 END)                   AS solo_cliente,
    SUM(CASE WHEN tiene_proveedor = 1 AND tiene_cliente = 1 THEN 1 ELSE 0 END) AS ambos
FROM (
    SELECT
        pr.PRSNCDGO                                                      AS titular,
        MAX(CASE WHEN pr.PRRLRZZA = 2 THEN 1 ELSE 0 END)                  AS tiene_proveedor,
        MAX(CASE WHEN pr.PRRLRZZA = 1 THEN 1 ELSE 0 END)                  AS tiene_cliente,
        CASE WHEN MAX(CASE WHEN pr.PRRLRZZA = 2 THEN 1 ELSE 0 END) = 1
              AND MAX(CASE WHEN pr.PRRLRZZA = 1 THEN 1 ELSE 0 END) = 0
             THEN 1 ELSE 0 END                                           AS solo_proveedor,
        CASE WHEN MAX(CASE WHEN pr.PRRLRZZA = 1 THEN 1 ELSE 0 END) = 1
              AND MAX(CASE WHEN pr.PRRLRZZA = 2 THEN 1 ELSE 0 END) = 0
             THEN 1 ELSE 0 END                                           AS solo_cliente
    FROM TSR.PRCC pcc
    JOIN TSR.PRRL pr ON pr.PRRLCDGO = pcc.PRRLCDGO
    WHERE pcc.PRCCTPOO = 1          -- tipoCuenta = Facturas
      AND pcc.PLNNCDGO IS NOT NULL  -- con cuenta contable real asignada
    GROUP BY pr.PRSNCDGO
) resumen;
```

## 2. Detalle: titulares que sólo tienen rol Proveedor

Si alguno de estos alguna vez se factura como **cliente** (factura de venta), el chequeo
estricto lo bloqueará donde antes el fallback lo dejaba pasar con la cuenta de proveedor.

```sql
SELECT
    t.TTLRCDGO                                            AS titular,
    t.TTLRNMBR                                            AS nombre,
    t.TTLRIDNT                                            AS identificacion,
    pcc.PJRQCDGO                                          AS empresa,
    pc.PLNNCTBL                                           AS cuenta_proveedor_actual
FROM TSR.PRCC pcc
JOIN TSR.PRRL pr  ON pr.PRRLCDGO = pcc.PRRLCDGO
JOIN TSR.TTLR t   ON t.TTLRCDGO  = pr.PRSNCDGO
JOIN CNT.PLNN pc  ON pc.PLNNCDGO = pcc.PLNNCDGO
WHERE pcc.PRCCTPOO = 1
  AND pr.PRRLRZZA = 2   -- Proveedor
  AND NOT EXISTS (
        SELECT 1 FROM TSR.PRCC pcc2
        JOIN TSR.PRRL pr2 ON pr2.PRRLCDGO = pcc2.PRRLCDGO
        WHERE pr2.PRSNCDGO = t.TTLRCDGO
          AND pcc2.PRCCTPOO = 1
          AND pr2.PRRLRZZA = 1   -- no tiene también Cliente
      )
ORDER BY t.TTLRNMBR;
```

## 3. Detalle: titulares que sólo tienen rol Cliente

Estos son los que **hoy mismo** quedan expuestos por el cambio: la carga automática de CxP y
la emisión de liquidaciones de compra los usan como **proveedor**. Si alguno de estos 24
aparece como proveedor en un documento (factura de compra recibida, liquidación de compra
emitida), el chequeo estricto lo bloquea donde antes el fallback usaba su cuenta de cliente.
**Este es el listado prioritario para parametrizar antes de desplegar.**

```sql
SELECT
    t.TTLRCDGO                                            AS titular,
    t.TTLRNMBR                                            AS nombre,
    t.TTLRIDNT                                            AS identificacion,
    pcc.PJRQCDGO                                          AS empresa,
    pc.PLNNCTBL                                           AS cuenta_cliente_actual
FROM TSR.PRCC pcc
JOIN TSR.PRRL pr  ON pr.PRRLCDGO = pcc.PRRLCDGO
JOIN TSR.TTLR t   ON t.TTLRCDGO  = pr.PRSNCDGO
JOIN CNT.PLNN pc  ON pc.PLNNCDGO = pcc.PLNNCDGO
WHERE pcc.PRCCTPOO = 1
  AND pr.PRRLRZZA = 1   -- Cliente
  AND NOT EXISTS (
        SELECT 1 FROM TSR.PRCC pcc2
        JOIN TSR.PRRL pr2 ON pr2.PRRLCDGO = pcc2.PRRLCDGO
        WHERE pr2.PRSNCDGO = t.TTLRCDGO
          AND pcc2.PRCCTPOO = 1
          AND pr2.PRRLRZZA = 2   -- no tiene también Proveedor
      )
ORDER BY t.TTLRNMBR;
```

## 4. El listado que de verdad importa: quién está **en riesgo real** hoy

Las dos consultas de arriba listan una condición de configuración, no de uso: un titular
"sólo Cliente" que nunca se usa como proveedor no bloquea nada. Este cruce filtra a los que
**sí aparecen** como proveedor/prestador en documentos de compra pero no tienen el rol
Proveedor tagueado — son los que van a fallar la próxima vez que se cargue o emita algo con
ellos.

```sql
-- Titulares usados como PROVEEDOR/PRESTADOR en documentos de compra,
-- sin cuenta contable bajo rol Proveedor (tipoCuenta=1).
WITH titulares_usados_como_proveedor AS (
    SELECT DISTINCT TITULAR AS titular FROM PGS.FCTC WHERE TITULAR IS NOT NULL
    UNION
    SELECT DISTINCT TITULAR FROM PGS.NTCC WHERE TITULAR IS NOT NULL
    UNION
    SELECT DISTINCT TITULAR FROM PGS.NTDC WHERE TITULAR IS NOT NULL
    UNION
    SELECT DISTINCT TITULAR FROM PGS.LQCC WHERE TITULAR IS NOT NULL
    UNION
    SELECT DISTINCT TITULAR FROM CBR.LQCS WHERE TITULAR IS NOT NULL  -- liquidación emitida
)
SELECT
    t.TTLRCDGO       AS titular,
    t.TTLRNMBR       AS nombre,
    t.TTLRIDNT       AS identificacion
FROM titulares_usados_como_proveedor u
JOIN TSR.TTLR t ON t.TTLRCDGO = u.titular
WHERE NOT EXISTS (
    SELECT 1 FROM TSR.PRCC pcc
    JOIN TSR.PRRL pr ON pr.PRRLCDGO = pcc.PRRLCDGO
    WHERE pr.PRSNCDGO = t.TTLRCDGO
      AND pcc.PRCCTPOO = 1
      AND pr.PRRLRZZA = 2
      AND pcc.PLNNCDGO IS NOT NULL
)
ORDER BY t.TTLRNMBR;
```

```sql
-- Titulares usados como CLIENTE en facturas de venta,
-- sin cuenta contable bajo rol Cliente (tipoCuenta=1).
SELECT DISTINCT
    t.TTLRCDGO       AS titular,
    t.TTLRNMBR       AS nombre,
    t.TTLRIDNT       AS identificacion
FROM CBR.FCTR f
JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
WHERE NOT EXISTS (
    SELECT 1 FROM TSR.PRCC pcc
    JOIN TSR.PRRL pr ON pr.PRRLCDGO = pcc.PRRLCDGO
    WHERE pr.PRSNCDGO = t.TTLRCDGO
      AND pcc.PRCCTPOO = 1
      AND pr.PRRLRZZA = 1
      AND pcc.PLNNCDGO IS NOT NULL
)
ORDER BY t.TTLRNMBR;
```

> Nombres de columna de `CNT.PLNN` (`PLNNCDGO`, `PLNNCTBL`) y de las tablas `PGS.FCTC/NTCC
> /NTDC/LQCC`, `CBR.FCTR/LQCS`, `TSR.PRCC/PRRL/TTLR` verificados contra las anotaciones JPA
> de las entidades correspondientes en este mismo cambio.

## 5. Cómo corregir un titular bloqueado

No hace falta una fila `TSR.PRCC` nueva si el titular ya tiene la cuenta contable correcta
guardada bajo el rol equivocado: alcanza con corregir `TSR.PRRL.PRRLRZZA` (1=Cliente,
2=Proveedor) de la fila `PersonaRol` que agrupa esa `PersonaCuentaContable`, **si** la cuenta
contable en sí es la misma que corresponde al otro rol. Si el titular necesita cuentas
**distintas** para cliente y proveedor (lo normal cuando es ambos), hay que crear la fila
`TSR.PRCC` que falta bajo el rol correcto, apuntando a la cuenta contable real de ese rol —
no reetiquetar la existente, porque dejaría sin cuenta al rol original.

## 6. Dónde vive el chequeo estricto nuevo

`AsientoContableService.existeCuentaConRolEstricto(codigoTitular, idEmpresa, tipoCuenta,
rolPersona)` — público, sin fallback, usado por:

- `AsientoContableServiceImpl.validarCuentasContables` (Factura de venta, PASO 0)
- `AsientoContableServiceImpl.validarCuentasContablesLiquidacion` (Liquidación de compra emitida, PASO 0)
- `ProcesoCargaDocumentosServiceImpl.verificarCuentaContableProveedor` (carga automática de CxP — no aborta el lote, sólo bloquea el documento individual con `PROVEEDOR_SIN_CUENTA`)

`PersonaCuentaContableDaoServiceImpl.selectByTitularRolTipoCuenta` **no se tocó**: su
fallback sigue activo para cualquier otro llamador que no pase por estos tres puntos.
