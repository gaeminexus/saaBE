-- =====================================================================================
-- DDL — RPR.CCPM: tres columnas nuevas para el informe mensual de prestamos
-- FECHA: 2026-09-09   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 220 (rango 200-249)
--
-- ⛔ CORRER ESTE SCRIPT **ANTES** DE DESPLEGAR EL WAR. No es una recomendacion.
--
--    Hibernate incluye TODA columna @Column en el SELECT que genera. En cuanto el WAR
--    suba con la entidad CreditoCuotasPrestamosMensual mapeando estas tres columnas,
--    CUALQUIER lectura del CCPM revienta con ORA-00904 si la tabla no las tiene todavia
--    — no solo el informe nuevo: la pantalla entera de informes mensuales de credito.
--
--    Ya paso tres veces en nueve dias en este repositorio. La ultima, el 2026-09-08:
--    CRD.CFCR no existia, los G40-G51 de agosto no salieron, y el error que se veia en
--    pantalla era un STATUS_MARKED_ROLLBACK que no nombraba la causa por ningun lado.
--
-- QUE HACE
--    Agrega a RPR.CCPM el nombre del participe, la fecha de vencimiento del prestamo y
--    el monto solicitado, para que el informe mensual deje de mostrar solo la cedula.
--
-- QUE **NO** HACE
--    No toca RPR.CG48 ni ningun otro reporte regulatorio: el CCPM es informe interno.
--    No toca RPR.HMCP (el historico), que ya hoy no espeja la columna CCPMFCPR que se
--    agrego antes que estas. Se mantiene ese precedente a proposito.
--    No inserta ninguna fila, asi que NO hay secuencias que sincronizar.
--
-- ORIGEN DE CADA DATO (verificado contra el codigo, no contra documentacion)
--    CCPMRZSC  <- Entidad.razonSocial      (CRD.ENTD.ENTDRZNS, VARCHAR2(2000))
--    CCPMFCVN  <- Prestamo.fechaFin        (la fecha de vencimiento de la ULTIMA cuota;
--                                           PrestamoServiceImpl:568-575 la calcula asi)
--    CCPMMNSL  <- Prestamo.montoSolicitado (elegido por el usuario el 2026-09-09)
--
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES. ¿Ya existen las columnas?
--   Esperado en una base que todavia no corrio este script: 0 filas.
--   Si devuelve 1, 2 o 3 filas, el script YA se corrio (total o parcialmente): NO
--   vuelvas a correr el bloque 2, revisa cual falta y agrega solo esa.
-- =====================================================================================
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'RPR'
   AND table_name = 'CCPM'
   AND column_name IN ('CCPMRZSC', 'CCPMFCVN', 'CCPMMNSL')
 ORDER BY column_name;


-- =====================================================================================
-- BLOQUE 1 — MEDICION PREVIA: ¿que tan poblado esta PRSTMNSL en la cartera viva?
--
--   ⚠️ POR QUE ESTE BLOQUE EXISTE, Y POR QUE CONVIENE LEERLO ANTES DEL BLOQUE 2.
--   El monto solicitado es el unico de los tres datos que NINGUNA linea del backend
--   escribe: llega tal cual del JSON del frontend al dar de alta el prestamo. La
--   cartera migrada entro por el camino de carga por Excel, que tampoco lo setea.
--   Si este bloque dice que la mayoria esta en NULL o en 0, la columna nueva va a
--   salir vacia en el informe y hay que decidir otra fuente ANTES de agregarla.
--
--   Se mide solo sobre prestamos operables (VIGENTE=2, EN_MORA=11), que es el
--   universo que el CCPM reporta.
-- =====================================================================================
SELECT COUNT(*)                                                        AS total_vivos,
       SUM(CASE WHEN p.PRSTMNSL IS NULL      THEN 1 ELSE 0 END)        AS monto_nulo,
       SUM(CASE WHEN p.PRSTMNSL = 0          THEN 1 ELSE 0 END)        AS monto_cero,
       SUM(CASE WHEN p.PRSTMNSL > 0          THEN 1 ELSE 0 END)        AS monto_util,
       SUM(CASE WHEN p.PRSTFCFN IS NULL      THEN 1 ELSE 0 END)        AS fecha_fin_nula,
       SUM(CASE WHEN e.ENTDRZNS IS NULL
                  OR TRIM(e.ENTDRZNS) IS NULL THEN 1 ELSE 0 END)       AS razon_social_vacia
  FROM CRD.PRST p
  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
 WHERE p.PRSTIDST IN (2, 11);


-- =====================================================================================
-- BLOQUE 2 — EL ALTER. Escribe.
--
--   CCPMRZSC se declara VARCHAR2(2000), el MISMO ancho que su origen CRD.ENTD.ENTDRZNS.
--   Es deliberado: cualquier ancho menor abre la puerta a un ORA-12899 al generar el
--   informe, con la corrida entera revertida por una sola razon social larga. Ese error
--   ya se pago una vez en los generadores de los reportes G (commit 0140477).
-- =====================================================================================
ALTER TABLE RPR.CCPM ADD (
    CCPMRZSC  VARCHAR2(2000)  NULL,
    CCPMFCVN  DATE            NULL,
    CCPMMNSL  NUMBER(18,2)    NULL
);

COMMENT ON COLUMN RPR.CCPM.CCPMRZSC IS 'Nombre / razon social del participe (CRD.ENTD.ENTDRZNS)';
COMMENT ON COLUMN RPR.CCPM.CCPMFCVN IS 'Fecha de vencimiento del prestamo = vencimiento de la ultima cuota (CRD.PRST.PRSTFCFN)';
COMMENT ON COLUMN RPR.CCPM.CCPMMNSL IS 'Monto solicitado del prestamo (CRD.PRST.PRSTMNSL)';

COMMIT;


-- =====================================================================================
-- BLOQUE 3 — CONTROL DESPUES.
--   Esperado: EXACTAMENTE 3 filas.
--     CCPMFCVN  DATE       -   Y
--     CCPMMNSL  NUMBER    22   Y
--     CCPMRZSC  VARCHAR2 2000  Y
--   Si devuelve menos de 3, el ALTER fallo parcialmente: NO despliegues el WAR.
-- =====================================================================================
SELECT column_name, data_type, data_length, data_precision, data_scale, nullable
  FROM all_tab_columns
 WHERE owner = 'RPR'
   AND table_name = 'CCPM'
   AND column_name IN ('CCPMRZSC', 'CCPMFCVN', 'CCPMMNSL')
 ORDER BY column_name;


-- =====================================================================================
-- BLOQUE 4 — REVERSO. COMENTADO A PROPOSITO.
--
--   Solo tiene sentido si el WAR NO se desplego todavia. Con el WAR arriba, borrar
--   estas columnas rompe toda lectura del CCPM con ORA-00904, que es exactamente el
--   problema que este script existe para evitar.
--
--   ALTER TABLE RPR.CCPM DROP (CCPMRZSC, CCPMFCVN, CCPMMNSL);
--   COMMIT;
-- =====================================================================================
