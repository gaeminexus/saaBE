# B17 de septiembre 2025: 149905 (provisión de intereses) contra 1402 (intereses por cobrar)

Pregunta de ASOPREP (24-nov-2026): ¿por qué la 1402 (1.091.782,53) es tan menor que la 149905 (−1.710.980,57), si deberían ser
iguales o parecidas?

Las cifras se reproducen con `provision-intereses-vs-1402.sql` (en esta misma carpeta) (base de trabajo, 24-nov-2026, solo SELECT).

## Composición al 30-sep-2025

| Componente | 1402 por cobrar | 149905 provisión | Diferencia |
|---|---:|---:|---:|
| Interés ordinario | 1.049.216,42 | 1.073.804,45 | 24.588,03 |
| Mora | 42.566,11 | 637.176,12 | **594.610,01** |
| **Total** | **1.091.782,53** | **1.710.980,57** | **619.198,04** |

- **Interés: cuadra, con 24.588,03 de diferencia.** El paso ⑦ del cierre de cartera provisiona el 100 % del interés no
  cobrado de toda cuota vencida, y la 1.4.02 está unos 19 mil por debajo por C27b (interés cobrado antes de su devengo),
  más el arranque. Cuando el código nuevo corrija C27b desde octubre (D-185), esta diferencia se cierra casi entera.
- **Mora: ahí está el problema.** El ⑦ provisiona el 100 % de la mora CALCULADA de cada cuota vencida (637.176,12), pero en
  la 1.4.02 solo está la mora devengada desde junio 2025 que sigue sin cobrar (42.566,11). Se provisiona un activo que no
  está registrado. Además, ASOPREP no cobró mora hasta mayo/junio de 2026 (D-156 (3)).
- **Por antigüedad de la cuota,** la mora provisionada es: 456.172,12 de cuotas vencidas antes de 2020; 159.107,04 de
  2020–2023; 21.844,23 de 2024 a mayo 2025; y 52,73 de junio a septiembre 2025.

## De dónde viene

| Origen | Interés | Mora |
|---|---:|---:|
| Semilla de apertura (D-016, provisión de Delta al 31-may-2025, corrida 26) | 503.481,91 | 306.265,71 |
| ⑦ de junio 2025 (completa al 100 % lo que Delta tenía provisionado a un porcentaje) | 498.010,97 | 302.871,55 |
| ⑦ de jul–sep, menos reversos por cobro y condonación | resto | resto |

La semilla no se duplicó: el ⑦ de junio la leyó y completó cada cuota hasta el 100 %. En el ejemplo de la cuota 402861,
la semilla puso 16,82 y junio 16,11, que suman 32,93 = interés de la cuota. El defecto es de regla: el ⑦ provisiona mora
que no está en el activo.

## Propuesta (la decide el líder; la cuenta del crédito, con el contador)

1. **Regla:** la provisión de mora de una cuota no puede pasar de la mora contabilizada pendiente en la 1.4.02 (MVIC 5 − 6 − 9
   menos la mora cobrada). La de interés se queda al 100 %. Es un cambio del paso ⑦ para el árbitro (`ProvisionInteresServiceImpl.calcularItems`).
   Sin ese cambio, el ⑦ del mes siguiente vuelve a provisionar lo que se reverse.
2. **Asiento de corrección por 594.610,01,** D 1.4.99.05 en el primer mes abierto (octubre 2025, o septiembre si se reabre),
   con el libro MVIC ajustado por cuota (movimiento de reverso de la provisión de mora) para que el ⑦ no lo repita. El
   crédito se divide en dos:
   - la parte que generó el SAA en 2025, unos 290 mil, va contra el gasto de provisión 4.7.05.10 del ejercicio;
   - la parte que vino de Delta (306.265,71 de la semilla) va a recuperación de provisiones o a resultados de ejercicios
     anteriores, según el contador.
3. **Jun–ago 2025 ya están presentados (D-168):** no se tocan. La corrección va en el primer mes abierto y se explica en
   la novedad.
