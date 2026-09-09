# Changelog V8.5.1

Correção de build da V8.5.

- Removida a dependência direta de `BuildConfig.VERSION_NAME` na tela Sobre/Créditos.
- A versão do app agora é lida pelo `PackageManager`, evitando `Unresolved reference: BuildConfig` em configurações onde o BuildConfig não é gerado.
- Mantidos versionName 1.5.5 e versionCode 14, pois esta é uma correção do pacote de projeto antes da publicação.
