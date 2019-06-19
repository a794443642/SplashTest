package android.wcj.com.helloworld;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {
    private final String url="http://10.134.141.214:8080/online/111.jpg";
    private Handler handler;
    private Timer timer;
    private Runnable runnable;
    private final int SPALSH_DISPLAY_TIME=3000;
    public static final int GET_IMAGE_SUCCESS=1;
    public static final int NET_ERROR=2;
    public static final int SERVER_ERROR=3;
    private ImageView image;
    private int time =3;
    //主线程用来更新ui
    private static Handler uihandler;
    public SplashActivity() {
        uihandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case GET_IMAGE_SUCCESS:
                        Bitmap bitmap=(Bitmap)msg.obj;
                        image=findViewById(R.id.image);
                        image.setImageBitmap(bitmap);
                        break;
                    case NET_ERROR:
                        Toast.makeText(getApplicationContext(),"网络连接失败",Toast.LENGTH_SHORT).show();
                    case SPALSH_DISPLAY_TIME:
                        Toast.makeText(getApplicationContext(),"服务器错误",Toast.LENGTH_SHORT).show();
                    case 0:
                        Button button=findViewById(R.id.quickbutton);
                        button.setText("点击跳过(" + time + ")");
                }
            }
        };
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏andorid标题栏和系统栏（全屏）
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setImageURL(url);
        setContentView(R.layout.activity_splash);
        //使用handler线程延迟跳转
        handler =new Handler();
        Button button =findViewById(R.id.quickbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SplashActivity.this,MainActivity.class);
                startActivity(intent);
                SplashActivity.this.finish();
                timer.cancel();
            }
        });
        handler.postDelayed(runnable =new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(SplashActivity.this,MainActivity.class);
                startActivity(intent);
                SplashActivity.this.finish();
                timer.cancel();
            }
        },SPALSH_DISPLAY_TIME);
        //使用timer进程做计时器显示
          timer =new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                time--;
                uihandler.sendEmptyMessage((time == 4 ? 0: 0));
            }
        },1000,1000);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void setImageURL(final String path){
        //网络操作需要在子进程进行
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    URL url =new URL(path);
                    HttpURLConnection connection =(HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setUseCaches(false);
                    connection.connect();
                    int code=connection.getResponseCode();
                    Log.d("SplashActivity", "code"+code);
                    if(code==200){
                        InputStream inputStream=connection.getInputStream();
                        Bitmap bitmap =BitmapFactory.decodeStream(inputStream);
                        Message message=Message.obtain();
                        message.obj=bitmap;
                        message.what=GET_IMAGE_SUCCESS;
                        uihandler.sendMessage(message);
                        inputStream.close();
                    }else{
                        uihandler.sendEmptyMessage(SERVER_ERROR);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    uihandler.sendEmptyMessage(NET_ERROR);
                }
            }
        }.start();
    }
//销毁线程
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        handler.removeCallbacks(runnable);
    }
}
