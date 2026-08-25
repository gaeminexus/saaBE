# PROMPT — Agente FRONTEND · Fase 1: pantalla de parametrización de bandas

> **Etiqueta: FRONTEND** (repo `saaFE`). **Orden y dependencia:** corre DESPUÉS de que el
> agente BACKEND haya avanzado: el contrato de endpoints vive en
> `docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md` del repo BACKEND (`saaBE`) y es la
> ÚNICA fuente de rutas y estructuras — si un endpoint no está ahí, no existe todavía;
> no inventes rutas ni payloads. Copia ese documento al repo FE si el estándar del
> proyecto es espejar docs (los .sql NUNCA se espejan).

---

Construye la pantalla de **Parametrización de bandas de cartera por producto** del módulo
de créditos, siguiendo los patrones de pantallas existentes del proyecto (layout, tablas,
mensajes, servicios HTTP, manejo de errores). Antes de codificar, revisa una pantalla CRUD
existente del módulo de créditos y reutiliza su estructura.

## Contexto de negocio (resumen)

Cada producto de crédito (tabla `CRD.PRDC`) tiene dos configuraciones de bandas de
cartera: **POR VENCER** (tipo cartera = 1) y **VENCIDO** (tipo cartera = 2). Cada
configuración es una lista de bandas numeradas 1..N; cada banda define cuántos
**períodos de 30 días** abarca (la última banda va con períodos vacío = "resto", abierta)
y la **cuenta contable** donde se registra el capital de esa banda. Los rangos en días se
derivan acumulando períodos (banda 1 con 1 período = 1-30; banda 2 con 2 = 31-90; …) — el
backend ya los devuelve calculados; la pantalla NO los calcula, los muestra. Las
configuraciones tienen vigencia (desde/hasta); el cambio normativo se hace cerrando la
vigencia y creando una configuración nueva, nunca editando la vigente en caliente.

## La pantalla

1. **Selector de producto** (catálogo que expone el backend; mostrar también los
   inactivos, marcados) y de **empresa** si el patrón de pantallas del proyecto la maneja
   explícitamente; si la empresa viene del contexto de sesión, usar esa.
2. Dos secciones (pestañas o bloques): **Por vencer** y **Vencido**, cada una mostrando la
   configuración vigente: tabla de bandas con columnas Nº de banda, Períodos de 30 días
   (la última se muestra como "Resto"), Rango en días (solo lectura, del backend), Cuenta
   contable (código + nombre, con buscador contra el plan de cuentas), y la vigencia de la
   configuración. Productos sin configuración (hoy: PRENDARIO NOVACION e HIPOTECARIO
   NOVACION en por vencer) muestran la sección vacía con opción de crearla.
3. **Edición**: agregar/quitar/reordenar bandas y cambiar períodos y cuentas, con
   validaciones espejo de las del backend (consecutivas desde 1, una sola banda "resto" y
   al final, períodos >= 1, cuenta obligatoria) — y mostrando tal cual los mensajes de
   error que devuelva el backend. Guardar envía la configuración completa (cabecera +
   bandas) al endpoint de guardado del contrato.
4. **Cambio de vigencia**: acción separada ("nueva vigencia") que usa el endpoint de
   cierre de vigencia; el historial de configuraciones cerradas se muestra solo lectura
   si el backend lo expone.
5. Si el contrato incluye el endpoint de verificación de clasificación (días → banda →
   cuenta), agrega un probador pequeño en la pantalla (input de días, muestra banda y
   cuenta) — ayuda a validar la parametrización.

## Restricción de acceso TEMPORAL — requisito explícito del usuario

La pantalla debe quedar **accesible únicamente si el usuario logueado es USUARIO 1**
(el usuario con código 1 del sistema; verifica cómo guarda el proyecto el usuario en
sesión y compara contra su código/identificador). Implementa el chequeo en UN solo lugar
(guard de ruta o equivalente del framework del proyecto): ocultar la opción de menú Y
bloquear la navegación directa por URL. Márcalo con un `TODO` visible indicando que es
temporal y se sustituirá por el esquema de permisos definitivo — no inventes un sistema
de permisos nuevo.

## Reglas técnicas

- Base de las llamadas: `/SaaBE/rest/...` según el contrato.
- Fechas: `LocalDate` viaja `"yyyy-MM-dd"`; nunca enviar un `Date` crudo de JavaScript ni
  nada terminado en `Z` (el backend descarta el offset y graba la hora corrida — regla
  del proyecto).
- Manejo de errores: los endpoints devuelven 500 con texto `"Error ...: mensaje"`;
  mostrar el mensaje al usuario según el patrón de la casa.
- Al terminar, deja un resumen: componentes/archivos creados, ruta de la pantalla, dónde
  quedó el guard de USUARIO 1, y qué endpoints del contrato consumiste.
