# md2docx — Markdown a Word sin Word

`md2docx.ps1` convierte un `.md` del repositorio en un `.docx` real (ZIP con WordprocessingML),
usando sólo Windows PowerShell y .NET. Nació el 2026-09-10 para producir el acta de
entrega-recepción y el memorando regulatorio en una máquina sin Word ni pandoc.

```
powershell -NoProfile -ExecutionPolicy Bypass -File tools\docx\md2docx.ps1 -In docs\contractual\ACTA-ENTREGA-RECEPCION-DEFINITIVA-SAA.md -Out C:\ruta\ACTA.docx
```

Soporta: `#`/`##`/`###`, párrafos (líneas seguidas se unen con espacio), `**negrita**`, tablas
`| a | b |` con fila de encabezado sombreada, viñetas `- `, listas `1. `, `&nbsp;` como párrafo
vacío, y `---` como salto de página **sólo** si lo sigue un `# ` (los anexos). Arial 11,
justificado, márgenes de una pulgada.

## Dos trampas que ya costaron una vuelta cada una

- **`ZipFile.CreateFromDirectory` de .NET Framework escribe las entradas con barra invertida**
  (`word\document.xml`) y Word abre el archivo como dañado. Las entradas se crean a mano con `/`.
- **Los `Add-Type` de `System.IO.Compression` van antes de cualquier literal de tipo**: Windows
  PowerShell los resuelve al analizar el script, no al ejecutarlo.

Las tablas salen con columnas del mismo ancho; en Word se ajustan arrastrando el borde. Un bloque
de firmas debe ir con cada línea como párrafo aparte (línea en blanco entre ellas), o se fusionan.
