package com.tern.greefigh;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WorldRenderer implements GLSurfaceView.Renderer {
    private volatile float moveX,moveY,sensitivity=1.25f;
    private final boolean previewMode;
    private float playerYaw=0f;
    private volatile boolean jumpRequest,crouch;
    private volatile int emoteRequest;
    private float px=0,pz=0,playerY=0,vy=0,yaw=180,pitch=12,anim=0,emoteTime=0;
    private boolean airborne=false,ragdoll=false;
    private int program,posHandle,colorHandle,mvpHandle;
    public WorldRenderer(){this(false);}
    public WorldRenderer(boolean preview){previewMode=preview;}
    private final float[] projection=new float[16],view=new float[16],model=new float[16],mvp=new float[16],temp=new float[16];
    private FloatBuffer cubeBuffer,gridBuffer,sphereBuffer;
    private int sphereCount;
    private float rollX=0f,rollZ=0f;
    private static final String VS="uniform mat4 uMVP; attribute vec3 aPos; void main(){gl_Position=uMVP*vec4(aPos,1.0);}";
    private static final String FS="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
    private static final float[] CUBE={
      -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f,
      0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f,
      -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f,
      0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f,
      -0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f,
      -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f
    };
    private float[] makeGrid(){float[] a=new float[21*12];int k=0;for(int i=-10;i<=10;i++){a[k++]=i;a[k++]=0;a[k++]=-10;a[k++]=i;a[k++]=0;a[k++]=10;a[k++]=-10;a[k++]=0;a[k++]=i;a[k++]=10;a[k++]=0;a[k++]=i;}return java.util.Arrays.copyOf(a,k);}
    private FloatBuffer buffer(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}
    private float[] makeSphere(){
        int stacks=16,slices=24;
        float[] a=new float[stacks*slices*18];
        int k=0;
        for(int i=0;i<stacks;i++){
            float v0=(float)(-Math.PI/2+i*Math.PI/stacks);
            float v1=(float)(-Math.PI/2+(i+1)*Math.PI/stacks);
            for(int j=0;j<slices;j++){
                float u0=(float)(j*2*Math.PI/slices);
                float u1=(float)((j+1)*2*Math.PI/slices);
                float[] p00=sp(v0,u0),p01=sp(v0,u1);
                float[] p10=sp(v1,u0),p11=sp(v1,u1);
                k=put(a,k,p00); k=put(a,k,p10); k=put(a,k,p11);
                k=put(a,k,p00); k=put(a,k,p11); k=put(a,k,p01);
            }
        }
        return a;
    }

    private float[] sp(float v,float u){
        float c=(float)Math.cos(v);
        return new float[]{c*(float)Math.sin(u),(float)Math.sin(v),
                c*(float)Math.cos(u)};
    }

    private int put(float[] a,int k,float[] p){
        a[k++]=p[0];a[k++]=p[1];a[k++]=p[2];
        return k;
    }

    private void ballPart(float x,float y,float z,
            float sx,float sy,float sz,float r,float g,float b){
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,px,playerY+0.68f,pz);
        Matrix.rotateM(model,0,rollX,1,0,0);
        Matrix.rotateM(model,0,rollZ,0,0,1);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        draw(sphereBuffer,sphereCount,GLES20.GL_TRIANGLES,r,g,b);
    }

    private int compile(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[] ok={0};GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new RuntimeException("Shader compile failed: "+GLES20.glGetShaderInfoLog(s));return s;}
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
        GLES20.glClearColor(0.88f,0.89f,0.87f,1);
        program=GLES20.glCreateProgram();
        GLES20.glAttachShader(program,compile(GLES20.GL_VERTEX_SHADER,VS));
        GLES20.glAttachShader(program,compile(GLES20.GL_FRAGMENT_SHADER,FS));
        GLES20.glLinkProgram(program);
        int[] ok={0};GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0)throw new RuntimeException("Shader link failed: "+GLES20.glGetProgramInfoLog(program));
        posHandle=GLES20.glGetAttribLocation(program,"aPos");
        colorHandle=GLES20.glGetUniformLocation(program,"uColor");
        mvpHandle=GLES20.glGetUniformLocation(program,"uMVP");
        cubeBuffer=buffer(CUBE);
        gridBuffer=buffer(makeGrid());
        float[] sphereData=makeSphere();
        sphereBuffer=buffer(sphereData);
        sphereCount=sphereData.length/3;
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDisable(GLES20.GL_CULL_FACE);
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,55f,(float)w/Math.max(1,h),0.1f,100f);}
    public void setMove(float x,float y){moveX=x;moveY=y;}
    public void setSensitivity(float s){sensitivity=Math.max(0.2f,Math.min(3f,s));}
    public void jump(){jumpRequest=true;}
    public void toggleCrouch(){crouch=!crouch;}
    public void emote(int e){emoteRequest=e;}
    public void rotateCamera(float dx,float dy){yaw-=dx*0.25f*sensitivity;pitch=Math.max(-5,Math.min(55,pitch+dy*0.18f*sensitivity));}
    private void draw(FloatBuffer b,int count,int mode,float r,float g,float bl){
        GLES20.glUseProgram(program);
        Matrix.multiplyMM(temp,0,view,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvp,0);GLES20.glUniform4f(colorHandle,r,g,bl,1);
        GLES20.glEnableVertexAttribArray(posHandle);GLES20.glVertexAttribPointer(posHandle,3,GLES20.GL_FLOAT,false,0,b);
        GLES20.glDrawArrays(mode,0,count);GLES20.glDisableVertexAttribArray(posHandle);
    }
    private void box(float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b){
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,px,0,pz);
        Matrix.rotateM(model,0,playerYaw,0,1,0);
        Matrix.translateM(model,0,x-px,y,z-pz);
        Matrix.rotateM(model,0,rz,0,0,1);Matrix.rotateM(model,0,ry,0,1,0);Matrix.rotateM(model,0,rx,1,0,0);
        Matrix.scaleM(model,0,sx,sy,sz);draw(cubeBuffer,36,GLES20.GL_TRIANGLES,r,g,b);
    }
    private void update(float dt){
        float rad=(float)Math.toRadians(yaw),forward=moveY,side=moveX;
        float vx=(float)(Math.sin(rad)*forward+Math.cos(rad)*side);
        float vz=(float)(-Math.cos(rad)*forward+Math.sin(rad)*side);
        float len=(float)Math.sqrt(vx*vx+vz*vz);
        if(len>1){vx/=len;vz/=len;}
        if(len>0.08f) playerYaw=(float)Math.toDegrees(Math.atan2(vx,-vz));
        if(emoteRequest!=0){ragdoll=emoteRequest==3;emoteTime=0;emoteRequest=0;}
        if(emoteTime<2.0f)emoteTime+=dt;else if(!ragdoll)emoteTime=0;
        if(ragdoll&&(len>0.1f||jumpRequest)){ragdoll=false;emoteTime=0;}
        if(jumpRequest&&!airborne&&!ragdoll){vy=5.2f;airborne=true;}jumpRequest=false;
        if(!ragdoll){
            float speed=3.1f;
            px+=vx*speed*dt;
            pz+=vz*speed*dt;
            if(len>0.01f){
                rollX+=vz*speed*dt*85f;
                rollZ-=vx*speed*dt*85f;
            }
        }
        if(airborne){vy-=9.8f*dt;playerY+=vy*dt;if(playerY<=0){playerY=0;vy=0;airborne=false;}}
        anim+=dt*(len>0.08f?9f:2f);
    }
    @Override public void onDrawFrame(GL10 gl){
        if(previewMode){
            yaw+=0.35f;
            px=0;pz=0;playerY=0;
        } else {
            update(0.016f);
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        float yr=(float)Math.toRadians(yaw),pr=(float)Math.toRadians(pitch);
        float tx=px,ty=1.05f+playerY,tz=pz,dist=5.5f,horiz=dist*(float)Math.cos(pr);
        float camX=tx-(float)Math.sin(yr)*horiz,camZ=tz+(float)Math.cos(yr)*horiz,camY=ty+dist*(float)Math.sin(pr);
        Matrix.setLookAtM(view,0,camX,camY,camZ,tx,ty,tz,0,1,0);
        Matrix.setIdentityM(model,0);draw(gridBuffer,gridBuffer.capacity()/3,GLES20.GL_LINES,0.62f,0.65f,0.66f);
        drawCharacter();
    }
    private void drawCharacter(){
        float pulse=airborne?(float)Math.sin(anim*2)*0.035f:0;

        // Bola utama
        ballPart(0,0,0,
                0.66f,0.66f+pulse,0.66f,
                0.98f,0.69f,0.12f);

        // Panel warna dekoratif
        ballPart(0,0.35f,-0.39f,0.24f,0.10f,0.055f,
                0.12f,0.68f,0.95f);

        // Mata
        ballPart(-0.21f,0.13f,-0.55f,
                0.105f,0.12f,0.065f,1f,1f,1f);
        ballPart(0.21f,0.13f,-0.55f,
                0.105f,0.12f,0.065f,1f,1f,1f);

        // Pupil
        ballPart(-0.21f,0.13f,-0.607f,
                0.045f,0.06f,0.025f,0.08f,0.08f,0.10f);
        ballPart(0.21f,0.13f,-0.607f,
                0.045f,0.06f,0.025f,0.08f,0.08f,0.10f);

        // Mulut
        ballPart(0,-0.12f,-0.60f,
                0.11f,0.035f,0.025f,0.18f,0.10f,0.08f);
    }
}
