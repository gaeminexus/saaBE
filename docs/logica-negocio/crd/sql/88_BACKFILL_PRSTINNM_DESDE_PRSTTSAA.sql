-- =====================================================================================
-- BACKFILL DE PRSTINNM (interesNominal) DESDE PRSTTSAA (tasa)  --  defecto D10
-- FECHA: 2026-08-31   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- POR QUE
-- El proceso de mora de las 02:00 (ProcesoMoraPrestamoServiceImpl.tasaDiariaDelPrestamo,
-- lineas 328-342) lee PRSTINNM y, si viene NULL o <= 0, cae en silencio a
-- TASA_POR_DEFECTO = 9.0. La decision 11 del PLAN-SIMULADORES-PRESTAMOS.md establecio que
-- PRSTTSAA y PRSTINNM son UNA SOLA TASA, y PrestamoServiceImpl.saveSingle ya la deriva en
-- cada guardado. Pero eso solo arregla lo que se vuelva a guardar: la cartera migrada
-- nunca se vuelve a guardar, y sigue devengando mora al 9% cada noche.
--
-- La decision 14 del mismo plan ("nada retroactivo") NO cubre este caso, y el propio plan
-- lo dice: no es un registro historico congelado, es un dato que un timer sigue leyendo
-- todas las noches.
--
-- Ver docs/logica-negocio/crd/REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md §2 (D10).
--
-- ⛔ NO EJECUTAR ENTRE LAS 01:30 Y LAS 03:00: es la ventana del proceso de mora.
--
-- =====================================================================================
-- RESULTADO EN LA BASE LOCAL — 2026-08-31 (corrido por el arbitro, bloque 0 solamente)
--
--   Cartera: 5.664 prestamos, 362.762 cuotas.
--   0.1  Caen al default del 9%: 7 de 5.664. De esos, SIN NINGUNA TASA: los 7.
--   0.2  Vivos (PRSTIDST 2/8/11) que caen al default: 4.
--   0.3  Ningun prestamo en los tramos "mora sube" ni "mora baja": los 4 vivos estan
--        todos en "SIN TASA (queda en 9%)".
--   0.4  7 filas, todas con PRSTTSAA = 0 (no es tasa fuera de escala: es tasa ausente).
--   0.5  0 descuadrados: donde PRSTINNM y PRSTTSAA existen, coinciden exactamente.
--   0.b  0 cuotas duplicadas.
--   Extra: PRSTTSAA en la cartera viva = 9% en 1.305 de 1.311 prestamos.
--
-- ⇒ EL UPDATE DEL BLOQUE 2 NO TOCA NINGUNA FILA EN LOCAL. Exige PRSTTSAA > 0 y los 7
--   candidatos tienen la tasa en 0. El script queda como red de seguridad para produccion,
--   donde los numeros pueden ser otros: CORRER EL BLOQUE 0 ALLA y comparar contra estos.
--   Si dan lo mismo, no hace falta ejecutar nada mas.
--
-- ⇒ LO QUE EL SCRIPT NO PUEDE ARREGLAR, y es lo unico que queda de D10: cuatro prestamos
--   vivos sin ninguna tasa — 8157 (VIGENTE), 8078 (PLAZO VENCIDO), 8085 y 8307 (EN MORA).
--   Entre 8078 y 8307 acumulan ~8.700 de mora devengada al 9% por defecto. Que tasa les
--   corresponde es una decision de negocio. OJO: los siete tienen PRSTSLCP = PRSTMNSL
--   (saldo de capital igual al monto original) en creditos de 2005 y 2012 — revisar si no
--   son basura de migracion antes de asignarles nada.
--
-- ⇒ AVISO AL EQUIPO A: REBAJADO. La version original de este script advertia que cambiaba
--   la mora nocturna de la cartera migrada. Medido, no la cambia.
-- =====================================================================================


-- =====================================================================================
-- 0. DIAGNOSTICO — correr TODO este bloque y traer los resultados ANTES de actualizar
-- =====================================================================================

-- 0.1 Cuantos prestamos cae hoy al default silencioso del 9%.
SELECT  COUNT(*)                                                  AS TOTAL_PRESTAMOS,
        SUM(CASE WHEN p.PRSTINNM IS NULL OR p.PRSTINNM <= 0
                 THEN 1 ELSE 0 END)                               AS CAEN_AL_DEFAULT_9,
        SUM(CASE WHEN (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
                  AND (p.PRSTTSAA IS NULL OR p.PRSTTSAA <= 0)
                 THEN 1 ELSE 0 END)                               AS SIN_TASA_NI_NOMINAL
FROM    CRD.PRST p;

-- 0.2 Los mismos, pero solo los que estan VIVOS: son los unicos a los que el proceso de
--     mora les toca algo. PRSTIDST es el estado vigente (ver CLAUDE.md); ESPSCDGO es la FK
--     al catalogo y NO se usa para filtrar.
--     EstadoPrestamo: 2=VIGENTE, 8=DE_PLAZO_VENCIDO, 10=VIGENTE_POR_REVISAR, 11=EN_MORA
SELECT  p.PRSTIDST                                                AS ESTADO,
        COUNT(*)                                                  AS PRESTAMOS,
        SUM(CASE WHEN p.PRSTINNM IS NULL OR p.PRSTINNM <= 0
                 THEN 1 ELSE 0 END)                               AS CAEN_AL_DEFAULT_9
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (2, 8, 10, 11)
GROUP   BY p.PRSTIDST
ORDER   BY p.PRSTIDST;

-- 0.3 En cuanto cambia la mora. Reparto de los afectados por tramo de tasa real:
--     los de PRSTTSAA > 9 van a devengar MAS que hoy; los de < 9, MENOS.
SELECT  CASE WHEN p.PRSTTSAA IS NULL OR p.PRSTTSAA <= 0 THEN 'SIN TASA (queda en 9%)'
             WHEN p.PRSTTSAA <  9 THEN 'MENOR A 9  (la mora BAJA)'
             WHEN p.PRSTTSAA =  9 THEN 'IGUAL A 9  (sin cambio)'
             ELSE                      'MAYOR A 9  (la mora SUBE)'
        END                                                       AS TRAMO,
        COUNT(*)                                                  AS PRESTAMOS,
        MIN(p.PRSTTSAA)                                           AS TASA_MIN,
        MAX(p.PRSTTSAA)                                           AS TASA_MAX
FROM    CRD.PRST p
WHERE  (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
AND     p.PRSTIDST IN (2, 8, 10, 11)
GROUP   BY CASE WHEN p.PRSTTSAA IS NULL OR p.PRSTTSAA <= 0 THEN 'SIN TASA (queda en 9%)'
                WHEN p.PRSTTSAA <  9 THEN 'MENOR A 9  (la mora BAJA)'
                WHEN p.PRSTTSAA =  9 THEN 'IGUAL A 9  (sin cambio)'
                ELSE                      'MAYOR A 9  (la mora SUBE)'
           END
ORDER   BY 1;

-- 0.4 Tasas fuera de rango razonable. Si aparece algo aca, PARAR: significa que PRSTTSAA
--     no es homogenea (algun origen guardo la tasa como fraccion 0.12 en vez de 12).
--     Esperado: 0 filas.
SELECT  p.PRSTCDGO, p.PRSTTSAA, p.PRSTINNM, p.PRSTIDST
FROM    CRD.PRST p
WHERE  (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
AND     p.PRSTTSAA IS NOT NULL
AND    (p.PRSTTSAA < 1 OR p.PRSTTSAA > 60)
ORDER   BY p.PRSTTSAA;

-- 0.5 Los que ya tienen PRSTINNM pero NO coincide con PRSTTSAA. El script NO los toca:
--     solo se listan para saber si existen y decidir aparte. Esperado (ideal): 0 filas.
SELECT  p.PRSTCDGO, p.PRSTTSAA, p.PRSTINNM,
        p.PRSTINNM - p.PRSTTSAA                                   AS DIFERENCIA,
        p.PRSTIDST
FROM    CRD.PRST p
WHERE   p.PRSTINNM IS NOT NULL AND p.PRSTINNM > 0
AND     p.PRSTTSAA IS NOT NULL AND p.PRSTTSAA > 0
AND     ABS(p.PRSTINNM - p.PRSTTSAA) > 0.0001
ORDER   BY ABS(p.PRSTINNM - p.PRSTTSAA) DESC
FETCH FIRST 50 ROWS ONLY;


-- =====================================================================================
-- 0.b DIAGNOSTICO APARTE — no es del backfill, es del defecto N1 (tablas duplicadas)
--     Ver REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md §3 (N1) y §4.
--     generarTablaAmortizacion no borra la tabla anterior: dos clics duplican las cuotas.
--     Esta consulta dice si ya le paso a alguien. Esperado: 0 filas.
-- =====================================================================================
SELECT  d.PRSTCDGO,
        d.DTPRNMCT                                                AS NUMERO_CUOTA,
        COUNT(*)                                                  AS VECES
FROM    CRD.DTPR d
GROUP   BY d.PRSTCDGO, d.DTPRNMCT
HAVING  COUNT(*) > 1
ORDER   BY VECES DESC, d.PRSTCDGO
FETCH FIRST 100 ROWS ONLY;


-- =====================================================================================
-- 1. RESPALDO — obligatorio, y se conserva hasta que la mora de la noche siguiente cuadre
-- =====================================================================================
CREATE TABLE CRD.BKP_PRSTINNM_20260831 AS
SELECT  p.PRSTCDGO, p.PRSTINNM, p.PRSTTSAA, p.PRSTIDST, SYSDATE AS FECHA_RESPALDO
FROM    CRD.PRST p
WHERE   p.PRSTINNM IS NULL OR p.PRSTINNM <= 0;

-- Control: tiene que dar el mismo numero que CAEN_AL_DEFAULT_9 del bloque 0.1.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_PRSTINNM_20260831;


-- =====================================================================================
-- 2. BACKFILL
--
-- Solo toca filas donde PRSTINNM esta vacia o no es positiva Y PRSTTSAA si lo es.
-- Los prestamos sin ninguna de las dos tasas NO se tocan: no hay de donde sacar el numero,
-- y ponerles 9 a mano seria fijar como dato el default que este script existe para quitar.
-- Salen en el bloque 0.1 (SIN_TASA_NI_NOMINAL) y se resuelven caso por caso con credito.
-- =====================================================================================
UPDATE  CRD.PRST p
SET     p.PRSTINNM = p.PRSTTSAA
WHERE  (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
AND     p.PRSTTSAA IS NOT NULL
AND     p.PRSTTSAA > 0;

-- Revisar el numero de filas afectadas contra el bloque 0.1 ANTES de confirmar:
--   filas actualizadas == CAEN_AL_DEFAULT_9 - SIN_TASA_NI_NOMINAL
-- Si no coincide, ROLLBACK y avisar.

-- COMMIT;      <-- descomentar y ejecutar SOLO despues de que el control de arriba cuadre


-- =====================================================================================
-- 3. VERIFICACION POSTERIOR
-- =====================================================================================

-- 3.1 Ya no debe quedar ningun prestamo VIVO con tasa real cayendo al default.
--     Esperado: solo los SIN_TASA_NI_NOMINAL del bloque 0.1.
SELECT  COUNT(*) AS SIGUEN_EN_DEFAULT
FROM    CRD.PRST p
WHERE  (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
AND     p.PRSTIDST IN (2, 8, 10, 11);

-- 3.2 Las dos tasas coinciden en todo lo que se toco. Esperado: 0 filas.
SELECT  p.PRSTCDGO, p.PRSTTSAA, p.PRSTINNM
FROM    CRD.PRST p
JOIN    CRD.BKP_PRSTINNM_20260831 b ON b.PRSTCDGO = p.PRSTCDGO
WHERE   b.PRSTTSAA IS NOT NULL AND b.PRSTTSAA > 0
AND     ABS(NVL(p.PRSTINNM, -1) - p.PRSTTSAA) > 0.0001;

-- 3.3 Al dia siguiente: en el log de WildFly ya NO deberia aparecer la traza
--     "sin interesNominal (PRSTINNM); se usa el default silencioso" salvo para los
--     prestamos sin ninguna tasa. Si aparece para otros, avisar al arbitro.


-- =====================================================================================
-- 4. ROLLBACK (si la mora de la noche siguiente no cuadra)
-- =====================================================================================
-- UPDATE CRD.PRST p
-- SET    p.PRSTINNM = (SELECT b.PRSTINNM FROM CRD.BKP_PRSTINNM_20260831 b
--                      WHERE  b.PRSTCDGO = p.PRSTCDGO)
-- WHERE  EXISTS (SELECT 1 FROM CRD.BKP_PRSTINNM_20260831 b WHERE b.PRSTCDGO = p.PRSTCDGO);
-- COMMIT;

-- No hay secuencias que sincronizar: este script no inserta claves explicitas.
