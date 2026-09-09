# V7.3 / 1.4.3

- Maestrias: adiciona fallback por WebView invisível para ler o DOM depois que o JavaScript do CellString terminar de carregar.
- Mantém o parser HTML rápido como primeira tentativa.
- Se o HTML inicial não contiver maestrias, o app renderiza o perfil, espera a hidratação da página e tenta novamente.
- Cacheia as maestrias encontradas para uso posterior/offline.
- Mensagem de diagnóstico melhor quando a fonte não expõe os dados.
