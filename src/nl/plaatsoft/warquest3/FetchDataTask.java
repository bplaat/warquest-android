package nl.plaatsoft.warquest3;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FetchDataTask {
    private static final Executor executor = Executors.newFixedThreadPool(8);
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static interface OnLoadListener {
        public abstract void onLoad(String data);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private final Context context;

    private String url;
    private boolean isLoadedFomCache = false;
    private boolean isSavedToCache = false;
    private OnLoadListener onLoadListener = null;
    private OnErrorListener onErrorListener = null;

    private boolean isCanceled = false;
    private boolean isFinished = false;

    private FetchDataTask(Context context) {
        this.context = context;
    }

    public static FetchDataTask with(Context context) {
        return new FetchDataTask(context);
    }

    public FetchDataTask load(String url) {
        this.url = url;
        return this;
    }

    public FetchDataTask withCache() {
        this.isLoadedFomCache = true;
        this.isSavedToCache = true;
        return this;
    }

    public FetchDataTask fromCache() {
        this.isLoadedFomCache = true;
        return this;
    }

    public FetchDataTask toCache() {
        this.isSavedToCache = true;
        return this;
    }

    public FetchDataTask then(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return fetch();
    }

    public FetchDataTask then(OnLoadListener onLoadListener, OnErrorListener onErrorListener) {
        this.onLoadListener = onLoadListener;
        this.onErrorListener = onErrorListener;
        return fetch();
    }

    public FetchDataTask fetch() {
        executor.execute(() -> {
            try {
                String data = fetchData();
                handler.post(() -> {
                    isFinished = true;
                    if (!isCanceled && onLoadListener != null) {
                        onLoadListener.onLoad(data);
                    }
                });
            } catch (Exception exception) {
                handler.post(() -> {
                    isFinished = true;
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

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void cancel() {
        isCanceled = true;
        isFinished = true;
    }

    private String fetchData() throws Exception {
        // Check if the url is already cached
        File file = new File(context.getCacheDir(), Utils.md5(url));
        if (isLoadedFomCache && file.exists()) {
            // Then read the cached file
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
            bufferedReader.close();
            return stringBuilder.toString();
        }

        // Or fetch the data from the internet
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append(System.lineSeparator());
        }
        bufferedReader.close();

        // And write to a cache file when needed
        String data = stringBuilder.toString();
        if (isSavedToCache) {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();
        }
        return data;
    }
}
