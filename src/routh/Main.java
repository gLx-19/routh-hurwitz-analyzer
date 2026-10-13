package routh;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível carregar o Look and Feel nativo do sistema: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            TelaRouth tela = new TelaRouth();
            tela.setVisible(true);
        });
    }
}
