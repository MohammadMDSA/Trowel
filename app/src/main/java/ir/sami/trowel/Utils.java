package ir.sami.trowel;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

    public static char[][] convertArgs(String[] argv) {
        char[][] result = new char[argv.length][];
        for (int i = 0; i < argv.length; i++) {
            result[i] = argv[i].toCharArray();
        }
        return result;
    }

    public static void copyStreamToFile(InputStream in, File outFile) throws IOException {
        OutputStream out = new FileOutputStream(outFile);
        copyFile(in, out);
        in.close();
        out.flush();
        out.close();

    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

    }

}
