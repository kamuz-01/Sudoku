# Sudoku JavaFX

Aplicação de Sudoku feita em Java com JavaFX, com interface gráfica, suporte a números fixos informados na inicialização, inserção e remoção de números, verificação de status e modo de rascunho.

## Visão geral

O projeto atende aos requisitos principais do exercício:

- menu interativo para iniciar novo jogo, reiniciar, limpar, verificar status, finalizar e sair
- inserção de números pelo usuário em células vazias
- remoção de números informados pelo jogador
- preservação dos números fixos do tabuleiro
- verificação de tabuleiro, status e conflitos
- modo de rascunho para candidatos em cada célula
- interface JavaFX com visual estilizado

## Tecnologias usadas

- Java 21
- JavaFX 21
- FXML
- CSS para estilização da interface
- Eclipse como ambiente de desenvolvimento

## Funcionalidades

- novo jogo com tabuleiros pré-definidos
- carregamento de números iniciais pelos argumentos do `main`
- colocar número em célula vazia
- remover número colocado pelo jogador
- limpar todos os números não fixos
- verificar o estado atual do tabuleiro
- verificar se o jogo está não iniciado, incompleto ou completo
- detectar erros por conflito de números
- modo rascunho com candidatos dentro das células
- seleção de célula com teclado: digite um número para preencher ou usar rascunho, e Backspace/Delete para limpar a célula selecionada

## Estrutura do projeto

- `src/module-info.java` - módulo JavaFX do projeto
- `src/org/JogoSudoku/Main.java` - ponto de entrada da aplicação
- `src/org/JogoSudoku/Sample.fxml` - layout principal da interface
- `src/org/JogoSudoku/SampleController.java` - lógica da interface e das ações do jogador
- `src/org/JogoSudoku/SudokuGame.java` - regras e estado do tabuleiro
- `src/org/JogoSudoku/application.css` - estilos visuais da interface

## Como executar

### No Eclipse

1. Importe o projeto como um projeto Java existente.
2. Configure o JDK 21.
3. Garanta que o JavaFX SDK esteja apontado no module path do projeto.
4. Execute a classe `org.JogoSudoku.Main`.

### Pela linha de comando

Compile a partir da raiz do projeto:

```bash
javac --module-path "C:\Program Files\Java\javafx-sdk-21.0.9\lib" --add-modules javafx.controls,javafx.fxml -d out src\module-info.java src\org\JogoSudoku\*.java
```

Execute em seguida:

```bash
java --module-path "C:\Program Files\Java\javafx-sdk-21.0.9\lib;out" --add-modules javafx.controls,javafx.fxml --module Sudoku/org.JogoSudoku.Main
```

## Formato dos argumentos iniciais

O jogo aceita números iniciais passados por argumentos do `main`.

### Formato amigável recomendado

- `r1c1=5`
- `linha1coluna1=5`
- `1,1=5`
- `1/1=5`

Você pode informar vários valores, um por argumento ou em texto separado.

### Formato simples legado

- trincas de números no estilo `linha coluna valor`
- exemplo: `1 1 5 1 2 3 2 3 4`

## Controles do jogo

- `Novo jogo` - carrega uma nova partida pré-definida
- `Reiniciar` - restaura o tabuleiro inicial da partida atual
- `Colocar` - insere um número ou um rascunho, conforme o modo selecionado
- `Remover` - remove número ou rascunho da célula selecionada
- `Verificar` - exibe a situação atual do tabuleiro
- `Status` - mostra o status do jogo e se há erros
- `Limpar` - remove os números informados pelo jogador
- `Finalizar` - encerra o jogo quando o tabuleiro está completo e válido
- `Modo rascunho` - alterna entre inserção de valor final e candidatos

## Regras implementadas

- números fixos não podem ser removidos ou alterados
- não é permitido inserir número em célula já ocupada
- o jogo detecta conflitos entre linhas, colunas e blocos 3x3
- o estado pode ser não iniciado, incompleto ou completo
- o jogo só pode ser finalizado quando o tabuleiro estiver completo e válido

## Recursos visuais

O projeto usa uma interface estilizada com:

- tabuleiro com separação clara dos blocos 3x3
- células com destaque para seleção, erro, fixo e preenchido
- botões com aparência consistente
- modo escuro com contraste forte

As imagens de referência do exercício estão na raiz do projeto:

- `sudoku.png`
- `draft.png`

## Licença

Este projeto está licenciado sob a licença MIT. Veja o arquivo `LICENSE`.
