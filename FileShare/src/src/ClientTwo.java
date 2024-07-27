package src;
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClientTwo extends JFrame {
    private Socket clientSocket;
    private DataOutputStream dos;
    private File selectedFile;
    private File selectedDirectory;
    private ImageIcon imgicon;
    private String path;
    private JLabel pictureLabel; 
    private JLabel textlabel;
    public ClientTwo() {
        //Let's make a frame for our client 1
        super("Client2");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        textlabel = new JLabel("This is a File sharing system, please share your files to the server!");
        JButton selectFileButton = new JButton("Select File");
        JButton selectDirButton = new JButton("Select Directory");
        JButton sendButton = new JButton("Send");
        JPanel panel = new JPanel();
        pictureLabel = new JLabel();
        path = "src\\images.png";
        imgicon = new ImageIcon(path);

        textlabel.setFont(new Font("Serif",Font.BOLD,17));

        pictureLabel.setIcon(imgicon);
        pictureLabel.setOpaque(true);
        pictureLabel.setBackground(Color.BLACK);
        getContentPane().setBackground(Color.BLACK);
        selectFileButton.setBackground(Color.GREEN);
        selectDirButton.setBackground(Color.GREEN);
        sendButton.setBackground(Color.BLUE);
        sendButton.setForeground(Color.WHITE);
        textlabel.setBackground(Color.BLACK);
        textlabel.setForeground(Color.WHITE);
        panel.setBackground(Color.BLACK);
        
        add(panel, BorderLayout.SOUTH);
        add(textlabel,BorderLayout.NORTH);

        panel.add(selectFileButton);
        panel.add(selectDirButton);
        panel.add(sendButton);
        
        JPanel picturePanel = new JPanel();
        picturePanel.add(pictureLabel);
        picturePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        picturePanel.setBackground(Color.BLACK);
        add(picturePanel,BorderLayout.CENTER);
        

        //Adding an action listener to our buttons
        selectFileButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
            }
            
        });
        selectDirButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {    
                selectDirectory();
            }
            
        });
        sendButton.addActionListener(new ActionListener() {
            //starting the connection after pressing send

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    initiateConnection();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            
        });
        setResizable(false);
        setVisible(true);
    }
    //Method to select a file by A file Chooser for files only
    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            path = "src\\fileselected.png";
            updatePicLabel();
            JOptionPane.showMessageDialog(null,"file loaded!");
        }
    }
    //Method to select a folder by A file Chooser for folders only
    private void selectDirectory() {
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = directoryChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = directoryChooser.getSelectedFile();
            System.out.println("Selected directory: " + selectedDirectory.getAbsolutePath());
            path ="src\\folderselected.png";
            updatePicLabel();
            JOptionPane.showMessageDialog(null,"folder loaded!");
        }
    }
    private void updatePicLabel(){
        imgicon = new ImageIcon(path);
        pictureLabel.setIcon(imgicon);
        pictureLabel.revalidate();
        pictureLabel.repaint();
        textlabel.setText("You uploaded a file/folder");
    }
    //Starting a connection  to connect to PORT1 of the Server class
    private void initiateConnection() throws IOException {
        try {
            clientSocket = new Socket("localhost", 54321);
            System.out.println("Connected to server port 54321.");
        } catch (IOException e) {
            System.out.println("Problem connecting");
        }

        dos = new DataOutputStream(clientSocket.getOutputStream());
        sendData();
    }
    //Method to check if we can send files or folders
    private void sendData() {
        //Checking the connection
        if (clientSocket == null || dos == null) {
            System.err.println("Connection not established.");
            return;
        }
        try {
            //calls the method  to send the selected file
            if (selectedFile != null) {
                System.out.println("Sending file: " + selectedFile.getName());
                sendFile(selectedFile);
            }
            //calls the method to send the selected directory
            if (selectedDirectory != null) {
                System.out.println("Sending directory: " + selectedDirectory.getName());
                sendDirectory(selectedDirectory);

                //zip the directory and send the zipped file
                File zippedFile = zipDirectory(selectedDirectory);
                sendFile(zippedFile);
                //deleting the zipped file after sending
                zippedFile.delete();
            }
            dos.writeUTF("END_OF_TRANSMISSION");
            dos.flush();
            System.out.println("Data sent.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Method to send the file
    private void sendFile(File file) throws IOException {
        //Writing metadata of the file
        dos.writeUTF("FILE");
        dos.writeUTF(file.getName());
        dos.writeLong(file.length());
        //Opening a file input stream and performing the send operation
        try (FileInputStream fis = new FileInputStream(file)) {
            //File will be sent chunk by chunk (4kb by 4kb)
            byte[] buffer = new byte[4096];
            int read;
            //Writing the file
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }
        }
        //Forcing and ensuring we sent everything to the stream
        dos.flush();
        System.out.println("Sent file: " + file.getName());
    }
    //Writing metadata of our directory and sending it to the next method 
    private void sendDirectory(File directory) throws IOException {
        dos.writeUTF("DIRECTORY");
        dos.writeUTF(directory.getName());
        dos.flush();
        sendDirectoryContents(directory, directory.getName());
    }
    //Takes the directory file by file and recursively searching if we have folders inside folders 
    private void sendDirectoryContents(File directory, String root) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    sendDirectoryContents(file, root + "/" + file.getName());
                    //else it will write the metadata and send the file
                } else if (file.isFile()) {
                    dos.writeUTF("FILE_IN_DIRECTORY");
                    dos.writeUTF(root + "/" + file.getName());
                    dos.writeLong(file.length());
                    //Reading and writing the file by the stream 4kb at a time
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = fis.read(buffer)) > 0) {
                            dos.write(buffer, 0, read);
                        }
                    }
                    //Force pushing the remains on the stream ensuring full transfer
                    dos.flush();
                }
            }
        }
        System.out.println("Sent directory: " + directory.getName());
    }
    //Method to zip the directory into a single ZIP file by passing it into a zip stream
    private File zipDirectory(File directory) throws IOException {
        File zipFile = new File(directory.getParentFile(), directory.getName() + ".zip");
        //try-with resources to make sure streams are completely closed
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            //Recursively zipping folders inside of folders
            zipFileContents(directory, directory.getName(), zos);
        }
        return zipFile;
    }
    //Method to recursively zip contents inside a directory, it covers cases like folder inside a folder
    private void zipFileContents(File file, String parentFolder, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                zipFileContents(childFile, parentFolder + "/" + childFile.getName(), zos);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(parentFolder);
                zos.putNextEntry(zipEntry);
                //Zipping everything chunk by chunk (4kb by 4kb)
                byte[] buffer = new byte[4096];
                int length;
                //Read and write into the ZIP output stream
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    public static void main(String[] args) {
        //Running the client
        SwingUtilities.invokeLater(() -> {
            new ClientTwo();
        });
    }
}
