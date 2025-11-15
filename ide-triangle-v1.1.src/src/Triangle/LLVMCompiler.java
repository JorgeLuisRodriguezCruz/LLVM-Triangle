package Triangle;

import java.io.*;
import java.nio.file.*;

public class LLVMCompiler {

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
     * Compila código LLVM a ensamblador (.s) y luego a ejecutable.
     * Devuelve el código ensamblador como String.
     */
    public static String compileLLVMToAssembly(String llvmCode) throws Exception {
        Path tempDir = Files.createTempDirectory("triangle_llvm");
        File llFile = tempDir.resolve("temp.ll").toFile();
        File sFile = tempDir.resolve("temp.s").toFile();

        // 1. Guardar .ll
        Files.write(llFile.toPath(), llvmCode.getBytes());

        // 2. LLVM -> Ensamblador
        ProcessBuilder pb1 = new ProcessBuilder(llcPath, "-filetype=asm", "-o", sFile.getAbsolutePath(), llFile.getAbsolutePath());
        pb1.redirectErrorStream(true);
        Process p1 = pb1.start();
        p1.waitFor();

        // 3. Leer ensamblador
        String assembly = Files.readString(sFile.toPath());

        // 4. Limpiar temporales
        Files.deleteIfExists(llFile.toPath());
        Files.deleteIfExists(sFile.toPath());
        Files.deleteIfExists(tempDir);

        return assembly;
    }

    /**
     * Compila LLVM a ejecutable y devuelve la salida de ejecución.
     */
    public static String compileAndRunLLVM(String llvmCode) throws Exception {
        Path tempDir = Files.createTempDirectory("triangle_llvm");
        File llFile = tempDir.resolve("temp.ll").toFile();
        File exeFile = tempDir.resolve("temp.exe").toFile();

        // 1. Guardar .ll
        Files.write(llFile.toPath(), llvmCode.getBytes());

        // 2. LLVM -> Ejecutable
        ProcessBuilder pb = new ProcessBuilder(clangPath, "-o", exeFile.getAbsolutePath(), llFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();

        // 3. Ejecutar
        ProcessBuilder runPb = new ProcessBuilder(exeFile.getAbsolutePath());
        runPb.redirectErrorStream(true);
        Process runProcess = runPb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        runProcess.waitFor();

        // 4. Limpiar
        Files.deleteIfExists(llFile.toPath());
        Files.deleteIfExists(exeFile.toPath());
        Files.deleteIfExists(tempDir);

        return output.toString();
    }
}