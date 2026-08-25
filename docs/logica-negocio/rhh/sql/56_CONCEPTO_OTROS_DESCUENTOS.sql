-- =====================================================
-- MODULO: RHH - CONCEPTO "OTROS DESCUENTOS" (alterno 31)
-- DESCRIPCION: El concepto que faltaba para poder REGISTRAR lo que el
--              cliente descontó y nuestro motor no sabe generar.
-- ORDEN DE EJECUCION: 56   (antes de reabrir abril)
-- FECHA: 2026-08-23
-- PARAMETRO: :EMPRESA -- 1236
-- =====================================================
-- POR QUE EXISTE ESTE CONCEPTO. Decision del cliente del 2026-08-23:
-- de enero a julio la informacion se sube tomando como base lo que se PAGO
-- al IESS y lo que se PAGO al empleado. Los periodos son modo 1 HISTORICO
-- justamente por eso: son un REGISTRO de lo que paso, no un calculo a
-- reproducir. Desde agosto rigen las reglas del modulo.
-- .
-- Consecuencia: los descuentos que el rol imprime en su columna OTROS se
-- cargan aunque no sepamos que son, porque se descontaron de verdad. Hasta
-- hoy no habia donde ponerlos: los egresos del catalogo son aporte, IR,
-- retencion judicial, los tres prestamos, seguro privado, multas, faltas y
-- retencion de servicios. Ninguno es generico.
-- .
-- POR QUE UN CONCEPTO NUEVO Y NO "MULTAS Y ATRASOS". Meter 175,00 sin
-- clasificar en un concepto que afirma ser una multa es guardar un dato
-- donde no corresponde: plausible, consultable, y falso. Es la familia de
-- fallos que este modulo lleva un mes evitando. Un concepto que dice "no
-- se sabe que es" es honesto; uno que dice "multa" miente.
-- .
-- LOS IMPORTES QUE VA A LLEVAR, ya conocidos y sin preguntas pendientes:
--   Abril  Calderon Parraga                                     175,00
--   Junio  Calderon Parraga                                       0,10
--   Julio  Barcenas 1,95 - Munoz 1,53 - Nieto 2,50 - Pardo 1,95
--          - Viteri 36,67 (fondo de reserva de junio, recuperado)  44,60
-- .
-- LAS BANDERAS, Y POR QUE TODAS EN 'N'. Un descuento no forma base de
-- nada: no es imponible al IESS, no es gravado de IR, no es base de
-- decimos, ni de vacaciones, ni de utilidades. Igual que los otros diez
-- egresos del catalogo.
-- .
-- RECORTABLE = 'S', Y ES DELIBERADO. Si un neto se fuera a negativo, este
-- es el primero que debe ceder: es lo unico del rol que no responde a una
-- obligacion identificada. Con ORDN 140 queda por encima del anticipo
-- (120), y recortaDescuentos empieza por el de MAYOR orden.
-- .
-- SIN ROL DE MOTOR (CPNMROLM en NULO), Y TAMBIEN ES DELIBERADO. El rol es
-- lo que el motor consulta para GENERAR un renglon solo. Este concepto no
-- se genera nunca: se captura a mano como novedad del periodo. Ponerle rol
-- lo haria candidato de conceptoPorRol y no hay ninguna rama que lo pida.
-- NO confundir con el punto 4 de la lista de correcciones
-- (RhhTipoDescuentoRecurrente.OTROS = 7 sin rol equivalente en
-- MigracionRhhServiceImpl:764): ese camino es el de DSRC/CTDS y es de
-- agosto. Por novedad no se pasa por rolDelDescuento.
-- =====================================================


-- ---------------------------------------------------.
-- CONTROL 1: el alterno 31 tiene que estar LIBRE.
-- El motor busca por CPNMALTR y con dos filas coge una al azar o revienta.
-- Tiene que devolver CERO filas.
-- ---------------------------------------------------.
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 31;


-- ---------------------------------------------------.
-- CONTROL 2: el orden 140 tampoco puede estar ocupado, porque decide a
-- quien recorta recortaDescuentos primero. Tiene que devolver CERO filas.
-- ---------------------------------------------------.
SELECT CPNMALTR, CPNMNMBR, CPNMORDN
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMORDN = 140;


INSERT INTO RHH.CPNM (
    PJRQCDGO, CPNMALTR, CPNMNMBR, CPNMABRV,
    CPNMTPCN, CPNMTPCL, CPNMBSCL, CPNMTPRL,
    CPNMVLRR, CPNMPRCN,
    CPNMIMIE, CPNMIMIR, CPNMAPFR, CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT,
    CPNMPTRN, CPNMPRVS, CPNMOBLG, CPNMRCRT,
    CPNMORDN, CPNMESTD, CPNMUSRR
)
SELECT :EMPRESA, 31, 'Otros descuentos', 'OTRDSC',
       2, 7, 1, NULL,
       NULL, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N',
       'N', 'N', 'N', 'S',
       140, 1, 'CARGA HISTORICA'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM
                    WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 31);

COMMIT;


-- ---------------------------------------------------.
-- CONTROL DESPUES: una fila, tipo 2 EGRESO, recortable S, rol en NULO.
-- ---------------------------------------------------.
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR, CPNMABRV, CPNMTPCN AS TIPO,
       CPNMRCRT AS RECORTABLE, CPNMORDN AS ORDEN, CPNMROLM AS ROL,
       CPNMOBLG AS OBLIGATORIO, CPNMESTD AS ESTADO,
       CASE WHEN CPNMTPCN = 2 AND CPNMRCRT = 'S' AND CPNMORDN = 140
             AND CPNMROLM IS NULL AND CPNMOBLG = 'N' AND CPNMESTD = 1
            THEN 'OK' ELSE '*** REVISAR ***' END AS VEREDICTO
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 31;


-- ---------------------------------------------------.
-- CONTROL DESPUES 2: el censo de roles NO cambia. Sigue en 31 filas,
-- del 1 al 31, porque este concepto no lleva rol. Si sale 32, algo puso
-- un rol donde no debia.
-- ---------------------------------------------------.
SELECT COUNT(*) AS CONCEPTOS_CON_ROL,
       CASE WHEN COUNT(*) = 31 THEN 'OK' ELSE '*** REVISAR ***' END AS VEREDICTO
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL;
