package hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import java.net.URI;

public class HDFSConfig {
    public static FileSystem getFileSystem() throws Exception {
        Configuration conf = new Configuration();
        // 根据你 core-site.xml 中的端口配置，通常是 9000
        String hdfsUri = "hdfs://localhost:9000";
        conf.set("fs.defaultFS", hdfsUri);
        // 指定用户为 hadoop，避免权限问题
        return FileSystem.get(URI.create(hdfsUri), conf, "hadoop");
    }
}