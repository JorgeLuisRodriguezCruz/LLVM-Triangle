package Triangle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
 
public class LLVMCompilerEXE {
 
    private static String llcPath;
    private static String clangPath;
    static {
        try {
            llcPath = LLVMPathFinder.getLLC();
            clangPath = LLVMPathFinder.getClang();
        } catch (Exception e) {
            // Fallback: ventana de error al primer uso
            llcPath = null;
            clangPath = null;
        }
    }

    /**
     * Compila código LLVM a .exe y lo guarda en la carpeta "ejecutables" al lado del .tri
     * @param llvmCode Código LLVM válido
     * @param basePath Ruta del archivo .tri original (sin extensión)
     * @return Ruta completa del .exe generado
     */
    public static String compileLLVMToEXE(String llvmCode, String basePath) throws Exception {
        File exeDir = new File(basePath + "_ejecutables");
        if (!exeDir.exists()) exeDir.mkdirs();

        File llFile = new File(exeDir, "temp.ll");
        File exeFile = new File(exeDir, "programa.exe");

        // 1. Guardar .ll
        Files.write(llFile.toPath(), llvmCode.getBytes());

        // 2. LLVM -> Ensamblador
        File sFile = new File(exeDir, "temp.s");
        ProcessBuilder pb1 = new ProcessBuilder(llcPath, "-filetype=asm", "-o", sFile.getAbsolutePath(), llFile.getAbsolutePath());
        pb1.redirectErrorStream(true);
        Process p1 = pb1.start();
        p1.waitFor();

        // 3. Ensamblador -> EXE
        ProcessBuilder pb2 = new ProcessBuilder(clangPath, "-o", exeFile.getAbsolutePath(), sFile.getAbsolutePath());
        pb2.redirectErrorStream(true);
        Process p2 = pb2.start();
        p2.waitFor();

        // 4. Limpiar temporales
        Files.deleteIfExists(llFile.toPath());
        Files.deleteIfExists(sFile.toPath());

        return exeFile.getAbsolutePath();
    }
    
    public static void runEXEWithBat(String exePath) throws Exception {
        File exeFile = new File(exePath);
        File dir = exeFile.getParentFile();
        File batFile = new File(dir, "run.bat");

        // 1. Crear .bat con los comandos
        String batContent = String.format(
            "@echo off\n" +
            "cd /d \"%s\"\n" +
            "\"%s\"\n" +
            "echo Exit code: %%errorlevel%%\n" +
            "pause\n",
            dir.getAbsolutePath(),
            exeFile.getAbsolutePath()
        );

        Files.write(batFile.toPath(), batContent.getBytes(StandardCharsets.UTF_8));

        // 2. Ejecutar el .bat en una terminal visible
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", batFile.getAbsolutePath());
        pb.start();
    }
}