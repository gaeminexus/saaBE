# Despliegue del EAR «pensión retenida de jubilados y cuenta de destino del desembolso» (2026-11-09)

**omen-saa-1-arb.** Se armó sobre el EAR desplegado `tablero-1109`. Cambian los dos WAR.

## 1. El EAR
- `F:\ears\asoprep\retenida-1109\SaaEAR.ear` — SHA-256 `713B2E21220ADBA284494FF6AC8A8F2DB1E769EE453549E268EF0A0EE0EF6823`
  (partido en `retenida-1109\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `3E1C2DE4D83BB9ADBAB130B261C9C80210FAF166FCCC2E8CA4A5481FA31CDF9F` — saaBE `main` en `98873f8e`
    (incluye un reporte nuevo de rhh de omen3, `RPRT_ROLL_DTLL`, aditivo).
  - `SaaFE.war` `8AE94DDC5603F478D41F2A2B3191E0CF7960EAB149B4171EE314AE380D2D4622` — saaFE `main` en `3983691`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war de `tablero-1109`).
- **Vuelta atrás:** `F:\ears\asoprep\tablero-1109\SaaEAR.ear` (`053BC05A…4C62C080`). Las columnas del 374 pueden quedarse
  (el EAR viejo no las lee), pero los pagos de retenidos hechos con el EAR nuevo no los entiende el viejo: avisar antes.

## 2. ⛔ ANTES del EAR: script 374 (bloque por bloque, leyendo cada control)
`crd/sql/374_DDL_PGPC_PENSION_RETENIDA.sql` — columnas `PGPCVLRT`/`PGPCDVRT` y backfill de los históricos.
**Sin las columnas toda lectura de PGPC da ORA-00904** (pantalla de jubilados, corridas de seguro y pensiones).
- Control 0.3 = salida del 373 (2026-11-09): **53 filas, $18.227,05**. El UPDATE del bloque 3 debe actualizar 53.
- `crd/sql/375` (cuenta de destino de las órdenes de desembolso ya creadas) se corre aparte, con o sin este EAR.

## 3. Qué trae
| Cambio | Commit |
|---|---|
| Pensión retenida (sin cuenta activa única con certificado): no se descuenta ni se devenga hasta pagarse; se reserva del saldo | BE `d935f6b3` |
| Pago de lo acumulado, una orden por mes: en la corrida de pensiones y con el botón «Pagar pendientes» por fila | BE `d935f6b3`, `a10a1d7f`; FE `3593d46`, `3983691` |
| Pensión rechazada por el banco o reversada después de confirmada → vuelve a pendiente (sin volver a devengar) | BE `a10a1d7f`, `1a291a78` |
| «Procesar pendientes del mes»: paga solo pendientes, no recorre el padrón, repetible | BE `a10a1d7f`; FE `3983691` |
| Orden de desembolso con la cuenta de destino del socio; sin cuenta activa única el último nivel no aprueba (aviso previo en la pantalla) | BE `5b6b09fb`; FE `3983691` |

## 4. Despliegue
1. Correr el 374 (§2) y verificar sus controles.
2. Desplegar `retenida-1109\SaaEAR.ear`. Propiedades de sistema sin cambios.
3. Verificar: `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `713B2E21…E0EF6823`; «Last enabled at»; log sin
   `ERROR`/`ORA-` desde el despliegue.
4. Humo (solo lectura, sin pagar ni aprobar nada de prueba):
   - Jubilados → Padrón: la columna «Pensión retenida (sin cuenta)» muestra a los 8 jubilados del 373 con sus meses y
     total; el botón «Pagar pendientes» deshabilitado con el motivo (ninguno tiene cuenta activa).
   - Jubilados → Corrida del mes → previsualizar: responde, con las columnas de retenido.
   - Préstamos: abrir un préstamo en trámite → el estado de aprobación carga; si el socio no tiene cuenta activa única sale
     el aviso ámbar.
5. Primer caso real: el próximo préstamo que se apruebe debe llegar a tesorería **con** cuenta de destino.

## 5. Riesgos
- Nada corrió contra Oracle ni en WildFly: solo compila y arma. Sin probar: el ciclo retenido → registrar cuenta → pagar →
  confirmar / rechazar → pendiente otra vez; la consulta agregada de pendientes; el callback del reverso en CXP.
- El primer pago real de retenidos (cuando uno de los 8 registre su cuenta) se revisa con el árbitro: orden por mes,
  negativo del aporte 23 por mes, y sin devengo nuevo (son históricos ya devengados, `PGPCDVRT = 1`).
