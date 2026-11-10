# DISEÑO — Campos de la ficha del partícipe que pide el Anexo I del contrato de adhesión

omen-saa-1-arb · 2026-11-10 · Pedido del usuario: «Sí, agregar a la ficha todos los campos necesarios» (respuesta a que
el Anexo I del modelo V2 trae datos que el sistema no guarda). Ejecutores: omen-saa-1-be y omen-saa-1-fe.
Relacionado: `crd/DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md` §9 (impreso `RPRT_CNTR_ADHS`).

## 1. Qué falta (cruce del Anexo I contra el modelo, verificado en código)
| Dato del Anexo I | Hoy | Cambio |
|---|---|---|
| Sexo M / F | **no existe** en `CRD.ENTD` ni `CRD.PRTC` (sí en `CRD.PRAS.PRASGNRO`, tabla de origen ASOPREP por cédula, y en `CRD.PRSN`, sin vínculo a la entidad) | **`ENTD.ENTDSEXO VARCHAR2(1)`** `CHECK IN ('M','F')`, nullable; backfill desde `PRAS` por cédula (§3) |
| Nombramiento / Contrato | **no existe** | **`PRTC.PRTCMDLB NUMBER(1)`** `CHECK IN (1,2)`: 1 NOMBRAMIENTO, 2 CONTRATO; nullable; sin backfill (no hay fuente) |
| Barrio / Sector | se imprime `DRCC.referencia` (es otra cosa) | **`DRCC.DRCCBRRO VARCHAR2(300)`**, nullable; el impreso usa barrio y, si está vacío, nada (no la referencia) |
| Título profesional | se imprime `PRTC.nivelEstudios` (nivel, no título) | **`PRTC.PRTCTTPR VARCHAR2(300)`**, nullable; el impreso usa el título y, si está vacío, `nivelEstudios` como respaldo |
| Resto (estado civil, nacimiento, cargas, rol, empresa, vivienda, teléfonos, correos, provincia/ciudad/parroquia, calles, remuneración, ingreso, lugar, unidad, cargo, ingresos adicionales) | existe | sin cambio |

Constantes en `rubros/` (estado técnico, sin `PRBR`/`PDTR`): `ModalidadLaboral` (1 NOMBRAMIENTO, 2 CONTRATO) y
`SexoParticipe` ('M','F').

## 2. Backend
- Mapear las 4 columnas en `Entidad`, `Participe` y `Direccion` (getters/setters, `obtieneCampos()` de sus DAOs).
- Validación en el guardado de la ficha: sexo solo `M`/`F`/null; modalidad solo 1/2/null. Error claro si no.
- `ContratoAdhesionParametros`: casillas Sexo y Nombramiento/Contrato con los campos nuevos; barrio desde `DRCCBRRO`;
  título desde `PRTCTTPR` con respaldo `nivelEstudios`. Recompilar no hace falta para el `.jasper` si solo cambian
  valores de parámetros; si cambia el `.jrxml`, compilar + fill + PDF mirado.
- **⛔ ORA-00904:** el EAR que mapee estas columnas va **después** del `crd/sql/377`. Toda lectura de ENTD/PRTC/DRCC
  falla sin ellas (son tablas centrales: padrón, préstamos, cargas Petro). Se anota en el runbook del EAR.

## 3. DDL `crd/sql/377` (aditivo, columnas nullable, con respaldo y reverso)
- Control: columnas no existen; distribución de `PRAS.PRASGNRO` (valores reales, para saber si son M/F u otros);
  cuántas entidades cruzan por cédula con un solo género en PRAS.
- Backfill de `ENTDSEXO` solo donde la cédula cruza con **un único** valor M/F en PRAS (cédula repetida con valores
  distintos → no se llena). Otros valores (p. ej. «H», «1») **no se traducen sin ver la distribución**: el bloque se
  ajusta con la salida del control.

## 4. Frontend (ficha del partícipe, `crd/forms/entidad-participe`)
- Sexo (M/F), Modalidad laboral (Nombramiento/Contrato), Título profesional, y Barrio/Sector en la dirección.
  Opcionales. Donde se editan los demás datos de cada tabla.
- La tarjeta de firma no cambia.

## 5. Riesgos
- Son columnas en tablas centrales: si el EAR sale antes que el 377, cae todo crd. Orden estricto.
- El backfill de sexo depende de la calidad de PRAS: se revisa la distribución antes de aceptar el UPDATE.
