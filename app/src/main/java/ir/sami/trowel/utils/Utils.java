package ir.sami.trowel.utils;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Pair;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
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

    public static Pair<Integer, Integer> resize(String inputImagePath, String outputImagePath, int maxSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(inputImagePath, options);
        Bitmap result = resize(bitmap, maxSize, false);
        File outFile = new File(outputImagePath);
        try {
            FileOutputStream out = new FileOutputStream(outputImagePath);
            result.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ExifInterface target = new ExifInterface(outFile);
            target.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, result.getWidth() + "");
            target.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, result.getHeight() + "");
            target.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Pair<>(result.getWidth(), result.getHeight());
    }


    public static Bitmap resize(Bitmap realImage, float maxImageSize, boolean filter) {
        int width = realImage.getWidth();
        int height = realImage.getHeight();
        if (maxImageSize < height || maxImageSize < width) {
            float ratio = Math.min(
                    (float) maxImageSize / realImage.getWidth(),
                    (float) maxImageSize / realImage.getHeight());
            width = Math.round((float) ratio * realImage.getWidth());
            height = Math.round((float) ratio * realImage.getHeight());
        }
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    public static float getFocalLengthInPixels(CameraCharacteristics cameraCharacteristics, int maxWidthHeight) {
        float sensorWidth = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth();
        float focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
        return maxWidthHeight * focalLength / sensorWidth;
    }

    public static File getTrowelRoot() {
        return new File(Environment.getExternalStorageDirectory(), "Trowel");
    }

    public static File getProjectRoot(String project) {
        return new File(getTrowelRoot(), project);
    }

    public static void deleteDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
            file.delete();
        }
    }

    public static void ensureAssetInData(Context context, String file) throws IOException {
        File assetFile = new File(context.getDataDir(), file);
        if(!assetFile.exists()){
            assetFile.createNewFile();
            copyStreamToFile(context.getAssets().open(file), assetFile);
        }
    }
}
