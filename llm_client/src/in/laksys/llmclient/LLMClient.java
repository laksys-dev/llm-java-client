package in.laksys.llmclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.*;

public class LLMClient extends JFrame {
    private JTextField queryField;
    private JTextArea conversationArea;
    private JButton sendButton;
    private JButton clearButton;
    private JButton refreshButton;
    private JButton newChatButton;
    private JComboBox<String> modelComboBox;
    private JLabel statusLabel;
    
    private static final String OLLAMA_API_BASE = "http://localhost:11434";
    private static final String OLLAMA_TAGS_URL = OLLAMA_API_BASE + "/api/tags";
    private static final String OLLAMA_CHAT_URL = OLLAMA_API_BASE + "/api/chat";
    
    private ArrayList<String> availableModels;
    private ArrayList<JSONObject> conversationHistory;

    public LLMClient() {
        setTitle("Ollama Chat Interface");
        setSize(750, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        availableModels = new ArrayList<>();
        conversationHistory = new ArrayList<>();
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
        
        JPanel modelButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        refreshButton = new JButton("↻");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 16));
        refreshButton.setPreferredSize(new Dimension(45, 25));
        refreshButton.setToolTipText("Refresh model list");
        refreshButton.addActionListener(e -> loadModels());
        
        newChatButton = new JButton("New Chat");
        newChatButton.setFont(new Font("Arial", Font.PLAIN, 11));
        newChatButton.setToolTipText("Start a new conversation");
        newChatButton.addActionListener(e -> startNewChat());
        
        modelButtonPanel.add(refreshButton);
        modelButtonPanel.add(newChatButton);
        
        modelControlPanel.add(modelComboBox, BorderLayout.CENTER);
        modelControlPanel.add(modelButtonPanel, BorderLayout.EAST);
        
        modelPanel.add(modelLabel, BorderLayout.NORTH);
        modelPanel.add(modelControlPanel, BorderLayout.CENTER);

        // Conversation area
        JLabel conversationLabel = new JLabel("Conversation:");
        conversationLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        conversationArea = new JTextArea();
        conversationArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        conversationArea.setLineWrap(true);
        conversationArea.setWrapStyleWord(true);
        conversationArea.setEditable(false);
        conversationArea.setText("Start a conversation by typing a message below...\n");
        
        JScrollPane scrollPane = new JScrollPane(conversationArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel conversationPanel = new JPanel(new BorderLayout(5, 5));
        conversationPanel.add(conversationLabel, BorderLayout.NORTH);
        conversationPanel.add(scrollPane, BorderLayout.CENTER);

        // Query panel
        JPanel queryPanel = new JPanel(new BorderLayout(5, 5));
        JLabel queryLabel = new JLabel("Your Message:");
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

        // Combine bottom elements
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(queryPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Top panel combining model selector
        JPanel topPanel = new JPanel(new BorderLayout(5, 10));
        topPanel.add(modelPanel, BorderLayout.NORTH);

        // Status bar at bottom
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));

        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(conversationPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        add(statusLabel, BorderLayout.SOUTH);

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

    private void startNewChat() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Start a new conversation? Current chat will be cleared.",
            "New Chat",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            conversationHistory.clear();
            conversationArea.setText("New conversation started...\n");
            queryField.setText("");
            statusLabel.setText("Ready - New conversation");
        }
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

        // Add user message to history
        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", query);
            conversationHistory.add(userMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Display user message
        conversationArea.append("\nYou: " + query + "\n");
        queryField.setText("");

        sendButton.setEnabled(false);
        modelComboBox.setEnabled(false);
        refreshButton.setEnabled(false);
        newChatButton.setEnabled(false);
        conversationArea.append("\n" + selectedModel + ": Thinking...\n");
        statusLabel.setText("Generating response...");

        new Thread(() -> {
            try {
                String response = callOllamaChat(selectedModel);
                
                // Add assistant response to history
                try {
                    JSONObject assistantMessage = new JSONObject();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", response);
                    conversationHistory.add(assistantMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                SwingUtilities.invokeLater(() -> {
                    // Remove "Thinking..." line
                    String currentText = conversationArea.getText();
                    int lastThinking = currentText.lastIndexOf("Thinking...\n");
                    if (lastThinking != -1) {
                        conversationArea.setText(currentText.substring(0, lastThinking));
                    }
                    
                    conversationArea.append(response + "\n");
                    conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
                    statusLabel.setText("Response complete (" + conversationHistory.size() + " messages in history)");
                    sendButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    refreshButton.setEnabled(true);
                    newChatButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    conversationArea.append("Error: " + ex.getMessage() + "\n");
                    statusLabel.setText("Error occurred");
                    sendButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    refreshButton.setEnabled(true);
                    newChatButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String callOllamaChat(String model) throws Exception {
        URL url = new URL(OLLAMA_CHAT_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        JSONObject json = new JSONObject();
        json.put("model", model);
        json.put("stream", false);
        
        // Add conversation history
        JSONArray messages = new JSONArray();
        for (JSONObject msg : conversationHistory) {
            messages.put(msg);
        }
        json.put("messages", messages);

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
        JSONObject message = responseJson.getJSONObject("message");
        return message.getString("content");
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear the entire conversation?",
            "Clear Conversation",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            conversationHistory.clear();
            queryField.setText("");
            conversationArea.setText("Conversation cleared. Start a new one...\n");
            statusLabel.setText("Ready");
        }
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