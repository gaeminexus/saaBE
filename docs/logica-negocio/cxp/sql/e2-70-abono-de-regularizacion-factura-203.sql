-- =====================================================================
-- e2-70 — El saldo de 11.975,04 de la factura 203 deja de figurar como
--          "por pagar", SIN tocar la contabilidad de junio
-- Modulo: cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-22
--
-- ⚠️ LOS BLOQUES 0 y 1 SON SOLO LECTURA. El BLOQUE 1 PUEDE MANDAR A PARAR.
--    El BLOQUE 2 ESCRIBE y esta COMENTADO. Su COMMIT tambien: sin el, el
--    INSERT no queda guardado y no deja ningun rastro de haber faltado.
--
-- EL CASO — continuacion del e2-69. El usuario ya corrio su BLOQUE 3 y la
--   factura 243 (001-501-000000203) figura como "Pagada", pero el Estado
--   de Cuenta del proveedor CUSTODIA & MAXIMA SEGURIDAD sigue mostrando
--   "SALDO POR PAGAR 11.975,04 · 1 factura(s) con saldo".
--
-- POR QUE, medido: el saldo de una factura NO sale de la marca FCTCEPAG.
--   Lo calcula el backend como total - aplicado, desde las aplicaciones
--   reales (`/aplp/saldo/{id}`), y el Estado de Cuenta acumula ese numero
--   (estado-cuenta-titular.component.ts:136). El unico abono de esta
--   factura son los 320,76 de la retencion, asi que 12.295,80 - 320,76 =
--   11.975,04. La marca sirvio para sacarla del combo de Solicitud de
--   Pago, que era otro filtro (ese si mira FCTCEPAG), y nada mas.
--
-- DECISIONES DEL USUARIO, 2026-09-22, con los numeros a la vista:
--   1. La contabilidad de junio/julio **se conserva**: no se anula la
--      factura ni su asiento (CXP-2026-06-0002), ni el de la retencion.
--      Por eso NO se usa `anularFacturaCompra`, que anularia el asiento.
--   2. Se registra el **abono que falta**, por 11.975,04, **sin asiento**:
--      el valor ya fue pagado y su contabilidad ya esta hecha en junio.
--      Con eso la factura cuadra: 320,76 (retencion) + 11.975,04 = 12.295,80
--      y el saldo queda en 0,00 en TODAS las pantallas, no solo en una.
--
-- ⛔ QUE ES ESTE ABONO Y QUE NO ES: es un asiento de REGULARIZACION DE
--    DATOS de una factura del periodo de pruebas (el usuario: "recien
--    desde agosto se generaron las facturas reales"). NO representa un
--    movimiento de dinero nuevo y NO genera contabilidad. Queda dicho en
--    la observacion de la propia fila para que dentro de seis meses nadie
--    lo confunda con un pago real.
--
-- Columnas verificadas contra la entidad AplicacionPagoCxp:
--   PGS.APLP -> APLPCDGO (PK, secuencia SQ_APLPCDGO), APLPPJRQ (empresa),
--               APLPFCTC (factura), APLPTDPG (tipo), APLPMAPL (monto),
--               APLPFAPL (fecha aplicacion), APLPOBSR (observacion),
--               APLPESTD (estado), APLPUSAR (usuario), APLPASNT (asiento),
--               APLPFCRG (fecha registro)
--   Tipos (com.saa.rubros.TipoDocPagoAplicacion): 1 COBRO_DIRECTO,
--               2 NOTA_CREDITO, 3 RETENCION, 4 ANTICIPO, 5 NOTA_DEBITO,
--               6 CAJA_CHICA
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — CONTROL PREVIO. Solo lectura. ANOTAR ESTA SALIDA.
-- ESPERADO: total 12.295,80 · aplicado 320,76 · saldo 11.975,04, y UNA
--           sola aplicacion activa (la 35, la retencion).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - antes' AS bloque,
       f.ID, f.NUMERO, NVL(f.TOTAL,0) AS total, f.FCTCEPAG AS estado_pago,
       NVL(( SELECT SUM(NVL(ap.APLPMAPL,0)) FROM PGS.APLP ap
              WHERE ap.APLPFCTC = f.ID AND NVL(ap.APLPESTD,1) = 1 ),0) AS aplicado,
       NVL(f.TOTAL,0)
         - NVL(( SELECT SUM(NVL(ap.APLPMAPL,0)) FROM PGS.APLP ap
                  WHERE ap.APLPFCTC = f.ID AND NVL(ap.APLPESTD,1) = 1 ),0) AS saldo
  FROM PGS.FCTC f
 WHERE f.ID = 243 AND f.NUMERO = '001-501-000000203';


-- ---------------------------------------------------------------------
-- BLOQUE 1 — ⛔ VERIFICACION DE ESTRUCTURA. Solo lectura, y PUEDE MANDAR
--            A PARAR. Contesta si la base acepta una aplicacion SIN
--            asiento, que es toda la premisa de este script.
--
-- COMO LEERLO:
--   · Fila de APLPASNT con NULLABLE = 'Y'  -> se puede. Seguir.
--   · Fila de APLPASNT con NULLABLE = 'N'  -> ⛔ PARAR. La base exige
--     asiento en toda aplicacion, asi que este camino no existe: avisar
--     al arbitro, que hay que resolverlo de otra forma (y la otra forma
--     implica decidir de nuevo, porque las dos que quedan tocan
--     contabilidad o cambian la pantalla).
--   · Cualquier OTRA columna con NULLABLE = 'N' que el INSERT del BLOQUE 2
--     no llene -> ⛔ PARAR y pasarle la lista al arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - columnas obligatorias de PGS.APLP' AS bloque,
       c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS' AND c.TABLE_NAME = 'APLP'
   AND ( c.NULLABLE = 'N' OR c.COLUMN_NAME = 'APLPASNT' )
 ORDER BY CASE WHEN c.COLUMN_NAME = 'APLPASNT' THEN 0 ELSE 1 END, c.COLUMN_ID;


-- BLOQUE 1b — La secuencia del PK tiene que existir y estar por encima
-- del maximo de la tabla. ESPERADO: SIGUIENTE > MAXIMO_EN_TABLA.
-- Si no existe la secuencia, PARAR: el INSERT del BLOQUE 2 la usa.
SELECT 'BLOQUE 1b - secuencia' AS bloque,
       ( SELECT COUNT(*) FROM ALL_SEQUENCES s
          WHERE s.SEQUENCE_OWNER = 'PGS' AND s.SEQUENCE_NAME = 'SQ_APLPCDGO' ) AS existe_secuencia,
       ( SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
          WHERE s.SEQUENCE_OWNER = 'PGS' AND s.SEQUENCE_NAME = 'SQ_APLPCDGO' ) AS siguiente,
       ( SELECT MAX(APLPCDGO) FROM PGS.APLP ) AS maximo_en_tabla
  FROM DUAL;


-- =====================================================================
-- BLOQUE 2 — EL ABONO DE REGULARIZACION. ⛔ COMENTADO.
-- Solo despues de que el BLOQUE 1 diga que APLPASNT admite NULL.
--
-- La empresa y el usuario NO se inventan: se copian de la aplicacion que
-- esa misma factura YA tiene (la 35, la de la retencion). Es la forma mas
-- segura de no equivocar una FK — el dato correcto ya esta en la tabla.
--
-- El tipo es 1 (COBRO_DIRECTO), que es el unico que representa "el valor
-- se pago". ⚠️ A diferencia de un COBRO_DIRECTO normal, este NO tiene un
-- PagoProgramado detras; el unico camino que lo busca es la anulacion en
-- cascada de la factura, que tiene su propio respaldo para cuando no lo
-- encuentra (FacturaCompraServiceImpl:~190-200). Queda anotado.
--
-- ESPERADO: "1 fila creada".
--
-- INSERT INTO PGS.APLP (
--     APLPCDGO, APLPPJRQ, APLPFCTC, APLPTDPG, APLPMAPL,
--     APLPFAPL, APLPESTD, APLPUSAR, APLPASNT, APLPOBSR, APLPFCRG )
-- SELECT PGS.SQ_APLPCDGO.NEXTVAL,
--        a.APLPPJRQ,                 -- empresa, copiada de la aplicacion 35
--        243,                        -- la factura
--        1,                          -- COBRO_DIRECTO
--        11975.04,                   -- el saldo exacto del BLOQUE 0
--        DATE '2026-09-22',          -- fecha de la regularizacion, no de junio
--        1,                          -- activa
--        a.APLPUSAR,                 -- usuario, copiado de la aplicacion 35
--        NULL,                       -- ⛔ SIN ASIENTO: no genera contabilidad
--        'Regularizacion de datos (e2-70, 2026-09-22): factura del periodo '
--        || 'de pruebas. El valor ya fue pagado y su contabilidad quedo en '
--        || 'junio (asiento CXP-2026-06-0002). Este abono NO representa un '
--        || 'movimiento de dinero nuevo y NO genera asiento.',
--        SYSTIMESTAMP
--   FROM PGS.APLP a
--  WHERE a.APLPCDGO = 35;
--
-- -- Control posterior, ANTES del COMMIT: volver a correr el BLOQUE 0.
-- -- ESPERADO: aplicado 12.295,80 · **saldo 0** · estado_pago 3.
-- --   Si el saldo no da 0 exacto, NO hacer COMMIT y avisar al arbitro.
--
-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA.
-- COMMIT;
--
-- DESPUES DEL COMMIT, mirar el Estado de Cuenta del proveedor:
--   · "SALDO POR PAGAR" debe decir 0,00 y "0 factura(s) con saldo".
--   · "ABONADO" sube 11.975,04.
--   · La factura 203 muestra saldo 0,00 y sigue "Pagada".
--   · En sus abonos aparecen DOS: la retencion de 320,76 con su asiento, y
--     este de 11.975,04 SIN asiento — y esta bien que se vea asi: dice la
--     verdad, que no genero contabilidad.
--
-- REVERSO (comentado): borra el abono. Reemplazar <<ID>> por el APLPCDGO
-- que haya quedado (se lo ve en el control posterior).
--   DELETE FROM PGS.APLP WHERE APLPCDGO = <<ID>> AND APLPASNT IS NULL AND APLPFCTC = 243;
--   COMMIT;
-- =====================================================================
