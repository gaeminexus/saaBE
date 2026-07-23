 PDF To Markdown Converter
Debug View
Result View
Ficha técnica:
Manual de usuario, catálogo y
especificaciones técnicas.
“Emisión de comprobantes electrónicos”
Método de automatización off-line.
ACTUALIZADO OCTUBRE 2025.
Versión 2. 32
Guía para contribuyentes
Índice
Caso específico de retenciones en la comercializadores / Distribuidores de derivados del
Introducción
Consideraciones generales
Base legal
Proceso de solicitud de certificación de emisión de documentos electrónicos
electrónicos 5. Proceso de generación, firma electrónica y solicitud de autorización en línea de comprobantes
Proceso de firmas electrónicas y lineamientos de parametrización en los aplicativos
Servicios expuestos en internet para la autorización de comprobantes electrónicos
Servicios expuestos en internet para consultas de comprobantes electrónicos
Facturador gratuito de generación de comprobantes electrónicos
la comercialización de periódicos y/o revistas. petróleo y retención presuntiva de IVA a los editores, distribuidores y voceadores que participan en
Códigos de errores y advertencias de validación
Códigos de error para aplicación de la devolución automática del IVA
Servicios web para la devolución automática del IVA a personas adultas mayores - DIG............
Anexos
ANEXO 1 - FORMATOS XML VERSIÓN 1.0.0
ANEXO 2 - FORMATO DE REPRESENTACIONES IMPRESAS DE DOCUMENTOS ELECTRÓNICOS (RIDE)
ANEXO 3 - FORMATOS XML VERSIÓN 1.1.0
ANEXO 4 - FORMATOS XML FACTURA EXPORTACIÓN APLICADOS A LAS VERSIONES 1.0.0 y 1.1.0
ANEXO 5 - FORMATOS XML FACTURA REEMBOLSO APLICADO EN LAS VERSIONES 1.0.0 y 1.1.0
ANEXO 6 - FORMATOS XML FACTURA CON SUBSIDIOS APLICADO EN LAS VERSIONES 1.0.0 y 1.1.0
SUBSIDIO (RIDE) ANEXO 7 – FORMATOS DE REPRESENTACIÓN IMPRESA DE DOCUMENTO ELECTRÓNICO CON
2.0.0 y 2.1.0 ANEXO 8 - FORMATOS XML FACTURA CON RUBROS DE TERCEROS APLICADO EN LAS VERSIONES
ANEXO 9 - FORMATOS XML FACTURA SUSTITUTIVA DE GUÍA DE REMISIÓN APLICADO EN LAS
VERSIONES 2.0.0 y 2.1.0 .................................................................................................................... 100

ANEXO 10 - FORMATO XML DE COMPROBANTE DE RETENCIÓN ATS VERSIÓN 2.0.0 ...................... 106

ANEXO 11 – REQUISITOS OBLIGATORIOS PARA EL XML DE FACTURA COMERCIAL NEGOCIABLE .... 111

ANEXO 12 – REQUISITO OBLIGATORIO PARA EL XML DE FACTURA EN VENTA DE COMBUSTIBLES
LÍQUIDOS DERIVADOS DE HIDROCARBUROS Y BIOCOMBUSTIBLES. ................................................ 112

ANEXO 13 – REQUISITO OBLIGATORIO PARA XML DE COMPROBANTES EMITIDOS DESDE UNA
MÁQUINA FISCAL .............................................................................................................................. 113

ANEXO 14 – EJEMPLO FIRMA ELECTRÓNICA BAJO ESTÁNDAR XADES_BES ..................................... 113

ANEXO 15 – COMPATIBILIDAD DISPOSITIVOS PROVISTOS ............................................................... 115

ANEXO 16 – REQUISITO OBLIGATORIO DE LLENADO PARA EL XML DE FACTURA EN LA VENTA DE
COMBUSTIBLES LÍQUIDOS DERIVADOS DE HIDROCARBUROS Y BIOCOMBUSTIBLES. ...................... 116

ANEXO 17 – FORMATOS XML LIQUIDACIÓN DE COMPRA DE BIENES Y PRESTACIÓN DE SERVICIOS EN
LAS VERSIONES 1.0.0 Y 1.1.0 ............................................................................................................. 117

ANEXO 18 – REQUISITOS OBLIGATORIOS DE LLENADO EN LA FACTURA ELECTRÓNICA POR LA
ENTREGA DE FUNDAS PLÁSTICAS ...................................................................................................... 125

ANEXO 19 – APLICACIÓN DE LAS AUTORETENCIONES ...................................................................... 126

ANEXO 20 – REQUISITO PARA LA APLICACIÓN DE LA DEVOLUCIÓN AUTOMÁTICA DEL IVA EN EL XML
DE FACTURAS, NOTAS DE CRÉDITO Y NOTAS DE DÉBITO. ................................................................ 129

ANEXO 21 – REQUISITO OBLIGATORIO PARA COMPROBANTES ELECTRÓNICOS EMITIDOS POR
CONTRIBUYENTES DESIGNADOS COMO AGENTES DE RETENCIÓN. ................................................. 130

ANEXO 22 – REQUISITO OBLIGATORIO PARA COMPROBANTES ELECTRÓNICOS EMITIDOS POR
CONTRIBUYENTES RIMPE. ................................................................................................................. 131

ANEXO 23 – REQUISITO OBLIGATORIO EL LLENADO PARA EL XML DE COMPROBANTES DE VENTA EN
LA TRANSFERENCIA LOCAL DE MATERIALES DE CONSTRUCCIÓN. .................................................... 134

ANEXO 24 – REQUISITO OBLIGATORIO PARA COMPROBANTES ELECTRÓNICOS EMITIDOS POR
GRANDES CONTRIBUYENTES. ............................................................................................................ 135

ANEXO 25 – REQUISITO OBLIGATORIO DE LLENADO PARA EL XML DE FACTURAS EMITIDAS POR
OPERADORAS TRANSPORTE COMERCIAL (NO APLICA PARA TAXIS Y SOCIOS O ACCIONISTAS DE
TAXIS). ............................................................................................................................................... 136

Glosario de términos ................................................................................................................... 137
Preguntas técnicas frecuentes .................................................................................................... 140
Versión (^) modificaciónFecha de Descripción de los cambios
2.0 05/08/2015 Nuevos WS para ambiente de pruebas.Se elimina las claves de uso complementario (contingencia).^
2.01 10/11/2015 Nuevos WS para ambiente de producción.
2.02 29/01/2016 Nuevos campos para factura con subsidios.
2.03 21/03/2016 Nuevos campos para factura con rubros de terceros y factura sustitutiva de guía de remisión.
2.04 01/05/2016 Tabla 18: Tabla 24: nnuevos códigos de ICE.uevas formas de pago.^
2.05 01/06/
Tabla 17: nueva tarifa de IVA vigente a partir del 01 de junio de 2016.
Se incluye en el numeral 11.8 (ANEXO 7) los requisitos obligatorios para el XML de
Factura Comercial Negociable.
2.06 22/06/
Tabla 27: nuevo código descuento solidario 2% IVA.
Tabla 28: nuevos códigos para las devoluciones de IVA por uso de medios electrónicos
exclusivamente para notas de crédito.
Se incluye ANEXO 8 nuevos campos para la inclusión del descuento solidario 2% de IVA,
devoluciones de IVA por uso de medios electrónicos y formas de pago.
2.07 28/06/2016 Actualización Actualización ttabla 24: abla 20: frormas de Pagoetenciones de IVA.^.
2.08 15/09/
Actualización tabla 24: formas de pago.
Actualización tabla 28: código para las devoluciones de IVA por descuento solidario 2%
IVA exclusivamente para notas de crédito.
2.09 18/09/
Eliminación de la tabla 27: nuevo código descuento solidario 2% IVA
Eliminación de la tabla 28: nuevos códigos para las devoluciones de IVA por uso de medios
electrónicos exclusivamente para notas de crédito.
Eliminación del anexo 8: nuevos campos para la inclusión del descuento solidario 2% de
IVA.
2.10 01/12/2017 Anexo 10: comprobante de retención ATS versión 2.0.0.
2.11 07/08/
Inclusión de campo placa para los XML de factura en la venta de combustibles líquidos
derivados de hidrocarburos (CLDH) y biocombustibles para las versiones 1.0.0, 1.1.0,
2.0.0, 2.1.0.
2.12 07/01/
Inclusión de los campos marca, tipo y serie en todas sus versiones para los XML de
Factura, Nota de Crédito, Nota de Débito, Guía de Remisión y Comprobantes de Retención
emitidos desde una máquina fiscal.
2.13 15/05/
Tabla 29: formatos de llenado del campo placa establecido por la Agencia de Regulación y
Control de Energía y Recursos Naturales no Renovables.
Tabla 30: códigos y descripción de llenado en la factura electrónica por la venta de
combustibles, según formatos establecido por la Agencia de Regulación y Control de
Energía y Recursos Naturales no Renovables.
2.14 19/07/2019 Anexo 17 las versiones 1.0.0 y 1.1.0–^ Formatos XML. liquidación de compra de bienes y prestación de servicios en
2.15 03/01/
Actualización tabla 29: formatos de llenado del campo placa establecido por la Agencia de
Regulación y Control de Energía y Recursos Naturales no Renovables.
Actualización tabla 18: tarifa del ICE.
2.16 03/02/
Actualización tabla 29: formatos de llenado del campo placa establecido por la Agencia de
Regulación y Control de Energía y Recursos Naturales no Renovables.
Actualización tabla 30: códigos y descripción de llenado en la factura electrónica por la
venta de combustibles, según formatos establecido por la Agencia de Regulación y Control
de Energía y Recursos Naturales no Renovables.
2.17 21/08/2020 Anexo 18 fundas plásticas.–^ Requisitos obligatorios de llenado en la factura electrónica por la entrega de
2.18 29/09/2020 Anexo 19 – Aplicación de las autoretenciones.
2.19 19/11/
Servicios web para la devolución automática del IVA a personas adultas mayores - DIG
Anexo 20 – Inclusión de campo para la devolución automática del IVA a personas adultas
mayores en facturas, notas de crédito y notas de débito.
2.20 11/12/2020 Anexo 21 contribuyentes designados –^ Requisito obligatorio para comprobantes electrónicos emitidos por Microempresas y/o Agentes de Retención.

Versión (^) modificaciónFecha de Descripción de los cambios
2.21 06/01/
Anexo 2 2 – Requisito obligatorio para comprobantes electrónicos emitidos por
contribuyentes RIMPE.
Actualización de porcentajes de retención de ISD.
Actualización de porcentajes de retención de IVA.
Actualización de tarifas de IVA.
2.22 01 / 09 / 2022
Actualización del Anexo 10 - Formato XML de comprobante de retención ATS versión
2.0.0.
Actualización del Anexo 22 – Requisito obligatorio para comprobantes electrónicos
emitidos por contribuyentes RIMPE Emprendedor y RIMPE Negocio Popular.
2.23 01 / 02 / 2023 Actualización del monto máximo para emitir una factura a consumidor final.Actualización de porcentajes de retención de ISD.
2.24 07 / 02 / 2023 Actualización tabla 18: tarifa del ICE.
2.25 30 / 01 / 2024 Actualización Actualización Ade porcentajes de retención de ISD.nexo 19 – Aplicación de las autoretenciones^.
2.26 05 / 03 / 2024
Actualización Tabla 17: Tarifas de IVA
Actualización de porcentajes de retención del impuesto a la renta
2.27 28/03/
Actualización de porcentajes de retención de ISD.
Anexo 2 : Formato de representaciones impresas de documentos electrónicos (RIDE)
Anexo 23 – Requisito obligatorio de llenado para los XML de comprobantes de venta y
documentos complementarios en la venta de materiales de construcción
2.28 25/06/2024 Anexo 24 Contribuyentes.-^ Requisito obligatorio para comprobantes electrónicos emitidos por Grandes
2.29 25/10/2024 Actualización de smayores - DIG ervicios web para la devolución automática del IVA a personas adultas
2.30 06/03/2025 Actualización de porcentajes de retención de ISD.
2.31 27/03/

Nuevos servicios expuestos en internet para consultas de comprobantes electrónicos:
WS - Consulta de validez de comprobantes electrónicos
WS - Consulta de factura comercial negociable
2.32 08/10/ 2025 Aoperadoras de transporte comercial (no aplica para taxis y socios o accionistas de taxis).nexo 25 –^ Requisito obligatorio de llenado para el XML^ de facturas emitidas por
FICHA TÉCNICA: MANUAL DE USUARIO,
CATÁLOGO Y ESPECIFICACIONES TÉCNICAS
SOBRE EL PROCESO DE AUTORIZACIÓN Y
EMISIÓN DE DOCUMENTOS ELECTRÓNICOS
(Aplica para la ciudadanía que emite facturas, comprobantes de retención,
guías de remisión, notas de crédito, notas de débito y liquidaciones de
compra de bienes y prestación de servicios firmadas electrónicamente)
1. Introducción
El presente documento tiene la finalidad de brindar la información, el servicio y la
asistencia a la ciudadanía, a los contribuyentes que opten por certificarse en el
Sistema de Comprobantes Electrónicos brindado por el Servicio de Rentas Internas
a través del portal web institucional http://www.sri.gob.ec.
Las directrices y actualizaciones de una implementación efectiva para los
contribuyentes se las realizará sobre este documento, el mismo que será
socializado a través de los medios de comunicación que dispone la Administración
Tributaria y principales medios de información a escala nacional.
2. Consideraciones generales
Las especificaciones operativas y técnicas se enmarcan en las siguientes
descripciones:
➢ Solicitud de certificación de emisión de comprobantes electrónicos para los
ambientes de pruebas y producción;
➢ Lineamientos en la parametrización de aplicativos del contribuyente (estándar en
firmas electrónicas);
➢ Servicios expuestos a través de WEB Service, conexiones con internet para la
autorización de comprobantes electrónicos;
➢ Uso del facturador electrónico gratuito para generar, firmar y solicitar autorización
de los comprobantes electrónicos;
➢ Esquemas XSD, formatos XML (generación individual y generación agrupados por
lotes de comprobantes electrónicos para solicitar la autorización).

Los emisores de comprobantes firmados electrónicamente operarán con
certificados digitales de firma electrónica adquiridos en cualquiera de las entidades
de certificación autorizadas en el país.
3. Base legal
Ley de Régimen Tributario Interno.
Ley de Comercio Electrónico, Firmas y Mensajes de Datos publicado en el
Suplemento del Registro Oficial No. 557 de 17 de abril de 2002.
Ley Orgánica de Solidaridad y de Corresponsabilidad Ciudadana para la
Reconstrucción y Reactivación de las zonas Afectadas por el Terremoto de 16
de abril de 2016.
Ley Orgánica de Simplificación y Progresividad Tributaria, Suplemento Registro
Oficial Nro. 111 de 31 de diciembre de 2019.
Decreto No. 181 publicado en el Registro Oficial No. 553 de 11 de octubre del
2011, en el cual norma la numeración de identificadores de campo y campos
mínimos de los tipos de certificados.
Reglamento para la Aplicación de la Ley de Régimen Tributario Interno.
Reglamento de Comprobantes de Venta, Retención y Documentos
Complementarios.
Reglamento a la Ley de Comercio Electrónico, Firmas y Mensajes de Datos,
publicado en el Registro Oficial No. 735 de 31 de diciembre de 2002.
Reglamento para la Aplicación de la Ley Orgánica de Simplificación y
Progresividad Tributaria, Segundo Suplemento al Registro Oficial Nro. 260 de 04
de agosto de 2020.
Resolución No. NAC-DGERCGC12-00105 de 09 de marzo de 2012, publicada
en Registro Oficial No. 666 de 21 de marzo de 2012.
Resolución NAC-DGERCGC14-00788, publicada en el Registro Oficial 351 del 9
de octubre de 2014.
Resolución NAC-DGERCGC15-00000284, publicada en el Registro Oficial 473
de 6 de abril de 2015.
Resolución NAC-DGERCGC15-00003184, publicada en el Registro Oficial 661
de 4 de enero de 2016.
Resolución NAC-DGERCGC16-00000247, publicada en el Registro Oficial 781
de 22 de junio de 2016.
Resolución NAC-DGERCGC16-00000385, publicada en el Registro Oficial 838
de 12 de septiembre de 2016.
Resolución NAC-DGERCGC17-00000309, publicada en el Segundo Suplemento
del Registro Oficial 8 de 6 de junio de 2017.
Resolución NAC-DGERCGC17-00000460, publicada en el Registro Oficial 72 de
5 de septiembre de 2017.
Resolución NAC-DGERCGC18-00000214, publicada en el Registro Oficial 255
de 5 de junio de 2018.
Resolución NAC-DGERCGC18-00000233, publicada en el Registro Oficial 255
de 5 de junio de 2018.
Resolución NAC-DGERCGC19-00000023 publicada en el Suplemento del
Registro Oficial No. 501 de 04 de junio de 2019.
Resolución NAC-DGERCGC20-00000059 publicada en la Edición Especial del
Registro Oficial No. 1100 de 30 de septiembre de 2020.
Los contribuyentes que ingresen una solicitud de certificación y emisión de
documentos electrónicos deberán emitir los comprobantes de venta, retención y
documentos complementarios firmados electrónicamente bajo las condiciones
señaladas en esta ficha técnica.

4. Proceso de solicitud de certificación de emisión de documentos electrónicos
4.1 El contribuyente, previo a la solicitud de certificación debe tener conocimiento
general del proceso de emisión de documentos electrónicos propuesto por la
Administración Tributaria (puede solicitar asistencia llamando al Centro de
Atención Telefónica 1700 774 774 o solicitar información y asistencia a los
funcionarios del SRI a escala nacional a través de nuestro canal de atención
presencial).

4.2 El contribuyente que se incorpore a la modalidad de emisión electrónica de
documentos deberá obtener un certificado digital de firma electrónica que
puede ser adquirido en cualquier entidad de certificación autorizada por el
organismo competente. En el enlace https://www.sri.gob.ec/nl/facturacion-
electronica encontrará las direcciones electrónicas de las entidades en donde
obtendrá detalles específicos de los certificados digitales de firma electrónica.

Hay que considerar que con la publicación del Decreto 181 de 11 de octubre de
2011, las entidades de certificación deberán actualizar los certificados digitales
de firma electrónica conforme a lo detallado en dicho decreto.

4.3 La solicitud de certificación para los ambientes de pruebas y producción deberá
realizarla directamente a través del portal web del SRI (Servicios en línea),
recuerde que debe encontrarse en estado activo, al día en sus obligaciones
tributarias y haber registrado un convenio de débito para pago de
declaraciones^1 para obtener exitosamente la autorización, esta solicitud se
realizará una sola vez para cada ambiente.

La solicitud de certificación en el ambiente de pruebas es obligatoria para todos
los solicitantes, puesto que en este ambiente los emisores podrán realizar
todas sus acciones en desarrollo, ejecutando y verificando que los
comprobantes electrónicos cumplan con los esquemas XSD, así como con el
tipo de firma electrónica incorporada en los comprobantes; adicionalmente se
verificará la conexión con los enlaces a través de WEB Service que se
utilizarán para solicitar la autorización de los comprobantes electrónicos
generados y recibir la respuesta por parte de la Administración Tributaria
conforme al acuerdo de nivel de servicio; cabe mencionar que los
comprobantes emitidos en ambiente de pruebas no tendrán ninguna validez
tributaria, ni legal.

Cabe recalcar que el ambiente de pruebas fue diseñado únicamente para
verificar que el comprobante electrónico generado cumpla con las validaciones
indicadas en el presente documento, por tal motivo no se deben hacer pruebas

(^1) Mediante Resolución No. NAC-DGERCGC18-00000108 publicada en Primer Suplemento del Registro Oficial No. 202 de 16 de marzo de
2018, se dispuso que los contribuyentes que se encuentran obligados a emitir comprobantes de venta, retención y documentos
complementarios a través de mensajes de datos y firmados electrónicamente, así como los que soliciten autorización para la emisión de dichos
comprobantes bajo esta modalidad de facturación están obligados al pago de impuestos mediante débito automático.

de stress o de masividad en este ambiente. Adicionalmente se recomienda
que, en este ambiente los contribuyentes consideren los diferentes escenarios
que podrían darse de acuerdo con su giro de negocio.
Los solicitantes, una vez que hayan verificado en el ambiente de desarrollo que
el proceso de generación de comprobantes electrónicos, así como su envío y
autorización, están estructurados correctamente y que sus pruebas realizadas
sean de calidad, podrán ingresar la solicitud de emisión en el ambiente de
producción; todas las acciones que se realicen en este ambiente, así como los
comprobantes electrónicos autorizados tendrán validez tributaria. Es
responsabilidad del emisor garantizar que el sistema utilizado para la
generación del comprobante electrónico cumpla con las validaciones y
requisitos establecidos en el Reglamento de Comprobantes de Venta,
Retención y Documentos Complementarios y Resoluciones relacionadas, a fin
de garantizar que los comprobantes generados en este ambiente sean
autorizados.
4.4 En la misma solicitud de certificación realizada para el ambiente de pruebas o
producción, el contribuyente deberá escoger el tipo de comprobante que va a
emitir de manera electrónica.

4.5 Todas las transacciones realizadas por los contribuyentes son sustentadas en
los comprobantes firmados electrónicamente, los mismos que deberán ser
enviados al SRI a través del canal WEB Service para la recepción y validación,
el sistema de comprobantes electrónicos realizará las validaciones
correspondientes, generando una contestación conforme al acuerdo de nivel de
servicio.

4.6 Todos los comprobantes que no son autorizados tendrán su descripción del
motivo por el cual no fueron autorizados.

4.7 Una vez generados los comprobantes electrónicos, el emisor tiene la obligación
de enviar dichos comprobantes al receptor mediante correo electrónico;
adicionalmente podrá utilizar otros medios de notificación (publicación en portal
web, mensaje de texto, entre otros).

4.8 En el caso de comprobantes no autorizados, el emisor deberá corregir el error
detectado y enviar nuevamente al SRI para su respectiva validación. Una vez
que el comprobante se encuentre validado y en estado autorizado, deberá
entregar y notificar al receptor.

4.9 Los contribuyentes podrán solicitar adicionalmente la inclusión de nuevos
comprobantes, según su giro de negocio.

electrónicos 5. Proceso de generación, firma electrónica y solicitud de autorización en línea de comprobantes
solicitud de autorización en línea de
comprobantes electrónicos
5.1 Los contribuyentes generarán sus comprobantes electrónicos en formato .xml
conforme a los esquemas .xsd que están disponibles en el portal web del SRI,
a través de sus propios aplicativos informáticos o mediante el facturador
electrónico que el SRI dispone gratuitamente para los contribuyentes.
5.2 Cada comprobante generado contendrá una clave de acceso única que estará
compuesta por 49 dígitos numéricos, el aplicativo a utilizar por el contribuyente
deberá generar de manera automática esta clave, la cual constituye un
requisito obligatorio que le dará el CARACTER de único a cada comprobante y
a la vez se constituirá en el número de autorización del mismo; en base a esta
clave el SRI generará la respuesta de autorizado o no; a continuación, se
describe su conformación:
TABLA 1
No. Descripción de campo Tipo de campo Formato Longitud Requisito (^) en archivo XMLEtiqueta o tag
1 Fecha de emisión
Numérico
ddmmaaaa 8
Obligatorio
2 Tipo de comprobante Tabla 3 2
3 Número de RUC 1234567890001 13
4 Tipo de ambiente Tabla 4 1
5 Serie 001001 6
6 Número del (secuencial) comprobante 000000001 9
7 Código numérico Numérico 8
8 Tipo de emisión Tabla 2 1
9 Dígito verificador (módulo 11) Numérico 1
Nota : todos los campos deben completarse conforme a la longitud indicada, es
decir si en el número secuencial no completa los 9 dígitos, la clave de acceso
estará mal conformada y será motivo de rechazo para su autorización.
El dígito verificador será aplicado sobre toda la clave de acceso (48 dígitos) y
deberá ser incorporado por el contribuyente a través del método denominado
“Módulo 11”, con un factor de chequeo ponderado (2), este mecanismo de
detección de errores será verificado al momento de la recepción del comprobante.
Cuando el resultado del dígito verificador obtenido sea igual a once (11), el digito
verificador será el cero (0) y cuando el resultado del dígito verificador obtenido sea
igual a diez 10, el dígito verificador será el uno (1).

El código numérico constituye un mecanismo para brindar seguridad al emisor en
cada comprobante emitido, el algoritmo numérico para conformar este código es
potestad absoluta del contribuyente emisor.

Ejemplo de verificación utilizando algoritmo de módulo 11:

Cadena de verificación: 41261533
+---+---+---+---+---+---+---+---+ +---+
| 4 | 1 | 2 | 6 | 1 | 5 | 3 | 3 | - |? |
Pasos 1 y 2 +---+---+---+---+---+---+---+---+ +---+
| | | | | | | |
x3 x2 x7 x6 x5 x4 x3 x
| | | | | | | |
=12 =2 =14 =36 =5 =20 =9 =
Paso 3 12 +2 +14 +36 +5 +20 +9 +6 = 104
Paso 4 104 mod 11 = 5 (ya que 104 = 11 x 9 + 5)
Paso 5 11 - 5 = 6 Resultado = 6
5.3 El código que conformará el tipo de emisión según la clave de acceso
generada se detalla a continuación:

TABLA 2

No. Tipo de emisión Código Requisito
1 Emisión normal^2 1 Obligatorio
5.4 Los tipos de comprobantes que pueden generar los contribuyentes de manera
electrónica se detalla conforme al siguiente cuadro:

TABLA 3

No. Nombre comprobante Código Requisito Etiqueta o tag en archivo XML
1 FACTURA 01
Obligatorio <codDoc>
2
LIQUIDACIÓN DE COMPRA DE
BIENES Y PRESTACIÓN DE
SERVICIOS
03
3 NOTA DE CRÉDITO 04
4 NOTA DE DÉBITO 05
5 GUÍA DE REMISIÓN 06
6 COMPROBANTE DE RETENCIÓN 07
(^2) Para el método de autorización offline, solo existe el tipo de emisión normal.

5.5 El código que conformará el tipo de ambiente según la clave de acceso se
cita a continuación:

TABLA 4

No. Tipo de ambiente Código Requisito
1 Pruebas 1
Obligatorio
2 Producción 2
5.6 Los contribuyentes que generen sus comprobantes de venta, retención y
documentos complementarios firmados electrónicamente en el ambiente de
pruebas, pueden utilizar en el campo de la razón social del receptor,
destinatario y agente retenido la denominación PRUEBAS SERVICIO DE
RENTAS INTERNAS.

TABLA 5

No. Identificación Receptor Número Razón Social
1 RUC xxxxxxxxxx
PRUEBAS SERVICIO DE
2 Cédula de identidad xxxxxxxxxx (^) RENTAS INTERNAS
3 Pasaporte xxxxxxxxxxxxx
5.7 Conforme al tipo de transacción efectuada deberá señalar el tipo de cliente,
sujeto retenido o destinatario, según el detalle:
TABLA 6
No. Tipo de identificación Código Requisito
1 RUC 04 Obligatorio
2 CÉDULA 05 Obligatorio
3 PASAPORTE 06 Obligatorio
4 VENTA A CONSUMIDOR FINAL* 07 Obligatorio
5 IDENTIFICACIÓN DELEXTERIOR* 08 Obligatorio
*Venta a consumidor final: se consignará 13 dígitos de nueve en la identificación del cliente
(9999999999999).
*Identificación del exterior: corresponderá al número de Identificación otorgado por la
Administración Tributaria (AT) del país que es residente fiscal.

En el caso de emisión de liquidaciones de compra de bienes y prestación de servicios no se
encuentra habilitado el uso del tipo de identificación venta a consumidor final
En el caso de emisión de notas de crédito, notas de débito y comprobantes de retención, se
debe obligatoriamente identificar al receptor o sujeto retenido con el tipo de identificación
correspondiente (RUC, cédula, pasaporte o identificación del exterior).
5.8 Si los comprobantes electrónicos cumplen con los esquemas y firmas
electrónicas, el Servicio de Rentas Internas autorizará los comprobantes de
manera automática, en caso de no autorizarlos se indicará el motivo del
rechazo.
5.9 En el método de autorización offline la clave de acceso generada por el
emisor se constituye en el número de autorización del mismo.

Como parte de la respuesta que el SRI genera por cada comprobante emitido
correctamente, se insertará un listado de advertencias; como por ejemplo para el
caso en que los comprobantes hayan sido emitidos en el ambiente de pruebas y
por alguna indicación que se quiera comunicar.

Listado de advertencias
Aparecerá texto informativo, por ejemplo, si es
una autorización para un ambiente de pruebas
o algún comunicado por parte del SRI.
5.10 En caso de que un comprobante haya sido rechazado debido a problemas de
inconsistencia en su información (ver tabla de códigos de errores y
advertencias de validación), el emisor deberá utilizar la misma clave de
acceso y secuencial para que una vez corregida la inconsistencia, pueda ser
enviado nuevamente al SRI para su autorización.

5.11 En el caso de que un comprobante se encuentre autorizado, el WEB Service
de autorización devuelve el XML autorizado, pero si el comprobante fue no
autorizado varias veces, el WEB Service retornará únicamente el último
estado.

5.12 Constituye obligación del contribuyente el envío del comprobante electrónico
al SRI de manera individual o en lote; y la verificación de que el comprobante
conste en estado autorizado. A continuación, se describen los estados del
comprobante electrónico:

TABLA 6

No. Estado del electrónicocomprobante SIGLAS
1 En procesamiento PPR
2 Autorizado AUT
3 No autorizado NAT
Cuando el comprobante electrónico se encuentre en estado No Autorizado (NAT),
el emisor estará en la obligación de corregir y enviar nuevamente el comprobante
electrónico a través del WEB Service y posteriormente notificar y entregar al
receptor: destinatario o sujeto retenido el nuevo comprobante electrónico, mediante
correo electrónico. Cabe aclarar que el tiempo máximo que le tomará al SRI en
procesar un comprobante electrónico será de 24 horas.

Es obligación de los ciudadanos que reciben comprobantes electrónicos validar sus
comprobantes mediante el portal web del Servicio de Rentas Internas.

6. Proceso de firmas electrónicas y lineamientos de parametrización en los aplicativos
6.1 Para la generación y emisión de los documentos electrónicos deberán
obligatoriamente firmar cada archivo xml bajo el estándar de firma digital de
documentos XML: XadES_BES, esto quiere decir que cada archivo .xml
tendrá dentro de su estructura la firma electrónica y constituirá un documento
electrónico válido una vez que el SRI proceda con la autorización.

6.2 A continuación, se detallan las especificaciones técnicas relacionadas al
estándar:

Descripción Especificación Documentación técnica relacionada
Estándar de firma XadES_BES http://uri.etsi.org/01903/v1.3.2/ts_101903v010302p.pdf
Versión del esquema 1.3.2 http://uri.etsi.org/01903/v1.3.2#
Codificación UTF- (^8)
Tipo de firma ENVELOPED http://www.w3.org/2000/09/xmldsig#enveloped-signature
6.3 La estructura del formato básico de firma electrónica avanzada acorde con la
presente política se adecua a las especificaciones definidas en XADES_BES
que incluyen los campos que se describen en el esquema 1.3.2 del cuadro
anterior.
6.4 La firma electrónica se considera un nodo más a añadir en el documento .xml.
El nivel de seguridad en la firma electrónica está ejecutado sobre tres partes
de la trama de datos:

Todos los elementos o nodos que conforman el comprobante electrónico.
Los elementos de firma ubicados en el contenedor “SignedProperties”.
El certificado digital con el que se ha firmado incluido en el elemento
“KeyInfo”.
6.5 Es necesario utilizar el elemento ds: KeyInfo, conteniendo al menos el
certificado firmante codificado en base64. Además, dicha información precisa
ser firmada con objeto de evitar la posibilidad de sustitución del certificado.

6.6 En el anexo 4 se muestra un ejemplo de una factura firmada bajo el estándar
XadES_BES.

Cada comprobante deberá incorporar la firma electrónica en formato XADES-
Bes, misma que se puede realizar con librerías destinadas para el efecto. El
SRI utilizó el siguiente set de librerías para incorporar y validar la firma de
cada comprobante:
MITyCLibXADES
MITyCLibTSA
MITyCLibAPI
MITyCLibOCSP
MITyCLibTrust
Para más información del estándar se puede explorar el siguiente enlace:
http://webapp.etsi.org/workprogram/Report_WorkItem.asp?WKI_ID=
6.7 Sobre aspectos técnicos del estándar de encriptación, se puede revisar la
siguiente dirección: http://www.ietf.org/rfc/rfc2313.txt (RSA Encryption).

6.8 A continuación, se detallan las especificaciones técnicas relacionadas al
algoritmo de encriptación:

Algoritmo de firmado: RSA-SHA
Longitud de clave: 2048 bits. Recomendación técnica basada en documento:
http://csrc.nist.gov/publications/nistpubs/800-57/sp800- 57 - Part1-revised2_Mar08-2007.pdf
Archivo de Intercambio de Información: PKCS12 (extensión. p12). Este
archivo deberá ser proporcionado ya sea de manera directa (a través de API ́s
de acceso al token USB), o de manera indirecta a través de la extracción del
mismo y posterior instalación en una carpeta específica de la cual el software
proporcionado por el SRI lo utilizará para firmar los comprobantes.
7. Servicios expuestos en internet para la autorización de comprobantes electrónicos
Los servicios expuestos en el internet por la Administración Tributaria están
estandarizados a través de canales seguros con protocolos de seguridad y
certificados SSL.

7.1 Procesos que ejecutan los servicios expuestos en internet:

7.1.1 Los procesos tienen la función de aceptar o rechazar comprobantes de
manera individual o por lotes.
7.1.2 Para el intercambio de información entre el contribuyente y la Administración
Tributaria, es requisito indispensable que el contribuyente cuente con acceso
a la red de internet banda ancha (por definición y recomendación del
MINTEL la conexión debe ser mayor a 256Kbps).
7.1.3 Para poder acceder al servicio de autorización de comprobantes
electrónicos, el contribuyente deberá crear el software cliente para poder
invocar a los WEB Service que el SRI pone a disposición.
7.1.4 Para garantizar que la conexión es segura se empleará Certificados Digitales
SSL, es decir, el SRI emitirá un certificado válido cuando se realice la
petición de los WEB Service.
Sin embargo, considerando que los certificados pueden ser cambiados
durante el periodo de su vigencia por causas técnicas o institucionales, se
recomienda a los contribuyentes que, en la programación de sus sistemas,
se considere los mecanismos necesarios para que no se queme en su
código la información, ni los certificados digitales de comprobantes
electrónicos del SRI, puesto que estos podrían cambiar sin previo aviso por
la urgencia según sea el caso.
7.2 Existen dos ambientes disponibles para la invocación de los WS publicados
por la Administración Tributaria:
7.2.1 Uno es para el ambiente de pruebas, donde cada contribuyente certificará
que su aplicación funcione correctamente con cada tipo de comprobante
electrónico, las direcciones de los WS son las siguientes:
https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl
https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl

7.2.2 El segundo es para el ambiente de producción, al cual cada contribuyente
deberá acceder una vez que ha realizado las pruebas y esté seguro de que
su aplicación funciona correctamente, las direcciones de los WS son las
siguientes:
https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl
https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl

7.2.3 Los WS expuestos por la Administración Tributaria son los siguientes:
Recepción de comprobantes electrónicos
@WebMethod
@WebResult(name="RespuestaRecepcionComprobante")
public RespuestaSolicitud validarComprobante(@WebParam(name = "xml")
byte[] xml);
Parámetros:
I/O Nombre Tipo Descripción
IN Xml byte[] Equivale al archivo xml del comprobante, el cual debe estar firmado por el contribuyente.
OUT
RespuestaC
omprobante
Autorizacion
Objeto
Retorna un objeto XML el cual indica la aceptación o rechazo del comprobante.
En caso de rechazo se envía el arreglo con los motivos.
La estructura que cumplirá la respuesta a la invocación del servicio es la siguiente:
Recepción exitosa
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:validarComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.recepcion">
<RespuestaRecepcionComprobante>
<estado>RECIBIDA</estado>
<comprobantes/>
</RespuestaRecepcionComprobante>
</ns2:validarComprobanteResponse>
</soap:Body>
</soap:Envelope>
Recepción fallida
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:validarComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.recepcion">
<RespuestaRecepcionComprobante>
<estado>DEVUELTA</estado>
<comprobantes>
<comprobante>
<claveAcceso>1702201205176001321000110010030001000011234567816</claveAcceso>
<mensajes>
<mensaje>
<identificador>35</identificador>
<mensaje>DOCUMENTO INVÁLIDO</mensaje>
<informacionAdicional>Se encontró el siguiente error en la estructura del comprobante: cvc-
complex-type.2.4.a: Invalid content was found starting with element 'totalSinImpuestos'. One
of '{fechaEmisionDocSustento}' is expected.</informacionAdicional>
<tipo>ERROR</tipo>
</mensaje>
</mensajes>
</comprobante>
</comprobantes>
</RespuestaRecepcionComprobante>
</ns2:validarComprobanteResponse>
</soap:Body>
</soap:Envelope>
Consulta de respuesta de autorización:
@WebMethod
@WebResult(name = "RespuestaAutorizacionComprobante")
public RespuestaComprobante autorizacionComprobante(
@WebParam(name = "claveAccesoComprobante") String
claveAccesoComprobante) ;
Consulta de respuesta de lote
@WebMethod
@WebResult(name = "RespuestaAutorizacionLote")
public RespuestaLote autorizacionComprobanteLote(@WebParam(name =
"claveAccesoLote") String claveAccesoLote) ;
Parámetros:
I/O Nombre Tipo Descripción
IN ClaveAcces
o

String Equivale a la clave de acceso del comprobante a ser consultado.
OUT RespuestaL
oteCompAu
torizacion

Objeto Retorna un objeto XML el cual indica la aceptación o rechazo de cada uno de los
comprobantes ingresado en el lote.
En caso de rechazo se envía el arreglo con los motivos por cada comprobante del lote.
Comprobante Autorizado
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:autorizacionComprobanteResponse
xmlns:ns2="http://ec.gob.sri.ws.autorizacion">
<RespuestaAutorizacionComprobante>
<claveAccesoConsultada>
0503201201176001321000110010030009900641234567814
</claveAccesoConsultada>
<numeroComprobantes>1</numeroComprobantes>
<autorizaciones>
<autorizacion>
<estado>AUTORIZADO</estado>
<numeroAutorizacion>
I/O Nombre Tipo Descripción
0503201201176001321000110010030009900641234567814
</numeroAutorizacion>
<fechaAutorizacion>2012- 03 - 05T16:57:34.997-05:00</fechaAutorizacion>
<ambiente>PRUEBAS</ambiente>
<comprobante><![CDATA[<?xml version="1.0" encoding="UTF-8"?>
<factura id="comprobante" version="1.0.0">
<!-- FACTURA FIRMADA DIGITALMENTE, VER ANEXO 3 -->
</factura>]]>
</comprobante>
<mensajes>
<mensaje>
<identificador>60</identificador>
<mensaje>ESTE PROCESO FUE REALIZADO EN EL AMBIENTE DE
PRUEBAS
</mensaje>
<tipo>ADVERTENCIA</tipo>
</mensaje>
</mensajes>
</autorizacion>
</autorizaciones>
</RespuestaAutorizacionComprobante>
</ns2:autorizacionComprobanteResponse>
</soap:Body>
</soap:Envelope>
Comprobante No Autorizado
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:autorizacionComprobanteResponse
xmlns:ns2="http://ec.gob.sri.ws.autorizacion">
<RespuestaAutorizacionComprobante>
<claveAccesoConsultada>
1302201201176001321000120010030000050431234567814
</claveAccesoConsultada>
<numeroComprobantes>1</numeroComprobantes>
<autorizaciones>
<autorizacion>
<estado>RECHAZADO</estado>
<fechaAutorizacion>2012- 02 - 13T16:34:48.997-05:00</fechaAutorizacion>
<ambiente>PRUEBAS</ambiente>
<comprobante><![CDATA[<?xml version="1.0" encoding="UTF-8"?>
<factura id="comprobante" version="1.0.0">
<!-- FACTURA FIRMADA DIGITALMENTE, VER ANEXO 4 -->
</factura>]]>
</comprobante>
<mensajes>
<mensaje>
<identificador>46</identificador>
<mensaje> RUC no existe </mensaje>
<tipo>ERROR</tipo>
</mensaje>
</mensajes>
</autorizacion>
</autorizaciones>
</RespuestaAutorizacionComprobante>
</ns2:autorizacionComprobanteResponse>
</soap:Body>
</soap:Envelope>
7.3 El Sistema de Autorización de Documentos Electrónicos soporta un proceso a
través de un ambiente computacional seguro, que brinda alta disponibilidad y
rendimiento, opta por utilizar la infraestructura necesaria para brindar el servicio
a la ciudadanía que realizan transferencias de bienes o prestación de servicios.

7.4 La manera correcta de consumir las direcciones URL de los WS, es de manera
asíncrona; es decir una vez que el contribuyente envíe el comprobante al WS

de recepción y obtenga la respuesta “RECIBIDA”, se debe esperar un
determinado tiempo (se recomienda que este tiempo sea parametrizable) antes
de proceder a consumir la segunda dirección URL de autorización mediante la
clave de acceso del comprobante, para obtener el resultado: procesamiento
(PPR), autorizado (AUT), no autorizado (NAT).
7.5 Procesos que ejecuta el Sistema de Autorización de Documentos Electrónicos:
Exposición de componentes tecnológicos para el servicio de autorización de
comprobantes electrónicos.
Receptar los documentos firmados electrónicamente (primera validación
general).
Validación de los documentos firmados electrónicamente (segunda validación
a detalle con certificados de firma electrónica).
Autorizar de manera automática cada comprobante electrónico. El tiempo
estimado de entrega de la autorización o motivos de errores de un
comprobante, será de un tiempo máximo de 24 horas a partir de la respuesta
de RECIBIDA, generada por el WS de recepción.
El límite máximo en tamaño y número de comprobantes electrónicos a ser
validados y autorizados por lote es de 500 kb o 50 comprobantes
aproximadamente (considerando cada comprobante con un solo ítem);
mientras que, para el envío individual, el tamaño máximo por comprobante
será de 320 Kb.
TABLA 8: FORMATO XML PARA ENVÍO POR LOTE
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
**Obligatorio** - -
Obligatorio - -
2808201400179210439400110010010000000091234567812 <
/claveAcceso>

Obligatorio Numérico 49
1792104394001 Obligatorio Numérico 13
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
TABLA 9:
Las claves de acceso para el envío de lote de máximo 50 comprobantes (512 kb)
estarán compuestas de 49 caracteres numéricos, la herramienta o aplicativo a
utilizar por el contribuyente deberá generar de manera automática la clave de
acceso, que constituirá un requisito que dará el CARACTER de único a cada lote, y
la misma servirá para que el SRI indique si fue recibido; se describe a continuación
su conformación:
No. Descripción de campo Tipo de campo Formato Longitud Requisito (^) en archivo XMLEtiqueta o tag
1 Fecha de emisión
Numérico
ddmmaaaa 8
Obligatorio
2 Tipo de comprobante Tabla 3 2
3 Número de RUC 1234567890001 13
4 Tipo de ambiente Tabla 4 1
5 Serie* 001001 6
6 Número secuencial* 000000001 9
7 Código numérico Numérico 8
8 Tipo de emisión Tabla 2 1
9 Dígito verificador (módulo 11) Numérico 1
*El emisor deberá asignar la serie y secuencial que corresponderá únicamente al envío en lote.

8. Servicios expuestos en internet para consultas de comprobantes electrónicos
Los servicios expuestos en el internet por la Administración Tributaria están
estandarizados a través de canales seguros con protocolos de seguridad y
certificados SSL.
8.1 Procesos que ejecutan los servicios expuestos en internet:
8.1.1 Los procesos tienen la función de consulta comprobantes de manera
individual.
8.1.2 Para el intercambio de información entre el contribuyente y la Administración
Tributaria, es requisito indispensable que el contribuyente cuente con acceso
a la red de internet banda ancha (por definición y recomendación del MINTEL
la conexión debe ser mayor a 256Kbps).
8.1.3 Para poder acceder al servicio de consulta de comprobantes electrónicos, el
contribuyente deberá crear el software cliente para poder invocar a los WEB
Service que el SRI pone a disposición.
8.1.4 Para garantizar que la conexión es segura se empleará Certificados Digitales
SSL, es decir, el SRI emitirá un certificado válido cuando se realice la
petición de los WEB Service.
Sin embargo, considerando que los certificados pueden ser cambiados
durante el periodo de su vigencia por causas técnicas o institucionales, se
recomienda a los contribuyentes que, en la programación de sus sistemas,
se considere los mecanismos necesarios para que no se queme en su
código la información, ni los certificados digitales de comprobantes
electrónicos del SRI, puesto que estos podrían cambiar sin previo aviso por
la urgencia según sea el caso.
8.2 Existen dos ambientes disponibles para la invocación de los WS publicados por
la Administración Tributaria:
8.2.1 Uno es para el ambiente de pruebas, donde cada contribuyente certificará
que su aplicación funcione correctamente con cada tipo de comprobante
electrónico, las direcciones de los WS son las siguientes:
https://celcer.sri.gob.ec/comprobantes-electronicos-ws/ConsultaComprobante?wsdl

https://celcer.sri.gob.ec/comprobantes-electronicos-ws/ConsultaFactura?wsdl

8.2.2 El segundo es para el ambiente de producción, al cual cada contribuyente
deberá acceder una vez que ha realizado las pruebas y esté seguro de que
su aplicación funciona correctamente, las direcciones de los WS son las
siguientes:
https://cel.sri.gob.ec/comprobantes-electronicos-ws/ConsultaComprobante?wsdl

https://cel.sri.gob.ec/comprobantes-electronicos-ws/ConsultaFactura?wsdl

8.2.3 Los WS expuestos por la Administración Tributaria son los siguientes:
Consulta de validez de comprobantes electrónicos
@WebMethod
@WebResult(name = " EstadoAutorizacionComprobante")
public RespuestaConsultaComprobante consultarEstadoAutorizacionComprobante
(@WebParam(name = "claveAcceso") String claveAcceso)
Parámetros:
I/O Nombre Tipo Descripción
IN claveAcceso String Clave de acceso del comprobante electrónico
OUT
EstadoAutori
zacionCompr
obante
Objeto
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:ec="http://ec.gob.sri.ws.consultas">
<soapenv:Header/>
<soapenv:Body>
<ec:consultarEstadoAutorizacionComprobante>
<claveAcceso>0211202401050306179800120010020000000677300995216</claveAcceso>
</ec:consultarEstadoAutorizacionComprobante>
</soapenv:Body>
</soapenv:Envelope>
Retorna un objeto XML con la información del estado del comprobante electrónico.
Dependiendo del estado de autorización del comprobante electrónicos, el servicio web,
devolverá en la etiqueta estadoAutorizacion el valor:
“AUTORIZADO”
“NO AUTORIZADO”
“PENDIENTE DE ANULAR” y
“ANULADO”
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoAutorizacionComprobanteResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">

2111202405176001321000110010010000001241234567810

AUTORIZADO
Nota de Débito
1760013210001
2024- 12 - 12T10:49:37-05:00

</ns2:consultarEstadoAutorizacionComprobanteResponse>
</soap:Body>
</soap:Envelope>

Si el comprobante consultado se encuentra fuera del rango de fechas permitido por el
SRI, devolverá en la etiqueta estadoAutorizacion el valor “RECHAZADA”, con el
identificador 99 correspondiente a “ERROR AL CONSULTAR DATOS DEL SERVICIO WEB” y
con el mensaje de informacionAdicional “No es posible validar la clave de acceso ya que
la fecha de emision esta fuera del rango permitido.”

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoAutorizacionComprobanteResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">

RECHAZADA

1510202407099313057500110010010000103591234567816


99
ERROR AL CONSULTAR DATOS DEL SERVICIO WEB
No es posible validar la clave de acceso ya que la fecha de
emision esta fuera del rango permitido.
ERROR



</ns2:consultarEstadoAutorizacionComprobanteResponse>
</soap:Body>
</soap:Envelope>

Si el comprobante no se encuentra en las bases de datos del SRI, devolverá en la etiqueta
estadoAutorizacion el valor “RECHAZADA”, con el identificador 99 correspondiente a
“ERROR AL CONSULTAR DATOS DEL SERVICIO WEB”

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoAutorizacionComprobanteResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">

RECHAZADA

2111202401176001321000110010010000011171234567810


99
ERROR AL CONSULTAR DATOS DEL SERVICIO WEB

<informacionAdicional>No existen datos para los parámetros
ingresados</informacionAdicional>
<tipo>ERROR</tipo>
</mensaje>
</mensajes>
</EstadoAutorizacionComprobante>
</ns2:consultarEstadoAutorizacionComprobanteResponse>
</soap:Body>
</soap:Envelope>
Consulta de factura comercial negociable:
@WebMethod
@WebResult(name = "EstadoConfirmacionFacturaComercialNegociable")
public RespuestaConsultaFacturaComercialNegociable
consultarEstadoConfirmacionFacturaComercialNegociable
(@WebParam(name = "claveAcceso") String claveAcceso)
Parámetros:
I/O Nombre Tipo Descripción
IN ClaveAcceso String (^) Clave de acceso del comprobante electrónico
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:ec="http://ec.gob.sri.ws.consultas">
<soapenv:Header/>
<soapenv:Body>
<ec:consultarEstadoConfirmacionFacturaComercialNegociable>
1211202401092554321700110021000000000790925543211
</ec:consultarEstadoConfirmacionFacturaComercialNegociable>
</soapenv:Body>
</soapenv:Envelope>
OUT (^) EstadoConfir
macionFactu
raComercial
Negociable
Objeto (^) Retorna un objeto XML con la información de la factura electrónica indicando si ha sido
aceptada como factura comercial negociable
Dependiendo si la factura electrónica tiene estado de confirmación, aceptado como
factura comercial negociable, el servicio web devolverá en la etiqueta
estadoConfirmacion el valor: “SI”
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">

1111202401099338176200110020010000003961234567815

SI

</ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse>
</soap:Body>
</soap:Envelope>

I/O Nombre Tipo Descripción
Si la factura consultada se encuentra fuera del rango de fechas permitido por el SRI,
devolverá en la etiqueta estadoConsulta el valor “RECHAZADA”, con el identificador 99
correspondiente a “ERROR AL CONSULTAR DATOS DEL SERVICIO WEB” y con el mensaje
de informacionAdicional “No es posible validar la clave de acceso ya que la fecha de
emision esta fuera del rango permitido.”
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">

RECHAZADA
1111202401099338176200110020010000003961234567815


99
ERROR AL CONSULTAR DATOS DEL SERVICIO WEB
No es posible validar la clave de acceso ya que la fecha de
emision esta fuera del rango permitido.
ERROR



</ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse>
</soap:Body>
</soap:Envelope>

Si la factura consultada no se encuentra en las bases de datos del SRI o no fue aceptada
como factura comercial negociable, devolverá en la etiqueta estadoConsulta el valor
“RECHAZADA”, con el identificador 99 correspondiente a “ERROR AL CONSULTAR DATOS
DEL SERVICIO WEB”
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
<soap:Body>
<ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse
xmlns:ns2="http://ec.gob.sri.ws.consultas">
<EstadoConfirmacionFacturaComercialNegociable>
<estadoConsulta>RECHAZADA</estadoConsulta>
<claveAcceso>1211202401092554321700110021000000000790925543211</claveAcceso>
<mensajes>
<mensaje>
<identificador>99</identificador>
<mensaje>ERROR AL CONSULTAR DATOS DEL SERVICIO WEB</mensaje>
<informacionAdicional>No existen datos para los parametros
ingresados</informacionAdicional>
<tipo>ERROR</tipo>
</mensaje>
</mensajes>
</EstadoConfirmacionFacturaComercialNegociable>
</ns2:consultarEstadoConfirmacionFacturaComercialNegociableResponse>
</soap:Body>
</soap:Envelope>
8.3 El Sistema de Autorización de Documentos Electrónicos soporta un proceso a
través de un ambiente computacional seguro, que brinda alta disponibilidad y
rendimiento, opta por utilizar la infraestructura necesaria para brindar el servicio
a la ciudadanía que realizan transferencias de bienes o prestación de servicios.
8.4 Una vez que el contribuyente realice la consulta con la clave de acceso del
comprobante electrónico en el WS de consulta de validez obtendrá en
respuesta los estados del comprobante “AUTORIZADO”, “NO AUTORIZADO”,
“PENDIENTE DE ANULAR” o “ANULADO”.

8.5 En el caso de la consulta de si es una factura comercial negociable, cuando el
contribuyente realice la consulta con la clave de acceso del comprobante
electrónico en el WS obtendrá en respuesta “SI”, caso contrario obtendrá el
valor “RECHAZADA”.

8.6 Es fundamental señalar que solo la factura comercial negociable generará la
respuesta “SI” una vez que haya sido notificada y aceptada a través del portal
web, en la opción de Comprobantes Electrónicos / Ambientes de Pruebas o
Producción / Factura Comercial Negociable

9. Facturador gratuito de generación de comprobantes electrónicos
9.1 El Servicio de Rentas Internas pone a disposición de la ciudadanía de manera
gratuita, un facturador electrónico, el cual permitirá generar comprobantes,
firmarlos electrónicamente y visualizarlos de manera amigable.

9.2 El facturador electrónico tiene la particularidad de asignar y modificar directorios
para los archivos de los comprobantes electrónicos, validar el esquema y firma
electrónica de comprobantes, también se puede visualizar los documentos
electrónicos.

9.3 Para instalar el facturador, los contribuyentes deberán descargar el instalador
desde el portal web del Servicio de Rentas Internas, ingresando a Inicio / Guía
Básica Tributaria / Facturación / Facturación Electrónica / Facturador electrónico
gratuito.

Para una correcta instalación, se recomienda descargar el Manual de Usuario
que le servirá como guía y pasos a seguir.
9.4 Una vez instalado el facturador electrónico, se deberá parametrizar los
directorios disponibles:

TABLA 10

No. Ruta o Directorios Observación
1 Comprobantes generados Directorio donde se encuentren los documentos para ser firmados.
2 Comprobantes electrónicamente firmados Directorio electrónicamente de manera satisfactoria.donde se guardarán los documentos firmados
No. Ruta o Directorios Observación
2.1 Comprobantes transmitidos y sin respuesta de autorización*
Directorio donde almacenarán los comprobantes firmados
electrónicamente remitidos a la Administración Tributaria y no se ha
recibido una respuesta.
3 Comprobantes autorizados
Directorio donde se almacenarán los comprobantes autorizados por el
SRI y automáticamente deberán eliminarse de los directorios 1 y/o 2
únicamente si son autorizados.
4 Comprobantes no autorizados Directorio donde se almacenarán los archivos con los motivos de por qué no se autorizó los comprobantes.
5 Comprobantes enviados Directorio donde se almacenarán los comprobantes en estado en procesamiento.
5.1 Comprobantes rechazados* Directorio donde no cumple esquemas o sin autorización de emisión.
* Estos directorios se configuran automáticamente dentro de la carpeta de documentos firmados.
9.5 De manera obligatoria deberá parametrizarse la información del emisor, con la
finalidad de que cuando se genera un comprobante electrónico, esta
información aparezca por defecto sin la necesidad de digitar en cada
transacción la misma información, generando posibles errores de forma y
digitación.

TABLA 11

No. Descripción (emisor o agente de retención) Tipo de campo Longitud Requisito
1 Número de RUC Numérico 13 Obligatorio
2 Razón social / nombres o apellidos Alfanumérico Max. 300 Obligatorio
3 Nombre comercial Alfanumérico Max. 300 Opcional
4 Dirección del establecimiento matriz Alfanumérico Max. 300 Obligatorio
5 Dirección del establecimiento emisor Alfanumérico Max. 300 Opcional
6 Código del establecimiento emisor Numérico 3 Obligatorio
7 Código del punto de emisión Numérico 3 Obligatorio
8 Contribuyente especial (Número de resolución) Numérico Min. 3 y Max. 13 Opcional
9 Obligado a NO) llevar contabilidad (Opciones SI o De selección 2 Opcional
10 Logo del emisor Imagen - Opcional
11 Tipo de ambiente Numérico 1 Obligatorio
12 Tipo de emisión Numérico 1 Obligatorio
9.6 Para una óptima utilización del facturador, también se deberá parametrizar los
productos o servicios que ofrece el vendedor, ingresando el detalle y un código
de producto y/o servicio, en conjunto con su tarifa de impuesto de IVA, ICE,
IRBPNR o ISD de ser el caso. Se podrá importar o exportar los productos o
servicios a parametrizar en formato txt.

TABLA 12

No. Descripción Requisito
1 Impuestos y tarifas parametrizables Obligatorio
2 Código identificador del producto o servicio asignado por el contribuyente. Obligatorio
2 Código identificador auxiliar del producto o servicio Opcional
3 Nombre del producto o servicio Obligatorio
4 Valor unitario Obligatorio
5 Descuento Obligatorio
6 Impuesto Obligatorio
7 Tarifa del impuesto Obligatorio
8 Campos adicionales (máximo tres campos de hasta 300 caracteres) Opcional
9.7 De igual manera se podrá parametrizar a los clientes ya identificados, a
quienes van a transferir los bienes o realizar la prestación de servicios, así
también la información de los transportistas para el caso de guías de remisión y
los agentes retenidos para los comprobantes de retención. Se podrá importar
los clientes a parametrizar en formato txt.
TABLA 13
No. Descripción Requisito
1 Identificación (Número de RUC, cédula o pasaporte) Obligatorio
2 Nombres y apellidos o razón social Obligatorio
3 Dirección de correo electrónico Obligatorio
4 Placa (para el caso de guías de remisión) Obligatorio
5 Teléfono Opcional
6 Dirección / ubicación Opcional
9.8 Por defecto aparecerá la denominación de la moneda de curso actual en el
país “DÓLAR”.
9.9 Se detalla en el cuadro adjunto los campos a ser llenados que corresponden a
facturas, comprobantes de retención, notas de crédito y notas de débito:
TABLA 14: FACTURAS, COMPROBANTES DE RETENCIÓN, NOTAS DE
CRÉDITO Y NOTAS DE DÉBITO:
No. (^) (comprador o Descripción de agente llenado retenido) Tipo de campo Longitud Requisito Comprobante
1 Número comprobantesecuencial del Numérico 9 Obligatorio TODOS
2 Razón apellidossocial / Nombres o Alfanumérico Max. 300 Obligatorio TODOS
3
Identificación (RUC, Cédula,
pasaporte, identificación del
exterior o placa)
Alfanumérico Max 20 Obligatorio TODOS
4 Fecha de emisión Numérico 8 Obligatorio TODOS
5 Número de la guía de remisión Numérico 15 Opcional CAMPO SOLO PARA FACTURA
6 Denominación del comprobante de venta que se modifica Numérico 2 Obligatorio CAMPO SOLO PARA NOTAS DE CRÉDITO Y NOTAS DE DÉBITO
7 Número del comprobante de venta que se modifica Numérico 15 Obligatorio CAMPO SOLO PARA NOTAS DE CRÉDITO Y NOTAS DE DÉBITO
8 Denominación del comprobante de venta que motiva la retención Numérico 2 Obligatorio
CAMPO SOLO PARA
COMPROBANTES DE
RETENCIÓN
9 Número del comprobante de venta que motiva la retención Numérico 15 Obligatorio
CAMPO SOLO PARA
COMPROBANTE DE
RETENCIÓN

10 Ejercicio fiscal (mmaaaa) Numérico 6 Obligatorio

CAMPO SOLO PARA
COMPROBANTE DE
RETENCIÓN
11 Código numérico Numérico 8 / 23 Obligatorio TODOS
12 Dígito verificador (Módulo 11) Numérico 1 Obligatorio TODOS

TABLA 15: GUÍAS DE REMISIÓN:
No. Descripción de Llenado Tipo de campo Longitud Requisito
1 Número secuencial del comprobante Numérico 9 Obligatorio
2 Razón social / nombres o apellidos (Destinatario) Alfanumérico Max. 300 Obligatorio
3 Identificación destinatario (RUC, identificación del exterior) cédula, pasaporte, Alfanumérico 10 a 20 Obligatorio
4 Dirección del punto de partida Alfanumérico Max. 300 Obligatorio
5 Dirección del destinatario o destinos Alfanumérico Max. 300 Obligatorio
6 Razón remitentesocial / Nombres o ) apellidos (transportista o Alfanumérico Max. 300 Obligatorio
7 Identificación transportista o remitentepasaporte) (ruc, cédula, Alfanumérico 10 a 13 Obligatorio
8 Número de placa Alfanumérico Max. 20 Obligatorio
9 Descripción de la mercadería transportada Alfanumérico Max. 300 Obligatorio
10 Cantidad de la mercadería transportada Alfanumérico Libre Obligatorio
11 Motivo del traslado Alfanumérico Max. 300 Obligatorio
12 Denominación del comprobante de venta Numérico 2 Opcional
13 Número de autorización del comprobante de venta Numérico 10 ó 37 Opcional
14 Fecha de emisión del comprobante de venta Numérico 8 Opcional
15 Numeración del comprobante de venta Numérico 15 Opcional
16 Número de la declaración aduanera Numérico 20 Opcional
17 Fecha de Inicio de transporte Numérico 8 Obligatorio
18 Fecha de terminación del transporte Numérico 8 Obligatorio
19 Ruta Alfanumérico Max. 300 Opcional
20 Código del establecimiento del destinatario del producto Numérico 3 Opcional
21 Código numérico Numérico 8 / 23 Obligatorio
22 Dígito verificador Numérico 1 Obligatorio
9.10 Entre la lista de clientes se encuentra el “Consumidor final”, para que por
defecto se identifique en ventas a consumidores finales. Si el valor de la factura
es mayor a 5 0 USD se deberá especificar obligatoriamente los datos del
adquirente.
9.11 Se podrá ingresar información adicional por cada comprobante, como
máximo quince campos de hasta 300 caracteres alfanuméricos.
9.12 Se detalla a continuación los códigos de los impuestos.
TABLA 16
Impuesto Código
IVA 2
ICE 3
IRBPNR 5
9.13 Se enlista a continuación los códigos de las tarifas de los impuestos:

17: TARIFA DEL IVA

Porcentaje de IVA Código
0% 0
12% 2
14% 3
15% 4
5% (^) 5
No Objeto de Impuesto 6
Exento de IVA 7
IVA diferenciado^3 8
13% 10
TABLA 18: TARIFA DEL ICE:
A continuación, se detalla el listado de códigos del Impuesto a los Consumos
Especiales, las tarifas deberán calcularse en base a la normativa vigente.
Código Descripción
Tarifa Ad
Valorem
enero 2023
Tarifa
específica de
enero 2023
Tarifa Ad
Valorem
febrero a
diciembre
2023
Tarifa
específica de
febrero a
diciembre
2023
3011 ICE Cigarrillos Rubios - 0,17 - 0,16
3021 ICE Cigarrillos Negros - 0,17 - 0,16
3023 ICE Productos del Tabaco y Sucedáneos del Tabaco excepto Cigarrillos 150% - 150% -
3031 ICE Bebidas Alcohólicas 75% 10,36 75% 10,00
3041 ICE Cerveza Industrial Gran Escala 75% - 75% -
3041 ICE Cerveza Industrial Mediana Escala 75% - 75% -
3041 ICE Cerveza Industrial Pequeña Escala 75% - 75% -
3073 ICE Vehículos Motorizados cuyo PVP sea hasta de 20000 USD 5% - 5% -
3075 ICE Vehículos Motorizados PVP entre 30000 y 40000 15% - 15% -
3077 ICE Vehículos Motorizados cuyo PVP superior USD 40.000 hasta 50.000 20% - 20% -
3078 ICE Vehículos Motorizados cuyo PVP superior USD 50.000 hasta 60.000 25% - 25% -
3079 ICE Vehículos Motorizados cuyo PVP superior USD 60.000 hasta 70.000 30% - 30% -
3080 ICE Vehículos Motorizados cuyo PVP superior USD 70.000 35% - 35% -
3081 ICE Aviones, Tricares, yates, Barcos de Recreo 15% - 10% -
3092 ICE Servicios de Televisión Prepagada 0% - 0% -
3610 ICE Perfumes y Aguas de Tocador 20% - 20% -
3620 ICE Videojuegos 0% - 0% -
3630 ICE Armas de Fuego, Armas deportivas y Municiones 300% - 30% -
(^3) Mediante decreto ejecutivo el presidente de la República podrá aplicar una tafia de IVA diferenciada del 8% para el sector turístico hasta 12
días al año según se establezca en el respectivo decreto.

Código Descripción

Tarifa Ad
Valorem
enero 2023
Tarifa
específica de
enero 2023
Tarifa Ad
Valorem
febrero a
diciembre
2023
Tarifa
específica de
febrero a
diciembre
2023
3640 ICE Focos Incandescentes 100% - 100% -
3660 ICE AccionesCuotas Membresías Afiliaciones 35% - 35% -
3093 ICE Servicios Telefonía Sociedades 15% - 15% -
3101 ICE Bebidas Energizantes 10% - 10% -
3053 ICE Bebidas Gaseosas con Alto Contenido de Azúcar -
0,19 por 100
gramos de
azúcar
-
0,18 por 100
gramos de
azúcar
3054 ICE Bebidas Gaseosas con Bajo Contenido de Azúcar 10% - 10% -
3111 ICE Bebidas No Alcohólicas -
0,19 por 100
gramos de
azúcar
-
0,18 por 100
gramos de
azúcar
3043 ICE Cerveza Artesanal - 1,55 - 1,50
3033 ICE Alcohol 75% 10,36 75% 10,00
3671 ICE calefones y sistemas de calentamiento de agua a gas SRI 100% - 100% -
3684
ICE vehículos motorizados camionetas y de
rescate cuyo PVP sea hasta DE 30.000
USD
5% - 5% -
3686
ICE vehículos motorizados excepto
camionetas y de rescate cuyo PVP sea
superior USD 20.000 hasta DE 30.000
10% - 10% -
3688 ICE vehículos híbridos cuyo PVP sea de hasta USD. 35.000 0% 0%
3691 ICE vehículos híbridos cuyo PVP superior USD. 35.000 hasta 40.000 8% 8%
3692 ICE vehículos híbridos cuyo PVP superior USD. 40.000 hasta 50.000 14% 14%
3695 ICE vehículos híbridos cuyo PVP superior USD. 50.000 hasta 60.000 20% 20%
3696 ICE vehículos híbridos cuyo PVP superior USD. 60.000 hasta 70.000 26% 26%
3698 ICE vehículos híbridos cuyo PVP superior a USD 70.000 32% - 32% -
3682 ICE consumibles tabaco calentado y líquidos con nicotina SRI 150% - 150% -
3681 ICE servicios de telefonía móvil personas naturales 0% - 0% -
3680 ICE fundas plásticas - 0,10 - 0,08
3533 ICE Import. Bebidas Alcohólicas 75% - 75% -
3541 ICE Cerveza Gran Escala CAE 75% - 75% -
3541 ICE Cerveza Industrial de Mediana Escala CAE 75% - 75% -
3541 ICE Cerveza Industrial de Pequeña Escala CAE 75% - 75% -
3542 ICE Cigarrillos Rubios CAE - - - -
3543 ICE Cigarrillos Negros CAE - - - -
3544 ICE Productos del Tabaco y Sucedáneos del Tabaco Excepto Cigarrillos CAE 150% - 150% -
3581 ICE Aeronaves CAE 15% - 10% -
3582 ICE Aviones, Avionetas y Helicópteros Exct. Aquellos destinados Al Trans. CAE 15% - 10% -
3710 ICE Perfumes Aguas de Tocador Cae 20% - 20% -
3720 ICE Video Juegos CAE 35% - 35% -
3730 ICE Importaciones Armas de Fuego, Armas deportivas y Municiones CAE 300% - 30% -
3740 ICE Focos Incandescentes CAE 100% - 100% -
3871 ICEhasta de 20000 USD SENAE-vehículos motorizados cuyo PVP SEA 5% - 5% -
Código Descripción
Tarifa Ad
Valorem
enero 2023
Tarifa
específica de
enero 2023
Tarifa Ad
Valorem
febrero a
diciembre
2023
Tarifa
específica de
febrero a
diciembre
2023
3873 ICE30000 Y 40000 SENAE-vehículos motorizados PVP entre 15% - 15% -
3874 ICEsuperior USD 40.000 hasta 50.000 SENAE-vehículos motorizados cuyo PVP 20% - 20% -
3875 ICEsuperior USD 50.000 hasta 60.000 SENAE-vehículos motorizados cuyo PVP 25% - 25% -
3876 ICEsuperior USD 60.000 hasta 70.000 SENAE-vehículos motorizados cuyo PVP 30% - 30% -
3877 ICEsuperior USD 70.000 SENAE-vehículos motorizados cuyo PVP 35% - 35% -
3878 ICERec SENAE-Aviones, Tricares, Yates, Barcos de 15% - 10% -
3601 ICE Bebidas Energizantes SENAE 10% - 10% -
3552 ICE bebidas gaseosas con alto contenido de azúcar SENAE -
0,19 por 100
gramos de
azúcar
-
0,18 por 100
gramos de
azúcar
3553 ICE bebidas gaseosas con bajo contenido de azúcar SENAE 10% - 10% -
3602 ICE bebidas no alcohólicas SENAE -
0,19 por 100
gramos de
azúcar
-
0,18 por 100
gramos de
azúcar
3545 ICE cerveza artesanal SENAE 75% 1,55 75% 1,5
3532 ICE Import. alcohol SENAE 75% 10,36 75% 10
3671 ICE calefones y sistemas de calentamiento de agua a gas SRI 100% - 100% -
3771 ICE calefones y sistemas de calentamiento de agua a gas SENAE 100% - 100% -
3685
ICE vehículos motorizados camionetas y de
rescate PVP sea hasta DE 30.000 USD
SENAE
5% - 5% -
3687
ICE vehículos motorizados excepto
camionetas y de rescate cuyo PVP sea
superior USD 20.000 hasta de 30.000
SENAE
10% - 10% -
3689 ICE vehículos híbridos cuyo PVP sea de hasta USD. 35.000 SENAE 0% - 0% -
3690 ICE vehículos híbridos cuyo PVP superior USD. 35.000 hasta 40.000 SENAE 8% - 8% -
3693 ICE vehículos híbridos cuyo PVP superior USD. 40.000 hasta 50.000 SENAE 14% - 14% -
3694 ICE vehículos híbridos cuyo PVP superior USD. 50.000 hasta 60.000 SENAE 20% - 20% -
3697 ICE vehículos híbridos cuyo PVP superior USD. 60.000 hasta 70.000 SENAE 26% - 26% -
3699 ICE vehículos híbridos cuyo PVP superior a USD 70.000 SENAE 32% - 32% -
3683 ICE consumibles tabaco calentado y líquidos con nicotina SENAE 150% 50%
9.14 Lista de códigos por impuestos asignados para la retención.

TABLA 19
Impuesto a retener Código
RENTA 1
IVA 2
ISD 6

9.15 Se describe los códigos por impuesto de acuerdo con el porcentaje de
retención.

TABLA 20: TABLAS DE RETENCIONES

RETENCIÓN DEL IVA

Porcentaje IVA Código
10% 9
20% 10
30% 1
50% 11
70% 2
100% 3
Retención en cero:
Porcentaje IVA Código
0.00% 7
*Aplica de conformidad con la Disposición Transitoria Única de la Resolución NAC-DGERCGC15-00000284.
No procede retención:
Porcentaje IVA Código
0.00% 8
RETENCIÓN DEL ISD

Porcentaje
ISD Código^
Vigencia
Desde Hasta
5% 4580 - Hasta el 31 de diciembre de 202 1
4.75% 4580 1 de enero de 2022 31 de marzo de 2022^4
4.50% 4580 1 de abril de 2022 30 de junio de 2022
4.25% 4580 1 de julio de 2022 30 de septiembre de 2022
4.00% 4580 1 de octubre de 2022 31 de diciembre de 2022
3.75% 4580 1 de febrero de 2023 30 de junio de 2023^5
3.50% 4580 1 de julio de 2023^6 31 de marzo de 202 4
5% 4580 1 de abril del 2024^7
(^4) Los porcentajes para el año 2022 se establecieron en el Decreto Ejecutivo No. 298 publicado en el Segundo Suplemento del Registro Oficial
No. 604 del 23 de diciembre de 2021.
(^5) Los porcentajes para el año 2023 se establecieron en el Decreto Ejecutivo No. 643 publicado en el Segundo Suplemento del Registro Oficial
No. 235 del 23 de enero de 2023.
(^6) El porcentaje para el año 2024 se establecieron en el Decreto Ejecutivo No. 98 publicado en el Segundo Suplemento del Registro Oficial No.
467 del 2 9 de diciembre de 202 3
(^7) Ley Orgánica para Enfrentar el Conflicto Armado Interno, la Crisis Social y Económica publicada en el Registro Oficial No. 516 del 12 de
marzo de 2024 que reforma el artículo 162 de la Ley. Reformatoria para la Equidad Tributaria en el Ecuador.

Porcentaje
ISD Código^
Vigencia
Desde Hasta
2.5% 4586 1 de mayo de 2025^8
RETENCIÓN DE RENTA:
Los porcentajes de retención del Impuesto a la Renta se aplicarán conforme lo
establecido en la normativa legal vigente, para lo cual se deberán considerar los
porcentajes establecidos en las tablas del Catálogo de Anexo Transaccional
Simplificado, publicado en la página web http://www.sri.gob.ec: Información sobre
impuestos/Cómo declaro mis impuestos?/Anexos y guías o directamente a través
del siguiente link: http://www.sri.gob.ec/web/guest/formularios-e-instructivos1.
9.16 A continuación, se detallan los valores subtotales y totales con impuestos
que deben constar en los comprobantes de venta, según el caso.
TABLA 2 1
No. Campos de valores Tipo de campo Requisito

1 Sub total IVA _%:
Sumarán todos los precios totales de los
productos gravados con la tarifa de IVA
vigente.
Obligatorio
2 Sub total 0%: Sumarán todos los precios totales de los productos gravados con tarifa de IVA 0%. Obligatorio
3 Sub total IVA:no objeto Sumarán todos los precios totales de los productos No Objeto de IVA. Obligatorio
3 Sub total IVA:exento de Sumarán todos los precios totales de los productos Exento de IVA. Obligatorio
4 Sub total: Sumará las tres bases (Tarifa de IVA vigente, 0%, no objeto de IVA o Exento de IVA). Obligatorio
5 Total descuento: Sumará los valores de los descuentos. Obligatorio
6 Valor ICE:
Calculará del campo Sub total según el
porcentaje ingresado, este campo será editable
por la naturaleza del cálculo del impuesto.
Obligatorio cuando corresponda /
editable
7 Valor IRBPNR: Este campo será editable por la naturaleza del cálculo del impuesto. Obligatorio cuando corresponda / editable
8 Valor IVA _%:
Sumará el campo Sub total IVA _% y el valor
del campo Valor ICE, el resultado aplicará la
tarifa de IVA vigente.
Obligatorio
9 Propina:
Este campo aparecerá vacío, si ingresa un
valor el sistema validará que el valor ingresado
no supere el 10% del campo Sub total
Obligatorio
10 VALOR TOTAL Sumará los campos Sub total, ICE, IRBPNR, Valor IVA _% y Propina. Obligatorio
9.17 El formato para todo campo correspondiente a valores será 123456.98
utilizando el punto como separador de decimales; se utilizará como máximo
(^8) El porcentaje para mayo de 2025 se estableció en el Decreto ejecutivo No. 589 publicado en el Registro Oficial – Cuarto Suplemento 9 del 31
de marzo de 2025.

dos decimales, a excepción de los campos de precio unitario y cantidad que
se podrá utilizar hasta 6 decimales, aplica para versión de comprobantes
1.1.0 (Anexo 3)
9.18 Los contribuyentes deberán implementar los controles necesarios en sus
sistemas informáticos que utilizan para la generación de comprobantes
electrónicos, con el fin de que los comprobantes sean emitidos en orden
cronológico y secuencial, controlando que no exista duplicidad tanto en la
secuencia como en las claves de acceso; así como también evitar el reenvío
innecesario de comprobantes para su autorización

9.19 Las representaciones impresas de los comprobantes electrónicos (RIDE),
tendrán validez tributaria y jurídica (Resolución 233 de junio 2018); como
anexos se adjuntan modelos en los cuales se detallan las posiciones de los
requisitos. Se podrán imprimir datos adicionales en el RIDE conforme lo
requiera el contribuyente.

9.20 En las representaciones impresas el emisor podrá incorporar un código de
barras que contenga la clave de acceso u otro código opcional de información
que el contribuyente crea importante para sus procesos.

9.21 Se recomienda revisar aspectos técnicos acerca de la ubicación de la
impresión de código de barras (GS1 – 128), para que puedan ser leídos por
máquinas lectoras de códigos de barras. Para más información pueden
ingresar a la siguiente dirección: http://www.gs1mexico.org/site/wp-
content/uploads/2012/06/Guia-codigo-GS1-128.pdf

10. Caso específico de retenciones en la
comercializadores / Distribuidores de
derivados del petróleo y retención presuntiva
de IVA a los editores, distribuidores y
voceadores que participan en la
comercialización de periódicos y/o revistas.
Los comercializadores y distribuidores de derivados de petróleo, deberán aplicar
los siguientes códigos de impuestos y tarifas de retenciones para la emisión de sus
facturas:

TABLA 2 2

IMPUESTO A RETENER CÓDIGO
IVA PRESUNTIVO Y RENTA 4
TABLA 2 3
Retención IVA
PORCENTAJE IVA RETENIDO Y/O PRESUNTIVO CÓDIGO TARIFA EN EL XML
** 100%^9 3 1
12% (Retención de IVA presuntivo por Editores a
Margen de Comercialización Voceadores) 4 0.12^
* 100% (Retención IVA venta periódicos y/o Revistas a
Distribuidores) 5 100
* 100% (Retención IVA Venta de Periódicos y/o
revistas a voceadores) 6 100
*Aplica para comprobantes de retención.
** Aplica para las retenciones de IVA de conformidad con Resolución No. NAC-DGERCGC21- 00000063.
Ejemplo
DESCRIPCIÓN %
LLENADO DEL XML
<codigo> <codigoPorcentaje> <tarifa> <valor>^10
Gasolina súper 13% 4 3 1 IVA EN VENTAS * 13%
Gasolina extra o
Ecopaís 5.85%^4 3 1 IVA EN VENTAS * 5.85%^
Diesel 4% 4 3 1 IVA EN VENTAS * 4%
Otros derivados
de petróleo 100%^4 3 1
IVA DEL MARGEN DE
COMERCIALIZACIÓN * 100%
Retención RENTA
PORCENTAJE RENTA CÓDIGO TARIFA EN EL XML
0.002 (2 por mil) 327 0.20
0.003 (3 por mil) 328 0.30
11. Códigos de errores y advertencias de validación
CÓDIGO
DE
ERROR
DESCRIPCIÓN MOTIVO DE ERROR
VALIDACIÓN:
RECEPCIÓN
/AUTORIZACIÓN/
EMISOR
2
RUC del emisor se
encuentra NO
ACTIVO.
Verificar que el número de RUC se encuentre en estado
ACTIVO AUTORIZACIÓN^
10
Establecimiento del
emisor se encuentra
Clausurado.
No se autorizará comprobantes si el establecimiento
emisor ha sido clausurado, automáticamente se habilitará
el servicio una vez concluido la clausura.
AUTORIZACIÓN
26 Tamaño máximo superado Tamaño del archivo supera lo establecido RECEPCIÓN
(^9) Para el llenado de la sección de IVA presuntivo en el XML de la factura electrónica se utilizará este código y la tarifa; sin embargo, los valores de los
porcentajes de retención corresponderán a los establecidos en la Resolución Nro. NAC-DGERCGC21-00000063.
10 Para el llenado de esta etiqueta se debe considerar el tipo de campo, formato y longitud establecida en el anexo 3.

CÓDIGO
DE
ERROR

DESCRIPCIÓN MOTIVO DE ERROR
VALIDACIÓN:
RECEPCIÓN
/AUTORIZACIÓN/
EMISOR
27 Clase no permitido La clase del contribuyente no puede emitir comprobantes electrónicos. AUTORIZACIÓN
28
Acuerdo de medios
electrónicos no
aceptado
Siempre el contribuyente debe haber aceptado el acuerdo
de medio electrónicos en el cual se establece que se
acepta que lleguen las notificaciones al buzón del
contribuyente.
RECEPCIÓN
35 Documento inválido Cuando el XML no pasa validación de esquema. RECEPCIÓN
36 Versión esquema descontinuada Cuando la versión del esquema no es la correcta. RECEPCIÓN
37 RUC sin autorización de emisión Cuando el RUC del emisor no cuenta con una solicitud de emisión de comprobantes electrónicos. AUTORIZACIÓN
39 Firma inválida Firma electrónica del emisor no es válida. AUTORIZACIÓN
40 Error en el certificado No se encontró el certificado o no se puede convertir en certificad X509. AUTORIZACIÓN
43 Clave acceso registrada Cuando la clave de acceso ya se encuentra registrada en la base de datos. RECEPCIÓN
45 Secuencial registrado Secuencial del comprobante ya se encuentra registrado en la base de datos RECEPCIÓN
46 RUC no existe Cuando el RUC^ emisor no existe en el Registro Único de Contribuyentes. AUTORIZACIÓN
47 Tipo de comprobante no existe exista en el catálogo de nuestros tipos de comprobantes.Cuando envían en el tipo de comprobante uno que no RECEPCIÓN
48 Esquema XSD no existe Cuando el esquema para el tipo de comprobante enviado no existe. RECEPCIÓN
49 (^) envían al WS nulosArgumentos que Cuando se consume el WS con argumentos nulos. RECEPCIÓN
50 Error interno general Cuando ocurre un error inesperado en el servidor. RECEPCIÓN
52 Error en diferencias Cuando existe error en los cálculos del comprobante. AUTORIZACIÓN
56 Establecimiento cerrado Cuando el establecimiento desde el cual se genera el comprobante se encuentra cerrado. AUTORIZACIÓN
57 Autorización suspendida
Cuando la autorización para emisión de comprobantes
electrónicos para el emisor se encuentra suspendida por
procesos de control de la Administración Tributaria o el
contribuyente no tenía la autorización para emitir
comprobantes electrónicos a la fecha de emisión del
comprobante

AUTORIZACIÓN
58 Error en la estructura de clave acceso Cuando la clave de acceso tiene componentes diferentes a los del comprobante. AUTORIZACIÓN
63 RUC clausurado Cuando el RUC del emisor se encuentra clausurado por procesos de control de la Administración Tributaria. AUTORIZACIÓN
65 Fecha de emisión extemporánea
Cuando el comprobante emitido no fue enviado de
acuerdo con el tiempo del tipo de emisión en el cual fue
realizado.
EMISOR/
RECEPCIÓN
67 Fecha inválida Cuando existe errores en el formato de la fecha. RECEPCIÓN
70 Clave de acceso en procesamiento
Cuando se desea enviar un comprobante que ha sido
enviado anteriormente y el mismo no ha terminado su
procesamiento.
RECEPCIÓN
CÓDIGO
DE
ERROR

DESCRIPCIÓN MOTIVO DE ERROR
VALIDACIÓN:
RECEPCIÓN
/AUTORIZACIÓN/
EMISOR
80 Error en la estructura de clave acceso
Cuando se ejecuta la consulta de autorización por clave de
acceso y el valor de este parámetro supera los 49 dígitos,
tiene caracteres alfanuméricos o cuando el tag
(claveAccesoComprobante) está vacío
AUTORIZACIÓN
82 Error en la fecha de inicio de transporte Cuando la fecha de inicio de transporte es menor a la fecha de emisión de la guía de remisión. RECEPCIÓN
92
Error al validar monto
de devolución del
IVA.
Cuando el valor registrado en el campo de devolución del
IVA, en facturas y notas de débito, no corresponde al que
fue autorizado por el servicio web DIG.
RECEPCIÓN
Notas :
1. Todos aquellos comprobantes que hayan sido rechazados por cualquiera de
los errores señalados en la tabla anterior pueden ser reenviados para su
autorización una vez corregido el error motivo del rechazo sin generar nuevos
números de clave de acceso o secuenciales para los comprobantes. A
excepción de aquellos casos específicos en los que aun cuando el archivo
esté correcto, el sistema no pueda autorizar el comprobante debido a algún
impedimento como, por ejemplo: en el caso de RUC o establecimiento
clausurado, RUC inactivo, establecimiento cerrado, entre otros. Los
comprobantes devueltos no se guardarán en la base de datos del SRI, se
almacenarán únicamente los comprobantes que no hayan sido autorizados.
2. En el caso del error con código 70 – Clave de acceso en procesamiento, no
se deberá reenviar el comprobante o generar el comprobante con otra clave
de acceso y secuencial hasta recibir una respuesta de autorización o rechazo
del mismo, en un tiempo máximo de 24 horas.

CÓDIGO DE
ADVERTENCIA DESCRIPCIÓN^ POSIBLE SOLUCIÓN^
59 Identificación no existe Cuando el número de la identificación del adquirente no existe.
60 Ambiente ejecución.
Siempre que el comprobante sea emitido en ambiente de
certificación o pruebas se enviará como parte de la autorización
esta advertencia.
62 Identificación incorrecta
Cuando el número de la identificación del adquirente del
comprobante está incorrecto. Por ejemplo, cédulas no pasan el
dígito verificador.
68 Documento sustento Cuando el comprobante relacionado no existe como electrónico.
Al momento de receptar el archivo se realizarán las siguientes validaciones,
según el orden especificado, cabe mencionar que estas no serán revisadas
en su totalidad; es decir, si el sistema detecta como inconsistente el tamaño
del archivo ese será el error remitido:
ORDEN VALIDACIÓN DESCRIPCIÓN
1 Validación XML
Tamaño archivo
Esquema activo
XML bien formado y válido
2 Validación contribuyente emisor (^) Establecimiento activoRUC activo^

ORDEN VALIDACIÓN DESCRIPCIÓN
Autorización para emitir comprobantes electrónicos
activa
Autorización para emisión del tipo de comprobante
3 Validación unicidad
Clave acceso única
Secuencial único
Clave acceso bien formada
4 Validación Firma Validez firma y cadena de confianzaOCSP
5 Verificaciones adicionales
Fecha emisión
identificación del receptor del comprobante
documentos de sustento
6 Validación diferencias
3. Las validaciones que se muestran a continuación deberán ser implementadas
en el sistema del contribuyente, a fin de garantizar que la información
transmitida a la base de datos del SRI cumpla con los requisitos establecidos
en la normativa tributaria y comercio electrónico.

CÓDIGO
DE
ERROR

DESCRIPCIÓN POSIBLE SOLUCIÓN
VALIDACIÓN:
RECEPCIÓN
/AUTORIZACIÓN/
EMISOR
34 Comprobante no autorizado Cuando el comprobante no ha sido autorizado como parte de la solicitud de emisión del contribuyente. EMISOR
42 Certificado revocado Certificado que ha superado su fecha de caducidad, y no ha sido renovado. EMISOR
52 Error en diferencias Cuando existe error en los cálculos del comprobante. EMISOR
64 Código documento sustento
Cuando el código del documento sustento no existe en el
catálogo de documentos que se tiene en la
Administración.
EMISOR
65 Fecha de emisión extemporánea
Cuando el comprobante emitido no fue enviado de
acuerdo con el tiempo del tipo de emisión en el cual fue
realizado.
EMISOR/
RECEPCIÓN
69 Identificación del receptor
Cuando la identificación asociada al adquirente no existe.
En general cuando el RUC del adquirente no existe en el
Registro Único de Contribuyentes.
EMISOR
Para acceder al catastro de RUC podrán descargarlo desde el siguiente link:
http://www.sri.gob.ec/web/guest/catastros
12. Códigos de error para aplicación de la devolución automática del IVA
CÓDIGO DE VALIDACIÓN DESCRIPCIÓN
2000 EXITO Éxito.
2001 EXITO_VALIDACION Validación exitosa.
CÓDIGO DE VALIDACIÓN DESCRIPCIÓN
3000 ERROR_FORMATO Estimadomplen con^ contribuyente, el formato establecido.^ los^ campos registrados^ no^ cu

3001 ERROR_TRANSACCION No se logró hacer la transacción.

3003 ERROR_CLAVE_YA_PROCESADA Comprobante ya procesado.

3004 ERROR_CODIGO_OPERACION_INVALIDO Código operación inválido.

3005 ERROR_INTERNO_SERVIDOR Se ha producido un error.

3006 ERROR_TIME_OUT No se ha podido responder a tiempo.

3007 ERROR_CODIGO_BENEFICIO Estimado contribuyente, el código de verificación ingresado no se encuentra vigente.

3008 ERROR_WS_NO_DISPONIBLE WEB Service no disponible.

3009 ERROR_WS_NO_AUTORIZADO No está autorizado.

3011 ERROR_NO_MONTO_MINIMO El valor no cumple en monto mínimo a devolver.

3013 ERROR_TARIFA_IMPUESTO La tarifa del impuesto calculado no coincide con el enviado.

3014 ERROR_TOKEN_BENEFICIARIO El token no pertenece al emisor.

3015
ERROR_CANAL_AUTOMATICO_NO_HABILITADO_
SALDO

El beneficiario no tiene el canal automático habilitado o
no existe un monto a devolver.
3016:
ERROR_MULTIPLE_TRANSACCIONES_LOTE

El beneficiario registra más de una
transacción en el lote enviado
4000 LOTE_RECIBIDO Lote recibido.

4001 LOTE_RECHAZADO Lote rechazado.

4002 LOTE_EN_PROCESO Lote en proceso.

4003 LOTE_PROCESADO Lote procesado.

5001 ERROR_CONFIGURACION_PILOTO Los parámetros: rucs, fecha inicio o fecha fin para el piloto no están configurados.

6000 ANULACION_ERROR Error al realizar la anulación.

6001
ANULACION_COMPROBANTE_NO_ENCONTRAD
O

No se realizó la transacción: comprobante electrónico
no encontrado.
6002 ANULACION_IVA_DEVOLVER_DIFERENTES No se realizó la transacción: el monto del IVA a devolver no es igual al que se registró en el débito.

6004 ANULACION_BENEFICIO_INCORRECTO El código del beneficio no corresponde al registrado en el SRI.

6005 ANULACION_FECHA_DIFERENTE_HOY La fecha de emisión no corresponde a la de hoy.

6006
ANULACION_TIPO_COMPROBANTE_INCORRECT
O

El comprobante tiene que ser una factura o una nota de
débito.
CÓDIGO DE VALIDACIÓN DESCRIPCIÓN
6007 ANULACION_ENVIADA_EXITO Anulación enviada con éxito.
6008
ANULACION_IVA_DEVOLVER_NO_ENCONTRADO
No se realizó la transacción: falta el monto del IVA a
devolver.
6009
ANULACION_IDENTIFICACIONES_DIFERENTES
No se realizó la transacción: la identificación no
corresponde a la clave de acceso.
6010
ANULACION_IVA_CALCULADO_DIFERENTES
No se realizó la transacción: el cálculo del IVA a
devolver no es igual al que se registró en el débito.
6011
ANULACION_BASE_IMPONIBLE_DIFERENTES
No se realizó la transacción: la base imponible no es
igual a la que se registró en el débito.
6012 ANULACION_NO_SALDO_DISPONIBLE No se realizó la transacción: no tiene saldo disponible.
7000
BENEFICIARIOS_ARCHIVO_NO_ENCONTRADO No existen registros en el canal automático.^
13. Servicios web para la devolución automática del IVA a personas adultas mayores - DIG............
13.1 Los enlaces WEB Service habilitados para los emisores electrónicos son los
siguientes:

Servicio para obtención de lista de beneficiarios
Servicio para la recepción de información por lotes
Servicio para la consulta de información por lote (respuesta)
Servicio para la recepción de información individual
Servicio para anulación de descuento de devolución del IVA
Existen dos ambientes disponibles para la invocación de los enlaces WEB
Service publicados por la Administración Tributaria:
Protocolo URL BASE Versión Ambiente
HTTPS https://celcer.sri.gob.ec/devolucion-iva/rest V1 Certificación producción
HTTPS https://srienlinea.sri.gob.ec/devolucion-iva/rest V1 Producción producción
Uno es para el ambiente de pruebas, donde cada contribuyente certificará que
su aplicación funcione correctamente con cada tipo de comprobante
electrónico.
El segundo es para el ambiente de producción, al cual cada contribuyente
deberá acceder una vez que ha realizado las pruebas y esté seguro de que su
aplicación funciona correctamente.
Se deberá configurar el dominio para el consumo de los enlaces WEB Service
dependiendo del ambiente a utilizar.
13.2 La seguridad para los servicios será provista mediante tokens del protocolo
OAuth2:

Protocolo URL Autenticación Versión
HTTPS
https://celcer.sri.gob.ec/sri-seguridad-sso-api-servicio-internet/rest/seguridad-sso-
rest/access-
token/ RUC_CONTRIBUYENTE [AD] CEDULA_ADICIONAL / CLAVE_ENCRIPTAD
A_ADICIONAL_SHA- 512
V1
HTTPS
https://srienlinea.sri.gob.ec/sri-seguridad-sso-api-servicio-internet/rest/seguridad-
sso-rest/access-
token/ RUC_CONTRIBUYENTE [AD] CEDULA_ADICIONAL / CLAVE_ENCRIPTAD
A_ADICIONAL_SHA- 512
V1
Los parámetros de “RUC_CONTRIBUYENTE”, “CEDULA_ADICIONAL” y
“CLAVE_ENCRIPTADA_ADICIONAL_SHA- 512 ” deberán ser reemplazados
con los datos propios del contribuyente emisor electrónico.
El Token tendrá una vigencia de 35 minutos.
En las llamadas a los servicios se deberá incluir el token generado como un
parámetro de cabecera con la etiqueta Authorization.
Tipo de operación Parámetros Tipo de dato (^) parámetrosTipo de Tamaño
GET
USUARIO_ADICIONAL
CLAVE_ENCRIPTADA_
ADICIONAL_ SHA- 512
String
string
Query
Query

13
Para los valores monetarios que son variables de entrada o salida de los
servicios deberán ser enviados o receptados con una precisión de dos
decimales.
13.3 Servicio web para obtención de lista de beneficiarios

La información que se requiere para el consumo de este servicio es:
RUC del emisor electrónico
El dato que devolverá el servicio es:
Un archivo zip “cedulas_canal” que contiene las identificaciones de las
personas que se encuentran habilitadas para acceder al beneficio por
el mecanismo automático.
Esta información se actualizará diariamente entre las 0:30 am y 2:00 am, y
estará disponible durante el día.
Para utilizar el servicio se deberá considerar lo siguiente:
Método URL
POST /devolucionesBeneficiarios
URL BASE CON EL SERVICIO Ambiente
https://celcer.sri.gob.ec/devolucion-iva/rest/devolucionesBeneficiarios CertificaciónProducción
https://srienlinea.sri.gob.ec/devolucion-
iva/rest/devolucionesBeneficiarios
Producción
Producción
Tipo de operación Parámetros Tipo de dato Tipo de parámetros
HEAD
POST
Token
ruc
String
string
Header
body
13.4 Servicio web para la recepción de información por lotes

El servicio para la recepción de información por lotes devolverá un código de
operación por el lote (lista datos enviados).
La longitud máxima de la lista de datos será de diez mil elementos, si los
emisores requieren enviar listas más largas deberán dividir los datos y hacer
uso varias veces del servicio.
La información que se requiere en este servicio por parte del emisor
electrónico en cada uno de los ítems de la lista de datos deberá contener:
RUC del emisor electrónico
Clave de acceso del comprobante
Identificación del beneficiario (cédula)
Base imponible gravada diferente a cero (subtotal del comprobante con
IVA gravada diferente a cero)
Tarifa (porcentaje) del IVA diferente de cero
Monto del IVA diferente de cero
El dato que devolverá el servicio es:
Código de operación (código lote)
Para utilizar el servicio se deberá considerar lo siguiente:
Método URL
POST /devolucionesLotesRecepciones
URL BASE CON EL SERVICIO Ambiente
https://celcer.sri.gob.ec/devolucion-iva/rest/devolucionesLotesRecepciones CertificaciónProducción
https://srienlinea.sri.gob.ec/devolucion-iva/rest/devolucionesLotesRecepciones ProducciónProducción^
Tipo de operación Parámetros Tipo de dato Tipo de parámetro
HEAD
POST
Token
datosBeneficio
String
json
header
body
Trama que recibe Trama de respuesta
DatosBeneficio:
type: array
properties:
rucEmisor:
required: true
type: string
claveAccesoComprobante:
required: true
type: string
idBeneficiario:
required: true
type: string
baseImponible:
required: true
type: number
porcentajeIva:
required: true
type: number
montoIva:
requiered: true
type: number
Respuesta:
type: object
properties:
codigoLote
required: true
type: string
MensajeRespueta
required: true
type: string
MensajeRespuesta:
type: string
13.5 Servicio web para la respuesta de información por lotes (respuesta)

Con el código de operación que se obtuvo del servicio para la recepción de
información por lote, se podrán consultar los resultados de los descuentos de
cada ítem de la lista de datos enviados anteriormente.
La información que se requiere en este servicio por parte del emisor
electrónico es:
Código de operación (Código lote)
Los datos que devolverá el servicio es una lista de objetos cuyos atributos
son:
Clave de acceso del comprobante
Valor del descuento IVA.
Mensaje asociado al valor.
Para utilizar el servicio se deberá considerar lo siguiente:
Método URL
POST /devolucionesLotesRespuestas
URL BASE CON EL SERVICIO Ambiente
https://celcer.sri.gob.ec/devolucion-iva/rest/devolucionesLotesRespuestas CertificaciónProducción
https://srienlinea.sri.gob.ec/devolucion-iva/rest/devolucionesLotesRespuestas ProducciónProducción^
Tipo de operación Parámetros Tipo de dato Tipo de parámetro
HEAD
POST
Token
codigoOperacion
String
string
Header
body
Trama que recibe Trama de respuesta
codigoLote
body:
application/json:
type: Respuesta
required: true
example:
{listaDescuento:
[
{ "claveAcceso": 12345678901234567890123454545454,
"valor": 1,”descripcion”:”aprobado”},
{ "claveAcceso": 12345678901234567890123454545455,
"valor": 2,”descripcion”:”aprobado”}
] , “codigo”: “4003”,”mensaje”:”lote_procesado”}
13.6 Servicio web para la recepción de información individual

Este servicio estará disponible para aquellos emisores electrónicos cuya
facturación se genere a demanda del cliente.
La información que se requiere en este servicio por parte del emisor
electrónico es:
RUC del emisor electrónico
Clave de acceso del comprobante
Identificación del beneficiario (cédula)
Código de acceso otorgado al beneficiario
Base imponible gravada diferente a cero (Subtotal del comprobante con
IVA gravada diferente a cero)
Tarifa(porcentaje) del IVA diferente de cero
Monto del IVA diferente de cero
Los datos que devolverá el servicio son:
Mensaje asociado al valor
Valor del descuento IVA
Nota: el código de confirmación en el ambiente de “Certificación Producción”
es 1234 para los beneficiarios que se encuentren en el servicio web para
obtención de lista de beneficiarios.
Para utilizar el servicio se deberá considerar lo siguiente:
Método URL
POST /devolucionesIndividualesRecepciones
URL BASE CON EL SERVICIO Ambiente
https://celcer.sri.gob.ec/devolucion-iva/rest/devolucionesIndividualesRecepciones CertificaciónProducción
https://srienlinea.sri.gob.ec/devolucion-iva/rest/devolucionesIndividualesRecepciones ProducciónProducción^
Tipo de operación Parámetros Tipo de dato Tipo de parámetro
HEAD
POST
Token
DatosBeneficio
String
Object-json
header
query
Trama que recibe Trama de respuesta
DatosBeneficio:
type: object
properties:
rucEmisor:
required: true
type: string
claveAccesoComprobante:
required: true
type: string
idBeneficiario:
required: true
type: string
codigoBeneficio:
required: true
type: string
baseImponible:
required: true
type: number
porcentajeIva:
required: true
type: number
montoIva:
required: true
type: number
Descuento:
type: object
properties:
montoIvaDevolver:
required: true
type: number
codigo:
required: true
type: String
mensaje:
required: true
type: string
13.7 Servicio web para anulación de descuento de devolución del IVA

El servicio se expone para los casos en que no se pueda concretar la
transacción entre el cliente y el local comercial del emisor electrónico.
La información que se requiere en este servicio por parte del emisor
electrónico es:
RUC del emisor electrónico
Clave de acceso del comprobante
Identificación del beneficiario (cédula)
Código de acceso otorgado al beneficiario
Base imponible gravada diferente a cero (Subtotal del comprobante con IVA
gravada diferente a cero)
Tarifa (porcentaje) del IVA diferente de cero
Monto del IVA diferente de cero
Monto IVA a devolver
El dato que devolverá el servicio es:
Mensaje de respuesta
Nota: el código de confirmación en el ambiente de “Certificación Producción”
es 1234 para los beneficiarios que se encuentren en el servicio web para
obtención de lista de beneficiarios.
Para utilizar el servicio se deberá considerar lo siguiente:
Método URL
POST /devolucionesIndividualesAnulaciones
URL BASE CON EL SERVICIO Ambiente
https://celcer.sri.gob.ec/devolucion-iva/rest/devolucionesIndividualesAnulaciones CertificaciónProducción
https://srienlinea.sri.gob.ec/devolucion-iva/rest/devolucionesIndividualesAnulaciones ProducciónProducción^
Tipo de operación Parámetros Tipo de dato Tipo de parámetro
HEAD
POST
Token
DatosAnulacion
String
object
header
body
Trama que recibe Trama de respuesta
DatosAnulacion:
type: object
properties:
rucEmisor:
required: true
type: string
claveAccesoComprobante:
required: true
type: string
idBeneficiario:
required: true
type: string
codigoBeneficio:
required: true
type: string
baseImponible:
required: true
type: number
porcentajeIva:
required: true
type: number
montoIva:
required: true
type: number
montoIvaDevolver:
required: true
type: number
mensaje:
required: true
type: string
14. Anexos
Se describe a continuación la estructura de los comprobantes electrónicos (no
incluye firma electrónica ni autorización por parte del SRI).

ANEXO 1 - FORMATOS XML VERSIÓN 1.0.0
Para el desarrollo de los XML de cualquier comprobante, se recuerda que los
campos de tipo alfanumérico no deberán contener espacios generados entre sus
caracteres, ya que esto será motivo de error de esquema que puede ocasionar
rechazo del comprobante o falta de respuesta en el envío; por ejemplo:

Error :

Av. 27 de febrero 1 - 47 y Av 10 de
Agosto

Corrección :

Av. 27 de febrero 1 - 47 y Av 10 de
Agosto

FORMATO XML FACTURA

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
Distribuidora de Suministros Nacional S.A. Obligatorio Alfanumérico Max 300

Empresa Importadora y Exportadora de Piezas </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1792146739001 Obligatorio Numérico 13

2110201101179214673900110020010000000011234567813

Obligatorio,
conforme
tabla 1
Numérico 49
01

Obligatorio,
conforme
tabla 3
Numérico 2
002 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000001 Obligatorio Numérico 9
Enrique Guerrero Portilla OE1-34 AV. Galo Plaza Lasso Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
21/10/2012 Obligatorio Fecha dd/mm/aaaa

Sebastián Moreno S/N Francisco García </ dirEstablecimiento >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
SI </ obligadoContabilidad >

Obligatorio
cuando
corresponda
Texto SI / NO
04 </ tipoIdentificacionComprador >

Obligatorio,
conforme
tabla 6
Numérico 2
001 - 001 - 000000001

Obligatorio
cuando
corresponda
Numérico 15
PRUEBAS SERVICIO DE RENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1713328506001 </ identificacionComprador > Obligatorio Alfanumérico Max 20

salinas y santiago

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
295000.00 Obligatorio Numérico Max 14
5005.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
295000.00 </ baseImponible > Obligatorio Numérico Max 14
14750.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2 </ codigoPorcentaje> Obligatorio, conforme Numérico Min 1 Max 4

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
tabla 17
5.00

Opcional,
aplica para
código
impuesto 2.
Numérico Max 14
309750.00 </ baseImponible > Obligatorio Numérico Max 14
37169.40 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

5

Obligatorio,
conforme
tabla 16
Numérico 1
5001 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
12000.00 </ baseImponible > Obligatorio Numérico Max 14
240.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
347159.40 </ importeTotal> Obligatorio Numérico Max 14

DOLAR

Obligatorio
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -

01

Obligatorio,
conforme
tabla 24
Numérico 2
347159.40 Obligatorio Numérico Max 14

30

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
10620.00 Opcional Numérico Max 14
2950.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
125BJC- 01 Obligatorio Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
CAMIONETA 4X4 DIESEL 3.7 Obligatorio Alfanumérico Max 300
10.00 Obligatorio Numérico Max 14
300000.00 Obligatorio Numérico Max 14
5000.00 Obligatorio Numérico Max 14
295000.00 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14


Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
5 </ tarifa> Obligatorio Numérico Min 1 Max 4
295000.00 Obligatorio Numérico Max 14
14750.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
12 </ tarifa> Obligatorio Numérico

Min 1 Max 4 /
2 enteros, 2
decimales
309750.00 Obligatorio Numérico Max 14
37170.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

5

Obligatorio,
conforme
tabla 16
Numérico 1
5001

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
0.02 </ tarifa> Obligatorio Numérico Min 1 Max 4
12000.00 Obligatorio Numérico Max 14
240.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -

Obligatorio - -


Obligatorio
cuando
corresponda
- -
4580

Obligatorio
cuando
corresponda
Alfanumérico Max 300
15.42x

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

FORMATO XML COMPROBANTE RETENCIÓN

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
**Obligatorio** - - **Obligatorio** - - << **Obligatorio** - -
<ambiente> 1 </ambiente>
Obligatorio,
conforme
tabla 4
Numérico 1
<tipoEmision> 1 </ tipoEmision>
Obligatorio,
conforme
tabla 2
Numérico 1
<razonSocial> Distribuidora de Suministros Nacional S.A. </razonSocial> Obligatorio Alfanumérico Max 300
<nombreComercial> Empresa Importadora y Exportadora de Piezas y Partes de Equipos
de Oficina </ nombreComercial >
Obligatorio
cuando
corresponda
Alfanumérico Max 300
<ruc> 1792146739001 </ruc> Obligatorio Numérico 13
<claveAcceso> 2410201107179214673900110020010000000011234567815 </claveAcceso>
Obligatorio,
conforme
tabla 1
Numérico 49
<codDoc> 07 </codDoc>
Obligatorio,
conforme
tabla 3
Numérico 2
<estab> 002 </estab> Obligatorio Numérico 3
<ptoEmi> 001 </ptoEmi> Obligatorio Numérico 3
<secuencial> 000000001 </secuencial> Obligatorio Numérico 9
<dirMatriz> Enrique Guerrero Portilla OE1-34 AV. GALO PLAZA LASSO </dirMatriz> Obligatorio Alfanumérico Max 300
</infoTributaria> Obligatorio - -
<infoCompRetencion> Obligatorio - -
<fechaEmision> 15/01/2012 </fechaEmision> Obligatorio Fecha dd/mm/aaaa
<dirEstablecimiento> Rodrigo Moreno S/N Francisco García </ dirEstablecimiento >
Obligatorio
cuando
corresponda
Alfanumérico Max 300
<contribuyenteEspecial> 5368 </contribuyenteEspecial>
Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
<obligadoContabilidad> SI </ obligadoContabilidad >
Obligatorio
cuando
corresponda
Texto SI / NO
<tipoIdentificacionSujetoRetenido>04</tipoIdentificacionSujetoRetenido>
Obligatorio,
conforme
tabla 6
Numérico 2
<razonSocialSujetoRetenido> Juan Pablo Chávez Núñez </razonSocialSujetoRetenido> Obligatorio Alfanumérico Max 300
<identificacionSujetoRetenido> 1713328506001 </identificacionSujetoRetenido> Obligatorio Alfanumérico Max 20
<periodoFiscal>03/2012</periodoFiscal> Obligatorio Fecha mm/aaaa
</infoCompRetencion> Obligatorio - -
<impuestos> Obligatorio - -
<impuesto> Obligatorio - -
<código> 2 </código>
Obligatorio,
conforme tabla
19
Numérico 1
<codigoRetencion> 1 </codigoRetencion>
Obligatorio,
conforme tabla
20
Alfanumérico Min 1 Max 5
<baseImponible> 101.94 </baseImponible> Obligatorio Numérico Max 14
<porcentajeRetener> 30 </porcentajeRetener>
Obligatorio,
conforme tabla
20
Numérico
Min 1 Max 5
entre enteros
y decimales
<valorRetenido> 30.58 </valorRetenido> Obligatorio Numérico Max 14
<codDocSustento> 01 </codDocSustento> Obligatorio Numérico 2
<numDocSustento> 002001000000001 </numDocSustento> Opcional Numérico 15
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
20/01/2012

Obligatorio
cuando
corresponda
Fecha dd/mm/aaaa
Obligatorio - -
Obligatorio - -

<código > 1 </código>

Obligatorio,
conforme tabla
19
Numérico 1
323B1

Obligatorio,
conforme tabla
20
Alfanumérico Min 1 Max 5
10904.50 Obligatorio Numérico Max 14

2

Obligatorio,
conforme tabla
20
Numérico Min 1 Max 5
218.09 Obligatorio Numérico Max 14
01 Opcional Numérico 2
002001000000001 Opcional Numérico 15

20/01/2012

Obligatorio
cuando
corresponda
Fecha dd/mm/aaaa
Obligatorio - -
Obligatorio - -

<código> 6 </código>

Obligatorio,
conforme tabla
19
Numérico 1
4580

Obligatorio,
conforme tabla
20
Alfanumérico Min 1 Max 5
2000 Obligatorio Numérico Max 14

5

Obligatorio,
conforme tabla
20
Numérico Min 1 Max 5
100 Obligatorio Numérico Max 14
12 Obligatorio Numérico 2
002001000000001 Opcional Numérico 15

20/01/2012

Obligatorio
cuando
corresponda
Fecha dd/mm/aaaa
Obligatorio - -
Obligatorio - -


Obligatorio
cuando
corresponda
- -
MA123456

Obligatorio
cuando
corresponda
Alfanumérico Max 300
BP2010- 01 - 0014

Obligatorio
cuando
corresponda
Alfanumérico Max 300
20000

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

FORMATO XML GUÍA DE REMISIÓN

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
Distribuidora de Suministros Nacional S.A. Obligatorio Alfanumérico Max 300
Empresa Importadora y Exportadora de Piezas y Partes de Equipos de
Oficina </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1792146739001 Obligatorio Numérico 13

2110201106179214673900100110020010000000011234567815

Obligatorio,
conforme
tabla 1
Numérico 49
06

Obligatorio,
conforme
tabla 3
Numérico 2
002 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000001 Obligatorio Numérico 9
Enrique Guerrero Portilla OE1-34 AV. Galo Plaza Lasso Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

Sebastián Moreno S/N Francisco García </ dirEstablecimiento >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Av. Eloy Alfaro 34 y Av. Libertad Esq. Obligatorio Alfanumérico Max 300
Transportes S.A. Obligatorio Alfanumérico Max 300

04

Obligatorio,
conforme
tabla 6
Numérico 2
1796875790001 Obligatorio Alfanumérico Max 13

Contribuyente Regimen Simplificado RISE

Obligatorio
cuando
corresponda
Alfanumérico Max 40
SI </ obligadoContabilidad >

Obligatorio
cuando
corresponda
Texto SI / NO
5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
21/10/2011 Obligatorio Fecha dd/mm/aaaa
22/10/2011 Obligatorio Fecha dd/mm/aaaa
MCL0827 Obligatorio Alfanumérico Max 20
Obligatorio - -
Obligatorio - -
Obligatorio - -
1716849140001 Obligatorio Alfanumérico Max 20
Alvarez Mina John Henry Obligatorio Alfanumérico Max 300
Av. Simón Bolívar S/N Intercambiador Obligatorio Alfanumérico Max 300
Venta de Maquinaria de Impresión Obligatorio Alfanumérico Max 300

0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio
cuando
corresponda
Numérico 3
Quito – Cayambe - Otavalo

Obligatorio
cuando
corresponda
Alfanumérico Max 300
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
01

Obligatorio
cuando
corresponda,
conforme tabla
3
Numérico 2
002 - 001 - 000000001

Obligatorio
cuando
corresponda
Numérico 15
2110201116302517921467390011234567891

Obligatorio
cuando
corresponda
Numérico 10 o 37 o 49
21/10/2011

Obligatorio
cuando
corresponda
Fecha dd/mm/aaaa
Obligatorio - -
Obligatorio - -
125BJC- 01 </ codigoInterno > Opcional^11 Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
CAMIONETA 4X4 DIESEL 3.7 Obligatorio Alfanumérico Max 300
10.00 Obligatorio Numérico Max 14


Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio
cuando
corresponda
- -
098568541

Obligatorio
cuando
corresponda
Alfanumérico Max 300
info@organizacion.com

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Guayaquil–12 de octubre y
Universo

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

(^11) Reglamento de Comprobantes de Venta, Retención y Documentos Complementarios. - Artículo 19, numeral 2: Descripción o concepto del
bien transferido o del servicio prestado, indicando la cantidad y unidad de medida, cuando proceda. Tratándose de bienes que están
identificados mediante códigos, número de serie o número de motor, deberá consignarse obligatoriamente dicha información.

FORMATO XML NOTA DE CRÉDITO

Nota: La tarifa de IVA corresponderá a la fecha de emisión del documento de
sustento.

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
Distribuidora de Suministros Nacional S.A. Obligatorio Alfanumérico Max 300

Empresa Importadora y Exportadora de Piezas </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1792146739001001 Obligatorio Numérico 13

2110201104179214673900110020010000000011234567812

Obligatorio,
conforme
tabla 1
Numérico 49
04

Obligatorio,
conforme
tabla 3
Numérico 2
002 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000001 Obligatorio Numérico 9
ENRIQUE GUERRERO PORTILLA OE1-34 AV. GALO PLAZA
LASSO Obligatorio^ Alfanumérico^ Max 300^
Obligatorio - -
Obligatorio - -
21/10/2012 Obligatorio Fecha dd/mm/aaaa

Sebastián Moreno S/N Francisco García </ dirEstablecimiento>

Obligatorio
cuando
corresponda
Alfanumérico Max 300
04 </ tipoIdentificacionComprador >

Obligatorio,
conforme
tabla 6
Numérico 2
PRUEBAS SERVICIO DERENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1713328506001 Obligatorio Alfanumérico Max 20

5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
SI </ obligadoContabilidad>

Obligatorio
cuando
corresponda
Texto SI / NO
Contribuyente Régimen Simplificado RISE

Obligatorio
cuando
corresponda
Alfanumérico Max 40
01

Obligatorio,
conforme
tabla 3
Numérico 2
002 - 001 - 000000001 Obligatorio Numérico 15

21/10/2011 Obligatorio Fecha dd/mm/aaaa

295000.00 Obligatorio Numérico Max 14

346920.00 Obligatorio Numérico Max 14

DOLAR

Obligatorio
cuando
corresponda
Alfanumérico Max 15
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -

Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
295000.00 </ baseImponible > Obligatorio Numérico Max 14
14750.00 Obligatorio Numérico Max 14
Obligatorio - -

Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
339250.25 </ baseImponible > Obligatorio Numérico Max 14
37170.00 Obligatorio Numérico Max 14

Obligatorio - -

Obligatorio - -

DEVOLUCIÓN Obligatorio Alfanumérico Max 300

Obligatorio - -

Obligatorio - -

Obligatorio - -

125BJC- 01 Opcional Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
CAMIONETA 4X4 DIESEL 3.7 Obligatorio Alfanumérico Max 300

10.00 Obligatorio Numérico Max 14

30000.00 Obligatorio Numérico Max 14

5000.00

Obligatorio
cuando
corresponda
Numérico Max 14
295000.00 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14


Obligatorio
cuando
corresponda

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
3072

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
5 </ tarifa>

Obligatorio
cuando
corresponda
Numérico Min 1 Max 3
295000.00 Obligatorio Numérico Max 14
14750.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
12 </ tarifa>

Obligatorio
cuando
corresponda
Numérico
Min 1 Max 4
/ 2 enteros, 2
decimales
309750.00 Obligatorio Numérico Max 14

37170.00 Obligatorio Numérico Max 14

Obligatorio - -

Obligatorio - -

Obligatorio - -
Obligatorio - -


Obligatorio
cuando
corresponda
- -
info@organizacion.com

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

FORMATO XML NOTA DE DÉBITO

Nota: la tarifa de IVA corresponderá a la fecha de emisión del documento de
sustento.

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1

Obligatorio,
conforme
tabla 2
Numérico 1
PRUEBA Obligatorio Alfanumérico Max 300

PRUEBA 2
Obligatorio
cuando
corresponda

Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13

2103201605176001321000110010010000000011234567814

Obligatorio,
conforme
tabla 1
Numérico 49
05

Obligatorio,
conforme
tabla 3
Numérico 2
001 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000001 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
21/03/2016 Obligatorio Fecha dd/mm/aaaa

PÁEZ

Obligatorio
cuando
corresponda
Alfanumérico Max 300
04

Obligatorio,
conforme
tabla 6
Alfanumérico Max 20
PRUEBA SRI Obligatorio Alfanumérico Max 300

1713328506001 Obligatorio Alfanumérico Max 20

12345

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
SI

Obligatorio
cuando
corresponda
Texto SI / NO
01

Obligatorio,
conforme
tabla 3
Numérico 2
001- 001 - 112312315 Obligatorio Numérico 15

21/03/2016 Obligatorio Fecha dd/mm/aaaa
50.0 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
12.00 Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
50.0 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
56.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

17

Obligatorio,
conforme
tabla 24
Numérico 2
56,00 Obligatorio Numérico Max 14

15

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -

Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Interés por mora Obligatorio Alfanumérico Max 300
50.00 Obligatorio Alfanumérico Max 300

Obligatorio - -

Obligatorio - -
Obligatorio - -

AMAZONAS S/N ROCA

Obligatorio
cuando
corresponda
Alfanumérico Max 300
prueba@sri.gob.ec

Obligatorio
cuando
corresponda
Alfanumérico Max 300
0222222222222 ext. 3322

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -

Obligatorio - -

ANEXO 2 - FORMATO DE REPRESENTACIONES IMPRESAS DE DOCUMENTOS ELECTRÓNICOS (RIDE)
FACTURA

Nota:

Para los contribuyentes comercializadores de derivados de petróleo, y, Editores, Distribuidores y Voceadores que
participan en la comercialización de periódicos y/o revistas, deberán ajustar el formato RIDE de acuerdo con la
información contenida en el comprobante electrónico con respecto a las retenciones. Se podrán imprimir datos
adicionales en el RIDE conforme lo requiera el contribuyente.
Los RIDE que se descarguen del portal web del SRI contendrán hora y fecha de autorización, dicha información no
es obligatoria registrarla en el RIDE generado por los emisores de comprobantes electrónicos.
El número de la clave de acceso corresponde al número de autorización.
Conforme consta en el numeral 9 .20, el código de barras es opcional.
El campo “Subtotal tarifa especial” corresponde a la tarifa de IVA por actividades de turismo.
Los contribuyentes podrán visualizar solo los subtotales que fueron llenos.
NOTA DE CRÉDITO

NOTA DE DÉBITO

COMPROBANTE DE RETENCIÓN

GUÍA DE REMISIÓN

LIQUIDACIÓN DE COMPRA DE BIENES Y PRESTACIÓN DE
SERVICIOS

ANEXO 3 - FORMATOS XML VERSIÓN 1.1.0
Incluyen el aumento de 2 a 6 decimales en los campos de cantidad y precio unitario
para quienes lo requieran. En el caso del formato de factura adicionalmente
contiene información de retenciones de IVA presuntivo e Impuesto a la Renta que
aplica para comercializadores de derivados de petróleo y retención presuntiva de
IVA a los editores, distribuidores y voceadores que participan en la
comercialización de periódicos y/o revistas.

FORMATO XML FACTURA

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR Obligatorio^ Alfanumérico^ Max 300^

EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1768153530001 Obligatorio Numérico 13

0403201301176815353000110015010000000081234567816

Obligatorio,
conforme
tabla 1
Numérico 49
01

Obligatorio,
conforme
tabla 3
Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
Alpallana Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
04/03/2013 Obligatorio Fecha dd/mm/aaaa

Alpallana </ dirEstablecimiento >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
SI </ obligadoContabilidad >

Obligatorio
cuando
corresponda
Texto SI / NO
04 </ tipoIdentificacionComprador >

Obligatorio,
conforme
tabla 6
Numérico 2
001 - 001 - 000000001

Obligatorio
cuando
corresponda
Numérico 15
PRUEBAS SERVICIO DERENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1760013210001 </ identificacionComprador > Obligatorio Alfanumérico Max 20

salinas y santiago

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
64.94 Obligatorio Numérico Max 14
5.00 Obligatorio Numérico Max 14
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
5.00

Opcional,
aplica para
código
impuesto 2.
Numérico Max 14
68.19 </ baseImponible > Obligatorio Numérico Max 14
7.58 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
64.94 </ baseImponible > Obligatorio Numérico Max 14
3.25 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
73.09 </ importeTotal> Obligatorio Numérico Max 14

DOLAR

Obligatorio
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -

21

Obligatorio,
conforme
tabla 24
Numérico 2
73,09 Obligatorio Numérico Max 14

60

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio^^
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
125BJC- 01 Obligatorio Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
DERIVADOS PETRÓLEO Obligatorio Alfanumérico Max 300

2.542563 Obligatorio Numérico

Max 18,
hasta 6
decimales
25.542365 Obligatorio Numérico

Max 18,
hasta 6
decimales
0.00 Obligatorio Numérico Max 14
64.94 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14

Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio cuando Alfanumérico Max 300

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
corresponda

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
12 </ tarifa> Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
68.19 Obligatorio Numérico Max 14
8.18 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
5 </ tarifa> Obligatorio Numérico Min 1 Max 4
64.94 Obligatorio Numérico Max 14
3.25 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
- -
4

Obligatorio
cuando
corresponda ,
conforme
tabla 22
Numérico 1
327

Obligatorio
cuando
corresponda ,
conforme
tabla 23
Numérico Min 1 Max 3
0.20

Obligatorio
cuando
corresponda
Numérico
Min 1 Max 5 /
3 enteros,
dos
decimales
0.13

Obligatorio
cuando
corresponda
Numérico
Max 14 /12
enteros, 2
decimales
Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
- -
4

Obligatorio
cuando
corresponda ,
conforme
tabla 22
Numérico 1
328

Obligatorio
cuando
corresponda ,
conforme
tabla 23
Numérico Min 1 Max 3
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
0.30

Obligatorio
cuando
corresponda
Numérico
Min 1 Max 5 /
3 enteros,
dos
decimales
0.19

Obligatorio
cuando
corresponda
Numérico
Max 14 /12
enteros, 2
decimales
Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
- -
4

Obligatorio
cuando
corresponda ,
conforme
tabla 22
Numérico 1
3

Obligatorio
cuando
corresponda ,
conforme
tabla 23
Numérico Min 1 Max 3
1

Obligatorio
cuando
corresponda
Numérico
Min 1 Max 5 /
3 enteros,
dos
decimales
2.00

Obligatorio
cuando
corresponda
Numérico
Max 14 /12
enteros, 2
decimales
Obligatorio
cuando
corresponda
- -
Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
- -
4580

Obligatorio
cuando
corresponda
Alfanumérico Max 300
15.42x

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

FORMATO XML GUÍA DE REMISIÓN

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR Obligatorio^ Alfanumérico^ Max 300^
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13

0603201306176001321000110015010000000081234567812

Obligatorio,
conforme
tabla 1
Numérico 49
06

Obligatorio,
conforme
tabla 3
Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
ALPALLANA Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

ALPALLANA </ dirEstablecimiento >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Av. Eloy Alfaro 34 y Av. Libertad Esq. Obligatorio Alfanumérico Max 300
Transportes S.A. Obligatorio Alfanumérico Max 300

04

Obligatorio,
conforme
tabla 6
Numérico 2
1796875790001 Obligatorio Alfanumérico Max 13

Contribuyente Regimen Simplificado RISE

Obligatorio
cuando
corresponda
Alfanumérico Max 40
SI </ obligadoContabilidad >

Obligatorio
cuando
corresponda
Texto SI / NO
5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
06/03/2013 Obligatorio Fecha dd/mm/aaaa
06/03/2013 Obligatorio Fecha dd/mm/aaaa
MCL0827 Obligatorio Alfanumérico Max 20
Obligatorio - -
Obligatorio - -
Obligatorio - -
1716849140001 Obligatorio Alfanumérico Max 20
Alvarez Mina John Henry Obligatorio Alfanumérico Max 300
Av. Simón Bolívar S/N Intercambiador Obligatorio Alfanumérico Max 300
Venta de Maquinaria de Impresión Obligatorio Alfanumérico Max 300

0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio
cuando
corresponda
Numérico 3
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Quito – Cayambe - Otavalo

Obligatorio
cuando
corresponda
Alfanumérico Max 300
01

Obligatorio
cuando
corresponda,
conforme tabla
3
Numérico 2
002 - 001 - 000000001

Obligatorio
cuando
corresponda
Numérico 15
211020111630251792146739011234567891

Obligatorio
cuando
corresponda
Numérico 10 o 37 o 49
21/10/2011

Obligatorio
cuando
corresponda
Fecha dd/mm/aaaa
Obligatorio - -
Obligatorio - -
125BJC- 01 </ codigoInterno > Opcional1 Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
DIESEL Obligatorio Alfanumérico Max 300

10.254632 Obligatorio Numérico

Max 18,
hasta 6
decimales

Obligatorio
cuando
corresponda
- -

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio
cuando
corresponda
- -
098568541

Obligatorio
cuando
corresponda
Alfanumérico Max 300
info@organizacion.com

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Guayaquil–12 de octubre y
Universo

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

FORMATO XML NOTA DE CRÉDITO

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1

Obligatorio,
conforme
tabla 4
Numérico 1
1 </ tipoEmision>

Obligatorio,
conforme
tabla 2
Numérico 1
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR Obligatorio^ Alfanumérico^ Max 300^
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR </ nombreComercial >

Obligatorio
cuando
corresponda
Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13

0603201304176001321000110015010000000461234567817

Obligatorio,
conforme
tabla 1
Numérico 49
04

Obligatorio,
conforme
tabla 3
Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000046 Obligatorio Numérico 9
ALPALLANA Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
06/03/2013 Obligatorio Fecha dd/mm/aaaa

ALPALLANA </ dirEstablecimiento>

Obligatorio
cuando
corresponda
Alfanumérico Max 300
04 </ tipoIdentificacionComprador >

Obligatorio,
conforme
tabla 6
Numérico 2
PRUEBAS SERVICIO DE RENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1792107865001 Obligatorio Alfanumérico Max 20

5368

Obligatorio
cuando
corresponda
Alfanumérico Min 3 Max 13
SI </ obligadoContabilidad>

Obligatorio
cuando
corresponda
Texto SI / NO
Contribuyente Régimen Simplificado RISE

Obligatorio
cuando
corresponda
Alfanumérico Max 40
01

Obligatorio,
conforme
tabla 3
Numérico 2
002 - 001 - 000000001 Opcional Numérico 15
03/03/2013 Obligatorio Fecha dd/mm/aaaa
38327.96 Obligatorio Numérico Max 14
45073.68 Obligatorio Numérico Max 14

DOLAR

Obligatorio
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
38327.96 </ baseImponible > Obligatorio Numérico Max 14

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
1916.40 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2 </ codigoPorcentaje>

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
40244.36 </ baseImponible > Obligatorio Numérico Max 14
4829.32 Obligatorio Numérico Max 14
Obligatorio - -

Obligatorio - -
DEVOLUCIÓN Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
Obligatorio - -
125BJC- 01 Opcional Alfanumérico Max 25

1234D56789-A

Obligatorio
cuando
corresponda
Alfanumérico Max 25
ABCD Obligatorio Alfanumérico Max 300
1500.564125 Obligatorio Numérico Max 18, hasta 6 decimales

25.542365 Obligatorio Numérico Max 18, hasta 6 decimales

0.00

Obligatorio
cuando
corresponda
Numérico Max 14
38327.96 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14


Obligatorio
cuando
corresponda

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
Obligatorio - -
Obligatorio - -

3

Obligatorio,
conforme
tabla 16
Numérico 1
3072

Obligatorio,
conforme
tabla 18
Numérico Min 1 Max 4
5 </ tarifa>

Obligatorio
cuando
corresponda
Numérico Min 1 Max 3
38327.96 Obligatorio Numérico Max 14
1916.40 Obligatorio Numérico Max 14

Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme
tabla 16
Numérico 1
2

Obligatorio,
conforme
tabla 17
Numérico Min 1 Max 4
12 </ tarifa> Obligatorio cuando Numérico Min 1 Max 4

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
corresponda (^) / 2 enteros, 2
decimales
40244.36 Obligatorio Numérico Max 14
4829.32 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

Obligatorio
cuando
corresponda

- -
info@organizacion.com

Obligatorio
cuando
corresponda
Alfanumérico Max 300
Obligatorio
cuando
corresponda
- -
Obligatorio - -

ANEXO 4 - FORMATOS XML FACTURA EXPORTACIÓN APLICADOS A LAS VERSIONES 1.0.0 y 1.1.0
Incluyen los campos requeridos para exportación, adicionalmente en el diseño del
Ride se podrá incluir e imprimir datos adicionales conforme lo requiera el
contribuyente. Los campos nuevos contenidos en los siguientes formatos deberán
ser utilizados únicamente en exportaciones, caso contrario se deberá utilizar los
formatos de factura establecidos en el Anexo 1 y Anexo 3 según corresponda.
FACTURA VERSIÓN 1.0.0

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

Obligatorio - - Obligatorio - - Obligatorio - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 </ tipoEmision> (^) conforme tabla 2Obligatorio, Numérico 1
CONTRIBUYENTE PRUEBA Obligatorio Alfanumérico Max 300
PRUEBA UNO
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
1792261104001 Obligatorio Numérico 13
0403201301179226110400110015010000000081234567816</claveAcc
eso>
Obligatorio,
conforme tabla 1 Numérico^49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
Alpallana Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
04/03/2013 Obligatorio Fecha dd/mm/aaaa
Alpallana
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
5368
Obligatorio,
cuando
corresponda
Alfanumérico Min 3 Max 13
SI</ obligadoContabilidad >
Obligatorio,
cuando
corresponda
Texto SI/NO
EXPORTADOR Obligatorio
Texto,
Mayúsculas,
siempre es
EXPORTADOR

10
CIF Obligatorio (^) MayúsculasTexto, Max 10
GUAYAQUIL Obligatorio Alfanumérico Max 300
593 (^) conforme tabla 25Obligatorio, Numérico 3
GUAYAQUIL Obligatorio Alfanumérico Max 300
CHINA Obligatorio Alfanumérico Max 300
593 (^) conforme tabla 25Opcional, Numérico 3
593 (^) conforme tabla 25Opcional, Numérico 3
04 Obligatorio, Numérico 2

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
conforme tabla 6
001- 001 - 000000001

Obligatorio,
cuando
corresponda
Numérico 15
PRUEBAS SERVICIO DE RENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1760013210001 Obligatorio Numérico Max 20

salinas y santiago

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
295000.00 Obligatorio Numérico Max 14

FOB Obligatorio (^) MayúsculasTexto, Max 10
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2 (^) conforme tabla 16Obligatorio, Numérico 1
0 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
0.00
Opcional, aplica
para código
impuesto 2.
Numérico Max 14
295000.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
1000.00
Obligatorio,
cuando
corresponda
Numérico Max 14
200.00
Obligatorio,
cuando
corresponda
Numérico Max 14
800.00
Obligatorio,
cuando
corresponda
Numérico Max 14
350.00
Obligatorio,
cuando
corresponda
Numérico Max 14
297350.00 Obligatorio Numérico Max 14
DOLAR
Obligatorio,
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -
15 (^) conforme tabla 24Obligatorio, Numérico 2
200000 Obligatorio Numérico Max 14
30
Obligatorio,
cuando
corresponda
Numérico Max 14
dias
Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
18 (^) conforme tabla 24Obligatorio, Numérico 2
97350 Obligatorio Numérico Max 14
15
Obligatorio,
cuando
corresponda
Numérico Max 14
dias
Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -
Obligatorio - -
Obligatorio - -
003 Obligatorio Alfanumérico Max 25

SER003

Obligatorio,
cuando
corresponda
Alfanumérico Max 25
FROZEN MOONFISH WR Obligatorio Alfanumérico Max 300

Kilos

Obligatorio,
cuando
corresponda
Alfanumérico Max 50
100.00 Obligatorio Numérico Max 14
2950.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
295000.00 Obligatorio Numérico Max 14


Obligatorio,
cuando
corresponda
- -
<detAdicional valor="KILOS"nombre="PESO NETO"/>

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
<detAdicional valor="KILOS"nombre="PESO BRUTO"/>

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
<detAdicional valor="KILOS"nombre="PARTIDA ARANCELARIA"/>

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio,
cuando
corresponda
- -
Obligatorio - -
Obligatorio - -

2 (^) conforme tabla 16Obligatorio, Numérico 1
0 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
0</ tarifa> Obligatorio Numérico
Min 1 Max 4
/ 2 enteros, 2
decimales
295000.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio cuando corresponda - -
CAJAS DE 10
KILOS
Obligatorio cuando
corresponda Alfanumérico^ Max 300^
NUMERO DE CUENTA DE
BANCO 1243546
Obligatorio cuando
corresponda Alfanumérico^ Max 300^
Obligatorio cuando corresponda -
Obligatorio -

FACTURA VERSIÓN 1.1.0

En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - -
Obligatorio - -
Obligatorio - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 </ tipoEmision> (^) conforme tabla 2Obligatorio, Numérico 1
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR Obligatorio^ Alfanumérico^ Max 300^
EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR EP
PETROECUADOR </ nombreComercial >
Obligatorio cuando
corresponda Alfanumérico^ Max 300^
1768153530001 Obligatorio Numérico 13
0403201301176815353000110015010000000081234567816 </claveAcce
so>
Obligatorio,
conforme tabla 1 Numérico^49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
Alpallana Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
04/03/2013 Obligatorio Fecha dd/mm/aaaa
Alpallana </ dirEstablecimiento > Obligatorio correspondacuando Alfanumérico Max 300
5368 Obligatorio cuando corresponda Alfanumérico Min 3 Max 13
SI </ obligadoContabilidad > Obligatorio cuando corresponda Texto SI / NO
EXPORTADOR Obligatorio
Texto,
Mayúsculas,
siempre es
EXPORTADOR

10
FOB Obligatorio (^) MayúsculasTexto, Max 10
GUAYAQUIL Obligatorio Alfanumérico Max 300
593 (^) conforme tabla 25Obligatorio, Numérico 3
GUAYAQUIL Obligatorio Alfanumérico Max 300
CHINA Obligatorio Alfanumérico Max 300
593 Opcional, conforme tabla 25 Numérico 3
593 Opcional, conforme tabla 25 Numérico 3
04 </ tipoIdentificacionComprador > (^) conforme tabla 6Obligatorio, Numérico 2
001 - 001 - 000000001 Obligatorio cuando corresponda Numérico 15
PRUEBAS SERVICIO DE RENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1760013210001 </ identificacionComprador > Obligatorio Alfanumérico Max 20
salinas y santiago Obligatorio Alfanumérico Max 300
64.94 Obligatorio Numérico Max 14
FOB Obligatorio (^) MayúsculasTexto, Max 10
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2 (^) conforme tabla 16Obligatorio, Numérico 1
0 </ codigoPorcentaje> (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
0.00
Opcional, aplica
para código
impuesto 2.
Numérico Max 14
64.94 </ baseImponible > Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
0.00 Obligatorio, cuando corresponda Numérico Max 14

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
0.00 Obligatorio, cuando corresponda Numérico Max 14

0.00 Obligatorio, cuando corresponda Numérico Max 14
0.00 Obligatorio, cuando corresponda Numérico Max 14
65.07 </ importeTotal> Obligatorio Numérico Max 14
DOLAR Obligatorio cuando corresponda Alfanumérico Max 15
Obligatorio -
Obligatorio -

16 (^) conforme tabla 24Obligatorio, Numérico 2
30.00 Obligatorio Numérico Max 14
90 Obligatorio, cuando corresponda Numérico Max 14
dias Obligatorio, cuando corresponda Texto Max 10
Obligatorio - -
Obligatorio - -
19 (^) conforme tabla 24Obligatorio, Numérico 2
34.94 Obligatorio Numérico Max 14
90 Obligatorio, cuando corresponda Numérico Max 14
dias Obligatorio, cuando corresponda Texto Max 10
Obligatorio -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -

Obligatorio - -
Obligatorio - -
003 Obligatorio Alfanumérico Max 25
001 Obligatorio cuando corresponda Alfanumérico Max 25
FROZEN MOONFISH WR Obligatorio Alfanumérico Max 300
2.542563 Obligatorio Numérico

Max 18,
hasta 6
decimales
25.542365 Obligatorio Numérico

Max 18,
hasta 6
decimales
0.00 Obligatorio Numérico Max 14
64.94 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14
Obligatorio cuando corresponda - -

Obligatorio cuando corresponda Alfanumérico Max 300
Obligatorio cuando corresponda Alfanumérico Max 300

Obligatorio cuando corresponda Alfanumérico Max 300
Obligatorio cuando corresponda - -
Obligatorio - -
Obligatorio - -

2 (^) conforme tabla 16Obligatorio, Numérico 1
0 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
0 </ tarifa> Obligatorio Numérico
Min 1 Max 4
/ 2 enteros, 2
decimales
64.94 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

Obligatorio cuando
corresponda. Aplica
para
comercializadores de
Derivados de
Petróleo y Retención
presuntiva de IVA a

- -
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
los Editores,
Distribuidores y
Voceadores que
participan en la
comercialización de
periódicos y/o
revistas.
Obligatorio cuando corresponda - -
4

Obligatorio cuando
corresponda
conforme tabla 22
Numérico 1
327

Obligatorio cuando
corresponda
conforme tabla 23
Numérico Min 1 Max 3
0.20 Obligatorio cuando corresponda Numérico

Min 1 Max 5 /
3 enteros,
dos
decimales
0.13 Obligatorio cuando corresponda Numérico

Max 14 /12
enteros, 2
decimales
Obligatorio cuando corresponda - -

Obligatorio cuando corresponda - -
Obligatorio cuando corresponda - -
CAJAS DE 10 KILOS

Obligatorio cuando
corresponda Alfanumérico^ Max 300^
<campoAdicionalnombre=" INFORMACION BANCARIA "> NUMERO DE CUENTA DE
BANCO 1243546

Obligatorio cuando
corresponda Alfanumérico^ Max 300^
Obligatorio cuando corresponda - -
Obligatorio - -

TABLA 24: FORMAS DE PAGO
FORMAS DE PAGO CÓDIGO FECHA INICIO FECHA FIN
SIN UTILIZACION DEL SISTEMA FINANCIERO 01 01/01/2013 -
COMPENSACIÓN DE DEUDAS 15 01/01/2013 -
TARJETA DE DÉBITO 16 01/06/2016 -
DINERO ELECTRÓNICO 17 01/06/2016 -
TARJETA PREPAGO 18 01/06/2016 -
TARJETA DE CRÉDITO 19 01/06/2016 -
OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO 20 01/06/2016 -
ENDOSO DE TÍTULOS 21 01/06/2016 -
Las formas de pago señaladas corresponden al Catálogo del Anexo Transaccional
Simplificado, publicado en la página web http://www.sri.gob.ec: Información sobre
impuestos/Cómo declaro mis impuestos? / Anexos y guías.
TABLA 25: PAÍSES
CÓDIGO DESCRIPCIÓN CÓDIGO DESCRIPCIÓN
016 AMERICAN SAMOA 334 QATAR
074 BOUVET ISLAND 335 MALDIVAS
101 ARGENTINA 336 NEPAL
102 BOLIVIA 337 OMAN
103 BRASIL 338 SINGAPUR
104 CANADÁ 339 SRI LANKA (CEILAN)
CÓDIGO DESCRIPCIÓN CÓDIGO DESCRIPCIÓN
105 COLOMBIA 341 VIETNAM
106 COSTA RICA 342 YEMEN
107 CUBA 343 ISLAS HEARD Y MCDONALD
108 CHILE 344 BRUNEI DARUSSALAM
109 ANGUILA 346 TURQUÍA
110 ESTADOS UNIDOS 347 AZERBAIJÁN
111 GUATEMALA 348 KAZAJSTÁN
112 HAITÍ 349 KIRGUIZISTÁN
113 HONDURAS 350 TAJIKISTAN
114 JAMAICA 351 TURKMENISTÁN
115 MALVINAS ISLAS 352 UZBEKISTÁN
116 MÉXICO 353 PALESTINA
117 NICARAGUA 354 HONG KONG
118 PANAMÁ 355 MACAO
119 PARAGUAY 356 ARMENIA
120 PERÚ 382 MONTENEGRO
121 PUERTO RICO 402 BURKINA FASO
122 REPÚBLICA DOMINICANA 403 ARGELIA
123 EL SALVADOR 404 BURUNDÍ
124 TRINIDAD Y TOBAGO 405 CAMERÚN
125 URUGUAY 406 CONGO
126 VENEZUELA 407 ETIOPÍA
127 CURAZAO 408 GAMBIA
129 BAHAMAS 409 GUINEA
130 BARBADOS 410 LIBERIA
131 GRANADA 412 MADAGASCAR
132 GUYANA 413 MALAWI
133 SURINAM 414 MALÍ
134 ANTIGUA Y BARBUDA 415 MARRUECOS
135 BELICE 416 MAURITANIA
136 DOMINICA 417 NIGERIA
137 SAN CRISTOBAL Y NEVIS 419 ZIMBABWE (RHODESIA)
138 SANTA LUCÍA 420 SENEGAL
139 SAN VICENTE Y LAS GRANAD. 421 SUDÁN
140 ANTILLAS HOLANDESAS 422 SUDAFRICA (CISKEI)
141 ARUBA 423 SIERRA LEONA
142 BERMUDA 425 TANZANIA
143 GUADALUPE 426 UGANDA
144 GUYANA FRANCESA 427 ZAMBIA
145 ISLAS CAIMÁN 428 ÅLAND ISLANDS
146 ISLAS VIRGENES (BRITANICAS) 429 BENIN
147 JOHNSTON ISLA 430 BOTSWANA
148 MARTINICA 431 REPUBLICA CENTROAFRICANA
149 MONTSERRAT ISLA 432 COSTA DE MARFIL
151 TURCAS Y CAICOS ISLAS 433 CHAD
152 VIRGENES, ISLAS (NORT.AMER.) 434 EGIPTO
201 ALBANIA 435 GABON
202 ALEMANIA 436 GHANA
203 AUSTRIA 437 GUINEA-BISSAU
204 BÉLGICA 438 GUINEA ECUATORIAL
205 BULGARIA 439 KENIA
207 ALBORAN Y PEREJIL 440 LESOTHO
208 DINAMARCA 441 MAURICIO
209 ESPAÑA 442 MOZAMBIQUE
211 FRANCIA 443 MAYOTTE
212 FINLANDIA 444 NIGER
213 REINO UNIDO 445 RWANDA
214 GRECIA 446 SEYCHELLES
CÓDIGO DESCRIPCIÓN CÓDIGO DESCRIPCIÓN
215 PAISES BAJOS (HOLANDA) 447 SAHARA OCCIDENTAL
216 HUNGRÍA 448 SOMALIA
217 IRLANDA 449 SANTO TOME Y PRINCIPE
218 ISLANDIA 450 SWAZILANDIA
219 ITALIA 451 TOGO
220 LUXEMBURGO 452 TUNEZ
221 MALTA 453 ZAIRE
222 NORUEGA 454 ANGOLA
223 POLONIA 456 CABO VERDE
224 PORTUGAL 458 COMORAS
225 RUMANIA 459 DJIBOUTI
226 SUECIA 460 NAMIBIA
227 SUIZA 463 ERITREA
228 CANARIAS ISLAS 464 MOROCCO
229 UCRANIA 465 REUNION
230 RUSIA 466 SANTA ELENA
231 YUGOSLAVIA 499 JERSEY
233 ANDORRA 501 AUSTRALIA
234 LIECHTENSTEIN 503 NUEVA ZELANDA
235 MÓNACO 504 SAMOA OCCIDENTAL
237 SAN MARINO 506 FIJI
238 VATICANO (SANTA SEDE) 507 PAPUA NUEVA GUINEA
239 GIBRALTAR 508 TONGA
241 BELARUS 509 PALAO (BELAU) ISLAS
242 BOSNIA Y HERZEGOVINA 510 KIRIBATI
243 CROACIA 511 MARSHALL ISLAS
244 ESLOVENIA 512 MICRONESIA
245 ESTONIA 513 NAURU
246 GEORGIA 514 SALOMON ISLAS
247 GROENLANDIA 515 TUVALU
248 LETONIA 516 VANUATU
249 LITUANIA 517 GUAM
250 MOLDOVA 518 ISLAS COCOS (KEELING)
251 MACEDONIA 519 ISLAS COOK
252 ESLOVAQUIA 520 ISLAS NAVIDAD
253 ISLAS FAROE 521 MIDWAY ISLAS
260 FRENCH SOUTHERN TERRITORIES 522 NIUE ISLA
301 AFGANISTAN 523 NORFOLK ISLA
302 ARABIA SAUDITA 524 NUEVA CALEDONIA
303 MYANMAR (BURMA) 525 PITCAIRN, ISLA
304 CAMBOYA 526 POLINESIA FRANCESA
306 COREA NORTE 529 TIMOR DEL ESTE
307 TAIWAN (CHINA) 530 TOKELAI
308 FILIPINAS 531 WAKE ISLA
309 INDIA 532 WALLIS Y FUTUNA, ISLAS
310 INDONESIA 590 SAINT BARTHELEMY
311 IRAK 593 ECUADOR
312 IRÁN (REPÚBLICA ISLÁMICA) 594 AGUAS INTERNACIONALES
313 ISRAEL 595 ALTO VOLTA
314 JAPÓN 596 BIELORRUSIA
315 JORDANIA 597 COTE DÍVOIRE
316 KUWAIT 598 CYPRUS
317 LAOS, REP. POP. DEMOC. 599 REPÚBLICA CHECA
318 LIBANO 600 FALKLAND ISLANDS
319 MALASIA 601 LATVIA
321 MONGOLIA (MANCHURIA) 602 LIBIA
322 PAKISTÁN 603 NORTHERN MARIANA ISL
323 SIRIA 604 ST. PIERRE AND MIQUE
CÓDIGO DESCRIPCIÓN CÓDIGO DESCRIPCIÓN
325 TAILANDIA 605 SYRIAN ARAB REPUBLIC
327 BAHREIN 606 TERRITORIO ANTÁRTICO BRITÁNICO
328 BANGLADESH 607 TERRITORIO BRITÁNICO OCÉANO IN
329 BUTÁN 688 SERBIA
330 COREA DEL SUR 831 GUERNSEY
331 CHINA POPULAR 832 JERSEY
332 CHIPRE 833 ISLE OF MAN
333 EMIRATOS ARABES UNIDOS
Los códigos establecidos para países corresponden al Catálogo de Anexo
Transaccional Simplificado, publicado en la página web http://www.sri.gob.ec:
Información sobre impuestos / Cómo declaro mis impuestos? / Anexos y guías.
ANEXO 5 - FORMATOS XML FACTURA REEMBOLSO APLICADO EN LAS VERSIONES 1.0.0 y 1.1.0
Incluyen los campos requeridos exclusivamente para rreembolso, caso contrario se
deberá utilizar los formatos de factura establecidos en el anexo 1 y anexo 3 según
corresponda.
FACTURA VERSIÓN 1.0.0

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

Obligatorio - - Obligatorio - - Obligatorio - - 1 Obligatorio, conforme tabla 4 Numérico 1 1 Obligatorio, conforme tabla 2 Numérico 1 CONTRIBUYENTE PRUEBA Obligatorio Alfanumérico Max 300 PRUEBA UNO Obligatorio, cuando corresponda Alfanumérico Max 300 1792261104001 Obligatorio Numérico 13 0403201301179226110400110015010000000081234567816
Obligatorio, conforme
tabla 1 Numérico^49
01 Obligatorio, conforme tabla 3 Numérico 2
001 Obligatorio Numérico 3
501 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
Alpallana Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
04/03/2013 Obligatorio Fecha dd/mm/aaaa
Alpallana Obligatorio, cuando corresponda Alfanumérico Max 300
5368 Obligatorio, cuando corresponda Alfanumérico Min 3 Max 13

SI</ obligadoContabilidad > Obligatorio, cuando corresponda Texto SI/NO
04 Obligatorio, conforme tabla 6 Numérico 2

001- 001 - 000000001 Obligatorio, cuando corresponda Numérico 15
PRUEBAS SERVICIO DE RENTAS
INTERNAS Obligatorio^ Alfanumérico^ Max 300^
1760013210001 Obligatorio Numérico Max 20
salinas y santiago Obligatorio, cuando corresponda Alfanumérico Max 300

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
150.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14

41

Obligatorio cuando
corresponda a
Reembolso (41)
Numérico 2
150.00

Obligatorio cuando
<codDocReemb>> sea
igual a 41, sumatoria
de
<totalBaseImponibleRe
embolso> y
<totalImpuestoReembol
so>.
Numérico Max 14
133.93

Obligatorio cuando
<codDocReemb> sea
igual a 41, en base a la
información
<reembolsos>,
sumatoria de
<baseImponibleReemb
olso>.
Numérico Max 14
16.07

Obligatorio cuando
<codDocReemb>> sea
igual a 41, en base a la
información
<reembolsos>
sumatoria de
<impuestoReembolso>.
Numérico Max 14
Obligatorio -^ -^
Obligatorio - -
2 Obligatorio, conforme tabla 16 Numérico 1

6 Obligatorio, conforme tabla 17 Numérico Min 1 Max 4
150.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
150.00 Obligatorio Numérico Max 14
DOLAR Obligatorio, cuando corresponda Alfanumérico Max 15
Obligatorio - -
Obligatorio - -
01 Obligatorio, conforme tabla 24 Numérico 2
150 Obligatorio Numérico Max 14
0 Obligatorio, cuando corresponda Numérico Max 14

dias Obligatorio, cuando corresponda Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
003 Obligatorio Alfanumérico Max 25
001 Obligatorio, cuando corresponda Alfanumérico Max 25
REEMBOLSO DE GASTOS Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Max 14
150.00 Obligatorio Numérico Max 14
0 Obligatorio Numérico Max 14
150.00 Obligatorio Numérico Max 14
Obligatorio, cuando corresponda - -

Obligatorio, cuando corresponda Alfanumérico Max 300
Obligatorio, cuando corresponda - -
Obligatorio - -
Obligatorio - -
2 Obligatorio, conforme tabla 16 Numérico 1

6 Obligatorio, conforme tabla 17 Numérico Min 1 Max 4

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
0.00</ tarifa> Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
150.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio cuando
<codDocReemb> sea
igual a 41
- -

Obligatorio cuando
<codDocReemb> sea
igual a 41
- -
04</tipoIdentificacionProveedorReembol
so>

Obligatorio cuando
<codDocReemb>sea
igual a 41, conforme
tabla 6
Numérico 2
1760013210001</identificacionProveedorRee
mbolso>

Obligatorio cuando
<codDocReemb> sea
igual a 41
Alfanumérico Max 13
593

Obligatorio cuando
<codDocReemb> sea
igual a 41, conforme
tabla 25
Numérico 3
01

Obligatorio cuando
<codDocReemb> sea
igual a 41, conforme
tabla 26
Numérico 2
01

Obligatorio cuando
<codDocReemb> sea
igual a 41, conforme
documentos de
reembolso del catálogo
del ATS
Numérico Min 2 Max 3
001

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico 3
501

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico 3
000000008

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico 9
04/03/2013

Obligatorio cuando
<codDocReemb> sea
igual a 41
Fecha dd/mm/aaaa
040320130117922611040011001501000000008123
4567816

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico 10, 37 o 49

Obligatorio cuando
<codDocReemb> sea
igual a 41
- -

Obligatorio cuando
<codDocReemb> sea
igual a 41
- -
2

Obligatorio cuando
<codDocReemb> sea
igual a 41, conforme
tabla 16
Numérico 1
2

Obligatorio cuando
<codDocReemb> sea
igual a 41, conforme
tabla 17
Numérico Min 1 Max 4
12

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico
Min 1 Max 4 / 2
enteros, 2
decimales
133.93

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico Max 14
16.07

Obligatorio cuando
<codDocReemb> sea
igual a 41
Numérico Max 14

Obligatorio cuando
<codDocReemb> sea
igual a 41
- -

Obligatorio cuando
<codDocReemb> sea
igual a 41
- -
(^) sea Obligatorio cuando -^ -^

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
igual a 41

Obligatorio cuando
<codDocReemb>sea
igual a 41
- -
Obligatorio cuando corresponda - -

4580 Obligatorio cuando corresponda Alfanumérico Max 300

Obligatorio cuando corresponda -^ -^
Obligatorio - -

FACTURA VERSIÓN 1.1.0

En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
<?xml version="1.0" encoding="UTF-8" ?> Obligatorio - -
<factura id="comprobante" version="1.1.0"> Obligatorio - -
<infoTributaria> Obligatorio - -
<ambiente> 1 </ambiente> Obligatorio, conforme tabla 4 Numérico 1
<tipoEmision> 1 </ tipoEmision> Obligatorio, conforme tabla 2 Numérico 1
<razonSocial> EMPRESA PUBLICA DE HIDROCARBUROS DEL ECUADOR
EP PETROECUADOR </razonSocial> Obligatorio^ Alfanumérico^ Max 300^
<nombreComercial> EMPRESA PUBLICA DE HIDROCARBUROS DEL
ECUADOR EP PETROECUADOR </ nombreComercial >
Obligatorio cuando
corresponda Alfanumérico^ Max 300^
<ruc> 1768153530001 </ruc> Obligatorio Numérico 13
<claveAcceso> 0403201301179226110400110015010000000081234567816 </cl
aveAcceso>
Obligatorio, conforme
tabla 1 Numérico^49
<codDoc> 01 </codDoc> Obligatorio, conforme tabla 3 Numérico 2
<estab> 001 </estab> Obligatorio Numérico 3
<ptoEmi> 501 </ptoEmi> Obligatorio Numérico 3
<secuencial> 000000008 </secuencial> Obligatorio Numérico 9
<dirMatriz> Alpallana </dirMatriz>
Obligatorio^ Alfanumérico^ Max 300^
</infoTributaria> Obligatorio - -
<infoFactura> Obligatorio - -
<fechaEmision> 04/03/2013 </fechaEmision> Obligatorio Fecha dd/mm/aaaa
<dirEstablecimiento> Alpallana </ dirEstablecimiento > Obligatorio correspondacuando Alfanumérico Max 300
<contribuyenteEspecial> 5368 </contribuyenteEspecial> Obligatorio cuando corresponda Alfanumérico Min 3 Max 13
<obligadoContabilidad> SI </ obligadoContabilidad > Obligatorio cuando corresponda Texto SI / NO
<tipoIdentificacionComprador> 04 </ tipoIdentificacionComprador > Obligatorio, conforme tabla 6 Numérico 2
<guiaRemision> 001 - 001 - 000000001 </guiaRemision> Obligatorio cuando corresponda Numérico 15
<razonSocialComprador> PRUEBAS SERVICIO DE RENTAS
INTERNAS </razonSocialComprador> Obligatorio^ Alfanumérico^ Max 300^
<identificacionComprador> 1760013210001 </ identificacionComprador > Obligatorio Alfanumérico Max 20
<direccionComprador>salinas y santiago</direccionComprador> Obligatorio cuando corresponda Alfanumérico Max 300
<totalSinImpuestos> 150.00 </totalSinImpuestos> Obligatorio Numérico Max 14
<totalDescuento> 0.00 </totalDescuento> Obligatorio Numérico Max 14
<codDocReembolso>41</codDocReembolso>
Obligatorio cuando
corresponda a Reembolso
(41)
Numérico 2
<totalComprobantesReembolso>150.00</totalComprobantesReembolso>
Obligatorio cuando
<codDocReemb>> sea
igual a 41, sumatoria de
<totalBaseImponibleReemb
olso> y
Numérico Max 14
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
<totalImpuestoReembolso>.
133.93

Obligatorio cuando
<codDocReemb> sea igual
a 41, en base a la
información <reembolsos>,
sumatoria de
<baseImponibleReembolso
>.
Numérico Max 14
16.07

Obligatorio cuando
<codDocReemb>> sea
igual a 41, en base a la
información <reembolsos>
sumatoria de
<impuestoReembolso>.
Numérico Max 14
Obligatorio - -
Obligatorio - -
2 Obligatorio, conforme tabla 16 Numérico 1

6 </ codigoPorcentaje> Obligatorio, conforme tabla 17 Numérico Min 1 M ax 4
0.00 Opcional, aplica para código impuesto 2. Numérico Max 14
150.00 </ baseImponible > Obligatorio Numérico Max 14

0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
150.13 </ importeTotal> Obligatorio Numérico Max 14
DOLAR Obligatorio cuando corresponda Alfanumérico Max 15
Obligatorio -
Obligatorio -

01

Obligatorio, cuando
corresponda conforme
tabla 24
Numérico 2
150.13 Obligatorio Numérico Max 14
0 Obligatorio, cuando corresponda Numérico Max 14

dias Obligatorio, cuando corresponda Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
003 Obligatorio Alfanumérico Max 25
001 Obligatorio cuando corresponda Alfanumérico Max 25
Reembolso de Gastos Obligatorio Alfanumérico Max 300

1.000000 Obligatorio Numérico

Max 18,
hasta 6
decimales
150.000000 Obligatorio Numérico

Max 18,
hasta 6
decimales
0.00 Obligatorio Numérico Max 14
150.00 </ precioTotalSinImpuesto> Obligatorio Numérico Max 14
Obligatorio cuando corresponda - -

Obligatorio cuando
corresponda Alfanumérico^ Max 300^
Obligatorio cuando corresponda - -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -
2 Obligatorio, conforme tabla 16 Numérico 1

6 Obligatorio, conforme tabla 17 Numérico Min 1 Max 4

0 </ tarifa> Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
150.00
Obligatorio^ Numérico^ Max 14^
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio cuando
<codDocReemb> sea igual
a 41
- -

Obligatorio cuando
<codDocReemb> sea igual
a 41
- -
04</tipoIdentificacionProveedorR
eembolso>

Obligatorio cuando
<codDocReemb>sea igual
a 41, conforme tabla 6
Numérico 2
1760013210001</identificacionProvee
dorReembolso>

Obligatorio cuando
<codDocReemb> sea igual
a 41
Alfanumérico Max 13
593</codPaisPagoProveedorReembol
so>

Obligatorio cuando
<codDocReemb> sea igual
a 41, conforme tabla 25
Numérico 3
01

Obligatorio cuando
<codDocReemb> sea igual
a 41, conforme tabla 26
Numérico 2
01

Obligatorio cuando
<codDocReemb> sea igual
a 41, conforme tabla 3
Numérico 2
001

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico 3
501

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico 3
000000008

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico 9
04/03/2013

Obligatorio cuando
<codDocReemb> sea igual
a 41
Fecha dd/mm/aaaa
04032013011792261104001100150100000
00081234567816

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico 10, 37 o 49

Obligatorio cuando
<codDocReemb> sea igual
a 41
- -

Obligatorio cuando
<codDocReemb> sea igual
a 41
- -
2

Obligatorio cuando
<codDocReemb> sea igual
a 41, conforme tabla 16
Numérico 1
2

Obligatorio cuando
<codDocReemb> sea igual
a 41, conforme tabla 17
Numérico Min 1 Max 4
12

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico
Min 1 Max 4 /
2 enteros, 2
decimales
133.93

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico Max 14
16.07

Obligatorio cuando
<codDocReemb> sea igual
a 41
Numérico Max 14

Obligatorio cuando
<codDocReemb> sea igual
a 41
- -
(^) sea igual Obligatorio cuando - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
a 41

Obligatorio cuando
<codDocReemb> sea igual
a 41
- -

Obligatorio cuando
<codDocReemb>sea igual
a 41
- -

Obligatorio cuando
corresponda. Aplica para
comercializadores de
Derivados de Petróleo.
- -
Obligatorio cuando corresponda - -

4

Obligatorio cuando
corresponda conforme
tabla 22
Numérico 1
327

Obligatorio cuando
corresponda conforme
tabla 23
Numérico Min 1 Max 3
0.20 Obligatorio cuando corresponda Numérico

Min 1 Max 5 /
3 enteros,
dos
decimales
0.13 Obligatorio cuando corresponda Numérico

Max 14 /12
enteros, 2
decimales
Obligatorio cuando corresponda - -

Obligatorio cuando corresponda - -
Obligatorio cuando corresponda - -

CAJAS DE 10
KILOS

Obligatorio cuando
corresponda Alfanumérico^ Max 300^
<campoAdicionalnombre=" INFORMACION BANCARIA "> NUMERO DE
CUENTA DE BANCO 1243546

Obligatorio cuando
corresponda Alfanumérico^ Max 300^
Obligatorio cuando corresponda - -

Obligatorio - -

TABLA 26: Tipo Proveedor de Reembolso

TIPO CÓDIGO
PERSONA NATURAL 01
SOCIEDAD 02
ANEXO 6 - FORMATOS XML FACTURA CON SUBSIDIOS APLICADO EN LAS VERSIONES 1.0.0 y 1.1.0
Incluyen los campos requeridos exclusivamente solo para subsidio; caso contrario
se deberá utilizar los formatos de factura establecidos en el anexo 1 y anexo 3
según corresponda^12.

(^12) Resolución NAC-DGERCGC15-00003184, publicada en el Registro Oficial 661 de 4 de enero de 2016

FACTURA VERSIÓN 1.0.0

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO / FORMATOLONGITUD

**Obligatorio** - -
Obligatorio - -
Obligatorio - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 (^) conforme tabla 2Obligatorio, Numérico 1
SERVICIO DE RENTAS INTERNAS Obligatorio Alfanumérico Max 300
SRI Obligatorio, cuando corresponda Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13
0601201601176001321000110011230000000081234567817 (^) conforme tabla 1Obligatorio, Numérico 49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
123 Obligatorio Numérico 3
000000008 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
06/01/2016 Obligatorio Fecha dd/mm/aaaa
PÁEZ Obligatorio, cuando corresponda Alfanumérico Max 300
123A Obligatorio, cuando corresponda Alfanumérico Min 3 Max 13
SI Obligatorio, cuando corresponda Texto SI/NO
04 (^) conforme tabla 6Obligatorio, Numérico 2
EMPRESA ABC Obligatorio Alfanumérico Max 300
1794567890001 Obligatorio Numérico Max 13
salinas y santiago Obligatorio, cuando corresponda Alfanumérico Max 300
25.00 Obligatorio Numérico Max 14
10.00
Opcional y se llenará
cuando exista el tag
.
Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2 (^) conforme tabla 16Obligatorio, Numérico 1
2 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
25.00 Obligatorio Numérico Max 14
3.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
28.00 Obligatorio Numérico Max 14
DOLAR Obligatorio, cuando corresponda Alfanumérico Max 15
Obligatorio - -
Obligatorio -
19 (^) conforme tabla 24Obligatorio, Numérico 2
28,000 Obligatorio Numérico Max 14
30 Obligatorio, cuando corresponda Numérico Max 14
dias Obligatorio, cuando corresponda Texto Max 10

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO / FORMATOLONGITUD
</pago> Obligatorio - -
</pagos> Obligatorio
<valorRetIva>10620.00</valorRetIva> Opcional Numérico Max 14
<valorRetRenta>2950.00</valorRetRenta> Opcional Numérico Max 14
</infoFactura> Obligatorio - -
<detalles> Obligatorio - -
<detalle> Obligatorio - -
<codigoPrincipal>0011</codigoPrincipal>
Obligatorio, (para
venta de combustible
ver tabla 30)
Alfanumérico Max 25
<codigoAuxiliar>0011</codigoAuxiliar> Obligatorio, cuando corresponda Alfanumérico Max 25
<descripcion>COMBUSTIBLE</descripcion>
Obligatorio, (para
venta de combustible
ver tabla 30)
Alfanumérico Max 300
<cantidad>1</cantidad> Obligatorio Numérico Max 14
<precioUnitario>25</precioUnitario> Obligatorio Numérico Max 14
<precioSinSubsidio>35.00</precioSinSubsidio> Obligatorio, cuando corresponda. Numérico Max 14
<descuento>0</descuento> Obligatorio Numérico Max 14
<precioTotalSinImpuesto>25.00</precioTotalSinImpuesto> Obligatorio. Numérico Max 14
<impuestos> Obligatorio - -
<impuesto> Obligatorio - -
2 (^) conforme tabla 16Obligatorio, Numérico 1
2 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
12.00 Obligatorio Numérico
Min 1 Max
4 / 2
enteros, 2
decimales
25.00 Obligatorio Numérico Max 14
3.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
FACTURA VERSIÓN 1.1.0
En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - -
Obligatorio - -
Obligatorio - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 (^) conforme tabla 2Obligatorio, Numérico 1
SERVICIO DE RENTAS INTERNAS Obligatorio Alfanumérico Max 300
SRI Obligatorio, cuando corresponda Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13
0601201601176001321000110011230000000081234567817 (^) conforme tabla 1Obligatorio, Numérico 49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
123 Obligatorio Numérico 3

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
000000008 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
06/01/2016 Obligatorio Fecha dd/mm/aaaa
PÁEZ Obligatorio, cuando corresponda Alfanumérico Max 300
123A Obligatorio, cuando corresponda Alfanumérico Min 3 Max 13

SI Obligatorio, cuando corresponda Texto SI/NO

04 (^) conforme tabla 6Obligatorio, Numérico 2
EMPRESA ABC Obligatorio Alfanumérico Max 300
1794567890001 Obligatorio Numérico Max 13
salinas y santiago Obligatorio, cuando corresponda Alfanumérico Max 300
25.00 Obligatorio Numérico Max 14
10.00
Opcional y se llenará
cuando exista el tag
.
Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2 (^) conforme tabla 16Obligatorio, Numérico 1
2 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
25.00 Obligatorio Numérico Max 14
3.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
28.00 Obligatorio Numérico Max 14
DOLAR Obligatorio, cuando corresponda Alfanumérico Max 15
Obligatorio - -
Obligatorio -
20 (^) conforme tabla 24Obligatorio, Numérico 2
28,000 Obligatorio Numérico Max 14
30 Obligatorio, cuando corresponda Numérico Max 14
dias Obligatorio, cuando corresponda Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
0011
Obligatorio, (para
venta de combustible
ver tabla 30)
Alfanumérico Max 25
0011 Obligatorio, cuando corresponda Alfanumérico Max 25
COMBUSTIBLE
Obligatorio, (para
venta de combustible
ver tabla 30)
Alfanumérico Max 300
1 Obligatorio Numérico
Max 18,
hasta 6
decimales
25 Obligatorio Numérico
Max 18,
hasta 6
decimales
35.00 Obligatorio, cuando Numérico Max 14

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
corresponda.
0 Obligatorio Numérico Max 14
25.00 Obligatorio. Numérico Max 14
Obligatorio - -
Obligatorio - -

2 (^) conforme tabla 16Obligatorio, Numérico 1
2 (^) conforme tabla 17Obligatorio, Numérico Min 1 Max 4
12.00 Obligatorio Numérico
Min 1 Max 4
/ 2 enteros, 2
decimales
25.00 Obligatorio Numérico Max 14
3.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

ANEXO 7 – FORMATOS DE
REPRESENTACIÓN IMPRESA DE
DOCUMENTO ELECTRÓNICO CON SUBSIDIO
(RIDE)
Nota:

El campo VALOR TOTAL SIN SUBSIDIO, corresponde a: precio sin subsidio + IVA según corresponda.
55.00 + 12% = $6.60
55.00 + 6.60 = $61.60

El campo AHORRO POR SUBSIDIO, corresponde al subsidio + IVA según corresponda.
27.75 + 12% = $3.33
27.75 + 3.33 = $31.08

La diferencia entre VALOR TOTAL SIN SUBSIDIO menos AHORRO POR SUBSIDIO es igual al valor total de la
factura, es decir: $30.52
ANEXO 8 - FORMATOS XML FACTURA CON
RUBROS DE TERCEROS APLICADO EN LAS
VERSIONES 2.0.0 y 2.1.0
Incluyen los campos requeridos exclusivamente para rubros de terceros, caso
contrario se deberá utilizar los formatos de factura establecidos en el anexo 1 y
anexo 3 según corresponda^13.

FACTURA VERSIÓN 2.0.0

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 (^) conforme tabla 2Obligatorio, Numérico 1
PRUEBA Obligatorio Alfanumérico Max 300
PRUEBA 2
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13
2103201601176001321000110010010000000061234567816
Obligatorio,
conforme tabla
1
Numérico 49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000006 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
21/03/2016 Obligatorio Fecha dd/mm/aaaa
PÁEZ
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
12345
Obligatorio,
cuando
corresponda
Alfanumérico Min 3 Max 13
SI
Obligatorio,
cuando
corresponda
Texto SI/NO
07 (^) conforme tabla 6Obligatorio, Numérico 2
CONSUMIDOR FINAL Obligatorio Alfanumérico Max 300
9999999999999 Obligatorio Numérico Max 13
salinas y santiago
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
50.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2
Obligatorio,
conforme tabla
16
Numérico 1
2 Obligatorio, Numérico Min 1 Max 4
(^13) Resolución NAC-DGERCGC15-00003184, publicada en el Registro Oficial 661 de 4 de enero de 2016

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
conforme tabla
17
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
61.00 Obligatorio Numérico Max 14

DOLAR

Obligatorio,
cuando
corresponda
Alfanumérico Max 15

Obligatorio,
cuando
corresponda
- -
Obligatorio -

19

Obligatorio,
conforme tabla
24
Numérico 2
61,00 Obligatorio Numérico Max 14

30

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
001 Obligatorio Alfanumérico Max 25

0011

Obligatorio,
cuando
corresponda
Alfanumérico Max 25
BIEN Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Max 14
50 Obligatorio Numérico Max 14
0 Obligatorio Numérico Max 14
50.00 Obligatorio. Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme tabla
16
Numérico 1
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
12.00 Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
CONCEPTO1 Obligatorio Alfanumérico Max 300
10 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
CONCEPTO2 Obligatorio Alfanumérico Max 300
12 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
CONCEPTO3 Obligatorio Alfanumérico Max 300
5 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
CONCEPTO4 Obligatorio Alfanumérico Max 300
25 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
Obligatorio - -

FACTURA VERSIÓN 2.1.0

En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
<?xml version="1.0" encoding="UTF-8"?> Obligatorio - -
<factura id="comprobante" version="2.1.0"> Obligatorio - -
<infoTributaria> Obligatorio - -
<ambiente>1</ambiente>
Obligatorio,
conforme tabla
4
Numérico 1
<tipoEmision>1</tipoEmision>
Obligatorio,
conforme tabla
2
Numérico 1
<razonSocial>PRUEBA</razonSocial> Obligatorio Alfanumérico Max 300
<nombreComercial>PRUEBA 2</nombreComercial>
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
<ruc>1760013210001</ruc> Obligatorio Numérico 13
<claveAcceso>2103201601176001321000110010010000000061234567816</claveAcceso>
Obligatorio,
conforme
tabla 1
Numérico 49
<codDoc>01</codDoc>
Obligatorio,
conforme tabla
3
Numérico 2
<estab>001</estab> Obligatorio Numérico 3
<ptoEmi>001</ptoEmi> Obligatorio Numérico 3
<secuencial>000000006</secuencial> Obligatorio Numérico 9
<dirMatriz>SALINAS</dirMatriz> Obligatorio Alfanumérico Max 300
</infoTributaria> Obligatorio - -
<infoFactura> Obligatorio - -
<fechaEmision>21/03/2016</fechaEmision> Obligatorio Fecha dd/mm/aaaa
<dirEstablecimiento>PÁEZ</dirEstablecimiento>
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
<contribuyenteEspecial>12345</contribuyenteEspecial>
Obligatorio,
cuando
corresponda
Alfanumérico Min 3 Max 13
<obligadoContabilidad>SI</obligadoContabilidad>
Obligatorio,
cuando
corresponda
Texto SI/NO
<tipoIdentificacionComprador>07</tipoIdentificacionComprador>
Obligatorio,
conforme tabla
6
Numérico 2
<razonSocialComprador>CONSUMIDOR FINAL</razonSocialComprador> Obligatorio Alfanumérico Max 300
<identificacionComprador>9999999999999</identificacionComprador> Obligatorio Numérico Max 13
<direccionComprador>salinas y santiago</direccionComprador>
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
<totalSinImpuestos>50.00</totalSinImpuestos> Obligatorio Numérico Max 14
<totalDescuento>0.00</totalDescuento> Obligatorio Numérico Max 14
<totalConImpuestos> Obligatorio - -
<totalImpuesto> Obligatorio - -
2 (^) conforme tabla Obligatorio, Numérico 1

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
16
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
61.00 Obligatorio Numérico Max 14

DOLAR

Obligatorio,
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -

19

Obligatorio,
conforme tabla
24
Numérico 2
61,00 Obligatorio Numérico Max 14

30

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
001 Obligatorio Alfanumérico Max 25

0011

Obligatorio,
cuando
corresponda
Alfanumérico Max 25
BIEN Obligatorio Alfanumérico Max 300

1 Obligatorio Numérico

Max 18,
hasta 6
decimales
50 Obligatorio Numérico

Max 18,
hasta 6
decimales
0 Obligatorio Numérico Max 14
50.00 Obligatorio. Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme tabla
16
Numérico 1
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
12.00 Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
CONCEPTO1 Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
CONCEPTO2 Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
CONCEPTO3 Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
CONCEPTO4 Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Min 1 Max 4
Obligatorio - -
Obligatorio - -
Obligatorio - -

ANEXO 9 - FORMATOS XML FACTURA
SUSTITUTIVA DE GUÍA DE REMISIÓN
APLICADO EN LAS VERSIONES 2.0.0 y 2.1.0
Incluyen los campos requeridos exclusivamente para la factura sustitutiva de guía
de remisión, caso contrario se deberá utilizar los formatos de factura establecidos
en el anexo 1 y anexo 3 según corresponda^14.
FACTURA VERSIÓN 2.0.0

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 (^) conforme tabla 2Obligatorio, Numérico 1
PRUEBA
Obligatorio^ Alfanumérico^ Max 300^
PRUEBA 2
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13
2203201601176001321000110010010000000101234567812
Obligatorio,
conforme tabla
1
Numérico 49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000010 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
22/03/2016 Obligatorio Fecha dd/mm/aaaa
PÁEZ
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
12345
Obligatorio,
cuando
corresponda
Alfanumérico Min 3 Max 13
SI
Obligatorio,
cuando
corresponda
Texto SI/NO
07 (^) conforme tabla 6Obligatorio, Numérico 2
CONSUMIDOR FINAL Obligatorio Alfanumérico Max 300
9999999999999 Obligatorio Numérico Max 13
salinas y santiago
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
50.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2
Obligatorio,
conforme tabla
16
Numérico 1
(^14) Resolución NAC-DGERCGC15-00003184, publicada en el Registro Oficial 661 de 4 de enero de 2016

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
56.00 Obligatorio Numérico Max 14

DOLAR

Obligatorio,
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -

18

Obligatorio,
conforme tabla
24
Numérico 2
56,00 Obligatorio Numérico Max 14

30

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
001 Obligatorio Alfanumérico Max 25

0011

Obligatorio,
cuando
corresponda
Alfanumérico Max 25
BIEN Obligatorio Alfanumérico Max 300
1 Obligatorio Numérico Max 14
50 Obligatorio Numérico Max 14
0 Obligatorio Numérico Max 14
50.00 Obligatorio. Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme tabla
16
Numérico 1
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
12.00 Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

DIRECCION PARTIDA

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
DESTINATARIO

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
22/03/2016

Obligatorio,
cuando
corresponda
Fecha dd/mm/aaaa
22/03/2016

Obligatorio,
cuando
corresponda
Fecha dd/mm/aaaa
RAZON SOCIAL

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
04

Obligatorio,
cuando
corresponda
conforme tabla 6
Numérico 2
1002576302001

Obligatorio,
cuando
corresponda
Numérico Max 13
PVB0341

Obligatorio
cuando
corresponda
(para la venta de
combustible ver
tabla 29)
Alfanumérico Max 20
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 2

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 3

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 4

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

FACTURA VERSIÓN 2.1.0

En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

**Obligatorio** - - **Obligatorio** - - **Obligatorio** - -
1 (^) conforme tabla 4Obligatorio, Numérico 1
1 (^) conforme tabla 2Obligatorio, Numérico 1
PRUEBA Obligatorio Alfanumérico Max 300
PRUEBA 2
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
1760013210001 Obligatorio Numérico 13
2203201601176001321000110010010000000101234567812
Obligatorio,
conforme tabla
1
Numérico 49
01 (^) conforme tabla 3Obligatorio, Numérico 2
001 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000010 Obligatorio Numérico 9
SALINAS Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
22/03/2016 Obligatorio Fecha dd/mm/aaaa
PÁEZ
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
12345
Obligatorio,
cuando
corresponda
Alfanumérico Min 3 Max 13
SI
Obligatorio,
cuando
corresponda
Texto SI/NO
07 (^) conforme tabla 6Obligatorio, Numérico 2
CONSUMIDOR FINAL Obligatorio Alfanumérico Max 300
9999999999999 Obligatorio Numérico Max 13
salinas y santiago
Obligatorio,
cuando
corresponda
Alfanumérico Max 300
50.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
2
Obligatorio,
conforme tabla
16
Numérico 1
2
Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
0.00 Obligatorio Numérico Max 14
56.00 Obligatorio Numérico Max 14
DOLAR
Obligatorio,
cuando
corresponda
Alfanumérico Max 15
Obligatorio - -
Obligatorio -
18
Obligatorio,
cuando
corresponda
Numérico 2

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
conforme tabla
24
56,00 Obligatorio Numérico Max 14

30

Obligatorio,
cuando
corresponda
Numérico Max 14
dias

Obligatorio,
cuando
corresponda
Texto Max 10
Obligatorio - -
Obligatorio - -
0.00 Opcional Numérico Max 14
0.00 Opcional Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
001 Obligatorio Alfanumérico Max 25

0011

Obligatorio,
cuando
corresponda
Alfanumérico Max 25
BIEN Obligatorio Alfanumérico Max 300

1 Obligatorio Numérico

Max 18,
hasta 6
decimales
50 Obligatorio Numérico

Max 18,
hasta 6
decimales
0 Obligatorio Numérico Max 14
50.00 Obligatorio. Numérico Max 14
Obligatorio - -
Obligatorio - -

2

Obligatorio,
conforme tabla
16
Numérico 1
2

Obligatorio,
conforme tabla
17
Numérico Min 1 Max 4
12.00 Obligatorio Numérico

Min 1 Max 4
/ 2 enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

DIRECCION PARTIDA

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
DESTINATARIO

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
22/03/2016

Obligatorio,
cuando
corresponda
Fecha dd/mm/aaaa
22/03/2016

Obligatorio,
cuando
corresponda
Fecha dd/mm/aaaa
RAZON SOCIAL

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
04

Obligatorio,
cuando
corresponda
conforme tabla 6
Numérico 2
1002576302001

Obligatorio,
cuando
corresponda
Numérico Max 13
PVB0341

Obligatorio
cuando
corresponda
Alfanumérico Max 20
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
(para la venta de
combustible ver
tabla 29)
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 1

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 2

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 3

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -

MOTIVO TRASLADO MERCADERIA 4

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
0041324846887

Obligatorio
cuando
corresponda
Alfanumérico Max 20
001

Obligatorio,
cuando
corresponda
Numérico 3
Quito - Cayambe- Otavalo

Obligatorio,
cuando
corresponda
Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

ANEXO 10 - FORMATO XML DE
COMPROBANTE DE RETENCIÓN ATS
VERSIÓN 2.0.0
Esta versión de comprobante incluye la información que se reporta a través del
módulo de compras del Anexo Transaccional Simplificado (ATS).
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - - Obligatorio - - Obligatorio - -
1

Obligatorio, conforme
tabla 4 de la Ficha
Técnica Offline
Numérico 1
1

Obligatorio, conforme
tabla 2 de la Ficha
Técnica Offline
Numérico 1
Distribuidora de Suministros Nacional S.A. Obligatorio Alfanumérico Max 300
Empresa Importadora y Exportadora de Piezas y Partes de
Equipos de Oficina Opcional^ Alfanumérico^ Max 300^
1792146739001 Obligatorio Numérico 13
2410201107179214673900110020010000000011234567815</clav
eAcceso> Obligatorio^ Numérico^49

07

Obligatorio, conforme
tabla 3 de la Ficha
Técnica Offline
Numérico 2
002 Obligatorio Numérico 3
001 Obligatorio Numérico 3
000000001 Obligatorio Numérico 9
Enrique Guerrero Portilla OE1-34 AV. GALO PLAZA LASSO Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
15/01/2012 Obligatorio Fecha dd/mm/aaaa
Rodrigo Moreno S/N Francisco García Opcional Alfanumérico Max 300
5368 Opcional Alfanumérico Min 3 Max 13
SI Opcional Texto SI/NO

04

Obligatorio, conforme
tabla 6 de la Ficha
Técnica Offline
Numérico 2
01

Obligatorio, conforme
tabla 14 Catalogo ATS. Si
el tipo de identificación
del Sujeto Retenido es
igual a IDENTIFICACION
DEL EXTERIOR
Numérico 2
SI Obligatorio Alfabético: SI/NO 2
Juan Pablo Chávez
Núñez Obligatorio^ Alfanumérico^ Max 300^
1713328506001 Obligatorio Alfanumérico Max 20
03/2012 Obligatorio Fecha mm/aaaa
Obligatorio - -
Obligatorio - -
Obligatorio

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
10 Obligatorio, conforme tabla 5 Catálogo ATS Numérico 2

19 (^) tabla 4 del Catálogo ATSObligatorio, conforme Numérico Min 2, Max 3
002001000000001 Opcional Numérico 15
20/01/2012 Obligatorio Fecha dd/mm/aaaa
15/03/2012 Opcional Fecha dd/mm/aaaa
2110201116 Opcional Numérico 10 o 37 o 49
01
Obligatorio, conforme
tabla 15 del Catálogo
ATS
Numérico 2
01
Obligatorio cuando el
campo
sea igual 02. Tabla 19 del
Catálogo ATS
Numérico 2
212
Se genera cuando el
código del campo
sea igual
02, si es igual
01 registrar el código de
la tabla 25 de la Ficha
Técnica Offline.
Si es igual 02
registrar el país asociado
al paraíso fiscal tabla 17
Catálogo ATS.
Si es igual 03
escoger códigos de la
tabla 16 del Catálogo
ATS, excepto código 593
Numérico 3 o 4
NO
Obligatorio cuando el
sea igual
02 se llena el campo
Texto SI/NO
NO
Obligatorio el campo
se
haya escogido la opción
NO
Texto SI/NO
SI
Obligatorio cuando el
campo
sea igual 02

SI 2
141. 01

Obligatorio, si
<codDocSustento> es
igual a 41, corresponde a
la suma de
<totalBaseImponibleRee
mbolso> y
<totalImpuestoReembols
o>
Numérico Max 14
120. 75

Obligatorio, si
<codDocSustento> es
igual a 41, corresponde a
la sumatoria de las
etiquetas
<baseImponibleReembol
so>, el cual es mayor o
igual a la sumatoria
Numérico Max 14
20.26

Obligatorio, si
<codDocSustento> es
igual a 41, corresponde a
la sumatoria de las
etiquetas
<impuestoReembolso>,
el cual es mayor o igual a
la sumatoria
Numérico Max 14
120.75 Obligatorio Numérico Max 14

141.01 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
2

Obligatorio, conforme
tabla 16 de la Ficha
Técnica Offline
Numérico 1
2

Obligatorio, conforme
tabla 17 o 18 de la Ficha
Técnica Offline
Numérico Min 1 Max 4
125. 90 Obligatorio Numérico Max 14

12 Obligatorio Numérico Max y 2 decimales3 enteros

1 5.11 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -

1

Obligatorio, conforme
tabla 19 de la Ficha
Técnica Offline
Numérico 1
312

Obligatorio, conforme
tabla 20 de la Ficha
Técnica Offline
Numérico Min 1 Max 5
125. 90 Obligatorio Numérico Max 14

1.75

Obligatorio, conforme
tabla 20 de la Ficha
Técnica Offline
Numérico
Min 1 Max 5
entre enteros
y decimales
2. 20 Obligatorio Numérico

Max 12
enteros y 2
decimales
^15

Obligatorio cuando la
etiqueta <codSustento>
es igual a 10
- -
15/03/2012

Obligatorio cuando la
etiqueta <codSustento>
es igual a 10
Fecha dd/mm/aaaa
102.54

Obligatorio cuando la
etiqueta <codSustento>
es igual a 10
Numérico
Max 1 4
enteros y 2
decimales
2012

Obligatorio cuando la
etiqueta <codSustento>
es igual a 10
Numérico 4
Obligatorio cuando la
etiqueta <codSustento>
es igual a 10
- -
Obligatorio cuando corresponda - -

2012

Obligatorio cuando
corresponda. Debe
desplegarse solamente
en el caso de que el
campo
<codigoRetencion> sea
igual a 338, 340, 341 y
342; 342A; 342B
Numérico Max 7 enteros
2012

Obligatorio cuando
corresponda. Debe
desplegarse solamente
en el caso de que el
campo
<codigoRetencion> sea
igual a 338, 340, 341 y
342; 342A; 342B
Numérico
Max 1 2
enteros y 2
decimales
Obligatorio cuando corresponda - -

(^15) Para efectos tributarios, se considerarán dividendos y tendrán el mismo tratamiento tributario todo tipo de participaciones en utilidades,
excedentes, beneficios o similares que se obtienen en razón de los derechos representativos de capital que el beneficiario mantiene, de
manera directa o indirecta.

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -
Obligatorio - -


Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
Obligatorio - -

04</tipoIdentificacionProveedorReembols
o >

Obligatorio cuando
<codDocSustento> sea
igual a 41, conforme tabla
6 de la Ficha Técnica
Offline
Numérico 2
1760013210001</identificacionProveedorRee
mbolso>

Obligatorio cuando
<codDocSustento> sea
igual a 41
Alfanumérico Max 20
212

Obligatorio cuando
<codDocSustento> sea
igual a 41, conforme la
tabla 25 de la Ficha
Técnica Offline
Numérico 3
01

Obligatorio cuando
<codDocSustento> sea
igual a 41, conforme tabla
26 de la Ficha Técnica
Offline
Numérico 2
01

Obligatorio cuando
<codDocSustento> sea
igual a 41, validar
conforme tabla 4 del
Catálogo ATS
Numérico 2
001

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico 3
501

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico 3
000000008

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico 9
04/03/2013

Obligatorio cuando
<codDocSustento> sea
igual a 41
Fecha dd/mm/aaaa
040320130117922611040011001501000000008
1234567816

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico 10 o 37 o 49

Obligatorio cuando
<codDocSustento> sea
igual a 41
- -

Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
2

Obligatorio cuando
<codDocSustento> sea
igual a 41, tabla 16 de la
Ficha Técnica Offline
Numérico 1
2

Obligatorio cuando
<codDocSustento> sea
igual a 41, conforme tabla
17 o 18 de la Ficha
Técnica Offline
Numérico Min 1 Max 4
12

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Min 1 Max 4
125. 90

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Max 14
15. 11

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Max 14
Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
3

Obligatorio cuando
<codDocSustento> sea
igual a 41, tabla 16 de la
Ficha Técnica Offline
Numérico 1
3011

Obligatorio cuando
<codDocSustento> sea
igual a 41, conforme tabla
17 o 18 de la Ficha
Técnica Offline
Numérico Min 1 Max 4
0

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Min 1 Max 4
0. 00

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Max 14
5. 15

Obligatorio cuando
<codDocSustento> sea
igual a 41
Numérico Max 14
Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
Obligatorio - -

Obligatorio cuando
<codDocSustento> sea
igual a 41
- -
Obligatorio - -
Obligatorio - -

01

Obligatorio, conforme
tabla 13 del Catálogo
ATS
Numérico 2
500 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Opcional - -
2000 Opcional^ Alfanumérico^1
Opcional Numérico Min 1 Max 4
Obligatorio Numérico Min 1 Max 4

Para registrar los códigos a utilizar, se recomienda revisar las tablas del catálogo
de Anexo Transaccional Simplificado (ATS), publicado en la página web
http://www.sri.gob.ec: Información sobre impuestos/Cómo declaro mis impuestos? /
Anexos y guías o directamente a través del siguiente link:
http://www.sri.gob.ec/web/guest/formularios-e-instructivos1
Nota : El formato RIDE del comprobante de retención corresponderá al publicado
para la versión 1.0.0.
ANEXO 11 – REQUISITOS OBLIGATORIOS
PARA EL XML DE FACTURA COMERCIAL
NEGOCIABLE
Las facturas electrónicas comerciales negociables deberán contener la siguiente
información en la estructura del XML; caso contrario no podrán ser generadas
como negociables:
1. Dirección comprador:

<direccionComprador>salinas y santiago</direccionComprador>
2. Formas de pago:

<pagos>
<pago>
<formaPago>21</formaPago>
<total>56,00</total>
<plazo>30</plazo>
<unidadTiempo>dias</unidadTiempo>
</pago>
</pagos>
Para más información respecto a Factura Electrónica Comercial Negociable,
ingrese al siguiente link: http://www.sri.gob.ec/web/guest/facturacion-
electronica#informaci%C3%B3n
Únicamente para aquellos contribuyentes que se dedican a la negociación de
facturas electrónicas y que requieran realizar la notificación masiva de las facturas
mediante el servicio expuesto en el portal web en la opción de Comprobantes
Electrónicos / Ambientes Pruebas o Producción / Factura Comercial Negociable,
deberán incluir obligatoriamente en la estructura del archivo xml entre los tags
</detalles> e <infoAdicional> previa autorización del comprobante, la dirección de
correo electrónico del receptor, en los siguientes campos:

controldecalidad@sriprueba.ad

Si la notificación de las facturas comerciales negociables es de manera individual,
no se registrará la información mencionada.
ANEXO 12 – REQUISITO OBLIGATORIO PARA
EL XML DE FACTURA EN VENTA DE
COMBUSTIBLES LÍQUIDOS DERIVADOS DE
HIDROCARBUROS Y BIOCOMBUSTIBLES.
Las facturas electrónicas en venta de combustibles líquidos derivados de
hidrocarburos (CLDH) y biocombustibles deberán contener el tag placa en la
estructura del XML, esto entre los tags y formas de pago para las
versiones 1.0.0, 1.1.0, 2.0.0, 2.1.0;

1. Placa

DOLAR
PCM4567

Para mayor información respecto a facturas para ventas de combustibles líquidos
derivados de hidrocarburos y biocombustibles, ingrese al siguiente link:
http://www.sri.gob.ec/web/guest/facturacion-electronica#informaci%C3%B3n

2. Llenado del campo Placa

El campo deberá llenarse considerando las siguientes especificaciones,
según lo dispuesto por el organismo regulador^16 :

TABLA 29: FORMATO DE LLENADO DEL CAMPO PLACA

Caso Descripción (^) Campo Observaciones
1 Vehículo automotor de transporte terrestre Se deberá ingresar las letras y números sin ningún espacio
2 Vehículo automotor de transporte terrestre
Si existen solo tres dígitos se
deberá anteponer el cero sin ningún
espacio
3 Cuantía doméstica
Las letras “CU” seguido de la parte
numérica de la autorización de la
cuantía doméstica
4
Personas naturales o jurídicas sin vehículo que
adquieran un volumen de despacho inferior a 5
galones en provincias no fronterizas
Se deberá ingresar tres letras “Z” y cuatro números nueves (9)

5
Personas naturales o jurídicas sin vehículo que
adquieran un volumen de despacho inferior a 3
galones en frontera
< ZZZ9999> Se deberá ingresar tres letras “Z” y cuatro números nueves (9)
6
Para el caso de venta de combustible a motos,
vehículos diplomáticos, régimen de internación
temporal y otros que tienen placa asignada por
Se deberá colocar la placa del
vehículo asignada por la Agencia
Nacional de Transito, tal como
(^16) Disposiciones sobre el llenado del campo PLACA dadas a los distribuidores de combustible por la Agencia de Regulación y Control de
Energía y Recursos Naturales No Renovables mediante Oficio Nro. ARCERNNR-CTRCH- 2024 - 0014 - OF del 10 de enero de 202 4.

Caso Descripción (^) Campo Observaciones
la Agencia Nacional de Tránsito consta en
la matrícula.
7 Para el caso de venta de combustibles a vehículos extranjeros. Se deberá colocar la placa internacional del vehículo.

8
Para el caso de Equipo Caminero, Maquinaria
Pesada y Maquinaria Agrícola que tengan
matricula asignada por el Ministerio de
Transporte y Obras Publicas- MTOP.
Se debe ingresar en el campo placa
las Letras MAQN, y seguido del
número completo de la matrícula
otorgado por el MTOP.
ANEXO 13 – REQUISITO OBLIGATORIO PARA
XML DE COMPROBANTES EMITIDOS DESDE
UNA MÁQUINA FISCAL
Los comprobantes factura, nota de crédito, nota de débito, guía de remisión y
comprobante de retención para todas sus versiones deberán contener los
siguientes tags: marca, modelo y serie en la estructura del XML como se muestra a
continuación:



SISPAU
ABC1234
CGMC1405


ANEXO 14 – EJEMPLO FIRMA ELECTRÓNICA
BAJO ESTÁNDAR XADES_BES
1 1 SERVICIO DE RENTAS INTERNAS LE HACE BIEN AL PAIS 1760013210001 0503201201176001321000110010030009900641234567814 01 001 003 000990064 AMAZONAS Y ROCA 05/03/2012 SALINAS Y SANTIAGO 12345 SI 05 EGUIGUREN PENARRETA GABRIEL FERNANDO 1103029144 100.00 0.00


< 114 escri> 2 </ 114 escri>
2
100.00
12.00


0.00
112.00
DÓLAR



001
001
< 114 escripción>SILLA DE MADERA</ 114 escripción>
1.00
100.00
0.00
100.00


< 114 escri> 2 </ 114 escri>
2
12.00
100.00
12.00





LOS PERALES Y AV. ELOY ALFARO
2123123
gfeguiguren@sri.gob.ec

→
<ds:Reference URI=”#Certificate1562780”>
<ds:DigestMethod Algorithm=”http://www.w3.org/2000/09/xmldsig#sha1”></ds:DigestMethod>
<ds:DigestValue><!–HASH O DIGEST DEL CERTIFICADO X509 →</ds:DigestValue>
</ds:Reference>
<ds:Reference Id=”Reference-ID- 363558 ” URI=”#comprobante”>
<ds:Transforms>
<ds:Transform Algorithm=”http://www.w3.org/2000/09/xmldsig#enveloped-signature”></ds:Transform>
</ds:Transforms>
<ds:DigestMethod Algorithm=”http://www.w3.org/2000/09/xmldsig#sha1”></ds:DigestMethod>
<ds:DigestValue><!–HASH O DIGEST DE TODO EL ARCHIVO XML IDENTIFICADO POR EL
id=”comprobante”→</ds:DigestValue>
</ds:Reference>
</ds:SignedInfo>
<ds:SignatureValue Id=”SignatureValue398963”>

<!–MODULO DEL CERTIFICADO X509 →
</ds:Modulus>
<ds:Exponent>AQAB</ds:Exponent>
</ds:RSAKeyValue>
</ds:KeyValue>
</ds:KeyInfo>
<ds:Object Id=”Signature620397-Object231987”><etsi:QualifyingProperties
Target=”#Signature620397”><etsi:SignedProperties Id=”Signature620397-
SignedProperties24123”><etsi:SignedSignatureProperties><etsi:SigningTime> 2012 - 03 - 05T16:57:32-
05:00</etsi:SigningTime><etsi:SigningCertificate><etsi:Cert><etsi:CertDigest><ds:DigestMethod
Algorithm=”http://www.w3.org/2000/09/xmldsig#sha1”></ds:DigestMethod><ds:DigestValue>xUQewsj7MrjSfyMnhWz5DhQn
WJM=</ds:DigestValue></etsi:CertDigest><etsi:IssuerSerial><ds:X509IssuerName>CN=AC BANCO CENTRAL DEL
ECUADOR,L=QUITO,OU=ENTIDAD DE CERTIFICACION DE INFORMACION-ECIBCE,O=BANCO CENTRAL DEL
ECUADOR,C=EC</ds:X509IssuerName><ds:X509SerialNumber> 1312833444 </ds:X509SerialNumber></etsi:IssuerSerial></
etsi:Cert></etsi:SigningCertificate></etsi:SignedSignatureProperties><etsi:SignedDataObjectProperties><etsi:DataObjectFor
mat ObjectReference=”#Reference-ID- 363558 ”><etsi:Description>contenido
comprobante</etsi:Description><etsi:MimeType>text/xml</etsi:MimeType></etsi:DataObjectFormat></etsi:SignedDataObject
Properties></etsi:SignedProperties></etsi:QualifyingProperties></ds:Object>
</ds:Signature>

Nota : Los archivos XML de comprobantes electrónicos se encuentran disponibles
en el portal web del SRI.

ANEXO 15 – COMPATIBILIDAD DISPOSITIVOS
PROVISTOS
BANCO CENTRAL DEL ECUADOR

Windows
XP,
Vista, 7
(32-bits)
Windows
Vista,
7
(64-bits)
Red Hat Enterprise
5.4
(32-bit and 64-bit)
en kernel 2.6
CentOS 5.4
(32-bit and 64-bit)
en kernel 2.6
SUSE Linux Enterprise
11 (32-bit) en kernel
2.6
Fedora 12 (32-bit)
Ubuntu 10.04 (32-bit
and 64 bit) en kernel
2.6
Ubuntu
8.0.x
9.0.x
MAC OS X
LION (10.7)
Ikey2032
✓
(A) (1)
✓
(B) (2)
× ✓
(D) (3)
×
Aladin
etoken PRO
✓
(A) (1)
✓
(B) (2)
✓
(C) (3)
× ✓^
(E) (5)
Driver SafeNet AuthenticationClient-x32-8.00.msi provisto por la página web del
B.C.E.
Driver SafeNet AuthenticationClient-x64-8.00.msi provisto por la página web del
B.C.E.
Driver SafeNetAuthenticationClient_Linux_v8.0.zip provisto por la página web
del B.C.E.
Driver BSecPKLinux-2.0.0.0007.zip provisto por la página web del B.C.E.
Driver eToken_PKI_Client_4_55_Mac.zip provisto por la página web del B.C.E.
(1) Requiere tener instalado el JRE de java versión 6.x (Java SE 6 Update 26 o
superior)

(2) Requiere tener instalado el JRE de java versión 7.x (Java SE 7u3)

(3) Requiere tener instalada el JRE SE 6.x respectivo a la versión que corresponda
de Linux

(4) Requiere tener instalada el Java SE 6 correspondiente al MAC OS

ANEXO 16 – REQUISITO OBLIGATORIO DE
LLENADO PARA EL XML DE FACTURA EN LA
VENTA DE COMBUSTIBLES LÍQUIDOS
DERIVADOS DE HIDROCARBUROS Y
BIOCOMBUSTIBLES.
En la emisión del comprobante de venta tipo factura realizados por la venta de
combustibles líquidos derivados de hidrocarburos (CLDH) y biocombustibles, en la
sección , para el llenado de los campos y
<descripción> se deberán considerar la información del combustible conforme al
siguiente detalle:

TABLA 30

<codigoPrincipal> <Descripción>
0103 SÚPER
0101 EXTRA
0174 EXTRA CON ETANOL
0121 DIESEL PREMIUM
0104 DIESEL 2
*De conformidad con el Oficio Nro. ARCERNNR-CTRCH- 2024 - 0014 - OF emitido por la Agencia de Regulación y Control de Energía y
Recursos Naturales No Renovables
ANEXO 17 – FORMATOS XML LIQUIDACIÓN
DE COMPRA DE BIENES Y PRESTACIÓN DE
SERVICIOS EN LAS VERSIONES 1.0.0 Y 1.1.0
LIQUIDACIÓN DE COMPRA DE BIENES Y PRESTACIÓN DE
SERVICIOS VERSIÓN 1.0.0
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -
<liquidacionCompra id="comprobante" versión="1.0.0"> Obligatorio - -
Obligatorio - -

1 Obligatorio, conforme tabla 4 de la Ficha Técnica Offline Numérico 1

1 Obligatorio, conforme tabla 2 de la Ficha Técnica Offline Numérico 1

razonSocial0 Obligatorio Alfanumérico Max 300
nombreComercial0 Opcional Alfanumérico Max 300
0000000000001 Obligatorio Numérico 13
0000000000000000000000000000000000000000000000000<
/claveAcceso> Obligatorio^ Numérico^49

03

Obligatorio, conforme tabla 4
de la Catálogo Técnica Anexo
ATS
Numérico 2
000 Obligatorio Numérico 3
000 Obligatorio Numérico 3
000000000 Obligatorio Numérico 9
dirMatriz0 Obligatorio Alfanumérico Max 300
Obligatorio - -

- Obligatorio - -
01/01/2000 Obligatorio Fecha dd/mm/aaaa
dirEstablecimiento0 Opcional Alfanumérico Max 300
contribuyente Opcional Alfanumérico Min 3 Max 13
SI Opcional Texto SI/NO

05 Opcional conforme tabla 6 de la Ficha Técnica Offline Numérico 2

EMPRESA ABC Obligatorio Alfanumérico Max 300
1794567890001 Obligatorio Alfanumérico 20
direccionComprador0 Opcional Alfanumérico Max 300
50.00 Obligatorio Numérico Max 14
0.00 Obligatorio Numérico Max 14

00

Obligatorio, si
<codDocReembolso> es igual a
41.
Numérico Max 2
56.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la suma de
<totalBaseImponibleReembolso
> y <totalImpuestoReembolso>
Numérico Max 14
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
50.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la suma de
<BaseImponibleReembolso>
Numérico Max 14
6.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la sumatoria
de los tags
<impuestoReembolso>, el cual
es mayor o igual a la sumatoria.
Numérico Max 14
Obligatorio
Obligatorio

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 16 Numérico Max 2

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 17 Numérico Max 2

0.00 Opcional Numérico Max 14
50.00 Obligatorio Numérico Max 14

12 Obligatorio Numérico

Min 1 Max 4 / 2
enteros, 2
decimales
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

56.00

Obligatorio corresponde a la
sumatoria de bases imponibles
e impuestos.
Numérico Max 14
moneda0 Obligatorio Alfanumérico Max 14
Obligatorio
Obligatorio

01 Obligatorio. Conforme tabla 24 de la Ficha Técnica Offline Numérico Max 2

56.00 Obligatorio Numérico Max 14
30 Obligatorio Numérico Max 14
unidadTiem Opcional Texto Max 10

Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
codigoPrincipal0 Obligatorio Alfanumérico Max 25
codigoAuxiliar0 Opcional Alfanumérico Max 25
descripcion0 Obligatorio Alfanumérico Max 300
unidadMedida0 Opcional Alfanumérico Max 50
1 Obligatorio Numérico Max 14
50.00 Obligatorio Numérico Max 14
0.00 Opcional Numérico Max 14

50.00 Obligatorio, debe multiplicar el campo precio por cantidad Numérico Max 14

Opcional - -
Opcional Alfanumérico Max 300
Opcional Alfanumérico Max 300

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Opcional - -
Obligatorio - -
Obligatorio - -

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 16 Numérico Max 2

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 17 Numérico Max 2

12 Obligatorio Numérico

Min 1 Max 4 / 2
enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio cuando
<codDocReembolso> sea igual
a 41
- -

Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
04</tipoIdentificacionProveedorRee
mbolso>

Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 2
Validar código de tipo de
identificación conforme tabla 6
de la Ficha Técnica Offline
identificacionProvee</identificacionPro
veedorReembolso>
Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 20
Validar código de tipo de
identificación conforme tabla
26 de la Ficha Técnica Offline
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 3
Validar de acuerdo tabla 25 de
la Ficha Técnica Offline
01
Obligatorio cuando
< codDocReembolso> sea igual
a 41, Validar con tabla 26 de la
Ficha Técnica Offline
Numérico Max 2
00
Obligatorio cuando
< codDocReembolso> sea igual
a 41, Validar tabla 4 de
Catálogo Anexo ATS
Numérico Max 3
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41.
Numérico Max 3
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 3
000000000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 9
01/01/2000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Fecha dd/mm/aaaa
0000000000</numeroautorizacionDocRee Obligatorio cuando Numérico Max 10, 37 ó 49

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
mb> < codDocReembolso> sea igual
a 41


Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -

Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
2

Obligatorio cuando
< codDocReembolso> sea igual
a 41. Conforme tabla 16 de la
Ficha Técnica Offline
Numérico Max 2
2

Obligatorio cuando
< codDocReembolso> sea igual
a 41, conforme tabla 17 de la
Ficha Técnica Offline
Numérico Max 2
12

Obligatorio cuando
< codDocReembolso> sea igual
a 41, conforme tabla 17 de la
Ficha Técnica Offline
Numérico
Min 1 Max 4 / 2
enteros, 2
decimales
50.00

Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 14
50.00

Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 14
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando corresponda - -

SISPAU Obligatorio cuando corresponda Alfanumérico Min 1 Max 100

ABC1234 Obligatorio cuando corresponda Alfanumérico Min 1 Max 100

CGMC1405 Obligatorio cuando corresponda Alfanumérico Max 30

Obligatorio cuando corresponda - -

Opcional - -
campoAdicional0 Opcional Alfanumérico Max 300
campoAdicional1 Opcional Alfanumérico Max 300
Opcional - -

LIQUIDACIÓN DE COMPRA DE BIENES Y PRESTACIÓN DE
SERVICIOS VERSIÓN 1.1.0
En esta versión se podrá utilizar de 2 a 6 decimales en los campos de cantidad y
precio unitario para contribuyentes que lo requieran.
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - - Obligatorio - - Obligatorio - -
1 Obligatorio, conforme tabla 4 de la Ficha Técnica Offline Numérico 1

1 Obligatorio, conforme tabla 2 de la Ficha Técnica Offline Numérico 1

razonSocial0 Obligatorio Alfanumérico Max 300
nombreComercial0 Opcional Alfanumérico Max 300
0000000000001 Obligatorio Numérico 13
0000000000000000000000000000000000000000000000000</
claveAcceso> Obligatorio^ Numérico^49

03 Obligatorio, conforme tabla 4 del Catálogo ATS Numérico 2

000 Obligatorio Numérico 3
000 Obligatorio Numérico 3
000000000 Obligatorio Numérico 9
dirMatriz0 Obligatorio Alfanumérico Max 300
Obligatorio - -
Obligatorio - -
01/01/2000 Obligatorio Fecha dd/mm/aaaa
dirEstablecimiento0 Opcional Alfanumérico Max 300
contribuyente Opcional Alfanumérico Min 3 Max 13
SI Opcional Texto SI/NO

05 Opcional conforme tabla 6 de la Ficha Técnica Offline Numérico 2

EMPRESA ABC Obligatorio Alfanumérico Max 300
1750863147 Obligatorio Alfanumérico Max 20
direccionProveedor Opcional Alfanumérico Max 300

50.00

Obligatorio conforme sumatoria
de bases imponibles de
Detalles.
Numérico Max 14
0.00

Opcional conforme sumatoria
de campos descuentos de
Detalles.
Numérico Max 14
00

Obligatorio, si
<codDocReembolso> es igual a
41.
Numérico Max 2
56.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la suma de
<totalBaseImponibleReembolso
> y <totalImpuestoReembolso>
Numérico Max 14
50.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la suma de
Numérico Max 14
ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
<BaseImponibleReembolso>
6.00

Obligatorio, si
<codDocReembolso> es igual a
41, corresponde a la sumatoria
de los tags
<impuestoReembolso>, el cual
es mayor o igual a la sumatoria.
Numérico Max 14
Obligatorio - -
Obligatorio - -

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 16 Numérico Max 2

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 17 Numérico Max 2

0.00 Opcional Numérico Max 14
50.00 Obligatorio Numérico Max 14

12 Obligatorio Numérico

Min 1 Max 4 / 2
enteros, 2
decimales
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -

56.00

Obligatorio corresponde a la
sumatoria de bases imponibles
e impuestos.
Numérico Max 14
moneda0 Obligatorio Alfanumérico Max 14
Obligatorio - -
Obligatorio - -

01 Obligatorio. Conforme tabla 24 de la Ficha Técnica Offline Numérico Max 2

56.00 Obligatorio Numérico Max 14
30 Obligatorio Numérico Max 14
unidadTiem Opcional Texto Max 10
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -
codigoPrincipal0 Obligatorio Alfanumérico Max 25
codigoAuxiliar0 Opcional Alfanumérico Max 25
descripcion0 Obligatorio Alfanumérico Max 300
unidadMedida0 Opcional Alfanumérico Max 50
1.000000 Obligatorio Numérico Max 14
50.000000 Obligatorio Numérico Max 14
0.00 Opcional Numérico Max 14

50.00 Obligatorio, debe multiplicar el campo precio por cantidad Numérico Max 14

Opcional - -
Opcional Alfanumérico Max 300
Opcional Alfanumérico Max 300
Opcional - -
Obligatorio - -

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO
Obligatorio - -

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 16 Numérico Max 2

2 Obligatoriode la Ficha Técnica Offline,^ conforme tabla 17 Numérico Max 2

12

Obligatorio cuando
< codDocReembolso> sea igual
a 41, conforme tabla 17 de la
Ficha Técnica Offline
Numérico
Min 1 Max 4 / 2
enteros, 2
decimales
50.00 Obligatorio Numérico Max 14
6.00 Obligatorio Numérico Max 14
Obligatorio - -
Obligatorio - -
Obligatorio - -
Obligatorio - -


Obligatorio cuando
<codDocReembolso> sea igual
a 41
- -

Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
04</tipoIdentificacionProveedorRee
mbolso>

Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 2
Validar código de tipo de
identificación conforme tabla 6
de la Ficha Técnica Offline
identificacionProvee</identificacionPro
veedorReembolso>
Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 20
Validar código de tipo de
identificación conforme tabla
26 de la Ficha Técnica Offline
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41 (^) Numérico Max 3
Validar de acuerdo tabla 25 de
la Ficha Técnica Offline
01
Obligatorio cuando
< codDocReembolso> sea igual
a 41, Validar con tabla 26 de la
Ficha Técnica Offline
Numérico Max 2
00
Obligatorio cuando
< codDocReembolso> sea igual
a 41, Validar tabla 3 de Ficha
Técnica
Numérico Max 3
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41, conforme tabla 4
Numérico Max 3
000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 3
000000000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 9
01/01/2000
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Fecha dd/mm/aaaa
0000000000</numeroautorizacionDocRee
mb>
Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 10, 37 ó 49

ETIQUETAS O TAGS CARACTER TIPO DE CAMPO LONGITUD / FORMATO

Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -

Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
2

Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 2
2

Obligatorio cuando
< codDocReembolso> sea igual
a 41, conforme tabla 17 de la
Ficha Técnica Offline
Numérico Max 2
12 Obligatorio, de la Ficha Técnica Offlineconforme tabla 17 Numérico

Min 1 Max 4 / 2
enteros, 2
decimales
50.00

Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 14
6.00

Obligatorio cuando
< codDocReembolso> sea igual
a 41
Numérico Max 14
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando
< codDocReembolso> sea igual
a 41
- -
Obligatorio cuando corresponda - -

SISPAU Obligatorio cuando corresponda Alfanumérico Min 1 Max 100

ABC1234 Obligatorio cuando corresponda Alfanumérico Min 1 Max 100

CGMC1405 Obligatorio cuando corresponda Alfanumérico Max 30

Obligatorio cuando corresponda - -

Opcional - -
campoAdicional0 Opcional Alfanumérico Max 300
campoAdicional1 Opcional Alfanumérico Max 300
Opcional - -
Obligatorio - -

ANEXO 18 – REQUISITOS OBLIGATORIOS DE
LLENADO EN LA FACTURA ELECTRÓNICA
POR LA ENTREGA DE FUNDAS PLÁSTICAS
Aplica para establecimientos de comercio con tres (3) o más establecimientos
abiertos y, al franquiciador y sus franquiciados, independientemente del número de
sus establecimientos que entreguen fundas o bolsas plásticas tipo acarreo o
camiseta al adquiriente o consumidor, para cargar o llevar los productos adquiridos.
En la emisión del comprobante de venta tipo factura, en la sección <detalles> para
el llenado de los campos <cantidad>, <codigoPrincipal> y <descripcion> se deberá
llenar el número, código y la descripción de las fundas plásticas gravadas con ICE,
como un producto adicional a los vendidos, conforme el siguiente detalle:
<cantidad> <codigoPrincipal> <descripcion> <precioUnitario>
Número de fundas o
bolsas plásticas tipo
camiseta o acarreo.

ICE-FPN- 01 Funda/bolsa plástica
0,00*
ICE-FPR- 02 Funda/bolsa plástica con rebaja 50% (aplicable para fundas biodegradables y compostables).
ICE-FPE- 03
Funda/bolsa plástica exenta (aplicable para fundas con
un mínimo de adición del 50% de materia prima
reciclada post consumo).
(*) Es importante recalcar que los agentes de percepción del ICE por concepto de
fundas plásticas no deberán establecer un precio de venta al público sugerido para
este bien, salvo que lo tuvieren.
El ICE corresponderá a la tarifa específica vigente multiplicada por la cantidad. (Ver
Tabla 18 – TARIFA DEL ICE).
El valor del ICE formará parte de la base imponible del IVA de conformidad con el
artículo 58 de la Ley de Régimen Tributario Interno.
Ejemplo de la estructura XML:
ANEXO 19 – APLICACIÓN DE LAS
AUTORETENCIONES
En el llenado del comprobante de retención que se emita por concepto de
autoretenciones de conformidad con la normativa correspondiente para cada caso,
se deberá considerar lo siguiente:

Código y porcentaje para llenar en el comprobante de retención
Código de
retención
Concepto retención en la fuente de Impuesto a la
Renta
Porcentaje de
retención
(Desde 01/04/2020)
350 Otras autoretenciones 1,50 ó 1,75
3481 Autorretenciones Sociedades Grandes Contribuyentes Varios porcentajes
El comprobante de retención se emite a nombre del mismo agente de retención,
esto es en el campo y
En cuanto al campo se considerará:
➢ En la versión 1.0 del comprobante de retención electrónico se utilizará el
código de documento 42 (Documento retención presuntiva y retención
emitida por propio vendedor o por intermediario. (Ver Ejemplo 1 a
continuación).
➢ En la versión 2.0 del comprobante de retención electrónico se utilizará el
código de documento 42 (Documento retención presuntiva y retención
emitida por propio vendedor o por intermediario y el código de sustento de la
operación 12 (Impuestos y retenciones presuntivos). (Ver Ejemplo 2 a
continuación).
En el campo ubicar el mismo número de comprobante de
retención por la autoretención que se está realizando.
Estas consideraciones aplican debido a que dicha retención no opera sobre compras
a terceros sino sobre sus propios ingresos.

Ejemplo 1 de la estructura XML – Comprobante de retención código 350:
Ejemplo 2 de la estructura XML – Comprobante de retención ATS versión
2.0.0 código 350:

Ejemplo 1 de la estructura XML – Comprobante de retención código 3 481 :

Ejemplo 2 de la estructura XML – Comprobante de retención ATS versión
2.0.0 código 3481:
^
ANEXO 20 – REQUISITO PARA LA
APLICACIÓN DE LA DEVOLUCIÓN
AUTOMÁTICA DEL IVA EN EL XML DE
FACTURAS, NOTAS DE CRÉDITO Y NOTAS
DE DÉBITO.
Las facturas, notas de crédito y notas de débito electrónicas deberá contener la
siguiente información en la estructura del XML, cuando aplique devolución del IVA,
cuyo valor deberá ser igual al autorizado por los servicios web – DIG, para el caso
de las notas de crédito deberá corresponder al valor que aplique al documento de
sustento:

Campo Devolución IVA en la cabecera del XML:
<totalConImpuestos>
<totalImpuesto>
<codigo>2</codigo>
<codigoPorcentaje>0</codigoPorcentaje>
<descuentoAdicional>0.00</descuentoAdicional>
<baseImponible>50.00</baseImponible>
<tarifa>12.00</tarifa>
<valor>6.00</valor>
<valorDevolucionIva>6.00</valorDevolucionIva>
</totalImpuesto>
Validaciones: Las validaciones en comprobantes electrónicos que se aplicarán al
campo son las siguientes:
➢ Tipo identificación del comprador o cliente según Tabla 6: Cédula (Código
05)
➢ Si el campo <valorDevolucionIva> es un valor mayor a cero, la clave de
acceso deberá estar registrada en el control de saldos del beneficiario y el
monto deberá ser igual al autorizado por el servicio web - DIG.
➢ El valor registrado en el campo <valorDevolucionIva> debe ser mayor o igual
a cero y menor o igual al campo <valor> de la misma sección. En caso de
que el campo se envíe con valor cero no aplica validación.
➢ Los campos que totalizan la factura <importeTotal>, nota de crédito
<valorModificacion> y débito <valorTotal>, deberán restar el valor
consignado en el campo <valorDevolucionIva>.
➢ Las facturas y notas de débito utilizarán los servicios web - DIG para el
registro del valor en el campo <valorDevolucionIva>.
ANEXO 21 – REQUISITO OBLIGATORIO PARA
COMPROBANTES ELECTRÓNICOS EMITIDOS
POR CONTRIBUYENTES DESIGNADOS COMO
AGENTES DE RETENCIÓN.
Los comprobantes de venta, retención y documentos complementarios electrónicos
deberán contener la leyenda Agente de Retención en la estructura del XML,
conforme las siguientes especificaciones:

Agente de retención
Nombre de la etiqueta: <agenteRetencion>
Formato: Numérico
Caracteres: Máximo 8
Contenido: Número de la resolución, omitiendo los ceros a la izquierda
Ubicación: Entre la etiqueta <regimenMicroempresas> y </infoTributaria>
Ejemplo 1 – Contribuyente designado Agente de Retención
Ejemplo 2 – Formato RIDE
Nota: Se incluirán únicamente las etiquetas que correspondan al contribuyente.

ANEXO 22 – REQUISITO OBLIGATORIO PARA
COMPROBANTES ELECTRÓNICOS EMITIDOS
POR CONTRIBUYENTES RIMPE.
Los comprobantes de venta, retención y documentos complementarios electrónicos
deberán contener la leyenda CONTRIBUYENTE RÉGIMEN RIMPE o
CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE, conforme las
siguientes especificaciones:

RIMPE
Nombre de la etiqueta: <contribuyenteRimpe>
Formato: Texto
Caracteres: 27 (Incluidos espacios)
Contenido: CONTRIBUYENTE RÉGIMEN RIMPE
Ubicación: Entre la etiqueta <agenteRetencion> y </infoTributaria>
Ejemplo 1 – Contribuyente RIMPE y Agente de Retención

Ejemplo 2 – Contribuyente RIMPE
Ejemplo 3 – Formato RIDE Contribuyente RIMPE
Negocio popular
Nombre de la etiqueta:
Formato: Texto
Caracteres: 45 (Incluidos espacios)
Contenido: CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE
Ubicación: Entre la etiqueta y

Ejemplo 4 – Contribuyente Negocio Popular
Ejemplo 5 – Formato RIDE Contribuyente Negocio Popular
ANEXO 23 – REQUISITO OBLIGATORIO EL
LLENADO PARA EL XML DE
COMPROBANTES DE VENTA EN LA
TRANSFERENCIA LOCAL DE MATERIALES
DE CONSTRUCCIÓN.
En la emisión de comprobantes por la transferencia local de materiales de
construcción establecidos en la Resolución No. NAC-DGERCGC24- 00000013 , en
la sección , en el campo se deberá colocar
obligatoriamente los siguientes códigos de manera exacta:

TABLA 3 1

<codigoAuxiliar> Subcategoría material de construcción
F010101 VARILLA LAMINADA CORRUGADA AS42 DE 8MM, 10MM Y 12MM DE DIÁMETRO
F010201 (^) ARCILLA
F010202 (^) ARENA
F010203 (^) CAL
F010204 (^) CALIZA
F010205 (^) PÉTROS

F010301 (^) HORMIGÓN PREMEZCLADO
F010401 (^) CEMENTO Y SUS DERIVADOS
F010402 (^) RESIDUO CEMENTO
F010501 (^) CHATARRA FERROSA
F010601 (^) MORTERS
F010701 (^) CLINKER
F010702 (^) PUZOLANA
F010703 (^) YESO
F010801 (^) ADOQUÍN
F010802 (^) BLOQUES
F010803 (^) LADRILLOS
F010804 (^) PRODUCTOS DE HORMIGÓN PREFABRICADO

ANEXO 24 – REQUISITO OBLIGATORIO PARA
COMPROBANTES ELECTRÓNICOS EMITIDOS
POR GRANDES CONTRIBUYENTES.
Los comprobantes de venta, notas de crédito y notas de débito electrónicos
deberán contener la leyenda “Gran Contribuyente” y el número de la resolución
mediante la cual fueron calificados como tal, en la estructura del XML, conforme las
siguientes especificaciones:

Gran Contribuyente
Nombre de la etiqueta: <campoadicional>
Formato: Alfanumérico
Caracteres: Máximo 300
Contenido: Leyenda “Gran Contribuyente” y número de resolución
Ubicación: Entre las etiquetas <infoAdicional> y </infoAdicional>
Ejemplo 1 – Contribuyente designado gran contribuyente
Ejemplo 2 – Formato RIDE
ANEXO 25 – REQUISITO OBLIGATORIO DE
LLENADO PARA EL XML DE FACTURAS
EMITIDAS POR LAS OPERADORAS DE
TRANSPORTE COMERCIAL (NO APLICA
PARA TAXIS Y SOCIOS O ACCIONISTAS DE
TAXIS).
De conformidad con el artículo 57 de la Ley de Transporte Terrestre Tránsito y
Seguridad Vial, el cual establece que los servicios de transporte comercial deben
ser prestados únicamente por operadoras de transporte terrestre autorizadas
para tal objeto, y el artículo 189 del Reglamento a la Ley de Régimen Tributario
Interno que señala que la contratación de servicios terrestre comercial, salvo los
prestados por taxis, será realizado únicamente por las operadoras
debidamente autorizadas por el organismo de tránsito competente.

La Circular No. NAC-DGECCGC24- 00000005 establece que, solamente los
servicios de transporte terrestre comercial que estén sustentados en
comprobantes de venta emitidos por las operadoras de transporte terrestre
comercial, excepto taxis, autorizadas por la autoridad competente, pueden
considerarse como deducibles para determinar la base imponible del impuesto a
la renta. Así mismo únicamente en estos casos, el servicio se encuentra gravado
con tarifa 0% del impuesto al valor agregado.

Requisito: En las facturas electrónicas emitidas por la prestación de servicios de
transporte comercial, excepto taxis, por parte de las operadoras de transporte
debidamente autorizadas, así como en aquellas emitidas por parte de sus socios o
accionistas, en la sección , para el llenado del campo

de las facturas electrónicas se deberá considerar la información conforme al
siguiente detalle:
TABLA 3 2
<codigoAuxiliar> Caso Observación^
H492001 (^) la operadora Facturas emitidas por al cliente
Aplica en las facturas emitidas
por la operadora de transporte
comercial (excepto taxis)
debidamente autorizada, a sus
clientes por la prestación de
servicio de transporte

H492002
Facturas emitidas por
el socio o accionista
a la operadora de
transporte
Aplica en las facturas emitidas
por el socio o accionista , a la
operadora de transporte por sus
servicios
Los códigos arriba detallados deberán incluirse en la factura electrónica en el campo
de cada ítem que corresponda a la actividad de transporte comercial.

Nota: Este requisito puede ser implementado desde su publicación en la Ficha Técnica
de Comprobantes Electrónicos, sin embargo, para efectos de ajustes en los sistemas
tecnológicos de los sujetos pasivos emisores de comprobantes electrónicos, este
requisito será obligatorio desde el 01 de noviembre de 2025.

15. Glosario de términos
ARCHIVOS PLANOS:
Son archivos que están compuestos únicamente por texto sin formato, sólo caracteres.

AMPERSAND (&):
El signo & (ampersand), deberá incorporarse en los comprobantes electrónicos de la
siguiente manera “&” caso contrario al solicitar la autorización se rechazará con
motivo de mal estructurado.

COMERCIO ELECTRÓNICO:
Es toda transacción comercial realizada en parte o en su totalidad, a través de redes
electrónicas de información.

DBF:
(Data Base File). Es la extensión que corresponde a un tipo de fichero de bases de
datos, originalmente utilizado por el SGBD Dbase, pero que es frecuente encontrar en
todo tipo de aplicaciones como el Lenguaje de Programación FOX PRO.

DOCUMENTO ELECTRÓNICO:
Es la emisión mediante mensaje de datos (documentos desmaterializados) de los
comprobantes de venta, retención y documentos complementarios.

ETIQUETAS O TAGS:
Etiqueta en lenguaje marcado. Es una marca con tipo que delimita una región en los
lenguajes basados en XML.

ESQUEMA OFFLINE:
En este esquema el número de autorización es la clave de acceso generada por el
emisor y los archivos XML contendrán únicamente la cave de acceso (49 dígitos).
Normativa: Resolución No. NAC-DGERCGC14-00790.

INTERFACES (Plural de interfaz):
En informática, es un elemento de conexión que facilita el intercambio de datos.
También se lo define como el conjunto de métodos para lograr interactividad entre un
usuario y una computadora.

LOG:
Registro oficial de eventos durante un rango de tiempo en particular. En seguridad
informática es usado para registrar datos o información sobre quién, qué, cuándo, dónde
y por qué un evento ocurre para un dispositivo en particular o aplicación.

MENSAJES DE DATOS:
Es toda información creada, generada, procesada, enviada, recibida, comunicada o
archivada por medios electrónicos, que puede ser intercambiada por cualquier medio.
Serán considerados como mensajes de datos, sin que esta enumeración limite su
definición, los siguientes documentos electrónicos, registros electrónicos, correo
electrónico, servicios web, telegrama, télex, fax e intercambio electrónico de datos.

MÓDULO:
Componente auto controlado de un sistema, dicho componente posee una interfaz bien
definida hacia otros componentes; algo es modular si está construido de manera tal que
se facilite su ensamblaje, acomodamiento flexible y reparación de sus componentes.

PASSWORD:
Clave de acceso. Es una forma de autentificación que utiliza información secreta para
controlar el acceso hacia algún recurso.

PKCS:
En criptografía, PKCS se refiere a un grupo de estándares de criptografía de clave
pública concebidos y publicados por los laboratorios de RSA en California.

RCVRYDC:
Reglamento de Comprobantes de Venta, Retención y Documentos Complementarios,
publicado en el Registro Oficial 247, del 30 de Julio de 2010 y sus reformas.

SERVICIO ELECTRÓNICO:
Es toda actividad realizada a través de redes electrónicas de información.

SGBD:

Siglas de Sistema Gestor de Base de Datos; programas que permiten almacenar y
posteriormente acceder a los datos de forma rápida y estructurada.

SISTEMA DE INFORMACIÓN:
Es todo dispositivo físico o lógico utilizado para crear, generar, enviar, recibir, procesar,
comunicar o almacenar, de cualquier forma, mensajes de datos.

USERNAME:
Nombre de usuario de un sistema computarizado que obedece a un perfil o roles
asignados por un Administrador.

UTF-8:
UTF-8 (8-bit Unicode Transformation Format) es un formato de codificación de
caracteres Unicode e ISO 10646 utilizando símbolos de longitud variable, capaz de
representar cualquier CARACTER Unicode.

WEB SERVICE:
Un servicio web (en inglés, Web service) es una pieza de software que utiliza un
conjunto de protocolos y estándares que sirven para intercambiar datos entre
aplicaciones. Distintas aplicaciones de software desarrolladas en lenguajes de
programaciones diferentes y ejecutadas sobre cualquier plataforma pueden utilizar los
servicios web para intercambiar datos en redes de ordenadores como Internet.

XAdES:
Firma electrónica avanzada XML. Es un conjunto de extensiones a las recomendaciones
XML-DSig haciéndolas adecuadas para la firma electrónica avanzada.

XML:
Siglas en inglés de EXtensible Markup Language (lenguaje de marcas extensible); es un
estándar para el intercambio de información estructurada entre diferentes plataformas.

XSD:
XML Schema es un lenguaje de esquema utilizado para describir la estructura y las
restricciones de los contenidos de los documentos XML de una forma muy precisa.

16. Preguntas técnicas frecuentes
Pregunta Solución
Firma inválida- El nodo comprobante
no está firmado.

Hay dos tipos de firmado: uno que firma el archivo completo y otro el
nodo especifico; se debe revisar el archivo XML y verificar que esté
firmado el nodo como en el siguiente ejemplo:
Firma inválida- La estructura de la
firma es incorrecta.

Se puede validar el firmado con herramientas auxiliares de validación,
como la herramienta XOLIDOSIGN. Link de descarga:
http://www.xolido.com/lang/productosxolidosign/xolidosignescritorio
/modulo/?refbol=xolidosign-escritorio&refsec=xolidosign-
escritorio_descargas
Firma inválida- La firma no
corresponde con el contenido del
documento.

Se puede validar el firmado con herramientas auxiliares de validación,
como la herramienta XOLIDOSIGN. Link de descarga:
http://www.xolido.com/lang/productosxolidosign/xolidosignescritorio
/modulo/?refbol=xolidosign-escritorio&refsec=xolidosign-
escritorio_descargas
Generalmente estos errores se deben a que en el documento existen
caracteres extraños, el contribuyente debe verificar en los campos de
descripción o tipo texto del XML.
Firma inválida- La fecha de la firma es
posterior a la actual.

Favor re-enviar todos los comprobantes que no fueron autorizados por
"[Firma inválida. La fecha contenida en la firma es posterior a la
actual]". Al respecto la fecha y hora de nuestros servidores están
configurados con un servidor NTP.
server 0.south-america.pool.ntp.org maxpoll 12
server 1.south-america.pool.ntp.org maxpoll 12
server 2.south-america.pool.ntp.org maxpoll 12
Firma inválida- No existe el RUC en el
certificado digital.

Se puede validar el firmado con herramientas auxiliares de validación,
como la herramienta XOLIDOSIGN. Link de descarga:
http://www.xolido.com/lang/productosxolidosign/xolidosignescritorio
/modulo/?refbol=xolidosign-escritorio&refsec=xolidosign-
escritorio_descargas
En la herramienta muestra el certificado con el que fue firmado el
archivo.
PASOS para validar:
Pregunta Solución

Seleccionar el archivo, clic en verificar.
Clic en el botón certificado.
Clic en detalles y luego en el tag que contiene el dato del RUC.
Pregunta Solución
Clave de acceso registrada. Revisar en la página web del SRI si la clave de acceso ya fue autorizada.

Secuencial registrado.

Es responsabilidad del emisor controlar la no generación de un mismo
secuencial para un mismo tipo de comprobante (cabe recordar que
estos casos debieron ser detectados y corregidos en el ambiente de
pruebas).
RUC no existe. El RUC ingresado en la identificación del receptor no consta en la base de RUC, esto se puede validar en la página Web del SRI.

No se pueden anular comprobantes.

Verificar que todos los datos ingresados para la anulación sean
correctos; debe coincidir con los datos del comprobante a anular, se
puede consultar en la página WEB del SRI o en Intranet en la opción
de Consultas.
Comprobantes no autorizados por
error en diferencias. Abrir el XML y revisar que todos los cálculos estén correctos.^

RUC clausurado. Validar si el RUC del emisor presenta alertas de Infracciones en la aplicación de RUC o consultar con el área de Infracciones.

Número de decimales en la estructura
del XML del comprobante.

Revisar el uso correcto de las versiones de los archivos XML:
Pueden utilizar dos decimales en la versión 1.0.0 y seis decimales en la
versión 1.1.0.
Pregunta Solución
Validar el estado del Comprobante. Revisar en las consultas públicas mediante el http://www.sri.gob.ec, el estado del comprobante. portal web^

Que quiere decir comprobante no
autorizado.

Un comprobante en estado no autorizado está atado a un mensaje de
rechazo, puede ser cualquiera de los errores detallados en esta ficha
técnica. Es importante notar que pueden existir varias respuestas en
estado no autorizado y una única respuesta en estado autorizado.
This is a offline tool, your data stays locally and is not send to any server!
Feedback & Bug Reports