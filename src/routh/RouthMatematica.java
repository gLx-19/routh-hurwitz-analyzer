package routh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouthMatematica {

    public static final double EPSILON = 1e-6;
    private static final double TOLERANCIA_ZERO = 1e-12;

    private int grau;
    private double[] coeficientes;
    private List<double[]> matrizRouth;
    private List<String> rotulosLinhas;
    private boolean houveLinhaDeZeros;
    private int trocasDeSinal;
    private String diagnostico;
    private List<String> linhasCaso1;
    private List<String> linhasCaso2;

    public RouthMatematica() {
        this.matrizRouth = new ArrayList<>();
        this.rotulosLinhas = new ArrayList<>();
        this.linhasCaso1 = new ArrayList<>();
        this.linhasCaso2 = new ArrayList<>();
    }

    public double[] parseEquacao(String equacao) {
        if (equacao == null || equacao.trim().isEmpty()) {
            throw new IllegalArgumentException("A equação não pode ser vazia.");
        }

        String texto = equacao.trim();

        if (texto.contains("=")) {
            texto = texto.split("=")[0].trim();
        }

        texto = texto.replace("*", "");

        Pattern pattern = Pattern.compile("([+-]?)\\s*([0-9]*\\.?[0-9]+)?(?:([sS])(?:\\^?([0-9]+))?)?");
        Matcher matcher = pattern.matcher(texto);

        Map<Integer, Double> mapaTermos = new HashMap<>();
        int maxGrau = -1;
        int matchedChars = 0;

        while (matcher.find()) {
            String fullMatch = matcher.group(0).trim();
            if (fullMatch.isEmpty()) {
                continue;
            }

            matchedChars += fullMatch.replaceAll("\\s+", "").length();

            String sinalStr = matcher.group(1);
            String coefStr = matcher.group(2);
            String variavelStr = matcher.group(3);
            String expStr = matcher.group(4);

            double sinal = "-".equals(sinalStr) ? -1.0 : 1.0;

            int expoente;
            if (variavelStr != null && !variavelStr.isEmpty()) {
                if (expStr != null && !expStr.isEmpty()) {
                    expoente = Integer.parseInt(expStr);
                } else {
                    expoente = 1;
                }
            } else {
                expoente = 0;
            }

            double coef;
            if (coefStr != null && !coefStr.isEmpty()) {
                coef = Double.parseDouble(coefStr);
            } else {
                if (variavelStr != null && !variavelStr.isEmpty()) {
                    coef = 1.0;
                } else {
                    continue;
                }
            }

            coef *= sinal;
            mapaTermos.put(expoente, mapaTermos.getOrDefault(expoente, 0.0) + coef);

            if (expoente > maxGrau) {
                maxGrau = expoente;
            }
        }

        String textoSemEspacos = texto.replaceAll("\\s+", "");
        if (matchedChars < textoSemEspacos.length() - 2 && mapaTermos.isEmpty()) {
            throw new IllegalArgumentException("Formato de equação inválido ou não reconhecido: " + equacao);
        }

        if (maxGrau < 1) {
            throw new IllegalArgumentException("O polinômio deve ter grau n >= 1 para análise de estabilidade.");
        }

        double coefMax = mapaTermos.getOrDefault(maxGrau, 0.0);
        if (Math.abs(coefMax) < TOLERANCIA_ZERO) {
            throw new IllegalArgumentException("O coeficiente do termo de maior grau (s^" + maxGrau + ") não pode ser zero.");
        }

        double[] resultado = new double[maxGrau + 1];
        for (int i = 0; i <= maxGrau; i++) {
            int expAtual = maxGrau - i;
            resultado[i] = mapaTermos.getOrDefault(expAtual, 0.0);
        }

        return resultado;
    }

    public void analisar(String equacao) {
        this.coeficientes = parseEquacao(equacao);
        this.grau = coeficientes.length - 1;
        this.matrizRouth.clear();
        this.rotulosLinhas.clear();
        this.linhasCaso1.clear();
        this.linhasCaso2.clear();
        this.houveLinhaDeZeros = false;
        this.trocasDeSinal = 0;

        construirTabelaRouth();
        avaliarEstabilidade();
    }

    private void construirTabelaRouth() {
        int numColunas = (grau + 2) / 2;

        double[] linha0 = new double[numColunas];
        int col = 0;
        for (int i = 0; i <= grau; i += 2) {
            linha0[col++] = coeficientes[i];
        }
        matrizRouth.add(linha0);

        if (grau % 2 == 0) {
            rotulosLinhas.add("Coeficientes pares: s^" + grau);
        } else {
            rotulosLinhas.add("Coeficientes ímpares: s^" + grau);
        }

        double[] linha1 = new double[numColunas];
        col = 0;
        for (int i = 1; i <= grau; i += 2) {
            linha1[col++] = coeficientes[i];
        }

        int grauLinha1 = grau - 1;
        if (isLinhaTotalmenteZero(linha1)) {
            this.houveLinhaDeZeros = true;
            linhasCaso2.add("s^" + grauLinha1);
            linha1 = derivarPolinomioAuxiliar(linha0, grau, numColunas);
        } else if (Math.abs(linha1[0]) < TOLERANCIA_ZERO && !isLinhaTotalmenteZero(linha1)) {
            linha1[0] = EPSILON;
            linhasCaso1.add("s^" + grauLinha1);
        }

        matrizRouth.add(linha1);

        if (grau % 2 == 0) {
            rotulosLinhas.add("Coeficientes ímpares: s^" + grauLinha1);
        } else {
            rotulosLinhas.add("Coeficientes pares: s^" + grauLinha1);
        }

        for (int k = 2; k <= grau; k++) {
            int grauAtual = grau - k;
            double[] linhaAnterior = matrizRouth.get(k - 1);
            double[] linhaPenultima = matrizRouth.get(k - 2);

            double[] novaLinha = new double[numColunas];
            double pivo = linhaAnterior[0];

            for (int j = 0; j < numColunas - 1; j++) {
                double a = linhaPenultima[0];
                double b = linhaPenultima[j + 1];
                double c = linhaAnterior[0];
                double d = linhaAnterior[j + 1];

                if (Math.abs(pivo) > TOLERANCIA_ZERO) {
                    novaLinha[j] = ((c * b) - (a * d)) / c;
                } else {
                    novaLinha[j] = 0.0;
                }
            }

            if (isLinhaTotalmenteZero(novaLinha)) {
                this.houveLinhaDeZeros = true;
                linhasCaso2.add("s^" + grauAtual);
                novaLinha = derivarPolinomioAuxiliar(linhaAnterior, grauAtual + 1, numColunas);
            } else if (Math.abs(novaLinha[0]) < TOLERANCIA_ZERO && !isLinhaTotalmenteZero(novaLinha)) {
                novaLinha[0] = EPSILON;
                linhasCaso1.add("s^" + grauAtual);
            }

            matrizRouth.add(novaLinha);
            rotulosLinhas.add("s^" + grauAtual);
        }
    }

    private boolean isLinhaTotalmenteZero(double[] linha) {
        for (double val : linha) {
            if (Math.abs(val) > TOLERANCIA_ZERO) {
                return false;
            }
        }
        return true;
    }

    private double[] derivarPolinomioAuxiliar(double[] linhaAuxiliar, int grauAuxiliar, int numColunas) {
        double[] linhaDerivada = new double[numColunas];
        int exp = grauAuxiliar;

        for (int j = 0; j < numColunas; j++) {
            if (exp > 0) {
                linhaDerivada[j] = linhaAuxiliar[j] * exp;
            } else {
                linhaDerivada[j] = 0.0;
            }
            exp -= 2;
        }

        return linhaDerivada;
    }

    private void avaliarEstabilidade() {
        trocasDeSinal = 0;
        double sinalAnterior = 0.0;

        for (int i = 0; i < matrizRouth.size(); i++) {
            double val = matrizRouth.get(i)[0];

            if (Math.abs(val) < TOLERANCIA_ZERO) {
                continue;
            }

            double sinalAtual = (val > 0) ? 1.0 : -1.0;
            if (sinalAnterior != 0.0 && sinalAtual != sinalAnterior) {
                trocasDeSinal++;
            }
            sinalAnterior = sinalAtual;
        }

        if (trocasDeSinal > 0) {
            diagnostico = "Sistema Instável. Possui " + trocasDeSinal + " polos no semiplano direito (SPD).";
        } else if (houveLinhaDeZeros) {
            diagnostico = "Sistema Marginalmente Estável (raízes no eixo imaginário).";
        } else {
            diagnostico = "Sistema Estável.";
        }
    }

    public List<String> getAvisosCasosEspeciais() {
        List<String> avisos = new ArrayList<>();
        if (!linhasCaso1.isEmpty()) {
            avisos.add("0 na primeira coluna na(s) linha(s): " + String.join(", ", linhasCaso1));
        }
        if (!linhasCaso2.isEmpty()) {
            avisos.add("Linha de zeros na(s) linha(s): " + String.join(", ", linhasCaso2));
        }
        return avisos;
    }

    public List<String> getLinhasCaso1() {
        return linhasCaso1;
    }

    public List<String> getLinhasCaso2() {
        return linhasCaso2;
    }

    public String getPolinomioFormatado() {
        if (coeficientes == null || coeficientes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = coeficientes.length - 1;

        for (int i = 0; i <= n; i++) {
            double c = coeficientes[i];
            int exp = n - i;

            if (Math.abs(c) < TOLERANCIA_ZERO && n > 0 && sb.length() > 0) {
                continue;
            }

            String cStr = formatarNumero(Math.abs(c));

            if (sb.length() == 0) {
                if (c < 0) {
                    sb.append("-");
                }
            } else {
                sb.append(c >= 0 ? " + " : " - ");
            }

            if (exp == 0) {
                sb.append(cStr);
            } else if (exp == 1) {
                if (Math.abs(c - 1.0) < TOLERANCIA_ZERO) {
                    sb.append("s");
                } else {
                    sb.append(cStr).append("s");
                }
            } else {
                if (Math.abs(c - 1.0) < TOLERANCIA_ZERO) {
                    sb.append("s^").append(exp);
                } else {
                    sb.append(cStr).append("s^").append(exp);
                }
            }
        }
        return sb.toString();
    }

    public static String formatarNumero(double valor) {
        if (Math.abs(valor - Math.round(valor)) < 1e-5) {
            return String.format("%.0f", (double) Math.round(valor));
        } else if (Math.abs(valor) == EPSILON) {
            return "\u03B5 (1e-6)";
        } else {
            return String.format("%.4f", valor).replace(",", ".");
        }
    }

    public int getGrau() {
        return grau;
    }

    public double[] getCoeficientes() {
        return coeficientes != null ? Arrays.copyOf(coeficientes, coeficientes.length) : null;
    }

    public List<double[]> getMatrizRouth() {
        return matrizRouth;
    }

    public List<String> getRotulosLinhas() {
        return rotulosLinhas;
    }

    public boolean isHouveLinhaDeZeros() {
        return houveLinhaDeZeros;
    }

    public int getTrocasDeSinal() {
        return trocasDeSinal;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public List<String> getLogProcessamento() {
        return getAvisosCasosEspeciais();
    }
}
