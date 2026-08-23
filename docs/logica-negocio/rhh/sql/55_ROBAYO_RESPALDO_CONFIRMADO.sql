-- =====================================================
-- MODULO: RHH - EL MOTIVO DE LA NO RETENCION DE ROBAYO, YA SIN "PENDIENTE"
-- DESCRIPCION: Actualiza CNTE.CNTENRMT tras la confirmacion de Steven.
-- ORDEN DE EJECUCION: 55
-- FECHA: 2026-08-22
-- PARAMETRO: :EMPRESA -- 1236
-- DESTINO: LOCAL Y PRODUCCION
-- =====================================================
-- QUE CAMBIO. El 2026-08-22 Steven confirmo que ASOPREP SI cuenta con los
-- respaldos de la proyeccion que Robayo presento a su otro empleador, y que
-- por eso no se le retiene. Es la respuesta a la pregunta abierta del sql/47.
-- .
-- NO CAMBIA NINGUN CALCULO. CNTENRIR = 'S' ya estaba puesto y ya esta en
-- produccion: enero y febrero cerraron con cero renglones de IR y el bloque 1
-- del contraste salio vacio en los dos meses. Este script SOLO reescribe el
-- texto del motivo.
-- .
-- POR QUE MERECE UN SCRIPT, y no es formalismo: CNTENRMT es OBLIGATORIO
-- precisamente para que la excepcion no sea indistinguible de un error. Hoy
-- ese campo dice "Pendiente de Steven: copia certificada...", es decir,
-- **declara un incumplimiento que ya no existe**. Quien lea ese contrato
-- dentro de un ano encontraria una alerta viva sobre un asunto cerrado, y la
-- reaccion natural seria "des-marcarlo y empezar a retener" -- que es
-- exactamente lo contrario de lo que corresponde.
-- .
-- ES LA MISMA FAMILIA QUE EL RESTO DEL MODULO, con el signo cambiado: no un
-- dato que miente diciendo que todo esta bien, sino uno que miente diciendo
-- que algo esta mal. Un aviso que ya no aplica se termina ignorando, y con el
-- se ignoran los que si aplican.
-- .
-- OJO AL ESCRIBIR EL TEXTO: CNTENRMT es VARCHAR2(200). La primera version de
-- este script llevaba 418 caracteres y murio con ORA-12899 en produccion. El
-- motivo de abajo mide 196. Si hiciera falta mas espacio, la via es ampliar la
-- columna con un script numerado, no recortar la justificacion hasta que deje
-- de justificar nada.
-- .
-- LO QUE SIGUE SIN TOCARSE, y conviene repetirlo aqui:
--   - PYIR de Robayo se queda como esta: su proyeccion es correcta y SI causa
--     impuesto. Agosto la necesita intacta para calcular el alcance.
--   - CNTERTFN tampoco se toca: hace lo contrario de lo que dice su nombre.
--   - Enero..julio quedan en cero y es lo que ocurrio de verdad.
-- =====================================================


-- =====================================================
-- CONTROL ANTES: una sola fila, Robayo, con el motivo viejo.
-- =====================================================
SELECT m.MPLDIDNT, m.MPLDAPLL, c.CNTECDGO, c.CNTENRIR, c.CNTENRMT
  FROM RHH.CNTE c JOIN RHH.MPLD m ON m.MPLDCDGO = c.MPLDCDGO
 WHERE c.CNTENRIR = 'S' AND m.PJRQCDGO = :EMPRESA;


UPDATE RHH.CNTE
   SET CNTENRMT = 'Art. 43 LRTI: presento proyeccion a su otro empleador y ASOPREP se abstiene.'
                  || ' RESPALDO CONFIRMADO POR EL CLIENTE 2026-08-22. Revisar si deja de tener'
                  || ' otro empleador o si ASOPREP pasa a pagarle mas.'
 WHERE MPLDCDGO = (SELECT MPLDCDGO FROM RHH.MPLD
                    WHERE MPLDIDNT = '1725996498' AND PJRQCDGO = :EMPRESA);
-- Debe tocar 1 fila. Si toca 0, comprobar la cedula y la empresa.

COMMIT;


-- =====================================================
-- CONTROL DESPUES
-- Una sola fila, con 'S', el motivo nuevo y sin la palabra "Pendiente".
-- =====================================================
SELECT m.MPLDIDNT, m.MPLDAPLL, c.CNTENRIR, c.CNTENRMT,
       CASE WHEN UPPER(c.CNTENRMT) LIKE '%PENDIENTE%'
            THEN '*** SIGUE DICIENDO PENDIENTE ***' ELSE 'OK' END AS VEREDICTO
  FROM RHH.CNTE c JOIN RHH.MPLD m ON m.MPLDCDGO = c.MPLDCDGO
 WHERE c.CNTENRIR = 'S' AND m.PJRQCDGO = :EMPRESA;


-- =====================================================
-- CONTROL DE NO REGRESION: nada de esto puede haber cambiado.
-- Cero renglones de IR en los meses ya cerrados.
-- =====================================================
SELECT p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES, COUNT(*) AS RENGLONES_DE_IR
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE c.CPNMALTR = 21
 GROUP BY p.PRDNANOO, p.PRDNMSEE
 ORDER BY 1, 2;
-- Esperado: SIN FILAS.
