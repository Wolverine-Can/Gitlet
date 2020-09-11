package gitlet;
import java.io.*;

public class Main {
    private static CommitTree commitTree;

    private static void serializeCommitTree() {
        File commitTreeFile = new File(".gitlet/commitTree");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(commitTreeFile));
            out.writeObject(commitTree);
            out.close();
        } catch (IOException e) {
            System.out.println("error.");
        }
    }

    private static boolean commandIsNotValid(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            return true;
        }
        return false;
    }

    private static void deSerializeCommitTree() {
        File commitTreeFile = new File(".gitlet/commitTree");
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(commitTreeFile));
            commitTree = (CommitTree) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            System.out.println("error.");
            commitTree = null;
        }
    }

    private static String[] getOperands(String[] args) {
        String[] copy = new String[args.length - 1];
        System.arraycopy(args, 1, copy, 0, copy.length);
        return copy;
    }

    private static boolean gitletInitialized() {
        File checkDir = new File(System.getProperty("user.dir") + "/.gitlet");
        return checkDir.exists();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (args.length == 1 && args[0].equals("init")) {
            if (gitletInitialized()) {
                System.out.println("A Gitlet version-control system already exists in the current directory.");
                return;
            }
            commitTree = new CommitTree();
            File directory = new File(".gitlet");
            directory.mkdir();
            serializeCommitTree();
        } else {
            if (!gitletInitialized()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
            deSerializeCommitTree();
            if (commitTree == null) {
                System.out.println("CommitTree not found");
                return;
            }
            switch (args[0]) {
                case "add":
                    addCommand(args);
                    break;
                case "commit":
                    commitCommand(args);
                    break;
                case "rm":
                    rmCommand(args);
                    break;
                case "log":
                    logCommand(args);
                    break;
                case "global-log":
                    globalLogCommand(args);
                    break;
                case "find":
                    findCommand(args);
                    break;
                case "status":
                    statusCommand(args);
                    break;
                case "checkout":
                    checkoutCommand(args);
                    break;
                case "branch":
                    branchCommand(args);
                    break;
                case "rm-branch":
                    rmBranchCommand(args);
                    break;
                case "reset":
                    resetCommand(args);
                    break;
                case "merge":
                    mergeCommand(args);
                    break;
                default:
                    System.out.println("No command with that name exists.");
            }
        }
    }

    private static void addCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.add(args[1]);
        serializeCommitTree();
    }

    private static void commitCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.commit(args[1]);
        serializeCommitTree();
    }

    private static void rmCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        String[] files = getOperands(args);
        for (String file : files) {
            commitTree.rm(file);
        }
        serializeCommitTree();
    }

    private static void logCommand(String[] args) {
        if (commandIsNotValid(args, 1)) {
            return;
        }
        commitTree.log();
    }

    private static void globalLogCommand(String[] args) {
        if (commandIsNotValid(args, 1)) {
            return;
        }
        commitTree.globalLog();
    }

    private static void findCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.find(args[1]);
    }

    private static void statusCommand(String[] args) {
        if (commandIsNotValid(args, 1)) {
            return;
        }
        commitTree.status();
    }

    private static void checkoutCommand(String[] args) {
        if (args.length > 4) {
            System.out.println("Incorrect operands.");
            return;
        }
        commitTree.checkout(getOperands(args));
        serializeCommitTree();
    }

    private static void branchCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.branch(args[1]);
        serializeCommitTree();
    }

    private static void rmBranchCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.rmBranch(args[1]);
        serializeCommitTree();
    }

    private static void resetCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.reset(args[1]);
        serializeCommitTree();
    }

    private static void mergeCommand(String[] args) {
        if (commandIsNotValid(args, 2)) {
            return;
        }
        commitTree.merge(args[1]);
        serializeCommitTree();
    }
 }
