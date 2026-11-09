# Despliegue del EAR «estado APROBADO y correcciones de otorgamiento» (2026-11-09)

**omen-saa-1-arb.** Se armó sobre el EAR desplegado `otorga-1107`. Cambian los dos WAR. Reemplaza al `niveles-1108`
(que no se desplegó: su cambio va aquí).

## 1. El EAR
- `F:\ears\asoprep\aprobado-1109\SaaEAR.ear` — SHA-256 `1F7871FFF5C5132DDECEC91627854B2BD1079526F91B9B6F40F5CA3B26FDB0BC`
  (partido en `aprobado-1109\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `E9273B53C363756F71864B6F335080B8F4E4C0F3CB9F71D7E25330E5C01F9FE0` — saaBE `main` en `bebb1fc4`.
  - `SaaFE.war` `A0CB2D35FA1F944ACFA39B59010E3CBC067DE137CE03D99775C37A5CC4CD2854` — saaFE `main` en `fcd8b5a`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war anterior).
- **Vuelta atrás:** `F:\ears\asoprep\otorga-1107\SaaEAR.ear` (`D71A05D5…8C787FC1`). Ver §5 antes de volver.

## 2. ⛔ ANTES del EAR: scripts 371 y 372 (bloque por bloque, leyendo cada control)
1. `crd/sql/371_ESTADO_APROBADO_14.sql` — estado 14 APROBADO en `CRD.ESPS` (y en el rubro SCP si se identifica).
   **Bloque 5 (solo lectura): préstamos VIGENTES con orden de desembolso no confirmada.** Si sale alguno, avisar al
   árbitro ANTES del EAR (el código nuevo no los toca: siguen vigentes, ya tienen su asiento de entrega).
2. `crd/sql/372_DDL_RGAP_OPERACION.sql` — columna `RGAP.RGAPTPOP`. **Sin ella, toda lectura de reglas de niveles da
   ORA-00904** (pantalla de niveles, aprobar, imprimir la solicitud).

## 3. Qué trae
| Cambio | Commit |
|---|---|
| Pantalla «Niveles de Aprobación» para todo usuario con el permiso de Parametrización de crédito | FE `8951965` |
| Primera cuota en el mes de inicio si el archivo Petro de ese mes y filial no se generó (al mes siguiente si ya se generó); 422 al aprobar si quedó fuera de un archivo generado | BE `83c388c1` |
| El archivo Petro deja de cobrar préstamos en estado GENERADO | BE `83c388c1` |
| Autorización de descuento sin la sección 4 (se queda «Distribución») | BE `83c388c1` |
| PDF del simulador de novación | BE `83c388c1`, FE `f2f518f` |
| Cupo de cuenta individual solo con quirografarios y emergentes; no aplica a hipotecario, prendario ni CENAPRO; capacidad de pago sin cambio | BE `9a6e84ad`, FE `1a3d8fd` |
| Preview y listado de desgravamen en una sola consulta (mismo resultado) | BE `9a6e84ad` |
| **Estado 14 APROBADO**: el préstamo nuevo o la novación queda APROBADO al último nivel y pasa a VIGENTE cuando tesorería confirma el pago del desembolso (asiento de entrega con la fecha del pago; en novación se cancela el original ahí). Reestructuración sin cambio. Anular en APROBADO. Reverso controlado. Un préstamo no desembolsado (1, 6, 7, 14) no cuenta en pagos, mora, carga Petro, devolución de aportes, certificados, app móvil ni cartera | BE `2140537c`, FE `1a3d8fd` |
| Aviso persistente a tesorería con las advertencias del desembolso (aprobar pagos, confirmación manual, respuesta del banco) | FE `fcd8b5a` |
| Criterio «Operación» (nuevo / novación / reestructuración) en las reglas de niveles de aprobación | BE `bebb1fc4`, FE `26e007c` |

## 4. Despliegue
1. Correr 371 y 372 (§2) y verificar sus controles. Pasar al árbitro la salida del bloque 5 del 371.
2. Desplegar `aprobado-1109\SaaEAR.ear`. Propiedades de sistema sin cambios.
3. Verificar: `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `1F7871FF…26FDB0BC`; «Last enabled at» con la hora
   del despliegue; log sin `ERROR`/`ORA-` desde el despliegue.
4. Humo (solo lectura, sin grabar nada de prueba):
   - Créditos → Parametrización → Niveles de Aprobación con un usuario distinto del 1 → abre; columna «Operación»;
     «Probar» con Operación = Novación → responde (sin reglas: «sin reglas activas»).
   - Imprimir la solicitud y la autorización de un préstamo existente → salen (autorización sin sección 4).
   - Simulador de crédito → Novación → simular → «Exportar PDF» descarga el PDF.
   - Pólizas → Desgravamen → vista previa → responde en segundos.
   - Abrir un préstamo con hipotecario y quirografario → «Evaluación del otorgamiento» muestra «Saldo quirografarios y
     emergentes».
5. El primer préstamo real del día valida el flujo nuevo: aprobar → queda **APROBADO** (chip ámbar) → tesorería
   confirma el pago → pasa a **VIGENTE**, con el asiento de entrega fechado el día del pago. Mirar que tesorería no
   reciba advertencias inesperadas.

## 5. Riesgos y vuelta atrás
- **Nada corrió contra Oracle ni en WildFly**: solo compila; los reportes pasaron compilar + verificar-fill y PDF con
  datos de prueba; el desgravamen se comparó contra la copia de desarrollo (0 diferencias). Sin probar: el aviso desde
  tesorería (hook), activar/revertir el desembolso, la novación al desembolsar, la consulta de GNAP/CXPG de la primera
  cuota.
- **Volver a `otorga-1107` con préstamos en estado 14** deja esos préstamos sin flujo (el EAR viejo no conoce el 14):
  antes de volver, `SELECT PRSTCDGO FROM CRD.PRST WHERE PRSTIDST = 14;` y avisar al árbitro. Los scripts 371/372 pueden
  quedarse (el EAR viejo no los lee).
- Un desembolso que se confirma con fecha en un mes de cartera cerrado falla (igual que cualquier movimiento de
  cartera en mes cerrado).
