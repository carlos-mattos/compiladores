# Compilador Lang - Entrega 1

Compilador para a linguagem Lang com análise sintática e interpretação funcional.

## Exit Codes

| Código | Significado |
|--------|-------------|
| 0 | Sucesso (accept) |
| 1 | Erro de sintática (reject) |
| 2 | Função main não encontrada |
| 3 | Erro de runtime (divisão por zero, índice fora de faixa, etc.) |

## Runtime Errors

O compilador detecta e reporta os seguintes erros de runtime:
- **Divisão por zero**: `1/0`, `1.0/0.0`
- **Módulo por zero**: `5%0`
- **Índice fora de faixa**: `v[5]` quando `v` tem menos de 6 elementos
- **Condição não-booleana**: `if 1 then ...` (apenas `true`/`false` são aceitos)

## Pré-requisitos

### Verificar Java
```bash
java -version
```
**Deve mostrar Java 17 ou superior**

### Verificar Maven
```bash
mvn -version
```
**Deve mostrar Maven 3.6 ou superior**

## Primeira execução

### 1. Limpar e compilar
```bash
mvn clean compile
```

### 2. Gerar JAR executável
```bash
mvn package
```

### 3. Verificar se o JAR foi criado
```bash
ls target/lang-*-jar-with-dependencies.jar
```

## Como executar

### Análise sintática (-syn)
```bash
# Criar arquivo de teste primeiro
echo 'main() { print 42 }' > teste.lang
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -syn teste.lang
```

### Interpretação (-i)
```bash
# Criar arquivo de teste primeiro  
echo 'main() { print 42 }' > teste.lang
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i teste.lang
```

## Como testar

### Executar todos os testes
```bash
mvn test
```

### Executar build completo + testes (recomendado)
```bash
chmod +x ci.sh
./ci.sh
```

### Teste de regressão manual
```bash
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i src/test/resources/regression/all.lang
```

**Saída esperada:**
```
3
2
1
4
5
9
7
1
1
```

## Exemplo de uso

### 1. Criar arquivo de teste
```bash
echo 'main() { x = 10; y = 5; if x > y then print x + y else print x - y }' > exemplo.lang
```

### 2. Executar
```bash
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i exemplo.lang
```

**Saída:** `15`

### Mais exemplos

#### Exemplo com iterate
```bash
echo 'main() { iterate(i:3) { print i } }' > iterate.lang
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i iterate.lang
```

#### Exemplo com array
```bash
echo 'main() { v = [1,2,3]; print v[1] }' > array.lang
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i array.lang
```

#### Exemplo com bloco-comentário
```bash
echo 'main() { {- comentário não-aninhado -} print 42 }' > comment.lang
java -jar target/lang-0.1-SNAPSHOT-jar-with-dependencies.jar -i comment.lang
```

**Nota:** Comentários de bloco não suportam aninhamento. `{- {- -} -}` resultará em erro léxico.

## Troubleshooting

### Erro: "java: command not found"
- Instale Java 17+ e configure JAVA_HOME
- No Windows: adicione Java ao PATH

### Erro: "mvn: command not found"  
- Instale Maven 3.6+ e configure MAVEN_HOME
- No Windows: adicione Maven ao PATH

### Erro: "Permission denied" no ci.sh
```bash
chmod +x ci.sh
```

### Erro: "JAR não encontrado"
- Execute `mvn clean package` primeiro
- Verifique se o arquivo existe: `ls target/`

### Erro: "Compilation failed"
- Verifique se está usando Java 17+
- Execute `mvn clean` antes de `mvn package` 