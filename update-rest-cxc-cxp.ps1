# Script para actualizar archivos REST de CXC y CXP
# Actualiza todos los métodos para usar Response en lugar de objetos directos

$modules = @("cxc", "cxp")

foreach ($module in $modules) {
    $path = "C:\work\saaBE\v1\saaBE\src\main\java\com\saa\ws\rest\$module\"
    $files = Get-ChildItem -Path $path -Filter "*Rest.java"
    
    Write-Host "Procesando módulo: $module - Total archivos: $($files.Count)" -ForegroundColor Cyan
    
    foreach ($file in $files) {
        Write-Host "  Actualizando: $($file.Name)" -ForegroundColor Yellow
        
        $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        
        # 1. Cambiar @Produces("application/json") por @Produces(MediaType.APPLICATION_JSON)
        $content = $content -replace '@Produces\("application/json"\)', '@Produces(MediaType.APPLICATION_JSON)'
        
        # 2. Cambiar @Consumes("application/json") por @Consumes(MediaType.APPLICATION_JSON)
        $content = $content -replace '@Consumes\("application/json"\)', '@Consumes(MediaType.APPLICATION_JSON)'
        
        # 3. Eliminar throws Throwable
        $content = $content -replace '\s+throws Throwable', ''
        
        # 4. Eliminar @throws Throwable de comentarios
        $content = $content -replace '\s+\*\s+@throws Throwable\s*\r?\n', ''
        
        # Solo guardar si hubo cambios
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            Write-Host "    ✓ Actualizado" -ForegroundColor Green
        } else {
            Write-Host "    - Sin cambios" -ForegroundColor Gray
        }
    }
}

Write-Host "`n✓ Proceso completado!" -ForegroundColor Green
