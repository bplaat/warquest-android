package nl.plaatsoft.warquest3;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import java.util.Locale;
import java.security.MessageDigest;

// Utils class
public class Utils {
    private Utils() {}

    // MD5 hash a string (returns hash string like the PHP function)
    public static String md5(String data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data.getBytes());
            byte[] bytes = messageDigest.digest();
            String hash = "";
            for (int i = 0; i < bytes.length; i++) {
                hash += String.format("%02x", bytes[i]);
            }
            return hash;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // Get the current locale of the device
    @SuppressWarnings("deprecation")
    public static Locale getCurrentLocale(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            // Suppress deprecation warning for this line:
            return context.getResources().getConfiguration().locale;
        }
    }

    // Convert a dp amount to pixels
    public static float convertDpToPixel(Context context, float dp){
        return dp * ((float)context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
