package com.tern.greefigh;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.opengl.GLSurfaceView;
import android.content.pm.ActivityInfo;

public class MainActivity extends Activity {
    private FrameLayout root;
    private GLSurfaceView gl;
    private int screen = 0; // 0 loading, 1 menu, 2 game, 3 settings
    private long loadStart;
    private TextView loadingText, tipText;
    private FrameLayout overlay;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        root = new FrameLayout(this);
        gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        gl.setRenderer(new WorldRenderer());
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(gl);
        setContentView(root);
        showLoading("Preparing world...", 1);
    }

    private void clearOverlay() {
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this);
        root.addView(overlay);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextColor(Color.rgb(35,35,35)); t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setGravity(Gravity.CENTER);
        return t;
    }

    private void showLoading(String label, int destination) {
        screen = 0; loadStart = System.currentTimeMillis(); clearOverlay();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER);
        box.setPadding(36, 20, 36, 20);
        box.setBackgroundColor(Color.rgb(244,244,244));
        TextView title = text("TERNGREEFigh", 30);
        loadingText = text("LOADING 0%", 17);
        tipText = text("TIP: Explore the world at your own pace.", 13);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress(0);
        box.addView(title, new LinearLayout.LayoutParams(-1, 65));
        box.addView(bar, new LinearLayout.LayoutParams(360, 12));
        box.addView(loadingText, new LinearLayout.LayoutParams(-1, 45));
        box.addView(tipText, new LinearLayout.LayoutParams(-1, 40));
        overlay.addView(box, new FrameLayout.LayoutParams(-1, -1));
        final String[] tips = {"TIP: Explore the world at your own pace.", "TIP: Move with the joystick.", "TIP: Try the emotes in the game.", "TIP: Drag the screen to rotate the camera."};
        final android.os.Handler handler = new android.os.Handler();
        final Runnable task = new Runnable() {
            public void run() {
                long elapsed = System.currentTimeMillis() - loadStart;
                int p = Math.min(100, (int)(elapsed / 100));
                loadingText.setText("LOADING " + p + "%");
                bar.setProgress(p);
                tipText.setText(tips[(int)((elapsed / 2300) % tips.length)]);
                if (elapsed >= 10000) {
                    if (destination == 1) showMenu();
                    else if (destination == 2) showGame();
                    else showMenu();
                } else handler.postDelayed(this, 80);
            }
        };
        handler.post(task);
    }

    private Button makeButton(String label) {
        Button b = new Button(this); b.setText(label); b.setTextSize(16);
        b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(45,45,45)));
        return b;
    }

    private void showMenu() {
        screen = 1; clearOverlay();
        LinearLayout row = new LinearLayout(this);
        row.setPadding(30,20,30,20); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.argb(225,245,245,245));
        TextView avatar = text("  ●\n ███\n╱   ╲\nTERNGREEFigh", 25);
        avatar.setBackgroundColor(Color.rgb(225,225,225));
        row.addView(avatar, new LinearLayout.LayoutParams(0,-1,1.5f));
        LinearLayout menu = new LinearLayout(this); menu.setGravity(Gravity.CENTER);
        menu.setOrientation(LinearLayout.VERTICAL);
        Button play = makeButton("PLAY");
        Button settings = makeButton("SETTINGS");
        Button quit = makeButton("QUIT");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(260,58); bp.setMargins(8,8,8,8);
        menu.addView(play,bp); menu.addView(settings,bp); menu.addView(quit,bp);
        row.addView(menu, new LinearLayout.LayoutParams(0,-1,1));
        overlay.addView(row, new FrameLayout.LayoutParams(-1,-1));
        play.setOnClickListener(v -> showLoading("Entering world...",2));
        settings.setOnClickListener(v -> showSettings());
        quit.setOnClickListener(v -> finish());
    }

    private void showSettings() {
        screen = 3; clearOverlay();
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER); panel.setPadding(30,15,30,15); panel.setBackgroundColor(Color.rgb(242,242,242));
        panel.addView(text("SETTINGS",25), new LinearLayout.LayoutParams(-1,55));
        panel.addView(text("Camera sensitivity",16), new LinearLayout.LayoutParams(-1,35));
        android.widget.SeekBar sensitivity = new android.widget.SeekBar(this); sensitivity.setMax(100); sensitivity.setProgress(50);
        panel.addView(sensitivity,new LinearLayout.LayoutParams(420,45));
        panel.addView(text("Control size",16),new LinearLayout.LayoutParams(-1,35));
        android.widget.SeekBar size = new android.widget.SeekBar(this); size.setMax(100); size.setProgress(50);
        panel.addView(size,new LinearLayout.LayoutParams(420,45));
        Button back = makeButton("BACK TO MENU");
        panel.addView(back,new LinearLayout.LayoutParams(240,55));
        overlay.addView(panel,new FrameLayout.LayoutParams(-1,-1));
        back.setOnClickListener(v -> showLoading("Returning to menu...",1));
    }

    private void showGame() {
        screen = 2; clearOverlay();
        overlay.setBackgroundColor(Color.TRANSPARENT);
        Button back = makeButton("MENU");
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(150,55,Gravity.TOP|Gravity.RIGHT);
        p.setMargins(0,12,12,0); overlay.addView(back,p);
        back.setOnClickListener(v -> showLoading("Returning to menu...",1));
        TextView controls = text("JOYSTICK                         JUMP     CROUCH     EMOTE",13);
        controls.setTextColor(Color.DKGRAY);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1,65,Gravity.BOTTOM);
        overlay.addView(controls,cp);
    }

    @Override protected void onPause() { super.onPause(); if (gl != null) gl.onPause(); }
    @Override protected void onResume() { super.onResume(); if (gl != null) gl.onResume(); }
}
