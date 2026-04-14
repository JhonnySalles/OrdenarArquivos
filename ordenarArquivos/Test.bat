@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando os testes unitários do programa
echo ===================================================
echo.

:: Executa os testes unitários do programa

mvn test -Dexclude.groups=UI
pause