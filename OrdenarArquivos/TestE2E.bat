@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando os testes E2E (Ponta a Ponta)
echo ===================================================
echo.

:: Executa os testes de interface de usuário (E2E)
:: -Dgroups=E2E : Executa apenas os testes marcados com a tag @Tag("E2E")
:: -DexcludedGroups= : Exclui testes marcados com a tag @Tag("UI")
:: -Dtest=AbaArquivoE2EFlowTest : Executa apenas os testes da classe AbaArquivoE2EFlowTest
:: Exemplo: mvn test -Dgroups=E2E -Dexclude.groups=UI -Dtest=AbaArquivoE2EFlowTest
mvn test -Dgroups=E2E -Dexclude.groups=UI
pause
