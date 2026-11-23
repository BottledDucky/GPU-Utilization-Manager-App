package storage;
import java.io.*;
import java.util.*;

public final class DataStore {
    private final File file;
    public DataStore(File file) {
        this.file = file;
    }
    
    public void save(SavedState state) throws IOException
    {
        File folder = file.getParentFile();

        if (folder != null && !folder.exists())
        {
            folder.mkdirs();
        }

        try (PrintWriter output = new PrintWriter(new FileWriter(file)))
        {
            state.writeText(output);

            if (output.checkError())
            {
                throw new IOException("Could not save simulation.");
            }
        }
    }
    
    public Optional<SavedState> load() throws IOException {
        if (!file.exists()) {
            return Optional.empty();
        }
        try (Scanner input = new Scanner(new FileReader(file))) {
            SavedState state = SavedState.readText(input);
            return Optional.of(state);
        }
    }
}
