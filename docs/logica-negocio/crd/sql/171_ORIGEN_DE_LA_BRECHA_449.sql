-- =====================================================================================
-- ⛔ DE DONDE SALEN LOS $2.906,52: origen de la brecha, pago por pago — carga 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- ⛔ LO QUE EL SCRIPT 170 DESCARTO, y hay que dejarlo escrito para no volver a perseguirlo:
--
--   Resultado del 170: BRECHA = 0,01 sobre 1.092 cuotas.
--     276.748,93 (DTPRTTLL) - 1.284,42 (mora) = 275.464,51
--     173.551,52 + 88.171,50 + 11.732,83 + 2.008,65      = 275.464,50
--
--   O sea: las CUOTAS cuadran perfecto. `calcularSaldosRealesCuota` y sus dos ramas NO son
--   la causa — esa hipotesis queda descartada con datos, no con argumentos. La brecha no
--   esta en LEER la cuota, esta en ESCRIBIR el pago: PGPRVLRR queda mas alto que la suma
--   de los cuatro componentes que se graban al lado.
--
-- LOS DOS PUNTOS DEL CODIGO QUE PUEDEN GENERARLA, los dos en la ruta de AFECTACION MANUAL
-- (aplicarAfectacionManualConRegistroPago), que es la que se uso para desbloquear la 449:
--
--   (A) El seguro de incendio NO EXISTE en la afectacion manual. Linea 3081:
--           double seguroIncendioAfectar = 0.0; // Por ahora no se maneja seguro
--       Si el operador digito un desglose (capital/interes/desgravamen), ese 0.0 sobrevive
--       hasta el grabado, pero el TOTAL que se graba es valorAfectar completo. Toda cuota
--       con seguro afectada a mano deja el seguro fuera de los componentes y dentro del
--       total. Encaja con que el seguro grabado (893,49) sea menos de la mitad del seguro
--       de las cuotas (2.008,65).
--
--   (B) La distribucion automatica descarta el sobrante en silencio. Linea 3152:
--           if (montoRestante > 0.01) {
--               System.out.println("  Excedente no aplicado: $" + montoRestante);
--           }
--       ...y despues graba valorTotalAfectar COMPLETO como PGPRVLRR. Lo que no encontro
--       destino no se resta del total: se convierte en brecha.
--
-- ⛔ POR QUE HACE FALTA ESTE SELECT Y NO ALCANZA CON LEER EL CODIGO: los dos defectos son
--    reales y estan a la vista, pero eso NO prueba que expliquen los 2.906,52. Si el grueso
--    de la brecha viene de los pagos normales ("Pago cuota #"), el problema es otro y
--    arreglar solo la ruta manual dejaria el descuadre igual. Ya se perdio un ciclo
--    arreglando el seguro por deduccion algebraica (commit a09732f) y salio PEOR:
--    el seguro bajo de 1.124,28 a 893,49 y la brecha SUBIO 230,79, exactamente lo mismo.
--    No se toca una linea mas sin esta medicion.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los tres bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL BLOQUE QUE DECIDE: la brecha partida por ORIGEN del pago
--
-- La observacion del pago dice de que ruta salio:
--   Afectacion manual AVPC -> aplicarAfectacionManualConRegistroPago  (defectos A y B)
--   Pago cuota #           -> procesarPagoCuota, la ruta normal del archivo
--
-- Como leerlo:
--   * BRECHA concentrada en AFECTACION_MANUAL -> son A y B. Arreglo acotado a un metodo:
--     agregar el seguro a la afectacion manual y no grabar como total lo que no se
--     distribuyo. Se corrige y se reprocesa.
--   * BRECHA repartida o concentrada en PAGO_NORMAL -> hay un tercer defecto que todavia
--     no identifique. NO se toca nada: lo digo y sigo buscando.
-- =====================================================================================
SELECT  CASE
            WHEN g.PGPROBSR LIKE 'Afectaci%n manual AVPC%' THEN 'AFECTACION_MANUAL'
            WHEN g.PGPROBSR LIKE 'Pago cuota%'             THEN 'PAGO_NORMAL'
            ELSE 'OTRO'
        END                                                 AS ORIGEN,
        COUNT(*)                                            AS PAGOS,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS TOTAL_REGISTRADO,
        ROUND(SUM(NVL(g.PGPRCPPG,0)), 2)                    AS CAPITAL,
        ROUND(SUM(NVL(g.PGPRINPG,0)), 2)                    AS INTERES,
        ROUND(SUM(NVL(g.PGPRDSGR,0)), 2)                    AS DESGRAVAMEN,
        ROUND(SUM(NVL(g.PGPRVLSI,0)), 2)                    AS SEGURO_INCENDIO,
        ROUND(SUM(NVL(g.PGPRVLRR,0))
              - SUM(NVL(g.PGPRCPPG,0)) - SUM(NVL(g.PGPRINPG,0))
              - SUM(NVL(g.PGPRMRPG,0)) - SUM(NVL(g.PGPRINVP,0))
              - SUM(NVL(g.PGPRDSGR,0)) - SUM(NVL(g.PGPRVLSI,0))
              - SUM(NVL(g.PGPRSLOT,0)), 2)                  AS BRECHA
FROM    CRD.PGPR g
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
GROUP   BY CASE
            WHEN g.PGPROBSR LIKE 'Afectaci%n manual AVPC%' THEN 'AFECTACION_MANUAL'
            WHEN g.PGPROBSR LIKE 'Pago cuota%'             THEN 'PAGO_NORMAL'
            ELSE 'OTRO'
        END
ORDER   BY BRECHA DESC;


-- =====================================================================================
-- BLOQUE 2 — Los pagos con brecha, uno por uno (los 30 mayores)
--
-- SEGURO_DE_LA_CUOTA es la columna clave para confirmar el defecto (A): si el pago tiene
-- brecha Y la cuota tiene seguro Y el pago grabo seguro 0, es exactamente el = 0.0
-- hardcodeado de la afectacion manual.
-- =====================================================================================
SELECT  g.PGPRCDGO                                          AS PAGO,
        g.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS NRO_CUOTA,
        NVL(g.PGPRVLRR,0)                                   AS TOTAL_GRABADO,
        NVL(g.PGPRCPPG,0)                                   AS CAPITAL,
        NVL(g.PGPRINPG,0)                                   AS INTERES,
        NVL(g.PGPRDSGR,0)                                   AS DESGRAVAMEN,
        NVL(g.PGPRVLSI,0)                                   AS SEGURO_GRABADO,
        NVL(d.DTPRVLSI,0)                                   AS SEGURO_DE_LA_CUOTA,
        ROUND(NVL(g.PGPRVLRR,0) - NVL(g.PGPRCPPG,0) - NVL(g.PGPRINPG,0)
              - NVL(g.PGPRMRPG,0) - NVL(g.PGPRINVP,0)
              - NVL(g.PGPRDSGR,0) - NVL(g.PGPRVLSI,0)
              - NVL(g.PGPRSLOT,0), 2)                       AS BRECHA,
        SUBSTR(g.PGPROBSR, 1, 45)                           AS ORIGEN
FROM    CRD.PGPR g
LEFT    JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
AND     ABS(NVL(g.PGPRVLRR,0) - NVL(g.PGPRCPPG,0) - NVL(g.PGPRINPG,0)
            - NVL(g.PGPRMRPG,0) - NVL(g.PGPRINVP,0)
            - NVL(g.PGPRDSGR,0) - NVL(g.PGPRVLSI,0)
            - NVL(g.PGPRSLOT,0)) > 0.01
ORDER   BY BRECHA DESC
FETCH FIRST 30 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 3 — El seguro de incendio: cuanto habia que cobrar y cuanto se grabo
--
-- Como leerlo: si NO_GRABADO se acerca a la brecha total, el defecto (A) la explica casi
-- entera y el arreglo es uno solo. Si es una fraccion, hay que sumar el (B) — y el bloque 1
-- ya dijo si alcanzan entre los dos.
-- =====================================================================================
SELECT  COUNT(DISTINCT d.DTPRCDGO)                          AS CUOTAS_CON_SEGURO,
        ROUND(SUM(NVL(d.DTPRVLSI,0)), 2)                    AS SEGURO_DE_LAS_CUOTAS,
        ROUND(NVL((SELECT SUM(NVL(g2.PGPRVLSI,0)) FROM CRD.PGPR g2
                    WHERE g2.CRARCDGO = &CARGA
                      AND NVL(g2.PGPRANUL,0) = 0), 0), 2)   AS SEGURO_GRABADO_EN_PAGOS,
        ROUND(SUM(NVL(d.DTPRVLSI,0))
              - NVL((SELECT SUM(NVL(g2.PGPRVLSI,0)) FROM CRD.PGPR g2
                      WHERE g2.CRARCDGO = &CARGA
                        AND NVL(g2.PGPRANUL,0) = 0), 0), 2) AS NO_GRABADO
FROM    CRD.DTPR d
WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                        WHERE g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0)
AND     NVL(d.DTPRVLSI,0) > 0.01;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
