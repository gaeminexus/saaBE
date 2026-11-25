# Encargo al árbitro, bloque nuevo: la provisión de intereses es igual a los intereses por cobrar al cierre de cada mes

**Qué es.** Bloque adicional al `ENCARGO-ARBITRO-CONTABILIDAD-PRODUCCION.md` (24-nov-2026), pedido por el líder el 25-nov-2026:
el proceso no debe permitir que la provisión de intereses (1.4.99.05) se aparte de los intereses por cobrar (1.4.02). El control
solo confirma, y el asiento de ajuste es el último recurso. Vale para el reproceso y para producción: es el mismo saaBE.

**De dónde sale.** `03-trabajo/revision/PROVISION-VS-1402-OCT-NOV-2025.md` y `MENSAJE-ARBITRO-12-PROVISION-VS-1402.md`.

**Verificación previa (25-nov-2026).** La Super **no** cruza el interés de la G48 (`interesOrdinario` + `interesMora`) con la
1402 del B17. Lo aceptó con diferencias grandes en todos los meses: mayo 2025 de Delta +234.001,11; junio −727.932,09;
julio −728.281,93; agosto −695.057,15. Sí valida el capital (1309/1311, por vencer y vencido) y la provisión de cartera
(1399), que esta regla no toca. Script: `03-trabajo/revision/g48-interes-vs-b17.js`. Por lo tanto la regla no crea riesgo de
rechazo, y la G48 sigue con D-156 (la cuota que vence el día del corte se reporta por vencer).

**Estado.** Regla confirmada por el líder el 25-nov-2026 (D-206), con el criterio del contador. Listo para enviar.

Copiar al árbitro el bloque de abajo tal cual. No lleva datos de partícipes.

---

```text
BLOQUE N — 1.4.02 Y 1.4.99.05 IGUALES AL CIERRE DE CADA MES (saaBE, reproceso y producción)

REGLA (confirmada por el líder el 25-nov-2026, con el criterio del contador de ASOPREP)
  (1) Estado de la cuota, igual en TODO el SAA: la cuota que vence el día del corte está POR VENCER (D-156). Vale para la G48,
      las bandas, el B17 de capital, la mora (0 días) y los cobros. Ningún proceso lo trata distinto.
  (2) La provisión de intereses NO depende del estado de la cuota: cubre todo interés (y mora) DEVENGADO Y NO COBRADO al
      cierre. Al último día del mes eso son las cuotas vencidas impagas y la que vence ese día y no se pagó (su interés se
      devengó el día 1). El ⑦ se programa con este criterio («devengado no cobrado»), no con «vencido».
  (3) Consecuencia, que es lo que revisa el contador: en el balance del ÚLTIMO DÍA del mes,
        1.4.99.05 = 1.4.02, al centavo, por tipo de préstamo (Q, P, H) y por componente (interés y mora).
  (4) El día 1 del mes siguiente: 1.4.02 − 1.4.99.05 = el asiento de devengo de ese día, al centavo.
  · La Super no cruza el interés de la G48 con la 1402 (verificado con mayo de Delta y jun–ago 2025 aceptados), así que la G48
    sigue con D-156 sin riesgo.

NIVEL 1 — CADA OPERACIÓN LO HACE BIEN (lo principal)
  1.1 limiteDevengado (DevengoCompletoServiceImpl): el último cierre EJECUTADO con corte anterior a la fecha de la operación,
      nunca el último de la base. En precancelación, acuerdo y abono se reversa de la 1.4.02 solo el interés que de verdad se
      devengó. Medición: def9-205-reverso-devengo-de-mas.sql (68 asientos de oct–dic 2025, 62.753,98 de más).
  1.2 Todo cobro de interés o mora ya provisionados reversa su provisión, cuota por cuota, por cualquier camino: ventanilla,
      Petro, cruce, débito, acuerdo con o sin depósito, precancelación y abono. Hoy el acuerdo con depósito no la reversa en las
      cuotas vencidas que paga (acuerdo 42: cuotas 49 y 50, 986,88).
  1.3 Toda condonación, castigo o anulación mueve la 1.4.02 y la provisión de la misma cuota, por el mismo valor.
  1.4 Libro de devengo de interés por cuota: hoy solo la mora tiene libro por cuota (MVIC 5/6). Sin libro del interés, la
      1.4.02 no se puede conciliar por cuota. Pedido: un movimiento MVIC de devengo y de reverso de devengo de interés
      (componente 1), grabado por el ④ y por cada reverso, con el mismo asiento.

NIVEL 2 — EL CIERRE DE CARTERA GARANTIZA LA REGLA AL ÚLTIMO DÍA
  2.1 El ⑦ arma el asiento desde el MISMO cálculo que graba en el libro (hoy el asiento se arma antes del ⑩ y el libro
      después: oct 1.189,76 y nov 10.986,74 de diferencia, en mora).
  2.2 La provisión de mora es igual a la mora contabilizada pendiente del préstamo, sin el límite de la mora calculada por
      cuota (al 30-nov la provisión queda 1.843,89 por debajo).
  2.3 Después del ⑦, el cierre compara 1.4.02 contra 1.4.99.05 por tipo y componente, y además contra el libro por cuota.
      Si queda diferencia, genera él mismo el asiento de ajuste de ÚLTIMO RECURSO:
        · lleva a la contabilidad lo que dice el libro por cuota: la 1.4.02 contra el ingreso 5.1.02 y la 1.4.99.05 contra el
          gasto 4.7.05.10, con las cuentas de las plantillas;
        · graba el ajuste por cuota en el libro (un tipo de MVIC nuevo), para que el mes siguiente no se repita;
        · deja la excepción registrada en la corrida (cuotas, valores y causa probable), visible en la previsualización.
      Una excepción es un defecto de algún flujo del nivel 1: se reporta y se corrige en el código.
  2.4 La previsualización del cierre muestra el control antes de ejecutar, y el cierre del período (/prdo/cerrar) no deja
      cerrar con la diferencia distinta de cero sin su ajuste registrado.

NIVEL 3 — CONTROL DEL DÍA 1
  3.1 Después del ④ del día 1: 1.4.02 − 1.4.99.05 = asiento de devengo del día 1, por tipo. Va en el mismo resumen de la
      corrida.

PRUEBAS DE ACEPTACIÓN (base de trabajo, copia de producción; SQL reproducibles del proyecto de cuadre)
  · provision-vs-1402-oct-nov-2025.sql al corte de cada mes: diferencia 0,00 por tipo y componente; bloque 05 (provisión en
    cuotas canceladas o pagadas) = 0 filas; bloque 06 (⑦ contra su libro) = 0 de diferencia.
  · def9-205-reverso-devengo-de-mas.sql sobre las operaciones nuevas: DE_MAS = 0,00 en cada asiento.
  · Día 1: diferencia = asiento de devengo del día 1.
  · Regresión: el BLOQUE 0 del encargo sigue igual. La G48 y el B17 de capital no cambian.

ARRANQUE DEL MOTOR NUEVO EN EL REPROCESO: AJUSTE AL 1-DIC-2025
  El proyecto de cuadre carga un asiento manual al 1-dic-2025 que deja 1.4.02 = 1.4.99.05 al centavo sobre el estado al 30-nov
  (sql def9-206-ajuste-1402-provision-1dic.sql):
    · D206-2025-12-001: 1.4.02 por tipo contra 5.1.02 (PH +19.702,44 · PQ +20.293,86 · PP −13,97: reverso de más de oct–nov
      y una diferencia de conciliación por cuota) y 1.4.99.05 +10.592,89 contra 4.7.05.10 (mora +14.020,41; interés −3.427,50).
    · D206-2025-12-002: arranque de Delta (PH 7.479,09 · PQ 998,76) en 1.4.02; la cuenta del crédito la define el contador.
  Para que el motor nuevo arranque alineado, pedimos:
    A1. Un ajuste del LIBRO por cuota ligado a ese asiento (como /prov/ajusteTopeMora con D-186): deja el saldo provisionado de
        cada cuota igual a su interés y su mora devengados y no cobrados al 30-nov. Lleva a 0 la provisión de las cuotas
        cerradas y las provisiones negativas, y suma al centavo lo del asiento, por componente. Con grabar=false para
        previsualizar e idempotente por asiento.
    A2. La semilla del libro de devengo de interés por cuota (nivel 1.4) al 30-nov con los mismos valores por cuota.
    Sin A1, el ⑦ nuevo de diciembre vería las diferencias por cuota y generaría otro ajuste por lo mismo. Por eso se cargan
    juntos y antes del cierre de diciembre.
  · Diciembre 2025: D205-2025-12-001 al 31-dic (D 1.4.02 / H 5.1.02, 20.719,68), el reverso de más de las operaciones de
    diciembre, que ya corrieron con el código viejo. Las provisiones de cuotas cerradas en diciembre las recoge A1 si se corre
    al corte del cierre, o el ajuste de último recurso del 2.3.
  · Producción: el mismo código. El arrastre de producción se mide con el mismo SQL a la fecha del reemplazo.
```
