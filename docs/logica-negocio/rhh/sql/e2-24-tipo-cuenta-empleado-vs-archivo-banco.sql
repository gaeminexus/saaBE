-- =====================================================================
-- e2-24 — ¿El tipo de cuenta del empleado significa lo mismo que el del banco?
-- Modulo: RHH / PGS  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ✅ EL BLOQUE 0 ES SOLO LECTURA. El bloque 1 escribe y esta condicionado.
--
-- 🔴 POR QUE EXISTE — LEER ESTO ANTES DE TRANSFERIR NADA
--
--   Hoy se corrigio que el archivo del Banco Internacional mandaba las cuentas de
--   ahorro como corrientes y viceversa: 159 transferencias por $119.472,32 con la
--   institucion equivocada. La causa era que TipoCuentasBancarias tenia invertidos
--   los codigos alternos del rubro 23 (lo correcto: 1 = AHORRO, 2 = CORRIENTE).
--
--   Al hacer que el anticipo tome la cuenta bancaria del empleado (commit 6e646ea)
--   aparecio la MISMA trampa por otra puerta, y hay que medirla antes de usarla:
--
--     PGS.PGTR.PGTRBFTP  -> la entidad dice: "codigoAlterno del DetalleRubro del
--                           rubro de tipo de cuenta bancaria". Los formateadores
--                           (Internacional, Pacifico, plano) lo leen asi, contra
--                           el RUBRO 23.
--     RHH.CBEM.CBEMTPCT  -> la entidad dice: "detalle del rubro
--                           RHH_TIPO_CUENTA_BANCARIA", que es el RUBRO 199
--                           (Rubros.java:202). OTRO catalogo.
--
--   O sea que se estaria copiando un valor del rubro 199 a un campo que el archivo
--   del banco interpreta contra el rubro 23. Si los dos catalogos no numeran igual,
--   la transferencia sale con el tipo de cuenta cambiado. Y NO DA NINGUN ERROR:
--   el archivo se genera, el banco lo recibe, y recien se nota cuando el dinero no
--   llega. Es exactamente lo de esta mañana.
--
--   Peor: ni siquiera esta claro si CBEMTPCT guarda el CODIGO ALTERNO del detalle o
--   su PK. La entidad dice "detalle del rubro" sin precisar cual de los dos.
--
--   Este script mide las dos cosas ANTES de que nadie transfiera.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — MEDICION. Solo lectura. Correr entero y pegar el resultado.
-- =====================================================================

-- 0.1 El catalogo que el ARCHIVO DEL BANCO da por cierto: rubro 23.
--     ESPERADO (ya verificado hoy): alterno 1 = AHORRO, alterno 2 = CORRIENTE.
SELECT '0.1 - rubro 23 (el que usa el archivo)' AS control,
       r.PRBRCDGO AS rubro_pk, d.PDTRCDGO AS detalle_pk,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 23
 ORDER BY d.PDTRALTR;

-- 0.2 El catalogo que usa la CUENTA DEL EMPLEADO: rubro 199.
--     🔴 Compara alterno y descripcion contra el 0.1. Si el 199 numera distinto
--        —por ejemplo 1 = CORRIENTE— copiar el valor tal cual INVIERTE el tipo.
SELECT '0.2 - rubro 199 (el que usa RHH.CBEM)' AS control,
       r.PRBRCDGO AS rubro_pk, d.PDTRCDGO AS detalle_pk,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 199
 ORDER BY d.PDTRALTR;

-- 0.3 🔴 ¿CBEMTPCT guarda el ALTERNO o la PK del detalle? Esto lo decide.
--     Muestra cada valor realmente almacenado y con que fila del rubro 199
--     coincidiria leyendolo de una forma y de la otra.
--     - Si "como_alterno" trae descripcion y "como_pk" viene vacio -> guarda alterno.
--     - Si es al reves -> guarda PK, y copiarlo al pago seria doblemente erroneo.
--     - Si las dos traen algo, mirar cual descripcion tiene sentido para esa cuenta.
SELECT '0.3 - que guarda CBEMTPCT' AS control,
       c.CBEMTPCT AS valor_guardado,
       COUNT(*)   AS cuentas_con_ese_valor,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 199 AND d.PDTRALTR = c.CBEMTPCT)  AS como_alterno,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 199 AND d.PDTRCDGO = c.CBEMTPCT)  AS como_pk
  FROM RHH.CBEM c
 GROUP BY c.CBEMTPCT
 ORDER BY c.CBEMTPCT;

-- 0.4 Lo mismo para lo que ya esta guardado en los pagos, como contraste.
--     Estos SI son alternos del rubro 23 por contrato de la entidad.
SELECT '0.4 - que hay hoy en PGTRBFTP' AS control,
       p.PGTRBFTP AS valor_guardado, COUNT(*) AS pagos,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 23 AND d.PDTRALTR = p.PGTRBFTP) AS significado
  FROM PGS.PGTR p
 WHERE p.PGTRBFTP IS NOT NULL
 GROUP BY p.PGTRBFTP
 ORDER BY p.PGTRBFTP;

-- 0.5 La cuenta del empleado del anticipo 1, para ver el caso concreto.
SELECT '0.5 - cuentas del empleado del anticipo 1' AS control,
       c.CBEMCDGO, c.MPLDCDGO AS empleado, c.BEXTCDGO AS banco,
       c.CBEMTPCT AS tipo_cuenta, c.CBEMNMCT AS numero,
       c.CBEMPRCP AS principal, c.CBEMESTD AS estado
  FROM RHH.CBEM c
 WHERE c.MPLDCDGO = (SELECT a.MPLDCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1)
 ORDER BY c.CBEMCDGO;


-- =====================================================================
-- BLOQUE 1 — ⛔ NO CORRER TODAVIA
-- =====================================================================
-- Este bloque rellenaria la cuenta de destino del pago 322 desde la cuenta del
-- empleado, para no tener que anular y recrear el anticipo.
--
-- 🔴 Esta deliberadamente SIN ESCRIBIR. Hasta saber que devuelve el BLOQUE 0 no se
--    sabe si CBEMTPCT se puede copiar tal cual a PGTRBFTP, o si hay que traducirlo
--    entre los dos catalogos. Escribirlo antes de medir seria repetir el error de
--    esta mañana con otro disfraz.
--
-- Pegame el resultado del BLOQUE 0 y lo completo con la traduccion correcta.
--
-- Recordatorio: rellenar el dato NO alcanza por si solo. El control que hoy esta
-- desplegado rechaza por ORIGEN, no por datos; hace falta el WAR con el commit
-- 6e646ea para que mire la cuenta de verdad.
-- =====================================================================
