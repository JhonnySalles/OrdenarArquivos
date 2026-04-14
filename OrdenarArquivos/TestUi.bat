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
:: Exemplo: mvn test -Dgroups="UI" -Dexcluded.groups="" -Dtest=AbaArquivoUiTest
mvn test -Dgroups=UI -Dexclude.groups= -Dtest=AbaArquivoUiTest
pause
