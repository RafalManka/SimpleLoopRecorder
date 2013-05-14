
 
import android.util.Log;
 
public class AppLog {
        private static final String TAG = "AudioRecorder";
         
        public static int logString(String message){
                return Log.d(TAG,message);
        }
}