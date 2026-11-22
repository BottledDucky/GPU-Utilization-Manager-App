package storage;
import java.io.*;
import java.util.*;

public final class DataStore {
    private final File file;
    public DataStore(File file) {
        this.file = file;
    }
    public void save(SavedState state) throws IOException {
        try (PrintWriter output = new PrintWriter(new FileWriter(file))) {
            state.writeText(output);
            if (output.checkError()) {
                throw new IOException("Could not save simulation.");
            }
        }
    }
    public SavedState load() throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (Scanner input = new Scanner(new FileReader(file))) {
            SavedState state = SavedState.readText(input);
            return state;
        }
    }
}
