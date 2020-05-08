package driver;

import antlr4.BLexer;
import antlr4.BParser;
import interpreter.FunctionVisitor;
import interpreter.TheVisitor;
import interpreter.Function;
import model.Scope;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

import static org.antlr.v4.runtime.CharStreams.fromString;

public class B extends JFrame {

    private JPanel cp;
    private static TextArea outputArea;
    private static ArrayList<String> errors;

    private B() {
        TitledBorder title = BorderFactory.createTitledBorder("B++ programming");

        cp = new JPanel();
        cp.setLayout(null);
        cp.setBorder(title);

        JPanel codePanel = new JPanel(new BorderLayout());


        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle("text/B");
        textArea.setCodeFoldingEnabled(true);
        textArea.setFont(new Font("Century Gothic", Font.PLAIN, 14));
        RTextScrollPane sp = new RTextScrollPane(textArea);
        codePanel.add(sp);

        codePanel.setSize(700, 500);

        RSyntaxTextArea.setTemplatesEnabled(true);

        codePanel.setLocation(260,20);
        cp.add(codePanel);

        outputArea = new TextArea();
        outputArea.setSize(950, 150);
        outputArea.setFont(new Font("Century Gothic", Font.BOLD, 13));

        cp.add(outputArea);
        outputArea.setLocation(20, 580);


        Label label = new Label("B++");
        label.setSize(150,50);
        label.setFont(new Font("Arial Black", Font.BOLD, 40));

        cp.add(label);
        label.setLocation(20, 100);

        Label label2 = new Label("Program");
        label2.setSize(170,50);
        label2.setFont(new Font("Arial Black", Font.BOLD, 40));

        cp.add(label2);
        label2.setLocation(20, 140);


        JLabel name2 = new JLabel("Dominic Chan");
        name2.setSize(200,75);
        name2.setFont(new Font("Rockwell", Font.PLAIN, 12));

        cp.add(name2);
        name2.setLocation(20, 400);

        

        JLabel name3 = new JLabel("Angelo Chua");
        name3.setSize(200,75);
        name3.setFont(new Font("Rockwell", Font.PLAIN, 12));

        cp.add(name3);
        name3.setLocation(20, 430);

       

        JLabel name4 = new JLabel("Emmanuel Ramin");
        name4.setSize(200,75);
        name4.setFont(new Font("Rockwell", Font.PLAIN, 12));

        cp.add(name4);
        name4.setLocation(20, 460);

        JButton runButton = new JButton("Run");
        runButton.setSize(220, 50);
        runButton.setForeground(Color.RED);
        runButton.setFont(new Font("Arial", Font.BOLD, 15));
        runButton.addActionListener(e -> {
            perform(textArea.getText());
        });

        cp.add(runButton);
        runButton.setLocation(750, 530);

        JButton manualButton = new JButton("B User Manual");
        manualButton.setFont(new Font("Arial", Font.BOLD, 12));
        manualButton.setSize(220, 50);
        manualButton.addActionListener(e -> {
            openManual();
        });

        cp.add(manualButton);
        manualButton.setLocation(20, 530);

        setContentPane(cp);
        pack();
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        errors = new ArrayList<>();
        SwingUtilities.invokeLater(() -> {
            try {
                String gui = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(gui);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new B().setVisible(true);
        });
    }

    private static void perform(String code)  {
        outputArea.setText("");
        errors.clear();

        BLexer lexer = new BLexer(fromString(code));
        BParser parser = new BParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.source();
        Scope scope = new Scope();
        HashMap<String, Function> functions = new HashMap<>();

        //parser.addErrorListener(ConsoleErrorListener.INSTANCE);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE);

        ErrorVisitor errorVisitor = new ErrorVisitor();
        errorVisitor.visit(tree);

        if(errors.size() > 0) {
        	outputArea.append("ERROR DETECTED \n");
            for(String error : errors)
                outputArea.append(error + "\n");
        } else {
            FunctionVisitor functionVisitor = new FunctionVisitor(functions);
            functionVisitor.visit(tree);
            TheVisitor theVisitor = new TheVisitor(scope, functions);
            theVisitor.visit(tree);
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            viewer.open();
        }
    }

    private static void openManual() {
        JFrame manualFrame = new JFrame("B User Manual");
        JPanel manualPanel = new JPanel(null);

        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setSize(300, 410);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12));

        manualPanel.add(textArea);
        textArea.setLocation(350, 30);

        JButton variableDeclaration = new JButton("Variable Declarations");
        variableDeclaration.setSize(250,50);

        variableDeclaration.addActionListener(e -> {
            textArea.setText("There are no data types in the grammar B.\n" +
                            "It is similar to the Python language\n\n" +
                            "Syntax: *identifier* = ... \n\n EXAMPLE: \n a = 5 + 5; ");
        });

        manualPanel.add(variableDeclaration);
        variableDeclaration.setLocation(50, 30);

        JButton inputOrOutput = new JButton("Input/ Output Statements");
        inputOrOutput.setSize(250, 50);
        inputOrOutput.addActionListener(e -> {

            textArea.setText("1. Input \n" +
                            "Syntax: bar a = bnput();\n or \n a = bnput();\n\n" +
                            "2. Output \n" +
                            "Syntax: brint(identifier or string);");
        });

        manualPanel.add(inputOrOutput);
        inputOrOutput.setLocation(50, 90);

        JButton conditional = new JButton("Conditional Statements");
        conditional.setSize(250, 50);
        conditional.addActionListener(e -> {
           textArea.setText("Conditional Statements: \n" +
                   "Syntax: bf condition { \n *statements* \n } bf blse condition {\n *statements* \n} \n blse {*statements*}");
        });

        manualPanel.add(conditional);
        conditional.setLocation(50, 150);

        JButton loop = new JButton("Loops");
        loop.setSize(250, 50);
        loop.addActionListener(e -> {
            textArea.setText("Loops: \n\n" +
                             "1. For loop \n" +
                             "Syntax: bor i = 0 bto { \n *insert statements * \n}\n " +
                             "2. Do While loop \n" +
                             "Syntax: bdo { \n *insert statements * \n} bhile(condition)\n" +
                             "3. While loop \n" +
                             "Syntax: bhile condition { \n *insert statements * \n}\n");
        });

        manualPanel.add(loop);
        loop.setLocation(50, 210);


        JButton keywords = new JButton("Keywords");
        keywords.setSize(250, 50);
        keywords.addActionListener(e -> {
            textArea.setText(//"MAIN: \t 'bain'\n" +
                             "PRINT:  'brint' \n" +
                             "INPUT:  'bnput' \n" +
                            // "FUNC: \t 'bunc' \n" +
                            // "CONST: \t 'bonst' \n" +
                             "IF: 'bf' \n " +
                             "ELSE: 'blse' \n " +
                             "FOR:  'bor' \n " +
                             "WHILE: 'bhile' \n " +
                             "DO: 'bdo' \n " +
                             "NULL: 'bull' \n "
                            );
        });

        JButton codeShortcuts = new JButton("Sample Codes");
        codeShortcuts.setSize(250, 50);

        manualPanel.add(codeShortcuts);
        codeShortcuts.setLocation(50, 390);
        codeShortcuts.addActionListener(e -> {
            textArea.setText("Open Link: \n https://tinyurl.com/SampleBppCodes");
        });

        manualPanel.add(keywords);
        keywords.setLocation(50, 330);

        manualFrame.setContentPane(manualPanel);
        manualFrame.pack();
        manualFrame.setSize(700, 500);
        manualFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        manualFrame.setLocationRelativeTo(null);
        manualFrame.setVisible(true);
    }


    public static void appendOutput(String output) {
        outputArea.append(output + "\n");
    }

    public static String getInput() {
        final String[] result = {""};

        JFrame f = new JFrame("Input");
        return JOptionPane.showInputDialog(f, "Enter Input");
    }

    static void addErrors(String error) {
        errors.add(error);
    }
}
