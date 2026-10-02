-- =====================================================================================
-- e2-86 — Destrabar el documento 1227 (R&M WORLDTRAVEL, 325,01), marcado por error como reembolso.
-- ⚠️ EL BLOQUE 1 ESCRIBE (solo la observación del documento de la bandeja). Equipo omen-saa-2 · 2026-10-02.
--
-- Qué pasó: al registrarlo como reembolso, se creó la factura de compra 798 SIN asiento (un reembolso se
-- contabiliza después, con sus sustentos), y el documento de la bandeja (PGS.DCXP 1227) quedó en estado 2
-- con la observación «REEMBOLSO: pendiente ingreso de documentos sustento y contabilizacion». El usuario
-- desmarcó el reembolso (la casilla aparece vacía), y eso dejó la factura 798 en esReembolso = 0. Pero:
--   · «Registrar» la rechaza porque la factura 798 ya existe («El documento ya esta registrado…»);
--   · «Revertir» solo acepta documentos en estado 3 (este está en 2);
--   · «Recontabilizar», que es justo lo que hace falta (genera el asiento de una factura ya creada), el
--     backend lo acepta en estado 2, pero la pantalla SOLO muestra el botón si la observación empieza con
--     «CONTABILIDAD ANULADA» (gestion-documentos.component.ts:1123-1125).
-- Es un callejón sin salida del código, y se corrige en código. Este script solo destraba ESTE documento
-- ya, sin esperar un despliegue: cambia la observación para que aparezca el botón Recontabilizar.
--
-- DESPUÉS DE CORRERLO: en Gestión de Documentos, el documento 1227 muestra «Recontabilizar». Al apretarlo
-- se genera el asiento normal de la factura de compra y el documento pasa a Procesados.
-- Columnas copiadas de DocumentoCxp (DCXP), FacturaCompra (FCTC) y ReembolsoFacturaCompra (RMBF).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. ESPERADO: DCXP 1227 en estado 2, destino FACTURA_COMPRA 798, reembolso 0;
-- la FCTC 798 con reembolso 0 (FCTCESRM), SIN asiento (ASIENTO nulo) y sin reembolsos activos (rmbf = 0).
-- Si la FCTC sigue con FCTCESRM = 1, PARAR: primero desmarcar el reembolso en la pantalla.
SELECT 'BLOQUE 0 - bandeja' AS bloque, d.DCXPCDGO, d.DCXPESTD, d.DCXPTBTD, d.DCXPIDBD, d.DCXPESRM, d.DCXPOBSR
  FROM PGS.DCXP d WHERE d.DCXPCDGO = 1227;
SELECT 'BLOQUE 0 - factura' AS bloque, f.ID, f.NUMERO, f.TOTAL, f.FCTCESRM, f.ASIENTO,
       (SELECT COUNT(*) FROM PGS.RMBF r WHERE r.RMBFFCTC = f.ID AND r.RMBFESTD = 1) AS rmbf_activos
  FROM PGS.FCTC f WHERE f.ID = (SELECT d.DCXPIDBD FROM PGS.DCXP d WHERE d.DCXPCDGO = 1227);

-- BLOQUE 1 — Solo cambia la observación, y solo si todo está como se midió.
DECLARE
    v_estado NUMBER; v_tabla VARCHAR2(50); v_idbd NUMBER; v_esrm NUMBER;
    v_fesrm  NUMBER; v_asiento NUMBER; v_rmbf NUMBER;
BEGIN
    SELECT d.DCXPESTD, d.DCXPTBTD, d.DCXPIDBD, NVL(d.DCXPESRM, 0)
      INTO v_estado, v_tabla, v_idbd, v_esrm
      FROM PGS.DCXP d WHERE d.DCXPCDGO = 1227;
    IF v_estado <> 2 OR v_tabla <> 'FACTURA_COMPRA' OR v_idbd IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'DCXP 1227: estado ' || v_estado || ', destino ' || v_tabla || ' ' || v_idbd
            || '. Se esperaba estado 2 con una FACTURA_COMPRA. No se toca nada.');
    END IF;

    SELECT NVL(f.FCTCESRM, 0), f.ASIENTO,
           (SELECT COUNT(*) FROM PGS.RMBF r WHERE r.RMBFFCTC = f.ID AND r.RMBFESTD = 1)
      INTO v_fesrm, v_asiento, v_rmbf
      FROM PGS.FCTC f WHERE f.ID = v_idbd;
    IF v_esrm <> 0 OR v_fesrm <> 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'El documento o la factura siguen marcados como reembolso: desmarcar '
            || 'primero la casilla «Reemb.» en Gestión de Documentos. No se toca nada.');
    END IF;
    IF v_asiento IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20003, 'La factura ' || v_idbd || ' ya tiene asiento: no hace falta recontabilizar. No se toca nada.');
    END IF;
    IF v_rmbf > 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'La factura ' || v_idbd || ' tiene ' || v_rmbf || ' sustento(s) de reembolso activos. No se toca nada.');
    END IF;

    UPDATE PGS.DCXP
       SET DCXPOBSR = 'CONTABILIDAD ANULADA: se desmarco el reembolso marcado por error (e2-86, 2026-10-02) '
                   || '— pendiente de recontabilizar.'
     WHERE DCXPCDGO = 1227;
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: la observación empieza con «CONTABILIDAD ANULADA». Recargar la
-- pantalla: el documento 1227 muestra el botón «Recontabilizar».
SELECT 'BLOQUE 2 - despues' AS bloque, d.DCXPCDGO, d.DCXPESTD, d.DCXPOBSR FROM PGS.DCXP d WHERE d.DCXPCDGO = 1227;
