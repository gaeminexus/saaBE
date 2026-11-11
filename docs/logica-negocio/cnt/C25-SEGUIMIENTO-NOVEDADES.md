# C25 — Seguimiento de novedades (contrato REST y pruebas)

Pantalla: Contabilidad > Procesos > Seguimiento Novedades (saaFE rama `cuadre/c25-seguimiento-novedades`).
Diseño y decisiones: expediente cuadreSuper, `03-trabajo/cambios-saa/c25-seguimiento-novedades/C25-SEGUIMIENTO-NOVEDADES.md`
y D-056. DDL: `sql/C25-01-seguimiento-novedades.sql` (va ANTES del WAR). Permisos: `../seguridad/sql/C25-FE-01-permisos-seguimiento-novedades.sql`.

## Estados

- Novedad (`NVDDESTD`): 1 BORRADOR, 2 ENVIADA, 3 RESPONDIDA_PARCIAL, 4 RESPONDIDA, 5 OBSERVADA, 6 POR_APLICAR, 7 CERRADA,
  8 ANULADA, 9 REEMPLAZADA. De 2 a 6 se recalcula en `SeguimientoNovedadServiceImpl.recalcular` desde los requerimientos.
- Requerimiento (`RQNVESTD`): 1 PENDIENTE → 2 RESPONDIDO → 3 ACEPTADO → 6 APLICADO; 4 OBSERVADO (vuelve a responderse);
  5 NO_EXISTE (exige decisión con constancia); 7 NO_APLICA; 8 ANULADO.
- Reglas: aceptar/observar lo hace un usuario distinto del que registró la respuesta; DECISION, APROBACION y NO_EXISTE
  exigen una decisión registrada (`POST /dcsn/registrar`, con adjunto de constancia obligatorio); APLICADO exige uno o
  varios asientos de ajuste ACTIVOS de la empresa (`CNT.ASNT`) o el motivo "sin ajuste"; cerrar exige todos los
  requerimientos APLICADO / NO_APLICA.
- Toda escritura valida el usuario (U-2) y deja fila en `CNT.HSNV`. Errores de negocio: 400 con el mensaje; resto 500.

## Endpoints (`/SaaBE/rest/...`)

| Ruta | Qué hace |
|---|---|
| `GET nvdd/bandeja?idEmpresa&anio&mes&modulo&tipo&estado&area&texto&incluirReemplazadas` | Una fila por novedad con conteos, asientos y saldo por regularizar vinculado |
| `GET nvdd/vistaRequerimientos?...` | Una fila por requerimiento |
| `GET nvdd/{id}/detalle` · `GET nvdd/{id}/historial` | Detalle completo · historial |
| `GET nvdd/resumenMes?idEmpresa&anio&mes` | Contadores, antigüedad, cuentas por regularizar |
| `GET nvdd/cuentasPorRegularizar?idEmpresa&anio&mes` | Saldo en vivo de las 8 cuentas por regularizar desde `CNT.DTAS` (enchufe de C22) |
| `GET nvdd/reporteActa?idEmpresa&anio&mes` | Anexo del acta (datos + texto markdown) |
| `GET nvdd/pendientePorArea?idEmpresa&anio&mes&area` | Pedido pendiente por área (texto) |
| `POST nvdd/cargaLote?idEmpresa&usuario&simular` | Carga el `novedades-AAAA-MM.json` del expediente (simular = rollback) |
| `POST nvdd/cargaDecisiones?idEmpresa&usuario&simular` | Carga `decisiones.json` del expediente |
| `POST nvdd/enviar?idEmpresa&anio&mes` | `{usuario, ids[], fechaLimite}`; sin ids, todo el mes |
| `POST nvdd/{id}/cerrar · reabrir · anular · nuevaVersion` | `{usuario, motivo}` |
| `POST nvdd/{id}/ajuste` | `{usuario, asientos[], idRequerimiento?, motivo?}` asientos manuales de ajuste (D-056 (7)) |
| `POST nvdd/{id}/vincular` · `POST vnnv` · `POST vnnv/{id}/anular` · `DELETE vnnv/{id}?usuario&motivo` | Vínculos |
| `POST nvdd/cerrarInformativas?idEmpresa&anio&mes` | Cierra en bloque las informativas enviadas |
| `POST rqnv/{id}/responder · noExiste · noAplica · aplicar` | Requerimiento |
| `POST rsnv/{id}/aceptar · observar` | Revisión |
| `POST dcsn/registrar?idEmpresa` · `POST dcsn/{id}/copiada` · `GET dcsn/{id}/textoExpediente` | Decisiones (D-056 (3)(4)) |
| `POST adnv` · `POST adnv/{id}/copiadoExpediente` | Adjuntos (archivo subido antes con `POST file/upload/custom`) |
| `GET {nvdd,rqnv,rsnv,vnnv,adnv,dcsn,hsnv}/getAll · getId/{id}` · `POST .../selectByCriteria` | Lecturas estándar |
| `GET prdo/controlCierre/...` | Dos controles nuevos del grupo NOVEDADES (advertencias) |

## Pruebas con el WildFly (no corridas: el despliegue lo hace el líder)

Base de trabajo local con C25-01 aplicado y junio 2025 cargado (66 novedades, ids 1–66; TABLAS-59 = 26 con
requerimientos 37–95). `B=http://localhost:8080/SaaBE/rest`; `UA` y `UB` = dos usuarios existentes distintos.

| # | Llamada | Esperado |
|---|---|---|
| 1 | `curl "$B/nvdd/resumenMes?idEmpresa=1236&anio=2025&mes=6"` | `novedades` 66; `porTipo` INFORMATIVA 17, DOCUMENTO 36, CONFIRMACION 7, DECISION 5, ELECCION 1; cuentas: 1.4.90.90.05 saldo final 100723.21, 2.3.90.90.05 -60819.97, 2.3.90.90.09 -68865.44; `totalSinNovedad` 0 |
| 2 | `curl "$B/nvdd/bandeja?idEmpresa=1236&anio=2025&mes=6&texto=REGULARIZAR"` | 1 fila B-07, 24 asientos de origen, saldoPorRegularizar 11400.78 |
| 3 | `curl "$B/nvdd/26/detalle"` | 59 requerimientos ELECCION con opciones `MANTENER|REEMPLAZAR`; c22.instalado false |
| 4 | `curl -X POST "$B/nvdd/cargaLote?idEmpresa=1236&usuario=UA&simular=true" -H "Content-Type: application/json" --data @novedades-2025-06.json` | 200; nuevas 0, omitidas 0, actualizadas 66 (todas en BORRADOR); nada grabado |
| 5 | igual sin `usuario` | 400 "No se puede procesar sin un usuario válido" |
| 6 | `curl -X POST "$B/rqnv/37/responder" -d '{"usuario":"UA","opcion":"MANTENER"}' -H "Content-Type: application/json"` | 400: la novedad está en BORRADOR |
| 7 | `curl -X POST "$B/nvdd/enviar?idEmpresa=1236" -d '{"usuario":"UA","ids":[26],"fechaLimite":"2026-11-25"}' ...` | enviadas 1 |
| 8 | repetir 6 con `"opcion":"OTRA"` | 400 "no es una de MANTENER|REEMPLAZAR"; con MANTENER, 200 y req 37 RESPONDIDO; novedad 26 RESPONDIDA_PARCIAL |
| 9 | `POST rsnv/{idRespuesta}/aceptar` con `UA` | 400 (cuatro ojos); con `UB` 200, req 37 ACEPTADO |
| 10 | `POST nvdd/26/ajuste` `{"usuario":"UB","asientos":["DIA-2026-03-0005"]}` | 400: el asiento está ANULADO |
| 11 | `POST nvdd/26/ajuste` `{"usuario":"UB","asientos":["DIA-2025-06-0001","13487"],"idRequerimiento":37}` | 200, 2 vínculos ASIENTO_AJUSTE, req 37 APLICADO |
| 12 | `POST dcsn/registrar?idEmpresa=1236` sin `adjuntos` | 400 (constancia obligatoria); con un adjunto, 200, número D-057 y `textoExpediente` |
| 13 | `GET nvdd/reporteActa?idEmpresa=1236&anio=2025&mes=6` | `puedeGenerarse` false (65 en BORRADOR); texto con los 2 ajustes del paso 11 |
| 14 | `GET prdo/controlCierre/1236/2025/6` | incluye NOVEDADES_BORRADOR (ADVERTENCIA, 65) y REGULARIZAR_SIN_NOVEDAD (OK) |

Para deshacer las pruebas en la base de trabajo: borrar las filas de HSNV/VNNV/RSNV/ADNV/DCSN creadas después de la
carga (por `...FCRG` de la fecha de la prueba) y volver la novedad 26 y sus requerimientos a estado 1; o recrear con el
reverso de C25-03 y volver a cargar.
