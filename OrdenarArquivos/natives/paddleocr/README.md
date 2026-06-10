# PaddleOCR-json

Baixe o release Windows x64 do [PaddleOCR-json v1.4.1](https://github.com/hiroi-sora/PaddleOCR-json/releases) e extraia **todo o conteúdo do instalador nesta pasta** (`natives/paddleocr/`).

O instalador possui muitos arquivos. **Não misture** com as DLLs do OpenCV em `natives/` — mantenha tudo do PaddleOCR apenas dentro desta subpasta.

Estrutura esperada:

```
natives/
  opencv_java320_64.dll          ← OpenCV (raiz de natives/)
  ...
  paddleocr/                     ← subpasta dedicada ao PaddleOCR
    PaddleOCR-json.exe
    models/
      config_japan.txt
      config_en.txt
      config_latin.txt
      ...
```

O aplicativo resolve o caminho automaticamente em `natives/paddleocr/`.
