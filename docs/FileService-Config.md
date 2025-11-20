# Configuración del Servicio de Archivos para Producción

## Descripción
El servicio FileService maneja upload y download de archivos con configuración automática del directorio de uploads basado en el entorno.

## Configuración del Directorio de Uploads

El servicio busca el directorio de uploads en el siguiente orden de prioridad:

1. **Variable de sistema JVM** (Recomendado para producción)
2. **Variable de entorno del sistema**
3. **Directorio por defecto según el SO**

### Configuración para WildFly en Producción

#### Opción 1: Variable de sistema JVM (Recomendado)

**Linux/Unix:**
Editar el archivo `$WILDFLY_HOME/bin/standalone.conf`:
```bash
JAVA_OPTS="$JAVA_OPTS -Dsaa.upload.dir=/opt/saa-uploads/"
```

**Windows:**
Editar el archivo `%WILDFLY_HOME%\bin\standalone.conf.bat`:
```batch
set "JAVA_OPTS=%JAVA_OPTS% -Dsaa.upload.dir=C:\saa-uploads\"
```

#### Opción 2: Variable de entorno del sistema

**Linux/Unix:**
```bash
export SAA_UPLOAD_DIR="/opt/saa-uploads/"
```

**Windows:**
```batch
set SAA_UPLOAD_DIR=C:\saa-uploads\
```

#### Opción 3: Configuración vía WildFly CLI

```bash
/system-property=saa.upload.dir:add(value="/opt/saa-uploads/")
```

### Directorios por Defecto

Si no se configura ninguna variable, el sistema usará:

- **Windows:** `C:\Users\[usuario]\saa-uploads\`
- **Linux/Unix:** `/opt/saa-uploads/`

## Configuración de Permisos

### Linux/Unix
```bash
# Crear el directorio
sudo mkdir -p /opt/saa-uploads

# Asignar permisos al usuario de WildFly
sudo chown wildfly:wildfly /opt/saa-uploads
sudo chmod 755 /opt/saa-uploads
```

### Windows
```batch
# Crear el directorio
mkdir C:\saa-uploads

# Asignar permisos completos al usuario de WildFly
icacls C:\saa-uploads /grant "wildfly-user:(OI)(CI)F"
```

## Características del Servicio

### Extensiones de Archivos Permitidas
- Documentos: `.pdf`, `.doc`, `.docx`, `.txt`
- Hojas de cálculo: `.xls`, `.xlsx`
- Imágenes: `.jpg`, `.jpeg`, `.png`, `.gif`

### Tamaño Máximo
- **Límite:** 10 MB por archivo

### Funcionalidades
- Upload con validación de extensión y tamaño
- Generación automática de nombres únicos
- Download de archivos
- Eliminación de archivos
- Listado de archivos en directorio
- Verificación de existencia de archivos

## Uso del Servicio

### Inyección en clases EJB
```java
@EJB
private FileService fileService;
```

### Ejemplos de uso

#### Upload de archivo
```java
String rutaArchivo = fileService.uploadFile(inputStream, "documento.pdf");
```

#### Download de archivo
```java
InputStream archivo = fileService.downloadFile(rutaArchivo);
```

#### Validar archivo antes de upload
```java
if (fileService.validarExtension("documento.pdf") && 
    fileService.validarTamaño(archivoSize)) {
    // Proceder con upload
}
```

## Troubleshooting

### Problemas Comunes

1. **Permisos insuficientes**
   - Verificar permisos del directorio de uploads
   - Asegurar que el usuario de WildFly tenga acceso

2. **Directorio no encontrado**
   - Verificar que las variables estén configuradas correctamente
   - Comprobar que el directorio exista y sea accesible

3. **Archivos no se guardan**
   - Verificar espacio en disco
   - Comprobar logs de WildFly para errores específicos

### Logs
Los errores se registran en los logs de WildFly:
- **Linux:** `$WILDFLY_HOME/standalone/log/server.log`
- **Windows:** `%WILDFLY_HOME%\standalone\log\server.log`

## Verificación de Configuración

Para verificar qué directorio está usando el servicio, puedes agregar logging temporal:

```java
String uploadDir = getUploadDirectory();
System.out.println("Directorio de uploads configurado: " + uploadDir);
```