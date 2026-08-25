# PROMPT — Agente FRONTEND · Corrección: nombres de campos al estándar (impacto nulo esperado)

> **Etiqueta: FRONTEND** (repo `saaFE`). **Orden:** solo aplica si ya empezaste o
> terminaste la pantalla de `PROMPT-FRONTEND-BANDAS-FASE1.md`. Si aún no has empezado,
> ignora este prompt por completo: el de Fase 1 ya es consistente.

---

## Qué pasó

En el backend se renombraron tres **columnas físicas de Oracle** de las tablas de bandas
para cumplir el estándar de nombres del sistema: `CBPRFCDE→CBPRFCIN`, `CBPRFCHS→CBPRFCFN`,
`BNDPPRDS→BNDPCNTD`.

**El contrato JSON NO cambió**: los atributos serializados siguen siendo `fechaDesde`,
`fechaHasta` y `periodos`, y las rutas de `API-BANDAS-PRODUCTO.md` son las mismas. El
frontend nunca ve nombres de columnas Oracle.

## Tu tarea (verificación mínima)

1. Confirma que la pantalla de parametrización de bandas consume únicamente los campos
   JSON del contrato (`fechaDesde`, `fechaHasta`, `periodos`, etc.) y ninguna cadena
   `CBPRFCDE` / `CBPRFCHS` / `BNDPPRDS` aparece en el código FE (no debería: esos nombres
   jamás viajaron por la API).
2. Si usaste `selectByCriteria`, verifica que los `DatosBusqueda` referencian nombres de
   atributo Java (camelCase), no columnas Oracle.
3. Reporta en una línea el resultado; si no hay nada que cambiar, dilo explícitamente y
   no toques código.
