-- =====================================================================================
-- 240 - TIPO DE ADJUNTO PROPIO PARA EL CERTIFICADO DE UN BENEFICIARIO (CRD.TPDJ)
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA de cada sentencia, nunca intercalados.
--
-- =====================================================================================
-- ⛔ POR QUE HACE FALTA: UN DEFECTO QUE INTRODUJIMOS HOY CON LA FASE 2a
-- =====================================================================================
-- Los adjuntos se resuelven SOLO por (ADJNIDRF, TPDJCDGO)
-- (AdjuntoDaoServiceImpl.selectByReferenciaYTipo:23-34). En CRD.ADJN no hay ninguna
-- columna que diga DE QUE TABLA viene ese ADJNIDRF.
--
-- Y CuentaBancariaBeneficiarioServiceImpl guarda el certificado con
--     idReferencia = codigo del BENEFICIARIO (CRD.CBBP)
--     tipoAdjunto  = 'CERTIFICADO BANCARIO'   <- el MISMO que usa CRD.CNBP
--
-- ⇒ El beneficiario 1 y la cuenta bancaria de participe 1 comparten clave. El primer
--   beneficiario que se cargue va a devolver (o a ser devuelto como) el certificado de
--   una cuenta bancaria existente. CRD.CBBP arranca en 1 y CRD.CNBP tiene cientos de
--   filas con esos codigos bajos, asi que la colision no es un caso raro: es el PRIMER
--   registro.
--
-- Esta semana ya se pago el precio de un tipo de adjunto mal resuelto (H74: 410
-- certificados invisibles en 273 cuentas). Esta vez se corrige ANTES de que alguien
-- cargue el primer beneficiario.
--
-- ⚠️ TODAVIA NO HAY NINGUN BENEFICIARIO CARGADO (CRD.CBBP se creo hoy y esta vacia), asi
--    que no hay datos que migrar. El bloque 0.2 lo confirma antes de seguir.
--
-- ALTERNATIVA DESCARTADA: agregar una columna de origen a CRD.ADJN. Es DDL sobre una
-- tabla que usan varios frentes y obligaria a tocar la query que hoy usa todo el modulo.
-- Un tipo propio no cambia ni una estructura ni una linea de la busqueda existente.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 El nombre nuevo no puede existir ya. Esperado: 0 filas.
SELECT t.TPDJCDGO, t.TPDJNMBR, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO';

-- 0.2 No debe haber ningun beneficiario cargado todavia. Esperado: 0.
--     Si devuelve algo, PARAR: hay certificados ya guardados con el tipo equivocado y
--     antes de crear el tipo nuevo hay que decidir como se reapuntan esos adjuntos.
SELECT COUNT(*) AS BENEFICIARIOS_CARGADOS FROM CRD.CBBP;

-- 0.3 Foto del catalogo, para comparar despues.
SELECT t.TPDJCDGO, t.TPDJNMBR, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(t.TPDJNMBR) LIKE '%CERTIFICAD%BANCARIO%'
 ORDER BY t.TPDJCDGO;


-- =====================================================================================
-- 1. LA FILA NUEVA
-- =====================================================================================
-- Con guarda NOT EXISTS, que es la leccion del incidente de esta semana: un script que
-- escribe no puede depender de que alguien lea el control de arriba. Correrlo dos veces
-- no duplica nada. Si dice "0 filas insertadas", la fila ya estaba.
--
-- El nombre es distinto de 'CERTIFICADO BANCARIO', asi que NO choca con el indice unico
-- UX_TPDJ_NOMBRE_ACTIVO creado en el sql/235.

INSERT INTO CRD.TPDJ (TPDJNMBR, TPDJIDST)
SELECT 'CERTIFICADO BANCARIO BENEFICIARIO', 1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM CRD.TPDJ t
                    WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO');


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 Esperado: EXACTAMENTE 1 fila, activa. El backend la resuelve por igualdad exacta
--     sobre el nombre y exige una sola activa, igual que la de las cuentas bancarias.
SELECT t.TPDJCDGO, t.TPDJNMBR, LENGTH(t.TPDJNMBR) AS LARGO, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO'
   AND t.TPDJIDST = 1;

-- 2.2 Y que la de las cuentas bancarias siga intacta y unica. Esperado: 1 fila, el id 4.
SELECT t.TPDJCDGO, t.TPDJNMBR, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
   AND t.TPDJIDST = 1;


-- =====================================================================================
-- 3. ORDEN DE DESPLIEGUE
-- =====================================================================================
-- Este script va ANTES del WAR que trae el cambio de codigo. Si el WAR sube primero, el
-- alta de un beneficiario va a responder TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO, que es
-- un fallo limpio y avisado: no graba nada y no corrompe ningun adjunto ajeno.
-- Es el lado seguro de equivocarse en el orden, a diferencia de lo que paso con CBBP.


-- =====================================================================================
-- 4. REVERSO — comentado
-- =====================================================================================
-- Solo si no se cargo ningun certificado de beneficiario todavia:
--
-- UPDATE CRD.TPDJ SET TPDJIDST = 0
--  WHERE UPPER(TRIM(TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO';
-- COMMIT;
