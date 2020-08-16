package nl.plaatsoft.warquest3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Base64;

// Fetch image async task
public class FetchImageTask extends AsyncTask<Void, Void, Bitmap> {
    private final Context context;
    private final ImageView imageView;
    private final String url;

    private FetchImageTask(Context context, ImageView imageView, String url) {
        this.context = context;
        this.imageView = imageView;
        this.url = url;
        imageView.setTag(url);
        imageView.setImageBitmap(null);
    }

    public static void fetchImage(Context context, ImageView imageView, String url) {
        String imageTagUrl = imageView.getTag() != null ? (String)imageView.getTag() : "";
        if (!imageTagUrl.equals(url)) {
            FetchImageTask fetchImageTask = new FetchImageTask(context, imageView, url);
            fetchImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public Bitmap doInBackground(Void... voids) {
        try {
            File file = new File(context.getCacheDir(), new String(Base64.getUrlEncoder().encode(url.getBytes())));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getPath(), options);
            } else {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new URL(url).openStream());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int number_read = 0;
                while ((number_read = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, number_read);
                }
                byteArrayOutputStream.close();
                bufferedInputStream.close();

                byte[] image = byteArrayOutputStream.toByteArray();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(image);
                fileOutputStream.close();

                return BitmapFactory.decodeByteArray(image, 0, image.length, options);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void onPostExecute(Bitmap image) {
        if (!isCancelled() && ((String)imageView.getTag()).equals(url)) {
            imageView.setImageBitmap(image);
        }
    }
}
