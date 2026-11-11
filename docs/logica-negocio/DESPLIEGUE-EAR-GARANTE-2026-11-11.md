# Despliegue del EAR «pago de préstamo con aportes del garante» (2026-11-11)

> **Último: `garante-cobros-1111`** = todo `main` (saaBE `6b5cbed8`, saaFE `a133e12`) sobre `adhesion-1110`. Suma a
> `garante-final-1111` el garante en **Cobros personales** (G6): «Pagar cuotas» → «Saldo de aportes» y «Cancelar el
> crédito» (100 % aportes) con aportes del garante (`idGarante` en `POST prst/precancelar`). Contra `garante-final-1111`
> cambian solo `SolicitudPrecancelacion`, `ProcesoPagoPrestamoServiceImpl` y `ContabilidadPrestamoServiceImpl`. Sin SQL
> nuevo (el `e3-17` sigue siendo necesario si no se corrió).
> - `F:\ears\asoprep\garante-cobros-1111\SaaEAR.ear` — SHA-256 `F59D666C257B57302F114B92593E26B5832DE6913AC95F1A35A1974120AF2C45`
>   (3 partes en `garante-cobros-1111\partes`). `saaBE.war` `036401B9DD5385EAA1F47F3E36CD9AC08FDCF22B41F0511328E9C8A9E9C66481`,
>   `SaaFE.war` `4D6603D43BB8847C80D534070F5BFE140DBD052362351F7CEE3841C11295D47C`.
> - Vuelta atrás: `seguro-garante-1111` (`7A9399D0…5968E139`), el desplegado.
> - Humo extra: Cobros personales → préstamo con garante → «Pagar cuotas» → «Saldo de aportes» muestra «Aportes del
>   deudor / Aportes del garante»; «Cancelar el crédito» igual. No pagar de prueba.
>
> Anterior: **`garante-final-1111`** = todo `main` (saaBE `14084155`, saaFE `c623255`) sobre `adhesion-1110`. Suma a
> `seguro-garante-1111` (ya desplegado por el usuario) solo el aviso de comprometido del garante (G5: `GET
> prst/compromisoGarante` + aviso en la confirmación del cruce). Contra `seguro-garante-1111` cambian solo
> `ProcesoPagoPrestamoService(Impl)`, `PrestamoRest` y entra `ResultadoCompromisoGarante`. Sin SQL nuevo.
> - `F:\ears\asoprep\garante-final-1111\SaaEAR.ear` — SHA-256 `58A0574E1057925200A3A878EE8C18ABB4B5596FEA047518F0ABA0E56A1BB58D`
>   (3 partes en `garante-final-1111\partes`). `saaBE.war` `C05294A2F91D9F2FC3361EC23B7A0F69877D5F61900AAB2736075B6195029FDF`,
>   `SaaFE.war` `405261F515F4920B805510857772E863542292425FADD0037B4C72857549183B`.
> - Vuelta atrás: `seguro-garante-1111` (`7A9399D0…5968E139`).
>
> Anterior: **`seguro-garante-1111`** (pedido del usuario: incluir el cambio de omen1 del seguro de incendio).
> Es `garante2-1111` con **solo `saaBE.war` reemplazado** por saaBE `main` en `eabd4722` (agrega omen1 `ab49c8f5`,
> `eabd4722`: el seguro de incendio incluye los hipotecarios DE PLAZO VENCIDO; **sin SQL ni DDL**, ver
> `crd/DISENO-SEGURO-INCENDIO-PLAZO-VENCIDO.md`). Contra `garante2-1111` cambia solo `DocumentoSeguroServiceImpl` (y sus
> clases internas). SaaFE.war idéntico (`3365CE04…D8920173`, saaFE `903edbc`, sin commits nuevos).
> - `F:\ears\asoprep\seguro-garante-1111\SaaEAR.ear` — SHA-256 `7A9399D044DBE174FBE304D7222B244A60381F04795B634F504D94BF5968E139`
>   (3 partes en `seguro-garante-1111\partes`). `saaBE.war` `74A072FA38436DB43167EC8883BE8C33CF8BB2CA8F60DEEE3106DF42BBA43898`.
> - `garante-1111` y `garante2-1111` quedan descartados. El resto de esta ficha (scripts, qué trae, humo) sigue valiendo.
>
> Histórico: se iba a desplegar `garante2-1111`, NO `garante-1111` (decisión del usuario: subir junto con el garante en préstamos ya
> otorgados, G4). `garante-1111` queda descartado.

**omen-saa-3-arb.** Se armó sobre `adhesion-1110` (omen1, `DESPLIEGUE-EAR-ADHESION-2026-11-10.md`), que contiene
`firmas-1110` y `retenida-1109`. Cambian los dos WAR. Es **todo `main`** de los dos repos al 2026-11-11.

## 1. El EAR
- `F:\ears\asoprep\garante2-1111\SaaEAR.ear` — SHA-256 `59FE01DAB8C71F9842804F74AFD09A8C7558D7FC996135BFBAEB575EA7E5486F`
  (partido en `garante2-1111\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `3790FE8880F6B907E7F1EF0739133670D5DAE9CB008A5815D3C535D6877D7957` — saaBE `main` en `015ae1da`
    (`mvn -o package` sobre un `git archive` limpio). Contra `adhesion-1110` cambian solo las clases de
    `ContextoPago`, `SolicitudPagoConAportes`, `GarantePrestamoServiceImpl`, `ProcesoPagoPrestamoServiceImpl` y
    `ContabilidadPrestamoServiceImpl`, `GarantePrestamoService(Impl)` y `GarantePrestamoRest` (más clases internas, por números
    de línea) y entran `SolicitudGaranteConAutorizacion` y `ResultadoGaranteConAutorizacion`.
  - `SaaFE.war` `3365CE0493701FFC0705ABC39E3DCC6A425E3868E1197145FEDA7E72D8920173` — saaFE `main` en `903edbc`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war de `adhesion-1110`).
- **Vuelta atrás:** `F:\ears\asoprep\adhesion-1110\SaaEAR.ear` (`20FFAA7B…F361168F`). Sin scripts ni datos de por medio
  (los pagos con aportes del garante hechos con este EAR el viejo los anula igual: el reverso usa la entidad de la fila APRT).

## 2. Antes del EAR
- **`crd/sql/e3-17-tipo-adjunto-autorizacion-garante.sql`** — tipo de adjunto «AUTORIZACION DEL GARANTE» (idempotente;
  control final = 1 fila). Sin él el EAR arranca igual; solo el alta de garante en préstamos ya otorgados responde
  `TIPO_ADJUNTO_AUTORIZACION_NO_CONFIGURADO`.
- Los de `adhesion-1110` (376, 377) y el 374 ya estaban corridos.
- Propiedades de sistema: sin cambios.

## 3. Qué trae (además de `adhesion-1110`)
| Cambio | Commit |
|---|---|
| Pago de préstamo con aportes del garante: `idGarante` opcional en `POST /prst/pagarConAportes` (garante activo, del préstamo y partícipe; autorización firmada obligatoria); consume los aportes del garante y paga las cuotas del deudor; APRT/PGAP, evento, huella y asiento nombran al garante | BE `d0bd20a4` |
| Cruce de valores: «Pagar con: aportes del deudor / aportes del garante», saldos del garante, respaldo obligatorio, confirmación explícita, origen de los aportes en el resultado | FE `c7d8e69` |
| Garante en préstamos ya otorgados (2, 8, 10, 11): partícipe + autorización firmada obligatoria en una sola transacción (`POST /grpr/conAutorizacion`, adjunto en `CRD.ADJN`), «Ver autorización», anular y agregar otro; el pagaré no se reimprime | BE `015ae1da`, FE `903edbc` |
| El garante es siempre partícipe: sin modo «tercero»; los históricos sin partícipe salen en solo lectura con «Anular y agregar otro» | BE `d0bd20a4`, FE `c7d8e69` |

Diseño: `crd/DISENO-PAGO-CON-APORTES-DEL-GARANTE.md`.

## 4. Despliegue y humo (solo lectura; sin pagos de prueba)
1. `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `59FE01DA…EA7E5486F`. Desplegar. «Last enabled at»; log sin `ERROR`/`ORA-`.
2. Préstamo con garante: la pantalla del garante ya no ofrece «tercero». En un préstamo vigente la sección «Garante»
   pide partícipe + autorización (no guardar uno de prueba: solo uno real con su autorización firmada).
3. Cruce de valores → deudor con préstamo con garante partícipe → aparece «Pagar con…»; al elegir el garante cambian
   los saldos. **No cruzar** salvo un caso real.
4. Primer caso real: revisarlo con el árbitro (APRT negativa en el garante, cuotas del deudor pagadas, asiento con la
   glosa del garante).

## 5. Riesgos
- Compila y arma; **nada corrió contra Oracle**. El cruce normal (sin garante) no cambia de código.
- El diálogo «Pagar con aportes» por fila (`pago-prestamo-dialog`) sigue solo con aportes del deudor.
- No hay aviso de «garante por debajo de su comprometido» (el FE no tiene ese dato sin reconstruir sus saldos).
