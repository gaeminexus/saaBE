# ESTADO — equipo `omen-saa-3` (sesión iniciada el 2026-09-28)

**Árbitro:** `omen-saa-3-arb` (OMEN) · **Agentes:** `omen-saa-3-be`, `omen-saa-3-fe` · **Marcador de commit:** `omen3` · **Scripts:** prefijo `e3-`
**Alcance dado por el usuario:** rhh, cxp, pagos, cnt, tsr, cxc, crd. **NO TOCAR:** ninguno — el usuario autorizó tocar cualquier módulo (2026-10-27). Igual: `git status` + `git log -3` antes de tocar un archivo de otro equipo.
**Lo mantiene SOLO este equipo.** No confundir con `ESTADO-CXP-CXC-TSR-RHH-SRI.md`, que es del `omen-saa-3` anterior (relevado el 2026-09-01) y está congelado al 2026-08-31.

> ⚠️ **Convivencia:** `omen-saa-2` estaba activo el 2026-09-28 sobre rhh/cxp/cxc/tsr/pagos/cnt/sri, y
> `omen-saa-1` es dueño de crd. Antes de tocar un archivo: `git status` + `git log -3` sobre él.

## Frentes

| # | Frente | Estado | Commits |
|---|---|---|---|
| 1 | rhh — crear un colaborador fallaba (estado 'A' en un Long) y la lista marcaba «Inactivo» a todos | ✅ FE entregado | saaFE `b4fa930` |
| 2 | rhh — vacaciones: 14 activos sin saldo 2026 → **modalidad parametrizable, ASOPREP en devengo mensual 1,25/mes** | ✅ BE+FE entregados; e3-07 corrido el 2026-10-05 (en DBeaver hizo falta la variante sin PL/SQL). **Falta:** reparar las 6 solicitudes de agosto sin consumir, y acreditar al 30/09 | `fa587127`, `943b08c2`, `de5767db`, `b4a0e3eb`, saaFE `c12c07a` |
| 3 | rhh — crear un contrato fallaba con ORA-02290 (estado y fecha de registro nulos) | ✅ BE+FE entregados; espera despliegue | `9ead911b`, saaFE `ece5e22` |
| 4 | rhh — archivo bancario de la nómina: formato del Internacional y código BCE en vez del nombre del banco | ✅ SQL corrido por el usuario (e3-04), BE entregado; espera despliegue y revisión del archivo | `40df8e87`, `806d9c97` |
| 5 | Documento comercial del SAA para otros fondos | ✅ entregado (Claude Docs) | — |
| 6 | rhh — liquidaciones de ex-colaboradores de la administración anterior | ✅ BE+FE, revisado con el documento de Contabilidad (cuenta por concepto, asiento en RRHH al confirmar el pago); e3-05 y e3-10 corridos. **Falta del contador:** cuentas de aporte personal, retención, FR, utilidades, salario digno | `87c4ffd4`, `048d97b3`, saaFE `6e48984`, `0b17f8a`, `5999015` |
| 7 | rol de pagos: fondo de reserva acumulado en el IESS como ingreso y egreso | ✅ entregado (solo reporte) | `1c2a1b87` |
| 8 | rhh — nómina pagada por empleado desde Tesorería, como jubilados | ✅ BE+FE. **Falta:** que el contador apunte el producto NOMINA a Sueldos por pagar; decisión sobre cerrar periodo con rechazados sin reenviar | `10b0dcb9`, `04215778`, saaFE `7def402` |
| 9 | Página comercial de SAA para gaeminexus.com | ✅ prototipo y `index.html` entregados; publicar es del usuario | — |
| 11 | rpr — provisiones del G48 y del resumen financiero (CCPM): un campo sale siempre en 0 | 🔵 diagnosticado en código; e3-11 (solo lectura) para medir. Toca `rpr`/crd, territorio de `omen-saa-1` (CCPM lo rediseña `laptop1`, `8ee56349`): **coordinar antes de despachar** | e3-11 |
| 10 | regulatorio — `CAMBIOS-SAA.md` (cuadre con la Superintendencia de Bancos) | 🔵 C1 entregado (periodos ordenados por fecha), C6 inventariado. **Bloqueado:** carpeta `cuadreSuper` y coordinación con `omen-saa-1` para C3/C4/C6/C8. Plan en `docs/regulatorio/PLAN-CAMBIOS-SAA-OMEN3.md` | `d88ba7e2`, saaFE `b448367`, e3-08/e3-09 |
| 12 | infra — ambientes de prueba en los servidores del cliente | 🔵 `.2.4` listo (ASOPREP CUADRE, 8180, base 1531); `.2.3` clonando. **Falta:** interruptor de correo (no facturar en pruebas), `saa-uploads`. Bitácora en `docs/infra/AMBIENTES-PRUEBA-SERVIDORES-CLIENTE.md` | `e409ef62`, `6801d9ef`, `fa8d0d7c`, `704ec2de` |
| 13 | rhh — rol de pagos consolidado: el pie no totalizaba sueldo base, aporte personal ni retención de IR | ✅ entregado, fill OK; espera despliegue | `28bd5601` |
| 14 | **producción — ORA-04036 (PGA) al confirmar el pago 769**: SELECT de 431 LEFT JOIN (254 de empresa/usuario y jerarquía) al cargar ANTE→PGTR | 🔵 `hibernate.max_fetch_depth=2` (global, relaciones siguen EAGER). El usuario lo sube **directo al .4 sin pasar por pruebas** (2026-10-19). **Falta:** su reporte; vigilar lentitud en procesos por lotes y `Unable to find … with id` | `04cecc37` |

| 16 | rhh — vacaciones (2026-10-27): 14 de 21 activos sin SLDV 2026 (única corrida: 27-08, CONTABILIDAD, corte 26-08, modo aniversario). **Usuario corre «Acreditar vacaciones» al 30-09-2026.** Aprobado: bitácora de corridas (e3-12), `GET /sldv/estadoAcreditacion`, alerta real + botón «Acreditar ahora», y acreditación automática al cerrar nómina (una llamada REQUIRES_NEW en `cerrarPeriodo`, archivo de omen2) | ✅ BE+FE entregados. **Orden:** correr `e3-12` (BLOQUE 1 o 1-B en DBeaver) ANTES del WAR. La automática va en `PeriodoNominaRest.cerrar` después del commit del cierre (no en `cerrarPeriodo`: `valorDia` solo ve periodos cerrados confirmados); en modalidad aniversario la automática queda OMITIDA (duplicaría la apertura) | ver commits del 2026-10-27 |
| 17 | rhh — LQEX, pedido de Ana (22-10, vía omen2): combo «tipo» con búsqueda y cuenta automática cuando el tipo tiene una sola sugerida (tipos 1-4 tienen dos: espera al contador) | ✅ FE entregado | saaFE `1d75411` |
| 18 | rhh — novedades IESS: registrar a mano fallaba con «Not able to deserialize data provided» (el FE mandaba `usuario`, la entidad tiene `usuarioRegistro`); combo Colaborador mostraba cesantes y se veía partido | ✅ FE: arreglo `a4e73ae`; combo: activos + licencia/suspendidos, cesante solo con salida en los últimos 60 días sin AVISO_DE_SALIDA, opción en una línea (opt-in en `app-campo-formulario`) | saaFE `a4e73ae` |
| 19 | rhh — un anticipo seguía «En descuento» aunque ya se había cobrado con un descuento recurrente manual (sin enlace al anticipo) | ✅ `e3-13` corrido el 2026-10-27: anticipo 41 CANCELADO, DSRC 41 ANULADO (no tenía cuotas). Sin explicar: por qué el DSRC del anticipo nació con 400 y sin cuotas — investigar si se repite | `20f80b13` |
| 20 | rhh — **la nómina pagaba 30 días de sueldo MÁS las vacaciones pagadas** (la aprobación crea la NVNM concepto alterno 12 y `calculaDiasTrabajados` no la restaba); eso además disparaba una «variación por extras» IESS falsa | ✅ BE: el sueldo usa días − días de vacación; NMNADITR, fondos de reserva y décimo cuarto siguen con días de servicio; el recálculo anula la VARIACION_POR_EXTRAS PENDIENTE que ya no corresponde. Desplegado y octubre recalculado por el usuario el 2026-10-27/28 (resultado no reportado al árbitro) | `b1d467d7` |
| 21 | rhh — menú: «Horas extra» sale de Procesos y queda bajo «Importación de marcaciones» (que pasa a grupo con «Importar») | ✅ FE + `e3-14` (PJRQ 1526 → padre 1524 nivel 5; 1538 se borra si nadie lo tenía) corrido por el usuario (salida del bloque 2 no reportada) | saaFE `0e0fab3`, `12df3573` |
| 22 | Informe DELTA 21 consolidado (3 documentos → formato del «3. Informe Revisión Sistema DELTA21») | ✅ entregado por SendUserFile, **fechado 26-oct-2026 v2.0**, citando las fechas originales. Se NEGÓ fecharlo 10-oct-2025 (antedatar un documento para un auditor). Generador fuera del repo | — |
| 15 | Documentación técnica para la auditoría (inventario, arquitectura, módulos, usuarios) | 🔵 Claude Docs + `docs/regulatorio/DOCUMENTACION-TECNICA-SAA-AUDITORIA.docx`; servidores levantados el 2026-10-25 con `docs/infra/auditoria/levantamiento-servidor.ps1`. **Falta:** [Completar] de la entidad y decisión del usuario sobre la sección 5.6 (brechas) | `f531db16` |

## Hallazgos (lo que costaba ver)

- **Levantamiento de servidores (2026-10-25), corrige lo que decían los docs:** los dos son **Dell PowerEdge R360, Xeon 6353P (8n/16h), 32 GB, PERC H355**, no R350/E-2314 como decía el memorando de septiembre. El .4 se llama **SRVDEV2** (aunque es producción) y el .3 SRVDEV1, ambos en el dominio `asoprep.com.ec`. La base es **Oracle AI Database 26ai Free 23.26.0** en producción (23.26.3 en el .3), no «23ai».
- **La clave del usuario SCP (el del datasource) vence el 2027-03-07** (perfil DEFAULT, 180 días): si vence, el SAA deja de conectarse. Todos los esquemas y SCP tienen rol **DBA**.
- **Las claves de los usuarios del SAA se guardan con codificación reversible** en `SCP.PDCN` característica 30 (ADMIN y JAGILA tienen el mismo valor; «CRUZ…» casi en claro). El script v1 las imprimió: corregido para excluir la 30; los `.txt` del 2026-10-25 son sensibles.
- **No hay respaldo programado** en ningún servidor (sin Windows Server Backup ni tareas). Docker Desktop corre en la sesión del usuario (servicio en Manual).
- **Permisos del SAA en producción: 0 de 305 cargados y el interruptor en 0**: todo usuario activo ve todo. 21 cuentas (19 activas), con genéricas ADMIN, TEST y USUARIO 1; el aprobador de CxP es el nodo raíz y el usuario de caja es USUARIO 1.
- **En el servidor de producción hay software ajeno al SAA**: WampServer (Apache 80/8085, MySQL 3306/3307, tarea «Gestor_Documental») y Oracle XE 21c (detenido). El firewall de Windows abre 1521/8080/3389 desde cualquier origen; RDP sin NLA; SRVDEV2 sin parches desde julio de 2026.

- **DBeaver se saltó en silencio un bloque PL/SQL** (`e3-07`, con un comentario `q'[...]'`) y el control posterior salió «vacío» sin dar error. Antes de dar por corrido un script, mirar que el control traiga las filas esperadas. Ante la duda, entregar la variante con sentencias sueltas.
- **Los periodos contables se ordenaban por `PRDOCDGO`**, no por fecha. Insertar meses anteriores (2025 del cuadre) los habría puesto «después» de 2026 y roto mayorización, saldos bancarios y balances. Además, **un periodo MAYORIZADO no bloquea asientos nuevos**: el control es código muerto (diagnosticado, sin activar, decide el usuario).
- **Las 6 solicitudes de vacaciones aprobadas en agosto nunca se descontaron del saldo** (`SLDVUSDO` en 0 para todos): 39 días inflados.
- **Beneficios sociales contabiliza el banco desde la plantilla de nómina**, no desde la cuenta con que pagó Tesorería. Liquidaciones anteriores usa la cuenta real del pago.
- **El RDEP del SAA es un XML propio de tres montos**, no el del SRI: exentos, FR, utilidades y salario digno no tienen dónde salir.
- **El árbol compartido de saaBE no compila el 2026-10-05** por `ProvisionInteresServiceImpl` de crd, sin commitear y ajeno: lo nuestro se verifica compilando `origin/main` + nuestros archivos en un worktree aparte.

- **Los NOT NULL de `RHH.CNTE` son CHECK con nombre de sistema y dan ORA-02290, no ORA-01400.** La consulta de `REFERENCIA-CHECKS-RHH.md` excluía `SYS_%`, dándolos por inofensivos: por eso `SYS_C009213` no estaba documentado.
- **Tesorería y RRHH tienen dos motores de archivo bancario.** El formato del Internacional estaba cargado en el de Tesorería y el de RRHH (`RHH.FMBN`) estaba vacío.
- **Re-correr «Acreditar vacaciones» después del aniversario duplicaría la apertura** de quien ingresó en 2025: 15 días completos más su apertura proporcional del mismo año de servicio.
- **Ninguna liquidación de haberes se paga por Tesorería**: la cuenta 70 `LIQUIDACIONES_POR_PAGAR` se acredita y nada la debita. Y el finiquito escribe el RDEP con el año de la fecha de salida, no la de pago. Detalle en `rhh/API-LIQUIDACION-EXCOLABORADORES.md` §9.
- **Provisiones G48/CCPM (2026-10-13):** `CCPM.provisionConstituida` está cableada a 0
  (`GeneracionCCPMServiceImpl:83,367`). El CCPM califica y provisiona con una tabla escrita en el
  código (`:521-582`, hipotecario = productos 7/8/21), mientras el G48 usa `CRD.CFCR/ESCR` desde el
  2026-09-02: pueden divergir. La «constituida» del G48 es la *requerida* del G48 anterior, no el
  saldo contable de `1.3.99.xx`; y si la EJRC más reciente del mes anterior tiene el EJRD de G48 sin
  filas, todo el mes sale en 0 sin aviso (`GeneracionG48ServiceImpl.cargarProvisionAnterior`).
- **La nómina consolidada (`RHH_NOMINA`) se aprueba con Débito automático**, no por transferencia: el archivo por empleado lo arma RRHH, no Tesorería. El mensaje de la guarda no lo dice (mejora pendiente, `PagoProgramadoServiceImpl` es territorio común).
- **`SCP.PJRQ` tiene dos empresas `ASOPREP` (1236 y 1237) y además guarda los usuarios**; la sesión usa la 1236 escrita a mano en el login del FE. Cualquier `UPDATE` por nombre en `PJRQ` toca más de lo que parece.
- **Un WildFly de pruebas copiado de producción sigue apuntando al 1521 si se olvida una línea**: se verifica por las conexiones TCP del proceso (`jboss.home.dir` → puerto), no por el XML.
- **ORA-04036 en producción (2026-10-19) no es falta de memoria, es el modelo:** con `pga_aggregate_limit` en 4G, `v$pgastat` marcó un máximo de 4710 MB. Casi todos los `@ManyToOne` son EAGER y sin límite de profundidad Hibernate los une en un solo SELECT (431 JOIN para `RHH.ANTE`, 254 de ellos `SCP.PJRQ`+`SCP.PGSP`: `Empresa.jerarquia` y `Usuario.jerarquia` EAGER multiplican todo; el comentario de `persistence.xml` dice 127 por error). Medido sobre las 435 entidades: ~30 con cadenas del mismo orden o mayores (desgloses de depósito, devoluciones de anticipo, cobros de tsr, liquidaciones). LAZY no es opción: las entidades se serializan directo a JSON. Ver `docs/general/HIBERNATE-MAX-FETCH-DEPTH.md`.
