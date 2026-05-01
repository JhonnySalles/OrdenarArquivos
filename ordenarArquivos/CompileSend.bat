@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   Compilando e Enviando para Google Drive
echo ============================================================

echo [1/2] Compilando projeto com Maven...
call mvn clean package -Dmaven.test.skip=true

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha na compilacao do projeto.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Enviando JARs para o Google Drive...

:: Busca dinamicamente os JARs na pasta target
set JARS=
for %%f in (target\ordenarArquivos*.jar) do (
    if not defined JARS (
        set "JARS=%%f"
    ) else (
        set "JARS=!JARS! %%f"
    )
)

if not defined JARS (
    echo [ERRO] Nenhum arquivo JAR encontrado em target/.
    pause
    exit /b 1
)

:: Executa o utilitário de upload passando todos os JARs encontrados
call mvn exec:java -Dexec.mainClass="com.fenix.ordenararquivos.service.GoogleDriveUploadService" -Dexec.args="!JARS!"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha ao enviar arquivos para o Google Drive.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================================
echo   PROCESSO CONCLUIDO COM SUCESSO!
echo ============================================================
pause
