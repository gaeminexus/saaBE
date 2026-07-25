# INSTRUCCIONES FRONTEND — REPORTES DE CARTERA (CPRM, CJBM, CCPM)

## CONTEXTO

Existen 3 nuevos reportes de uso interno del departamento de cartera:

- **CPRM** (Crédito Partícipes Mensual): muestra el total de aportes por partícipe agrupado por tipo de aporte. Cada partícipe puede tener varias filas, una por cada tipo de aporte que tenga registrado.
- **CJBM** (Crédito Jubilados Mensual): muestra datos de jubilados (pensión, saldo de cuenta, valores compensados, etc.).
- **CCPM** (Crédito Cuotas Préstamos Mensual): muestra el detalle de los préstamos activos (cuotas, provisiones, desgravamen, incendio, etc.).

Los 3 reportes se generan juntos para un mes y año determinados. El sistema registra quién los generó y cuándo.

---

## URL BASE

```
http://{servidor}:{puerto}/{contexto}/rest
```

Ejemplo local:
```
http://localhost:8080/saaBE/rest
```

---

## PASO 1 — VERIFICAR SI EL MES YA FUE GENERADO

Antes de mostrar el botón "Generar", consulta si ya existe una ejecución para el mes y año seleccionados.

**Request:**
```
GET /rest/ejcc/getByMesAnio/{mes}/{anio}
```

**Ejemplo:**
```
GET /rest/ejcc/getByMesAnio/6/2026
```

**Respuesta si NO fue generado (lista vacía):**
```json
[]
```
→ Mostrar botón **"Generar Reportes"** habilitado.

**Respuesta si YA fue generado:**
```json
[
  {
    "codigo": 1,
    "mes": 6,
    "anio": 2026,
    "usuario": "jperez",
    "fechaGeneracion": "2026-06-05",
    "observaciones": "CPRM: 450 registros. CJBM: 38 registros. CCPM: 312 registros."
  }
]
```
→ Mostrar botón **"Generar Reportes"** deshabilitado. Guardar el `codigo` (en este ejemplo `1`) para usarlo en las consultas.

---

## PASO 2 — GENERAR LOS REPORTES

Llamar este endpoint cuando el usuario presione el botón "Generar Reportes".

**Request:**
```
POST /rest/ejcc/ejecutar
Content-Type: application/json
```

**Body:**
```json
{
  "mes": 6,
  "anio": 2026,
  "usuario": "jperez"
}
```

> El campo `usuario` debe ser el nombre del usuario logueado en el sistema.

**Respuesta exitosa (200):**
```json
{
  "codigo": 1,
  "mes": 6,
  "anio": 2026,
  "usuario": "jperez",
  "fechaGeneracion": "2026-06-05",
  "observaciones": "CPRM: 450 registros. CJBM: 38 registros. CCPM: 312 registros."
}
```
→ Guardar el `codigo` devuelto para usarlo en las consultas de los 3 reportes.

**Respuesta si ya fueron generados (200):**
```json
{
  "mensaje": "Los reportes de cartera CPRM, CJBM y CCPM ya fueron generados para 6/2026..."
}
```
→ Mostrar mensaje al usuario indicando que ya existen y ofrecer la opción de ver los datos.

---

## PASO 3 — CONSULTAR REPORTE CPRM

Usar el `codigo` de la ejecución obtenido en el paso 1 o 2.

**Request:**
```
POST /rest/cprm/selectByCriteria
Content-Type: application/json
```

**Body:**
```json
[
  {
    "campo": "ejecucionReporte.codigo",
    "valor": "1",
    "tipo": "Long"
  }
]
```

**Respuesta:**
```json
[
  {
    "codigo": 1,
    "tipoIdentificacion": "C",
    "identificacion": "0501234567",
    "tipoAporte": {
      "codigo": 11,
      "nombre": "APORTE CESANTIA EMPLEADO"
    },
    "total": 1254.30,
    "entidad": { "codigo": 5, "numeroIdentificacion": "0501234567" },
    "ejecucionReporte": { "codigo": 1, "mes": 6, "anio": 2026 }
  },
  {
    "codigo": 2,
    "tipoIdentificacion": "C",
    "identificacion": "0501234567",
    "tipoAporte": {
      "codigo": 3,
      "nombre": "APORTE PATRONAL"
    },
    "total": 890.00,
    "entidad": { "codigo": 5, "numeroIdentificacion": "0501234567" },
    "ejecucionReporte": { "codigo": 1, "mes": 6, "anio": 2026 }
  }
]
```

> **Nota importante:** Un mismo partícipe (misma `identificacion`) puede aparecer en **varias filas**, una por cada tipo de aporte. Para mostrar el resumen de un partícipe se deben agrupar las filas por `identificacion` y mostrar cada `tipoAporte.nombre` con su `total` como columnas o filas del detalle.

---

## PASO 4 — CONSULTAR REPORTE CJBM

**Request:**
```
POST /rest/cjbm/selectByCriteria
Content-Type: application/json
```

**Body:**
```json
[
  {
    "campo": "ejecucionReporte.codigo",
    "valor": "1",
    "tipo": "Long"
  }
]
```

**Respuesta:**
```json
[
  {
    "codigo": 1,
    "tipoIdentificacion": "C",
    "identificacion": "0501111111",
    "tipoJubilacion": "V",
    "fechaJubilacion": "2020-03-15",
    "imposicionesAcumuladas": 240,
    "valorPension": 320.50,
    "valorNetoRecibir": 120.50,
    "saldoCuenta": 4500.00,
    "valoresCompensados": 200.00,
    "jubilacionIess": "S",
    "ejecucionReporte": { "codigo": 1, "mes": 6, "anio": 2026 }
  }
]
```

---

## PASO 5 — CONSULTAR REPORTE CCPM

**Request:**
```
POST /rest/ccpm/selectByCriteria
Content-Type: application/json
```

**Body:**
```json
[
  {
    "campo": "ejecucionReporte.codigo",
    "valor": "1",
    "tipo": "Long"
  }
]
```

**Respuesta:**
```json
[
  {
    "codigo": 1,
    "tipoIdentificacion": "C",
    "identificacion": "0502222222",
    "numeroOperacion": "10045",
    "tipoCredito": "Q",
    "diasMorosidad": 0,
    "calificacionPropia": "A1",
    "tasaInteres": 9.0,
    "valorPorVencer": 4500.00,
    "valorVencido": 0.00,
    "costosOperativos": 0.00,
    "interesOrdinario": 33.75,
    "interesMora": 0.00,
    "valorDemandaJudicial": 0.00,
    "carteraCastigada": 0.00,
    "provisionRequeridaOriginal": 44.55,
    "provisionConstituida": 44.55,
    "valorTotalCuentaIndividual": 2100.00,
    "valorSujetoProvision": 2400.00,
    "tipoSistemaAmortizacion": "FR",
    "cuotaCredito": 150.00,
    "dividendo": 183.75,
    "fechaExigibilidad": "2026-06-30",
    "valorDesgravamen": 4.50,
    "valorIncendio": 0.00,
    "ejecucionReporte": { "codigo": 1, "mes": 6, "anio": 2026 }
  }
]
```

---

## PASO 6 (OPCIONAL) — ELIMINAR UNA EJECUCIÓN PARA REGENERAR

Si se necesita volver a generar los reportes de un mes que ya fue procesado, primero hay que eliminar la ejecución.

**Request:**
```
DELETE /rest/ejcc/{codigo}
```

**Ejemplo:**
```
DELETE /rest/ejcc/1
```

**Respuesta (200):**
```json
"EjecucionReporteCartera eliminado correctamente"
```

> Después de eliminar, volver al **Paso 2** para generar nuevamente.

---

## RESUMEN DE TODOS LOS ENDPOINTS

| # | Método | URL | Para qué sirve |
|---|--------|-----|----------------|
| 1 | GET | `/rest/ejcc/getByMesAnio/{mes}/{anio}` | Verificar si el mes ya fue generado |
| 2 | POST | `/rest/ejcc/ejecutar` | Generar los 3 reportes del mes |
| 3 | POST | `/rest/cprm/selectByCriteria` | Consultar datos del CPRM |
| 4 | POST | `/rest/cjbm/selectByCriteria` | Consultar datos del CJBM |
| 5 | POST | `/rest/ccpm/selectByCriteria` | Consultar datos del CCPM |
| 6 | DELETE | `/rest/ejcc/{codigo}` | Eliminar ejecución para regenerar |

---

## CAMPOS IMPORTANTES POR REPORTE

### CPRM — campos a mostrar en tabla
| Campo JSON | Descripción |
|---|---|
| `identificacion` | Cédula del partícipe |
| `tipoAporte.nombre` | Nombre del tipo de aporte |
| `total` | Total acumulado de aportes para ese tipo |

### CJBM — campos a mostrar en tabla
| Campo JSON | Descripción |
|---|---|
| `identificacion` | Cédula del jubilado |
| `tipoJubilacion` | Tipo de jubilación (V = Voluntaria) |
| `fechaJubilacion` | Fecha en que se jubiló |
| `imposicionesAcumuladas` | Número de imposiciones |
| `valorPension` | Valor de la pensión mensual |
| `valoresCompensados` | Valor descontado por préstamos |
| `valorNetoRecibir` | Valor neto que recibe |
| `saldoCuenta` | Saldo de su cuenta individual |
| `jubilacionIess` | ¿Tiene jubilación IESS? (S/N) |

### CCPM — campos a mostrar en tabla
| Campo JSON | Descripción |
|---|---|
| `identificacion` | Cédula del deudor |
| `numeroOperacion` | Número del préstamo |
| `tipoCredito` | Tipo de crédito |
| `diasMorosidad` | Días de mora |
| `calificacionPropia` | Calificación de riesgo (A1, A2...E) |
| `tasaInteres` | Tasa nominal anual |
| `valorPorVencer` | Saldo de capital por vencer |
| `valorVencido` | Capital vencido no pagado |
| `interesOrdinario` | Interés de la cuota |
| `interesMora` | Interés por mora |
| `valorSujetoProvision` | Base de cálculo de provisión |
| `provisionRequeridaOriginal` | Provisión calculada |
| `provisionConstituida` | Provisión del período anterior |
| `valorDesgravamen` | Seguro de desgravamen de la cuota |
| `valorIncendio` | Seguro de incendio de la cuota |
| `fechaExigibilidad` | Fecha de vencimiento de la cuota |
