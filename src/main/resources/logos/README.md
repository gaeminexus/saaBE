# Carpeta de Logos para Documentos Electrónicos

Esta carpeta contiene los logos que se utilizan en los documentos electrónicos (facturas, notas de crédito, etc.).

## Nomenclatura de Archivos

Los logos deben seguir este formato de nombre:

```
logo_facturador_{ID_FACTURADOR}.png
```

### Ejemplos:
- `logo_facturador_1.png` → Logo del facturador con ID 1
- `logo_facturador_123.png` → Logo del facturador con ID 123

## Formato Recomendado

- **Formato:** PNG (con transparencia)
- **Tamaño recomendado:** 200x80 píxeles
- **Peso máximo:** 100 KB

## Uso en el Sistema

El sistema busca automáticamente el logo correspondiente al facturador cuando:
- Se genera una factura electrónica
- Se genera una nota de crédito
- Se genera una nota de débito
- Se genera una retención
- Se genera una liquidación de compra

Si no existe el logo para un facturador específico, el sistema continuará sin error pero el PDF no incluirá logo.

## Ubicación en el Proyecto

```
saaBE/
  src/
    main/
      resources/
        logos/           ← AQUÍ van los logos
          logo_facturador_1.png
          logo_facturador_2.png
          ...
```

## Nota Importante

Los logos colocados aquí estarán disponibles en el classpath de la aplicación y serán incluidos en el archivo WAR desplegado en WildFly.
