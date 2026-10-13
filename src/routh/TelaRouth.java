package routh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class TelaRouth extends JFrame {

    private final RouthMatematica motor;

    private JTextField txtEquacao;
    private JButton btnCalcular;
    private JButton btnLimpar;

    private JTable tabelaRouth;
    private DefaultTableModel modeloTabela;

    private JLabel lblPolinomioInterpretado;
    private JLabel lblDiagnostico;
    private JLabel lblCasosEspeciais;

    public TelaRouth() {
        super("Calculadora do Critério de Estabilidade de Routh-Hurwitz");
        this.motor = new RouthMatematica();
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 600));
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        JPanel painelGeral = new JPanel(new BorderLayout(12, 12));
        painelGeral.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setContentPane(painelGeral);

        painelGeral.add(criarPainelTopo(), BorderLayout.NORTH);
        painelGeral.add(criarPainelCentro(), BorderLayout.CENTER);
        painelGeral.add(criarPainelRodape(), BorderLayout.SOUTH);
    }

    private JPanel criarPainelTopo() {
        JPanel painelTopo = new JPanel(new BorderLayout(8, 8));
        painelTopo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Entrada do Polinômio Característico P(s)"),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)
        ));

        JLabel lblInstrucoes = new JLabel(
                "<html><b>Formato aceito:</b> digite livremente os coeficientes e potências de <b>s</b>. " +
                        "Exemplos: <i>2s2 - 3</i> | <i>1s4 + 2s3 - s + 5</i> | <i>s^5 + 2s^4 + 3s^3 + 6s^2 + 5s + 3</i></html>"
        );
        lblInstrucoes.setForeground(new Color(60, 60, 60));
        painelTopo.add(lblInstrucoes, BorderLayout.NORTH);

        JPanel painelEntrada = new JPanel(new BorderLayout(8, 0));

        JLabel lblRotuloEquacao = new JLabel("P(s) = ");
        lblRotuloEquacao.setFont(new Font("SansSerif", Font.BOLD, 15));
        painelEntrada.add(lblRotuloEquacao, BorderLayout.WEST);

        txtEquacao = new JTextField("1s4 + 2s3 - s + 5");
        txtEquacao.setFont(new Font("Consolas", Font.PLAIN, 15));
        txtEquacao.setToolTipText("Digite o polinômio. Ex: 2s2 - 3 ou 1s4 + 2s3 - s + 5");
        painelEntrada.add(txtEquacao, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnCalcular = new JButton("Calcular");
        btnCalcular.setFont(new Font("SansSerif", Font.BOLD, 13));

        btnLimpar = new JButton("Limpar");
        btnLimpar.setFont(new Font("SansSerif", Font.PLAIN, 13));

        painelBotoes.add(btnCalcular);
        painelBotoes.add(btnLimpar);
        painelEntrada.add(painelBotoes, BorderLayout.EAST);

        painelTopo.add(painelEntrada, BorderLayout.SOUTH);

        ActionListener calcularAction = (ActionEvent e) -> processarCalculo();
        btnCalcular.addActionListener(calcularAction);
        txtEquacao.addActionListener(calcularAction);

        btnLimpar.addActionListener((ActionEvent e) -> limparCampos());

        return painelTopo;
    }

    private JPanel criarPainelCentro() {
        JPanel painelCentro = new JPanel(new BorderLayout());
        painelCentro.setBorder(BorderFactory.createTitledBorder("Tabela de Routh"));

        modeloTabela = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaRouth = new JTable(modeloTabela);
        tabelaRouth.setFont(new Font("Arial", Font.PLAIN, 14));
        tabelaRouth.setRowHeight(28);
        tabelaRouth.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 14));
        tabelaRouth.getTableHeader().setReorderingAllowed(false);

        modeloTabela.addColumn("Linha");
        modeloTabela.addColumn("Coluna 1");
        modeloTabela.addColumn("Coluna 2");
        modeloTabela.addColumn("Coluna 3");

        configurarLarguraColunaLinha();

        JScrollPane scrollPane = new JScrollPane(tabelaRouth);
        painelCentro.add(scrollPane, BorderLayout.CENTER);

        return painelCentro;
    }

    private void configurarLarguraColunaLinha() {
        if (tabelaRouth.getColumnCount() > 0) {
            TableColumn col0 = tabelaRouth.getColumnModel().getColumn(0);
            col0.setPreferredWidth(190);
            col0.setMinWidth(180);
        }
    }

    private JPanel criarPainelRodape() {
        JPanel painelRodape = new JPanel(new GridBagLayout());
        painelRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Resultado & Diagnóstico de Estabilidade"),
                BorderFactory.createEmptyBorder(6, 12, 8, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        lblPolinomioInterpretado = new JLabel("Polinômio interpretado: [Aguardando cálculo]");
        lblPolinomioInterpretado.setFont(new Font("SansSerif", Font.PLAIN, 14));
        painelRodape.add(lblPolinomioInterpretado, gbc);

        gbc.gridy = 1;
        lblCasosEspeciais = new JLabel("Casos especiais: Nenhum");
        lblCasosEspeciais.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblCasosEspeciais.setForeground(new Color(110, 110, 110));
        painelRodape.add(lblCasosEspeciais, gbc);

        gbc.gridy = 2;
        lblDiagnostico = new JLabel("[Aguardando cálculo]");
        lblDiagnostico.setFont(new Font("Arial", Font.BOLD, 18));
        painelRodape.add(lblDiagnostico, gbc);

        return painelRodape;
    }

    private void processarCalculo() {
        String texto = txtEquacao.getText();
        if (texto == null || texto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, digite uma equação polinomial.",
                    "Entrada Vazia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            motor.analisar(texto);
            atualizarTabela();

            lblPolinomioInterpretado.setText("Polinômio interpretado: P(s) = " + motor.getPolinomioFormatado());

            List<String> avisos = motor.getAvisosCasosEspeciais();
            if (avisos.isEmpty()) {
                lblCasosEspeciais.setText("Casos especiais: Nenhum");
                lblCasosEspeciais.setForeground(new Color(90, 90, 90));
            } else {
                StringBuilder sbLogs = new StringBuilder("<html><b>Casos especiais detectados:</b>");
                for (String aviso : avisos) {
                    sbLogs.append("<br>&bull; ").append(aviso);
                }
                sbLogs.append("</html>");
                lblCasosEspeciais.setText(sbLogs.toString());
                lblCasosEspeciais.setForeground(new Color(180, 100, 0));
            }

            String diag = motor.getDiagnostico();
            lblDiagnostico.setText(diag);

            if (diag.contains("Instável")) {
                lblDiagnostico.setForeground(new Color(190, 25, 25));
            } else if (diag.contains("Marginalmente")) {
                lblDiagnostico.setForeground(new Color(200, 130, 0));
            } else {
                lblDiagnostico.setForeground(new Color(25, 130, 35));
            }

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao analisar o polinômio:\n" + ex.getMessage(),
                    "Erro de Sintaxe / Entrada",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ocorreu um erro inesperado no cálculo: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarTabela() {
        modeloTabela.setRowCount(0);
        modeloTabela.setColumnCount(0);

        List<double[]> matriz = motor.getMatrizRouth();
        List<String> rotulos = motor.getRotulosLinhas();

        if (matriz.isEmpty()) {
            return;
        }

        int numColunas = matriz.get(0).length;

        modeloTabela.addColumn("Linha");
        for (int j = 1; j <= numColunas; j++) {
            modeloTabela.addColumn("Col " + j);
        }

        for (int i = 0; i < matriz.size(); i++) {
            Object[] rowData = new Object[numColunas + 1];
            rowData[0] = rotulos.get(i);
            double[] linhaValores = matriz.get(i);

            for (int j = 0; j < numColunas; j++) {
                rowData[j + 1] = RouthMatematica.formatarNumero(linhaValores[j]);
            }
            modeloTabela.addRow(rowData);
        }

        configurarLarguraColunaLinha();

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c = 0; c < tabelaRouth.getColumnCount(); c++) {
            tabelaRouth.getColumnModel().getColumn(c).setCellRenderer(centerRenderer);
        }
    }

    private void limparCampos() {
        txtEquacao.setText("");
        modeloTabela.setRowCount(0);
        modeloTabela.setColumnCount(0);
        modeloTabela.addColumn("Linha");
        modeloTabela.addColumn("Coluna 1");
        modeloTabela.addColumn("Coluna 2");
        modeloTabela.addColumn("Coluna 3");
        configurarLarguraColunaLinha();
        lblPolinomioInterpretado.setText("Polinômio interpretado: [Aguardando cálculo]");
        lblCasosEspeciais.setText("Casos especiais: Nenhum");
        lblCasosEspeciais.setForeground(new Color(110, 110, 110));
        lblDiagnostico.setText("[Aguardando cálculo]");
        lblDiagnostico.setForeground(Color.BLACK);
        txtEquacao.requestFocus();
    }
}