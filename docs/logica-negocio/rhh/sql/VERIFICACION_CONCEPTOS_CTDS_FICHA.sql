-- =====================================================
-- MODULO: RHH - LAS REFERENCIAS A CPNM, Y SI ALGUNA GUARDA EL ALTERNO
-- DESCRIPCION: Lista RHH.CTDS y los conceptos de la ficha del empleado con
--              CPNMCDGO, CPNMALTR y nombre, y senala los valores que podrian
--              ser un codigo o un alterno indistintamente.
-- SOLO LECTURA. No modifica nada.
-- FECHA: 2026-08-21
-- =====================================================
-- POR QUE ESTE CONTROL EXISTE
--   RHH.CPNM tiene DOS identificadores: CPNMCDGO, la PK que asigna la
--   secuencia, y CPNMALTR, el codigo alterno de negocio. **Sus rangos se
--   solapan** --hoy los codigos van del 1 al 45 y los alternos del 1 al 68--,
--   asi que un 22 guardado en una FK es un valor VALIDO en las dos lecturas y
--   ninguna restriccion lo detecta: la FK resuelve, la fila existe, y el
--   concepto es otro.
--
--   Es el mismo modo de fallo que CRD.ENTD, donde ENTDIDST apuntaba al PK del
--   catalogo mientras el rubro usaba el codigo alterno (ver
--   docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md).
-- =====================================================


-- =====================================================
-- BLOQUE 0: EL SOLAPE, QUE ES LA CONDICION DEL DEFECTO
-- =====================================================
SELECT (SELECT MIN(CPNMCDGO) FROM RHH.CPNM) AS MIN_CDGO,
       (SELECT MAX(CPNMCDGO) FROM RHH.CPNM) AS MAX_CDGO,
       (SELECT MIN(CPNMALTR) FROM RHH.CPNM) AS MIN_ALTR,
       (SELECT MAX(CPNMALTR) FROM RHH.CPNM) AS MAX_ALTR,
       (SELECT COUNT(*) FROM RHH.CPNM a
         WHERE EXISTS (SELECT 1 FROM RHH.CPNM b WHERE b.CPNMALTR = a.CPNMCDGO
                         AND b.CPNMCDGO <> a.CPNMCDGO)) AS CDGO_AMBIGUOS
  FROM DUAL;


-- =====================================================
-- BLOQUE 1: EL MAPA DE COLISIONES
-- =====================================================
-- Para cada concepto, con que OTRO concepto se le confundiria si alguien
-- guardara su alterno donde va su codigo.
SELECT a.CPNMCDGO AS CDGO, a.CPNMALTR AS ALTR, a.CPNMNMBR AS CONCEPTO,
       b.CPNMCDGO AS CDGO_HOMONIMO, b.CPNMNMBR AS SI_SE_GUARDA_EL_ALTERNO_SALE
  FROM RHH.CPNM a JOIN RHH.CPNM b ON b.CPNMCDGO = a.CPNMALTR
 WHERE a.CPNMCDGO <> a.CPNMALTR
 ORDER BY a.CPNMALTR;


-- =====================================================
-- BLOQUE 2: RHH.CTDS -- las cuotas, via su descuento
-- =====================================================
-- CTDS no referencia CPNM directamente: lo hace a traves de DSRC. Se muestra
-- la cadena entera para que se vea de donde sale el concepto de cada cuota.
SELECT c.CTDSCDGO AS CUOTA, c.CTDSNMCT AS NRO, c.CTDSESTD AS EST,
       c.CTDSTTAL AS VALOR, c.PRDNCDGO AS PERIODO,
       d.DSRCCDGO AS DSRC, d.DSRCTPDS AS TIPO_DSC,
       p.CPNMCDGO AS CPNM_CDGO, p.CPNMALTR AS CPNM_ALTR, p.CPNMNMBR AS CONCEPTO,
       CASE WHEN p.CPNMCDGO IS NULL THEN 'FK ROTA'
            WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = d.CPNMCDGO
                           AND q.CPNMCDGO <> d.CPNMCDGO)
            THEN 'AMBIGUO: el valor tambien es alterno'
            ELSE 'ok' END AS DIAGNOSTICO
  FROM RHH.CTDS c
  JOIN RHH.DSRC d ON d.DSRCCDGO = c.DSRCCDGO
  LEFT JOIN RHH.CPNM p ON p.CPNMCDGO = d.CPNMCDGO
 ORDER BY c.CTDSCDGO;


-- =====================================================
-- BLOQUE 3: LOS CONCEPTOS FIJOS DE LA FICHA -- RHH.DSRC
-- =====================================================
SELECT d.DSRCCDGO AS DSRC, m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS EMPLEADO,
       d.DSRCTPDS AS TIPO_DSC, d.DSRCVLCT AS CUOTA, d.DSRCSLDD AS SALDO,
       p.CPNMCDGO AS CPNM_CDGO, p.CPNMALTR AS CPNM_ALTR, p.CPNMNMBR AS CONCEPTO,
       p.CPNMROLM AS ROL_MOTOR,
       CASE WHEN p.CPNMCDGO IS NULL THEN 'FK ROTA'
            WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = d.CPNMCDGO
                           AND q.CPNMCDGO <> d.CPNMCDGO)
            THEN 'AMBIGUO: el valor tambien es alterno'
            ELSE 'ok' END AS DIAGNOSTICO
  FROM RHH.DSRC d
  LEFT JOIN RHH.CPNM p ON p.CPNMCDGO = d.CPNMCDGO
  LEFT JOIN RHH.MPLD m ON m.MPLDCDGO = d.MPLDCDGO
 ORDER BY d.DSRCCDGO;


-- =====================================================
-- BLOQUE 4: BARRIDO DE LAS SIETE TABLAS QUE REFERENCIAN CPNM
-- =====================================================
-- Una fila por tabla: cuantas referencias hay, cuantas no resuelven, y
-- cuantas guardan un valor que ADEMAS es el alterno de otro concepto.
SELECT 'DSRC' AS TABLA, COUNT(*) AS FILAS,
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO)
                THEN 1 ELSE 0 END) AS FK_ROTAS,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END) AS AMBIGUAS
  FROM RHH.DSRC t
UNION ALL
SELECT 'RNGL', COUNT(*),
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END)
  FROM RHH.RNGL t
UNION ALL
SELECT 'PVNM', COUNT(*),
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END)
  FROM RHH.PVNM t
UNION ALL
SELECT 'NVNM', COUNT(*),
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END)
  FROM RHH.NVNM t
UNION ALL
SELECT 'CPXM', COUNT(*),
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END)
  FROM RHH.CPXM t
UNION ALL
SELECT 'TMLQ', COUNT(*),
       SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM RHH.CPNM p WHERE p.CPNMCDGO = t.CPNMCDGO) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM RHH.CPNM q WHERE q.CPNMALTR = t.CPNMCDGO
                               AND q.CPNMCDGO <> t.CPNMCDGO) THEN 1 ELSE 0 END)
  FROM RHH.TMLQ t
ORDER BY 1;
