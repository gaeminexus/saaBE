# Beneficiarios del partícipe y valores de seguro a entregar (sepelio) — diseño

**Equipo:** `omen-saa-1` (CRD · EQUIPO B) · **Árbitro:** `omen-saa-1-arb` · **Fecha:** 2026-09-21
**Estado:** ⛔ **DISEÑO. Nada construido, ningún DDL corrido.** Las decisiones marcadas
«PENDIENTE» las cierra el usuario antes de que un agente escriba una línea.

---

## 1. El requerimiento, como lo dio el usuario

Cuando un partícipe fallece, la aseguradora paga —además del desgravamen, que cancela sus
préstamos— una cobertura de **sepelio**: dinero que entra al fondo y que el fondo **entrega a los
familiares**. Se necesita:

1. Registrar ese valor en la cuenta del partícipe, como se hace con los aportes, para poder
   entregarlo después.
2. **Registrar cuentas de beneficiarios**: nombre, cédula, banco, tipo de cuenta, número de cuenta,
   **porcentaje** y **certificado bancario digitalizado**. El sistema paga **según el porcentaje**.
3. Una **pantalla en créditos que registre la recepción del dinero**, que **contabilidad apruebe**
   confirmando que está en la cuenta, y **el asiento se genera con esa aprobación** — igual que los
   cobros hoy.
4. El pago sale **por la misma pantalla de devolución**, marcando que **no es un aporte**.
5. **Sólo dos asientos**, los del correo de contabilidad:

| Momento | DEBE | HABER |
|---|---|---|
| **Recepción** del dinero de la aseguradora | `1102XX` Banco | `23909011` Indemnizaciones de seguros por pagar a beneficiarios |
| **Entrega** del dinero al beneficiario | `23909011` | `1102XX` Banco |

Y una ampliación que dio el usuario de paso: **la devolución de aportes de un fallecido también
debería poder pagarse a los beneficiarios**, reusando esto mismo.

---

## 2. Lo que YA existe y se reusa (verificado contra el código, 2026-09-21)

| Pieza | Dónde | Qué aporta |
|---|---|---|
| `CRD.CTAP` (`CuentaTipoAporte`) | `model/crd/CuentaTipoAporte.java` | Cuenta de **pasivo** y de **liquidación** por tipo de aporte **y por empresa**. Tiene REST propio (`CuentaTipoAporteRest`): se configura por pantalla, **sin SQL** |
| Saldo por tipo | `saldoAporteService.saldoPorEntidadYTipo` | El «valor en la cuenta del partícipe» sale sin código nuevo |
| Devolución completa | `DevolucionAporteServiceImpl` | Registro, asiento, orden a CXP, sincronización, anulación y reemisión (H65) |
| **Cuenta bancaria con certificado PDF** | `CuentaBancariaParticipeServiceImpl.crearConCertificado:131` · `POST /rest/cnbp/conCertificado` | ⭐ **El patrón exacto de lo que se pide**: multipart, sólo PDF, validación de tamaño (10 MB), adjunto en `CRD.ADJN` con el tipo `CRD.TPDJ` «CERTIFICADO BANCARIO» resuelto **por nombre** |
| Aprobación contable | `CobroCreditoServiceImpl.aprobarCobro:296` + pantalla `bandeja-contabilidad` | El flujo «registro → contabilidad aprueba → asiento» ya existe y está en producción |
| Pago a un tercero | `BeneficiarioOcasional` (CXP) | CXP ya acepta beneficiario denormalizado (nombre, identificación, banco, cuenta). **No hay que crear `Titular`** |

### Dos riesgos descartados midiéndolos, no suponiendo

- **El ancla envenenada de H46 no se toca.** `resolverAnclaRetroactivo` mira **sólo movimientos
  negativos del tipo 23**; un tipo de aporte nuevo le es invisible.
- **`registrarDevolucion` no valida el estado del partícipe**: un fallecido **no está bloqueado**.
  Y `EstadoParticipeEntidad.CESANTE_FALLECIDO` (5, PK 40) ya existe.

Se heredan gratis el bloqueo `FOR UPDATE` por partícipe y el rechazo de saldo negativo de H61.

---

## 3. Tabla nueva: beneficiarios del partícipe

⛔ **DDL sobre `CRD`: lo autoriza el usuario** (recuadro §3 de `REGISTRO-RESERVAS-EQUIPOS.md`).
El nombre se reserva acá porque es un recurso global, pero **no se crea hasta el visto bueno**.

✅ **Nombre decidido por el usuario (2026-09-21): `CRD.CBBP` — Cuenta Bancaria Beneficiario
Partícipe.** Verificado libre contra los `@Table` de `src/main/java/com/saa/model/` (cero
ocurrencias). **Falta el control contra `ALL_TABLES`**, que va como bloque 0 del propio DDL y
detiene el script si devuelve filas.

> El árbitro había propuesto `BNFC` (beneficiario). **`CBBP` es mejor nombre y describe mejor lo
> que la tabla realmente guarda**: no es un beneficiario en abstracto, es **la cuenta bancaria a la
> que cobra un beneficiario**. Además queda en paralelo directo con `CRD.CNBP`
> (Cuenta Bancaria Partícipe), que es su equivalente para el titular.

Una fila = **un beneficiario con su cuenta**. No hacen falta dos tablas: un beneficiario cobra a una
sola cuenta.

| Campo Java | Columna | Notas |
|---|---|---|
| `codigo` | `CBBPCDGO` | PK, `SQ_CBBPCDGO` |
| `entidad` | `ENTDCDGO` | FK al partícipe |
| `nombre` | `CBBPNMBR` | |
| `numeroIdentificacion` | `CBBPIDNT` | cédula |
| `bancoExterno` | `BEXTCDGO` | FK, igual que `CNBP` |
| `tipoCuenta` | `CBBPTPCN` | mismo catálogo que `CNBPTPCN` |
| `numeroCuenta` | `CBBPNMRO` | |
| `porcentaje` | `CBBPPRCN` | `NUMBER(5,2)` |
| `estado` | `CBBPIDST` | activo/inactivo |

El **certificado bancario** NO va como columna: va en `CRD.ADJN` con el tipo `CRD.TPDJ`
«CERTIFICADO BANCARIO», exactamente como lo hace `crearConCertificado` para `CNBP`.

⚠️ **Verificar que `CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql` haya corrido en producción.**
El tipo se resuelve **por nombre**, así que sin esa fila el endpoint responde
`TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO` (500) en cualquier intento. No consta que se haya corrido.

### El porcentaje — dos decisiones que no se deducen

**a) Cuándo se valida que sume 100.** Si se valida **al guardar cada beneficiario**, no se puede
registrar el primero (50 %) sin que falle. ⇒ **Decisión del árbitro: se valida AL PAGAR**, no al
guardar; la pantalla muestra el acumulado mientras se cargan. Un pago con porcentajes que no suman
100 se rechaza con el total que sí suman.

**b) El centavo del reparto.** $1.000,00 entre tres al 33,33 % da $999,99: **falta un centavo**.
Este fondo ya se quemó con esto (H48, la cascada de pagos que abandonaba un residuo).
⇒ ✅ **DECIDIDO POR EL USUARIO (2026-09-21): el residuo va al beneficiario de mayor porcentaje;
si empatan, al de menor código.** Determinista, auditable y explicable a un familiar que pregunte
por qué recibió un centavo más. La suma de las órdenes generadas tiene que dar **exactamente** el
valor entregado — esa es la guarda, no el redondeo de cada línea por separado.

---

## 4. El valor de sepelio en la cuenta del partícipe

**Se modela como un TIPO DE APORTE nuevo** (`CRD.TPAP`), con su cuenta en `CRD.CTAP`
(`cuentaPasivo = 2.3.90.90.11`). Con eso, saldo, invariantes, pantalla de devolución y
configuración contable salen sin código nuevo.

⚠️ **Contablemente esto NO es un aporte, es un pasivo con terceros.** El atajo se sostiene sólo si
el saldo de sepelio **no se mezcla** con los aportes. **A verificar antes de construir:** que ningún
certificado de partícipe, reporte ni devolución masiva sume «todos los tipos» sin filtrar — si
alguno lo hace, el certificado de un partícipe le mostraría plata que no es suya.

### Lo que NO se reusa: el asiento de reclasificación

`generarAsientoReclasificacion:1284` hace hoy **D `cuentaPasivo` / H `cuentaLiquidacion`**, y
después CXP arma el pago. Eso son **tres** asientos en total. **El usuario pidió dos.**

⇒ La rama de devolución debe **omitir la reclasificación** para este tipo. La marca tiene que ser
**explícita, no mágica**: una columna nueva en `CRD.CTAP` (p. ej. `CTAPRCLS`, «genera
reclasificación S/N») en vez de deducirlo de que las dos cuentas coincidan.

⛔ **Lo que se prohíbe:** resolver esto con un `if` sobre el código del tipo de aporte quemado en
Java. Es exactamente la fragilidad que ya costó el bug de la condonación (`aux1=10`).

---

## 5. Los dos asientos, y quién los arma

### Recepción — pantalla nueva en créditos, asiento en la aprobación

Flujo calcado del cobro (`aprobarCobro:296` + bandeja de contabilidad):
registrar recepción → queda pendiente → **contabilidad aprueba** → se genera **un** asiento
**D Banco / H 2.3.90.90.11** y el valor queda disponible en la cuenta del partícipe.

⚠️ **Diferencia deliberada con el cobro:** el cobro genera un asiento transitorio **al registrar**
(ASN1) y el definitivo al procesar. Acá **no hay transitorio**: un solo asiento, en la aprobación.
Es lo que pidió el usuario y lo que sostiene el «sólo dos asientos».

### Entrega — al pagar, por la pantalla de devolución

**D 2.3.90.90.11 / H Banco**, repartido entre los beneficiarios según porcentaje (N órdenes, una por
beneficiario, cada una con su `BeneficiarioOcasional`).

⛔ **Dependencia fuera de nuestro alcance.** Si el pago sale por una orden de CXP, **el asiento lo
arma CXP con la cuenta del grupo del producto de pago**. Hace falta **un producto de pago de CXP
cuya cuenta sea `2.3.90.90.11`** — el mismo acoplamiento de los productos 516 y 517 (P19/P20).
`cxp` es de otro equipo: **esto se coordina, no se toca.**

---

## 6. Lo que queda pendiente del usuario

| # | Qué | Tipo |
|---|---|---|
| S1 | **Crear `CRD.CBBP`** — autorización de DDL sobre `CRD` | bloqueante |
| ~~S2~~ | ~~¿El residuo del centavo?~~ | ✅ **decidido**, §3b |
| S3 | **El producto de pago de CXP contra `2.3.90.90.11`** — pide coordinación con el equipo de `cxp` | bloqueante |
| S4 | ¿`CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql` corrió en producción? Sin esa fila no se puede adjuntar ningún certificado | bloqueante |
| ~~S5~~ | ~~¿La devolución normal también a beneficiarios?~~ | ✅ **decidido**, §7 |
| ~~S6~~ | ~~¿Un beneficiario de varios partícipes?~~ | ✅ **decidido**, §7 |
| ~~S7~~ | ~~¿Recibir sin beneficiarios cargados?~~ | ✅ **decidido**, §7 |

---

## 7. Decisiones del usuario — 2026-09-21. NO re-litigar

Las cuatro se preguntaron con las alternativas a la vista y se decidieron el mismo día.

| Decisión | Qué implica en el código |
|---|---|
| **El residuo del centavo va al de mayor porcentaje** (empate → menor código) | §3b. La guarda es que la **suma de las órdenes** dé exactamente el valor entregado |
| **La devolución de aportes normal TAMBIÉN se paga a beneficiarios** cuando el partícipe está fallecido | ⚠️ **Amplía el alcance:** `registrarDevolucion` deja de pagar siempre a `CuentaBancariaParticipe` y pasa a elegir destino según el estado del partícipe. Se resuelve **una sola vez** para sepelio y para aportes — es el mismo problema: la cuenta de un muerto no sirve |
| **La recepción del dinero se puede registrar SIN beneficiarios cargados** | La validación del 100 % vive en el **pago**, nunca en la recepción. La plata ya está en el banco: negarse a registrarla no la hace desaparecer. La pantalla debe avisar que faltan beneficiarios, sin bloquear |
| **La misma cédula puede ser beneficiaria de VARIOS partícipes** | El índice único es **(partícipe, cédula)**, NO la cédula sola. Un hijo es beneficiario del padre y de la madre: caso real y frecuente. ⛔ Un `UNIQUE` sobre `CBBPIDNT` solo sería un defecto que aparece recién cuando muere el segundo progenitor |
