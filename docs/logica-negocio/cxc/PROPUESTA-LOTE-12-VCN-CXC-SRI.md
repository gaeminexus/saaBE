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
