-- =====================================================================
-- MODULO      : RHH - verificacion previa al frente de pago de beneficios sociales
-- EQUIPO      : omen-saa-2  (prefijo e2-, ver REGISTRO-RESERVAS-EQUIPOS.md 2b)
-- FECHA       : 2026-09-01
-- DISENO      : docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md
--
-- QUE HACE    : NADA. Es 100% SOLO LECTURA. Ningun INSERT, UPDATE, DELETE ni DDL.
--               Seguro de correr de corrido, en local y en produccion.
--
-- PARA QUE    : responder las seis preguntas que el diseno no pudo cerrar leyendo
--               codigo. Cada bloque dice que se espera y que hacer si no coincide.
--
-- COMO CORRER : de corrido. Anotar el resultado de cada bloque y devolverselo al
--               arbitro (omen-saa-2-arb) antes de que se escriba una linea de codigo.
-- =====================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================
-- V1  El codigo de tabla ODBS esta libre?
-- =====================================================================
-- POR QUE: el codigo de 4 letras es unico en TODO el proyecto, no por esquema
--          (REGISTRO-RESERVAS-EQUIPOS.md seccion 3). Ya verificado libre en
--          src/main/java/com/saa/model/ y en docs/. Falta la base.
--
-- ESPERADO: CERO FILAS.
-- SI DEVUELVE FILAS: PARAR. Hay que elegir otro codigo (candidatos verificados
--          libres en Java: OPBS, PGBS, ORBS) y avisar al arbitro.

SELECT owner, table_name
  FROM all_tables
 WHERE table_name IN ('ODBS', 'OPBS', 'PGBS', 'ORBS')
 ORDER BY table_name, owner;


-- =====================================================================
-- V2  Estado real de las liquidaciones de beneficio social (RHH.LQBS)
-- =====================================================================
-- POR QUE: el codigo dice que toda LQBS nace estado=1 y valorPagado=0, y que
--          nadie escribe nunca otro valor. Esto lo confirma o lo desmiente
--          contra datos, que es lo unico que vale.
--
-- ESPERADO: todas las filas con ESTADO=1 y PAGADAS=0.
-- SI HAY FILAS CON LQBSVLPG > 0: PARAR y avisar. Significa que algo si escribe
--          el pago, y el diagnostico del diseno esta incompleto.

SELECT LQBSTPBN            AS tipo_beneficio,
       LQBSANOO            AS anio,
       LQBSESTD            AS estado,
       COUNT(*)            AS filas,
       SUM(LQBSVLRR)       AS valor_total,
       SUM(CASE WHEN NVL(LQBSVLPG, 0) > 0 THEN 1 ELSE 0 END) AS pagadas
  FROM RHH.LQBS
 GROUP BY LQBSTPBN, LQBSANOO, LQBSESTD
 ORDER BY LQBSTPBN, LQBSANOO, LQBSESTD;


-- =====================================================================
-- V3  Que guarda PVNMESTD, la columna de estado de las provisiones?
-- =====================================================================
-- POR QUE: RHH.PVNM tiene columna de estado pero NO existe rubro de estados de
--          provision en com.saa.rubros (solo RhhTipoProvision, que son los 7
--          tipos). Antes de escribir sobre esa columna hay que saber que guarda.
--
-- ESPERADO: sin expectativa. Es exploratorio.
-- QUE MIRAR: si hay un unico valor, la columna no se usa como estado y dar de
--          baja la provision sera puramente contable. Si hay varios, hay una
--          maquina de estados no documentada que respetar.

SELECT PVNMTPPR      AS tipo_provision,
       PVNMESTD      AS estado,
       COUNT(*)      AS filas,
       SUM(PVNMVLOR) AS valor_total
  FROM RHH.PVNM
 GROUP BY PVNMTPPR, PVNMESTD
 ORDER BY PVNMTPPR, PVNMESTD;


-- =====================================================================
-- V4  MAX de los catalogos compartidos, antes de reservar el rubro nuevo
-- =====================================================================
-- POR QUE: regla 2 de REGISTRO-RESERVAS-EQUIPOS.md. El rango reservado dice que
--          me corresponde; el MAX real dice que hay. Si no coinciden, PARAR.
--          El frente necesita un rubro nuevo (RHH_ESTADO_ORDEN_BENEFICIO).
--
-- ESPERADO: valores de referencia para elegir el codigo del rubro nuevo.

SELECT MAX(PRBRCDGO) AS max_prbr FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS max_pdtr FROM SCP.PDTR;


-- =====================================================================
-- V5  Cuantos contratos tienen el decimo en modalidad ACUMULADO?
-- =====================================================================
-- POR QUE: dimensiona el frente 1 y, sobre todo, alimenta la verificacion mas
--          importante del diseno (seccion 3.3): si generarDecimoTercero NO filtra
--          por modalidad, pagar la orden pagaria dos veces a los mensualizados.
--
-- MODALIDAD: 1 = MENSUALIZADO, 2 = ACUMULADO  (RhhModalidadDecimoTercero/Cuarto)
--
-- ESPERADO: sin expectativa. Si ACUMULADO da 0, el frente 1 es preventivo (no hay
--          plata sin pagar hoy); si da > 0, hay obligacion vencida acumulandose.

SELECT CNTEMDD3      AS modalidad_decimo_tercero,
       CNTEMDD4      AS modalidad_decimo_cuarto,
       COUNT(*)      AS contratos
  FROM RHH.CNTE
 GROUP BY CNTEMDD3, CNTEMDD4
 ORDER BY 1, 2;

-- NOTA: los nombres CNTEMDD3 / CNTEMDD4 son la lectura del arbitro de los campos
--       modalidadDecimoTercero / modalidadDecimoCuarto de model/rhh/ContratoEmpleado.java
--       y NO fueron verificados contra el DDL. Si Oracle responde ORA-00904, sacar
--       los nombres reales con el bloque de abajo y volver a correr este.

SELECT column_name
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'CNTE'
   AND column_name LIKE 'CNTE%'
 ORDER BY column_id;


-- =====================================================================
-- V6  Las columnas de pago de LQBS existen en la base?
-- =====================================================================
-- POR QUE: regla 9 del arbitro. Hibernate incluye TODA columna @Column en el
--          SELECT que genera, asi que una columna mapeada que no existe rompe
--          CUALQUIER lectura de la entidad con ORA-00904 -- no se ve al compilar
--          y aparece cuando un usuario abre la pantalla.
--          LQBSVLPG, LQBSFCPG y LQBSESTD estan mapeadas y hoy no las lee nadie:
--          es exactamente el patron del defecto CBR.ANTC del 2026-08-29.
--
-- ESPERADO: LAS TRES presentes.
-- SI FALTA ALGUNA: PARAR. Hay que agregarla por DDL antes de desplegar cualquier
--          WAR que lea LiquidacionBeneficioSocial.

SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS'
   AND column_name IN ('LQBSVLPG', 'LQBSFCPG', 'LQBSESTD', 'LQBSVLMN', 'LQBSPRDN')
 ORDER BY column_name;

-- Control de completitud: compara la entidad entera contra el esquema.
-- ESPERADO: 16 columnas (las que declara model/rhh/LiquidacionBeneficioSocial.java).
SELECT COUNT(*) AS columnas_en_base
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS';


-- =====================================================================
-- FIN. Nada de lo anterior modifica datos.
-- No hay bloque de reverso porque no hay nada que revertir.
-- =====================================================================
