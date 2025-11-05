/*
 * IDE-Triangle v1.1 - Backend Selection Dialog
 * BackendSelectionDialog.java
 */

package GUI;

import Triangle.IDEMultiBackendCompiler.BackendType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for selecting compilation backend (TAM, LLVM IR, or both)
 */
public class BackendSelectionDialog extends JDialog {
    
    private BackendType selectedBackend;
    private boolean cancelled;
    
    // UI Components
    private JRadioButton tamRadio;
    private JRadioButton llvmRadio;
    private JRadioButton bothRadio;
    private JButton okButton;
    private JButton cancelButton;
    
    public BackendSelectionDialog(Frame parent) {
        super(parent, "Select Compilation Backend", true);
        this.selectedBackend = BackendType.BOTH; // Default
        this.cancelled = false;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        // Radio buttons for backend selection
        tamRadio = new JRadioButton("TAM (Triangle Abstract Machine)", false);
        llvmRadio = new JRadioButton("LLVM IR (LLVM Intermediate Representation)", false);
        bothRadio = new JRadioButton("Both TAM and LLVM IR", true);
        
        // Group radio buttons
        ButtonGroup backendGroup = new ButtonGroup();
        backendGroup.add(tamRadio);
        backendGroup.add(llvmRadio);
        backendGroup.add(bothRadio);
        
        // Action buttons
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        
        // Add tooltips
        tamRadio.setToolTipText("Generate TAM assembly code (.tam file)");
        llvmRadio.setToolTipText("Generate LLVM Intermediate Representation (.ll file)");
        bothRadio.setToolTipText("Generate both TAM and LLVM IR files");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        JLabel titleLabel = new JLabel("Choose compilation backend:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14.0f));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 15, 10);
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(titleLabel, gbc);
        
        // Radio buttons
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 20, 5, 10);
        
        gbc.gridy = 1;
        mainPanel.add(tamRadio, gbc);
        
        gbc.gridy = 2;
        mainPanel.add(llvmRadio, gbc);
        
        gbc.gridy = 3;
        mainPanel.add(bothRadio, gbc);
        
        // Description panel
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setBorder(BorderFactory.createTitledBorder("Backend Information"));
        
        JTextArea descArea = new JTextArea(
            "• TAM: Generates Triangle Abstract Machine assembly code\n" +
            "• LLVM IR: Generates LLVM Intermediate Representation for modern optimization\n" +
            "• Both: Generates output for both backends for comparison and flexibility"
        );
        descArea.setEditable(false);
        descArea.setBackground(getBackground());
        descArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        descPanel.add(descArea, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        
        // Add panels to dialog
        add(mainPanel, BorderLayout.CENTER);
        add(descPanel, BorderLayout.SOUTH);
        add(buttonPanel, BorderLayout.PAGE_END);
        
        // Set preferred size
        setPreferredSize(new Dimension(450, 280));
    }
    
    private void setupEventHandlers() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Determine selected backend
                if (tamRadio.isSelected()) {
                    selectedBackend = BackendType.TAM;
                } else if (llvmRadio.isSelected()) {
                    selectedBackend = BackendType.LLVM_IR;
                } else {
                    selectedBackend = BackendType.BOTH;
                }
                
                cancelled = false;
                setVisible(false);
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                setVisible(false);
            }
        });
        
        // ESC key to cancel
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0, false);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                setVisible(false);
            }
        });
        
        // Enter key to confirm
        getRootPane().setDefaultButton(okButton);
    }
    
    /**
     * Show the dialog and return the selected backend
     * @return Selected backend type, or null if cancelled
     */
    public BackendType showDialog() {
        setVisible(true);
        
        if (cancelled) {
            return null;
        }
        
        return selectedBackend;
    }
    
    /**
     * Get the selected backend without showing the dialog
     * @return Currently selected backend
     */
    public BackendType getSelectedBackend() {
        return selectedBackend;
    }
    
    /**
     * Set the initially selected backend
     * @param backend Backend to select
     */
    public void setSelectedBackend(BackendType backend) {
        this.selectedBackend = backend;
        
        switch (backend) {
            case TAM:
                tamRadio.setSelected(true);
                break;
            case LLVM_IR:
                llvmRadio.setSelected(true);
                break;
            case BOTH:
                bothRadio.setSelected(true);
                break;
        }
    }
    
    /**
     * Check if the dialog was cancelled
     * @return true if cancelled, false otherwise
     */
    public boolean wasCancelled() {
        return cancelled;
    }
}