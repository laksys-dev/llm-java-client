package in.laksys.llmclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class LLMClient extends JFrame {
    private JTextField queryField;
    private JTextArea responseArea;
    private JButton sendButton;
    private JButton clearButton;
    private JButton refreshButton;
    private JComboBox<String> modelComboBox;
    private JLabel statusLabel;
    
    private static final String OLLAMA_API_BASE = "http://localhost:11434";
    private static final String OLLAMA_TAGS_URL = OLLAMA_API_BASE + "/api/tags";
    private static final String OLLAMA_GENERATE_URL = OLLAMA_API_BASE + "/api/generate";
    
    private ArrayList<String> availableModels;

    public LLMClient() {
        setTitle("Ollama Chat Interface");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        availableModels = new ArrayList<>();
        initComponents();
        loadModels();
        setVisible(true);
    }

    private void initComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for model selection
        JPanel modelPanel = new JPanel(new BorderLayout(5, 5));
        JLabel modelLabel = new JLabel("Select Model:");
        modelLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        JPanel modelControlPanel = new JPanel(new BorderLayout(5, 5));
        modelComboBox = new JComboBox<>();
        modelComboBox.setFont(new Font("Arial", Font.PLAIN, 13));
        modelComboBox.addItem("Loading models...");
        
        refreshButton = new JButton("↻");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 16));
        refreshButton.setPreferredSize(new Dimension(45, 25));
        refreshButton.setToolTipText("Refresh model list");
        refreshButton.addActionListener(e -> loadModels());
        
        modelControlPanel.add(modelComboBox, BorderLayout.CENTER);
        modelControlPanel.add(refreshButton, BorderLayout.EAST);
        
        modelPanel.add(modelLabel, BorderLayout.NORTH);
        modelPanel.add(modelControlPanel, BorderLayout.CENTER);

        // Query panel
        JPanel queryPanel = new JPanel(new BorderLayout(5, 5));
        JLabel queryLabel = new JLabel("Query:");
        queryLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        queryField = new JTextField();
        queryField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        queryPanel.add(queryLabel, BorderLayout.NORTH);
        queryPanel.add(queryField, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        clearButton = new JButton("Clear");
        sendButton = new JButton("Send");
        
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setFont(new Font("Arial", Font.PLAIN, 12));
        
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        // Combine top elements
        JPanel topPanel = new JPanel(new BorderLayout(5, 10));
        topPanel.add(modelPanel, BorderLayout.NORTH);
        topPanel.add(queryPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Response area
        JLabel responseLabel = new JLabel("Response:");
        responseLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
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

        // Status bar at bottom
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));

        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);

        // Event listeners
        sendButton.addActionListener(e -> sendQuery());
        clearButton.addActionListener(e -> clearAll());
        queryField.addActionListener(e -> sendQuery());
    }

    private void loadModels() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading models...");
        modelComboBox.removeAllItems();
        modelComboBox.addItem("Loading...");

        new Thread(() -> {
            try {
                ArrayList<String> models = fetchAvailableModels();
                
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    
                    if (models.isEmpty()) {
                        modelComboBox.addItem("No models found");
                        statusLabel.setText("No models installed. Run 'ollama pull <model>' to install.");
                        JOptionPane.showMessageDialog(this, 
                            "No models found!\n\nInstall a model using:\nollama pull gemma3:270m", 
                            "No Models", JOptionPane.WARNING_MESSAGE);
                    } else {
                        for (String model : models) {
                            modelComboBox.addItem(model);
                        }
                        statusLabel.setText("Loaded " + models.size() + " model(s)");
                    }
                    
                    refreshButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    modelComboBox.addItem("Error loading models");
                    statusLabel.setText("Error: " + ex.getMessage());
                    refreshButton.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(this, 
                        "Failed to connect to Ollama!\n\nMake sure Ollama is running.\n\nError: " + ex.getMessage(), 
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private ArrayList<String> fetchAvailableModels() throws Exception {
        URL url = new URL(OLLAMA_TAGS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Ollama API returned code: " + responseCode);
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
        JSONArray modelsArray = responseJson.getJSONArray("models");
        
        ArrayList<String> models = new ArrayList<>();
        for (int i = 0; i < modelsArray.length(); i++) {
            JSONObject modelObj = modelsArray.getJSONObject(i);
            String modelName = modelObj.getString("name");
            models.add(modelName);
        }
        
        return models;
    }

    private void sendQuery() {
        String query = queryField.getText().trim();
        String selectedModel = (String) modelComboBox.getSelectedItem();
        
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a query!", 
                "Empty Query", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedModel == null || selectedModel.equals("Loading...") || 
            selectedModel.equals("No models found") || selectedModel.equals("Error loading models")) {
            JOptionPane.showMessageDialog(this, "Please select a valid model!", 
                "No Model Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        sendButton.setEnabled(false);
        modelComboBox.setEnabled(false);
        refreshButton.setEnabled(false);
        responseArea.setText("Generating response from " + selectedModel + "...\n");
        statusLabel.setText("Generating...");

        new Thread(() -> {
            try {
                String response = callOllamaAPI(query, selectedModel);
                SwingUtilities.invokeLater(() -> {
                    responseArea.setText(response);
                    statusLabel.setText("Response complete");
                    sendButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    refreshButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    responseArea.setText("Error: " + ex.getMessage());
                    statusLabel.setText("Error occurred");
                    sendButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    refreshButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String callOllamaAPI(String prompt, String model) throws Exception {
        URL url = new URL(OLLAMA_GENERATE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        JSONObject json = new JSONObject();
        json.put("model", model);
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
        statusLabel.setText("Ready");
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