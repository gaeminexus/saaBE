-- =====================================================================
-- e2-24 — Unificar el tipo de cuenta bancaria: RHH usa OTRO rubro que TSR
-- Modulo: RHH / TSR / PGS  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- 🔴 POR QUE EXISTE — LEER ANTES DE TRANSFERIR NADA
--
--   Pedido del usuario: «que el tipo de cuenta bancaria del empleado lea del mismo
--   rubro que la de participes y que sea la misma que la de generar el archivo de
--   tsr».
--
--   Verificado en el codigo: HOY NO ES EL MISMO. Hay dos catalogos paralelos.
--
--     RUBRO 23  (com.saa.rubros.TipoCuentasBancarias)
--       TSR.CTBN.CTBNTPCT   cuenta bancaria del titular / participe
--       PGS.PGTR.PGTRBFTP   beneficiario ocasional del pago
--       -> es el que leen InternacionalArchivoPagoFormateador:196-199 y
--          PacificoArchivoPagoFormateador:135-138, y el que se corrigio hoy:
--          alterno 1 = AHORRO, alterno 2 = CORRIENTE.
--
--     RUBRO 199 (Rubros.RHH_TIPO_CUENTA_BANCARIA, Rubros.java:202)
--       RHH.CBEM.CBEMTPCT   cuenta bancaria del empleado
--       RHH.DRPG.DRPGTPCT   snapshot del tipo de cuenta en la orden de pago
--       RHH.FMBN.FMBNMPTC   mapa "alternoRubro199=codigoBanco;..." de los formatos
--                           de archivo de nomina
--       -> y el frontend lo confirma: RubrosRrh.TIPO_CUENTA_BANCARIA = 199.
--
--   Ya hay un puente entre los dos, y es el peligroso: el commit 6e646ea hace que
--   el anticipo copie CBEMTPCT (rubro 199) a PGTRBFTP (rubro 23) TAL CUAL. Si los
--   dos catalogos no numeran igual, la transferencia sale con el tipo de cuenta
--   cambiado. Es LITERALMENTE el defecto de esta mañana —159 transferencias por
--   $119.472,32 con la institucion equivocada— por otra puerta, y con la misma
--   propiedad: no da ningun error, el archivo se genera y el banco lo acepta.
--
--   🔴 Y unificar NO es cambiar un numero. RHH.FMBN.FMBNMPTC guarda un mapa CON LAS
--      CLAVES DEL RUBRO 199 (GeneracionOrdenPagoServiceImpl:502,590). Si se cambia
--      el catalogo sin re-clavar esos mapas, el ARCHIVO DE NOMINA empieza a salir
--      mal, tambien en silencio.
--
--   Este script mide todo el alcance antes de tocar nada.
-- =====================================================================


-- =====================================================================
-- 1 — Los dos catalogos, lado a lado
-- =====================================================================

-- 1.1 Rubro 23: el que manda, porque es el que lee el archivo del banco.
--     ESPERADO (verificado hoy): alterno 1 = AHORRO, alterno 2 = CORRIENTE.
SELECT '1.1 - rubro 23 (TSR, archivo banco)' AS control,
       r.PRBRCDGO AS rubro_pk, d.PDTRCDGO AS detalle_pk,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion, d.PDTRESTD AS estado
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 23
 ORDER BY d.PDTRALTR;

-- 1.2 Rubro 199: el que usa RHH.
--     🔴 Compara alterno + descripcion contra el 1.1. Si el 199 numera al reves
--        —por ejemplo 1 = CORRIENTE— copiar el valor tal cual INVIERTE el tipo.
--        Si numeran IGUAL, la unificacion es solo de codigo y no hay migracion de
--        datos, que seria el mejor escenario posible.
SELECT '1.2 - rubro 199 (RHH)' AS control,
       r.PRBRCDGO AS rubro_pk, d.PDTRCDGO AS detalle_pk,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion, d.PDTRESTD AS estado
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 199
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- 2 — ¿Que guardan realmente las columnas? ¿Alterno o PK?
-- =====================================================================
-- Las entidades de RHH dicen "detalle del rubro" sin precisar cual de los dos.
-- Esa misma ambiguedad ya costo hoy una consulta equivocada (el rubro con PK 199
-- es "ESTADO DEL DESCUENTO RECURRENTE", mientras el de tipo de cuenta tiene PK 200
-- y alterno 199).
--
-- Lectura de cada bloque:
--   - si "como_alterno" trae descripcion y "como_pk" viene vacio -> guarda ALTERNO
--   - si es al reves -> guarda PK
--   - si las dos traen algo, mirar cual descripcion tiene sentido

-- 2.1 La cuenta del empleado.
SELECT '2.1 - RHH.CBEM.CBEMTPCT' AS control,
       c.CBEMTPCT AS valor_guardado, COUNT(*) AS cuentas,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 199 AND d.PDTRALTR = c.CBEMTPCT) AS como_alterno_199,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 199 AND d.PDTRCDGO = c.CBEMTPCT) AS como_pk_199,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 23  AND d.PDTRALTR = c.CBEMTPCT) AS si_fuera_alterno_23
  FROM RHH.CBEM c
 GROUP BY c.CBEMTPCT
 ORDER BY c.CBEMTPCT;

-- 2.2 La cuenta del titular / participe, que es la referencia correcta.
SELECT '2.2 - TSR.CTBN.CTBNTPCT' AS control,
       t.CTBNTPCT AS valor_guardado, COUNT(*) AS cuentas,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 23 AND d.PDTRALTR = t.CTBNTPCT) AS como_alterno_23
  FROM TSR.CTBN t
 WHERE t.CTBNTPCT IS NOT NULL
 GROUP BY t.CTBNTPCT
 ORDER BY t.CTBNTPCT;

-- 2.3 Lo que ya esta guardado en los pagos.
SELECT '2.3 - PGS.PGTR.PGTRBFTP' AS control,
       p.PGTRBFTP AS valor_guardado, COUNT(*) AS pagos,
       (SELECT MAX(d.PDTRDSCR) FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
         WHERE r.PRBRALTR = 23 AND d.PDTRALTR = p.PGTRBFTP) AS como_alterno_23
  FROM PGS.PGTR p
 WHERE p.PGTRBFTP IS NOT NULL
 GROUP BY p.PGTRBFTP
 ORDER BY p.PGTRBFTP;

-- 2.4 El snapshot de las ordenes de pago de nomina.
SELECT '2.4 - RHH.DRPG.DRPGTPCT' AS control,
       o.DRPGTPCT AS valor_guardado, COUNT(*) AS detalles
  FROM RHH.DRPG o
 WHERE o.DRPGTPCT IS NOT NULL
 GROUP BY o.DRPGTPCT
 ORDER BY o.DRPGTPCT;


-- =====================================================================
-- 3 — 🔴 LOS MAPAS DE FORMATO BANCARIO. La trampa de la unificacion.
-- =====================================================================
-- FMBNMPTC es un texto del tipo "1=CTE;2=AHO" cuyas CLAVES son alternos del rubro
-- 199 (GeneracionOrdenPagoServiceImpl:502 y 590). Si se cambia el catalogo de RHH
-- al 23 sin re-clavar estos mapas, el archivo de nomina empieza a salir con el
-- tipo de cuenta equivocado, en silencio.
--
-- Mira cada mapa y contrastalo con el 1.1 y el 1.2 antes de decidir nada.
SELECT '3 - mapas de tipo de cuenta por formato' AS control,
       f.FMBNCDGO AS formato, f.FMBNMPTC AS mapa_tipo_cuenta
  FROM RHH.FMBN f
 ORDER BY f.FMBNCDGO;


-- =====================================================================
-- 4 — El caso concreto que hay que destrabar
-- =====================================================================
SELECT '4 - cuentas del empleado del anticipo 1' AS control,
       c.CBEMCDGO, c.MPLDCDGO AS empleado, c.BEXTCDGO AS banco,
       c.CBEMTPCT AS tipo_cuenta, c.CBEMNMCT AS numero,
       c.CBEMPRCP AS principal, c.CBEMESTD AS estado
  FROM RHH.CBEM c
 WHERE c.MPLDCDGO = (SELECT a.MPLDCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1)
 ORDER BY c.CBEMCDGO;


-- =====================================================================
-- LO QUE VIENE DESPUES, segun lo que devuelva esto
-- =====================================================================
-- CASO A — Los dos rubros numeran IGUAL (1 = AHORRO, 2 = CORRIENTE en ambos).
--   Es el mejor caso: la unificacion es SOLO de codigo y documentacion. No hay
--   migracion de datos, los mapas de FMBN siguen siendo validos y el commit
--   6e646ea ya estaba copiando un valor correcto por casualidad. Igual conviene
--   unificar para que deje de ser casualidad.
--
-- CASO B — Numeran DISTINTO.
--   Hay migracion de datos y son cuatro frentes, no uno:
--     1. RHH.CBEM.CBEMTPCT      traducir
--     2. RHH.DRPG.DRPGTPCT      traducir (es historico: evaluar si se toca)
--     3. RHH.FMBN.FMBNMPTC      re-clavar los mapas
--     4. Codigo: RubrosRrh.TIPO_CUENTA_BANCARIA en el front, y los javadoc de
--        CuentaBancariaEmpleado / DetalleOrdenPagoNomina en el back
--   Y hasta que eso este hecho, el anticipo NO debe copiar el valor tal cual:
--   hay que traducirlo al vuelo o bloquear la transferencia.
--
-- ⚠️ En los dos casos: mientras esto no se resuelva, NO generar archivo al banco
--    con un anticipo a empleado.
-- =====================================================================
