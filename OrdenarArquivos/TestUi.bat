@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando os testes de UI do programa
echo ===================================================
echo.

:: Executa os testes de interface de usuário (UI) da classe AbaArquivoUiTest
:: -Dgroups="UI" : Executa apenas os testes marcados com a tag @Tag("UI")
:: -DexcludedGroups="" : Exclui testes marcados com a tag @Tag("UI")
:: -Dtest=AbaArquivoUiTest : Executa apenas os testes da classe AbaArquivoUiTest
:: mvn test -Dgroups="UI" -DexcludedGroups="" -Dtest=AbaArquivoUiTest
mvn test -Dgroups="UI" -DexcludedGroups="" -Dtest=AbaArquivoUiTest
pause
