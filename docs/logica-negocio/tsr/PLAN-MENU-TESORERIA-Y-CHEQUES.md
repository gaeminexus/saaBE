# Plan — limpieza del menú de Tesorería y mejora de las pantallas de cheques

**Equipo:** `omen-saa-2` · **Escrito:** 2026-09-07 · **Módulo:** `tsr` (solo frontend)

**Pedido del usuario, textual:**

> *«Arreglemos el menú de tesorería, quitar los submenús de cobro y pagos, pero de pagos
> reclasificar y mejorar las pantallas de cheques, esas son las únicas que me interesan de ese
> menú.»*

**Decisiones del usuario, 2026-09-07:**

| # | Decisión |
|---|---|
| **M1** | Las 10 pantallas de **Cobros** salen del menú. **NO se borran** ni sus rutas ni sus componentes: quedan alcanzables por URL y volver atrás es una línea |
| **M2** | ~~«Solicitud Pagos» ENTRA al nodo de cheques~~ → ⛔ **REVERTIDA el 2026-09-07. La pantalla se BORRÓ.** Ver §4 |
| **M3** | Menú y mejora de pantallas, **las dos cosas ahora** |

---

## 1. El menú, antes y después

### Antes — nodo `Procesos`

```
Cobros                        ← SE VA (10 pantallas)
  Cierre de Caja
  Depósitos > Envío · Ratificación
  Consultas > Cobros · Cierres
  Procesos  > Cobros · Cierres · Depósitos · Ratificación Depósitos

Pagos por transferencia       ← SE QUEDA TAL CUAL (circuito nuevo)
  Aprobación de pagos · Generación de archivo
  Recepción y confirmación · Consulta y gestión

Pagos                         ← SE RECLASIFICA a «Cheques»
  Consulta > Cheques
  Procesos > Solicitud Pagos · Cheques Generados · Cheques Impresos · Cheques Entregados
```

### Después

```
Cheques                       ← reemplaza al nodo «Pagos», en el mismo lugar
  Solicitud de pago
  Cheques generados
  Cheques impresos
  Cheques entregados
  Consulta de cheques
```

**Plano, sin los subniveles «Consulta» y «Procesos».** Eran cinco pantallas repartidas en dos
carpetas de dos y tres; el árbol costaba más que lo que ordenaba.

**Y el orden es el del ciclo real**, no alfabético: Solicitud → Generados → Impresos → Entregados,
y la consulta al final. Un menú que sigue el flujo se aprende una vez.

### ⛔ Por qué el nodo se llama «Cheques» y no «Pagos»

Tesorería quedaba con **dos** nodos que empiezan igual —«Pagos por transferencia» y «Pagos»— y son
circuitos distintos: uno transfiere y el otro gira cheques. **Dos nombres parecidos en el mismo menú
es cómo se pierde media hora buscando una pantalla**, y ya estaba anotado como riesgo al reorganizar
el circuito de pagos. Renombrarlo a «Cheques» dice lo que hay adentro.

### Lo que NO se toca

⛔ `app.routes.ts` **no se toca**: las rutas de Cobros siguen vivas (decisión M1) y las de cheques
no cambian de path. Sólo cambia el menú.
⛔ Ni un componente de Cobros se borra.
⛔ El nodo `Parametrización > Bancos > Mis Bancos > Chequeras > Cheques` es **otra cosa** —
parametrización de chequeras, no el ciclo de pago— y se queda donde está.

---

## 2. Las cinco pantallas: qué les falta, medido

No se preguntó qué molestaba de cada una: se midió. Las cinco tienen **exactamente la misma
carencia**, y es la que el usuario ya había señalado en el mayor analítico —*«muy limitada por
scroll… incómoda»*—.

| Pantalla | `matSort` | Paginación | Filtro |
|---|---|---|---|
| Solicitud de pago | ❌ | ❌ | ⚠️ mínimo |
| Cheques generados | ❌ | ❌ | ✅ |
| Cheques impresos | ❌ | ❌ | ✅ |
| Cheques entregados | ❌ | ❌ | ✅ |
| Consulta de cheques | ❌ | ❌ | ✅ |

**Ninguna de las cinco ordena por columna. Ninguna pagina.** Las cinco usan `mat-table` renderizando
todas las filas, así que la página entera crece con los datos y el encabezado se pierde al bajar.

### Lo que se hace, y es lo mismo en las cinco

1. **`matSort` en todas las columnas**, con orden inicial por la fecha o el número de cheque
   —lo que la pantalla ya use como criterio natural—.
2. **`mat-paginator`**, 25/50/100, con el total visible.
3. **La tabla scrollea, no la página**: alto propio con `overflow: auto` y encabezado `sticky`.
   Es la corrección que se hizo en el mayor analítico V2 y es la que resuelve la queja de fondo.
4. **Filtro rápido en «Solicitud de pago»**, que es la única que casi no tiene.

⛔ **No se toca la lógica de negocio de ninguna**: ni cómo se genera un cheque, ni cómo se marca
impreso o entregado, ni ninguna llamada al backend. **Es presentación.**

### Por qué no se rehacen como el mayor analítico V2

Ahí se creó una pantalla nueva **porque la vieja se usa todos los días y no se apaga el mismo día
que nace su reemplazo**. Acá no aplica: son cambios aditivos sobre la misma pantalla —ordenar,
paginar, scrollear— que no cambian lo que la pantalla hace ni cómo se opera. Duplicar cinco
pantallas para eso sería cargar al usuario con diez.

---

## 3. Riesgos

| # | Riesgo | Mitigación |
|---|---|---|
| 1 | 🟠 Alguien usa una pantalla de Cobros y deja de encontrarla | Las rutas quedan vivas (M1): el enlace guardado sigue funcionando, y volver a mostrarla en el menú es una línea |
| 2 | 🟠 `tsr` está compartido con `lap-saa-1`, y el menú es un archivo de alto tráfico | `git status` y `git log -3` sobre `menutesoreria.component.ts` antes de editarlo; si hay algo sin commitear, parar |
| 3 | 🟡 Ordenar por una columna formateada ordena el texto, no el valor | El `sortingDataAccessor` devuelve el dato crudo para fechas e importes, no la cadena mostrada |

---

## 4. ⛔ «Solicitud de pago» de Tesorería: era una maqueta, y se BORRÓ

**2026-09-07.** La decisión M2 de este mismo plan decía que esa pantalla entraba al nodo de cheques
como primer paso del ciclo. **Estaba mal, y se revirtió el mismo día.**

### 📌 EL COMMIT QUE LA BORRA — anotado acá a pedido del usuario

| | |
|---|---|
| **Repositorio** | `saaFE` |
| **Commit** | **`9d091b9`** |
| **Mensaje** | `tsr(omen2): BORRADA la pantalla mock "Solicitud de pago" de Tesoreria` |

**Para recuperar el código fuente**, desde el commit anterior:

```bash
git show 9d091b9~1:src/app/modules/tsr/forms/pagos/procesos/solicitud/solicitud-pagos.component.ts
git show 9d091b9~1:src/app/modules/tsr/forms/pagos/procesos/solicitud/solicitud-pagos.component.html
git show 9d091b9~1:src/app/modules/tsr/forms/pagos/procesos/solicitud/solicitud-pagos.component.scss
```

Se borraron los tres archivos, su ruta en `app.routes.ts` y su entrada del menú.

### Por qué

La pantalla era **enteramente simulada**: sus filas eran un arreglo escrito a mano y «aprobar
seleccionadas» hacía `console.log` + `setTimeout`. **Cero llamadas al backend, ningún servicio
inyectado.**

**La solicitud de pago real es la de CxP** — `cxp/forms/pagos/solicitud-pago/`, con cinco servicios
reales, construida el mismo día en la reorganización del circuito de pagos. Vive en
**Cuentas por Pagar → Solicitud de pago** (`/menucuentaxpagar/pagos/solicitud`) y **no se tocó**.

### 🔴 Cómo se coló, y es un error de método del árbitro

Al escribir este plan puse esa pantalla **primera en el ciclo de cheques**, basándome en **su nombre
y en su lugar en el árbol viejo**. No abrí el componente. El ejecutor lo abrió para agregarle
ordenamiento y encontró que no tenía backend.

> **La había puesto justo donde más daño hacía: la primera que alguien iba a abrir.**
>
> **Un nombre en un menú no es evidencia de qué hace la pantalla.** En un sistema con esta cantidad
> de piezas viejas conviviendo, el rótulo y el contenido se separan sin que nadie lo note — es la
> misma familia que `TipoCuentasBancarias`, cuyo nombre decía `CORRIENTE = 1` mientras el catálogo
> decía lo contrario. **Antes de mover una pantalla de lugar en un menú, hay que abrirla.**

### Por qué se borró en vez de esconderla

El usuario lo pidió explícitamente —*«dala por muerta y bórrala de una vez»*— y el criterio es
correcto: **una pantalla que muestra datos inventados y responde «aprobado» sin aprobar nada es peor
que no tenerla.** El que la abre cree que hizo algo. El historial de git conserva el código, así que
no se pierde nada; sólo deja de estar viva.

⚠️ Se borró también la ruta, no sólo el menú: **una ruta que importa un componente inexistente rompe
el build.** El `ng build` con exit 0 fue la verificación de que no quedó ninguna huérfana.

### El nodo Cheques queda así

```
Cheques
  Cheques generados
  Cheques impresos
  Cheques entregados
  Consulta de cheques
```
