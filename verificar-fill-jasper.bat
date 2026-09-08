@echo off
REM Llena cada .jasper con JREmptyDataSource (sin BD, sin filas) y parametros dummy: atrapa
REM errores que SOLO aparecen al llenar el reporte -- estilos condicionales mal referenciados,
REM subreportes rotos, expresiones que revientan en runtime -- que compilar NO detecta.
REM Ver CLAUDE.md ("Reportes") y tools/jasper/src/tools/jasper/VerificarFill.java.
REM
REM Uso:
REM   verificar-fill-jasper.bat                              -> todos los .jasper de rep\
REM   verificar-fill-jasper.bat ruta\al\REPORTE.jasper [...]  -> solo esos
REM
REM Requiere Maven en el PATH (correr "mvn -v" primero si no estas seguro en esta maquina).

setlocal

set REPO_ROOT=%~dp0
set TOOLS_DIR=%REPO_ROOT%tools\jasper
set BUILD_DIR=%TOOLS_DIR%\target\classes
set CP_FILE=%TOOLS_DIR%\target\classpath.txt

echo === 1/3: armando el classpath (mvn dependency:build-classpath) ===
call mvn -f "%TOOLS_DIR%\pom.xml" -q dependency:build-classpath -Dmdep.outputFile="%CP_FILE%"
if errorlevel 1 (
    echo ERROR: no se pudo armar el classpath. Revisa que Maven este en el PATH ^(mvn -v^).
    exit /b 1
)
REM "set /p VAR=<archivo" trunca en Windows cmd.exe alrededor de los 1024 caracteres --
REM silenciosamente, sin error -- y este classpath supera los 3700. "for /f" no tiene ese
REM limite. Con set /p, esto era EXACTAMENTE la causa del "Class not found" intermitente en
REM los RIDE y del fallo de UPPER() en RPRT_MVMN_APXT: jasperreports-functions, barcode4j,
REM zxing y jasperreports-jdt son los ULTIMOS de la lista y quedaban afuera del classpath real
REM sin ningun aviso -- no era un reporte roto, era la herramienta. Ver CLAUDE.md/"Reportes".
for /f "usebackq delims=" %%C in ("%CP_FILE%") do set "CLASSPATH=%%C"

echo === 2/3: compilando el verificador ===
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
javac -encoding UTF-8 -cp "%CLASSPATH%" -d "%BUILD_DIR%" "%TOOLS_DIR%\src\tools\jasper\VerificarFill.java"
if errorlevel 1 (
    echo ERROR: el verificador no compilo.
    exit /b 1
)

echo === 3/3: llenando los .jasper de src\main\resources\rep ===
pushd "%REPO_ROOT%"
java -cp "%BUILD_DIR%;%CLASSPATH%" tools.jasper.VerificarFill %*
set RESULTADO=%errorlevel%
popd

exit /b %RESULTADO%
