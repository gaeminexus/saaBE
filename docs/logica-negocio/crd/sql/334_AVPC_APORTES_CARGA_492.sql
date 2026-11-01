-- =====================================================================================
-- 334 — SOLO LECTURA. omen-saa-1, 2026-10-31.
-- Afectaciones manuales de la carga 492 que caen en filas AH o HS (las que hoy el proceso
-- IGNORA, defecto F1 de petro/DISENO-AFECTACION-MANUAL-V2.md) y si cubren TODO lo descontado
-- en su fila. Con el arreglo de esta noche (las AVPC se aplican también en AH/HS, con el mismo
-- método que las filas de préstamo), una fila cuyas AVPC NO cubren todo su descontado deja la
-- diferencia sin aplicar y la carga vuelve a abortar. Esperado: COBERTURA = 'CUBRE' en todas.
-- =====================================================================================

SELECT d.DTCACDPP AS PRODUCTO, p.PXCACDPT AS ROL, p.PXCANMBR, p.PXCACDGO,
       p.PXCADSCT AS DESCONTADO,
       SUM(CASE WHEN f.AVPCESTD = 1 THEN f.AVPCVAFA ELSE 0 END) AS AFECTADO_ACTIVO,
       LISTAGG(f.AVPCCDGO || ':' || NVL(TO_CHAR(f.TPAPCDGO), 'cuota ' || f.DTPRCDGO) || '=' || f.AVPCVAFA
               || CASE WHEN f.AVPCESTD <> 1 THEN '(inactiva)' END, ', ')
         WITHIN GROUP (ORDER BY f.AVPCCDGO) AS AFECTACIONES,
       CASE WHEN ABS(p.PXCADSCT - SUM(CASE WHEN f.AVPCESTD = 1 THEN f.AVPCVAFA ELSE 0 END)) <= 0.01
            THEN 'CUBRE' ELSE 'NO CUBRE' END AS COBERTURA
  FROM CRD.AVPC f
  JOIN CRD.NVPC n ON n.NVPCCDGO = f.NVPCCDGO
  JOIN CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 492
   AND d.DTCACDPP IN ('AH', 'HS')
 GROUP BY d.DTCACDPP, p.PXCACDPT, p.PXCANMBR, p.PXCACDGO, p.PXCADSCT
 ORDER BY p.PXCACDPT;

-- Todas las AVPC de la carga, para ver el panorama completo (préstamos incluidos).
SELECT d.DTCACDPP AS PRODUCTO, p.PXCACDPT AS ROL, f.AVPCCDGO, f.TPAPCDGO, f.DTPRCDGO,
       f.AVPCVAFA, f.AVPCESTD, f.AVPCUSAR, f.AVPCFCRG
  FROM CRD.AVPC f
  JOIN CRD.NVPC n ON n.NVPCCDGO = f.NVPCCDGO
  JOIN CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 492
 ORDER BY p.PXCACDPT, f.AVPCCDGO;
