package nl.plaatsoft.warquest3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.os.Handler;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FetchImageTask {
    private static final Executor executor = Executors.newFixedThreadPool(8);
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private final Context context;
    private final ImageView imageView;
    private final String url;
    private final boolean loadFomCache;
    private final boolean saveToCache;
    private boolean finished;
    private boolean canceled;

    public FetchImageTask(Context context, ImageView imageView, String url, boolean loadFomCache, boolean saveToCache) {
        this.context = context;
        this.imageView = imageView;
        this.url = url;
        this.loadFomCache = loadFomCache;
        this.saveToCache = saveToCache;
        finished = false;
        canceled = false;

        if (!(imageView.getTag() != null ? (String)imageView.getTag() : "").equals(url)) {
            imageView.setTag(url);
            imageView.setImageBitmap(null);

            executor.execute(() -> {
                Bitmap image = fetchImage();
                if (!canceled) {
                    handler.post(() -> {
                        finished = true;
                        if ((imageView.getTag() != null ? (String)imageView.getTag() : "").equals(url)) {
                            imageView.setImageBitmap(image);
                        }
                    });
                }
            });
        } else {
            finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        canceled = true;
    }

    private Bitmap fetchImage() {
        try {
            File file = new File(context.getCacheDir(), Utils.md5(url));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            if (loadFomCache && file.exists()) {
                return BitmapFactory.decodeFile(file.getPath(), options);
            }

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

            if (saveToCache) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(image);
                fileOutputStream.close();
            }

            return BitmapFactory.decodeByteArray(image, 0, image.length, options);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
