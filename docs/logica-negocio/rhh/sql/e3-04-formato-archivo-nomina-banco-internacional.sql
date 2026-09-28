-- =====================================================================================
-- e3-04 — Formato del archivo bancario de NÓMINA para el Banco Internacional (RHH.FMBN/DFMB)
-- Equipo omen-saa-3 · 2026-09-28
--
-- Por qué: RRHH → Órdenes de pago → descargar archivo falla con
--   «La empresa no tiene un formato de archivo bancario activo (RHH.FMBN)».
-- El formato del Internacional que existe está cargado en TESORERÍA, que usa OTRO motor
-- (tsr/formateador/InternacionalArchivoPagoFormateador). La nómina usa el motor
-- parametrizable de RRHH, que está vacío. Este script le carga el mismo formato del banco:
-- «Estructura de archivo CORTO» — docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md §1.
--
--   TAB, sin cabecera, 12 campos, ANSI (windows-1252):
--   1 PA · 2 contrapartida · 3 USD · 4 valor en centavos sin separador · 5 CTA
--   6 AHO/CTE · 7 cuenta · 8 referencia · 9 C · 10 cédula · 11 nombre (máx 41) · 12 código BCE
--
-- ⛔ EL CAMPO 12 NECESITA EL WAR NUEVO. Con el WAR anterior el motor escribe el NOMBRE del
-- banco en el campo 12 y el Internacional rechaza el archivo. El WAR nuevo pone el código BCE
-- (TSR.BEXT.BEXTTRJT) y recorta el nombre a 41. Este script se puede correr ANTES o DESPUÉS
-- del WAR (no hay DDL), pero el archivo sólo sirve con los dos.
--
-- Correr de corrido. El BLOQUE 1 se detiene solo si algún control falla.
-- =====================================================================================

-- BLOQUE 0 — CONTROLES DE LECTURA (pegar la salida)

-- 0.1 Empresa de la orden de septiembre (debe salir UNA fila)
SELECT r.RDPGCDGO, r.RDPGNMRO, r.PJRQCDGO FROM RHH.RDPG r WHERE r.RDPGNMRO = 'OP-202609';

-- 0.2 Formatos que ya existan para esa empresa (se espera CERO activos)
SELECT f.FMBNCDGO, f.FMBNNMBR, f.FMBNESTD
  FROM RHH.FMBN f
 WHERE f.PJRQCDGO = (SELECT r.PJRQCDGO FROM RHH.RDPG r WHERE r.RDPGNMRO = 'OP-202609');

-- 0.3 Cada beneficiario de la orden: banco, código BCE, tipo de cuenta y cédula
--     Una fila con CODIGO_BCE nulo hará fallar el archivo (a propósito) hasta que se cargue
--     en Tesorería → Bancos. TIPO_CUENTA debe ser 1 (ahorro) o 2 (corriente).
SELECT d.DRPGNMBN               AS BENEFICIARIO,
       d.DRPGIDNT               AS IDENTIFICACION,
       LENGTH(d.DRPGIDNT)       AS LARGO_ID,
       d.DRPGBNCO               AS BANCO_SNAPSHOT,
       b.BEXTTRJT               AS CODIGO_BCE,
       d.DRPGTPCT               AS TIPO_CUENTA,
       LENGTH(d.DRPGNMBN)       AS LARGO_NOMBRE,
       d.DRPGVLOR               AS VALOR
  FROM RHH.DRPG d
  JOIN RHH.RDPG r      ON r.RDPGCDGO = d.RDPGCDGO
  LEFT JOIN RHH.CBEM c ON c.CBEMCDGO = d.CBEMCDGO
  LEFT JOIN TSR.BEXT b ON b.BEXTCDGO = c.BEXTCDGO
 WHERE r.RDPGNMRO = 'OP-202609'
 ORDER BY d.DRPGNMBN;

-- BLOQUE 1 — ALTA DEL FORMATO (escribe; hace COMMIT al final)
DECLARE
    v_empresa  NUMBER;
    v_activos  NUMBER;
    v_formato  NUMBER;
    v_usuario  CONSTANT VARCHAR2(60) := 'e3-04';

    PROCEDURE campo(p_orden NUMBER, p_campo NUMBER, p_fijo VARCHAR2 := NULL,
                    p_long NUMBER := NULL, p_dec NUMBER := NULL, p_sep VARCHAR2 := NULL) IS
    BEGIN
        INSERT INTO RHH.DFMB (FMBNCDGO, DFMBCMPO, DFMBORDN, DFMBLNGT, DFMBDCML, DFMBSPDC,
                              DFMBVLFJ, DFMBESTD, DFMBFCHR, DFMBUSRR)
        VALUES (v_formato, p_campo, p_orden, p_long, p_dec, NVL(p_sep, 'N'),
                p_fijo, 1, SYSTIMESTAMP, v_usuario);
    END;
BEGIN
    SELECT r.PJRQCDGO INTO v_empresa FROM RHH.RDPG r WHERE r.RDPGNMRO = 'OP-202609';

    SELECT COUNT(*) INTO v_activos FROM RHH.FMBN f
     WHERE f.PJRQCDGO = v_empresa AND f.FMBNESTD = 1;
    IF v_activos > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Ya hay ' || v_activos || ' formato(s) activo(s) para la '
            || 'empresa ' || v_empresa || '. No se crea otro: revisar el control 0.2.');
    END IF;

    INSERT INTO RHH.FMBN (PJRQCDGO, FMBNNMBR, FMBNBNCO, FMBNTPFR, FMBNDLMT, FMBNEXTN,
                          FMBNCDFC, FMBNFRFC, FMBNCBCR, FMBNPIEE, FMBNMPTC,
                          FMBNESTD, FMBNFCHR, FMBNUSRR)
    VALUES (v_empresa, 'BANCO INTERNACIONAL - ARCHIVO CORTO PAGOS', 'BANCO INTERNACIONAL',
            2,             -- RhhFormatoArchivoMarcacion.DELIMITADO
            CHR(9),        -- tabulador
            'txt', 'windows-1252', NULL, NULL, NULL,
            '1=AHO;2=CTE', -- alterno del tipo de cuenta del empleado -> código del banco
            1, SYSTIMESTAMP, v_usuario)
    RETURNING FMBNCDGO INTO v_formato;

    -- Campos del rubro 224 (RhhCampoArchivoBancario):
    -- 1 SECUENCIAL · 2 IDENTIFICACION · 3 NOMBRE · 4 NUMERO_CUENTA · 5 TIPO_CUENTA
    -- 6 CODIGO_BANCO · 7 VALOR · 8 MONEDA · 9 REFERENCIA · 10 FECHA · 11 LITERAL_FIJO
    campo( 1, 11, 'PA');                 -- código de orientación: pagos
    campo( 2,  1);                       -- contrapartida: secuencial de la línea
    campo( 3,  8, 'USD');                -- moneda (el motor lee el literal del campo)
    campo( 4,  7, NULL, NULL, 2, 'N');   -- valor: 2 decimales SIN separador = centavos corridos
    campo( 5, 11, 'CTA');                -- forma de pago: crédito a cuenta
    campo( 6,  5);                       -- tipo de cuenta -> AHO/CTE por FMBNMPTC
    campo( 7,  4);                       -- número de cuenta
    campo( 8,  9);                       -- referencia: número de la orden (OP-AAAAMM)
    campo( 9, 11, 'C');                  -- tipo de identificación: cédula
    campo(10,  2);                       -- número de identificación
    campo(11,  3, NULL, 41);             -- nombre, máximo 41 (el recorte lo hace el WAR nuevo)
    campo(12,  6);                       -- código BCE del banco (lo resuelve el WAR nuevo)

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Formato creado: FMBNCDGO = ' || v_formato || ' para la empresa ' || v_empresa);
END;
/

-- BLOQUE 2 — CONTROL DESPUÉS (se esperan 1 formato activo y 12 campos, órdenes 1..12)
SELECT f.FMBNCDGO, f.FMBNNMBR, f.FMBNTPFR, ASCII(f.FMBNDLMT) AS DELIM_ASCII_9,
       f.FMBNCDFC, f.FMBNMPTC, f.FMBNESTD,
       (SELECT COUNT(*) FROM RHH.DFMB d WHERE d.FMBNCDGO = f.FMBNCDGO) AS CAMPOS
  FROM RHH.FMBN f
 WHERE f.FMBNUSRR = 'e3-04';

SELECT d.DFMBORDN, d.DFMBCMPO, d.DFMBVLFJ, d.DFMBLNGT, d.DFMBDCML, d.DFMBSPDC
  FROM RHH.DFMB d
 WHERE d.FMBNCDGO = (SELECT f.FMBNCDGO FROM RHH.FMBN f WHERE f.FMBNUSRR = 'e3-04')
 ORDER BY d.DFMBORDN;

-- REVERSO (comentado; sólo si hubiera que deshacerlo)
-- DELETE FROM RHH.DFMB WHERE FMBNCDGO IN (SELECT FMBNCDGO FROM RHH.FMBN WHERE FMBNUSRR = 'e3-04');
-- DELETE FROM RHH.FMBN WHERE FMBNUSRR = 'e3-04';
-- COMMIT;
