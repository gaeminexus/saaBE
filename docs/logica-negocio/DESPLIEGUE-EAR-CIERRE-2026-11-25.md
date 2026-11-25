# Despliegue del EAR «cierre-1125»: todo `main` para el cierre de noviembre 2026 en producción

**omen-saa-1-arb, 2026-11-25.** Lo eligió el usuario: la opción (b), con todo `main`.
- Reemplaza a `c27b-1125c`, `c27b-1125b`, `c27b-1125`, `c42-1124` y `cancelados-1124`. **Ninguno se desplegó.**
- Lo de adentro está descrito en `DESPLIEGUE-EAR-C27B-2026-11-24.md` §4 y en `PLAN-CONTABILIDAD-PRODUCCION-2026-11-24.md`.
- **Agrega:** el lote 7 (tope de la provisión de mora, D-186) y la C19b (B17 y flujo de caja en el motor de balances).

## 1. El EAR
- `F:\ears\asoprep\cierre-1125\SaaEAR.ear`, SHA-256 `72D42219FD19BAEC69FEACE0516A401FAEAFC319D73B0D0279F90CC4D3E76B34`
  (62.500.908 bytes).
  - 3 partes en `partes\`. Se unen con `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`.
  - `testzip` sin errores.
- `saaBE.war` `6738F8526ADECE21C7FC110EAD0209DF929B71E373F41E6E0A1D84B56F2FF996`.
  - saaBE `main` `37c540f3`, empaquetado con `mvn -o package` sobre un `git archive` limpio.
  - No lleva el USAP de app1.
- `SaaFE.war` `69B9403C…84276410`: saaFE `63a225d`. Desde ahí el FE solo cambió documentos.
- **Vuelta atrás:** `precancelar-vencido-1118`.

## 2. Lo que dijo el `crd/sql/382` en producción (25-11)

| Bloque | Resultado | Consecuencia |
|---|---|---|
| 1-2 | Plantilla 25, línea 70 → **4.1.01.30** (inversiones) | Falta **def9-07** |
| 3 | No existe la cuenta 4.8.90.90.45 | Lo mismo: **def9-07** |
| 4 | `CK_ANCC_TPOO` 1..10 | El 383 lo amplía a 1..12 |
| 5 | `CK_SG45_ORGN` sin CONFIRMADO/PENDIENTE; no hay SSTG ni SG45TPPR | Faltan **C39-01, C40-01 y C41-01** (esperado) |
| 6 | **LNNV, HCNV, NTNV, HCNVTPAP, HCNVCNDN y HDTP.EVPRCDGO nulo: todos en 0** | ⛔ **Faltan los 9 scripts del cuadre (C34…, 380, C37-01, C38-01).** Sin ellos, Seguimiento Novedades da ORA-00904 y el ⑦ y la historización de cuotas fallan |
| 7 | Corridas EJECUTADAS 1 (2026-07-31), 2 (08-31), 4 (09-30) y 24 (10-31) | `anclaArranque = 2026-07-31`, **si la corrida 1 fue la de arranque** (confirmar). El próximo cierre es **noviembre 2026** |
| 8 | Tipos 1 Q (823 vivos), 2 H (393), 3 P (2), 4 y 5 (0) | Base `1:0;2:0;3:0;4:0;5:0` |
| 9 | Plantilla 17 con papeles 10 y 30 para los tipos 1, 2 y 3 | OK: los tipos 4 y 5 no tienen préstamos |
| props | Leídas en la consola de WildFly (25-11). `saa.cartera.alCorte = nunca`, `saa.cartera.neteoPorPrestamo = nunca`, `saa.cartera.inicioDevengoMora = 2025-05-31`. **No están** `devengoMoraCierre`, `corteInclusivo`, `corteExclusivoDesde` ni `moraNoCobradaHasta` | ⚠️ Con `alCorte = nunca`, producción **nunca** corrió ⑧, ⑨ ni ⑩: la 1399 nunca se ajustó a la G48 por el cierre. Pasar a `siempre` (bloque 7 del encargo) cambia el cierre de noviembre. Ver §4 |
| firma | `saa.firma.securitydata.pruebas = true` y la URL apunta a `portal-bpmdesarrollo…` | ⚠️ **Hallazgo:** la firma electrónica de producción usa el ambiente de PRUEBAS de Security Data |

## 3. ⛔ Scripts ANTES del WAR, en este orden (bloque por bloque, cada uno con sus controles)

1. **Los 9 del cuadre, en el orden de `DESPLIEGUE-EAR-CANCELADOS-2026-11-24.md` §2:** `cnt/sql/C34-01`, `C34-01b`,
   `C34-07`, `C34-07b`, `C34-07c`, `C34-07d`, `crd/sql/380`, `sri/sql/C37-01` y `rpr/sql/C38-01`.
   - ⚠ El `C38-01` escribe en `CNT.CRRP`: va antes de la C19 (punto 6).
2. `reportes/sql/C39-01`, luego `C40-01` (revisar el bloque 3 y hacer COMMIT), luego `C41-01`.
3. **`crd/sql/C45-03` (OBLIGATORIO)** y después `crd/sql/C45-02`.
4. `cuadre/def9-07` (cuenta 4.8.90.90.45 y línea 70 de la 25) y `cuadre/def9-09` (4.8.99.9, con su fila CRRP 489090).
   El def9-09 escribe en CRRP, así que va antes de la C19.
5. **U1–U7 del pase del cuadre (`cuadre/README.md` §3), si no corrieron el 1-nov.** El U6 y el 4.15 escriben en CRRP:
   van antes de la C19. **Confirmar con el cuadre cuáles ya están en producción.**
6. **C19b.** Depende de lo que dé el SELECT de §3.1:
   - **Si existe `CNT.CRRP`** (lo más probable): `rpr/sql/C19-05-migrar-crrp-a-dtrp.sql`, que migra a `CNT.DTRP`
     todas las reglas activas, incluidas las de los puntos 1, 4 y 5. No van ni C19-01 ni C19-07.
     - Antes del WAR, generar y anotar los balances del mes con el WAR viejo (`C19-06`, paso 1), para compararlos
       después.
   - **Si NO existe `CNT.CRRP`:** C19-01 → C19-02 → C19-03 → C19-04 → C38-01 sin sus bloques 5 y 7 → C19-07. Es el
     orden de la cabecera del C19-07. En ese caso, el C38-01 del punto 1 se corre así recortado.
7. `crd/sql/383_C27B_INTERES_ANTICIPADO_PLANTILLA_39_Y_SUBPROCESOS_11_12.sql`: plantilla 39 con 2.9.01.05, más
   `CK_ANCC_TPOO` 1..12. El alterno 39 ya se verificó libre en producción.
8. `crd/sql/392_MVIC_TIPO_10_AJUSTE_TOPE_PROVISION.sql` (`CK_MVIC_TIPO` 1..10).
9. `cnt/sql/omen1-02-hcpr-accion-desmayorizar.sql`.

### 3.1 SELECT para decidir el camino de la C19 (solo lectura)
```sql
SELECT table_name FROM all_tables WHERE owner = 'CNT' AND table_name IN ('CRRP', 'DTRP', 'RNRP') ORDER BY 1;
SELECT column_name FROM all_tab_columns WHERE owner = 'CNT' AND table_name = 'RPRT'
   AND column_name IN ('RPRTTPRP', 'RPRTCDES', 'RPRTFRAR', 'RPRTTTLO') ORDER BY 1;
SELECT COUNT(*) reglas_crrp FROM CNT.CRRP;   -- solo si CRRP existe
```

## 4. Propiedades del servidor

**Para leerlas,** desde la carpeta `bin` del WildFly de producción (el `jboss-cli` no está en el PATH):
```powershell
cd "<WILDFLY_HOME>\bin"
.\jboss-cli.bat -c --command="/core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)" > $env:TEMP\props.txt
Select-String -Path $env:TEMP\props.txt -Pattern "saa\.cartera"
```
- Si no sabes dónde está WildFly:
  `Get-ChildItem C:\,D:\ -Filter jboss-cli.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 3 FullName`.
- Sin cli, en la configuración:
  `Select-String -Path "<WILDFLY_HOME>\standalone\configuration\standalone*.xml","<WILDFLY_HOME>\bin\standalone.conf.bat" -Pattern "saa\.cartera"`.

**Valores a fijar:**

| Propiedad | Valor en producción |
|---|---|
| `saa.cartera.devengoCompletoDesde` | `2026-11-30` |
| `saa.cartera.topeProvisionMoraDesde` | `2026-11-30` |
| `saa.cartera.anclaArranque` | `2026-07-31` (corrida 1), si fue la de arranque |
| `saa.cartera.anticipadoBaseTransicion` | `1:0;2:0;3:0;4:0;5:0` |
| `saa.cartera.alCorte` | **Hoy `nunca`.** El encargo pide `siempre`, para que corran ⑧ (provisión de cartera contra la G48), ⑨ (ajuste de bandas a saldo) y ⑩ (devengo de mora al corte). ⚠️ Sería la **primera vez** que esos tres pasos corren en producción: el primer ⑧/⑨ ajusta a saldo todo lo acumulado y puede dar asientos grandes. **Decisión del líder, con una previsualización antes.** C27b y el lote 7 no dependen de esta propiedad |
| `saa.cartera.neteoPorPrestamo` | Hoy `nunca`. Confirmar con el cuadre si D-032 (neteo por préstamo) debe activarse en producción |
| Cómo se agregan | En la consola de WildFly, System Properties → Add (la misma pantalla de la captura). El código las lee en cada cierre, sin reiniciar |
| `saa.cartera.corteInclusivo`, `saa.cartera.moraNoCobradaHasta`, `saa.cartera.timerMoraSuspendido` | **NO** se definen |

## 5. Humo (solo lectura) y cierre
1. Verificar el hash y desplegar. Log del arranque sin `ERROR` ni `ORA-`. Ojo con dos cosas:
   - las inyecciones circulares (acuerdo ↔ pagos; cierre del período ↔ mayorización);
   - las entidades nuevas de la C19b (`RNRP`).
2. Seguimiento Novedades v1 y v2.
3. **Balances:** con la C19b, generar el B17 y el balance general del mes y compararlos con lo anotado antes (`C19-06`).
4. Préstamos Cancelados del mes anterior contra el bloque 1 del `381`.
5. **Cierre de noviembre 2026:**
   - **previsualizar** primero;
   - en las advertencias, ⑪ (objetivo, asentado y base), que aparezca ④b y que el ⑦ ya venga topado;
   - recién entonces ejecutar, y correr el `384` y el `391` sobre el 2026-11-30.
6. Sin cobros ni asientos de prueba.

## 6. Riesgos
- **Nada de lo nuevo corrió contra Oracle** (C27b, lote 7, C19b). La previsualización del cierre es la primera
  ejecución real de ④, ④b, ⑪ y del ⑦ topado.
- **Con la C19b cambia el motor del B17 y del flujo de caja.** `/esrp` y `/esfo` quedan como alias.
- **El lote 3 cambia la mora diaria desde el primer disparo del timer.**
- **El exceso de mora ya provisionado en producción no se corrige con esta versión.** El ⑦ topado no provisiona de
  más, pero no reversa lo que ya está. El líder dice que eso se reemplaza con la base re-procesada.
