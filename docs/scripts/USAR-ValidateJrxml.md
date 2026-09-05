# Validar un `.jrxml` contra el motor real, sin Jaspersoft Studio

Hallazgo del 2026-09-05: un `.jrxml` bien formado (XML válido) puede igual romper Jaspersoft
Studio / JasperReports 7.0.3 al abrirlo, porque el formato compacto que usa Studio 7
(`<element kind="staticText" ...>`) no tiene XSD — se deserializa directo a las clases Java de
diseño (`JRDesignStaticText`, `JRDesignTextField`, `JRDesignBand`, ...) vía Jackson
(`net.sf.jasperreports.engine.xml.JacksonReportLoader`, ya dentro de `jasperreports-7.0.3.jar`,
que el `pom.xml` del proyecto ya trae). Errores típicos que "bien formado" no detecta:

- Un `<element kind="staticText">` con `<expression>` en vez de `<text>` (o al revés en un
  `textField`) — `UnrecognizedPropertyException` sobre `JRDesignStaticText`/`JRDesignTextField`.
- `<summary>`, `<title>`, `<pageHeader>`, `<pageFooter>`, `<columnHeader>`, `<columnFooter>`
  llevan `height`/`splitType` como atributos **directos** del tag y los `<element>` sueltos
  adentro — **sin** un `<band>` anidado. `<detail>`, `<groupHeader>` y `<groupFooter>` sí llevan
  un (o más) `<band height="..." splitType="...">` anidado. Mezclar los dos estilos da
  `UnrecognizedPropertyException` sobre `JRDesignBand` (campo `"band"` no reconocido).

⚠️ **Trampa aparte, y esta SÍ la agarra el chequeo de "XML bien formado" — pero solo si te
acordás de correrlo primero.** Los comentarios `<!-- ... -->` de cabecera (documentando límites
del modelo, decisiones, etc.) son casi siempre varias líneas largas con guiones como los de este
párrafo — un doble guion `--` dentro de un comentario XML es **XML inválido** (la especificación
lo prohíbe explícitamente), y un editor de texto no lo avisa. Pasó dos veces seguidas en este
reporte: usar `--` como raya larga en la documentación del propio `.jrxml`. Antes de escribir un
comentario de cabecera largo, usar `:` o `;` donde iría una raya — nunca `--`.

`ValidateJrxml.java` (en esta misma carpeta) corre el mismo `JacksonReportLoader` que usa Studio,
sin necesitar Studio instalado, y sin compilar el reporte (no genera `.jasper`, no necesita
`jasperreports-jdt` — eso sigue siendo tarea de Studio, ver
`docs/logica-negocio/crd/API-PAGO-PENSION-COMPLEMENTARIA.md` §8 para el porqué).

## Procedimiento (PowerShell o Git Bash, con `mvn`/JDK 21 en el PATH)

1. **Armar el classpath desde el propio `pom.xml` del proyecto** (solo lectura — no modifica el
   `pom`, solo resuelve sus dependencias ya declaradas):
   ```
   mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
   ```
2. **Compilar el validador** (una sola vez, o cuando cambie de versión JasperReports):
   ```
   javac -cp @cp.txt -d . docs/scripts/ValidateJrxml.java
   ```
   (en PowerShell, si `@cp.txt` no expande, usar `-cp (Get-Content cp.txt)`)
3. **Correrlo contra el `.jrxml` a entregar**:
   ```
   java -cp ".;$(cat cp.txt)" ValidateJrxml src/main/resources/rep/<modulo>/<NOMBRE>.jrxml
   ```
   Windows nativo (no Git Bash) usa `;` como separador de classpath; ajustar si se corre desde
   otra shell.

Salida esperada si está bien: `VALIDO - JasperDesign cargado OK: <nombre> - parametros=N - campos=N - variables=N - grupos=N`.
Si falla, imprime la excepción real de Jackson con la línea/columna del `.jrxml` — la misma que
vería el usuario al abrirlo en Studio, pero antes de mandárselo.

**No reemplaza compilar el `.jasper`** (eso sigue haciéndolo Studio) — solo evita entregar un
`.jrxml` que ni siquiera abre.
