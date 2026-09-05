package vercoi.gui;

import vercoi.generator.XacmlGenerator;
import vercoi.model.*;
import vercoi.parser.*;
import vercoi.verification.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.List;

public final class VerCoIApp extends JFrame {
    private final JTextArea textInput=area(), xacmlOutput=area(), conflictInput=area(), conflictOutput=area(),
            complianceXacml=area(), complianceJava=area(), complianceOutput=area();

    public VerCoIApp(){
        super("VerCoI V2 - Conflict and Compliance Verification");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1150,780);setLocationRelativeTo(null);
        JTabbedPane tabs=new JTabbedPane();
        tabs.add("Txt2Xacml",txtTab());
        tabs.add("Consistency Verification",conflictTab());
        tabs.add("Compliance Verification",complianceTab());
        add(tabs);
    }

    private JPanel txtTab(){
        JPanel p=new JPanel(new BorderLayout(6,6));
        JSplitPane sp=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(textInput),new JScrollPane(xacmlOutput));sp.setResizeWeight(.45);
        JButton run=new JButton("Convert to XACML PolicySet");
        run.addActionListener(e->safe(()->{
            List<PolicyRule> r=ControlledTextParser.parse(textInput.getText());
            xacmlOutput.setText(XacmlGenerator.generate("AccessControlPolicySet",CombiningAlgorithm.FIRST_APPLICABLE,r));
        }));
        JButton load=new JButton("Load TXT");load.addActionListener(e->load(textInput));
        JPanel b=new JPanel();b.add(load);b.add(run);p.add(sp);p.add(b,BorderLayout.SOUTH);return p;
    }

    private JPanel conflictTab(){
        JPanel p=new JPanel(new BorderLayout(6,6));
        JSplitPane sp=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(conflictInput),new JScrollPane(conflictOutput));sp.setResizeWeight(.55);
        JButton run=new JButton("Detect and Resolve");
        run.addActionListener(e->safe(()->{
            ParsedPolicy pol=XacmlParser.parse(conflictInput.getText());
            StringBuilder b=new StringBuilder();
            var dup=DuplicateDetector.findDuplicates(pol.rules());
            var analysis=ConflictResolver.analyze(pol.rules(),pol.combiningAlgorithm());
            b.append("Policy/PolicySet: ").append(pol.policyId())
             .append("\nCombining algorithm: ").append(pol.combiningAlgorithm())
             .append("\nPolicies: ").append(pol.policies().size())
             .append("\nRules: ").append(pol.rules().size())
             .append("\nDuplicate groups: ").append(dup.size())
             .append("\nConflicts: ").append(analysis.conflicts().size()).append("\n");
            for(var r:analysis.resolutions()) b.append("CONFLICT: ").append(r.conflict().left().ruleId())
                    .append(" <-> ").append(r.conflict().right().ruleId())
                    .append(" actions=").append(r.conflict().overlappingActions())
                    .append(" => ").append(r.resolvedEffect()).append(" by ").append(r.algorithm()).append("\n");
            conflictOutput.setText(b.toString());
        }));
        JButton load=new JButton("Load XACML/XML");load.addActionListener(e->load(conflictInput));
        JPanel b=new JPanel();b.add(load);b.add(run);p.add(sp);p.add(b,BorderLayout.SOUTH);return p;
    }

    private JPanel complianceTab(){
        JPanel top=new JPanel(new GridLayout(1,2,6,6));top.add(new JScrollPane(complianceXacml));top.add(new JScrollPane(complianceJava));
        JSplitPane sp=new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,new JScrollPane(complianceOutput));sp.setResizeWeight(.65);
        JButton run=new JButton("Check compliance");
        run.addActionListener(e->safe(()->{
            ParsedPolicy p=XacmlParser.parse(complianceXacml.getText());
            if(!ConflictDetector.findConflicts(p.rules()).isEmpty())
                throw new IllegalArgumentException("XACML contains unresolved conflicts. Run Consistency Verification first.");
            var act=JavaPolicyParser.parse(complianceJava.getText());
            var v=ComplianceChecker.check(p.rules(),act);
            StringBuilder b=new StringBuilder("Expected policy rules: "+p.rules().size()+"\nRecognized Java authorization calls: "+act.size()+"\nViolations: "+v.size()+"\n");
            for(var x:v)b.append(x.type()).append(": ").append(x.subject()).append(" / ").append(x.resource()).append(" / ").append(x.action())
                    .append(" expected=").append(x.expected()).append(" actual=").append(x.actual()).append(" - ").append(x.detail()).append("\n");
            complianceOutput.setText(b.toString());
        }));
        JButton lx=new JButton("Load XACML");lx.addActionListener(e->load(complianceXacml));
        JButton lj=new JButton("Load Java");lj.addActionListener(e->load(complianceJava));
        JPanel buttons=new JPanel();buttons.add(lx);buttons.add(lj);buttons.add(run);
        JPanel p=new JPanel(new BorderLayout());p.add(sp);p.add(buttons,BorderLayout.SOUTH);return p;
    }

    private static JTextArea area(){JTextArea a=new JTextArea();a.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));return a;}
    private void load(JTextArea target){JFileChooser c=new JFileChooser();if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)safe(()->target.setText(Files.readString(c.getSelectedFile().toPath())));}
    private void safe(Throwing r){try{r.run();}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}}
    @FunctionalInterface private interface Throwing{void run()throws Exception;}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new VerCoIApp().setVisible(true));}
}
