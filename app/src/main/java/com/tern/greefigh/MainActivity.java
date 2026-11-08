package com.tern.greefigh;
import android.graphics.drawable.GradientDrawable;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.*;
import android.content.pm.ActivityInfo;
import android.view.*;
import android.view.WindowManager;
import android.widget.*;
import android.opengl.GLSurfaceView;

public class MainActivity extends Activity {
    private FrameLayout root, overlay;
    private GLSurfaceView gl;
    private WorldRenderer renderer;
    private final Handler handler = new Handler();
    private Runnable loadingTask;
    private int screen = 0;
    private float controlScale = 1f;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        root = new FrameLayout(this);
        gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        renderer = new WorldRenderer();
        gl.setRenderer(renderer);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(gl, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        showLoading(1);
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(13); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setIncludeFontPadding(false);
        b.setPadding(dp(8), 0, dp(8), 0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setBackground(bg(Color.rgb(35,35,35), 14));
        return b;
    }
    private void resetOverlay() {
        if (loadingTask != null) handler.removeCallbacks(loadingTask);
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }
    private void showLoading(final int destination) {
        screen = 0; resetOverlay();
        overlay.setBackgroundColor(Color.rgb(245,245,242));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1); box.setGravity(Gravity.CENTER);
        TextView title = label("TERNGREEFigh", 30, Color.BLACK);
        TextView progress = label("LOADING 0%", 16, Color.DKGRAY);
        TextView tip = label("Building the world...", 13, Color.GRAY);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        box.addView(title, new LinearLayout.LayoutParams(-1, dp(65)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(300), dp(10));
        bp.setMargins(0, dp(10), 0, dp(8)); box.addView(bar, bp);
        box.addView(progress, new LinearLayout.LayoutParams(-1, dp(38)));
        box.addView(tip, new LinearLayout.LayoutParams(-1, dp(32)));
        overlay.addView(box, new FrameLayout.LayoutParams(-1, -1));
        final long start = System.currentTimeMillis();
        loadingTask = new Runnable() {
            @Override public void run() {
                if (screen != 0) return;
                int p = Math.min(100, (int)((System.currentTimeMillis()-start)/65));
                progress.setText("LOADING " + p + "%"); bar.setProgress(p);
                String[] tips = {"Explore the world at your own pace", "Drag the right side to rotate camera",
                        "Use the left joystick to move", "Try jump, crouch and emotes"};
                tip.setText(tips[(int)((System.currentTimeMillis()-start)/2200)%tips.length]);
                if (p >= 100) { if (destination == 1) showMenu(); else showGame(); }
                else handler.postDelayed(this, 70);
            }
        };
        handler.post(loadingTask);
    }
    private void showMenu() {
        screen = 1; resetOverlay();
        overlay.setBackgroundColor(Color.argb(238,244,244,240));
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(24),dp(18),dp(24),dp(18)); row.setGravity(Gravity.CENTER);
        FrameLayout preview = new FrameLayout(this);
        preview.setBackground(bg(Color.rgb(225,226,222),22));
        preview.addView(label("3D CHARACTER PREVIEW\n\nTERNGREEFigh",22,Color.DKGRAY),
                new FrameLayout.LayoutParams(-1,-1));
        row.addView(preview,new LinearLayout.LayoutParams(0,-1,1.3f));
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(1); menu.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0,-1,1);
        mp.setMargins(dp(18),0,0,0); row.addView(menu,mp);
        menu.addView(label("MAIN MENU",24,Color.BLACK),new LinearLayout.LayoutParams(-1,dp(65)));
        Button play=button("PLAY"), settings=button("SETTINGS"), quit=button("QUIT");
        for (Button b : new Button[]{play,settings,quit}) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(230),dp(52));
            p.setMargins(0,dp(6),0,dp(6)); menu.addView(b,p);
        }
        overlay.addView(row,new FrameLayout.LayoutParams(-1,-1));
        play.setOnClickListener(v->showLoading(2));
        settings.setOnClickListener(v->showSettings());
        quit.setOnClickListener(v->finish());
    }
    private void showSettings() {
        screen=3; resetOverlay(); overlay.setBackgroundColor(Color.rgb(242,242,238));
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(1); panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24),dp(12),dp(24),dp(12));
        panel.addView(label("SETTINGS",24,Color.BLACK),new LinearLayout.LayoutParams(-1,dp(48)));
        panel.addView(label("Camera sensitivity",14,Color.DKGRAY),new LinearLayout.LayoutParams(-1,dp(28)));
        SeekBar sensitivity=new SeekBar(this); sensitivity.setMax(100); sensitivity.setProgress(50);
        panel.addView(sensitivity,new LinearLayout.LayoutParams(dp(360),dp(38)));
        panel.addView(label("Control size",14,Color.DKGRAY),new LinearLayout.LayoutParams(-1,dp(28)));
        SeekBar size=new SeekBar(this); size.setMax(100); size.setProgress(50);
        panel.addView(size,new LinearLayout.LayoutParams(dp(360),dp(38)));
        Button back=button("BACK TO MENU");
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(220),dp(48));
        p.setMargins(0,dp(12),0,0); panel.addView(back,p);
        overlay.addView(panel,new FrameLayout.LayoutParams(-1,-1));
        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int v,boolean u){renderer.setSensitivity(0.35f+v/45f);}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int v,boolean u){controlScale=0.8f+v/250f;}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        back.setOnClickListener(v->showMenu());
    }
    private void addControl(String text,int w,int h,int gravity,int right,int bottom,Runnable action) {
        Button b=button(text);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(w),dp(h),gravity);
        p.setMargins(dp(8),dp(8),dp(right),dp(bottom));
        overlay.addView(b,p); b.setOnClickListener(v->action.run());
    }
    private void showGame() {
        screen=2; resetOverlay(); overlay.setBackgroundColor(Color.TRANSPARENT);
        CameraTouch camera=new CameraTouch();
        overlay.addView(camera,new FrameLayout.LayoutParams(-1,-1));
        Joystick joy=new Joystick();
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(dp((int)(142*controlScale)),
                dp((int)(142*controlScale)),Gravity.BOTTOM|Gravity.LEFT);
        jp.setMargins(dp(22),0,0,dp(20)); overlay.addView(joy,jp);
        addControl("MENU",82,44,Gravity.TOP|Gravity.RIGHT,dp(12),0,()->showLoading(1));
        addControl("JUMP",82,54,Gravity.BOTTOM|Gravity.RIGHT,dp(24),dp(22),()->renderer.jump());
        addControl("CROUCH",82,48,Gravity.BOTTOM|Gravity.RIGHT,dp(116),dp(22),()->renderer.toggleCrouch());
        addControl("HALO",70,44,Gravity.BOTTOM|Gravity.RIGHT,dp(24),dp(88),()->renderer.emote(1));
        addControl("MURUNG",78,44,Gravity.BOTTOM|Gravity.RIGHT,dp(100),dp(88),()->renderer.emote(2));
        addControl("RAGDOLL",84,44,Gravity.BOTTOM|Gravity.RIGHT,dp(190),dp(22),()->renderer.emote(3));
        TextView hint=label("MOVE  •  DRAG TO LOOK",11,Color.WHITE);
        hint.setShadowLayer(3,1,1,Color.BLACK);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(220),dp(28),Gravity.TOP|Gravity.LEFT);
        hp.setMargins(dp(12),dp(10),0,0); overlay.addView(hint,hp);
    }
    private class CameraTouch extends View {
        float lastX,lastY; boolean active;
        CameraTouch(){super(MainActivity.this);setBackgroundColor(Color.TRANSPARENT);}
        @Override public boolean onTouchEvent(MotionEvent e) {
            int a=e.getActionMasked(); float x=e.getX(),y=e.getY();
            if(a==MotionEvent.ACTION_DOWN){lastX=x;lastY=y;active=true;return true;}
            if(a==MotionEvent.ACTION_MOVE&&active){
                float dx=x-lastX,dy=y-lastY;
                if(x>getWidth()*0.36f) renderer.rotateCamera(dx,dy);
                lastX=x;lastY=y;return true;
            }
            if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){active=false;return true;}
            return true;
        }
    }
    private class Joystick extends View {
        Paint paint=new Paint(3); float cx,cy,knobX,knobY,radius; boolean held;
        Joystick(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){cx=w/2f;cy=h/2f;radius=Math.min(cx,cy)*0.82f;}
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); cx=getWidth()/2f;cy=getHeight()/2f;radius=Math.min(cx,cy)*0.82f;
            paint.setStyle(Paint.Style.FILL);paint.setColor(0x66777777);c.drawCircle(cx,cy,radius,paint);
            paint.setColor(0x99FFFFFF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));
            c.drawCircle(cx,cy,radius,paint);paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xDD303030);c.drawCircle(held?knobX:cx,held?knobY:cy,radius*0.43f,paint);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(),y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE) {
                held=true;float dx=x-cx,dy=y-cy,d=(float)Math.sqrt(dx*dx+dy*dy),max=Math.max(1,radius*0.62f);
                if(d>max){dx=dx*max/d;dy=dy*max/d;}
                knobX=cx+dx;knobY=cy+dy;renderer.setMove(dx/max,-dy/max);invalidate();return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL) {
                held=false;renderer.setMove(0,0);invalidate();return true;
            }
            return true;
        }
    }
    @Override protected void onPause(){if(gl!=null)gl.onPause();super.onPause();}
    @Override protected void onResume(){super.onResume();if(gl!=null)gl.onResume();}
    @Override protected void onDestroy(){if(loadingTask!=null)handler.removeCallbacks(loadingTask);super.onDestroy();}
}
