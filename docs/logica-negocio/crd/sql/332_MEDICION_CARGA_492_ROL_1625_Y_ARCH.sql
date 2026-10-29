-- =====================================================================================
-- 332 — MEDICIÓN (SOLO LECTURA). Equipo omen-saa-1, 2026-10-29.
--
-- PARTE A: por qué la carga Petro 492 (octubre) no se procesa.
--   Error: "Rol 1625 cédula 1802846046: descontado $334,75, aplicado $264,25, diferencia
--   $-70,50". La novedad dice: "Contrato activo (código 10546) pero su vigencia no cubre uno o
--   más de los 60 meses evaluados (sin vigencia vigente, o vigencia con monto $0)".
--   Código: CargaArchivoPetroServiceImpl.distribuirAportePorDevengo (~5100). El esperado de
--   cada mes sale de CRD.VGCN con VGCNIDST = 1 (ACTIVO), VGCNFCIN <= fin de mes y
--   (VGCNFCFN IS NULL OR VGCNFCFN >= fin de mes), una fila por tipo de aporte (9 jubilación,
--   11 cesantía). Si ninguna vigencia cubre el mes, o su monto es 0, ese mes no recibe aporte.
--   Hay que saber cuál de las dos causas es:
--     (1) dato mal cargado (la vigencia debía cubrir octubre)  -> se corrige el dato y los
--         $70,50 van a aportes;
--     (2) el partícipe ya no debe aportar (jubilado / cesado)  -> los $70,50 se reparten a mano
--         (Afectar cuotas) o se devuelven. Decisión de negocio.
--
-- PARTE B: datos para diseñar la carga de la respuesta ARCH (filial 2).
--
-- NO modifica nada. Se puede correr de corrido. Pasar al árbitro la salida de cada bloque.
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- A1. Cabecera de la carga 492
-- ---------------------------------------------------------------------------
SELECT c.CRARCDGO, c.FLLLCDGO, c.CRARMSAF AS MES, c.CRARANAF AS ANIO, c.CRARESTD AS ESTADO,
       c.CRARFCCR AS FECHA_CARGA, c.CRARNMBR
  FROM CRD.CRAR c
 WHERE c.CRARCDGO = 492;

-- ---------------------------------------------------------------------------
-- A2. Lo que trajo el archivo para el rol 1625, por producto
-- ---------------------------------------------------------------------------
SELECT d.DTCACDPP AS PRODUCTO, p.PXCACDGO, p.PXCACDPT AS ROL, p.PXCANMBR,
       p.PXCADSCT, p.PXCADSDO, p.PXCAESTD, p.PXCANVCA, p.PXCANVFN
  FROM CRD.PXCA p
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 492
   AND p.PXCACDPT = 1625
 ORDER BY d.DTCACDPP;

-- ---------------------------------------------------------------------------
-- A3. La entidad (por cédula y por rol: deben ser la misma fila)
-- ---------------------------------------------------------------------------
SELECT e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC, e.FLLLCDGO, e.ENTDIDST
  FROM CRD.ENTD e
 WHERE e.ENTDNMID = '1802846046'
    OR e.ENTDRLPC = 1625;

-- ---------------------------------------------------------------------------
-- A4. Todos los contratos de la entidad (el 10546 es el ACTIVO que vio el proceso)
-- ---------------------------------------------------------------------------
SELECT k.CNTRCDGO, k.TPCNCDGO, k.CNTRFCIN, k.CNTRFCTR AS FECHA_TERMINACION,
       k.CNTRMTTR AS MOTIVO_TERMINACION, k.CNTRESTD, k.CNTRIDST,
       k.CNTRMNAJ, k.CNTRMNAC
  FROM CRD.CNTR k
 WHERE k.ENTDCDGO IN (SELECT e.ENTDCDGO FROM CRD.ENTD e WHERE e.ENTDNMID = '1802846046')
 ORDER BY k.CNTRCDGO;

-- ---------------------------------------------------------------------------
-- A5. Todas las vigencias del contrato 10546 (activas e inactivas).
--     Mirar: ¿hay fila ACTIVA (VGCNIDST = 1) por tipo 9 y 11 que cubra el 31-10-2026?
--     ¿VGCNFCFN cerrada? ¿VGCNMNTO = 0? ¿VGCNFCIN posterior a octubre?
-- ---------------------------------------------------------------------------
SELECT v.VGCNCDGO, v.TPAPCDGO, v.VGCNFCIN, v.VGCNFCFN, v.VGCNMNTO, v.VGCNPRCN, v.VGCNRMUN,
       v.VGCNMODO, v.VGCNIDST, v.VGCNUSRG, v.VGCNFCRG, v.VGCNOBSR,
       CASE WHEN v.VGCNIDST = 1
             AND v.VGCNFCIN <= DATE '2026-10-31'
             AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= DATE '2026-10-31')
            THEN 'CUBRE OCT-2026' ELSE 'NO CUBRE' END AS CUBRE_OCTUBRE
  FROM CRD.VGCN v
 WHERE v.CNTRCDGO = 10546
 ORDER BY v.TPAPCDGO, v.VGCNFCIN;

-- ---------------------------------------------------------------------------
-- A6. Aportes de la entidad en los últimos 18 meses de devengo (¿hasta cuándo aportó?)
-- ---------------------------------------------------------------------------
SELECT a.APRTCDGO, a.TPAPCDGO, a.APRTPRDV AS DEVENGO, a.APRTVLRR, a.APRTVLPG, a.APRTSLDO,
       a.APRTIDST, a.APRTTPMV, a.CRARCDGO, a.APRTFCRG
  FROM CRD.APRT a
 WHERE a.ENTDCDGO IN (SELECT e.ENTDCDGO FROM CRD.ENTD e WHERE e.ENTDNMID = '1802846046')
   AND (a.APRTPRDV >= DATE '2025-04-01' OR a.APRTPRDV IS NULL)
 ORDER BY a.APRTPRDV DESC NULLS FIRST, a.TPAPCDGO;

-- ---------------------------------------------------------------------------
-- A7. Novedades y afectaciones manuales del rol 1625 en la carga 492
-- ---------------------------------------------------------------------------
SELECT n.NVPCCDGO, n.PXCACDGO, n.NVPCTPNV, n.NVPCMNES, n.NVPCMNRC, n.NVPCMNDF, n.NVPCESTD,
       SUBSTR(n.NVPCDSCR, 1, 300) AS DESCRIPCION
  FROM CRD.NVPC n
 WHERE n.PXCACDGO IN (SELECT p.PXCACDGO FROM CRD.PXCA p JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
                       WHERE d.CRARCDGO = 492 AND p.PXCACDPT = 1625);

SELECT f.AVPCCDGO, f.NVPCCDGO, f.PRSTCDGO, f.DTPRCDGO, f.TPAPCDGO, f.AVPCVAFA, f.AVPCESTD,
       f.AVPCUSAR, f.AVPCFCRG
  FROM CRD.AVPC f
 WHERE f.NVPCCDGO IN (SELECT n.NVPCCDGO FROM CRD.NVPC n
                       WHERE n.PXCACDGO IN (SELECT p.PXCACDGO FROM CRD.PXCA p
                                              JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
                                             WHERE d.CRARCDGO = 492 AND p.PXCACDPT = 1625));

-- ---------------------------------------------------------------------------
-- A8. ¿Es el único? Otras novedades del mismo tipo en la carga 492
-- ---------------------------------------------------------------------------
SELECT p.PXCACDPT AS ROL, p.PXCANMBR, n.NVPCMNRC AS SOBRANTE, n.NVPCESTD
  FROM CRD.NVPC n
  JOIN CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 492
   AND n.NVPCDSCR LIKE 'Contrato activo%vigencia no cubre%';

-- ---------------------------------------------------------------------------
-- A9. Riesgo para las próximas cargas: contratos ACTIVOS sin vigencia ACTIVA que cubra
--     el 31-10-2026 en algún tipo (9 o 11), con aporte esperado en el contrato.
-- ---------------------------------------------------------------------------
SELECT k.CNTRCDGO, k.ENTDCDGO, e.ENTDRLPC, e.FLLLCDGO, t.TPAPCDGO,
       (SELECT COUNT(*) FROM CRD.VGCN v WHERE v.CNTRCDGO = k.CNTRCDGO AND v.TPAPCDGO = t.TPAPCDGO) AS VIGENCIAS_TOTALES
  FROM CRD.CNTR k
  JOIN CRD.ENTD e ON e.ENTDCDGO = k.ENTDCDGO
  CROSS JOIN (SELECT 9 AS TPAPCDGO FROM DUAL UNION ALL SELECT 11 FROM DUAL) t
 WHERE k.CNTRESTD = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.VGCN v
                    WHERE v.CNTRCDGO = k.CNTRCDGO AND v.TPAPCDGO = t.TPAPCDGO
                      AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
                      AND v.VGCNFCIN <= DATE '2026-10-31'
                      AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= DATE '2026-10-31'))
 ORDER BY e.FLLLCDGO, k.CNTRCDGO, t.TPAPCDGO;
-- ⚠️ Si CNTRESTD no es el campo de estado ACTIVO en tu base (hay CNTRESTD y CNTRIDST), cambia la
--    condición a k.CNTRIDST = 1 y avísame cuál de las dos devolvió el contrato 10546 en A4.

-- =====================================================================================
-- PARTE B — respuesta ARCH (filial 2)
-- =====================================================================================

-- B1. Generaciones ARCH registradas (para saber contra qué generación cuadrar cada respuesta)
SELECT g.GNAPCDGO, g.GNAPMSPE AS MES, g.GNAPANPE AS ANIO, g.GNAPESTD, g.GNAPTLRG, g.GNAPMTEN,
       g.GNAPNMAR, g.GNAPFCGN
  FROM CRD.GNAP g
 WHERE g.FLLLCDGO = 2
 ORDER BY g.GNAPANPE DESC, g.GNAPMSPE DESC;

-- B2. Detalle por partícipe y producto de la generación ARCH de AGOSTO 2026 (comparar con el
--     PDF de la respuesta: 6 partícipes, total 866,70)
SELECT e.ENTDNMID AS CEDULA, e.ENTDRZNS, e.ENTDRLPC, x.PDGACDPT AS PRODUCTO, x.PRSTCDGO,
       x.PDGAMNEN AS ENVIADO, x.PDGAMNDC AS DESCONTADO, x.PDGAESTD
  FROM CRD.PDGA x
  JOIN CRD.DTGA d ON d.DTGACDGO = x.DTGACDGO
  JOIN CRD.GNAP g ON g.GNAPCDGO = d.GNAPCDGO
  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = x.ENTDCDGO
 WHERE g.FLLLCDGO = 2 AND g.GNAPANPE = 2026 AND g.GNAPMSPE = 8
 ORDER BY e.ENTDNMID, x.PDGACDPT;

-- B3. Entidades de la filial ARCH: ¿tienen rol (ENTDRLPC) o solo cédula?
--     La carga actual identifica por rol; ARCH responde solo con cédula.
SELECT COUNT(*) AS TOTAL,
       SUM(CASE WHEN e.ENTDRLPC IS NULL THEN 1 ELSE 0 END) AS SIN_ROL,
       SUM(CASE WHEN e.ENTDNMID IS NULL THEN 1 ELSE 0 END) AS SIN_CEDULA,
       COUNT(DISTINCT e.ENTDNMID) AS CEDULAS_DISTINTAS
  FROM CRD.ENTD e
 WHERE e.FLLLCDGO = 2;

-- B4. Cargas que ya se hicieron con filial 2 (¿alguien cargó ARCH por la vía de Petro?)
SELECT c.CRARCDGO, c.CRARMSAF, c.CRARANAF, c.CRARESTD, c.CRARNMBR, c.CRARFCCR
  FROM CRD.CRAR c
 WHERE c.FLLLCDGO = 2
 ORDER BY c.CRARCDGO DESC;

-- ---------------------------------------------------------------------------
-- No hay bloque de reverso: este script no modifica nada.
-- ---------------------------------------------------------------------------
