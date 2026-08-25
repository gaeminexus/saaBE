# PROMPT — Agente BACKEND · Corrección: nombres de campos al estándar del sistema

> **Etiqueta: BACKEND** (repo `saaBE`). **Orden:** corre DESPUÉS de la Fase 1 de bandas
> (`PROMPT-BACKEND-BANDAS-FASE1.md`). Es una corrección corta de verificación.

---

## Qué pasó

Tres columnas de las tablas de bandas se crearon con descriptores que no siguen el
estándar de nombres del sistema y fueron renombradas (en BD y en las entidades JPA):

| Antes | Ahora | Estándar aplicado |
|---|---|---|
| `CRD.CBPR.CBPRFCDE` | `CBPRFCIN` | `FCIN` = fecha inicio (44 usos en el sistema, p.ej. `CNT.DTPL.DTPLFCIN`) |
| `CRD.CBPR.CBPRFCHS` | `CBPRFCFN` | `FCFN` = fecha fin (44 usos, p.ej. `CNT.DTPL.DTPLFCFN`) |
| `CRD.BNDP.BNDPPRDS` | `BNDPCNTD` | `CNTD` = cantidad (descriptor establecido; aquí: cantidad de períodos de 30 días) |

Ya están corregidos: los `@Column` de `ConfiguracionBandaProducto` y `BandaProducto`, el
DDL (`sql/DDL-BANDAS-PRODUCTO.sql`), la carga inicial (`sql/CARGA-INICIAL-BANDAS-PRODUCTO.sql`
y su runbook), y la BD local de docker (migrada con `sql/ALTER-BANDAS-RENOMBRE-CAMPOS.sql`,
datos intactos: 28/143). **Los atributos Java NO cambiaron** (`fechaDesde`, `fechaHasta`,
`periodos`), así que el JSON del contrato de API tampoco cambia.

## Tu tarea (verificación)

1. Barre TODO el código de la Fase 1 de bandas (entidades, DAOs, services, DTOs, REST)
   buscando los literales `CBPRFCDE`, `CBPRFCHS` y `BNDPPRDS`. No debe quedar ninguno
   (las JPQL usan atributos Java y no deberían verse afectadas; lo crítico sería alguna
   query nativa o `@Column` residual).
2. Verifica que ninguna JPQL/consulta nativa ni `obtieneCampos()` mencione columnas
   físicas de estas dos tablas; si alguna lo hace, corrígela a los nombres nuevos.
3. Revisa `docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md`: si en algún lugar cita los
   nombres físicos viejos de columnas, actualízalos; si solo usa atributos JSON, no hay
   nada que tocar (deja constancia de que lo verificaste).
4. Reporta al final: qué encontraste, qué corregiste y qué estaba ya limpio.
