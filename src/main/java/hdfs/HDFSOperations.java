package hdfs;

import org.apache.hadoop.fs.*;
import java.io.*;
import java.util.Date;
import java.util.Scanner;

public class HDFSOperations {
    private static FileSystem fs;
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        fs = HDFSConfig.getFileSystem();
        while (true) {
            printMenu();
            int choice = readInt();
            try {
                switch (choice) {
                    case 1: uploadFile(); break;
                    case 2: downloadFile(); break;
                    case 3: catFile(); break;
                    case 4: fileInfo(); break;
                    case 5: listRecursive(); break;
                    case 6: createOrDeleteFile(); break;
                    case 7: createOrDeleteDir(); break;
                    case 8: appendToFile(); break;
                    case 9: deleteFile(); break;
                    case 10: moveFile(); break;
                    case 0: fs.close(); System.out.println("退出"); return;
                    default: System.out.println("无效选择");
                }
            } catch (Exception e) {
                System.out.println("操作失败: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n===== HDFS 操作菜单 =====");
        System.out.println("1. 上传文件（存在时选择追加/覆盖）");
        System.out.println("2. 下载文件（本地同名自动重命名）");
        System.out.println("3. 输出文件内容到终端");
        System.out.println("4. 显示文件信息");
        System.out.println("5. 递归列出目录下所有文件信息");
        System.out.println("6. 创建/删除 HDFS 文件");
        System.out.println("7. 创建/删除 HDFS 目录");
        System.out.println("8. 向文件追加内容（开头/结尾）");
        System.out.println("9. 删除 HDFS 文件");
        System.out.println("10. 移动文件");
        System.out.println("0. 退出");
        System.out.print("请输入选项: ");
    }

    private static int readInt() {
        while (!sc.hasNextInt()) { sc.next(); }
        int n = sc.nextInt();
        sc.nextLine();
        return n;
    }

    private static String readString(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static void uploadFile() throws IOException {
        String local = readString("本地文件路径: ");
        String hdfs = readString("HDFS 目标路径: ");
        Path dst = new Path(hdfs);
        if (fs.exists(dst)) {
            System.out.print("文件已存在，选择操作 (append/overwrite): ");
            String choice = sc.nextLine().trim();
            if ("overwrite".equalsIgnoreCase(choice)) {
                fs.delete(dst, false);
                fs.copyFromLocalFile(new Path(local), dst);
                System.out.println("已覆盖上传");
            } else if ("append".equalsIgnoreCase(choice)) {
                FSDataOutputStream out = fs.append(dst);
                FileInputStream in = new FileInputStream(local);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                System.out.println("已追加到末尾");
            } else {
                System.out.println("无效选择");
            }
        } else {
            fs.copyFromLocalFile(new Path(local), dst);
            System.out.println("上传成功");
        }
    }

    private static void downloadFile() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        String local = readString("本地目标路径: ");
        Path src = new Path(hdfs);
        Path dst = new Path(local);
        if (!fs.exists(src)) {
            System.out.println("HDFS 文件不存在");
            return;
        }
        if (new File(local).exists()) {
            dst = new Path(local + "_" + System.currentTimeMillis());
            System.out.println("本地文件已存在，重命名为: " + dst);
        }
        fs.copyToLocalFile(src, dst);
        System.out.println("下载成功");
    }

    private static void catFile() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        Path path = new Path(hdfs);
        if (!fs.exists(path)) {
            System.out.println("文件不存在");
            return;
        }
        FSDataInputStream in = fs.open(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
    }

    private static void fileInfo() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        Path path = new Path(hdfs);
        if (!fs.exists(path)) {
            System.out.println("文件不存在");
            return;
        }
        FileStatus status = fs.getFileStatus(path);
        System.out.println("路径: " + status.getPath());
        System.out.println("权限: " + status.getPermission());
        System.out.println("大小: " + status.getLen() + " bytes");
        System.out.println("修改时间: " + new Date(status.getModificationTime()));
        System.out.println("所有者: " + status.getOwner());
    }

    private static void listRecursive() throws IOException {
        String hdfs = readString("HDFS 目录路径: ");
        Path path = new Path(hdfs);
        if (!fs.exists(path)) {
            System.out.println("目录不存在");
            return;
        }
        RemoteIterator<LocatedFileStatus> it = fs.listFiles(path, true);
        while (it.hasNext()) {
            LocatedFileStatus status = it.next();
            System.out.println(status.getPermission() + "\t" +
                    status.getLen() + "\t" +
                    new Date(status.getModificationTime()) + "\t" +
                    status.getPath());
        }
    }

    private static void createOrDeleteFile() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        Path path = new Path(hdfs);
        System.out.print("操作 (create/delete): ");
        String op = sc.nextLine().trim();
        if ("create".equalsIgnoreCase(op)) {
            if (fs.exists(path)) {
                System.out.println("文件已存在");
                return;
            }
            fs.mkdirs(path.getParent());
            fs.create(path).close();
            System.out.println("文件创建成功");
        } else if ("delete".equalsIgnoreCase(op)) {
            if (!fs.exists(path)) {
                System.out.println("文件不存在");
                return;
            }
            fs.delete(path, false);
            System.out.println("文件删除成功");
        } else {
            System.out.println("无效操作");
        }
    }

    private static void createOrDeleteDir() throws IOException {
        String hdfs = readString("HDFS 目录路径: ");
        Path path = new Path(hdfs);
        System.out.print("操作 (create/delete): ");
        String op = sc.nextLine().trim();
        if ("create".equalsIgnoreCase(op)) {
            fs.mkdirs(path);
            System.out.println("目录创建成功（父目录自动创建）");
        } else if ("delete".equalsIgnoreCase(op)) {
            if (!fs.exists(path)) {
                System.out.println("目录不存在");
                return;
            }
            FileStatus status = fs.getFileStatus(path);
            if (!status.isDirectory()) {
                System.out.println("不是目录");
                return;
            }
            FileStatus[] children = fs.listStatus(path);
            if (children.length == 0) {
                fs.delete(path, false);
                System.out.println("空目录删除成功");
            } else {
                System.out.println("目录非空，不删除");
            }
        } else {
            System.out.println("无效操作");
        }
    }

    private static void appendToFile() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        Path path = new Path(hdfs);
        if (!fs.exists(path)) {
            System.out.println("文件不存在");
            return;
        }
        String content = readString("要追加的内容: ");
        System.out.print("追加位置 (head/tail): ");
        String pos = sc.nextLine().trim();
        if ("tail".equalsIgnoreCase(pos)) {
            FSDataOutputStream out = fs.append(path);
            out.write(content.getBytes("UTF-8"));
            out.close();
            System.out.println("已追加到结尾");
        } else if ("head".equalsIgnoreCase(pos)) {
            FSDataInputStream in = fs.open(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            in.close();
            String original = baos.toString("UTF-8");
            fs.delete(path, false);
            FSDataOutputStream out = fs.create(path);
            out.write((content + original).getBytes("UTF-8"));
            out.close();
            System.out.println("已追加到开头");
        } else {
            System.out.println("无效位置");
        }
    }

    private static void deleteFile() throws IOException {
        String hdfs = readString("HDFS 文件路径: ");
        Path path = new Path(hdfs);
        if (!fs.exists(path)) {
            System.out.println("文件不存在");
            return;
        }
        if (fs.getFileStatus(path).isDirectory()) {
            System.out.println("这是目录，不是文件");
            return;
        }
        fs.delete(path, false);
        System.out.println("文件删除成功");
    }

    private static void moveFile() throws IOException {
        String src = readString("源路径: ");
        String dst = readString("目标路径: ");
        Path srcPath = new Path(src);
        Path dstPath = new Path(dst);
        if (!fs.exists(srcPath)) {
            System.out.println("源文件不存在");
            return;
        }
        if (dstPath.getParent() != null) {
            fs.mkdirs(dstPath.getParent());
        }
        fs.rename(srcPath, dstPath);
        System.out.println("移动成功");
    }
}