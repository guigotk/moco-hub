# mo.co hub

> **V1.5.5:** melhorias de primeiro uso, validação do Hunter ID, retry de consultas, cache reforçado, tela Sobre e limpeza de código antigo.


Companion app comunitário e não oficial para **mo.co**, desenvolvido para Android.

> Eventos, alertas, temporada, perfil de Hunters, Elite Contracts, notícias e comunidade em um só lugar.

## Versão atual

**1.5.5 — versionCode 14**

## Recursos

- Home com próximo evento, temporada, acessos rápidos, comunidade e notícia em destaque.
- Eventos Double Chaos Energy e Overcharged Alert com contagem regressiva e alertas configuráveis.
- Perfil local com foto, nome, idade, bio e Hunter ID.
- Perfil Hunter nativo com Career Level, Collector Level, Elite Merits e indicadores públicos disponíveis.
- Rating calculado localmente: Elite 30% + Grind 30% + Progression 20% + Collection 20%.
- Classificação automática: Recruit, Rookie Hunter, Junior Hunter, Advanced Hunter e Veteran Hunter.
- Elite Contracts com tela nativa e cache offline quando os dados estiverem disponíveis.
- Widget do próximo evento.
- Tema claro, escuro ou sistema e idiomas PT-BR/EN/ES.
- Comunidade: @joinmocobrasil, @joinmocohub, mo.co BR/PT e Quantum.

## Build

Extraia para um caminho simples, por exemplo `C:\AndroidProjects\moco-hub`, abra a pasta no Android Studio e aguarde o Gradle Sync.

No Windows, após o primeiro Sync:

```powershell
.\gradlew.bat clean assembleDebug
```

## Dados externos

Algumas informações públicas dependem de fontes externas e podem ficar temporariamente indisponíveis. O app utiliza cache local em áreas compatíveis e não inventa valores ausentes.

## Suporte

`joinmocohub@outlook.com`

## Aviso

**mo.co hub é um projeto comunitário e não oficial.** Não é afiliado, patrocinado ou endossado pela Supercell. mo.co e demais marcas pertencem aos respectivos proprietários.

## Desenvolvedor

Desenvolvido por **Guigo**.


## V8.5 / 1.5.5

- primeiro uso mais claro quando ainda não existe Hunter ID;
- validação e normalização do Hunter ID;
- botão **Tentar novamente** e mensagens de erro melhores;
- cache preserva campos públicos válidos quando a fonte falha parcialmente;
- tela **Sobre o mo.co hub** mostra versão, fontes, GitHub e aviso legal;
- links externos verificam se existe app compatível antes de abrir;
- código antigo de Weapon Masteries removido definitivamente.

## V8.1 / 1.5.1

- restaura e reforça a leitura do nível da temporada;
- preserva o último nível válido no cache quando a fonte omite o campo;
- perfil abre sempre no topo;
- cards de progressão reorganizados em grade 2x2 para celulares;
- atividades fixadas em modo retrato para evitar o botão de sugestão de rotação sobre a navegação.


## V8.2 / 1.5.2

- remove o card redundante **Resumo do Hunter** da aba Perfil;
- mantém Rating e progressão nos cards próprios;
- mantém as correções da V8.1 para nível da temporada, cache, layout 2x2 e orientação retrato.
