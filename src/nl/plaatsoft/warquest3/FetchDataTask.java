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

    private final Context context;
    private final String url;
    private final boolean loadFomCache;
    private final boolean saveToCache;
    private final OnLoadListener onLoadListener;
    private boolean finished;
    private boolean canceled;

    public FetchDataTask(Context context, String url, boolean loadFomCache, boolean saveToCache, OnLoadListener onLoadListener) {
        this.context = context;
        this.url = url;
        this.loadFomCache = loadFomCache;
        this.saveToCache = saveToCache;
        this.onLoadListener = onLoadListener;
        finished = false;
        canceled = false;

        executor.execute(() -> {
            String data = fetchData();
            if (!canceled) {
                handler.post(() -> {
                    finished = true;
                    if (onLoadListener != null) {
                        onLoadListener.onLoad(data);
                    }
                });
            }
        });
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

    private String fetchData() {
        try {
            File file = new File(context.getCacheDir(), Utils.md5(url));
            if (loadFomCache && file.exists()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.lineSeparator());
                }
                bufferedReader.close();
                return stringBuilder.toString();
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            bufferedReader.close();

            String data = stringBuilder.toString();
            if (saveToCache) {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(data);
                fileWriter.close();
            }
            return data;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
