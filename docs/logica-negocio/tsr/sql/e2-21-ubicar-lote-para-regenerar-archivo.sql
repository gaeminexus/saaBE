-- =====================================================================
-- e2-21 — Ubicar un lote ya generado para volver a bajar su archivo
-- Modulo: PGS/TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚪ SOLO LECTURA. No modifica ningun estado. URGENTE.
--
-- ⛔ LO PRIMERO, PORQUE AHORRA EL TRABAJO: NO HACE FALTA HABILITAR NADA.
--
--   El archivo del banco NO se guarda. `obtenerArchivoLote`
--   (PagoProgramadoServiceImpl) lo REGENERA desde el formateador en cada
--   llamada: busca el lote, busca sus pagos y vuelve a formatear.
--
--       GET /SaaBE/rest/pgtr/lote/{idLote}/archivo
--
--   Asi que con el WAR corregido (commit 5e344ae, TipoCuentasBancarias), volver
--   a pedir el MISMO lote ya devuelve el archivo con el tipo de cuenta bien.
--   No hay que reabrir el lote, ni revertir pagos, ni cambiar LTPGESTD, ni
--   volver a aprobar nada. Cualquier script que tocara estados aca seria
--   trabajo de mas y riesgo de mas.
--
--   Lo unico que falta es el idLote, porque todavia no hay pantalla de consulta
--   de lotes generados. Eso es lo que resuelve este script.
--
-- Columnas verificadas contra las entidades LotePago.java y PagoProgramado.java:
--   PGS.LTPG: LTPGCDGO · LTPGPJRQ · LTPGCNBC · LTPGFGNR · LTPGNMAR · LTPGPATH
--             LTPGVLTT · LTPGNPAG · LTPGESTD · LTPGOBSR · LTPGUSAR · LTPGFCRG
--   PGS.PGTR.PGTRLTPG = FK al lote
-- =====================================================================


-- =====================================================================
-- BLOQUE 1 -- ⭐ LOS ULTIMOS LOTES GENERADOS. De aca sale el idLote.
-- =====================================================================
SELECT * FROM (
  SELECT l.LTPGCDGO                AS id_lote,
         l.LTPGFGNR                AS fecha_generacion,
         l.LTPGNMAR                AS nombre_archivo,
         l.LTPGNPAG                AS numero_pagos,
         l.LTPGVLTT                AS valor_total,
         l.LTPGESTD                AS estado_lote,
         c.CNBCNMRO                AS cuenta_origen,
         b.BNCONMBR                AS banco_origen
    FROM PGS.LTPG l
    LEFT JOIN TSR.CNBC c ON c.CNBCCDGO = l.LTPGCNBC
    LEFT JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
   ORDER BY l.LTPGFGNR DESC, l.LTPGCDGO DESC
) WHERE ROWNUM <= 20;


-- =====================================================================
-- BLOQUE 2 -- ANTES DE MANDARLO AL BANCO: que tipo de cuenta va a salir ahora
-- =====================================================================
-- ⚠️ Reemplazar :ID_LOTE por el id que salio del bloque 1.
--
-- Con la correccion, el formateador mapea:
--     tipo 1 (AHORRO)    -> "AHO" en el Internacional, "10" en el Pacifico
--     tipo 2 (CORRIENTE) -> "CTE" en el Internacional, "00" en el Pacifico
--
-- Esta consulta muestra, pago por pago, que tipo tiene guardado y que le va a
-- salir. Contrastar A MANO contra lo que se sabe de esas cuentas ANTES de subir
-- el archivo: si alguna sigue apareciendo al reves, NO es la constante — es el
-- dato de esa cuenta, y hay que corregirlo en la pantalla de la entidad.

SELECT p.PGTRCDGO                                        AS id_pago,
       NVL(t.PRSNNMBR, p.PGTRBNNM)                        AS beneficiario,
       NVL(cd.CTBNTPCT, p.PGTRTPCT)                       AS tipo_guardado,
       CASE NVL(cd.CTBNTPCT, p.PGTRTPCT)
            WHEN 1 THEN 'AHORRO   -> AHO / 10'
            WHEN 2 THEN 'CORRIENTE-> CTE / 00'
            ELSE '*** SIN TIPO — el archivo aborta ***'
       END                                                AS saldra_como,
       p.PGTRVLOR                                         AS valor
  FROM PGS.PGTR p
  LEFT JOIN TSR.CTBN cd ON cd.CTBNCDGO = p.PGTRCTBN
  LEFT JOIN TSR.TTLR t  ON t.TTLRCDGO  = p.PGTRTTLR
 WHERE p.PGTRLTPG = :ID_LOTE
 ORDER BY p.PGTRCDGO;


-- =====================================================================
-- COMO SE VUELVE A BAJAR EL ARCHIVO, mientras no exista la pantalla
-- =====================================================================
--   1. Desplegar el WAR con el commit 5e344ae.
--   2. Tomar el id_lote del bloque 1.
--   3. Llamar, autenticado, a:
--          GET /SaaBE/rest/pgtr/lote/{idLote}/archivo
--      La respuesta trae `contenidoBase64` (siempre) y `nombreArchivo`.
--      Decodificar el base64 a bytes y guardarlo con ese nombre.
--      ⚠️ El texto del Internacional es ANSI (windows-1252): guardar los BYTES
--         del base64, no re-codificar el texto como UTF-8, o se rompen las
--         tildes y las ñ de los nombres.
--   4. Contrastar el campo 6 (tipo de cuenta) contra el bloque 2 antes de
--      subirlo al banco.
--
-- El endpoint NO cambia estados ni regenera el lote: solo vuelve a formatear.
--
-- FIN — no se modifico nada.
-- =====================================================================
