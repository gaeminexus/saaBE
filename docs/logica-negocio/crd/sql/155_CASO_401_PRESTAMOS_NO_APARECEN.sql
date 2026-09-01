-- =====================================================================================
-- CASO CONCRETO — codigo Petro 401 (BUSTOS ALMEIDA LUIS GUILLERMO)
-- Por que no aparecen sus prestamos al afectar una novedad bloqueante
--
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- QUE RESPONDE:
--   El dialogo "Afectacion de valores por cuotas" muestra
--   «No se encontraron prestamos activos con cuotas pendientes para este participe».
--   Ese mensaje sale cuando la lista llega VACIA, y hay TRES filtros encadenados que
--   pueden vaciarla. Este script dice cual de los tres es, con datos.
--
--   Los tres filtros, en orden (detalle-consulta-carga.component.ts):
--     F1  el participe tiene entidad con ENTDRLPC = 401
--     F2  prestamos de esa entidad con PRSTIDST IN (2 VIGENTE, 11 EN_MORA)
--         [antes del 2026-09-01 este filtro era PRSTSLTT > 0, no por estado]
--     F3  de esos, los que tienen al menos una cuota cuyo DTPRESTD NO es
--         4 PAGADA ni 7 CANCELADA_ANTICIPADA
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cinco bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230

DEFINE ROL_PETRO = 401


-- =====================================================================================
-- BLOQUE 1 — F1: la entidad
--
-- Esperado: una fila, BUSTOS ALMEIDA LUIS GUILLERMO.
-- Si devuelve 0 filas -> el problema es el mapeo del codigo Petro, no los prestamos.
-- Si devuelve MAS de una -> la pantalla toma entidades[0] y puede estar mirando la
-- entidad equivocada. Eso seria la causa y hay que avisar.
-- =====================================================================================
SELECT  e.ENTDCDGO                                          AS ID_ENTIDAD,
        e.ENTDRLPC                                          AS ROL_PETRO,
        e.ENTDNMID                                          AS CEDULA,
        e.ENTDRZNS                                          AS NOMBRE,
        e.ENTDIDST                                          AS ESTADO_PARTICIPE
FROM    CRD.ENTD e
WHERE   e.ENTDRLPC = &ROL_PETRO;


-- =====================================================================================
-- BLOQUE 2 — F2 y F3: TODOS los prestamos del participe, y por que filtro cae cada uno
--
-- Como leerlo: la columna VEREDICTO dice, para cada prestamo, si la pantalla lo muestra
-- hoy y si no, cual de los filtros lo saca.
--
--   * "SE MUESTRA"            -> pasa los tres filtros
--   * "F2: estado excluido"   -> el prestamo no esta VIGENTE ni EN_MORA
--   * "F3: sin cuotas pend."  -> pasa el estado pero TODAS sus cuotas estan PAGADA(4)
--                                o CANCELADA_ANTICIPADA(7)
--   * "F3: sin tabla"         -> el prestamo no tiene ninguna cuota en CRD.DTPR
--
-- Tambien se muestra PRSTSLTT para ver si el filtro VIEJO (saldoTotal > 0) lo habria
-- dejado pasar: si un prestamo dice "SE MUESTRA" pero tiene PRSTSLTT = 0, entonces el
-- cambio del 2026-09-01 SI lo arregla y lo que falta es DESPLEGAR el frontend.
-- =====================================================================================
SELECT  p.PRSTCDGO                                          AS PRESTAMO,
        p.PRSTIDST                                          AS ESTADO,
        CASE p.PRSTIDST
            WHEN 1  THEN 'Generado'          WHEN 2  THEN 'Vigente'
            WHEN 3  THEN 'Cancelado'         WHEN 4  THEN 'Cancelado anticipado'
            WHEN 5  THEN 'Cancelado x novac' WHEN 8  THEN 'De plazo vencido'
            WHEN 10 THEN 'Vigente x revisar' WHEN 11 THEN 'En mora'
            ELSE 'Otro'
        END                                                 AS NOMBRE_ESTADO,
        p.PRDCCDGO                                          AS ID_PRODUCTO,
        p.PRSTSLTT                                          AS PRSTSLTT_FILTRO_VIEJO,
        (SELECT COUNT(*) FROM CRD.DTPR d
          WHERE d.PRSTCDGO = p.PRSTCDGO)                    AS CUOTAS_TOTAL,
        (SELECT COUNT(*) FROM CRD.DTPR d
          WHERE d.PRSTCDGO = p.PRSTCDGO
            AND (d.DTPRESTD IS NULL
                 OR d.DTPRESTD NOT IN (4, 7)))              AS CUOTAS_PENDIENTES,
        CASE
            WHEN p.PRSTIDST IS NULL OR p.PRSTIDST NOT IN (2, 11)
                 THEN 'F2: estado excluido'
            WHEN (SELECT COUNT(*) FROM CRD.DTPR d
                   WHERE d.PRSTCDGO = p.PRSTCDGO) = 0
                 THEN 'F3: sin tabla'
            WHEN (SELECT COUNT(*) FROM CRD.DTPR d
                   WHERE d.PRSTCDGO = p.PRSTCDGO
                     AND (d.DTPRESTD IS NULL
                          OR d.DTPRESTD NOT IN (4, 7))) = 0
                 THEN 'F3: sin cuotas pend.'
            ELSE 'SE MUESTRA'
        END                                                 AS VEREDICTO
FROM    CRD.PRST p
WHERE   p.ENTDCDGO = (SELECT MIN(e.ENTDCDGO) FROM CRD.ENTD e
                       WHERE e.ENTDRLPC = &ROL_PETRO)
ORDER   BY p.PRSTIDST, p.PRSTCDGO;


-- =====================================================================================
-- BLOQUE 3 — El detalle de las cuotas, por si el veredicto fue "F3"
--
-- Como leerlo: si todas las cuotas salen con DTPRESTD 4 o 7, el prestamo esta pagado
-- en la practica y la pantalla hace bien en no ofrecerlo. Si hay cuotas en 1/2/5/6/8
-- y aun asi no aparece, el problema NO es este filtro y hay que avisar.
--
-- ⚠️ Se mira DTPRESTD (estado), NO DTPRIDST (idEstado): son dos columnas distintas y
--    la vigente es DTPRESTD — ver CLAUDE.md, "que columna lleva realmente el estado".
-- =====================================================================================
SELECT  d.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS NRO_CUOTA,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD')                   AS VENCIMIENTO,
        d.DTPRESTD                                          AS ESTADO_CUOTA,
        CASE d.DTPRESTD
            WHEN 1 THEN 'Pendiente'  WHEN 2 THEN 'Activa'
            WHEN 3 THEN 'Emitida'    WHEN 4 THEN 'PAGADA'
            WHEN 5 THEN 'En mora'    WHEN 6 THEN 'Parcial'
            WHEN 7 THEN 'CANCELADA ANTICIPADA' WHEN 8 THEN 'Vencida'
            ELSE 'Otro/NULL'
        END                                                 AS NOMBRE_ESTADO_CUOTA,
        d.DTPRIDST                                          AS IDST_ESPEJO,
        d.DTPRTTLL                                          AS TOTAL_CUOTA,
        d.DTPRSLDO                                          AS SALDO_CUOTA
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (SELECT p.PRSTCDGO FROM CRD.PRST p
                        WHERE p.ENTDCDGO = (SELECT MIN(e.ENTDCDGO) FROM CRD.ENTD e
                                             WHERE e.ENTDRLPC = &ROL_PETRO))
ORDER   BY d.PRSTCDGO, d.DTPRNMCT;


-- =====================================================================================
-- BLOQUE 4 — Resumen de una linea: que veria la pantalla hoy
--
-- Como leerlo:
--   PASAN_FILTRO_NUEVO = 0  -> la lista sale vacia AUNQUE se despliegue el cambio.
--                              La causa es F3 (cuotas) o F2 (estados distintos de 2/11),
--                              no el filtro de saldo. AVISAR.
--   PASAN_FILTRO_NUEVO > 0 y PASAN_FILTRO_VIEJO = 0
--                           -> el cambio del 2026-09-01 lo arregla y lo unico que falta
--                              es DESPLEGAR el build del frontend.
--   Los dos > 0             -> el participe deberia estar viendo prestamos ya hoy; si no
--                              los ve, el problema esta en otro lado. AVISAR.
-- =====================================================================================
SELECT  COUNT(*)                                            AS PRESTAMOS_TOTALES,
        SUM(CASE WHEN p.PRSTIDST IN (2, 11)
                  AND (SELECT COUNT(*) FROM CRD.DTPR d
                        WHERE d.PRSTCDGO = p.PRSTCDGO
                          AND (d.DTPRESTD IS NULL
                               OR d.DTPRESTD NOT IN (4, 7))) > 0
                 THEN 1 ELSE 0 END)                         AS PASAN_FILTRO_NUEVO,
        SUM(CASE WHEN NVL(p.PRSTSLTT, 0) > 0
                  AND (SELECT COUNT(*) FROM CRD.DTPR d
                        WHERE d.PRSTCDGO = p.PRSTCDGO
                          AND (d.DTPRESTD IS NULL
                               OR d.DTPRESTD NOT IN (4, 7))) > 0
                 THEN 1 ELSE 0 END)                         AS PASAN_FILTRO_VIEJO
FROM    CRD.PRST p
WHERE   p.ENTDCDGO = (SELECT MIN(e.ENTDCDGO) FROM CRD.ENTD e
                       WHERE e.ENTDRLPC = &ROL_PETRO);


-- =====================================================================================
-- BLOQUE 5 — Generalizacion: cuantos participes de la carga estan en la misma situacion
--
-- El caso 401 puede ser particular o puede ser el patron. Esto cuenta, sobre TODOS los
-- participes que tienen alguna novedad, cuantos se quedarian sin ningun prestamo que
-- ofrecer con el filtro NUEVO.
--
-- Como leerlo: si SIN_PRESTAMOS_QUE_OFRECER es alto, el problema es estructural (la
-- cartera de esos participes esta cancelada o sin cuotas pendientes) y la solucion no
-- es tocar el filtro: es decidir que se le ofrece al operador en ese caso.
-- =====================================================================================
SELECT  COUNT(*)                                            AS PARTICIPES_CON_PRESTAMOS,
        SUM(CASE WHEN t.CON_OFERTA = 0 THEN 1 ELSE 0 END)   AS SIN_PRESTAMOS_QUE_OFRECER,
        SUM(CASE WHEN t.CON_OFERTA > 0 THEN 1 ELSE 0 END)   AS CON_PRESTAMOS_QUE_OFRECER
FROM    (
    SELECT  e.ENTDCDGO,
            SUM(CASE WHEN p.PRSTIDST IN (2, 11)
                      AND (SELECT COUNT(*) FROM CRD.DTPR d
                            WHERE d.PRSTCDGO = p.PRSTCDGO
                              AND (d.DTPRESTD IS NULL
                                   OR d.DTPRESTD NOT IN (4, 7))) > 0
                     THEN 1 ELSE 0 END)                     AS CON_OFERTA
    FROM    CRD.ENTD e
    JOIN    CRD.PRST p ON p.ENTDCDGO = e.ENTDCDGO
    WHERE   e.ENTDRLPC IS NOT NULL
    GROUP   BY e.ENTDCDGO
) t;


-- =====================================================================================
-- FIN. Pegar la salida de los cinco bloques.
-- =====================================================================================
