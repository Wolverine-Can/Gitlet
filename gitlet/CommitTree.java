package gitlet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class CommitTree implements Serializable {
    CommitNode initialCommit;
    CommitNode head;
    Map<String, CommitNode> branches;
    String currentBranchName;
    StagingArea stagingArea;
    Set<String> removedFiles;
    Set<String> untrackedFiles;
    Map<String, CommitNode> idToCommits;

    public CommitTree() {
        initialCommit = new CommitNode(null, "initial commit", new HashMap<>());
        head = initialCommit;
        branches = new HashMap<>(){{put("master", initialCommit);}};
        currentBranchName = "master";
        stagingArea = new StagingArea();
        removedFiles = new HashSet<>();
        untrackedFiles = new HashSet<>();
        idToCommits = new HashMap<>(){{put(initialCommit.ID(), initialCommit);}};
    }

    public void add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String id = stagingArea.add(fileName);
        if (head.blobs().getOrDefault(fileName, "").equals(id)) {
            stagingArea.remove(fileName);
        }
        untrackedFiles.remove(fileName);
        removedFiles.remove(fileName);
    }

    public void commit(String message) {
        if (message.isEmpty()) {
            System.out.println("Please enter a commit message");
            return;
        }
        if (stagingArea.stagedFiles().isEmpty() && removedFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        HashMap<String, String> Blobs = new HashMap<>(head.blobs());
        Blobs.putAll(stagingArea.stagedFiles());
        for (String fileName : removedFiles) {
            Blobs.remove(fileName);
        }
        CommitNode newCommit = new CommitNode(head.ID(), message, Blobs);
        idToCommits.put(newCommit.ID(), newCommit);
        head = newCommit;
        branches.put(currentBranchName, newCommit);
        stagingArea.clear();
        removedFiles.clear();
    }

    public void rm(String fileName) {
        File file = new File(fileName);
        if (!stagingArea.stagedFiles().containsKey(fileName) && !head.blobs().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        stagingArea.remove(fileName);
        if (head.blobs().containsKey(fileName)) {
            head.blobs().remove(fileName);
            Utils.restrictedDelete(file);
            removedFiles.add(fileName);
        }
    }

    public void log() {
        CommitNode node = head;
        while(node != null) {
            System.out.println("===");
            System.out.println("commit " + node.ID());
            System.out.println("Date:  " + node.timeStamp());
            System.out.println(node.logMessage() + "\n");
            node = idToCommits.get(node.firstParent());
        }
    }

    public void globalLog() {
        for (CommitNode commit : idToCommits.values()) {
            System.out.println("===");
            System.out.println("commit " + commit.ID());
            System.out.println("Date:  " + commit.timeStamp());
            System.out.println(commit.logMessage() + "\n");
        }
    }

    public void find(String message) {
        boolean found = false;
        for(String id : idToCommits.keySet()) {
            if (idToCommits.get(id).logMessage().equals(message)) {
                found = true;
                System.out.println(id);
            }
        }
        if(!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        for(String branchName : branches.keySet()) {
            if (branchName.equals(currentBranchName)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println("\n=== Staged Files ===");
        for(String stagedFile : stagingArea.stagedFiles().keySet()) {
            System.out.println(stagedFile);
        }
        System.out.println("\n=== Removed Files ===");
        for(String removedFile : removedFiles) {
            System.out.println(removedFile);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String fileName : head.blobs().keySet()) {
            File file = new File(fileName);
            if(!file.exists() && !removedFiles.contains(fileName)) {
                System.out.println(fileName + " (deleted)");
            }
            if (file.exists() && !stagingArea.stagedFiles().containsKey(fileName)
                    && fileModified(head.blobs().get(fileName), fileName)) {
                System.out.println(fileName + " (modified)");
            }
        }
        for (String fileName : stagingArea.stagedFiles().keySet()) {
            File file = new File(fileName);
            if (file.exists() && fileModified(stagingArea.stagedFiles().get(fileName), fileName)) {
                System.out.println(file + " (modified)");
            }
            if (!file.exists()) {
                System.out.println(file + " (deleted)");
            }
        }
        System.out.println("\n=== Untracked Files ===");
        updateUntrackedFiles();
        for(String unTrackedFile : untrackedFiles) {
            System.out.println(unTrackedFile);
        }
    }

    private boolean fileModified(String id, String fileName) {
        File file = new File(fileName);
        byte[] content = Utils.readContents(file);
        String fileId = Utils.sha1((Object) content);
        return !fileId.equals(id);
    }

    private void updateUntrackedFiles(){
        untrackedFiles.clear();
        File folder = new File(System.getProperty("user.dir"));
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isFile() && !head.blobs().containsKey(file.getName()) && !stagingArea.stagedFiles().containsKey(file.getName())) {
                untrackedFiles.add(file.getName());
            }
        }
    }

    public void checkout(String[] args) {
        if (args.length == 2 && args[0].equals("--")) {
            String fileName = args[1];
            if (!head.blobs().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            writeFile(fileName, head.blobs().get(fileName));
        } else if (args.length == 3 && args[1].equals("--")) {
            CommitNode targetCommit = idToCommits.get(args[0]);
            String fileName = args[2];
            if (!targetCommit.blobs().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            writeFile(fileName, targetCommit.blobs().get(fileName));
        } else if (args.length == 1) {
            String targetBranch = args[0];
            if(!branches.containsKey(targetBranch)) {
                System.out.println("No such branch exists.");
                return;
            }
            if(targetBranch.equals(currentBranchName)) {
                System.out.println("No need to checkout the current branch.");
                return;
            }
            updateUntrackedFiles();
            for (String fileName : untrackedFiles) {
                if(branches.get(targetBranch).blobs().containsKey(fileName)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
            }
            head = branches.get(targetBranch);
            currentBranchName = targetBranch;
            for (String fileName : head.blobs().keySet()) {
                writeFile(fileName, head.blobs().get(fileName));
            }
            stagingArea.clear();
            removedFiles.clear();
        } else {
            System.out.println("Invalid Command.");
        }
    }

    private void writeFile(String fileName, String id) {
        File to = new File(fileName);
        File from = new File(".gitlet/blobs/" + id);
        byte[] content = Utils.readContents(from);
        Utils.writeContents(to, content);
    }

    public void branch(String branch) {
        if (branches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branches.put(branch, head);
    }

    public void rmBranch(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (branch.equals(currentBranchName)) {
            System.out.println("Cannot remove the current branch");
        } else {
            branches.remove(branch);
        }
    }

    public void reset(String commitID) {
        if (!idToCommits.containsKey(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        CommitNode targetCommit = idToCommits.get(commitID);
        updateUntrackedFiles();
        for (String fileName : untrackedFiles) {
            if(targetCommit.blobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        for (String file : targetCommit.blobs().keySet()) {
            String[] input = new String[]{commitID, "--", file};
            checkout(input);
        }
        head = targetCommit;
        branches.replace(currentBranchName, targetCommit);
        stagingArea.clear();
        removedFiles.clear();
        untrackedFiles.clear();
    }

    public void merge(String branch) {
        if (mergeFailureCases(branch)) {
            return;
        }
        CommitNode splitPoint = getSplitPoint(branch);
        if (splitPoint == null) {
            System.out.println("Error");
            return;
        }
        CommitNode branchHead = branches.get(branch);

        for (String file : branchHead.blobs().keySet()) {
            if(!head.blobs().containsKey(file) && !splitPoint.blobs().containsKey(file)) {
                String[] input = new String[]{branchHead.ID(), "--", file};
                checkout(input);
                stagingArea.add(file);
            }
            if(!splitPoint.blobs().containsKey(file) && head.blobs().containsKey(file)
                    && !head.blobs().get(file).equals(branchHead.blobs().get(file))) {
                handleConflict(file, branch);
            }
        }

        for (String file : splitPoint.blobs().keySet()) {
            String splitPointBlobId = splitPoint.blobs().get(file);
            String branchHeadBlobId = branchHead.blobs().getOrDefault(file, "");
            String headBlobId = head.blobs().getOrDefault(file, "");
            if (!branchHeadBlobId.equals("") && !splitPointBlobId.equals(branchHeadBlobId)
                    && splitPointBlobId.equals(headBlobId)) {
                String[] input = new String[]{branchHead.ID(), "--", file};
                checkout(input);
                stagingArea.add(file);
            }
            if (splitPointBlobId.equals(headBlobId) && branchHeadBlobId.equals("")) {
                rm(file);
            }
            if(!splitPointBlobId.equals(headBlobId) && !splitPointBlobId.equals(branchHeadBlobId)
                    && !headBlobId.equals(branchHeadBlobId)) {
                handleConflict(file, branch);
            }
        }
        commit("Merged " + branch + " into " + currentBranchName + " .");
        head.setSecondParent(branchHead.ID());
    }

    private boolean mergeFailureCases(String givenBranchName) {
        if (!stagingArea.stagedFiles().isEmpty() || !removedFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!branches.containsKey(givenBranchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (currentBranchName.equals(givenBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        updateUntrackedFiles();
        for (String fileName : untrackedFiles) {
            if(branches.get(givenBranchName).blobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    private void handleConflict(String fileName, String branch) {
        System.out.println("Encountered a merge conflict : " + fileName);
        CommitNode node = branches.get(branch);
        String branchFileId = node.blobs().get(fileName);
        String headFileId = head.blobs().get(fileName);
        File file = new File(fileName);
        File headFile = new File(".gitlet/blobs/" + headFileId);
        File branchFile = new File(".gitlet/blobs/" + branchFileId);
        ByteArrayOutputStream newContent = new ByteArrayOutputStream();
        byte[] contentOfHeadFile = headFileId == null ? "".getBytes() : Utils.readContents(headFile);
        byte[] contentOfBranchFile = branchFileId == null ? "".getBytes() : Utils.readContents(branchFile);
        try {
            newContent.write("<<<<<<< HEAD\n".getBytes());
            if (headFileId != null) {
                newContent.write(contentOfHeadFile);
            }
            newContent.write("\n=======\n".getBytes());
            if (branchFileId != null) {
                newContent.write(contentOfBranchFile);
            }
            newContent.write("\n>>>>>>>\n".getBytes());
        } catch (IOException e) {
            return;
        }
        byte[] contentOfRewrite = newContent.toByteArray();
        Utils.writeContents(file, contentOfRewrite);
        stagingArea.add(fileName);
    }

    private CommitNode getSplitPoint(String branch) {
        Queue<CommitNode> queue = new LinkedList<>();
        queue.offer(head);
        while (!queue.isEmpty()) {
            CommitNode node = queue.poll();
            if (isAncestor(node, branches.get(branch))) {
                return node;
            }
            CommitNode firstParent = idToCommits.get(node.firstParent());
            CommitNode secondParent = idToCommits.get(node.secondParent());
            if(firstParent != null) {
                queue.offer(firstParent);
            }
            if(secondParent != null) {
                queue.offer(secondParent);
            }
        }
        return null;
    }

    private boolean isAncestor(CommitNode ancestor, CommitNode son) {
        Queue<CommitNode> queue = new LinkedList<>();
        queue.offer(son);
        while (!queue.isEmpty()) {
            CommitNode node = queue.poll();
            if(node == ancestor) {
                return true;
            }
            CommitNode firstParent = idToCommits.get(node.firstParent());
            CommitNode secondParent = idToCommits.get(node.secondParent());
            if(firstParent != null) {
                queue.offer(firstParent);
            }
            if(secondParent != null) {
                queue.offer(secondParent);
            }
        }
        return false;
    }
}
