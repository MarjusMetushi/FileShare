package src;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ServerFiles extends JFrame{
    //Passing the received files from our server to display them in this class
    public ServerFiles(List<File> receivedFiles){
        //Making a frame
        super("Uploaded files");
        setSize(700, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLocationRelativeTo(null);
        setVisible(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        //For each file in the list we make it's own panel and download button
        for (File file : receivedFiles) {
            JLabel label = new JLabel(file.getAbsolutePath());
            JButton downloadbtn = new JButton("Download");
            //add action listener to download to call the download method each time it's pressed and download a file
            downloadbtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                        download(file);
                }
            });
            panel.add(label);
            panel.add(downloadbtn);
        }
        //Adding a scroller
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }
    //Method to copy the file into the set Path
    private void download(File file) {
        //Taking the users location for the user's home directory and desktop
        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop";
        //making a destination path
        File destFile = new File(desktopPath, file.getName());
        //Copying the file to the user's desktop
        try {
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
