package gitlet;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.io.Serializable;

public class CommitNode implements Serializable {
    private String id;
    private String logMessage;
    private String timeStamp;
    private String firstParent;
    private String secondParent;
    private HashMap<String, String> blobs;

    CommitNode(String parent, String logMessage, HashMap<String, String> blobs) {
        this.firstParent = parent;
        this.logMessage = logMessage;
        this.blobs = blobs;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timeStamp = dtf.format(LocalDateTime.now());
        if (parent == null) {
            timeStamp = dtf.format(LocalDateTime.ofEpochSecond(0,0, ZoneOffset.UTC));
        }
        String prt = parent == null ? "" : parent;
        String files = blobs.isEmpty() ? "" : blobs.toString();
        id = Utils.sha1(files, prt, logMessage, timeStamp);
    }

    public String ID() {
        return id;
    }

    public String timeStamp() {
        return timeStamp;
    }

    public String firstParent() {
        return firstParent;
    }

    public String secondParent() {
        return secondParent;
    }

    public void setSecondParent(String secondParent) {
        this.secondParent = secondParent;
    }

    public String logMessage() {
        return logMessage;
    }

    public HashMap<String, String> blobs() {
        return blobs;
    }

    public CommitNode copy() {
        CommitNode copy = new CommitNode(firstParent, logMessage, blobs);
        copy.secondParent = this.secondParent;
        copy.timeStamp = this.timeStamp;
        copy.id = this.id;
        return copy;
    }
}
