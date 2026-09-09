-- =====================================================================
-- e2-39 — Por que la retencion 216 salio declarando CEDULA con un RUC
-- Modulo: cxc  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-09
--
-- SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
-- Correr entero y pegar la salida completa.
--
-- POR QUE EXISTE
--   El SRI rechazo la retencion 216 (clave 0409...7811):
--     identificador 69 - ERROR EN LA IDENTIFICACION DEL RECEPTOR
--     "La longitud del numero de cedula debe ser 10"
--   El XML salio con <tipoIdentificacionSujetoRetenido>05</> (=CEDULA) y
--   <identificacionSujetoRetenido>1716218670001</> (13 digitos = RUC).
--
--   RetencionV2ServiceImpl:361 arranca con el valor por defecto "05" y solo
--   lo cambia si logra leer el valorAlfanumerico del catalogo:
--
--     String tipoIdentificacionSujetoRetenido = "05";   // por defecto: cedula
--     try { ... selectValorStringByRubAltDetAlt(P, H) ... }
--     catch (Throwable e) { System.err.println("...") }  // se traga el error
--
--   O sea que hay DOS causas posibles y hay que distinguirlas:
--     A) el titular esta clasificado como cedula (PDTRALTR/H = 1) teniendo RUC
--     B) el catalogo no devuelve valor -fila ausente, o PDTRVLRV vacio- y
--        el default "05" sale igual, en silencio, para TODAS las retenciones
--
--   La B es la grave: selectValorStringByRubAltDetAlt usa getSingleResult(),
--   asi que una fila ausente lanza NoResultException, el catch la absorbe y
--   el comprobante viaja al SRI con un tipo de identificacion ADIVINADO.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El catalogo del rubro 36 (tipo de identificacion)
-- ESPERADO SI ESTA BIEN: una fila por tipo, con PDTRALTR = el codigo interno
-- (1=cedula, 2=RUC, 3=pasaporte) y VALOR_ALFANUMERICO = el codigo del SRI
-- ("05" cedula, "04" RUC, "06" pasaporte, "08" exterior).
--
-- SI VALOR_ALFANUMERICO VIENE NULL O VACIO EN LA FILA DEL RUC, esa es la
-- causa B: todas las retenciones a proveedores con RUC salen como cedula.
-- SI FALTA LA FILA DEL RUC ENTERA, idem, y ademas explica el catch mudo.
-- ---------------------------------------------------------------------
select 'BLOQUE 1 - catalogo rubro 36' as bloque,
       r.PRBRALTR   as rubro_alterno,
       r.PRBRDSCR   as rubro,
       d.PDTRCDGO   as detalle_pk,
       d.PDTRALTR   as detalle_alterno,
       d.PDTRDSCR   as detalle,
       d.PDTRVLRV   as valor_alfanumerico,
       d.PDTRESTD   as estado
  from SCP.PDTR d
  join SCP.PRBR r on r.PRBRCDGO = d.PRBRCDGO
 where r.PRBRALTR = 36
 order by d.PDTRALTR;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Como esta clasificado el titular que el SRI rechazo
-- ESPERADO SI ESTA BIEN: TIPO_REAL = 2 (RUC), porque la identificacion tiene
-- 13 digitos. Si viene 1 (cedula), esa es la causa A y se corrige el dato
-- del titular, no el codigo.
-- ---------------------------------------------------------------------
select 'BLOQUE 2 - titular rechazado' as bloque,
       t.TTLRCDGO   as id_titular,
       t.TTLRNMBR   as nombre,
       t.TTLRIDNT   as identificacion,
       length(t.TTLRIDNT) as largo,
       t.TTLRRYYB   as padre_36,
       t.TTLRRZZB   as tipo_real
  from TSR.TTLR t
 where t.TTLRIDNT = '1716218670001';


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Cuantos titulares mas quedarian mal declarados
-- ESPERADO: idealmente cero filas. Cada fila es un titular cuyo tipo de
-- identificacion NO concuerda con la longitud de su identificacion, o sea
-- una retencion que el SRI va a rechazar con el mismo identificador 69.
-- Se mide ahora para no ir descubriendolos de a uno.
-- ---------------------------------------------------------------------
select 'BLOQUE 3 - incoherentes' as bloque,
       t.TTLRCDGO as id_titular,
       t.TTLRNMBR as nombre,
       t.TTLRIDNT as identificacion,
       length(t.TTLRIDNT) as largo,
       t.TTLRRZZB as tipo_real,
       case when t.TTLRRZZB = 1 then 'dice CEDULA (espera 10)'
            when t.TTLRRZZB = 2 then 'dice RUC (espera 13)'
            else 'otro tipo' end as declara
  from TSR.TTLR t
 where t.TTLRIDNT is not null
   and ( (t.TTLRRZZB = 1 and length(t.TTLRIDNT) <> 10)
      or (t.TTLRRZZB = 2 and length(t.TTLRIDNT) <> 13) )
 order by t.TTLRCDGO;
