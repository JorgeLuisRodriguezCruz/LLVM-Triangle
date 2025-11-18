package Triangle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class LLVMRunnerLLI {

    private static String lliPath;

    static {
        try {
            lliPath = LLVMPathFinder.find("lli.exe");
            if (lliPath == null) {
                throw new RuntimeException("lli.exe no encontrado en PATH.");
            }
        } catch (Exception e) {
            lliPath = null;
        }
    }

    /**
     * Guarda el código LLVM en un .ll y ejecuta con lli en una terminal visible.
     * @param llvmCode código LLVM válido
     * @param basePath ruta del .tri original (sin extensión)
     */
    public static void runLLVMWithLLI(String llvmCode, String basePath) throws Exception {
        if (lliPath == null) {
            throw new RuntimeException("lli.exe no está en el PATH. Agregá LLVM al PATH y reiniciá el IDE.");
        }

        File lliDir = new File(basePath).getParentFile();
        if (!lliDir.exists()) lliDir.mkdirs();

        
        String originalName = new File(basePath).getName();
        File llFile = new File(lliDir, originalName + ".ll");

        Files.write(llFile.toPath(), llvmCode.getBytes(StandardCharsets.UTF_8));

        // Crear .bat para ejecutar lli en terminal visible
        File batFile = new File(lliDir, "run_lli.bat");
        String batContent = String.format(
            "@echo off\n" +
            "cd /d \"%s\"\n" +
            "\"%s\" \"%s\"\n" +
            "echo.\n" +
            "pause\n",
            lliDir.getAbsolutePath(),
            lliPath,
            llFile.getAbsolutePath()
        );

        Files.write(batFile.toPath(), batContent.getBytes(StandardCharsets.UTF_8));

        // Abrir terminal
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", batFile.getAbsolutePath());
        pb.start();
    }
}