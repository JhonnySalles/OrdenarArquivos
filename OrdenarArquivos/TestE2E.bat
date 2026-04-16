@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando os testes E2E (Ponta a Ponta)
echo ===================================================
echo.

:: Executa os testes marcados com a tag @Tag("E2E")
:: Esses testes validam o fluxo completo das abas do sistema.
mvn test -Dgroups=E2E -Dexclude.groups=
pause
