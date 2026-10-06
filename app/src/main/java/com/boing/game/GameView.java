package com.boing.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

/** Entirely local game: no networking, external assets, or runtime dependencies. */
public final class GameView extends View {
    private static final float W=400, H=760, LEFT=18, RIGHT=382, TOP=100, FLOOR=666, R=8;
    private static final int LIMIT=20;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random=new Random();
    private final ArrayList<Target> targets=new ArrayList<>();
    private final ArrayList<Ball> balls=new ArrayList<>();
    private final ArrayList<Particle> particles=new ArrayList<>();
    private final SharedPreferences prefs;
    private ToneGenerator tone;
    private float scale=1, ox, oy, age, spawnClock, shotAge, aimX=0, aimY=-1, downX, downY;
    private int insetLeft, insetTop, insetRight, insetBottom;
    private int score, best, combo, shots, multi;
    private boolean aiming, active, over, paused, sound;
    private long last, holdStart;
    private String message="Swipe UP from the launch zone";
    private float messageTime=5;
    private static final int INK=Color.rgb(48,48,79), PINK=Color.rgb(255,135,171), MINT=Color.rgb(112,220,189), GOLD=Color.rgb(255,211,108);
    private static class Target { float x,y,r=22, cooldown; int type,hp; boolean power; }
    private static class Ball { float x,y,vx,vy; Ball(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;} }
    private static class Particle { float x,y,vx,vy,life; int color; }
    public GameView(Context context) {
        super(context); setFocusable(true);
        setOnApplyWindowInsetsListener((view, insets) -> {
            insetLeft=insets.getSystemWindowInsetLeft(); insetTop=insets.getSystemWindowInsetTop();
            insetRight=insets.getSystemWindowInsetRight(); insetBottom=insets.getSystemWindowInsetBottom();
            fit(getWidth(),getHeight()); return insets;
        });
        prefs=context.getSharedPreferences("boing",Context.MODE_PRIVATE);
        best=prefs.getInt("best",0); sound=prefs.getBoolean("sound",true);
        try { tone=new ToneGenerator(AudioManager.STREAM_MUSIC,35); } catch(RuntimeException ignored) { }
        reset();
    }
    private void reset(){
        targets.clear(); balls.clear(); particles.clear(); score=combo=shots=multi=0;
        age=spawnClock=shotAge=0; active=over=aiming=false;
        message="Swipe UP from the launch zone"; messageTime=5;
        for(int i=0;i<6;i++) spawn(); last=0; invalidate();
    }
    public void pause(){paused=true; aiming=false; last=0;}
    public void resume(){paused=false;last=0;invalidate();}
    public void dispose(){if(tone!=null){tone.release();tone=null;}}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        fit(w,h);
    }
    private void fit(int w,int h){
        // Leave room for system bars; drawing and touch use the same transform.
        float usableW=Math.max(1,w-insetLeft-insetRight),usableH=Math.max(1,h-insetTop-insetBottom);
        scale=Math.min(usableW/W,usableH/H);ox=insetLeft+(usableW-W*scale)/2;oy=insetTop+(usableH-H*scale)/2;
    }
    private void spawn(){
        if(targets.size()>=LIMIT){end();return;}
        Target t=new Target();t.type=random.nextInt(3);t.hp=t.type+1;
        t.power=random.nextFloat()<0.13f;
        boolean placed=false;
        for(int attempt=0;attempt<120;attempt++){
            t.x=46+random.nextFloat()*308;t.y=146+random.nextFloat()*410;
            placed=true;
            for(Target a:targets)if(Math.hypot(t.x-a.x,t.y-a.y)<a.r+t.r+9){placed=false;break;}
            if(placed)break;
        }
        if(!placed)return;
        targets.add(t); if(targets.size()>=LIMIT)end();
    }
    private void end(){over=true;aiming=false;active=false;balls.clear();saveBest();beep(ToneGenerator.TONE_PROP_NACK);}
    private void saveBest(){if(score>best){best=score;prefs.edit().putInt("best",best).apply();}}
    private void beep(int type){if(sound&&tone!=null)try{tone.startTone(type,65);}catch(RuntimeException ignored){}}
    private void burst(Target t){
        for(int i=0;i<14;i++){Particle a=new Particle();a.x=t.x;a.y=t.y;
            double angle=random.nextDouble()*Math.PI*2;float speed=50+random.nextFloat()*160;
            a.vx=(float)Math.cos(angle)*speed;a.vy=(float)Math.sin(angle)*speed;a.life=0.6f;a.color=targetColor(t);particles.add(a);}
    }
    private int targetColor(Target t){return t.type==0?PINK:t.type==1?MINT:GOLD;}
    private void update(float dt){
        if(over)return;
        age+=dt;spawnClock+=dt;messageTime=Math.max(0,messageTime-dt);
        float interval=Math.max(1.6f,6-age/35);
        if(spawnClock>=interval){spawnClock-=interval;spawn();}
        for(Target t:targets)t.cooldown=Math.max(0,t.cooldown-dt);
        for(int i=particles.size()-1;i>=0;i--){Particle a=particles.get(i);a.life-=dt;a.x+=a.vx*dt;a.y+=a.vy*dt;a.vy+=120*dt;if(a.life<=0)particles.remove(i);}
        if(!active||over)return;
        shotAge+=dt;
        // Small substeps prevent tunneling even on slower phones.
        int steps=Math.max(1,(int)Math.ceil(dt/0.008f));float step=dt/steps;
        for(int s=0;s<steps;s++){
            for(int bi=balls.size()-1;bi>=0;bi--){Ball b=balls.get(bi);b.x+=b.vx*step;b.y+=b.vy*step;
                if(b.x<LEFT+R){b.x=LEFT+R;b.vx=Math.abs(b.vx);}if(b.x>RIGHT-R){b.x=RIGHT-R;b.vx=-Math.abs(b.vx);}
                if(b.y<TOP+R){b.y=TOP+R;b.vy=Math.abs(b.vy);}
                for(int ti=targets.size()-1;ti>=0;ti--){Target t=targets.get(ti);float dx=b.x-t.x,dy=b.y-t.y;float d=(float)Math.hypot(dx,dy);
                    if(d<t.r+R){float nx=d>0?dx/d:0,ny=d>0?dy/d:1;b.x=t.x+nx*(t.r+R+1);b.y=t.y+ny*(t.r+R+1);
                        float dot=b.vx*nx+b.vy*ny;if(dot<0){b.vx-=2*dot*nx;b.vy-=2*dot*ny;}
                        if(t.cooldown<=0){t.cooldown=0.12f;t.hp--;score+=5;
                            if(t.hp<=0){combo++;score+=25*t.type+25*Math.min(combo,8);burst(t);targets.remove(ti);
                                if(t.power){multi=3;message="MULTI-BALL! Next 3 shots";messageTime=3;}
                                beep(ToneGenerator.TONE_PROP_BEEP);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            } saveBest();
                        }
                    }
                }
                // A gentle downward pull and a hard time limit always return the ball.
                if(shotAge>5){b.vy+=100*step;float v=(float)Math.hypot(b.vx,b.vy);b.vx=b.vx/v*470;b.vy=b.vy/v*470;}
                if(b.y>=FLOOR||shotAge>11)balls.remove(bi);
            }
        }
        if(balls.isEmpty()){active=false;shots++;message=combo>1?combo+"-target combo!":"Nice! Swipe for another shot";messageTime=2;}
    }
    private void aim(float x,float y){
        float dx=x-downX,dy=y-downY; if(Math.hypot(dx,dy)<10){aimX=0;aimY=-1;return;}
        dy=Math.min(-25,dy);float len=(float)Math.hypot(dx,dy);aimX=dx/len;aimY=dy/len;
        // Snap within eight degrees to a visible target. Preview uses this same vector.
        double bestAngle=Math.toRadians(8);Target chosen=null;
        for(Target t:targets){float tx=t.x-200,ty=t.y-650;float l=(float)Math.hypot(tx,ty);
            double angle=Math.acos(Math.max(-1,Math.min(1,(tx*aimX+ty*aimY)/l)));
            if(angle<bestAngle){bestAngle=angle;chosen=t;}}
        if(chosen!=null){float dx2=chosen.x-200,dy2=chosen.y-650;float len2=(float)Math.hypot(dx2,dy2);aimX=dx2/len2;aimY=dy2/len2;}
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(y<90&&x>305){sound=!sound;prefs.edit().putBoolean("sound",sound).apply();invalidate();return true;}
                if(over){if(y>430&&y<540){reset();}return true;}
                if(!active&&y>590){aiming=true;downX=x;downY=y;holdStart=System.nanoTime();aimX=0;aimY=-1;}return true;
            case MotionEvent.ACTION_MOVE:if(aiming)aim(x,y);return true;
            case MotionEvent.ACTION_UP:
                if(aiming){aim(x,y);aiming=false;if(Math.hypot(x-downX,y-downY)>18&&y<downY-12){
                    combo=0;shotAge=0;active=true;balls.add(new Ball(200,650,aimX*470,aimY*470));
                    if(multi>0){multi--;double a=Math.atan2(aimY,aimX);for(int sign:new int[]{-1,1})balls.add(new Ball(200,650,(float)Math.cos(a+sign*0.14)*470,(float)Math.sin(a+sign*0.14)*470));}
                    beep(ToneGenerator.TONE_PROP_ACK);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }}performClick();return true;
            case MotionEvent.ACTION_CANCEL:aiming=false;return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void fill(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setAlpha(255);}
    private void text(Canvas c,String t,float x,float y,float size,int color){fill(color);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);}
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color){fill(color);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long now=System.nanoTime();if(!paused){if(last!=0)update(Math.min(0.04f,(now-last)/1e9f));last=now;}
        c.drawColor(Color.rgb(238,237,249));c.save();c.translate(ox,oy);c.scale(scale,scale);
        text(c,"BOING!",75,40,27,INK);text(c,""+score,200,40,30,INK);text(c,sound?"Sound ON":"Sound OFF",344,38,13,INK);
        text(c,"BEST "+best,200,65,12,INK);
        round(c,18,77,382,86,4,Color.rgb(220,217,234));round(c,18,77,18+364*targets.size()/LIMIT,86,4,targets.size()>15?PINK:MINT);
        round(c,LEFT,TOP,RIGHT,FLOOR,25,Color.WHITE);
        for(Target t:targets)drawTarget(c,t);
        for(Particle a:particles){fill(a.color);p.setAlpha((int)(255*a.life/0.6f));c.drawCircle(a.x,a.y,4,p);}p.setAlpha(255);
        for(Ball b:balls){fill(INK);c.drawCircle(b.x,b.y+3,R,p);fill(GOLD);c.drawCircle(b.x,b.y,R,p);}
        if(!active&&!over){fill(GOLD);c.drawCircle(200,650,11,p);fill(INK);c.drawCircle(197,648,1.5f,p);c.drawCircle(203,648,1.5f,p);}
        if(aiming){
            fill(INK);p.setStrokeWidth(4);c.drawLine(200,650,200+aimX*60,650+aimY*60,p);
            float ex=200+aimX*60,ey=650+aimY*60;c.drawLine(ex,ey,ex-aimX*12-aimY*6,ey-aimY*12+aimX*6,p);c.drawLine(ex,ey,ex-aimX*12+aimY*6,ey-aimY*12-aimX*6,p);
            if((now-holdStart)>300000000L){float x=200,y=650,vx=aimX,vy=aimY;int bounces=0;
                for(int i=0;i<70;i++){x+=vx*12;y+=vy*12;if(x<LEFT+R||x>RIGHT-R){x=Math.max(LEFT+R,Math.min(RIGHT-R,x));vx=-vx;bounces++;}if(y<TOP+R){y=TOP+R;vy=-vy;bounces++;}
                    if(y>FLOOR||bounces>1)break;fill(Color.rgb(177,176,205));c.drawCircle(x,y,2.5f,p);
                    boolean hit=false;for(Target t:targets)if(Math.hypot(x-t.x,y-t.y)<t.r+R){hit=true;break;}if(hit)break;
                }
            }
        }
        text(c,targets.size()+" / "+LIMIT+" friends • clear space!",200,117,12,INK);
        text(c,multi>0?"MULTI-BALL: "+multi+" shots":"SWIPE UP TO LAUNCH",200,702,17,INK);
        text(c,messageTime>0?message:active?"Boing, boing…":"Hold your swipe for a dotted preview",200,729,12,INK);
        if(active&&combo>1)text(c,"COMBO ×"+Math.min(combo,8),200,610,20,PINK);
        if(over){fill(Color.argb(215,238,237,249));c.drawRect(0,0,W,H,p);
            text(c,"Arena full!",200,285,36,INK);text(c,"You scored "+score,200,332,24,INK);text(c,"Best "+best,200,367,20,INK);
            round(c,65,440,335,523,24,INK);text(c,"PLAY AGAIN",200,491,25,Color.WHITE);
            text(c,"Every bounce is a fresh start.",200,563,15,INK);
        }
        c.restore();if(!paused)postInvalidateOnAnimation();
    }
    private void drawTarget(Canvas c,Target t){
        fill(targetColor(t));
        if(t.type==0){c.drawOval(new RectF(t.x-18,t.y-36,t.x-7,t.y-8),p);c.drawOval(new RectF(t.x+7,t.y-36,t.x+18,t.y-8),p);}
        if(t.type==1){c.drawCircle(t.x-14,t.y-17,10,p);c.drawCircle(t.x+14,t.y-17,10,p);}
        c.drawCircle(t.x,t.y,t.r,p);fill(INK);c.drawCircle(t.x-7,t.y-2,2.5f,p);c.drawCircle(t.x+7,t.y-2,2.5f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawArc(new RectF(t.x-5,t.y+1,t.x+5,t.y+9),0,180,false,p);p.setStyle(Paint.Style.FILL);
        fill(Color.rgb(255,191,196));c.drawCircle(t.x-13,t.y+6,4,p);c.drawCircle(t.x+13,t.y+6,4,p);
        for(int i=0;i<t.hp;i++){fill(INK);c.drawCircle(t.x+(i-(t.hp-1)/2f)*7,t.y+16,2,p);}
        if(t.power)text(c,"+",t.x+19,t.y-17,21,INK);
    }
}
