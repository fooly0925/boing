package com.boing.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() { super.onPause(); game.pause(); }
    @Override protected void onResume() { super.onResume(); if (game != null) game.resume(); }
    @Override protected void onDestroy() { game.dispose(); super.onDestroy(); }
}
