package ir.sami.trowel;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import ir.sami.trowel.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'trowel' library on application startup.
    static {
        try {
//            System.loadLibrary("trowel");
            System.loadLibrary("openMVG");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
//        tv.setText("foo");
        tv.setText(stringFromJNI(1));
    }

    /**
     * A native method that is implemented by the 'trowel' native library,
     * which is packaged with this application.
     * @param i
     */
    public native String stringFromJNI(int num);
}