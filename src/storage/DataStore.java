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
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(state);
        }
    }

    public Optional<SavedState> load() throws IOException {

        if (!file.exists()) {
            return Optional.empty();
        }

        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {

            Object object = input.readObject();

            if (!(object instanceof SavedState)) {
                throw new IOException("Invalid save file.");
            }

            SavedState state = (SavedState) object;
            state.validate();

            return Optional.of(state);

        } catch (ClassNotFoundException e) {
            throw new IOException("Could not load simulation.", e);
        }
    }
}
