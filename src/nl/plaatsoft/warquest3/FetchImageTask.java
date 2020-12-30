package nl.plaatsoft.warquest3;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
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

    public static interface OnLoadListener {
        public abstract void onLoad(Bitmap image);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private final Context context;

    private String url;
    private boolean isTransparent = false;
    private boolean isFadedIn = false;
    private boolean isLoadedFomCache = true;
    private boolean isSavedToCache = true;
    private OnLoadListener onLoadListener = null;
    private OnErrorListener onErrorListener = null;
    private ImageView imageView;

    private boolean isFinished = false;
    private boolean isCanceled = false;

    private FetchImageTask(Context context) {
        this.context = context;
    }

    public static FetchImageTask with(Context context) {
        return new FetchImageTask(context);
    }

    public FetchImageTask load(String url) {
        this.url = url;
        return this;
    }

    public FetchImageTask transparent() {
        this.isTransparent = true;
        return this;
    }

    public FetchImageTask fadeIn() {
        this.isFadedIn = true;
        return this;
    }

    public FetchImageTask noCache() {
        this.isLoadedFomCache = false;
        this.isSavedToCache = false;
        return this;
    }

    public FetchImageTask notFromCache() {
        this.isLoadedFomCache = false;
        return this;
    }

    public FetchImageTask notToCache() {
        this.isSavedToCache = false;
        return this;
    }

    public FetchImageTask then(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public FetchImageTask then(OnLoadListener onLoadListener, OnErrorListener onErrorListener) {
        this.onLoadListener = onLoadListener;
        this.onErrorListener = onErrorListener;
        return this;
    }

    public FetchImageTask into(ImageView imageView) {
        this.imageView = imageView;
        return fetch();
    }

    public FetchImageTask fetch() {
        if (imageView != null) {
            FetchImageTask previousFetchImageTask = (FetchImageTask)imageView.getTag();
            if (previousFetchImageTask != null) {
                if (url != previousFetchImageTask.getUrl()) {
                    if (!previousFetchImageTask.isFinished()) {
                        previousFetchImageTask.cancel();
                    }

                    imageView.setTag(this);
                    imageView.setImageBitmap(null);
                } else {
                    cancel();
                    return this;
                }
            }
        }

        long startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {
                Bitmap image = fetchImage();
                handler.post(() -> {
                    finish();
                    if (!isCanceled) {
                        if (imageView != null) {
                            imageView.setBackgroundColor(0);

                            boolean isWaitingLong = (System.currentTimeMillis() - startTime) > Config.ANIMATION_IMAGE_LOADING_TIMEOUT;
                            if (isFadedIn && isWaitingLong) {
                                imageView.setImageAlpha(0);
                            }

                            imageView.setImageBitmap(image);

                            if (isFadedIn && isWaitingLong) {
                                ValueAnimator animation = ValueAnimator.ofInt(0, 255);
                                animation.setDuration(Config.ANIMATION_DURATION);
                                animation.setInterpolator(new AccelerateDecelerateInterpolator());
                                animation.addUpdateListener(animator -> {
                                    imageView.setImageAlpha((int)animator.getAnimatedValue());
                                });
                                animation.start();
                            }
                        }

                        if (onLoadListener != null) {
                            onLoadListener.onLoad(image);
                        }
                    }
                });
            } catch (Exception exception) {
                handler.post(() -> {
                    finish();
                    if (!isCanceled) {
                        if (onErrorListener != null) {
                            onErrorListener.onError(exception);
                        } else {
                            exception.printStackTrace();
                        }
                    }
                });
            }
        });

        return this;
    }

    public String getUrl() {
        return url;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel() {
        isCanceled = true;
        finish();
    }

    private void finish() {
        isFinished = true;
        if (imageView != null) {
            imageView.setTag(null);
        }
    }

    private Bitmap fetchImage() throws Exception {
        // Check if the file exists in the cache
        File file = new File(context.getCacheDir(), Utils.md5(url));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = isTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        if (isLoadedFomCache && file.exists()) {
            return BitmapFactory.decodeFile(file.getPath(), options);
        }

        // Or fetch the image from the internet in to a byte array buffer
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

        // When needed save the image to a cache file
        if (isSavedToCache) {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(image);
            fileOutputStream.close();
        }

        return BitmapFactory.decodeByteArray(image, 0, image.length, options);
    }
}
