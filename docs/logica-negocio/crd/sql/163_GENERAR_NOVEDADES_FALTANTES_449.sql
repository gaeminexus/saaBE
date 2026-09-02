-- =====================================================================================
-- ⛔ URGENTE — generar las novedades faltantes de la carga 449, para poder repartirlas
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ ESTE SCRIPT ESCRIBE (INSERT en CRD.NVPC). Controles antes y despues, y reverso al
--    final. NO toca ninguna fila existente: solo agrega las que faltan.
--
-- DECISION DEL USUARIO (2026-09-02): el tipo 4 CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE
-- **SIGUE BLOQUEANDO**. Lo que se corrige es que hoy no hay forma de resolverlo: se
-- generan las filas de novedad que faltan para que aparezcan en pantalla y el operador
-- las reparte a mano, como cualquier otra bloqueante.
--
-- EL PROBLEMA, medido con el script 162:
--   30 participes bloquean la carga 449 por PXCA.PXCANVCA = 4, por $10.219,29.
--   **26 de ellos NO TIENEN NINGUNA FILA EN CRD.NVPC.**
--
--   El bloqueo lo produce un campo PLANO del participe (PXCANVCA), que
--   novedadesQueRequierenAfectacion evalua con montoDiferencia = NULL — y NULL cuenta
--   como BLOQUEANTE. Pero la pantalla lista filas de CRD.NVPC. Sin fila NVPC, el
--   mensaje "Registre en las novedades como aplicar cada valor" pide algo IMPOSIBLE.
--
-- QUE HACE ESTE SCRIPT: crea una fila NVPC de tipo 4 por cada uno de esos participes,
-- con el patron EXACTO de registrarNovedad() (CargaArchivoPetroServiceImpl:939).
--
-- ⚠️ POR QUE montoEsperado = 0 y montoRecibido = totalDescontado, y no al reves:
--   Asi la novedad queda con montoDiferencia = totalDescontado (positiva), y entonces:
--     * clasificar(4, positiva) -> BLOQUEANTE. Sigue bloqueando, que es lo que se pidio.
--     * La pantalla la muestra (filtra tipoNovedad > 3, y 4 pasa).
--     * montoDisponibleAfectacion usa montoRecibido -> el operador reparte el TOTAL
--       DESCONTADO, que es exactamente lo que validarValoresConDestino exige para dejar
--       procesar. Al repartirlo entero, valorSinDestino queda en 0 y la carga avanza.
--   Con los montos al reves nada de esto encaja.
--
-- LA PK ES IDENTITY (NovedadParticipeCarga:38), asi que NO hay secuencia que sincronizar.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220

DEFINE CARGA = 449


-- =====================================================================================
-- CONTROL 0 — ANTES DE EJECUTAR
-- =====================================================================================

-- 0.1 A quienes se les va a crear la novedad. Esperado: 26 filas (las del bloque 2 del
--     162 con NVPC_TOTAL = 0). Revisar la lista antes de seguir.
SELECT  p.PXCACDGO                                      AS ID_PXCA,
        p.PXCACDPT                                      AS ROL_PETRO,
        SUBSTR(p.PXCANMBR, 1, 34)                       AS PARTICIPE,
        p.PXCADSDO                                      AS TOTAL_DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n WHERE n.PXCACDGO = p.PXCACDGO)
ORDER   BY p.PXCADSDO DESC;

-- 0.2 El total que se va a poder repartir. Esperado: cerca de $10.219,29 menos lo de los
--     4 que si tienen novedad. Si el numero es muy distinto, PARAR y avisar.
SELECT  COUNT(*)                                        AS NOVEDADES_A_CREAR,
        ROUND(SUM(p.PXCADSDO), 2)                       AS MONTO_TOTAL
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n WHERE n.PXCACDGO = p.PXCACDGO);


-- =====================================================================================
-- EJECUCION
--
-- Una fila NVPC por participe sin novedad. Los campos siguen registrarNovedad():
--   tipoNovedad   = el que trae el campo plano PXCANVCA (normalmente 4)
--   descripcion   = el mismo texto que usa describirNovedades()
--   montoEsperado = 0        (no hay cuota esperada: la novedad es de identificacion)
--   montoRecibido = PXCADSDO (lo que Petro descontó)
--   montoDiferencia = recibido - esperado = PXCADSDO
--   codigoProducto / codigoPrestamo / idAsoprepPrestamo = NULL — igual que
--       registrarNovedad cuando no los tiene; el operador elige el destino en pantalla
--   estado = 1
-- =====================================================================================

INSERT INTO CRD.NVPC (PXCACDGO, NVPCTPNV, NVPCDSCR, NVPCMNES, NVPCMNRC, NVPCMNDF,
                      NVPCCDCA, NVPCESTD)
SELECT  p.PXCACDGO,
        p.PXCANVCA,
        CASE p.PXCANVCA
            WHEN 1  THEN 'PARTICIPE NO ENCONTRADO'
            WHEN 2  THEN 'CODIGO ROL DUPLICADO'
            WHEN 3  THEN 'NOMBRE DE ENTIDAD DUPLICADO'
            WHEN 4  THEN 'CÓDIGO PETRO NO COINCIDE CON EL NOMBRE'
            ELSE 'NOVEDAD ESTRUCTURAL ' || TO_CHAR(p.PXCANVCA)
        END || ' (novedad generada para permitir la afectación manual, 2026-09-02).',
        0,
        p.PXCADSDO,
        p.PXCADSDO,
        &CARGA,
        1
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n WHERE n.PXCACDGO = p.PXCACDGO);

COMMIT;


-- =====================================================================================
-- CONTROL 1 — DESPUES DE EJECUTAR
-- =====================================================================================

-- 1.1 Las novedades creadas. Esperado: las mismas 26 filas del control 0.1, ahora con
--     su NVPCCDGO asignado y NVPCMNDF = el total descontado.
SELECT  n.NVPCCDGO                                      AS ID_NOVEDAD,
        p.PXCACDPT                                      AS ROL_PETRO,
        SUBSTR(p.PXCANMBR, 1, 30)                       AS PARTICIPE,
        n.NVPCTPNV                                      AS TIPO,
        n.NVPCMNES                                      AS ESPERADO,
        n.NVPCMNRC                                      AS RECIBIDO,
        n.NVPCMNDF                                      AS DIFERENCIA,
        n.NVPCESTD                                      AS ESTADO
FROM    CRD.NVPC n
JOIN    CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
WHERE   n.NVPCCDCA = &CARGA
AND     n.NVPCDSCR LIKE '%generada para permitir%'
ORDER   BY n.NVPCMNRC DESC;

-- 1.2 ⛔ EL CONTROL QUE IMPORTA: ya no debe quedar NINGUN participe que bloquee sin
--     tener donde afectar. Esperado: 0 filas.
SELECT  p.PXCACDGO, p.PXCACDPT, p.PXCANMBR, p.PXCADSDO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n WHERE n.PXCACDGO = p.PXCACDGO);

-- 1.3 Lo que le queda por repartir al operador para que la carga procese.
--     Cada fila es un participe que tiene que afectarse en pantalla por su TOTAL.
SELECT  COUNT(*)                                        AS PARTICIPES_A_REPARTIR,
        ROUND(SUM(p.PXCADSDO), 2)                       AS MONTO_A_REPARTIR
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0) < p.PXCADSDO - 1;


-- =====================================================================================
-- REVERSO — COMENTADO
--
-- Borra SOLO las novedades creadas por este script (se reconocen por la descripcion).
-- ⛔ Si el operador ya registro afectaciones contra ellas, primero hay que borrar esas
--    filas de CRD.AVPC — y eso SI es perdida de trabajo del operador. Mirar el control
--    de abajo antes de revertir.
-- =====================================================================================
--
-- -- Cuantas afectaciones se perderian. Si da > 0, PARAR y decidir con el usuario.
-- SELECT COUNT(*) FROM CRD.AVPC a
--  JOIN CRD.NVPC n ON n.NVPCCDGO = a.NVPCCDGO
--  WHERE n.NVPCCDCA = &CARGA AND n.NVPCDSCR LIKE '%generada para permitir%';
--
-- DELETE FROM CRD.NVPC
--  WHERE NVPCCDCA = &CARGA
--    AND NVPCDSCR LIKE '%generada para permitir%';
-- COMMIT;


-- =====================================================================================
-- DESPUES DE ESTO
--
-- 1. Las 26 novedades aparecen en la pantalla de descuentos como BLOQUEANTES.
-- 2. El operador las reparte por su TOTAL DESCONTADO (la pantalla ya ofrece ese monto).
-- 3. Con todas repartidas, validarValoresConDestino deja procesar el archivo.
--
-- ⚠️ ESTO ES UNA CORRECCION DE DATOS DE ESTA CARGA. Para que no vuelva a pasar el mes
--    que viene, la generacion de la novedad tiene que salir del propio proceso de carga
--    — pendiente de backend, ver el prompt despachado el 2026-09-02.
-- =====================================================================================
