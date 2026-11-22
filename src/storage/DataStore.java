package storage;
import java.io.*;
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
            throw new IllegalArgumentException("Saved state is required.");
        }
        state.validate();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            state.writeText(writer);
        }
    }

    public Optional<SavedState> load() throws IOException {

        if (!file.exists()) {
            return Optional.empty();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            SavedState state = SavedState.readText(reader);
            state.validate();

            return Optional.of(state);
        }
    }
}
