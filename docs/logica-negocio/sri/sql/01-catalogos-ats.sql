-- =====================================================================
-- ATS FASE 1: catalogos de referencia del Anexo Transaccional Simplificado
-- Modulo: PGS (catalogos SRI compartidos)
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- ORIGEN DE LOS DATOS
--   docs/logica-negocio/cxp/Catalogo_ATS.xls, el catalogo oficial del SRI
--   que entrego el usuario. Los INSERT se GENERARON leyendo ese archivo;
--   no se transcribieron a mano.
--
-- SOLO SE CARGA LO VIGENTE
--   Cada tabla del catalogo trae Fecha Inicio / Fecha Fin. Se excluyo todo
--   lo que tiene fecha de fin cumplida. Lo que queda fuera y por que:
--     T2  codigo 09  (EXPORTACION sin identificacion)  caduco 28/02/2015
--     T4  codigo 375 (liquidacion de compra RISE)      caduco 31/12/2021
--     T5  codigo 00  (casos especiales)                caduco 28/02/2015
--     T13 codigos 02..14 (cheque propio, certificado, de gerencia y del
--         exterior, debito de cuenta, las tres transferencias, las dos
--         tarjetas de credito, giro, deposito en cuenta y endoso)
--                                                      caducaron 31/08/2016
--
--   OJO CON LA T13. El SRI retiro en 2016 todas las formas de pago
--   especificas. Hoy un pago por transferencia, cheque o debito automatico
--   se reporta como 20 OTROS CON UTILIZACION DEL SISTEMA FINANCIERO, y solo
--   el efectivo va como 01 SIN UTILIZACION DEL SISTEMA FINANCIERO. El mapeo
--   desde la forma de pago interna del sistema (1 Efectivo, 2 Transferencia,
--   3 Cheque, 4 Debito automatico) es por tanto:  1 -> 01 ;  2, 3 y 4 -> 20.
--
-- POR QUE VA SIN ACENTOS
--   Las 474 filas preexistentes de PGS.TSRI no tienen ni un solo acento, y
--   este script se ejecuta una sola vez en produccion a traves de un cliente
--   cuyo encoding no podemos verificar de antemano. Al probarlo aqui, un
--   cliente mal configurado convirtio cada tilde en un caracter de
--   reemplazo. Un catalogo tributario con basura dentro es peor que uno sin
--   tildes, asi que se cargan en ASCII puro.
--
-- LO QUE NO SE CARGA
--   T3.x (conceptos de retencion de renta) ya vive en TSRI. T12 (porcentaje
--   de IVA) se omite a proposito: el propio catalogo del SRI sigue diciendo
--   12% desde 2017 cuando la tarifa vigente es 15%, asi que la tarifa se
--   toma del documento y no de aqui. T16 (paises) y T17 (paraisos fiscales)
--   solo hacen falta si hay pagos al exterior, que hoy no los hay.
--
-- UN DETALLE DEL CATALOGO OFICIAL
--   En la T4 los codigos 373 y 374 traen la MISMA descripcion en el archivo
--   del SRI ("Nota de debito operadora transporte / socio"). Se cargan tal
--   como vienen: corregir un catalogo a ojo es peor que el error.
--
--
-- ESTE SCRIPT SE PUEDE VOLVER A EJECUTAR SIN MIEDO
--   PGS.LSRI y PGS.TSRI NO tienen clave primaria, ni indice unico, ni
--   ningun indice: nada impide insertar la misma fila dos veces. Ejecutar
--   este script dos veces DUPLICABA el catalogo en silencio (comprobado:
--   cuatro pasadas dejaron 76 filas donde debia haber 19). Por eso el
--   BLOQUE 1 empieza borrando el rango propio antes de insertar.
--   El DELETE toca UNICAMENTE ID >= 475 en TSRI e ID >= 25 en LSRI, que es
--   territorio exclusivo del ATS; las 474 + 24 filas preexistentes no se
--   tocan. Verificado ademas que esos ID preexistentes SI son unicos.
--
-- Ojo: PGS.LSRI.ID y PGS.TSRI.ID NO son identity, van explicitos.
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   En una base limpia debe dar MAX_LSRI = 24 y MAX_TSRI = 474.
--   Si dan MAS y el excedente NO son filas del ATS (TABLA 701..707), hay
--   otro catalogo ocupando ese rango: DETENERSE y avisar, porque el
--   BLOQUE 1 borra por rango de ID. Si el excedente es del ATS, es una
--   reejecucion y se puede seguir: el BLOQUE 1 lo limpia.
-- ---------------------------------------------------------------------
SELECT MAX(ID) AS MAX_LSRI FROM PGS.LSRI;
SELECT MAX(ID) AS MAX_TSRI FROM PGS.TSRI;
SELECT TABLA, DETALLE FROM PGS.LSRI WHERE TABLA IN ('701','702','703','704','705','706','707');

-- ---------------------------------------------------------------------
-- BLOQUE 1: limpieza del rango propio (hace el script repetible)
--   Sin esto, una segunda pasada duplica el catalogo sin avisar.
--   Si las dos consultas de control devuelven 0, es la primera ejecucion.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS TSRI_ATS_PREVIAS FROM PGS.TSRI WHERE ID >= 475;
SELECT COUNT(*) AS LSRI_ATS_PREVIAS FROM PGS.LSRI WHERE ID >= 25;

DELETE FROM PGS.TSRI WHERE ID >= 475;
DELETE FROM PGS.LSRI WHERE ID >= 25;

-- ---------------------------------------------------------------------
-- BLOQUE 2: declaracion de los siete catalogos (PGS.LSRI, ID 25..31)
-- ---------------------------------------------------------------------
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (25, '701', 'Cat ATS - T2 - Tipo de identificacion por transaccion', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (26, '702', 'Cat ATS - T4 - Tipos de comprobante autorizados', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (27, '703', 'Cat ATS - T5 - Sustento del comprobante', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (28, '704', 'Cat ATS - T11 - Porcentajes de retencion de IVA', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (29, '705', 'Cat ATS - T13 - Formas de pago', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (30, '706', 'Cat ATS - T14 - Tipo de identificacion del proveedor', 1);
INSERT INTO PGS.LSRI (ID, TABLA, DETALLE, ESTADO) VALUES (31, '707', 'Cat ATS - T15 - Tipo de pago residente/no residente', 1);

-- ---------------------------------------------------------------------
-- BLOQUE 3: valores vigentes (PGS.TSRI, ID 475..565)
-- ---------------------------------------------------------------------
-- Cat ATS - T2 - Tipo de identificacion por transaccion  (LSRI.ID=25, TABLA=701) -- 19 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (475, 25, '01', 'COMPRA - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (476, 25, '02', 'COMPRA - CEDULA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (477, 25, '03', 'COMPRA - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (478, 25, '04', 'VENTA - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (479, 25, '05', 'VENTA - CEDULA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (480, 25, '06', 'VENTA - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (481, 25, '07', 'VENTA - CONSUMIDOR FINAL', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (482, 25, '10', 'TARJETA DE CREDITO - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (483, 25, '11', 'TARJETA DE CREDITO - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (484, 25, '12', 'RENDIMIENTOS FINANCIEROS - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (485, 25, '13', 'RENDIMIENTOS FINANCIEROS - CEDULA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (486, 25, '14', 'RENDIMIENTOS FINANCIEROS - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (487, 25, '15', 'FONDOS Y FIDEICOMISOS - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (488, 25, '16', 'FONDOS Y FIDEICOMISOS - CEDULA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (489, 25, '17', 'FONDOS Y FIDEICOMISOS - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (490, 25, '18', 'COMPROBANTES ANULADOS - -', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (491, 25, '19', 'VENTA - PLACA o RAMV/CPN', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (492, 25, '20', 'EXPORTACION - RUC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (493, 25, '21', 'EXPORTACION - PASAPORTE / IDENTIFICACION TRIBUTARIA DEL EXTERIOR', NULL, 1);

-- Cat ATS - T4 - Tipos de comprobante autorizados  (LSRI.ID=26, TABLA=702) -- 39 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (494, 26, '1', 'Factura', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (495, 26, '2', 'Nota o boleta de venta', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (496, 26, '3', 'Liquidacion de compra de Bienes o Prestacion de servicios', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (497, 26, '4', 'Nota de credito', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (498, 26, '5', 'Nota de debito', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (499, 26, '6', 'Guias de Remision', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (500, 26, '7', 'Comprobante de Retencion', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (501, 26, '8', 'Boletos o entradas a espectaculos publicos', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (502, 26, '9', 'Tiquetes o vales emitidos por maquinas registradoras', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (503, 26, '11', 'Pasajes expedidos por empresas de aviacion', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (504, 26, '12', 'Documentos emitidos por instituciones financieras', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (505, 26, '15', 'Comprobante de venta emitido en el Exterior', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (506, 26, '16', 'Formulario Unico de Exportacion (FUE) o Declaracion Aduanera Unica (DAU) o Declaracion Andina de Valor (DAV)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (507, 26, '18', 'Documentos autorizados utilizados en ventas excepto N/C N/D', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (508, 26, '19', 'Comprobantes de Pago de Cuotas o Aportes', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (509, 26, '20', 'Documentos por Servicios Administrativos emitidos por Inst. del Estado', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (510, 26, '21', 'Carta de Porte Aereo', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (511, 26, '22', 'RECAP', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (512, 26, '23', 'Nota de Credito TC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (513, 26, '24', 'Nota de Debito TC', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (514, 26, '41', 'Comprobante de venta emitido por reembolso', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (515, 26, '42', 'Documento retencion presuntiva y retencion emitida por propio vendedor o por intermediario', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (516, 26, '43', 'Liquidacion para Explotacion y Exploracion de Hidrocarburos', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (517, 26, '44', 'Comprobante de Contribuciones y Aportes', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (518, 26, '45', 'Liquidacion por reclamos de aseguradoras', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (519, 26, '47', 'Nota de Credito por Reembolso Emitida por Intermediario', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (520, 26, '48', 'Nota de Debito por Reembolso Emitida por Intermediario', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (521, 26, '49', 'Proveedor Directo de Exportador Bajo Regimen Especial', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (522, 26, '50', 'A Inst. Estado y Empr. Publicas que percibe ingreso exento de Imp. Renta', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (523, 26, '51', 'N/C A Inst. Estado y Empr. Publicas que percibe ingreso exento de Imp. Renta', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (524, 26, '52', 'N/D A Inst. Estado y Empr. Publicas que percibe ingreso exento de Imp. Renta', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (525, 26, '294', 'Liquidacion de compra de Bienes Muebles Usados', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (526, 26, '344', 'Liquidacion de compra de vehiculos usados', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (527, 26, '364', 'Acta Entrega-Recepcion PET', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (528, 26, '370', 'Factura operadora transporte / socio', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (529, 26, '371', 'Comprobante socio a operadora de transporte', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (530, 26, '372', 'Nota de credito operadora transporte / socio', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (531, 26, '373', 'Nota de debito operadora transporte / socio', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (532, 26, '374', 'Nota de debito operadora transporte / socio', NULL, 1);

-- Cat ATS - T5 - Sustento del comprobante  (LSRI.ID=27, TABLA=703) -- 15 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (533, 27, '01', 'Credito Tributario para declaracion de IVA (servicios y bienes distintos de inventarios y activos fijos)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (534, 27, '02', 'Costo o Gasto para declaracion de IR (servicios y bienes distintos de inventarios y activos fijos)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (535, 27, '03', 'Activo Fijo - Credito Tributario para declaracion de IVA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (536, 27, '04', 'Activo Fijo - Costo o Gasto para declaracion de IR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (537, 27, '05', 'Liquidacion Gastos de Viaje, hospedaje y alimentacion Gastos IR (a nombre de empleados y no de la empresa)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (538, 27, '06', 'Inventario - Credito Tributario para declaracion de IVA', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (539, 27, '07', 'Inventario - Costo o Gasto para declaracion de IR', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (540, 27, '08', 'Valor pagado para solicitar Reembolso de Gasto (intermediario)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (541, 27, '09', 'Reembolso por Siniestros', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (542, 27, '10', 'Distribucion de Dividendos, Beneficios o Utilidades', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (543, 27, '11', 'Convenios de debito o recaudacion para IFIs', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (544, 27, '12', 'Impuestos y retenciones presuntivos', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (545, 27, '13', 'Valores reconocidos por entidades del sector publico a favor de sujetos pasivos', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (546, 27, '14', 'Valores facturados por socios a operadoras de transporte (que no constituyen gasto de dicha operadora)', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (547, 27, '15', 'Pagos efectuados por consumos propios y de terceros de servicios digitales', NULL, 1);

-- Cat ATS - T11 - Porcentajes de retencion de IVA  (LSRI.ID=28, TABLA=704) -- 6 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (548, 28, '9', 'Retencion IVA 10%', 10, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (549, 28, '10', 'Retencion IVA 20%', 20, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (550, 28, '1', 'Retencion IVA 30%', 30, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (551, 28, '11', 'Retencion IVA 50%', 50, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (552, 28, '2', 'Retencion IVA 70%', 70, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (553, 28, '3', 'Retencion IVA 100%', 100, 1);

-- Cat ATS - T13 - Formas de pago  (LSRI.ID=29, TABLA=705) -- 8 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (554, 29, '01', 'SIN UTILIZACION DEL SISTEMA FINANCIERO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (555, 29, '15', 'COMPENSACION DE DEUDAS', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (556, 29, '16', 'TARJETA DE DEBITO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (557, 29, '17', 'DINERO ELECTRONICO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (558, 29, '18', 'TARJETA PREPAGO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (559, 29, '19', 'TARJETA DE CREDITO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (560, 29, '20', 'OTROS CON UTILIZACION DEL SISTEMA FINANCIERO', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (561, 29, '21', 'ENDOSO DE TITULOS', NULL, 1);

-- Cat ATS - T14 - Tipo de identificacion del proveedor  (LSRI.ID=30, TABLA=706) -- 2 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (562, 30, '01', 'PERSONA NATURAL', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (563, 30, '02', 'SOCIEDAD', NULL, 1);

-- Cat ATS - T15 - Tipo de pago residente/no residente  (LSRI.ID=31, TABLA=707) -- 2 valores vigentes
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (564, 31, '01', 'PAGO A RESIDENTE /ESTABLECIMIENTO PERMANENTE', NULL, 1);
INSERT INTO PGS.TSRI (ID, LSRI, CODIGO, DETALLE, PORCENTAJE, ESTADO) VALUES (565, 31, '02', 'PAGO A NO RESIDENTE', NULL, 1);

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final
--   Esperado: 701=19, 702=39, 703=15, 704=6, 705=8, 706=2, 707=2  (91 filas)
-- ---------------------------------------------------------------------
SELECT L.TABLA, L.DETALLE, COUNT(T.ID) AS VALORES
  FROM PGS.LSRI L LEFT JOIN PGS.TSRI T ON T.LSRI = L.ID
 WHERE L.TABLA IN ('701','702','703','704','705','706','707')
 GROUP BY L.TABLA, L.DETALLE ORDER BY L.TABLA;

-- La T5 es la que desbloquea el ATS: revisar los 15 codigos antes de seguir.
SELECT T.CODIGO, T.DETALLE FROM PGS.TSRI T JOIN PGS.LSRI L ON L.ID = T.LSRI
 WHERE L.TABLA = '703' ORDER BY T.CODIGO;

COMMIT;

