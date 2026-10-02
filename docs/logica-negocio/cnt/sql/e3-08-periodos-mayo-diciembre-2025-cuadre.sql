-- =====================================================================================
-- e3-08 — C1 del cuadre: periodos contables de MAYO a DICIEMBRE 2025, empresa 1236
-- Equipo omen-saa-3 · 2026-10-02 · Plan: docs/regulatorio/PLAN-CAMBIOS-SAA-OMEN3.md (C1)
--
-- ⛔ SÓLO EN LA BASE DEL PROYECTO (saa-oracle-cuadre, puerto 1522). NUNCA EN PRODUCCIÓN.
-- ⛔ ANTES de usar estos periodos hay que desplegar en esa base el WAR que ordena los periodos por
--    su fecha de inicio (PRDOINCO) y no por PRDOCDGO. Con el WAR viejo, los periodos de 2025 (que
--    nacen con PRDOCDGO mayores que los de 2026) quedan "después" de 2026: rompen la mayorización,
--    el arrastre de saldos y los balances. Crear las filas antes no rompe nada mientras nadie
--    mayorice ni genere asientos.
-- Por qué MAYO: el asiento de apertura del cuadre (C2) va al 31-may-2025.
-- Correr de corrido; el BLOQUE 1 se detiene solo si algo no cuadra.
-- =====================================================================================

-- BLOQUE 0 — CONTROLES ANTES
-- 0.1 Periodos existentes de 1236, con su fecha de inicio y fin. Todos deben tener PRDOINCO y
--     PRDOFNN coherentes con mes y año (columna FECHAS = OK): el orden nuevo depende de PRDOINCO.
SELECT p.PRDOCDGO, p.PRDOANNN, p.PRDOMSSS, p.PRDONMBR, p.PRDOESTD, p.PRDOINCO, p.PRDOFNN, p.PRDOCRRE,
       CASE WHEN p.PRDOINCO IS NULL OR p.PRDOFNN IS NULL
              OR p.PRDOINCO <> TRUNC(p.PRDOINCO, 'MM') OR p.PRDOFNN <> LAST_DAY(p.PRDOINCO)
              OR EXTRACT(MONTH FROM p.PRDOINCO) <> p.PRDOMSSS OR EXTRACT(YEAR FROM p.PRDOINCO) <> p.PRDOANNN
            THEN 'FECHAS MAL' ELSE 'OK' END AS FECHAS
  FROM CNT.PRDO p
 WHERE p.PJRQCDGO = 1236
 ORDER BY p.PRDOANNN, p.PRDOMSSS;

-- 0.2 Ningún periodo de 2025 debe existir todavía (se espera CERO)
SELECT COUNT(*) AS PERIODOS_2025 FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2025;

-- BLOQUE 1 — ALTA DE LOS 8 PERIODOS (ACTIVO = 1, sin mayorizar, no de cierre)
DECLARE
    v_existen NUMBER;
    v_malos   NUMBER;
    v_nombre  VARCHAR2(100);
    v_inicio  DATE;
    TYPE t_meses IS VARRAY(12) OF VARCHAR2(12);
    meses t_meses := t_meses('ENERO','FEBRERO','MARZO','ABRIL','MAYO','JUNIO','JULIO','AGOSTO',
                             'SEPTIEMBRE','OCTUBRE','NOVIEMBRE','DICIEMBRE');
BEGIN
    SELECT COUNT(*) INTO v_existen FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2025;
    IF v_existen > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Ya hay ' || v_existen || ' periodos de 2025 para 1236. No se crea nada.');
    END IF;

    SELECT COUNT(*) INTO v_malos FROM CNT.PRDO p
     WHERE p.PJRQCDGO = 1236
       AND (p.PRDOINCO IS NULL OR p.PRDOFNN IS NULL
            OR EXTRACT(MONTH FROM p.PRDOINCO) <> p.PRDOMSSS OR EXTRACT(YEAR FROM p.PRDOINCO) <> p.PRDOANNN);
    IF v_malos > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, v_malos || ' periodo(s) existentes tienen PRDOINCO/PRDOFNN vacíos o '
            || 'incoherentes: el orden por fecha de inicio no sería confiable. Corregirlos primero (control 0.1).');
    END IF;

    FOR m IN 5 .. 12 LOOP
        v_inicio := TO_DATE('2025-' || LPAD(m, 2, '0') || '-01', 'YYYY-MM-DD');
        -- Mismo formato de nombre que el periodo del mismo mes de 2026, si existe; si no, MES AAAA.
        BEGIN
            SELECT REPLACE(p.PRDONMBR, '2026', '2025') INTO v_nombre
              FROM CNT.PRDO p
             WHERE p.PJRQCDGO = 1236 AND p.PRDOANNN = 2026 AND p.PRDOMSSS = m AND INSTR(p.PRDONMBR, '2026') > 0;
        EXCEPTION WHEN NO_DATA_FOUND OR TOO_MANY_ROWS THEN
            v_nombre := meses(m) || ' 2025';
        END;

        INSERT INTO CNT.PRDO (PRDOCDGO, PJRQCDGO, PRDOMSSS, PRDOANNN, PRDONMBR, PRDOESTD,
                              PRDOCRRE, PRDOINCO, PRDOFNN)
        VALUES (CNT.SQ_PRDOCDGO.NEXTVAL, 1236, m, 2025, v_nombre, 1,
                0, v_inicio, LAST_DAY(v_inicio));
    END LOOP;
    COMMIT;
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS: 8 periodos de 2025 (mayo a diciembre), ACTIVO, FECHAS = OK, y en
-- orden por PRDOINCO quedan ANTES que los de 2026 aunque sus PRDOCDGO sean mayores.
SELECT p.PRDOCDGO, p.PRDOANNN, p.PRDOMSSS, p.PRDONMBR, p.PRDOESTD, p.PRDOINCO, p.PRDOFNN
  FROM CNT.PRDO p
 WHERE p.PJRQCDGO = 1236
 ORDER BY p.PRDOINCO;

-- REVERSO (comentado; sólo si no se generó ningún asiento en esos periodos)
-- SELECT COUNT(*) FROM CNT.ASNT a JOIN CNT.PRDO p ON p.PRDOCDGO = a.PRDOCDGO
--  WHERE p.PJRQCDGO = 1236 AND p.PRDOANNN = 2025;        -- debe dar 0 antes de borrar
-- DELETE FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2025;
-- COMMIT;
