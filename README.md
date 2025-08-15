# Compilador **Lang** — Entregas 1 & 2

Implementação de um compilador/interpretador para a linguagem **Lang**, com:

* **Análise léxica e sintática** (`-syn`)
* **Análise semântica e verificação de tipos** (`-t`)
* **Interpretação** (`-i`)
* **Geração de código *source-to-source*** (`-src`)
* **Geração de código de baixo nível (Jasmin/JVM)** (`-gen`)

O projeto inclui gramáticas, AST, *type checker*, interpretador e *backends* de geração de código, além de testes automatizados.

---

## Sumário

* [Pré-requisitos](#pré-requisitos)
* [Build rápido](#build-rápido)
* [Estrutura do repositório](#estrutura-do-repositório)
* [Uso do CLI (todas as diretivas)](#uso-do-cli-todas-as-diretivas)
* [Exemplos rápidos](#exemplos-rápidos)
* [Códigos de saída](#códigos-de-saída)
* [Erros de *runtime*](#erros-de-runtime)
* [Checagens semânticas e de tipos](#checagens-semânticas-e-de-tipos)
* [Geração de código](#geração-de-código)
* [Testes](#testes)
* [Checklist de conformidade (Entrega 2)](#checklist-de-conformidade-entrega-2)
* [Solução de problemas (FAQ)](#solução-de-problemas-faq)
* [Licença](#licença)

---

## Pré-requisitos

* **Java 17+**

  ```bash
  java -version
  ```
* **Maven 3.6+**

  ```bash
  mvn -version
  ```

---

## Build rápido

Compile e gere artefatos:

```bash
mvn clean package
```

O *fat JAR* ficará em:

```
target/lang-*-jar-with-dependencies.jar
```

Atalho de CI local (build + testes + sumário):

```bash
./all.sh
```

> Se estiver no Windows: rode os comandos Maven direto no terminal (o `all.sh` é opcional).

---

## Estrutura do repositório

```
.
  README.md
  all.sh
  pom.xml
  src/
    main/
      antlr4/           # Gramáticas (léxico/sintaxe)
      java/             # AST, checker, interpretador, geradores de código e CLI
    test/
      java/             # Testes de unidade
      resources/        # Programas de exemplo / casos de teste
```

---

## Uso do CLI (todas as diretivas)

Formato geral:

```bash
java -jar target/lang-*-jar-with-dependencies.jar <flag> <arquivo.lang>
```

### `-syn` — Análise sintática

Valida a sintaxe e informa **accept** ou **reject**.

```bash
java -jar target/lang-*-jar-with-dependencies.jar -syn programa.lang
# Saída: "accept"  ou  "reject"
```

### `-t` — Verificação de tipos (análise semântica)

Executa o *type checker* e reporta erros semânticos/tipos.

```bash
java -jar target/lang-*-jar-with-dependencies.jar -t programa.lang
# Saída: "OK" ou relatório de erros (linha/coluna/mensagem)
```

### `-i` — Interpretador

Executa o programa e imprime sua saída.

```bash
java -jar target/lang-*-jar-with-dependencies.jar -i programa.lang
```

### `-src` — Geração *source-to-source*

Gera código de alto nível equivalente (ex.: Java ou *pretty print* da própria Lang, conforme configuração do CLI).

```bash
java -jar target/lang-*-jar-with-dependencies.jar -src programa.lang
```

### `-gen` — Geração de baixo nível (Jasmin/JVM)

Emite código de montagem Jasmin (`.j`) compatível com a JVM.

```bash
java -jar target/lang-*-jar-with-dependencies.jar -gen programa.lang
```

> Observação: a geração de Jasmin imprime o código na saída padrão; você pode redirecionar para um arquivo `.j` se desejar.

---

## Exemplos rápidos

Crie um arquivo mínimo:

```bash
cat > teste.lang <<'EOF'
main() {
  print 42
}
EOF
```

* **Sintaxe**

  ```bash
  java -jar target/lang-*-jar-with-dependencies.jar -syn teste.lang
  # accept
  ```
* **Tipos**

  ```bash
  java -jar target/lang-*-jar-with-dependencies.jar -t teste.lang
  # OK
  ```
* **Interpretar**

  ```bash
  java -jar target/lang-*-jar-with-dependencies.jar -i teste.lang
  # 42
  ```
* **Source-to-source**

  ```bash
  java -jar target/lang-*-jar-with-dependencies.jar -src teste.lang
  # (código gerado equivalente)
  ```
* **Jasmin**

  ```bash
  java -jar target/lang-*-jar-with-dependencies.jar -gen teste.lang > teste.j
  ```

### `iterate` (contagem simples)

```bash
cat > iterate.lang <<'EOF'
main() {
  iterate(i:3) { print i }
}
EOF

java -jar target/lang-*-jar-with-dependencies.jar -i iterate.lang
# 0
# 1
# 2
```

### Vetores

```bash
cat > array.lang <<'EOF'
main() {
  v = [1,2,3];
  print v[1]
}
EOF

java -jar target/lang-*-jar-with-dependencies.jar -i array.lang
# 2
```

### Comentário de bloco (não aninhado)

```bash
cat > comment.lang <<'EOF'
main() {
  {- comentário não-aninhado -}
  print 42
}
EOF

java -jar target/lang-*-jar-with-dependencies.jar -i comment.lang
# 42
```

---

## Códigos de saída

| Código | Significado                                                      |
| -----: | ---------------------------------------------------------------- |
|      0 | Sucesso (*accept* / execução sem erros)                          |
|      1 | Erro de sintaxe ou uso incorreto do CLI                          |
|      2 | Função `main` não encontrada                                     |
|      3 | Erro de *runtime* (divisão por zero, índice fora de faixa, etc.) |

---

## Erros de *runtime*

Detectados e reportados em tempo de execução:

* **Divisão por zero**: `1/0`, `1.0/0.0`
* **Módulo por zero**: `5%0`
* **Índice fora de faixa**: `v[5]` quando `v` não possui esse índice
* **Condição não-booleana**: `if 1 then ...` (apenas `true`/`false` são aceitos)

---

## Checagens semânticas e de tipos

Principais validações:

* **`main()`**: obrigatória, sem parâmetros; retorno conforme a especificação do dialeto adotado (neste projeto, `main` não retorna valor).
* **Declarações e escopos**: identificadores declarados antes do uso; proibição de duplicatas em escopos incompatíveis.
* **Atribuições**: tipo do RHS compatível com o LHS (inclui vetores).
* **Operadores aritméticos/relacionais/lógicos**: operandos de tipos compatíveis; resultado tipado corretamente.
* **Controle de fluxo**: guardas (`if`, `while`, `iterate`) aceitam **Bool**; *lvalues* válidos em atribuições e `read`.
* **I/O**:

  * `print` aceita primitivos (ex.: **Int**, **Float**, **Bool**, **Char**).
  * `read` lê para *lvalues* primitivos compatíveis (ex.: **Int**, **Float**, **Bool**, **Char**).
* **Vetores**: `T[]` com checagem de elemento em acesso/atribuição; tamanho verificado em *runtime*.
* **Funções/procedimentos**: checagem de número/tipos de argumentos; retornos nos blocos corretos.

> Observação: o projeto pode incluir extensões opcionais (ex.: operadores adicionais, açúcares sintáticos). Elas são aceitas quando não conflitam com a especificação base.

---

## Geração de código

### `-src` — alto nível

* Emite código equivalente de alto nível.
* Pode operar como *pretty-printer* da própria Lang **ou** gerar Java, dependendo da configuração do CLI.
* Útil para inspeção semântica e como etapa intermediária de depuração.

### `-gen` — Jasmin/JVM

* Emite *assembly* Jasmin (`.j`) visando a JVM.
* Estruturas de controle, chamadas, vetores e I/O são traduzidos para instruções de pilha.
* O objetivo é manter a **equivalência de comportamento** com o interpretador.

---

## Testes

* **Unitários** (Maven Surefire):

  ```bash
  mvn test
  ```
* **Build + Testes + Sumário**:

  ```bash
  ./all.sh
  ```

> Casos de teste adicionais encontram-se em `src/test/resources`. Você pode adicionar programas Lang para ampliar a cobertura de sintaxe, tipos e execução.

---

## Checklist de conformidade (Entrega 2)

* [x] **`-syn`**: aceita/rejeita com base no parser.
* [x] **`-t`**: *type checker* com mensagens claras (linha/coluna).
* [x] **`-i`**: interpretador funcional cobrindo as construções da linguagem.
* [x] **`-src`**: geração de alto nível (pretty-print ou Java).
* [x] **`-gen`**: Jasmin/JVM com equivalência de comportamento ao interpretador.
* [x] **Erros de runtime** cobertos (divisão/módulo por zero, *out of bounds*, guardas não-booleanas).
* [x] **Estrutura de projeto** (ANTLR, AST, checker, intérprete, *backends*) organizada.
* [x] **Testes automatizados** e *script* de build/execução local.

---

## Solução de problemas (FAQ)

**“`java: command not found`”**
Instale o Java 17+ e configure `JAVA_HOME`. No Windows, adicione ao `PATH`.

**“`mvn: command not found`”**
Instale o Maven 3.6+ e configure `MAVEN_HOME`. No Windows, adicione ao `PATH`.

**“JAR não encontrado”**
Rode `mvn clean package` e verifique `target/`.

**“Permission denied” no `all.sh`**

```bash
chmod +x all.sh
```

**“BUILD FAILURE”**
Verifique a versão do Java (17+) e execute `mvn clean package` novamente.

---