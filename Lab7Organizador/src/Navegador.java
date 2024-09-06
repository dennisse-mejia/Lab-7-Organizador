
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

public class Navegador extends JPanel implements ActionListener {

    public File dirActual;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private File clipboard;
    private JButton ordenar;
    private JButton renombrar;
    private JButton crear;
    private JButton copiar;
    private JButton pegar;
    private JButton escribir;

    public Navegador() {
        this.clipboard = null;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(730, 480));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(255, 240, 245));

        dirActual = new File("Z");
        if (!dirActual.exists()) {
            dirActual.mkdir();
        }

        rootNode = new DefaultMutableTreeNode(dirActual.getName());
        fileTree = new JTree(rootNode);
        treeModel = (DefaultTreeModel) fileTree.getModel();

        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        panel.add(treeScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 5, 10, 10));
        buttonPanel.setBackground(new Color(255, 240, 245));

        ordenar = crearBoton("Ordenar");
        renombrar = crearBoton("Renombrar");
        crear = crearBoton("Crear");
        copiar = crearBoton("Copiar");
        pegar = crearBoton("Pegar");
        escribir = crearBoton("Escribir");

        buttonPanel.add(ordenar);
        buttonPanel.add(renombrar);
        buttonPanel.add(crear);
        buttonPanel.add(copiar);
        buttonPanel.add(pegar);
        buttonPanel.add(escribir);

        panel.add(buttonPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        loadFileTree();
        setVisible(true);
    }

    private JButton crearBoton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(255, 182, 193));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.addActionListener(this);
        return button;
    }

    private void loadFileTree() {
        rootNode.removeAllChildren();
        addNodes(rootNode, dirActual);
        File[] archivos = dirActual.listFiles();

        printLastModifiedDates(archivos);  

        treeModel.reload();
    }

    private void addNodes(DefaultMutableTreeNode parentNode, File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();

            if (childFiles != null) {
                Arrays.sort(childFiles, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1; 
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1; 
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName()); 
                    }
                });

                for (File childFile : childFiles) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childFile.getName());
                    parentNode.add(childNode);

                    if (childFile.isDirectory()) {
                        addNodes(childNode, childFile);
                    }
                }
            }
        }
    }

    private void createFileOrFolder() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        System.out.println("Selected Node: " + selectedNode);

        if (selectedNode != null) {
            File selectedFile;

            String fullPath = getFullPath(selectedNode);
            selectedFile = new File(fullPath);

            System.out.println("Selected Node: " + selectedNode);
            System.out.println("Selected File Path: " + selectedFile.getAbsolutePath());
            System.out.println("Es directorio: " + selectedFile.isDirectory());


            System.out.println("Selected File Path: " + selectedFile.getAbsolutePath());
            System.out.println("Es directorio: " + selectedFile.isDirectory());

            if (selectedFile.isDirectory()) {
                String[] options = {"Archivo", "Carpeta"};
                int choice = JOptionPane.showOptionDialog(this,
                        "¿Qué desea crear?",
                        "Crear",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice != -1) {
                    String name = JOptionPane.showInputDialog("Ingrese el nombre:");
                    if (name != null && !name.trim().isEmpty()) {
                        File newFile;
                        if (choice == 0) {
                            newFile = new File(selectedFile, name);
                            try {
                                if (newFile.createNewFile()) {
                                    System.out.println("Archivo creado: " + newFile.getAbsolutePath());
                                } else {
                                    JOptionPane.showMessageDialog(this, "No se pudo crear el archivo.");
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(this, "Error al crear el archivo.");
                            }
                        } else { // Crear carpeta
                            newFile = new File(selectedFile, name);
                            if (newFile.mkdir()) {
                                System.out.println("Carpeta creada: " + newFile.getAbsolutePath());
                            } else {
                                JOptionPane.showMessageDialog(this, "No se pudo crear la carpeta.");
                            }
                        }
                        loadFileTree();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione una carpeta válida.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "No hay ninguna carpeta seleccionada.");
        }
    }

    private void copiar() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        if (selectedNode != null) {
            String fullPath = getFullPath(selectedNode); 
            clipboard = new File(fullPath);

            System.out.println("Archivo/Carpeta copiado al portapapeles: " + clipboard.getAbsolutePath());
        }
    }

    private void pegar() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        if (clipboard != null && selectedNode != null) {
            String destinoPath;

            if (selectedNode == rootNode) {
                destinoPath = dirActual.getAbsolutePath();
            } else {
                destinoPath = getFullPath(selectedNode);
            }

            File destinoDir = new File(destinoPath);

            if (!destinoDir.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Seleccione una carpeta válida para pegar el archivo.");
                return;
            }

            File destino = new File(destinoDir, clipboard.getName());

            if (destino.exists()) {
                destino = obtenerNombreUnico(destino);  
            }

            try {
                if (clipboard.isDirectory()) {
                    copiarDirectorio(clipboard, destino);
                } else {
                    copiarArchivo(clipboard, destino);
                }

                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(destino.getName());
                selectedNode.add(newNode);  
                addNodes(newNode, destino);
                treeModel.reload();

                fileTree.setSelectionPath(new TreePath(newNode.getPath()));

                System.out.println("Archivo o carpeta copiado correctamente a: " + destino.getAbsolutePath());

                clipboard = destino; 
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al copiar el archivo o carpeta.");
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "No hay ningún archivo en el portapapeles o destino no seleccionado.");
        }
    }

    private File getNewFileName(File dir, String originalName) {
        String name = originalName;
        String extension = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex > 0) {
            name = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        int count = 1;
        File newFile = new File(dir, name + "_" + count + extension);
        while (newFile.exists()) {
            count++;
            newFile = new File(dir, name + "_" + count + extension);
        }
        return newFile;
    }

    private File obtenerNombreUnico(File destino) {
        String nombre = destino.getName();
        String path = destino.getParent();
        String extension = "";

        int dotIndex = nombre.lastIndexOf(".");
        if (dotIndex != -1) {
            extension = nombre.substring(dotIndex);
            nombre = nombre.substring(0, dotIndex);
        }

        int contador = 1;
        File nuevoDestino = new File(path, nombre + extension);

        while (nuevoDestino.exists()) {
            nuevoDestino = new File(path, nombre + "(" + contador + ")" + extension);
            contador++;
        }

        return nuevoDestino;
    }

    private void copiarArchivo(File origen, File destino) throws IOException {
        if (!origen.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo de origen no existe.");
            return;
        }

        try (InputStream in = new FileInputStream(origen); OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }

        if (!destino.exists()) {
            JOptionPane.showMessageDialog(this, "No se pudo copiar el archivo.");
        } else {
            System.out.println("Archivo copiado a: " + destino.getAbsolutePath());
        }
    }

    private void copiarDirectorio(File origen, File destino) throws IOException {
        if (!destino.exists()) {
            destino.mkdir();
        }

        for (String archivo : origen.list()) {
            File origenArchivo = new File(origen, archivo);
            File destinoArchivo = new File(destino, archivo);

            if (origenArchivo.isDirectory()) {
                copiarDirectorio(origenArchivo, destinoArchivo);
            } else {
                copiarArchivo(origenArchivo, destinoArchivo);
            }
        }
    }

    private void renameFileOrFolder() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        if (selectedNode != null) {
            if (selectedNode == rootNode) {
                JOptionPane.showMessageDialog(this, "El archivo raíz no puede ser renombrado.");
                return; 
            }

            String fullPath = getFullPath(selectedNode);
            File file = new File(fullPath);

            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "El archivo no existe: " + file.getAbsolutePath());
                System.out.println("Archivo no encontrado en la ruta: " + file.getAbsolutePath());
                return;
            }

            System.out.println("Intentando renombrar archivo: " + file.getAbsolutePath());

            String newName = JOptionPane.showInputDialog("Ingrese el nuevo nombre:");
            if (newName != null && !newName.trim().isEmpty()) {
                File newFile = new File(file.getParent(), newName);

                if (newFile.exists()) {
                    newFile = obtenerNombreUnico(newFile);
                }

                if (file.renameTo(newFile)) {
                    System.out.println("Archivo renombrado correctamente a: " + newFile.getAbsolutePath());
                    loadFileTree(); 
                } else {
                    System.out.println("Error al renombrar archivo: " + file.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
                    JOptionPane.showMessageDialog(this, "No se pudo renombrar el archivo o carpeta. Verifique permisos o si el archivo está en uso.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "El nombre proporcionado no es válido.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "No hay ninguna carpeta o archivo seleccionado.");
        }
    }

    private String getFullPath(DefaultMutableTreeNode node) {
        StringBuilder fullPath = new StringBuilder();
        TreeNode[] path = node.getPath();

        for (int i = 1; i < path.length; i++) {
            fullPath.append("/").append(path[i].toString());
        }

        return dirActual.getAbsolutePath() + fullPath.toString();
    }

    private void printLastModifiedDates(File[] archivos) {
        if (archivos == null || archivos.length == 0) {
            System.out.println("No se encontraron archivos en el directorio.");
            return;
        }
        for (File archivo : archivos) {
            System.out.println("Archivo: " + archivo.getName() + " - Última modificación: " + new java.util.Date(archivo.lastModified()));
        }
    }

    private void ordenarPor(Comparator<File> comparador) {
        File[] archivos = dirActual.listFiles();

        if (archivos != null) {
            Arrays.sort(archivos, comparador);

            rootNode.removeAllChildren();

            for (File archivo : archivos) {
                DefaultMutableTreeNode archivoNode = new DefaultMutableTreeNode(archivo.getName());
                rootNode.add(archivoNode);
                if (archivo.isDirectory()) {
                    addNodes(archivoNode, archivo);
                }
            }

            treeModel.reload();
        }
    }

    private int alphanumCompare(String s1, String s2) {
        int result = s1.compareToIgnoreCase(s2);

        if (s1.matches("\\d+") && s2.matches("\\d+")) {
            return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
        }

        if (s1.matches("\\d+")) {
            return 1;
        } else if (s2.matches("\\d+")) {
            return -1;
        }

        return result;
    }

    private void ordenarArchivos() {
        String[] opciones = {"Nombre", "Tipo", "Tamaño", "Fecha"};
        int opcion = JOptionPane.showOptionDialog(this, "¿Cómo desea ordenar los archivos?",
                "Ordenar", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        switch (opcion) {
            case 0:
                ordenarPor((f1, f2) -> {
                    if (f1.isDirectory() && f2.isDirectory()) {
                        return f1.getName().compareToIgnoreCase(f2.getName()); 
                    } else if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1; 
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1; 
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName()); 
                    }
                });
                break;
            case 1:
                ordenarPor((f1, f2) -> Boolean.compare(f1.isDirectory(), f2.isDirectory()));
                break;
            case 2:
                ordenarPor((f1, f2) -> {
                    if (f1.isDirectory() && f2.isDirectory()) {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    } else if (f1.isDirectory()) {
                        return -1;
                    } else if (f2.isDirectory()) {
                        return 1;  
                    } else {
                        return Long.compare(f1.length(), f2.length()); 
                    }
                });
                break;
            case 3:
                ordenarPor((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                break;
        }
         treeModel.reload();

    }

    private void writeToFile() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        if (selectedNode != null) {
            String fullPath = getFullPath(selectedNode);
            File file = new File(fullPath);

            System.out.println("File Path for writing: " + file.getAbsolutePath());

            if (file.isFile()) {
                String content = JOptionPane.showInputDialog(this, "Ingrese el contenido del archivo:");
                if (content != null) {
                    try (FileWriter writer = new FileWriter(file, false)) {
                        writer.write(content);
                        file.setLastModified(System.currentTimeMillis());

                        JOptionPane.showMessageDialog(this, "Contenido escrito en el archivo.");
                        loadFileTree();
                        ordenarArchivos();

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error al escribir en el archivo.");
                        ex.printStackTrace();
                    }
                }
            } else if (file.isDirectory()) {
                JOptionPane.showMessageDialog(this, "No se puede escribir en carpetas.");
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione un archivo.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "No hay ningún archivo seleccionado.");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Button clicked: " + e.getActionCommand());
        if (e.getSource() == crear) {
            createFileOrFolder();
        } else if (e.getSource() == copiar) {
            copiar();
        } else if (e.getSource() == pegar) {
            pegar();
        } else if (e.getSource() == renombrar) {
            renameFileOrFolder();
        } else if (e.getSource() == ordenar) {
            ordenarArchivos();
        } else if (e.getSource() == escribir) {
            writeToFile();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Navegador de Archivos");
        Navegador navegador = new Navegador();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(navegador);
        frame.pack();
        frame.setVisible(true);
    }

}
