import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List; // Importe a interface java.util.List
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public class InterfaceUsuario extends JFrame {
    private JTextField campoConcurso;
    private ArrayList<JToggleButton> botoesNumeros;
    private JButton botaoSalvar;
    private JButton botaoGerarPalpites;

    // Constantes para credenciais de banco de dados
    private static final String URL = "jdbc:mysql://localhost:3306/concursos_db";
    private static final String USUARIO = "root";
    private static final String SENHA = "9221";

    public InterfaceUsuario() {
        super("Interface do Usuário");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);

        // Painel principal
        JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel);

        // Painel para os botões e campo de texto
        JPanel inputPanel = new JPanel(new FlowLayout());
        panel.add(inputPanel, BorderLayout.NORTH);

        // Campo de inserção do número do concurso
        campoConcurso = new JTextField(10);
        inputPanel.add(new JLabel("Número do Concurso:"));
        inputPanel.add(campoConcurso);

        // Botão para gerar palpites
        botaoGerarPalpites = new JButton("Gerar Palpites");
        botaoGerarPalpites.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gerarPalpites();
            }
        });
        inputPanel.add(botaoGerarPalpites);

        // Botão para salvar dados
        botaoSalvar = new JButton("Salvar");
        botaoSalvar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                salvarDados();
            }
        });
        inputPanel.add(botaoSalvar);

        // Inicializar arraylist de botões
        botoesNumeros = new ArrayList<>();

        // Adiciona 25 botões de números
        JPanel numerosPanel = new JPanel(new GridLayout(5, 5));
        panel.add(numerosPanel, BorderLayout.CENTER);
        for (int i = 1; i <= 25; i++) {
            JToggleButton btn = new JToggleButton(String.valueOf(i));
            numerosPanel.add(btn);
            botoesNumeros.add(btn);
        }
    }

    // Método para contar quantos números foram selecionados
    private int contarSelecionados() {
        int selecionados = 0;
        for (JToggleButton btn : botoesNumeros) {
            if (btn.isSelected()) {
                selecionados++;
            }
        }
        return selecionados;
    }

    private void salvarDados() {
        String numeroConcurso = campoConcurso.getText();
        if (numeroConcurso.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Insira o número do concurso.");
            return;
        }

        List<Integer> numerosSelecionados = new ArrayList<>();
        for (JToggleButton btn : botoesNumeros) {
            if (btn.isSelected()) {
                numerosSelecionados.add(Integer.parseInt(btn.getText()));
            }
        }

        if (numerosSelecionados.size() != 15) {
            JOptionPane.showMessageDialog(this, "Selecione exatamente 15 números.");
            return;
        }

        try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA)) {
            // Inserindo o número do concurso e os números selecionados na tabela concursos
            String sqlConcurso = "INSERT INTO concursos (numero_concurso, numeros_selecionados) VALUES (?, ?)";
            try (PreparedStatement stmtConcurso = conexao.prepareStatement(sqlConcurso)) {
                stmtConcurso.setString(1, numeroConcurso);
                String numerosConcatenados = String.join("-", numerosSelecionados.stream().map(String::valueOf).collect(Collectors.toList()));
                stmtConcurso.setString(2, numerosConcatenados);
                stmtConcurso.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Dados salvos com sucesso!");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar os dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

private void gerarPalpites() {
    try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA)) {
        // Consulta para recuperar todos os números selecionados nos concursos anteriores
        String sqlConsulta = "SELECT numeros_selecionados FROM concursos";
        Statement stmtConsulta = conexao.createStatement();
        ResultSet rs = stmtConsulta.executeQuery(sqlConsulta);

        ArrayList<Integer> todosNumeros = new ArrayList<>();

        // Iterar sobre o resultado da consulta
        while (rs.next()) {
            String numerosConcurso = rs.getString("numeros_selecionados");
            // Verificar se a string está vazia
            if (!numerosConcurso.isEmpty()) {
                // Dividir a string apenas se não estiver vazia
                String[] numeros = numerosConcurso.split("-");
                for (String numero : numeros) {
                    // Verificar se a string é um número antes de tentar convertê-la
                    try {
                        todosNumeros.add(Integer.parseInt(numero));
                    } catch (NumberFormatException ex) {
                        System.err.println("Erro ao converter número: " + ex.getMessage());
                    }
                }
            }
        }

        // Gerar palpites
        ArrayList<ArrayList<Integer>> palpites = new ArrayList<>();
        Random random = new Random();

        // Escolher 11 números fixos com base nos números mais frequentes
        List<Integer> numerosMaisFrequentes = obterNumerosMaisFrequentes(todosNumeros, 11);
        for (int i = 0; i < 10; i++) {
            ArrayList<Integer> palpite = new ArrayList<>(numerosMaisFrequentes);

            // Escolher 4 números alternados
            for (int j = 0; j < 4; j++) {
                int numeroAlternado;
                do {
                    numeroAlternado = random.nextInt(25) + 1;
                } while (numerosMaisFrequentes.contains(numeroAlternado) || palpite.contains(numeroAlternado));
                palpite.add(numeroAlternado);
            }

            // Embaralhar os números no palpite
            Collections.shuffle(palpite);
            palpites.add(palpite);
        }

        // Exibir os palpites
        exibirPalpites(palpites);

        stmtConsulta.close();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro ao gerar palpites: " + e.getMessage());
        e.printStackTrace();
    }
}

    // Método para obter os números mais frequentes
    private List<Integer> obterNumerosMaisFrequentes(List<Integer> todosNumeros, int quantidade) {
        Map<Integer, Long> frequenciaNumeros = todosNumeros.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Integer> numerosOrdenados = frequenciaNumeros.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return numerosOrdenados.subList(0, quantidade);
    }

    // Método para exibir os palpites ordenados em ordem crescente
private void exibirPalpites(List<ArrayList<Integer>> palpites) {
    StringBuilder mensagem = new StringBuilder();
    mensagem.append("<html><body><h2>Palpites gerados:</h2><ul>");

    // Ordenar os palpites em ordem crescente
    palpites.sort((p1, p2) -> {
        Collections.sort(p1);
        Collections.sort(p2);
        return p1.toString().compareTo(p2.toString());
    });

    // Exibir os palpites ordenados
    for (ArrayList<Integer> p : palpites) {
        mensagem.append("<li>").append(p).append("</li>");
    }
    mensagem.append("</ul></body></html>");
    JLabel label = new JLabel(mensagem.toString());
    JOptionPane.showMessageDialog(this, label, "Palpites", JOptionPane.PLAIN_MESSAGE);
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new InterfaceUsuario().setVisible(true);
            }
        });
    }
}
