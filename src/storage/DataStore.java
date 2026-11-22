package storage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class DataStore {
    private final File file;
    public DataStore(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Save file is required.");
        }
        this.file = file;
    }
    public void save(SavedState state) throws IOException {
        if (state == null) {
            throw new IllegalArgumentException("Saved state is required.");}
        state.validate();
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null) {
            Files.createDirectories(folder.toPath());}
        try (PrintWriter output = new PrintWriter(new FileWriter(file))) {
            state.writeText(output);
            if (output.checkError()) {
                throw new IOException("Could not save simulation.");}}
    }
    public Optional<SavedState> load() throws IOException {
        if (!file.exists()) {
            return Optional.empty();
        }
        try (Scanner input = new Scanner(new FileReader(file))) {
            SavedState state = SavedState.readText(input);
            state.validate();
            return Optional.of(state);
        }
    }
}
