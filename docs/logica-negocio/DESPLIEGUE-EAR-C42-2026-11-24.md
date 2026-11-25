# Despliegue del EAR «c42-1124» — todo `main` con C39–C44 (2026-11-24)

**omen-saa-1-arb.** **Reemplaza a `cancelados-1124`**, que no se llegó a desplegar. Es `cancelados-1124` más la rama del
cuadre `cuadre-c39-c44-validaciones-g`, mergeada en `897d6eb4`.

## 1. El EAR
- `F:\ears\asoprep\c42-1124\SaaEAR.ear`
  - SHA-256 `C2D8BE5D3A1209CC7F3FD60CB96737C964DCD8900EA378F4AE675F6A3A7F7C23` (62.450.512 bytes).
  - 3 partes en `partes\`; se unen con `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`.
  - `testzip` sin errores.
- `saaBE.war` `2F9008818C1BB1BF936FDEF5FEE75C1FB67CD030CCA56093FD6C7A05E96652A2`
  - Es saaBE `main` `897d6eb4`, empaquetado con `mvn -o package` sobre un `git archive` limpio.
  - **No** lleva el USAP de app1 que está sin commitear.
- `SaaFE.war` `69B9403C…84276410`: el mismo de `cancelados-1124` (saaFE `63a225d`; la rama no trae FE).
- **Base:** `precancelar-vencido-1118`, que es el desplegado. Esa misma es la **vuelta atrás**.

## 2. ⛔ ANTES del EAR, en este orden
1. **Los 9 scripts del §2 de `DESPLIEGUE-EAR-CANCELADOS-2026-11-24.md`** (C34-01…07d, 380, C37-01, C38-01), si no se
   corrieron todavía. El bloque 6 de `crd/sql/382` lo mide.
2. `docs/logica-negocio/reportes/sql/C39-01-ddl-sg45-origenes.sql`
3. `docs/logica-negocio/reportes/sql/C40-01-ddl-sustituciones-g.sql`: termina sin COMMIT. Revisar el bloque 3 (5 filas)
   y hacer COMMIT.
4. `docs/logica-negocio/reportes/sql/C41-01-ddl-sg45-tipo-participe.sql`
5. **Propiedades:** `saa.cartera.corteInclusivo` **NO** debe estar definida; si está, C42 no rige.
   `saa.cartera.moraNoCobradaHasta` **NO** se define en producción. Los dos se verifican con el jboss-cli de
   `crd/sql/382`.

## 3. Qué cambia en producción respecto de `cancelados-1124`
- ⚠️ **C42: la cuota que vence el mismo día del corte pasa a ser POR VENCER** en los cortes desde el 2025-08-01:
  cierre de cartera (①, ② y bandas), CarteraAlCorte, G48, G49 y CCPM.
  - **Regla 30/60:** un préstamo sin nada por vencer, con mora de hasta 30 días (60 en hipotecario), va entero a por
    vencer, también en la contabilidad. El asiento ① puede salir invertido.
- **C43:** sin la propiedad no cambia nada; el motor de pagos cobra la mora igual que hoy (lo revisé en el diff).
- **C44 / C39:**
  - `GET /ejrc/validar`;
  - el control de cierre suma «B17 sin saldos negativos», que da ERROR si hay alguna cuenta negativa que no sea
    correctora;
  - la G48 se cuadra por cuenta.
- **C40:** sustituciones de las G (`RPR.SSTG`, 5 filas de D-154). **C41:** la G48 pone la cuenta individual en cero a
  los sujetos J.

## 4. Humo (solo lectura)
1. Verificar el hash, desplegar y revisar que el log no tenga `ERROR` ni `ORA-`.
2. Correr el humo §4 de `DESPLIEGUE-EAR-CANCELADOS-2026-11-24.md` (Préstamos Cancelados, Seguimiento Novedades, primeros
   pagos reales).
3. Llamar a `GET /ejrc/validar` para un mes ya presentado y comparar con los rechazos de la Super de junio (1309/1311) y
   julio 2025 (58,97 en 1301/1304).
4. Abrir el control de cierre del mes en curso: aparece el control `B17_SALDOS_NEGATIVOS`.
5. **No** ejecutar el cierre de cartera de un mes en producción como prueba: solo previsualizar.

## 5. Riesgos
- C42 cambia los números del cierre y de las G para los cortes desde ago-2025. Es lo pedido (D-156), pero un mes ya
  presentado regenerado hoy **no** sale igual que lo presentado, salvo jun y jul-2025.
- `controlSaldosNegativosB17` arma el B17 preliminar dentro del control de cierre. Si falla, queda como ADVERTENCIA y no
  bloquea.
