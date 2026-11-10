# Despliegue del EAR «contrato de adhesión con firma electrónica, ficha del Anexo I y pruebas de Security Data» (2026-11-10)

**omen-saa-1-arb.** Se armó sobre el EAR desplegado `firmas-1110` (omen3, `DESPLIEGUE-EAR-FIRMAS-2026-11-10.md`), que a
su vez contiene `retenida-1109`. Cambian los dos WAR.

## 1. El EAR
- `F:\ears\asoprep\adhesion-1110\SaaEAR.ear` — SHA-256 `20FFAA7B0BCBBE66F5F25F3E9A6EF65A27F048E3ADEB07548772CB08F361168F`
  (partido en `adhesion-1110\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `BB4DF67184D5F0B8273875DCD6961DD8DBB0F100169186065C79FA1C29D01661` — saaBE `main` en `330cdcdb`
    (`mvn -q clean package -DskipTests`). Los tres reportes de omen3 (`RPRT_PRST_PGRE`, `RPRT_PRST_SLCT`,
    `RPRT_ROLL_DTLL`) quedan idénticos a `firmas-1110` (mismo CRC).
  - `SaaFE.war` `19681DC78C5EE6FC55EEC2B6F366EDFC3B3E06D79E6FDF2346D03B652C346B91` — saaFE `main` en `81893c4`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war de `firmas-1110`).
- **Vuelta atrás:** `F:\ears\asoprep\firmas-1110\SaaEAR.ear` (`CE864F6F…2BC4D4`). Las tablas y columnas de 376/377
  pueden quedarse (el EAR viejo no las lee).

## 2. ⛔ ANTES del EAR: scripts (YA CORRIDOS el 2026-11-10, controles OK)
- `crd/sql/376` — `CRD.FRCN`, `CRD.FRFR`, tipos de adjunto «CONTRATO DE ADHESION» / «… FIRMADO». ✅
- `crd/sql/377` — `ENTDSEXO`, `PRTCMDLB`, `PRTCTTPR`, `DRCCBRRO`. ✅ (backfill de sexo: 0 filas, `CRD.PRAS` sin datos).
  **Sin el 377 todo crédito da ORA-00904** (Entidad, Partícipe y Dirección se leen en todas partes).
- `crd/sql/374` (de `retenida-1109`) tiene que estar corrido: `firmas-1110` ya lo exigía.

## 3. Propiedades de sistema de WildFly (nuevas)
| Propiedad | Valor | |
|---|---|---|
| `saa.firma.securitydata.url` | `https://portal-bpmdesarrollo.securitydata.net.ec/microservices-security-contrato/api/dtagree` (desarrollo) | obligatoria para firmar |
| `saa.firma.securitydata.usuario` / `.clave` | usuario API y su clave (los del PDF de Security Data; **no** el usuario del portal) | obligatorias; **nunca en el repo** |
| `saa.firma.securitydata.ruc` | RUC del cliente en Security Data | obligatoria |
| `saa.firma.securitydata.pruebas` | `true` para habilitar la pantalla de pruebas | opcional (sin ella, la pantalla solo muestra la configuración) |
| `.metodoNotificacion` / `.diasVigencia` / `.horasRenotificacion` | por defecto `ENVIO_OTPN_CORREO_WSS` / 7 / 24 | opcionales |
| `.referenciaRepresentante` | solo si Security Data pide nombrar la firma del representante | opcional (decisión: la estampan ellos) |

Sin las obligatorias el EAR arranca igual; «Enviar a firmar» responde `PROVEEDOR_NO_CONFIGURADO` nombrando la que falta.

## 4. Qué trae
| Cambio | Commits |
|---|---|
| Contrato de adhesión impreso `RPRT_CNTR_ADHS` (modelo V2 del cliente, Anexo I) | BE `bc50b41a`, `710bdc29`, `08acf8d1` |
| Firma electrónica del contrato con Security Data: tarjeta en el contrato, código dactilar al enviar (no se guarda), enlace al socio, seguimiento y descarga del PDF firmado | BE `1fad1d23`, `02d32fc5`, `330cdcdb`; FE `bd97364`, `41a1a76` |
| Ficha del socio: sexo, modalidad laboral, título profesional, barrio/sector | BE `08acf8d1`; FE `7b4eeaa` |
| Pantalla «Pruebas Security Data» (Parametrización de créditos, todos los usuarios con ese permiso) | BE `e724ba07`; FE `81893c4` |

## 5. Despliegue y humo (solo lectura; nada de tareas reales)
1. `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `20FFAA7B…F361168F`. Agregar las propiedades (§3). Desplegar.
   «Last enabled at»; log sin `ERROR`/`ORA-`.
2. Ficha del socio: abrir uno, ver los 4 campos nuevos; guardar sexo y modalidad en un socio y reabrir (es una
   corrección de ficha real, no una prueba contable).
3. Contrato de adhesión: abrir uno → tarjeta «Firma electrónica» → «Sin trámite de firma». **No** enviar todavía.
4. Parametrización de créditos → «Pruebas Security Data»: paso 0 (configuración, sin clave) y, con `pruebas=true`,
   paso 1 (autenticar → 200, token enmascarado). **No** ejecutar «Crear tarea» ni «Cancelar».

## 6. Riesgos y pendientes
- Nada corrió contra Oracle en WildFly. Contra Security Data (desarrollo) solo: login, validación del socio, búsqueda de
  firmas y detalle de tarea. **Crear tarea nunca se ejecutó.**
- **Bloqueo del proveedor:** el plan de desarrollo rechaza la firma de persona natural («máximo 0 firmantes»). Hasta que
  Security Data lo habilite, todo envío termina en `FIRMANTE_RECHAZADO`.
- La primera tarea real se hace con una persona autorizada, y se revisa: posiciones de las firmas en el PDF y que el
  devuelto traiga la firma del socio y la del representante.
- Sexo y modalidad laboral nacen vacíos para todos los socios: el Anexo I sale con esas casillas sin marcar hasta que se
  llenen.
