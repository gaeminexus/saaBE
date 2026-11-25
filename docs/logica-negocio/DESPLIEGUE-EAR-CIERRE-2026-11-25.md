# Despliegue del EAR «cierre-1125-d187» para el cierre de noviembre 2026 en producción

**omen-saa-1-arb, 2026-11-25.** El EAR sigue la decisión D-187 del líder del cuadre, confirmada por el usuario.
- Lleva el mismo código que el WAR del 8080 del reproceso: `5a40b3ac` + lote 7 + lote 7b, **sin la C19b**.
- La C19b sigue en `main` y va en el EAR siguiente. Antes se prueba en el reproceso: el B17 de mayo y el de septiembre
  2025 tienen que salir idénticos byte a byte.
- **Es la única excepción a «una sola versión».**
- Reemplaza a `cierre-1125` (todo main con C19b), `c27b-1125c`, `c27b-1125b`, `c27b-1125`, `c42-1124` y
  `cancelados-1124`. Ninguno se desplegó.

## 1. El EAR
- `F:\ears\asoprep\cierre-1125-d187\SaaEAR.ear`, SHA-256
  `563599A49830476A200D105A5F15D4D8D0A4BC9CBCFA40BBC8D822AAAA5A701A` (62.494.036 bytes).
  - 3 partes en `partes\`. Se unen con `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`.
  - `testzip` sin errores.
- `saaBE.war` `EF3BC844C2867E5571806074109F57DEE3F284DA393514D3F9BAF7759D6AD336`.
  - Es un `git archive` de `5a40b3ac` con los commits `37c540f3` (lote 7) y `c728ba3a` (lote 7b) aplicados con
    `git apply`.
  - Los dos parches están junto al EAR (`lote7.patch`, `lote7b.patch`), para que se pueda reproducir.
  - Empaquetado con `mvn -o package`. No lleva la C19b ni el USAP de app1.
- `SaaFE.war` `69B9403C…84276410`: saaFE `63a225d`.
- **Vuelta atrás:** `precancelar-vencido-1118`.

## 2. Lo que dijo el `crd/sql/382` en producción (25-11) y lo que decidió el líder

| Tema | Producción | Consecuencia |
|---|---|---|
| Plantilla 25, línea 70 | → 4.1.01.30. No existe la 4.8.90.90.45 | Falta **def9-07** |
| `CK_ANCC_TPOO` | 1..10 | El 383 lo amplía a 1..12 |
| C39–C41 | No corridos | Faltan **C39-01, C40-01 y C41-01** |
| C34, 380, C37-01, C38-01 | **No corridos** (todo en 0) | ⛔ Van antes del EAR. U1–U9 sí corrieron el 12-nov |
| Corridas | 1 (2026-07-31) es la primera de producción | `anclaArranque = 2026-07-31` (confirmado por el líder) |
| Tipos de préstamo | 1, 2 y 3 con préstamos vivos; 4 y 5 en 0 | Base `1:0;2:0;3:0;4:0;5:0` |
| Plantilla 17 | Papeles 10 y 30 para los tipos 1, 2 y 3 | OK |
| Alterno 39 | Libre | El 383 puede correr |
| Propiedades | `alCorte = nunca`, `neteoPorPrestamo = nunca`, `inicioDevengoMora = 2025-05-31` | Ver §4 |
| Firma electrónica | `saa.firma.securitydata.pruebas = true`, URL de desarrollo | Intencional: Security Data sigue en pruebas (lo confirmó el usuario el 25-11). No es hallazgo |

## 3. ⛔ Scripts ANTES del WAR, en este orden (bloque por bloque, cada uno con sus controles)
1. **Los 9 del cuadre:** `cnt/sql/C34-01-novedades-v2.sql`, `C34-01b`, `C34-07`, `C34-07b`, `C34-07c`, `C34-07d`,
   `crd/sql/380_C35_HDTP_EVENTO_OPCIONAL.sql`, `sri/sql/C37-01-…` y `rpr/sql/C38-01-…`.
2. `reportes/sql/C39-01`, luego `C40-01` (revisar el bloque 3 y hacer COMMIT), luego `C41-01`.
3. **`crd/sql/C45-03-D053-lineas-redondeo-4.8.99.08.sql` (OBLIGATORIO)** y después `crd/sql/C45-02-…`.
4. `cuadre/def9-07-cuenta-4.8.90.90.45-perdidas-condonaciones.sql` y `cuadre/def9-09-cuenta-4.8.99.9-gnd-multas.sql`.
5. `crd/sql/383_C27B_INTERES_ANTICIPADO_PLANTILLA_39_Y_SUBPROCESOS_11_12.sql`.
6. `crd/sql/392_MVIC_TIPO_10_AJUSTE_TOPE_PROVISION.sql`.
7. `cnt/sql/omen1-02-hcpr-accion-desmayorizar.sql`.

**No van** los `rpr/sql/C19-*` de `main`, que son de la C19b.

⚠️ **Corrección (25-11): el `C38-01` necesita antes el `C19-01` y el `C19-02` de la C19 vieja,** la de `5a40b3ac`,
porque escribe en `CNT.RNRP`.
- En producción probablemente ya están: el U6 del 12-nov escribió en `CNT.CRRP`.
- El script `00` de la carpeta lo verifica. Si faltan, se corren las versiones de `5a40b3ac`, nunca las de `main`.

**Carpeta lista para correr:** `F:\ears\asoprep\cierre-1125-d187\scripts\`. Lleva los scripts numerados en orden
(`00` solo lectura; `01`…`19`, con `09a` y `09b` condicionales) y un `LEEME.txt` con el COMMIT de cada uno.

## 4. Propiedades (consola de WildFly → System Properties → Add; el código las lee en cada cierre)

| Propiedad | Valor | Cuándo |
|---|---|---|
| `saa.cartera.devengoCompletoDesde` | `2026-11-30` | Antes de previsualizar noviembre |
| `saa.cartera.topeProvisionMoraDesde` | `2026-11-30` | Antes de previsualizar noviembre |
| `saa.cartera.anclaArranque` | `2026-07-31` | Antes de previsualizar noviembre |
| `saa.cartera.anticipadoBaseTransicion` | `1:0;2:0;3:0;4:0;5:0` | Antes de previsualizar noviembre |
| `saa.cartera.alCorte` | `nunca` → **`siempre`**, con la condición del §5 | Para la previsualización |
| `saa.cartera.neteoPorPrestamo` | **quitar** el `nunca` (queda `auto`) | Junto con `alCorte = siempre` |
| `saa.cartera.inicioDevengoMora` | `2025-05-31` (ya está) | — |
| `saa.cartera.corteInclusivo`, `saa.cierres.permitirFueraDeOrden`, `saa.cartera.moraNoCobradaHasta`, `saa.cartera.timerMoraSuspendido` | **NO** se definen | — |

**Estado al 25-11:** propiedades del §4 ya puestas en producción (verificado en la consola: alCorte=siempre, neteoPorPrestamo retirada, las cuatro de C27b y el tope de mora).

## 5. Cierre de noviembre 2026 (condición del líder sobre `alCorte`)
1. Desplegar y revisar que el log no tenga `ERROR` ni `ORA-`. Ojo con las inyecciones circulares (acuerdo ↔ pagos;
   cierre del período ↔ mayorización).
2. Humo de solo lectura:
   - Seguimiento Novedades v1 y v2;
   - Préstamos Cancelados del mes anterior contra el bloque 1 del `381`;
   - `GET /ejrc/validar` de un mes presentado.
3. Con las propiedades del §4, `alCorte = siempre` y sin `neteoPorPrestamo`, **PREVISUALIZAR** noviembre (no graba).
   Revisar:
   - **⑧** provisión requerida contra constituida, **⑨** ajuste de bandas y **⑥** el «abierto» del neteo;
   - **④** con el interés completo, que aparezca **④b**, la línea del **⑪** (objetivo, asentado y base) y que el **⑦**
     venga topado.
4. **Si ⑧, ⑨ o ⑥ salen en millones,** como en octubre:
   - volver `alCorte = nunca` y `neteoPorPrestamo = nunca`;
   - previsualizar de nuevo;
   - mandar la previsualización al cuadre.

   Si salen del orden de un mes del reproceso, ejecutar con `siempre`.
5. Después de ejecutar, correr el `384` y el `391` al 2026-11-30.
6. Sin cobros ni asientos de prueba.

## 5b. Previsualización de prueba, 25-11, con el EAR `cierre-1125-d187` y `alCorte=siempre`

**Corrió sin errores contra Oracle:** ①, ②, ①.1, ③, ④, ④b, ⑥, ⑩, ⑦, ⑪, ⑧ y ⑨. Todos los asientos cuadran.

**C27b, primera vez en producción:**
- ④ = 89.751,14 (Q 28.245,94 · H 61.505,20);
- ④b = 639,92;
- ⑪ = 823,89 (Q 224,73 · H 599,16), contra 2.9.01.05.

**Tres bloqueos de la ejecución:**

| Bloqueo | Detalle | Salida |
|---|---|---|
| DTPRSLOT sin respaldo | 7 cuotas, 38.401,75 | Script C35-02 del cuadre (no está en saaBE) |
| Prelación C33 rota | 97 préstamos | `POST /prelacioncuotas/reimputarAlCorte`, simulando primero, o la omisión del control con motivo. Lo decide el líder |
| Sin archivo Petro de 11/2026 | — | Esperado: el mes no terminó |

**Montos para la condición del líder:**
- **⑥ neteo:** 5.902.951,30. Está inflado porque todavía no se carga Petro. Pero hay **−1.405.742,53 «sin explicar»** entre la apertura recalculada por préstamo y el saldo de 1.4.05.10. Eso es independiente de Petro: es el primer neteo por préstamo en producción.
- **⑧ provisión de cartera:** +725.075,85 (Q 90.197,72 · H 634.878,13). Es el primer ⑧ en producción: requerida 3.999.904,65 contra constituida 3.274.828,80.
- **⑨ bandas:** 781.079,94 de reclasificación dentro de cada grupo. Diferencia contabilidad − módulo: H −53.163,03 y Q −78.846,25, a la banda .25.
- **⑩ devengo de mora:** 510.377,09 (Q 139.640,61 · H 363.580,94 · P 7.155,54). Es el primer ⑩ en producción: pone en 1.4.02 la mora acumulada desde `inicioDevengoMora`.
- **⑦:** 146.806,18, ya con el tope D-186.

**Se mandó al cuadre** por la condición del líder (⑥ con un millón sin explicar; ⑧, ⑨ y ⑩ grandes por ser la primera vez).

## 5c. D-188 (usuario, que es el líder del cuadre, 25-11): cómo se destraba la ejecución de noviembre

1. **DTPRSLOT:** se corre `cuadre/C35-02-PRODUCCION-NOV-2026.sql` (merge `31f8ac5a`; en la carpeta de scripts es el
   `20_…`).
   - Primero respalda; se detiene si no encuentra exactamente 7 cuotas y 38.401,75.
   - COMMIT después del bloque 03.
2. **Prelación C33 (97 préstamos):** `POST /rest/prelacioncuotas/reimputarAlCorte`, **SIMULANDO PRIMERO**.
   - Se ejecuta solo si la simulación reparte de nuevo entre las cuotas de cada préstamo sin cambiar el total cobrado
     por préstamo, el dinero ni los asientos de cobro (D-130/D-142).
   - Si mueve algo más, se para y se manda la simulación.
   - **Sin omisión con motivo** (confirmado por el usuario en la D-188 corregida).
3. **Petro de noviembre:** se carga antes de ejecutar el cierre, y después se vuelve a previsualizar.
4. **⑩ (510.377,09): ACEPTADO como ajuste de la primera vez (D-188 corregida, 25-11).** `saa.cartera.inicioDevengoMora`
   **se queda en 2025-05-31**. Desde mayo/junio de 2026 el sistema calcula y cobra la mora de todas las cuotas vencidas: es
   una cuenta por cobrar real que producción nunca había devengado. El ⑦ con tope la provisiona (149905 de mora = mora de
   1.4.02). La versión anterior de D-188 (bajar la propiedad a 2026-05-01) queda **anulada**.
5. **⑧ (+725.075,85) y ⑨ (781.079,94): ACEPTADOS** como ajuste de la primera vez.
6. **⑥:** con Petro cargado, si el «sin explicar» baja a menos de unos 1.000, `neteoPorPrestamo` sigue en `auto`. Si
   sigue en cientos de miles, `neteoPorPrestamo = nunca` para noviembre y se manda el detalle por préstamo del ⑥.

## 5d. ⛔ INCIDENTE 25-11 (madrugada): la re-imputación C33 empeoró la prelación

- **Qué se corrió:** `reimputarAlCorte` con `simular=false` al 2026-11-30, por recomendación del árbitro. La simulación
  salía limpia (sin pagos creados ni anulados, sin dinero sin destino, deltas en cero).
- **Resultado:** 95 préstamos re-imputados. La previsualización siguiente marcaba 59 rotos, 39 de ellos con una cuota
  **pagada de más** (2904, cuota 5: 950,21). El ① subió de 52.796 a 73.162.
- **Causa** (diagnóstico del BE, a confirmar con el `394`):
  - la herramienta lee los pagos por `PGPR.PRSTCDGO` (`selectVigentesByPrestamo`) y el control del cierre los lee por
    cuota (`PGPR.DTPRCDGO`);
  - los PGPR con `PRSTCDGO` nulo son invisibles para la herramienta, que imputó de nuevo encima de cuotas ya cubiertas;
  - su chequeo de residuo usa la misma lectura, así que no lo detectó.
- **Qué escribió:** PGPR, DTPR (mora recalculada AL 2026-11-30, cinco días en el futuro), MVIC (anuló devengos de
  cobro e insertó tipos 2 y 4), ASNT y DTAS (asientos de provisión por préstamo, fechados el 30-11) y HDTP.
- **Salida:** RESTAURACIÓN COMPLETA de la base desde el respaldo del usuario.
  - Es de después del C35-02 y antes de la re-imputación, y nadie más trabajaba (03:42).
  - No hay que volver a correr ningún script.
- **Lección:** antes de ejecutar una herramienta que reescribe pagos, comprobar que mide lo mismo que el control que
  se quiere destrabar: simular y **previsualizar con el resultado simulado**, no solo leer el resumen de la
  herramienta.
- **`reimputarAlCorte` NO se vuelve a correr en producción** hasta corregirlo (lote aparte: leer por cuota, el mismo
  criterio que el control y la mora a la fecha real).

## 5e. EAR con la herramienta corregida: `cierre-1125-d187-l10` (lote 10, `ee647164`)

- **EAR:** `F:\ears\asoprep\cierre-1125-d187-l10\SaaEAR.ear`, SHA-256
  `01C614D2E3DD250555A7B54B3A6B077E92A67A2C08AD7D1E8AC6D56B0A2D930F`.
  - Código: `5a40b3ac` + lote 7 + lote 7b + lote 10 (solo `src`, con `git apply`).
  - Los parches y el `397` están junto al EAR.
- **Orden, PRIMERO sobre una copia de producción y después en producción:**
  1. `396` (solo lectura);
  2. `397`: respaldo de PGPR y UPDATE de `PRSTCDGO` nulo desde la cuota; COMMIT manual;
  3. desplegar el EAR;
  4. `reimputarAlCorte` con `simular=true`;
  5. ejecutar con `simular=false`;
  6. previsualizar.
  - Aceptación: prelación en 0, sin «pagada de más». Revisar a mano el 2904 y el 3492.
- La herramienta corregida rechaza los préstamos con pagos de `PRSTCDGO` nulo (por eso el 397 va antes). Además
  valida contra el control del cierre antes y después de aplicar, con rollback por préstamo.

## 6. Riesgos
- **Lo nuevo nunca corrió contra Oracle en producción:** C27b, el lote 7 y su reparto por préstamo, y el ⑧, ⑨ y ⑩ con
  `siempre`. La previsualización es su primera ejecución, y no graba nada.
- **El exceso de mora ya provisionado en producción no se corrige con este EAR.** El ⑦ topado deja de provisionar de
  más. La corrección la trae la base re-procesada (decisión del líder).
- **El lote 3 cambia el cálculo diario de la mora** desde el primer disparo del timer.
- **Restricción de C27b:** volver a ejecutar un cierre ya ejecutado después de cancelaciones del mes siguiente se revisa
  antes con el `384`.
