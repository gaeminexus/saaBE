# ASOCIACIÓN DEL FONDO COMPLEMENTARIO PREVISIONAL CERRADO ASOPREP-FCPC

**DEPARTAMENTO DE SISTEMAS**

Memorando No. ASOPREP-TIC-[___]-2026

Quito D.M., [__] de septiembre de 2026

**PARA:** [Nombre y cargo de quien remite el requerimiento]

**DE:** Ing. Paúl Manosalvas — Jefe de Sistemas

**ASUNTO:** Estado de la documentación de gestión tecnológica y de seguridad de la información
requerida, y cronograma de formalización

---

## 1. Antecedentes

1.1. Mediante comunicación de [fecha], se solicitó al Departamento de Sistemas remitir la
documentación vigente relacionada con la arquitectura tecnológica, el sistema de información que
soporta el core de la entidad, el Sistema de Gestión de Seguridad de la Información, la gestión de
riesgos operativos tecnológicos, la integración de los sistemas de crédito con el sistema contable,
y el conjunto de políticas, planes, procedimientos, inventarios, informes y registros detallados en
la matriz adjunta a dicha comunicación.

1.2. La Superintendencia de Bancos, mediante Resolución No. SB-INJ-2025-1418 de 10 de junio de
2025, calificó la habilidad legal del Econ. Leonardo David Ramírez Molina como Representante Legal
de ASOPREP-FCPC, con lo que se produjo un **cambio de administración** de la entidad.

1.3. Como parte de las acciones iniciales de la nueva administración, la empresa **AUDIPYMES S.A.**
efectuó una auditoría de sistemas cuya conclusión fue que el sistema informático entonces en uso
(DELTA 21) **no se encontraba en funcionamiento por varias razones**, y recomendó iniciar con
urgencia la selección e implementación de un sistema estable, seguro y funcional. Sobre esa base,
este Departamento emitió el **Informe de Necesidad No. ASOPREP-TIC-NEC-2025-003**.

1.4. El levantamiento técnico del sistema DELTA 21, documentado en el informe
INF-ASOPREP-SRV-2025-01 de 9 de diciembre de 2025, constató que **no existían manuales
funcionales ni técnicos** que definieran flujos, procedimientos ni secuencias de operación, y que
el sistema presentaba vulnerabilidades críticas: manipulación manual de datos sin validación ni
auditoría, accesos sin control con modificación directa de la base de datos, y ausencia de
trazabilidad. Es decir, **la administración anterior no dejó documentación de gestión tecnológica
ni de seguridad de la información sobre la cual construir**.

1.5. El 13 de octubre de 2025 ASOPREP-FCPC contrató a GAEMII NEXUS S.A.S. el desarrollo,
instalación y puesta en producción de un sistema a la medida, el **Sistema de Administración
ASOPREP (SAA)**, con módulos de Contabilidad, Tesorería, Cuentas por Cobrar, Cuentas por Pagar,
Créditos, Recursos Humanos, servidor de reportes y conexión con la aplicación móvil para
partícipes. El plazo se extendió mediante adendum de 3 de julio de 2026 hasta el 16 de septiembre
de 2026.

1.6. El sistema SAA se encuentra instalado en los servidores de propiedad de la entidad y **la
totalidad de la operación del mes de agosto de 2026** —contable, de tesorería, de cartera de
crédito, de cuentas por cobrar y por pagar y de nómina— fue procesada en él, incluidos el cierre de
cartera, el pago mensual a jubilados, los reportes regulatorios G40 a G51 y los balances del
período. El Acta de Entrega-Recepción Definitiva del desarrollo [fue suscrita el 10 de septiembre
de 2026], con lo que inició el **período de garantía y estabilización de seis meses**, hasta el
[10 de marzo de 2027], durante el cual el proveedor atiende los ajustes derivados de la operación.

## 2. Situación general de la documentación requerida

2.1. La documentación de gestión tecnológica y de seguridad de la información —políticas,
procedimientos, planes, matrices de riesgo, inventarios de activos y controles— **describe y
gobierna un entorno tecnológico concreto**. Su valor depende de que ese entorno sea estable y esté
definido.

2.2. En el período comprendido en el requerimiento, la entidad atravesó la sustitución completa de
su plataforma tecnológica: de un sistema sin documentación y en desuso, a un sistema nuevo cuya
implantación concluyó el mes anterior y que se encuentra en su período de estabilización. **No
existe, por tanto, documentación «vigente» sobre el entorno anterior** —nunca la hubo, como consta
en el numeral 1.4—, **y la documentación sobre el entorno actual se encuentra en construcción**,
porque documentar formalmente un entorno que aún está siendo ajustado produciría instrumentos que
quedarían desactualizados en semanas.

2.3. Lo anterior **no significa que no exista información**. Existe, y buena parte puede
remitirse de inmediato, según se detalla en la sección 3. Lo que no existe todavía es su
formalización como el sistema documental que la normativa exige, y este memorando establece el
cronograma para producirlo.

2.4. Con relación a lo señalado en el requerimiento sobre información remitida en un proceso
anterior: aquella respuesta correspondió al estado de la entidad en ese momento, con el sistema
anterior. La presente respuesta la complementa y, en lo que corresponde al entorno tecnológico, la
sustituye.

## 3. Detalle por requerimiento

### 3.1. Información que se remite o puede remitirse de inmediato

| Requerimiento | Situación | Documento de respaldo |
|---|---|---|
| Arquitectura tecnológica de producción, desarrollo y pruebas, incluidos los recursos de seguridad informática | **Disponible.** Producción: Oracle Database, WildFly 38, Java 21 / Jakarta EE 10, Angular 20, JasperReports 7, en servidores propios de la entidad (Dell PowerEdge R350, Windows Server 2025). El ambiente de desarrollo lo provee el proveedor en su infraestructura; el de pruebas se realiza sobre el mismo servidor de la entidad con base de datos separada. [Confirmar/ajustar según configuración real.] | Anexo B del Acta de Entrega-Recepción; sección 6 del informe INF-ASOPREP-SRV-2025-01 |
| Sistema de información que soporta el core de la entidad, en específico la operación de inversiones privativas | **Disponible.** El módulo de Créditos del SAA soporta íntegramente la operación de inversiones privativas: partícipes, contratos, otorgamiento, tablas de amortización, cobros (archivo de descuentos de Petrocomercial y cobros directos), mora, precancelación, condonación, cierre mensual de cartera, calificación de riesgo y reportes regulatorios G40–G51. | Anexo A del Acta, §5 y §7 |
| Explicación sobre la integración de los sistemas de créditos quirografario, prendarios e hipotecarios con el sistema contable | **Disponible.** Los tres tipos de crédito operan en el mismo módulo, diferenciados por producto. La integración con Contabilidad es automática y parametrizada: cada evento de crédito (desembolso, cobro, devengo, mora, precancelación, condonación, cierre de cartera, provisión) genera su asiento contable a partir de plantillas parametrizadas por tipo de evento y producto, con clasificación por bandas de cartera hacia las cuentas del catálogo y una bandeja de aprobación contable previa al registro. No existe interfaz ni carga manual entre los dos módulos: comparten la misma base de datos. | Anexo A del Acta, §1 y §5.5; documentación técnica del proveedor (`REGLAS-ASIENTOS-CONTABLES-CRD`) |
| Inventario de sistemas de información | **Disponible.** Sistema SAA (ocho módulos) y aplicación móvil ASOPREP CONTIGO (desarrollada, pendiente de publicación). Se incluye el sistema DELTA 21 como sistema retirado, con su información histórica migrada al SAA. | Anexo A del Acta |
| Listado de proveedores tecnológicos contratados | **Disponible.** GAEMII NEXUS S.A.S. — desarrollo, implantación y garantía del SAA; contrato de 13-oct-2025 y adendum de 3-jul-2026; garantía hasta [10-mar-2027]. [Agregar proveedor de internet, hosting de correo, licencias y cualquier otro contrato vigente, con fechas y niveles de servicio.] | Contrato y adendum |
| Procedimientos que permitan contar con pistas de auditoría a nivel de aplicativos y bases de datos | **Parcialmente disponible.** El SAA registra en su tabla de auditoría cada acción de usuario (usuario, rol, módulo, acción, registro afectado, valores anterior y posterior, dirección IP y fecha). La auditoría a nivel de sistema operativo y base de datos se formaliza en la Fase 2 del cronograma. | Anexo A del Acta, §10 |
| Administración de usuarios y perfiles, incluidos accesos privilegiados | **Parcialmente disponible.** El SAA cuenta con administración de usuarios, contraseñas generadas con cambio obligatorio al primer ingreso y asignación de permisos por perfil. **La definición de perfiles por área está pendiente de que cada jefatura entregue los roles que requiere**, conforme al Acta; el procedimiento formal se emite en la Fase 2. | Acta, cláusula Octava |

### 3.2. Documentación en construcción, a formalizar dentro del período de estabilización

| Requerimiento | Situación | Fase del cronograma |
|---|---|---|
| Políticas, reglamentos, procesos, procedimientos y manuales de gestión tecnológica (incidentes y problemas, infraestructura, adquisición y desarrollo, control de calidad, control de cambios, monitoreo, respaldos, continuidad, terceros, Comité de TI) | No existen instrumentos formales heredados. Se redactan sobre el entorno SAA una vez estabilizado. Los procedimientos de respaldo y de control de cambios se emiten primero, por ser los de mayor riesgo operativo. | Fase 2 (respaldos, cambios, incidentes) · Fase 3 (resto) |
| Plan Estratégico de Tecnología de la Información | No existe. Se formula para el período 2027–2029 sobre la plataforma ya establecida. | Fase 3 |
| Plan Operativo de TI 2025 y 2026 | El plan operativo de facto de 2025–2026 fue el proyecto de sustitución del sistema, documentado en el contrato, su cronograma y sus informes. Se formaliza como POA retrospectivo y se emite el POA 2027. | Fase 3 |
| Matriz de riesgos operativos relacionados con la infraestructura y plataformas tecnológicas | No existe. Se construye sobre el inventario de activos y la arquitectura definitiva. | Fase 2 |
| Metodología para identificar, medir, controlar y monitorear los riesgos asociados a los activos de información | No existe. Se adopta junto con la matriz. | Fase 2 |
| Políticas, procedimientos y manuales de Seguridad de la Información (SGSI), Ciberseguridad y Comité de Seguridad | No existen. El SGSI se formaliza una vez definidos los perfiles de acceso por área y completado el inventario de activos. | Fase 3 |
| Plan de seguridad de la información y monitoreo de controles | No existe. Depende del SGSI. | Fase 3 |
| Procedimientos de monitoreo periódico de la efectividad de los niveles de seguridad en hardware, software, redes y comunicaciones | No existen formalmente. | Fase 3 |
| Inventario de activos de la información con clasificación, propietario, custodio y ubicación | En elaboración. El inventario de sistemas y de la base de datos existe (sección 3.1); falta la clasificación por valor, sensibilidad y criticidad y la designación formal de propietarios. | Fase 1 |
| Designación de los propietarios de los activos de información del sistema core | En elaboración, junto con el inventario. | Fase 1 |
| Manual de desarrollo de sistemas de información | El proveedor entrega, dentro del período de garantía, el código fuente con su documentación técnica (estándares de mapeo de tablas, de creación de objetos de base de datos y guías de API). Sobre esa base se emite el manual de desarrollo y mantenimiento de la entidad. | Fase 3 |
| Procedimientos para el control y monitoreo de accesos a la información | En elaboración; depende de la definición de perfiles por área. | Fase 2 |
| Último informe de análisis y desempeño de la capacidad tecnológica instalada | No existe. Se emite el primero al cierre del período de estabilización, con datos de operación reales. | Fase 3 |
| Plan anual de capacitación 2025 y 2026 | La capacitación de 2025–2026 fue la del sistema SAA, impartida por el proveedor de manera continua durante la implantación, módulo por módulo. Se documenta como ejecutado y se formula el plan 2027. | Fase 3 |

### 3.3. Requerimientos que no pueden atenderse con información del período, y por qué

| Requerimiento | Situación |
|---|---|
| Base de incidentes y problemas de TI con datos de 2025 y 2026 | **No existe registro estructurado.** El sistema anterior no tenía trazabilidad y el nuevo entró en operación plena en agosto de 2026. **A partir de la fecha de este memorando se abre el registro formal de incidentes** (título, descripción, inicio, resolución, causa raíz), que se remitirá con los datos que acumule. |
| Prueba de vulnerabilidad y penetración a los equipos y medios utilizados en transacciones por canales electrónicos | **No aplica todavía.** El único canal electrónico transaccional previsto —la aplicación móvil ASOPREP CONTIGO— se encuentra desarrollada y pendiente de publicación; el sistema de oficina no está expuesto a internet. La prueba de penetración se contrata antes de la publicación de la aplicación y sobre su infraestructura definitiva. |
| Informes trimestrales de CAIR del cuarto trimestre de 2025 y primer trimestre de 2026 | [**Confirmar con la Gerencia / Comité de Riesgos**: si dichos informes existen, no son de emisión de este Departamento; si no existen, indicarlo expresamente.] |
| Listado de empleados asignados a la operación tecnológica y a Seguridad de la Información, Ciberseguridad y Seguridad Informática | Se remite el listado del personal del Departamento de Sistemas: [nombres, cargo, título, experiencia, capacitación, fecha de ingreso]. **La entidad no cuenta con personal dedicado exclusivamente a seguridad de la información**; esas funciones las ejerce el Departamento de Sistemas con apoyo del proveedor durante la garantía, y su asignación formal se define en el SGSI. |
| Gestión de riesgos operativos efectuada por la primera, segunda y tercera línea en relación con tecnología y seguridad de los aplicativos | Durante el período la gestión del riesgo tecnológico se concentró en la sustitución del sistema —que era el riesgo principal identificado por la auditoría de AUDIPYMES— y en las pruebas de aceptación y validación de la operación de agosto por cada área. La asignación formal de las tres líneas sobre la nueva plataforma se establece con la metodología y matriz de riesgos de la Fase 2. |

## 4. Cronograma de formalización

El cronograma se alinea con el período de garantía y estabilización del sistema, de modo que cada
instrumento se emita sobre una plataforma ya definida y no requiera reelaboración inmediata.

| Fase | Período | Entregables |
|---|---|---|
| **Fase 1 — Inventario y base** | septiembre – octubre 2026 | Inventario de activos de información con clasificación; designación de propietarios; apertura del registro formal de incidentes; listado de personal y proveedores; remisión de la arquitectura y la descripción del core (sección 3.1). |
| **Fase 2 — Control y riesgo** | noviembre – diciembre 2026 | Definición de perfiles por área y procedimiento de administración de accesos; procedimientos de respaldo, control de cambios y gestión de incidentes; metodología de riesgos de TI y matriz de riesgos operativos tecnológicos; pistas de auditoría a nivel de base de datos y sistema operativo. |
| **Fase 3 — Sistema documental** | enero – marzo 2027 (cierre de garantía) | Políticas y manuales de gestión tecnológica; SGSI, plan de seguridad y monitoreo de controles; PETI 2027–2029 y POA 2027; manual de desarrollo sobre las fuentes entregadas; informe de capacidad instalada; plan de capacitación 2027; prueba de penetración previa a la publicación de la aplicación móvil. |

Este Departamento informará el avance de cada fase [mensualmente / trimestralmente] a
[Gerencia / Comité correspondiente].

## 5. Conclusión

La entidad no dispone, a la fecha, del sistema documental de gestión tecnológica y de seguridad de
la información en los términos requeridos, **porque el entorno que debía documentar fue sustituido
en su totalidad durante el período**: el anterior nunca estuvo documentado, según constató la
auditoría de sistemas y el levantamiento técnico, y el actual concluyó su implantación el mes
pasado y se encuentra en estabilización. Lo que sí existe —la arquitectura, el inventario de
sistemas, la descripción del core y de su integración contable, las pistas de auditoría del
aplicativo y la administración de usuarios— se remite con este memorando, y lo que falta se produce
conforme al cronograma de la sección 4, sobre una plataforma ya en operación y con datos reales.

Quedo atento a cualquier precisión adicional que se requiera.

Atentamente,

&nbsp;

**Ing. Paúl Manosalvas**

Jefe de Sistemas

ASOPREP-FCPC

&nbsp;

**Adjuntos:** [Anexo A y Anexo B del Acta de Entrega-Recepción Definitiva · Informe
INF-ASOPREP-SRV-2025-01 · Listado de personal del Departamento de Sistemas · Listado de
proveedores tecnológicos]
