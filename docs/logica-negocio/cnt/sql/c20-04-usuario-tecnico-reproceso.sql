-- =====================================================================================
-- c20-04 — INSERT de UNA fila en SCP.PJRQ. omen-saa-1, 2026-11-01. Paso 2 de 2 (el 1 fue c20-03).
-- Usuario técnico «REPROCESO» (decisión del usuario): el reproceso de la carga Petro graba con él los
-- periodos HASTA 2026-08; desde 2026-09 graba con el usuario real de la carga
-- (cnt/DISENO-USUARIO-OBLIGATORIO.md §8, código en el commit 9e5a2466).
-- Estructura copiada del usuario 38 «USUARIO 1» (c20-03): PGSPCDGO 9, nivel 1, padre 51 «RAIZ USUARIO».
-- NO PUEDE ENTRAR AL SISTEMA: no se le crea clave ni permisos, y no se carga en SCP.PDCN (donde vive el
-- «activo» del login y de las listas de usuarios). Solo existe por nombre, que es lo que valida
-- UsuarioValidacionService.
-- La secuencia SCP.SQ_PJRQCDGO no aparece en ALL_SEQUENCES (c20-03 bloque 4 vacío): PK = MAX + 1.
-- ⛔ VA ANTES DEL EAR ÚNICO, y TAMBIÉN en la base del reproceso (avisado al árbitro del cuadre).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: 0 filas en la primera; el 38 y el 51 como en c20-03.
SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ WHERE UPPER(PJRQNMBR) = 'REPROCESO';
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR FROM SCP.PJRQ WHERE PJRQCDGO IN (38, 51);
SELECT MAX(PJRQCDGO) + 1 AS PK_NUEVA FROM SCP.PJRQ;


-- BLOQUE 1 — (ESCRIBE) El NOT EXISTS impide duplicarlo.
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR)
SELECT (SELECT MAX(PJRQCDGO) + 1 FROM SCP.PJRQ), u.PGSPCDGO, 'REPROCESO', u.PJRQNVLL, u.PJRQCDPD, u.PJRQINGR
  FROM SCP.PJRQ u
 WHERE u.PJRQCDGO = 38
   AND NOT EXISTS (SELECT 1 FROM SCP.PJRQ x WHERE UPPER(x.PJRQNMBR) = 'REPROCESO');
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 1 fila REPROCESO con PGSPCDGO 9, nivel 1, padre 51; y NINGUNA fila
-- en PDCN para él (no puede entrar).
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR FROM SCP.PJRQ WHERE PJRQNMBR = 'REPROCESO';
SELECT COUNT(*) AS FILAS_PDCN FROM SCP.PDCN d
 WHERE d.PJRQCDGO = (SELECT PJRQCDGO FROM SCP.PJRQ WHERE PJRQNMBR = 'REPROCESO');


-- REVERSO (comentado; solo si ningún asiento lo usa todavía):
-- DELETE FROM SCP.PJRQ WHERE PJRQNMBR = 'REPROCESO';
-- COMMIT;
