# Despliegue del EAR «firmas de la solicitud y del pagaré + rol detallado» (2026-11-10)

**omen-saa-3-arb.** Se armó sobre `retenida-1109` (`DESPLIEGUE-EAR-RETENIDA-2026-11-09.md`). **Solo cambia `saaBE.war`.**

## 1. El EAR
- `F:\ears\asoprep\firmas-1110\SaaEAR.ear` — SHA-256 `CE864F6F731B07ED40238C5C253D403BB068304322E9B985A679B192DC2BC4D4`
  (partido en `firmas-1110\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `40F7E5EE95C8F3F4CA4D8FCDF8820A1A765327D148F0535E3725F51F31531A65` — saaBE `main` en `c2634125`
    (`mvn -o package` sobre un `git archive` limpio de ese commit, fuera del árbol de trabajo).
  - `SaaFE.war` `8AE94DDC…2D4622` — **el mismo** de `retenida-1109` (saaFE `main` en `3983691`, sin commits nuevos;
    la tarjeta «Rol detallado», `c376275`, ya venía ahí).
- Contra el `saaBE.war` de `retenida-1109`, ignorando finales de línea, cambian **solo seis archivos**:
  `rep/crd/RPRT_PRST_PGRE`, `rep/crd/RPRT_PRST_SLCT` y `rep/rhh/RPRT_ROLL_DTLL` (`.jrxml` + `.jasper` de cada uno). Ninguna clase.
- **Vuelta atrás:** `F:\ears\asoprep\retenida-1109\SaaEAR.ear` (`713B2E21…E0EF6823`). Sin scripts ni datos de por medio.

## 2. Antes del EAR
- **Ningún script nuevo.** Pero este EAR **contiene todo lo de `retenida-1109`**: si ese no se desplegó todavía, el
  `crd/sql/374` es obligatorio antes (sin él, cualquier lectura de PGPC da ORA-00904). Ver §2 de su ficha.

## 3. Qué trae (además de lo de `retenida-1109`)
| Cambio | Commit |
|---|---|
| Rol de pagos detallado (RRHH › Reportes de nómina › «Rol detallado»), con columnas fijas HORAS EXTRAS (100 %) y HORAS COMPLEM. (50 %) | `3d2e8f86` (el `.jasper` del `98873f8e` ya iba en `retenida-1109`) |
| Pagaré: sin firma ni mención del cónyuge en quirografarios y emergentes (código Petro `PQ`/`PE`) | `a570d587` |
| Solicitud de crédito: firmas de ASOPREP sin nombres de usuario ni cargos (solo «Elaborado por», «Revisado por», «Aprobado por») | `a570d587`, `c2634125` |

Diseños: `reportes/REPORTE-ROL-PAGO-DETALLADO.md`, `crd/AJUSTE-FIRMAS-SOLICITUD-PAGARE.md`.

## 4. Despliegue y humo (solo lectura)
1. `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `CE864F6F…2BC4D4`. Desplegar. Revisar «Last enabled at» y el log sin `ERROR`/`ORA-`.
2. Préstamos: imprimir la solicitud + pagaré de un **quirografario o emergente con cónyuge** (sin cónyuge en el pagaré)
   y de un **hipotecario/prendario con cónyuge** (con cónyuge, igual que antes). En la solicitud, la sección 7 con las tres
   firmas sin nombres ni cargos.
3. RRHH › Reportes de nómina › Rol detallado → octubre: comparar totales contra el Rol consolidado.

## 5. Riesgos
- Los tres reportes compilan, pasan el fill y se miraron en PDF con datos ficticios; **ninguno corrió contra Oracle**.
- `compilar-jasper.bat` no compila el pagaré ni la solicitud (no encuentra `NumeroALetrasUtil`) y el fill posterior da
  un **falso OK** sobre el `.jasper` viejo; se compilaron a mano con esa clase en el classpath.
