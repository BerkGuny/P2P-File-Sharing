package project471;

public class Main {
    public static void main(String[] args) {
        // This will create an instance of the GUI in the Event Dispatch Thread.
        javax.swing.SwingUtilities.invokeLater(GUI::new);
    }
}