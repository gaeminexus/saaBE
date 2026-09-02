-- =====================================================================================
-- POR QUE LA CARGA 449 NO SE DEJA PROCESAR — replica exacta de la validacion del backend
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en produccion
--    en cualquier momento.
--
-- EL PLANTEO DEL USUARIO, y tiene razon: las 284 novedades del CSV son todas de tipo
-- 5 (SIN_DESCUENTOS) y 6 (DESCUENTOS_INCOMPLETOS), TODAS con monto diferencia NEGATIVO,
-- y ninguno de esos dos tipos deberia bloquear.
--
-- LA REGLA REAL (FamiliaNovedadCarga.clasificar):
--
--   BLOQUEANTE  si  tipo ∈ TIPOS_QUE_EXIGEN_AFECTACION  Y  (montoDiferencia IS NULL
--                                                            O montoDiferencia >= 0)
--   COBRANZA    si  montoDiferencia < -1.00   (cualquier tipo)
--   INFORMATIVA en cualquier otro caso
--
--   TIPOS_QUE_EXIGEN_AFECTACION = 1, 2, 3, 4, 7, 9, 10, 11, 12, 13, 18, 19, 20, 22
--   ⚠️ El 5 y el 6 NO ESTAN en esa lista. Con diferencia negativa dan COBRANZA.
--
-- ENTONCES, POR QUE BLOQUEA. La validacion que aborta (validarValoresConDestino ->
-- buscarValoresSinDestino -> novedadesQueRequierenAfectacion) mira TRES fuentes, no una:
--
--   1. CRD.NVPC — las novedades con monto, que son las del CSV.
--   2. CRD.PXCA.PXCANVCA  (novedadesCarga)       ⬅ se evalua con montoDiferencia = NULL
--   3. CRD.PXCA.PXCANVFN  (novedadesFinancieras) ⬅ se evalua con montoDiferencia = NULL
--
--   ⛔ Y AHI ESTA LA TRAMPA: los campos 2 y 3 son campos PLANOS del participe, sin monto
--      asociado, asi que el codigo les pasa NULL. Y en la regla, NULL cuenta como
--      «no hay dato para decir que sobra o falta» => BLOQUEANTE si el tipo esta en la
--      lista. Un tipo estructural (1,2,3,4) ahi BLOQUEA SIEMPRE, sin importar que la
--      novedad NVPC del mismo participe sea negativa y de cobranza.
--
--   El CSV muestra la fuente 1. El bloqueo puede venir de la 2 o la 3, que NO estan en
--   el CSV. Este script mira las tres juntas.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — LA RESPUESTA: quien bloquea, y por que fuente
--
-- Replica `buscarValoresSinDestino`: participes con descuento > 0.01 que tienen al menos
-- una novedad BLOQUEANTE. La columna QUE_BLOQUEA dice de cual de las tres fuentes sale.
--
-- Como leerlo:
--   * 0 filas -> la validacion de "valores sin destino" NO es la que bloquea, y hay que
--     mirar el bloque 4 (excedentes) o pedir el mensaje de error literal.
--   * Filas con QUE_BLOQUEA = 'PXCANVCA' o 'PXCANVFN' -> CONFIRMA el diagnostico: bloquea
--     un campo plano evaluado con NULL, no las novedades negativas del CSV.
--   * Filas con QUE_BLOQUEA = 'NVPC' -> hay novedades NVPC de tipo bloqueante que el CSV
--     no mostraba (el CSV puede estar filtrado).
-- =====================================================================================
SELECT  p.PXCACDPT                                      AS CODIGO_PETRO,
        SUBSTR(p.PXCANMBR, 1, 32)                       AS PARTICIPE,
        p.PXCADSDO                                      AS TOTAL_DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA,
        p.PXCANVFN                                      AS NOV_FINANCIERA,
        CASE
            WHEN p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVCA'
            WHEN p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVFN'
            WHEN EXISTS (SELECT 1 FROM CRD.NVPC n
                          WHERE n.PXCACDGO = p.PXCACDGO
                            AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                            AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0)) THEN 'NVPC'
            ELSE 'NINGUNA'
        END                                             AS QUE_BLOQUEA,
        NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0)        AS AFECTADO_MANUAL,
        ROUND(p.PXCADSDO
              - NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
                      JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
                     WHERE n2.PXCACDGO = p.PXCACDGO), 0), 2) AS SIN_DESTINO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     (   p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
         OR p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
         OR EXISTS (SELECT 1 FROM CRD.NVPC n
                     WHERE n.PXCACDGO = p.PXCACDGO
                       AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                       AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0)))
ORDER   BY QUE_BLOQUEA, SIN_DESTINO DESC;


-- =====================================================================================
-- BLOQUE 2 — El resumen: cuantos bloquean y por que fuente
--
-- Como leerlo: es el conteo de arriba agrupado. Si el grueso cae en PXCANVCA/PXCANVFN,
-- el arreglo es de esa evaluacion con NULL y no de la lista de tipos.
-- =====================================================================================
SELECT  CASE
            WHEN p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVCA'
            WHEN p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVFN'
            ELSE 'NVPC u otra'
        END                                             AS FUENTE,
        COUNT(*)                                        AS PARTICIPES,
        ROUND(SUM(p.PXCADSDO), 2)                       AS TOTAL_DESCONTADO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     (   p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
         OR p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
         OR EXISTS (SELECT 1 FROM CRD.NVPC n
                     WHERE n.PXCACDGO = p.PXCACDGO
                       AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                       AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0)))
GROUP   BY CASE
            WHEN p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVCA'
            WHEN p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVFN'
            ELSE 'NVPC u otra'
          END;


-- =====================================================================================
-- BLOQUE 3 — El panorama completo de la carga, sin filtrar
--
-- Que valores tienen PXCANVCA y PXCANVFN en TODA la carga. Sirve para ver si el tipo
-- que bloquea es masivo o puntual.
--
-- Referencia de tipos (ASPNovedadesCargaArchivo):
--   0 OK · 1 PARTICIPE_NO_ENCONTRADO · 2 CODIGO_ROL_DUPLICADO · 3 NOMBRE_DUPLICADO
--   4 CODIGO_PETRO_NO_COINCIDE · 5 SIN_DESCUENTOS · 6 DESCUENTOS_INCOMPLETOS
--   7 DESCUENTOS_ADICIONALES · 8 VALORES_CERO · 9 PRODUCTO_NO_MAPEADO
--   10 PRESTAMO_NO_ENCONTRADO · 11 MULTIPLES_PRESTAMOS · 12 CUOTA_NO_ENCONTRADA
--   13 MONTO_INCONSISTENTE · 14 PRESTAMO_PROCESADO_OK · 15 APORTE_GENERADO_OK
--   16 CUOTA_FECHA_DIFERENTE · 17 DIFERENCIA_MENOR_UN_DOLAR · 18-20 HISTORIAL SUELDO
--   21 APORTE_VALORES_CERO · 22 APORTE_MONTO_INCONSISTENTE · 23 APORTE_DIF_MENOR_UN_DOLAR
-- =====================================================================================
SELECT  p.PXCANVCA                                      AS NOV_CARGA,
        p.PXCANVFN                                      AS NOV_FINANCIERA,
        COUNT(*)                                        AS PARTICIPES,
        SUM(CASE WHEN NVL(p.PXCADSDO,0) > 0.01 THEN 1 ELSE 0 END) AS CON_DESCUENTO,
        CASE WHEN p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                OR p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
             THEN 'SI' ELSE 'no' END                    AS BLOQUEA_POR_CAMPO_PLANO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
GROUP   BY p.PXCANVCA, p.PXCANVFN
ORDER   BY PARTICIPES DESC;


-- =====================================================================================
-- BLOQUE 4 — La OTRA validacion que aborta: el reparto de excedentes
--
-- validarRepartoDeExcedentes exige que toda novedad CON EXCEDENTE (diferencia POSITIVA:
-- Petro descontó de mas) este repartida al 100%. Si una sola no cuadra, no se procesa
-- NADA de la carga.
--
-- Como leerlo: las novedades del CSV son todas NEGATIVAS, asi que no deberian entrar
-- aca. Si este bloque devuelve filas, ESA es la validacion que esta bloqueando y no la
-- de valores sin destino.
-- =====================================================================================
SELECT  p.PXCACDPT                                      AS CODIGO_PETRO,
        SUBSTR(p.PXCANMBR, 1, 32)                       AS PARTICIPE,
        n.NVPCCDGO                                      AS ID_NOVEDAD,
        n.NVPCTPNV                                      AS TIPO,
        n.NVPCMNDF                                      AS DIFERENCIA,
        NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              WHERE a.NVPCCDGO = n.NVPCCDGO), 0)        AS REPARTIDO,
        ROUND(n.NVPCMNDF
              - NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
                      WHERE a.NVPCCDGO = n.NVPCCDGO), 0), 2) AS FALTA_REPARTIR
FROM    CRD.NVPC n
JOIN    CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(n.NVPCMNDF, 0) > 1.00
AND     ABS(NVL(n.NVPCMNDF,0)
            - NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
                    WHERE a.NVPCCDGO = n.NVPCCDGO), 0)) > 1.00
ORDER   BY FALTA_REPARTIR DESC;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
--
-- ⚠️ Y si tenés a mano el MENSAJE DE ERROR literal que devolvio la pantalla, pegalo
--    tambien: el backend lo arma nombrando rol, valor sin aplicar y la novedad culpable
--    ("Rol X NOMBRE (PQ): $Y sin aplicar de $Z descontados. Novedad: ..."), asi que dice
--    en una linea lo que estos cuatro bloques deducen.
-- =====================================================================================
