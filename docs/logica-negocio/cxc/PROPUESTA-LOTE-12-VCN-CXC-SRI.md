# Lote 12 — CXC / SRI (VCN #6, #7, #8, #9, #18): medición y propuesta

*2026-11-25 · omen-saa-1 · SIN programar: espera el OK del árbitro (y del usuario en lo que toca al SRI de producción). Referencia leída en `F:\work\vcnBE` (3529fdb, 81d0834, 84f2c42, 8eb9bdf). La cola de emisión de VCN (`CBR.EMSN` y sus servicios) NO se porta: es una reescritura de la emisión; aquí va lo mínimo para SAA.*
*SQL de medición (solo lectura): `cxc/sql/omen1-01-medicion-lote12-vcn-cxc-sri.sql`. Módulo de otro equipo (cxc): se sigue su estándar y `ESTADO-CXP-CXC-TSR-RHH-SRI.md`. Sin nombres ni montos individuales.*

## Principio común: lo ya emitido no cambia
Ningún cambio toca comprobantes ya grabados, autorizados o enviados al SRI. Todo aplica a las emisiones nuevas. Lo ya afectado se mide con el SQL (bloques indicados) y, si hay casos, lo corrige el usuario con script autorizado, no el código.

## #9 TLS normal hacia el SRI (la medición que se pidió)
**Hallazgo.** `SriHttpUtil` instala un `X509ExtendedTrustManager` que no valida nada y un `HostnameVerifier` que devuelve `true`. Lo usan **16 llamadas** en los seis generadores (factura, nota de crédito, nota de débito, liquidación de compra, retención v1 y V2), `ConsultaSRIRest` y `SriAutorizacionServiceImpl` (CXP, descarga de recibidos). El comentario de la clase dice que se hizo porque el SRI «responde desde la IP 181.113.227.222» y el certificado lleva el nombre de dominio: ese problema es de conectarse **por IP**; el código solo usa los nombres `cel.sri.gob.ec` y `celcer.sri.gob.ec`.
**Medición (2026-11-25, apretón de manos TLS real contra los dos servidores, con SNI):**
| | producción `cel.sri.gob.ec` | pruebas `celcer.sri.gob.ec` |
|---|---|---|
| Certificado | CN = el host; SAN = solo ese nombre | CN = el host; SAN = solo ese nombre |
| Emisor | DigiCert Global G2 TLS RSA SHA256 2020 CA1 | el mismo |
| Raíz | DigiCert Global Root G2 | la misma |
| Vence | 2027-01-14 | 2027-02-10 |
| Cadena enviada por el servidor | hoja + intermedio (verificación OK) | igual |
**¿Los tiene el JDK?** La raíz `digicertglobalrootg2` está en el `cacerts` de **JDK 21.0.8** (`C:\Program Files\Java\jdk-21`, alias `digicertglobalrootg2 [jdk]`) y también en el de JDK 8u261: el TLS normal valida sin importar nada **si WildFly de producción usa el `cacerts` por defecto de un JDK reciente**. Lo que NO se pudo medir desde aquí: el JDK y el almacén de confianza del servidor de producción (si arranca con `-Djavax.net.ssl.trustStore=…` propio, o con un JDK sin esa raíz, el TLS normal romperá la emisión).
**Propuesta.**
- Cambiar el interior de `SriHttpUtil` (una sola clase; los 18 llamadores quedan igual): conexión HTTPS normal con la validación de certificado y de nombre del JDK. Se conserva la firma pública.
- Válvula transitoria (a decidir por el árbitro): propiedad `saa.sri.tlsPermisivo=true`, por defecto apagada, que reactiva el comportamiento anterior con un aviso fuerte en el log, para poder volver sin redesplegar si la verificación previa fallara en producción. Recomiendo quitarla en el siguiente ciclo.
**Antes de desplegar (lista de control, a correr en el servidor de WildFly de producción con el MISMO JDK):**
1. `keytool -list -cacerts -storepass changeit | grep -i digicertglobalrootg2` → debe aparecer `digicertglobalrootg2`. Si WildFly usa otro almacén: `-Djavax.net.ssl.trustStore` en su configuración; listar ese almacén igual.
2. Prueba de apretón de manos con ese JDK (`jshell`): `new java.net.URL("https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl").openStream().read()` y lo mismo con `celcer…`; no debe lanzar `SSLHandshakeException`.
3. Si falta la raíz: `keytool -importcert -alias digicertglobalrootg2 -file <raiz.pem> -cacerts -storepass changeit` y reiniciar WildFly. La raíz se baja del sitio de DigiCert, no del servidor del SRI.
4. Vigilar los vencimientos de las hojas (enero y febrero de 2027): se renuevan solas con el mismo emisor; solo falla si el SRI cambia de autoridad.
**Efecto sobre lo ya emitido:** ninguno. **Tamaño: chica** (1 clase + doc). **Riesgo: alto si el paso 1 o 2 no se hizo** (la emisión deja de comunicarse con el SRI).
Archivos: `ejb/cxc/util/SriHttpUtil.java` (+ este .md y `ESTADO-CXP-CXC-TSR-RHH-SRI.md`).

## #6 Secuencial con bloqueo
**Hallazgo.** Hay **seis copias** idénticas de `obtenerSecuencial` (Factura, NotaCredito, NotaDebito, LiquidacionCompra, Retencion, RetencionV2). Hacen `SELECT n FROM NumeracionPuntoEmision` sin bloqueo, leen `NUMACTUAL`, hacen `UPDATE … SET numActual = :leído + 1` y devuelven el valor leído. Con dos emisiones del mismo punto y tipo a la vez, las dos leen el mismo número: el `UPDATE` de la segunda espera la fila de la primera, pero escribe `leído + 1` (otra vez el mismo): **dos comprobantes con el mismo secuencial y el contador avanza una sola vez**. (El contador es «el próximo a usar»: se devuelve el valor previo; no se cambia esa semántica, ver `lap1-01-diagnostico-secuencial-comprobantes.sql`.)
**Lo que NO hay que hacer:** la cola de VCN (nuevas tablas y estados). **Propuesta mínima:** un servicio nuevo `NumeracionPuntoEmisionService.siguienteSecuencial(idPunto, tipoDoc)` (REQUIRED: se une a la transacción de la emisión, que ya es `REQUIRES_NEW`) con `SELECT … FOR UPDATE` (`LockModeType.PESSIMISTIC_WRITE`, con tiempo de espera de la consulta) de la fila de `CBR.NXPE`, lee el valor ya confirmado, escribe `+1` y devuelve el previo formateado `%09d`. Las seis copias llaman al servicio. No agrega bloqueo nuevo: hoy el `UPDATE` ya retiene la fila hasta el commit de la emisión (que incluye las llamadas al SRI); el `FOR UPDATE` solo adelanta la espera al `SELECT`. Si una emisión falla y se deshace, el número vuelve (igual que hoy).
**Descartado:** una secuencia Oracle por punto y tipo (DDL nueva, la numeración se edita desde pantalla y una secuencia deja huecos al deshacer). **Alternativa de defensa:** un índice único `(PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL)` en cada tabla, solo si el bloque 6a del SQL sale en 0.
**Efecto sobre lo ya emitido:** ninguno. El SQL 6a lista repetidos ya existentes y 6b los contadores atrasados (un contador menor que `MAX(secuencial)+1` repetiría un número). Si 6b muestra atrasados, corregir el contador es un UPDATE autorizado, no del código.
**Tamaño: media.** Archivos: `NumeracionPuntoEmisionService.java` y `…ServiceImpl.java` (+ el DAO si hace falta una consulta con bloqueo), y los seis `*ServiceImpl` de comprobantes (quitar la copia, llamar al servicio). **Riesgo: medio-alto** (núcleo de la emisión). Prueba: dos emisiones simultáneas del mismo punto sobre una copia, esperado: dos números distintos y contador +2.

## #8 Estados del SRI
**Hallazgo (en los seis generadores, 12 sitios).**
- Si la autorización (WS2) no responde `AUTORIZADO`, se trata como rechazo: `ESTADO = 6` y `ESTADOEMISION = 2`. Una respuesta «en proceso», o una respuesta sin `<autorizacion>` (estado vacío), también queda como NO AUTORIZADO aunque el SRI termine autorizándola después.
- Si la recepción (WS1) dice «CLAVE ACCESO REGISTRADA» (el SRI ya la tenía) se salta WS2 y se marca autorizada con `autorizacion = clave` y **fecha de autorización inventada** (`fecha del documento + 1 min 15 s`, `plusMinutes(1).plusSeconds(15)`).
**Propuesta mínima (sin cola).**
1. Un clasificador único (`ClasificacionEstadoSri`): `AUTORIZADO`; `RECHAZADO` solo cuando el SRI dice explícitamente `NO AUTORIZADO` (o la recepción lo devuelve); `PENDIENTE` para todo lo intermedio, vacío o desconocido.
2. `PENDIENTE` NO marca 6: el comprobante queda `ESTADO = 4` (enviada) y `ESTADOEMISION = 1`, la respuesta dice «pendiente de autorización: el SRI aún lo procesa» y se completa con la recuperación que **ya existe en los seis** (`consultarYActualizar…`, que consulta el SRI y, si autoriza, marca, genera el asiento y envía el correo). Dentro de la emisión, hasta 3 consultas más a la autorización con espera creciente (2, 4 y 8 s) antes de dar el resultado.
3. «CLAVE ACCESO REGISTRADA»: se quita el atajo y sigue al WS2 normal; la autorización y la fecha salen del SRI. Si WS2 todavía no la tiene, queda `PENDIENTE` como en el punto 2. Se quita la fecha inventada de los 12 sitios.
**Efecto sobre lo ya emitido:** nada se reescribe. El SQL 8a/8b lista los NO AUTORIZADOS de los últimos 90 días de producción (con su clave) para que el usuario los consulte en el SRI; 8c lista los autorizados con la fecha inventada. Corregir esos casos es manual (consultar → `consultarYActualizar…`).
**Tamaño: media-grande** (seis servicios, dos caminos cada uno). Archivos: los seis `*ServiceImpl` de cxc + una clase nueva `ClasificacionEstadoSri`. **Riesgo: alto** (toca el estado fiscal del comprobante): probar contra el ambiente de pruebas del SRI con un comprobante real y con uno repetido (clave registrada).

## #7 La retención la calcula el servidor con la tarifa de TSRI
**Hallazgo.** `RetencionV2ServiceImpl` escribe en el XML la base, el porcentaje y el valor tal como llegan del navegador (`det.getBaseImponible()`, `det.getPorcentajeReten()`, `det.getValorReten()`, líneas ~484 y ~1504/~1834 al copiar el detalle). No se calcula ni se comprueba nada. `CBR.TSRI` guarda `CODIGO`, `PORCENTAJE`, `VALOR` y `ESTADO`; **no tiene columnas de vigencia** (a diferencia de la copia de VCN, que versiona las tarifas), y `LSRI.TABLA` dice a qué tabla del SRI pertenece cada tarifa (608 renta, 20 IVA).
**Decisión que hace falta del árbitro / usuario: ¿cómo se resuelve «a la fecha del documento» sin vigencia?**
- **Opción A (recomendada, sin DDL):** usar la tarifa **activa** (`ESTADO = 1`) de la tabla 608 o 20 para el código recibido. No hay historia: un comprobante con fecha pasada usa la tarifa de hoy. Solo es seguro si el bloque 7b da 0 (ningún código con dos tarifas activas).
- **Opción B (con DDL, aparte):** columnas `vigencia desde/hasta` en `TSRI` y consulta por fecha del documento; es la regla completa de VCN, pero toca el esquema y la carga de tarifas.
**Propuesta (opción A) y qué hacer si el servidor calcula distinto del navegador.** En la emisión de la retención V2 (el único camino que arma el XML), para cada línea: buscar la tarifa por código y tabla (renta o IVA según `codImpuesto`), `valor = round(base × porcentaje / 100, 2)`. Reglas:
- Código inexistente o sin tarifa activa → se rechaza la emisión (400) con el texto de la línea.
- Si el porcentaje del navegador **difiere** del de la tarifa → **se rechaza** (400, con la lista de líneas y los dos porcentajes): un porcentaje distinto significa que el usuario eligió otra tarifa o el catálogo cambió, y retener por un valor distinto del que vio y aprobó sería peor que fallar.
- Si el porcentaje coincide y solo el **valor** difiere hasta un centavo (redondeo) → **manda el servidor** (se reemplaza el valor y se anota en el log); si difiere más de un centavo → se rechaza.
- La base nunca la corrige el servidor (es del documento); solo se valida que sea mayor que cero.
**Efecto sobre lo ya emitido:** ninguno (solo emisiones nuevas). El SQL 7c lista las ya emitidas cuyo valor no es base × porcentaje o cuyo porcentaje no es el de la tarifa activa, solo informativo. Dudoso 7 del informe (el código de 2,75 % cargado como 3 %): se resuelve en datos de `TSRI` por el contador, no por este cambio.
**Tamaño: media.** Archivos: `RetencionV2ServiceImpl.java` (+ la consulta de tarifa en un DAO o `TarifaSriService` nuevo de lectura). La retención v1 (`RetencionServiceImpl`) sigue activa vía `RetencionRest` y tiene el mismo defecto: se propone el mismo cálculo y se confirma con el árbitro si está en el alcance. **Riesgo: medio-alto** (fiscal). Prueba: emitir en pruebas con una tarifa conocida, con un porcentaje alterado (400) y con un valor con un centavo de diferencia (manda el servidor).

## #18 Razón social cuando el nombre está vacío
**Hallazgo.** `titular.getNombre()` va directo al XML (`razonSocialComprador`, `razonSocialProveedor`, `razonSocialSujetoRetenido`) y a los mensajes en los seis generadores (~5 sitios cada uno) más `AnticipoClienteServiceImpl`, `AplicacionPagoCxcServiceImpl` y `ProcesoCargaDocumentosServiceImpl` (CXP) en mensajes. Un titular con solo razón social sale con la razón social vacía y el SRI rechaza el comprobante; los mensajes dicen «proveedor 'null'».
**Propuesta.** Clase `NombreTitular.de(Titular)` (el nombre; si está vacío, la razón social; `null` si ninguno), como la de VCN, en `ejb/cxc/util`. Reemplazar `getNombre()` por `NombreTitular.de(...)` en XML, RIDE y mensajes de los seis generadores y en los tres servicios con mensajes. Un titular sin nombre ni razón social falla con un mensaje claro (400) antes de armar el XML, no con un rechazo del SRI.
**Efecto sobre lo ya emitido:** ninguno. SQL 18a/18b cuenta los titulares afectados.
**Tamaño: media** (muchos sitios, cada uno una línea). Riesgo: bajo.

## Orden sugerido
#18 (bajo riesgo) → #6 → #7 → #8 → #9 al final, y #9 solo después de la lista de control en el servidor de producción.

## Pruebas (no hay tests automáticos)
En ambiente de pruebas del SRI (`celcer`): una emisión de cada tipo; dos emisiones simultáneas del mismo punto; una retención con tarifa alterada; una clave ya registrada; una respuesta en proceso (si el SRI de pruebas la produce, o simulada con un servicio falso).

## Qué necesito del árbitro
1. OK a las propuestas, y la decisión del **#7**: opción A (tarifa activa, sin vigencia) o B (DDL de vigencia), y si la retención v1 entra.
2. **#9:** si va con la válvula `saa.sri.tlsPermisivo` y quién corre la lista de control en producción antes del despliegue.
3. Quién corre el SQL `omen1-01` en producción antes de programar (6a, 6b, 7b y 8b pueden mostrar casos que cambien el orden o el alcance).

---

## ESTADO (2026-11-25): #18, #6 y #8 PROGRAMADOS; #7 en pausa; #9 al final

### Medición en producción (omen1-01) y decisiones
- 6a duplicados: 0. **6b: contador de notas de crédito (punto 1, tipo 04) atrasado** (contador 3, máximo emitido 200): script `sql/omen1-02-corrige-contador-nota-credito.sql` (respaldo de la fila, UPDATE a máximo + 1, controles, sin COMMIT). **Correrlo antes de emitir otra NC.** Cómo pudo quedar atrás (hipótesis, no verificada): el contador es UNA fila por punto y tipo, sin ambiente; si al pasar a producción se reinició a un valor bajo mientras las NC de pruebas (ambiente 1) llegaron a 200, o si hubo NC cargadas por migración/otro camino sin pasar por `obtenerSecuencial`, el contador quedó por debajo del máximo. La consulta 0 del script muestra el máximo por ambiente para distinguirlo; el script usa el máximo de cualquier ambiente (un hueco es inofensivo, un repetido no).
- 8b: 0 no autorizadas en 90 días. 8c: 1 comprobante (retención V2, autorizada a las 05:01:15 sobre su fecha) con fecha de autorización inventada: **consultarlo en el SRI; no se toca.**
- #18: 0 titulares afectados hoy; corrección preventiva.
- **#7 EN PAUSA.** La tabla 20 (IVA) no tiene tarifas en TSRI y el cruce del bloque de recálculo toma los códigos de la tabla 608 (renta): con la opción A tal cual, el servidor rechazaría las retenciones de IVA. Opciones: (1) sembrar la tabla 20 con los porcentajes de retención de IVA vigentes del SRI y su código de anexo, en script aparte validado por el contador; (2) validar SOLO renta (608) y dejar el IVA como hoy. Preferencia del árbitro: (2) ahora, (1) cuando exista la semilla.

### Programado
- **#18** `cxc/util/NombreTitular` (`de` = nombre, si vacío razón social; `requerido` falla antes de armar el XML si no hay ninguno). Usado en el XML (razón social del comprador/proveedor/sujeto retenido) de los seis generadores y en RIDE/correo/mensajes de ellos, `AnticipoClienteServiceImpl`, `AplicacionPagoCxcServiceImpl` y `ProcesoCargaDocumentosServiceImpl`.
- **#6** `SecuencialComprobanteService.siguienteSecuencial(punto, tipo)`: `SELECT … FOR UPDATE` (espera 30 s) de la fila de CBR.NXPE, relee con el bloqueo, suma 1 y devuelve el previo, con el mismo contrato de antes. Las seis copias privadas de `obtenerSecuencial` ahora delegan. REQUIRED: se une a la transacción de la emisión; si la emisión falla el número no se consume.
- **#8** `cxc/util/EstadoSri` (AUTORIZADO / RECHAZADO = «NO AUTORIZADO» expreso / PENDIENTE = vacío, sin `<autorizacion>` o desconocido). En los seis servicios: WS2 con hasta 3 reintentos (2, 4 y 8 s) en las rutas de emisión y reenvío (`llamarAutorizacionConReintentos`); PENDIENTE ya no pasa a ESTADO 6/EstadoEmision 2: queda en 4, devuelve `estado = PENDIENTE` y se completa con la consulta `consultarYActualizar…` que ya existía; «CLAVE ACCESO REGISTRADA» ya no se marca autorizada con fecha inventada: en la emisión sigue al WS2 normal y en el reenvío pide consultar el estado. `FacturaRest` trata PENDIENTE como NO_AUTORIZADO en el código HTTP (200, exito=false).
  - **No cubierto a propósito:** los `catch` que ante una excepción de red al llamar al WS2 dejan ESTADO 6 (siguen igual); en la RetencionV2 y la retención v1 solo se cubrió el flujo con `ra`/`resultado` (ver pruebas).

### Pruebas en COPIA (obligatorias antes de producción)
**#6 (secuencial)**
1. Dos emisiones simultáneas del mismo punto y tipo (dos hilos/pestañas, factura): números distintos y consecutivos, contador = último + 1, ningún error 500.
2. Una emisión que falla después de pedir el secuencial (forzar error al firmar): el contador NO avanza (rollback) y la siguiente emisión usa el mismo número.
3. Punto/tipo sin fila en NXPE: error claro «No existe numeración…». Repetir para NC, ND, liquidación, retención v1 y V2.
4. Emisión con la fila bloqueada por otra sesión > 30 s: falla con tiempo de espera, sin dejar comprobante a medias.

**#8 (estados)** contra el ambiente de pruebas del SRI (celcer) con un comprobante real
1. Emisión normal: AUTORIZADO, estado 5, fecha y número de autorización reales.
2. Comprobante ya enviado (clave repetida): no queda autorizado al instante con fecha inventada; toma la autorización y fecha reales del WS2 (emisión) o pide consultar el estado (reenvío).
3. Simular respuesta sin `<autorizacion>` (apuntar a un doble que devuelva vacío): 3 reintentos, queda en estado 4 con `estado = PENDIENTE`, no 6; luego `consultarYActualizar…` lo pasa a autorizado cuando el SRI responde.
4. Respuesta «NO AUTORIZADO» expresa: estado 6 como antes, con el mensaje del SRI.
5. Repetir 1–4 en factura, NC, ND, liquidación, retención v1 y V2 (los seis tienen su copia).

**#18** cliente solo con razón social: el XML y el RIDE salen con ella; titular sin nombre ni razón social: falla con mensaje claro antes de firmar.

---

## ESTADO (parte 2, 2026-11-25): #8 (red), #7 (solo renta) y #9 PROGRAMADOS

### #8 — errores de red
Los `catch` que ante una excepción de red al llamar al WS2 dejaban ESTADO 6 (reenvío de los seis servicios) ya no tocan el estado: el comprobante queda como estaba (pendiente) y se completa con la consulta de estado. Solo un «NO AUTORIZADO» expreso del SRI pasa a 6.

### #7 — retención de renta con la tarifa del servidor (solo renta)
`TarifaRetencionService.validarRenta` (nuevo), llamado al inicio de `procesarRetencionV2Completa` (V2) y de `emitirRetencionAnteSRI` (v1), antes de grabar nada. Solo líneas con código de impuesto 1 (renta): tarifa ACTIVA de `CBR.TSRI` con `LSRI.TABLA = '608'` y `CODIGO = codRetencion`. Reglas aprobadas: sin tarifa activa o con varias → se rechaza; porcentaje distinto de la tarifa → se rechaza; valor que difiere de `round(base × % / 100, 2)` hasta 1 centavo → manda el servidor (se reemplaza y se anota en el log); más de 1 centavo → se rechaza; base ≤ 0 → se rechaza; la base nunca se corrige. Se listan TODAS las líneas rechazadas; etapa `VALIDACION_RETENCION` (422 en `RetencionV2Rest`). **IVA (código 2) e ISD quedan como hoy** hasta que el contador valide la semilla de la tabla 20. Sin historia de vigencia: un documento con fecha pasada usa la tarifa de hoy. Precondición ya medida: 7b = 0 códigos con dos tarifas activas.
**Pruebas en copia:** (1) código de renta con tarifa conocida y valores exactos: emite igual que antes; (2) porcentaje alterado: 422 con la línea; (3) valor con 1 centavo de diferencia: emite con el valor del servidor; (4) valor con más de 1 centavo: 422; (5) código sin tarifa activa: 422; (6) línea de IVA con cualquier porcentaje: pasa como hoy; (7) lo mismo en la retención v1.

### #9 — TLS normal hacia el SRI
`SriHttpUtil` ya no acepta cualquier certificado ni cualquier nombre: usa la validación normal de la JVM. **Válvula transitoria** `-Dsaa.sri.tlsPermisivo=true` (apagada por defecto) vuelve al comportamiento anterior y escribe un AVISO fuerte en cada conexión; se retira en el ciclo siguiente.

**Lista de control en producción (la corre el usuario ANTES de desplegar), con el JDK del WildFly:**
```
:: 1) ¿Qué JDK usa WildFly? (mirar JAVA_HOME en standalone.conf.bat / el servicio) y si tiene truststore propio:
::    buscar -Djavax.net.ssl.trustStore en standalone.conf.bat / standalone.xml  (si aparece, ese es el almacén a revisar)
:: 2) ¿Está la raíz en el almacén por defecto?
"%JAVA_HOME%\bin\keytool" -list -cacerts -storepass changeit | findstr /i digicertglobalrootg2
:: 3) Prueba real del handshake a las dos URLs con ese mismo JDK (debe imprimir 200/405/500 y NO una excepción de certificado):
echo var u=new java.net.URL("https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"); var c=(java.net.HttpURLConnection)u.openConnection(); System.out.println(c.getResponseCode()); /exit | "%JAVA_HOME%\bin\jshell" -
echo var u=new java.net.URL("https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"); var c=(java.net.HttpURLConnection)u.openConnection(); System.out.println(c.getResponseCode()); /exit | "%JAVA_HOME%\bin\jshell" -
:: 4) Si el paso 2 no encuentra la raíz: importarla (descargar «DigiCert Global Root G2» de digicert.com/kb/digicert-root-certificates.htm como .pem) y:
"%JAVA_HOME%\bin\keytool" -importcert -trustcacerts -cacerts -storepass changeit -alias digicertglobalrootg2 -file DigiCertGlobalRootG2.pem
::    (si WildFly usa un truststore propio, importar en ese archivo con -keystore <ruta>). Reiniciar WildFly y repetir el paso 3.
:: 5) Si algo falla y hay que emitir YA: añadir -Dsaa.sri.tlsPermisivo=true a JAVA_OPTS y reiniciar (válvula), y avisar para retirarla.
```
**Prueba en copia:** emisión contra celcer con TLS normal (debe autorizar); con la raíz fuera del almacén de prueba debe fallar con error de certificado; con la válvula encendida debe emitir y dejar el aviso en el log.
