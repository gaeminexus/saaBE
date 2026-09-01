-- =====================================================================================
-- DIAGNOSTICO — PRSTSLTT (saldoTotal) y la lista de prestamos afectables de Petro
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- QUE RESPONDE:
--   El usuario reporto que en archivo-petro/carga/detalle, pestana descuentos, al
--   afectar una novedad BLOQUEANTE solo aparecen los prestamos VIGENTES y no los que
--   estan EN MORA.
--
--   La causa encontrada leyendo el codigo: la pantalla
--   (detalle-consulta-carga.component.ts:2408 y :2419) filtra la lista de prestamos
--   afectables por  saldoTotal > 0  — es decir, por la columna CRD.PRST.PRSTSLTT —
--   y NO por el estado del prestamo.
--
--   Y PRSTSLTT no la actualiza NADIE: en todo el backend existe el setter
--   Prestamo.setSaldoTotal() y CERO llamadas. Es un dato migrado y congelado.
--   (La pantalla de cobros personales ya habia abandonado esa columna por poco
--   fiable — ver el comentario de cobros-personales.component.ts:292 — pero la de
--   Petro nunca se actualizo.)
--
--   Este script mide cuantos prestamos quedan invisibles por ese filtro.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220


-- =====================================================================================
-- BLOQUE 1 — LA MEDICION QUE IMPORTA: cuantos prestamos ve y cuantos pierde el filtro
--
-- Como leerlo: la columna INVISIBLES es la respuesta. Son los prestamos que EXISTEN y
-- que la pantalla NO muestra hoy, porque su PRSTSLTT es 0 o NULL.
--
--   * Si INVISIBLES es alto en el estado 11 (EN MORA) -> queda explicado el sintoma
--     que reporto el usuario, y la correccion (filtrar por estado en vez de por
--     PRSTSLTT) lo arregla.
--   * Si INVISIBLES es 0 en el estado 11 -> la causa es OTRA y hay que seguir
--     buscando. AVISAR, no aplicar la correccion a ciegas.
--   * Mirar tambien los estados 3 y 4 (cancelados): si tienen PRSTSLTT > 0, la
--     pantalla les esta ofreciendo al operador prestamos YA CANCELADOS. Es el otro
--     lado del mismo campo muerto.
-- =====================================================================================
SELECT  p.PRSTIDST                                          AS ESTADO,
        CASE p.PRSTIDST
            WHEN 1  THEN 'Generado'          WHEN 2  THEN 'Vigente'
            WHEN 3  THEN 'Cancelado'         WHEN 4  THEN 'Cancelado anticipado'
            WHEN 5  THEN 'Cancelado x novac' WHEN 8  THEN 'De plazo vencido'
            WHEN 10 THEN 'Vigente x revisar' WHEN 11 THEN 'En mora'
            ELSE 'Otro'
        END                                                 AS NOMBRE_ESTADO,
        COUNT(*)                                            AS PRESTAMOS,
        SUM(CASE WHEN NVL(p.PRSTSLTT,0) > 0
                 THEN 1 ELSE 0 END)                         AS VISIBLES_HOY,
        SUM(CASE WHEN NVL(p.PRSTSLTT,0) <= 0
                 THEN 1 ELSE 0 END)                         AS INVISIBLES,
        SUM(CASE WHEN p.PRSTSLTT IS NULL
                 THEN 1 ELSE 0 END)                         AS PRSTSLTT_NULL,
        ROUND(SUM(NVL(p.PRSTSLTT,0)), 2)                    AS SUMA_PRSTSLTT
FROM    CRD.PRST p
GROUP   BY p.PRSTIDST
ORDER   BY p.PRSTIDST;


-- =====================================================================================
-- BLOQUE 2 — El contraste: PRSTSLTT contra el saldo REAL de las cuotas
--
-- El saldo de verdad se reconstruye desde las cuotas (que es lo que hace
-- SaldoPrestamoService en el frontend). Aca se compara contra la columna congelada,
-- solo sobre la cartera viva (2, 8, 11).
--
-- Como leerlo: DESFASADOS son prestamos donde la columna dice una cosa y las cuotas
-- otra. El caso grave es COLUMNA_CERO_CUOTAS_DEBEN: la columna dice 0 (asi que la
-- pantalla lo esconde) pero las cuotas tienen saldo pendiente de verdad.
-- =====================================================================================
SELECT  p.PRSTIDST                                          AS ESTADO,
        COUNT(*)                                            AS PRESTAMOS,
        SUM(CASE WHEN NVL(p.PRSTSLTT,0) <= 0
                  AND NVL((SELECT SUM(d.DTPRSLDO) FROM CRD.DTPR d
                            WHERE d.PRSTCDGO = p.PRSTCDGO), 0) > 0
                 THEN 1 ELSE 0 END)                         AS COLUMNA_CERO_CUOTAS_DEBEN,
        SUM(CASE WHEN NVL(p.PRSTSLTT,0) > 0
                  AND NVL((SELECT SUM(d.DTPRSLDO) FROM CRD.DTPR d
                            WHERE d.PRSTCDGO = p.PRSTCDGO), 0) <= 0
                 THEN 1 ELSE 0 END)                         AS COLUMNA_DEBE_CUOTAS_CERO
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (2, 8, 11)
GROUP   BY p.PRSTIDST
ORDER   BY p.PRSTIDST;


-- =====================================================================================
-- BLOQUE 3 — Muestra concreta: prestamos EN MORA que la pantalla esconde hoy
--
-- Para poder abrir uno en la pantalla y confirmar el sintoma con un caso real.
-- Como leerlo: si esta lista trae filas, cada una es un prestamo en mora que el
-- operador NO puede elegir hoy al afectar una novedad bloqueante.
-- =====================================================================================
SELECT  p.PRSTCDGO                                          AS PRESTAMO,
        p.ENTDCDGO                                          AS ID_PARTICIPE,
        e.ENTDNMID                                          AS CEDULA,
        SUBSTR(e.ENTDRZNS, 1, 35)                           AS PARTICIPE,
        p.PRSTIDST                                          AS ESTADO,
        p.PRSTSLTT                                          AS PRSTSLTT,
        NVL((SELECT SUM(d.DTPRSLDO) FROM CRD.DTPR d
              WHERE d.PRSTCDGO = p.PRSTCDGO), 0)            AS SALDO_SEGUN_CUOTAS,
        TO_CHAR(p.PRSTFCIN, 'YYYY-MM-DD')                   AS FECHA_INICIO
FROM    CRD.PRST p
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   p.PRSTIDST = 11
  AND   NVL(p.PRSTSLTT, 0) <= 0
ORDER   BY p.PRSTCDGO
FETCH FIRST 30 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — El otro lado: cancelados que la pantalla SI ofrece hoy
--
-- Como leerlo: cada fila es un prestamo YA CANCELADO que hoy aparece en la lista de
-- afectables, porque su PRSTSLTT quedo con un valor viejo. Filtrar por estado —la
-- correccion propuesta— tambien cierra este agujero, que nadie habia reportado.
-- =====================================================================================
SELECT  COUNT(*)                                            AS CANCELADOS_OFRECIDOS,
        ROUND(SUM(NVL(p.PRSTSLTT,0)), 2)                    AS SUMA_PRSTSLTT
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (3, 4, 5)
  AND   NVL(p.PRSTSLTT, 0) > 0;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================
