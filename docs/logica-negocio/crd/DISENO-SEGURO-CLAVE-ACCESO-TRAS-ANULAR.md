# Pólizas de seguro: la clave de acceso de un documento ANULADO se puede volver a registrar (equipo omen-saa-3, 2026-11-11)

Caso del usuario: en seguro de incendio se anuló una carga porque se ingresó mal la tasa; al registrar otra vez la
misma factura de la aseguradora (GENERALI, 002-002-000137724) en un listado nuevo, el registro responde
`CLAVE_ACCESO_DUPLICADA: ... ya está registrada en el documento 1` — y el documento 1 está ANULADO.

## Causa
La clave de acceso es única en **toda** la tabla, sin mirar el estado:
- Java: `DocumentoSeguroDaoServiceImpl.selectByClaveAccesoNormalizada` (`upper(trim(claveAcceso)) = :clave`), usado
  por `registrarDocumento` (factura, `PUT /posg/{id}/documento`) y por el registro de notas (ND/NC).
- BD: `CREATE UNIQUE INDEX CRD.UX_POSG_CLAVE_ACCESO ON CRD.POSG (UPPER(TRIM(POSGCLAC)))` (`crd/sql/306`).

Anular es justamente la salida para corregir un documento mal cargado; la clave tiene que quedar libre.

## Corrección
- **K1 (BD):** el índice único deja fuera los ANULADOS (5):
  `CREATE UNIQUE INDEX CRD.UX_POSG_CLAVE_ACCESO ON CRD.POSG (CASE WHEN POSGESTD <> 5 THEN UPPER(TRIM(POSGCLAC)) END)`.
  Oracle no indexa las filas donde la expresión es NULL: una clave puede estar en N anulados y en **un** vivo.
  Script `crd/sql/e3-20-posg-clave-unica-sin-anulados.sql`.
- **K2 (Java):** `selectByClaveAccesoNormalizada` agrega `and (d.estado is null or d.estado <> ANULADO)`.
  Los dos chequeos (`registrarDocumento` y notas) pasan a ignorar anulados sin más cambios.
- El documento anulado conserva su clave tal cual (auditoría). Nada más cambia: el enlace con CXP es por
  `POSGDCXP` (id), no por clave.

## Orden de despliegue
K1 y K2 son independientes: con solo K1 el Java sigue rechazando; con solo K2 la BD rechaza con ORA-00001.
Hacen falta los dos. K1 primero (no rompe nada con el EAR actual).
