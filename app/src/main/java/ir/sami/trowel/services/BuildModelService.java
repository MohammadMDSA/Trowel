package ir.sami.trowel.services;

import static ir.sami.trowel.Constants.BUILD_NOTIFICATION_CHANNEL_ID;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Random;

import ir.sami.trowel.Constants;
import ir.sami.trowel.R;
import ir.sami.trowel.data.ModelBuildConfig;
import ir.sami.trowel.project_detail.ProjectDetailActivity;

public class BuildModelService extends Service {

    private boolean running;
    private BuildModelTask task;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (running)
            Toast.makeText(this, "A model is already being processed", Toast.LENGTH_SHORT).show();
        running = true;
        String projectName = intent.getStringExtra(Constants.PROJECT_NAME_REFERENCE);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, ProjectDetailActivity.class);
        notificationIntent.putExtra(Constants.PROJECT_NAME_REFERENCE, projectName);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, BUILD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Building Model")
                .setContentText(projectName)
                .setSmallIcon(R.drawable.trowel_svgrepo_com)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(Constants.BUILD_NOTIFICATION_ID, notification);
        //do heavy work on a background thread
//        stopSelf();
        ModelBuildConfig config = getConfig();
        config.setProjectName(projectName);
        task = new BuildModelTask(config, this);
        task.execute("");
        return START_NOT_STICKY;
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        running = false;
    }

    private ModelBuildConfig getConfig() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        ModelBuildConfig config = new ModelBuildConfig();
        config.setFeatureDescriberMethod(preference.getString("feature_method", "SIFT"));
        config.setFeatureDescriberPreset(preference.getString("feature_preset", "NORMAL"));
        config.setComputeFeatureUpRight(preference.getBoolean("upright", false));
        config.setMatchGeometricModel(preference.getString("geom_model", "e"));
        config.setMaxImageDimension(preference.getInt("image_max_dim", 1000));
        config.setMatchRatio(preference.getString("match_ratio", "0.8f"));
        config.setMatchMethod(preference.getString("Match_method", "AUTO"));
        ModelBuildConfig.ReconstructionRefinement reconstructionRefinement = new ModelBuildConfig.ReconstructionRefinement();
        reconstructionRefinement.setDistortion(preference.getBoolean("refine_distortion", true));
        reconstructionRefinement.setPrincipalPoint(preference.getBoolean("refine_principal_point", true));
        reconstructionRefinement.setFocalLength(preference.getBoolean("refine_focal_length", true));
        config.setReconstructionRefinement(reconstructionRefinement);
        config.setFixPoseBundleAdjustment(true);
        return config;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    BUILD_NOTIFICATION_CHANNEL_ID,
                    "Model Building",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
