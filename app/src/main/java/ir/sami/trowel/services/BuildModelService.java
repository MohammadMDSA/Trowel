package ir.sami.trowel.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import ir.sami.trowel.data.ModelBuildConfig;

public class BuildModelService extends Service {

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private String projectName;
    private ModelBuildConfig config;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        private BuildModelTask task;


        public ServiceHandler(Looper looper) {
            super(looper);
            this.task = new BuildModelTask(config, BuildModelService.this);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg2 == 1) {
                task.execute("");
                return;
            }
            if (msg.arg2 == -1) {
                this.task.cancel(true);
                stopSelf();
            }
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_AUDIO);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Building model...", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.arg2 = 1;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Message msg = serviceHandler.obtainMessage();
        msg.arg2 = -1;
        serviceHandler.sendMessage(msg);
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
