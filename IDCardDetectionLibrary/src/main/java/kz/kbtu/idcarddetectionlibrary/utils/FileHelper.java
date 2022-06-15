package kz.kbtu.idcarddetectionlibrary.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileHelper {

    public static String writeImageFileToDisk(Context context, byte[] data) {
        try {
            File file = createImageFile(context);
            if (file == null) return null;
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                outputStream.write(data);
                outputStream.flush();
                return file.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File writeImageFileToDiskV2(Context context, byte[] data) {
        try {
            File file = createImageFile(context);
            if (file == null) return null;
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                outputStream.write(data);
                outputStream.flush();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static File createImageFile(Context context) {
        return createFile(context, "jpg", true);
    }

    public static File createFile(Context context, String extension, boolean isCache) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = extension.toUpperCase() + "_" + timeStamp + "_";
            File storageDir = isCache ? context.getCacheDir() : context.getDir("wisdom", Context.MODE_PRIVATE);
            return File.createTempFile(imageFileName, "." + extension.toLowerCase(), storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
