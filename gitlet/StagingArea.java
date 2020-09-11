package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

public class StagingArea implements Serializable {
    private HashMap<String, String> stage;
    private String directory = ".gitlet/blobs";
    StagingArea() {
        stage = new HashMap<>();
        File path = new File(directory);
        path.mkdirs();
    }

    public String add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return "";
        }
        byte[] content = Utils.readContents(file);
        String id = Utils.sha1(content);
        File blob = new File(directory + "/" + id);

        if (!stage.containsKey(fileName) || !stage.get(fileName).equals(id)) {
            stage.put(fileName, id);
            Utils.writeContents(blob, content);
        }
        return id;
    }

    public void clear() {
        stage.clear();
    }

    public HashMap<String, String> stagedFiles() {
        return stage;
    }

    public void remove(String filename) {
        stage.remove(filename);
    }
}
