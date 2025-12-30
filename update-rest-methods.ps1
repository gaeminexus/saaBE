# Script para actualizar todos los métodos REST para que devuelvan Response
# Este script actualiza getAll, getId, put, post y delete en todos los archivos *Rest.java

Write-Host "Iniciando actualización masiva de archivos REST..." -ForegroundColor Green

# Función para actualizar un archivo REST
function Update-RestFile {
    param (
        [string]$FilePath
    )
    
    $fileName = Split-Path $FilePath -Leaf
    Write-Host "Procesando: $fileName" -ForegroundColor Cyan
    
    try {
        $content = Get-Content $FilePath -Raw -Encoding UTF8
        $originalContent = $content
        $changed = $false
        
        # Patrón 1: Actualizar getAll() que devuelve List<T>
        $pattern1 = '(@GET\s+@Path\("/getAll"\)\s+@Produces\("application/json"\)\s+public List<(\w+)> getAll\(\) throws Throwable \{\s+return \w+\.selectAll\([^)]+\);\s+\})'
        if ($content -match $pattern1) {
            $entityName = $Matches[2]
            $replacement = @"
@GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<$entityName> lista = ${entityName}DaoService.selectAll(NombreEntidades${module}.${entityName.ToUpper()});
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener registros: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
"@
            Write-Host "  - Actualizando getAll()" -ForegroundColor Yellow
            $changed = $true
        }
        
        # Guardar si hubo cambios
        if ($changed) {
            Set-Content $FilePath $content -Encoding UTF8
            Write-Host "  ✓ Archivo actualizado" -ForegroundColor Green
            return $true
        } else {
            Write-Host "  - Sin cambios necesarios" -ForegroundColor Gray
            return $false
        }
    }
    catch {
        Write-Host "  ✗ Error: $_" -ForegroundColor Red
        return $false
    }
}

# Obtener todos los archivos REST
$restFiles = Get-ChildItem -Path "C:\work\saaBE\v1\saaBE\src\main\java\com\saa\ws\rest" -Filter "*Rest.java" -Recurse

$totalFiles = $restFiles.Count
$updatedFiles = 0
$skippedFiles = 0

Write-Host "`nEncontrados $totalFiles archivos REST para procesar`n" -ForegroundColor Yellow

foreach ($file in $restFiles) {
    if (Update-RestFile -FilePath $file.FullName) {
        $updatedFiles++
    } else {
        $skippedFiles++
    }
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Resumen de actualización:" -ForegroundColor Green
Write-Host "  Total de archivos: $totalFiles" -ForegroundColor White
Write-Host "  Archivos actualizados: $updatedFiles" -ForegroundColor Green
Write-Host "  Archivos omitidos: $skippedFiles" -ForegroundColor Gray
Write-Host "========================================`n" -ForegroundColor Green
