# ANEXOS AL MEMORANDO No. ASOPREP-TIC-[___]-2026

**Respuesta al requerimiento de documentación de gestión tecnológica y de seguridad de la
información — Departamento de Sistemas, ASOPREP-FCPC**

| Anexo | Contenido | Responde a los requerimientos |
|---|---|---|
| Anexo 1 | Arquitectura tecnológica del Sistema de Administración ASOPREP (SAA) | 4 |
| Anexo 2 | Inventario de sistemas de información y descripción funcional del sistema core | 13, 24, 27 |
| Anexo 3 | Listado de proveedores tecnológicos contratados | 8 |
| Anexo 4 | Listado del personal asignado a la operación tecnológica y a seguridad de la información | 9, 22 |
| Anexo 5 | Formato del registro de incidentes y problemas de tecnología de la información | 7 |

Los Anexos 1 y 2 describen el sistema en operación desde agosto de 2026. Los Anexos 3 y 4 se
completan con la información de la entidad. El Anexo 5 es el formato con el que se abre el
registro a partir de la fecha del memorando.

---

# ANEXO 1 — ARQUITECTURA TECNOLÓGICA DEL SISTEMA SAA

## 1.1. Plataforma

| Componente | Tecnología |
|---|---|
| Base de datos | Oracle Database, organizada en esquemas por dominio: CNT (contabilidad), CRD (créditos), CBR (cuentas por cobrar), PGS (cuentas por pagar), TSR (tesorería), RHH (recursos humanos), RPR (reportes regulatorios), SCP (núcleo y seguridades) |
| Servidor de aplicaciones | WildFly 38, con datasource JTA administrado por el servidor |
| Backend | Java 21 sobre Jakarta EE 10: EJB para la lógica de negocio y las transacciones, JPA/Hibernate para la persistencia, JAX-RS para los servicios REST |
| Reportes | JasperReports 7.0.3, con plantillas precompiladas por módulo |
| Frontend | Angular 20 con Angular Material 20, aplicación de una sola página organizada en módulos por área |
| Aplicación móvil | Flutter (Dart), para Android e iOS, con una capa intermedia de publicación (WAR de borde) que aísla el sistema de oficina de internet |
| Servidor | Dell PowerEdge R350 (Intel Xeon E-2314, 16 GB RAM, 2 TB), Windows Server 2025 Standard, de propiedad de la entidad |

## 1.2. Desarrollo en capas del backend

Cada tabla del sistema se implementa siguiendo un mismo patrón de capas, lo que garantiza
uniformidad, mantenibilidad y separación de responsabilidades:

| Capa | Responsabilidad |
|---|---|
| Modelo (entidades JPA) | Representación de cada tabla como clase Java, con el mapeo de columnas, relaciones y consultas nominadas. 415 entidades. |
| Acceso a datos (DAO) | Interfaz e implementación por entidad, sobre un DAO genérico que provee las operaciones comunes y consultas específicas por entidad. |
| Servicios de negocio | Interfaz e implementación por entidad, con las reglas de negocio, las validaciones y el control transaccional. Los procesos complejos (cargas de archivos, conciliaciones, cierres, corridas de pago, reportes regulatorios) se implementan como servicios adicionales con control explícito de transacciones. |
| Servicios REST | Exposición de cada entidad y proceso como recurso HTTP, con un conjunto estándar de operaciones por entidad y operaciones específicas por proceso. 400 recursos. |
| Utilidades y núcleo | Conversores, validadores, manejo de archivos, catálogo de rubros y constantes compartidas por todos los módulos. |
| Reportes | Servicio único de generación que resuelve la plantilla por módulo y nombre y produce el documento. |
| Procesos automáticos | Temporizadores del servidor de aplicaciones para procesos programados (cálculo diario de interés de mora). |

## 1.3. Frontend

Aplicación Angular organizada en módulos por área (Contabilidad, Créditos, Cuentas por Cobrar,
Cuentas por Pagar, Tesorería, Recursos Humanos, Reportes, Tablero), cada uno con su menú, sus
modelos, sus servicios de acceso a la API y sus pantallas (289 componentes, 222 rutas). La
comunicación con el backend es exclusivamente a través de la API REST.

## 1.4. Aplicación móvil

La aplicación se comunica únicamente con una capa intermedia de publicación, desplegada en un
servidor distinto del sistema de oficina, que reenvía cada consulta a los servicios de la capa de
conexión del sistema SAA mediante un secreto compartido. La capa intermedia no tiene acceso directo
a la base de datos y el sistema de oficina no se expone a internet. Toda la información que la
aplicación muestra se consulta en vivo; no existe copia ni sincronización de datos en el
dispositivo.

## 1.5. Ambientes

| Ambiente | Ubicación | Descripción |
|---|---|---|
| Producción | Servidores de propiedad de ASOPREP-FCPC | WildFly con el sistema SAA y base de datos Oracle. El sistema de oficina **no está expuesto a internet**. |
| Desarrollo | Infraestructura del proveedor (GAEMII NEXUS S.A.S.) | Conforme al esquema de trabajo tipo outsourcing establecido en el Anexo 1 del contrato. |
| Pruebas | [Confirmar: mismo servidor de la entidad con base de datos separada / otro] | [Describir] |

## 1.6. Recursos de seguridad informática en operación

- Aislamiento del sistema de oficina respecto de internet; la única exposición prevista es la capa
  intermedia de publicación de la aplicación móvil, en un servidor distinto y sin acceso directo a
  la base de datos.
- Administración de usuarios del sistema con contraseñas generadas y cambio obligatorio en el
  primer ingreso; asignación de permisos por perfil.
- Registro de auditoría de las acciones de los usuarios en el aplicativo (usuario, rol, módulo,
  acción, registro afectado, valores anterior y posterior, dirección IP y fecha).
- Para la aplicación móvil: credenciales con hash y sal, bloqueo por intentos fallidos, sesión con
  token de vigencia limitada, validación de pertenencia por recurso y secreto compartido entre la
  capa intermedia y el sistema.
- [Confirmar y agregar: antivirus, respaldos programados, certificados, firewall y políticas de
  contraseña del dominio, según lo instalado en el servidor.]

---

# ANEXO 2 — INVENTARIO DE SISTEMAS DE INFORMACIÓN Y DESCRIPCIÓN DEL SISTEMA CORE

## 2.1. Inventario de sistemas de información

| Sistema | Estado | Función | Proveedor / origen |
|---|---|---|---|
| **Sistema de Administración ASOPREP (SAA)** | En producción desde agosto de 2026 | Sistema core de la entidad: contabilidad, tesorería, cuentas por cobrar y por pagar, créditos (inversiones privativas), recursos humanos, reportes regulatorios y seguridades | GAEMII NEXUS S.A.S., desarrollo a la medida, contrato de 13-oct-2025 |
| **Aplicación móvil ASOPREP CONTIGO** | Desarrollada; pendiente de publicación en tiendas | Consulta del partícipe: créditos, cuotas, aportes, cuenta individual, simulador | GAEMII NEXUS S.A.S., mismo contrato |
| DELTA 21 | **Retirado** | Sistema anterior; su información histórica fue migrada al SAA | Proveedor anterior; sin documentación técnica (informe INF-ASOPREP-SRV-2025-01, §4) |
| [Otros: correo electrónico, ofimática, otros sistemas externos si los hubiera] | | | |

## 2.2. Descripción funcional del sistema SAA

El sistema comprende 415 tablas de base de datos, 400 servicios de aplicación, 289 pantallas y 55
reportes, organizados en los siguientes módulos.

### 2.2.1. Contabilidad

Parametrización: naturaleza de cuentas; plan de cuentas en vista de árbol y de grilla; centros de
costo; tipos de asiento; plantillas contables generales y de sistema, que definen los asientos
automáticos que generan los demás módulos; períodos contables. Procesos: registro de asientos con
detalle y subdetalle; listado y consulta de asientos; mayorización por período; numeración alterna;
reversos. Reportes: balance general y estado de resultados, en formato de trabajo y en el formato
formal de la Superintendencia de Bancos (Resolución SBS-2013-0507); balance de prueba; mayor
analítico; listado de asientos; plan de cuentas; centros de costo.

### 2.2.2. Tesorería

Bancos y cuentas bancarias, chequeras y cheques, titulares, cajas chicas; estado de cuenta;
anticipos a clientes y proveedores; ingresos y egresos; cobros por efectivo, cheque, tarjeta,
transferencia y retención; cierre de caja; depósitos con envío y ratificación; pagos por
transferencia con aprobación, generación del archivo bancario (Banco Internacional y Banco del
Pacífico), recepción y confirmación; extractos bancarios con carga desde los archivos de los bancos;
conciliación bancaria contable con tablero de cumplimiento. Reportes de conciliación y anticipos.

### 2.2.3. Cuentas por Cobrar

Grupos de productos e impuestos; datos del facturador y del SRI; emisión de facturas, notas de
crédito y débito, liquidaciones de compra y retenciones; documentos electrónicos con generación,
firma, envío y autorización en el SRI; financiamiento de facturas; registro y consulta de cobros;
cruce de anticipos; RIDE de cada comprobante; Anexo Transaccional Simplificado y cuadre 103/104.

### 2.2.4. Cuentas por Pagar

Productos, proveedores, datos del SRI; bandeja electrónica de recepción de comprobantes; gestión y
consulta de documentos de compra; nota de venta manual; proposición y aprobación de pago;
solicitudes de pago; cruce de anticipos; pagos programados por lotes; negociaciones con proveedores;
Anexo Transaccional Simplificado.

### 2.2.5. Créditos (inversiones privativas)

**Históricos y migración:** carga y consulta de la información migrada del sistema DELTA 21, con
herramientas de depuración de estados con auditoría de cambios. **Parametrización:** información
general del fondo; tipos, estados y listados; productos de crédito; bandas de cartera por producto
con vigencia; escala de calificación de riesgo por producto; cuentas contables por tipo de aporte;
plantillas contables del módulo. **Partícipes:** administración, consulta, listado, consolidado y
dash; certificados; contratos y vigencias con devengo de aportes; jubilación y pago mensual a
jubilados; padrón de partícipes para procesos electorales; cesantías; devolución de aportes.
**Préstamos:** ingreso de solicitudes, generación de la tabla de amortización, aprobación y rechazo;
consulta de préstamos y cuotas; valores insolutos; simuladores; abono a capital; precancelación;
acuerdos de pago y condonación; proceso diario de cálculo de interés de mora. **Cobros:** carga del
archivo de descuentos de Petrocomercial con validación y aplicación de pagos; generación del archivo
de descuentos por filial; pago de cuota, cruce de valores con aportes, cobros personales; bandeja de
contabilidad con aprobación y generación del asiento; anulación y reverso; motor de pago con
prelación de cobro y distribución por bandas; cierre de cartera mensual. **Reportes:** tablas de
amortización, simulaciones, comprobantes de pago, certificados, estado de cuenta y movimientos de
aportes, informe de devolución, reporte de la corrida de jubilados.

### 2.2.6. Recursos Humanos

Conceptos y parámetros de nómina; impuesto a la renta; departamentos, cargos, tipos de contrato,
turnos; colaboradores con ficha, contrato y cuenta bancaria; vacaciones, permisos y licencias;
marcaciones y asistencia; períodos de nómina; novedades; IESS; roles de pago con cálculo,
aprobación, contabilización y órdenes de pago; beneficios sociales; liquidaciones y finiquito;
salidas oficiales; utilidades; anticipos. Reportes de rol, finiquito, formulario 107, aportes y
provisiones.

### 2.2.7. Reportes regulatorios

Generación de las estructuras **G40 a G51** para la Superintendencia de Bancos, con ejecución por
período, control de ejecución, detalle por reporte e histórico; informes mensuales de cartera
(CPRM, CJBM, CCPM); exportación a CSV/Excel desde todas las pantallas de consulta.

### 2.2.8. Seguridades, auditoría y parametrización

Administración de usuarios y asignación de permisos por perfil; contraseñas generadas y cambio
obligatorio en el primer ingreso; registro de auditoría de las acciones de los usuarios (usuario,
rol, módulo, acción, registro afectado, valores anterior y posterior, dirección IP y fecha); más de
120 catálogos parametrizables administrables desde las pantallas de cada módulo.

### 2.2.9. Integraciones externas

Servicio de Rentas Internas (comprobantes electrónicos con firma, envío, autorización y reenvío);
archivos de descuentos de Petrocomercial; formatos de pago de Banco Internacional y Banco del
Pacífico; extractos bancarios de Atlántida, Pacífico, Cooperativa Policía Nacional y Cooperativa
JEP; migración del sistema DELTA 21.

## 2.3. El sistema core y la operación de inversiones privativas (requerimiento 24)

La operación de inversiones privativas de la entidad —los créditos a partícipes— se soporta
íntegramente en el módulo de Créditos del SAA (§2.2.5), sobre la misma base de datos que el resto
de los módulos. Cubre el ciclo completo: partícipes y contratos, otorgamiento con aprobación, tabla
de amortización, cobro por el archivo de descuentos de Petrocomercial y por cobros directos,
cálculo diario de interés de mora, abonos a capital, precancelación, acuerdos de condonación,
cierre mensual de cartera, calificación de riesgo por producto y generación de las estructuras
regulatorias G40 a G51.

## 2.4. Integración de los créditos quirografarios, prendarios e hipotecarios con el sistema contable (requerimiento 27)

Los tres tipos de crédito se administran en el **mismo módulo**, diferenciados por **producto**
(cada producto define tasa, plazo, garantía, cuentas contables y clasificación de riesgo). No
existen sistemas separados por tipo de crédito.

La integración con Contabilidad es **automática y parametrizada**, no una interfaz ni una carga:

1. Cada **evento** del crédito —desembolso, cobro de cuota, devengo, mora, abono, precancelación,
   condonación, cierre mensual de cartera, constitución de provisión— tiene una **plantilla
   contable** parametrizada en el módulo de Contabilidad, que define las líneas del asiento (cuenta,
   naturaleza, origen del valor) por tipo de evento y por producto.
2. Al ocurrir el evento, el módulo de Créditos genera el asiento a partir de la plantilla. La cuenta
   de cartera se resuelve por **bandas de cartera** (por vencer, vencida por rangos de días),
   parametrizadas por producto y con vigencia, que llevan cada valor a la cuenta del catálogo que le
   corresponde según la situación de la cuota.
3. Los cobros pasan por una **bandeja de aprobación contable**: el asiento se registra
   definitivamente sólo con la aprobación del área contable, con posibilidad de anulación y reverso.
4. El **cierre mensual de cartera** reclasifica los saldos entre bandas y genera los asientos de
   reclasificación; la **calificación de riesgo** por producto alimenta el cálculo de provisiones y
   el reporte G48.
5. Los dos módulos comparten la **misma base de datos y el mismo motor transaccional**: no hay
   archivos intermedios, procesos de carga ni conciliación manual entre créditos y contabilidad.

---

# ANEXO 3 — LISTADO DE PROVEEDORES TECNOLÓGICOS CONTRATADOS

| Empresa | Servicio | Fecha de inicio | Fecha de finalización | Nivel de servicio acordado |
|---|---|---|---|---|
| GAEMII NEXUS S.A.S. | Desarrollo a la medida, instalación y puesta en producción del sistema SAA y de la aplicación móvil ASOPREP CONTIGO | 13-oct-2025 (contrato); 3-jul-2026 (adendum) | 16-sep-2026 (plazo de ejecución); garantía hasta [10-mar-2027] | Garantía de seis meses sin costo sobre los módulos desarrollados; soporte y nuevas versiones por un mínimo de tres años con costo independiente (cláusulas Décima Primera y Décima Segunda del contrato) |
| [Proveedor de internet] | [Enlace, IP pública] | | | [Ancho de banda, disponibilidad] |
| [Proveedor de correo electrónico] | | | | |
| [Licencias: sistema operativo, Oracle, ofimática, antivirus] | | | | |
| [Otros] | | | | |

---

# ANEXO 4 — PERSONAL ASIGNADO A LA OPERACIÓN TECNOLÓGICA Y A SEGURIDAD DE LA INFORMACIÓN

## 4.1. Operación tecnológica (requerimiento 9)

| Nombres y apellidos | Unidad | Cargo | Título | Experiencia | Capacitación | Fecha de ingreso |
|---|---|---|---|---|---|---|
| Paúl Manosalvas | Departamento de Sistemas | Jefe de Sistemas | [ ] | [ ] | [ ] | [ ] |
| [ ] | | | | | | |

## 4.2. Seguridad de la información, ciberseguridad y seguridad informática (requerimiento 22)

La entidad **no cuenta con personal dedicado exclusivamente** a seguridad de la información. Las
funciones las ejerce el Departamento de Sistemas (§4.1), con el apoyo del proveedor durante el
período de garantía. La asignación formal de responsabilidades se define en el Sistema de Gestión
de Seguridad de la Información (Fase 3 del cronograma).

| Nombres y apellidos | Unidad | Función de seguridad asignada | Desde |
|---|---|---|---|
| Paúl Manosalvas | Departamento de Sistemas | Administración de usuarios y perfiles; custodia de accesos privilegiados; respaldos | [ ] |

---

# ANEXO 5 — REGISTRO DE INCIDENTES Y PROBLEMAS DE TECNOLOGÍA DE LA INFORMACIÓN

**Formato con el que se abre el registro a partir del [fecha del memorando].** Se registra todo
incidente con afectación a un servicio crítico de la entidad (sistema SAA, base de datos, servidor,
red, correo electrónico, aplicación móvil una vez publicada).

| No. | Título | Descripción | Servicio afectado | Inicio (fecha y hora) | Resolución (fecha y hora) | Causa raíz | Fecha del informe de causa raíz | Responsable |
|---|---|---|---|---|---|---|---|---|
| 001 | | | | | | | | |

**Criterios mínimos:** se abre una fila al detectarse el incidente, aunque no se conozca la causa;
la causa raíz y la fecha de su informe se completan al cierre; un incidente que se repite con la
misma causa se vincula al anterior y se registra como problema.
