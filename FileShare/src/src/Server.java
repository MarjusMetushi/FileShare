package src;

import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Server extends JFrame {
    private static final int PORT1 = 12345;
    private static final int PORT2 = 54321;
    private static final List<File> receivedFiles = new CopyOnWriteArrayList<>();

    public Server() {
        super("Server");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create a panel with GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.BLACK);

        // Configure GridBagConstraints for the button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        // Create the button
        JButton view = new JButton("View uploaded files");
        view.setFont(new Font("Arial", Font.BOLD, 14));
        view.setPreferredSize(new Dimension(200, 50));
        view.setBackground(Color.DARK_GRAY);
        view.setForeground(Color.WHITE);
        view.setFocusPainted(false);
        view.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Add action listener to the button
        view.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> new ServerFiles(receivedFiles));
            }
        });

        // Add the button to the panel
        panel.add(view, gbc);

        // Add the panel to the frame
        add(panel);
        setResizable(false);
        setVisible(true);
    }

    public static void main(String[] args) {
        // Starting connections by threads to connect to user1 and user2 by a specific port
        SwingUtilities.invokeLater(Server::new);
        new Thread(() -> startServer(PORT1)).start();
        new Thread(() -> startServer(PORT2)).start();
    }

    // StartServer method to start the serversocket
    private static void startServer(int port) {
        // try-with resources are used to make sure the serverSocket is closed properly
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                // Accepting connections and starting the handle client which will handle the connections with clients
                Socket socket = serverSocket.accept();
                System.out.println("Client connected on port " + port);
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // HandleClient method to handle the client connections and transfers
    private static void handleClient(Socket socket) {
        // Getting the files/folders from the client using a DataInputStream
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            while (true) {
                // Depicting if the file is a file or directory
                String type = dis.readUTF();
                System.out.println("Received type: " + type);
                // if it's a file then call a method to receive this file
                if (type.equals("FILE")) {
                    receiveFile(dis);
                    // else if it's a file in directory call a method to receive files of directories
                } else if (type.equals("FILE_IN_DIRECTORY")) {
                    receiveFileInDirectory(dis);
                    // else if it's a whole directory we call a method to receive directories
                } else if (type.equals("DIRECTORY")) {
                    String dirName = dis.readUTF();
                    System.out.println("Directory " + dirName + " created successfully");
                    // else if there are no more files then we break out of the while loop
                } else if (type.equals("END_OF_TRANSMISSION")) {
                    System.out.println("All files received.");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to receive files
    private static void receiveFile(DataInputStream dis) throws IOException {
        // Reading metadata of files
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        // Making a new file object for the received files
        File receivedFile = new File("received_files/" + fileName);
        // Create parent directories if they do not exist
        if (receivedFile.getParentFile() != null) {
            receivedFile.getParentFile().mkdirs();
        }
        // using try-with resources to make sure the outputstream is correctly closed
        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
            // Reading and writing the file chunk by chunk(4kb by 4kb)
            byte[] buffer = new byte[4096];
            int read;
            long remaining = fileSize;
            while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
        // Thread-safe add to the received files list
        receivedFiles.add(receivedFile);

        // Indicator that the file is received
        System.out.println("File Received " + fileName);
    }

    // Method to receive files of directories
    private static void receiveFileInDirectory(DataInputStream dis) throws IOException {
        // Reading metadata of the file
        String filePath = dis.readUTF();
        long fileSize = dis.readLong();
        System.out.println("Receiving file in directory: " + filePath + ", size: " + fileSize);
        // Making a file object for the received files
        File receivedFile = new File("received_files/" + filePath);
        // Create parent directories if they do not exist
        if (receivedFile.getParentFile() != null) {
            receivedFile.getParentFile().mkdirs();
        }
        // Using try-with resources to make sure fileoutputstream closes properly
        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
            // Writing and reading the file chunk by chunk (4kb by 4kb)
            byte[] buffer = new byte[4096];
            int read;
            long remaining = fileSize;
            while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
        // Thread-safe add to the received files list
        receivedFiles.add(receivedFile);

        // Calling the method to zip these files
        zipItems(receivedFile);
        System.out.println("Received file in directory: " + filePath);
    }

    // Method to zip received files
    private static void zipItems(File receivedFile) throws IOException {
        // Making a new folder used to store the received files
        File folder = new File("Zipped_" + receivedFile.getName());
        // If folder doesn't exist then make one
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // Copy files to folder
        for (File file : receivedFiles) {
            // a new file will combine folder and the name of the file to create the path for the destination
            File destFile = new File(folder, file.getName());
            // And then the file gets copied to the destination
            copyFile(file, destFile);
        }

        File zipFile = new File("zipped_" + folder.getName() + ".zip");
        // using try-with resources to ensure fileoutputstream and zipoutputstream get closed neatly
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // Zip all files in the received list
            for (File file : receivedFiles) {
                if (file.isDirectory()) {
                    zipFolder(file, file.getName(), zos);
                } else {
                    // or else create a fileinputstream inside try-with resources
                    try (FileInputStream fis = new FileInputStream(file)) {
                        // new zip entry
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        // Preparing zipoutputstream to accept new data for the zip entry we made
                        zos.putNextEntry(zipEntry);
                        // We read and write into the zip output stream
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        // Closing the entry
                        zos.closeEntry();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Zipped up and sent");
    }

    // Copy file method with try-with resources that will read and write from the input stream
    // of the source to the destination, effectively copying the file
    private static void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    // Making a zip folder method to zip the folders
    private static void zipFolder(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        // for each file in the folder we check for directories recursively
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipFolder(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            // try-with resources to ensure fileinputstream is correctly closed
            try (FileInputStream fis = new FileInputStream(file)) {
                // Making a zip entry
                ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                // Preparing zipoutputstream to accept data
                zos.putNextEntry(zipEntry);
                // Reading and writing into the zipoutputstream chunk by chunk(4kb by 4kb)
                byte[] buffer = new byte[4096];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                // closing the zipoutputstream
                zos.closeEntry();
            }
        }
    }
}
