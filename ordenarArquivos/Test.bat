@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando os testes unitários do programa
echo ===================================================
echo.

:: Executa os testes unitários do programa
:: -Dexclude.groups=UI,E2E : Exclui testes marcados com a tag @Tag("UI") e @Tag("E2E")
:: -Dtest=AbaArquivoControllerTest : Executa apenas os testes da classe AbaArquivoControllerTest
:: Exemplo: mvn test -Dexclude.groups=UI,E2E -Dtest=AbaArquivoControllerTest
mvn test -Dexclude.groups=UI,E2E
pause