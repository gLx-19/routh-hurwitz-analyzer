# Calculadora do Critério de Estabilidade de Routh-Hurwitz

Aplicativo gráfico desenvolvido em Java puro (Swing) para análise rigorosa da estabilidade de sistemas lineares e invariantes no tempo (LTI) a partir de seus polinômios característicos, utilizando o método da Matriz de Routh-Hurwitz com tratamento automatizado de casos singulares.

O projeto foi estruturado sem dependência de gerenciadores como Maven ou Gradle, permitindo compilação nativa com o JDK padrão e geração de arquivo executável `.jar`.

---

## 1. Visão Geral da Arquitetura e Estrutura dos Arquivos

O código-fonte está no pacote `routh` e foi concebido sem comentários internos para manter a base limpa e enxuta:

- **`Main.java`**: Ponto de entrada da aplicação. Aplica a aparência nativa do sistema operacional (Look and Feel do Windows/Linux/macOS) e despacha a abertura da janela para a thread de interface gráfica (EDT - Event Dispatch Thread).
- **`TelaRouth.java`**: Interface gráfica (JFrame). Constrói as camadas visuais (entrada de dados, tabela interativa e painel de diagnóstico), valida entradas e renderiza os dados formatados com estilo e alinhamento centrais.
- **`RouthMatematica.java`**: Motor matemático isolado. Contém o interpretador da equação polinomial por expressão regular, o algoritmo de construção e derivação da Matriz de Routh, o tratamento dos casos singulares e a avaliação de estabilidade.

---

## 2. Explicação Humana e Didática do Código

### A) Interpretação da Equação Polinomial (Parser)
Na prática acadêmica, cada aluno ou usuário digita polinômios de formas diferentes: com ou sem espaços, com `s2` ou `s^2`, omitindo potências intermediárias, ou até mesmo incluindo termos como `= 0`.

O método `parseEquacao(String equacao)` lida com essas variações em três etapas:
1. **Limpeza preliminar**: Se houver `= 0` ou qualquer igualdade, o lado direito é descartado. Multiplicações explícitas como `*` também são removidas.
2. **Leitura por Expressão Regular (Regex)**:
   A expressão regular `([+-]?)\s*([0-9]*\.?[0-9]+)?(?:([sS])(?:\^?([0-9]+))?)?` decompõe cada pedaço do texto em quatro partes essenciais:
   - Sinal do termo (`+` ou `-`).
   - Coeficiente numérico (por exemplo, `2`, `0.5`, ou implícito `1.0` quando vem apenas `s` ou `-s`).
   - Presença da letra `s` (maiúscula ou minúscula).
   - Expoente da potência (seja `s^3` ou `s3`). Caso venha apenas `s`, deduz-se expoente 1. Se não houver `s`, trata-se do termo independente (expoente 0).
3. **Mapeamento e Preenchimento dos Termos Faltantes**:
   Os coeficientes são agrupados em um mapa chaveado pelo expoente. O algoritmo descobre o grau máximo `n` e gera um vetor contínuo de `s^n` até `s^0`. Qualquer potência que não foi digitada (por exemplo, `s^1` em `2s2 - 3`) é automaticamente completada com o valor `0.0`. O resultado para `2s2 - 3` é o vetor `[2.0, 0.0, -3.0]`.

---

### B) Nomenclatura Didática das Linhas Iniciais
A teoria de controle organiza as duas primeiras linhas da tabela separando os termos em índices alternados. Para tornar a tabela visualmente clara quanto à paridade das potências:
- Se o grau máximo do polinômio $n$ for **par** (ex: $s^4, s^2$):
  - A 1ª linha recebe o rótulo **`Coeficientes pares: s^n`**.
  - A 2ª linha recebe o rótulo **`Coeficientes ímpares: s^(n-1)`**.
- Se o grau máximo $n$ for **ímpar** (ex: $s^5, s^3$):
  - A 1ª linha recebe o rótulo **`Coeficientes ímpares: s^n`**.
  - A 2ª linha recebe o rótulo **`Coeficientes pares: s^(n-1)`**.
- As linhas seguintes seguem o formato tradicional de potências decrescentes: `s^(n-2)`, `s^(n-3)`, ..., `s^0`.

---

### C) Tratamento de Casos Especiais

#### Caso Especial 1: Primeiro Elemento Zero
Quando o primeiro elemento de uma linha recém-calculada resulta em `0.0`, mas existem outros números diferentes de zero à sua direita na mesma linha, o algoritmo não pode simplesmente usar esse zero como divisor na linha seguinte (pois causaria divisão por zero).
- **Como o código resolve**: O `0.0` é substituído por um valor infinitesimal positivo $\varepsilon = 10^{-6}$ (`EPSILON`). Isso viabiliza a continuidade do cálculo sem perda da tendência de sinal.
- **Aviso emitido**: Agrupado em uma única linha indicando onde ocorreu, no formato `"0 na primeira coluna na(s) linha(s): s^4, s^2"`.

#### Caso Especial 2: Linha Inteira de Zeros
Se todos os elementos de uma nova linha calculada (ou da linha de coeficientes ímpares, como no caso de $s^2 - 1$ ou $s^2 + 1$) resultarem em zero, ocorre o caso mais rico teoricamente. Uma linha inteira de zeros sinaliza que o polinômio possui **raízes simétricas em relação à origem do plano complexo** (podem ser raízes puramente imaginárias $\pm j\omega$, raízes reais simétricas $\pm\sigma$, ou pares conjugados em ambos os semiplanos).

- **Como o código resolve**:
  1. A linha de zeros é descartada.
  2. O código consulta a linha imediatamente anterior, cujos coeficientes formam o chamado **Polinômio Auxiliar** $A(s)$.
  3. No polinômio auxiliar, as potências diminuem de 2 em 2 (por exemplo, na linha de $s^2$, os termos correspondem a $s^2$ e $s^0$).
  4. O código efetua a derivada analítica $\frac{dA(s)}{ds}$, multiplicando cada coeficiente pelo seu expoente original (preservando sinais positivos ou negativos).
  5. Os coeficientes obtidos dessa derivada substituem a linha de zeros.
  6. O sinalizador `houveLinhaDeZeros = true` é ativado.
  7. O cálculo de Routh continua normalmente até $s^0$.
- **Aviso emitido**: Agrupado em uma única linha indicando onde ocorreu, no formato `"Linha de zeros na(s) linha(s): s^6, s^1"`.

---

### D) Hierarquia Absoluta do Diagnóstico de Estabilidade
Um ponto fundamental de Teoria de Controle é que **a presença de qualquer troca de sinal na 1ª coluna sobrepõe qualquer outra condição**:

1. **Instabilidade Absoluta (Hierarquia 1)**:
   - Se houver 1 ou mais trocas de sinal entre os termos da primeira coluna da tabela, o sistema é **obrigatoriamente Instável**, independente de ter havido linha de zeros ou não.
   - Polinômios como $s^2 - 1 = 0$ geram linha de zeros em $s^1$, cuja derivada fornece $2s$. A primeira coluna resultante fica $[1.0, 2.0, -1.0]$, contendo 1 troca de sinal ($+2 \to -1$). O sistema tem um polo em $+1$ no semiplano direito e outro em $-1$. Portanto, o diagnóstico correto é:
     $$\text{"Sistema Instável. Possui 1 polos no semiplano direito (SPD)."}$$
2. **Estabilidade Marginal (Hierarquia 2)**:
   - Se **não houver nenhuma troca de sinal** na 1ª coluna **E** `houveLinhaDeZeros == true`, as raízes simétricas estão puramente sobre o eixo imaginário $\pm j\omega$ (ex: $s^2 + 1 = 0$ ou $s^3 + 2s^2 + 4s + 8 = 0$). O veredito é:
     $$\text{"Sistema Marginalmente Estável (raízes no eixo imaginário)."}$$
3. **Estabilidade Assintótica (Hierarquia 3)**:
   - Se **não houver trocas de sinal** na 1ª coluna **E** `houveLinhaDeZeros == false`, todas as raízes residem estritamente no semiplano esquerdo (SPE). O veredito é:
     $$\text{"Sistema Estável."}$$

---

### E) Detalhes Visuais da Interface Gráfica (`TelaRouth.java`)
- **Tabela**: Tanto o cabeçalho quanto todas as células de dados utilizam a fonte **Arial tamanho 14**. Todas as células numéricas são estritamente centralizadas via renderizador customizado (`SwingConstants.CENTER`). A primeira coluna ("Linha") tem largura mínima de 180px e preferencial de 190px, garantindo que os rótulos de coeficientes pares e ímpares nunca sejam truncados com reticências.
- **Diagnóstico no Rodapé**: A frase com o veredito final é exibida em destaque com fonte **Arial tamanho 18 em Negrito**, sem prefixos desnecessários como "Diagnóstico:", e colorida dinamicamente conforme a classificação (Verde para estável, Âmbar para marginalmente estável e Vermelho para instável).

---

## 3. Instruções de Compilação e Execução

### Compilação Manual
Abra o terminal na pasta raiz do projeto e execute:

```bash
# Compilar os arquivos fonte gerando os bytecodes na pasta bin
javac -d bin src/routh/*.java

# Executar a aplicação
java -cp bin routh.Main
```

### Empacotar e Rodar como `.jar`
```bash
# Criar o arquivo JAR executável
jar cfe RouthHurwitzAnalyzer.jar routh.Main -C bin .

# Executar via linha de comando
java -jar RouthHurwitzAnalyzer.jar
```
*(Também é possível dar duplo clique no arquivo `RouthHurwitzAnalyzer.jar` pelo explorador de arquivos).*
