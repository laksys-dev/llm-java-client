package in.laksys.llmclient;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.json.*;

public class LLMClient extends JFrame {
    private JTextField queryField;
    private JTextArea responseArea;
    private JButton sendButton;
    private JButton clearButton;
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma3:270m";

    public LLMClient() {
        setTitle("Gemma 3 Chat Interface");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for query input
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JLabel queryLabel = new JLabel("Query:");
        queryField = new JTextField();
        queryField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        topPanel.add(queryLabel, BorderLayout.NORTH);
        topPanel.add(queryField, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("Send");
        clearButton = new JButton("Clear");
        
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setFont(new Font("Arial", Font.PLAIN, 12));
        
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Response area
        JLabel responseLabel = new JLabel("Response:");
        responseArea = new JTextArea();
        responseArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(responseArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(responseLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Event listeners
        sendButton.addActionListener(e -> sendQuery());
        clearButton.addActionListener(e -> clearAll());
        
        queryField.addActionListener(e -> sendQuery());
    }

    private void sendQuery() {
        String query = queryField.getText().trim();
        
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a query!", 
                "Empty Query", JOptionPane.WARNING_MESSAGE);
            return;
        }

        sendButton.setEnabled(false);
        responseArea.setText("Generating response...\n");

        new Thread(() -> {
            try {
                String response = callOllamaAPI(query);
                SwingUtilities.invokeLater(() -> {
                    responseArea.setText(response);
                    sendButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    responseArea.setText("Error: " + ex.getMessage());
                    sendButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String callOllamaAPI(String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("model", MODEL);
        json.put("prompt", prompt);
        json.put("stream", false);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.toString().getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned code: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JSONObject responseJson = new JSONObject(response.toString());
        return responseJson.getString("response");
    }

    private void clearAll() {
        queryField.setText("");
        responseArea.setText("");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new LLMClient());
    }
}