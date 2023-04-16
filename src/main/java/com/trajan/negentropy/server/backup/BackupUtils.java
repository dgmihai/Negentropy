package com.trajan.negentropy.server.backup;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BackupUtils {
    public static String getBackupFileName(String backupPath, Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = String.format("backup-%s.sql", dateFormat.format(date));
        return Paths.get(backupPath, fileName).toString();
    }

    public static List<File> getBackupFileList(String backupPath) {
        File[] files = new File(backupPath).listFiles((dir, name) -> name.matches("^backup-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.sql$"));
        return Arrays.stream(files).sorted(Comparator.comparing(File::lastModified).reversed()).collect(Collectors.toList());
    }

    public static void deleteBackupFiles(String backupPath, int maxBackups) {
        List<File> backupFileList = getBackupFileList(backupPath);
        if (backupFileList.size() > maxBackups) {
            for (int i = 0; i < backupFileList.size() - maxBackups; i++) {
                backupFileList.get(i).delete();
            }
        }
    }
}

