package com.tern.greefigh;

import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WorldRenderer implements GLSurfaceView.Renderer {
    private FloatBuffer vertices;
    private int program, positionHandle, colorHandle, mvpHandle;
    private final float[] projection = new float[16], view = new float[16], model = new float[16], mvp = new float[16];
    private static final String VERT =
        "uniform mat4 uMVP; attribute vec3 aPos; void main(){ gl_Position=uMVP*vec4(aPos,1.0); }";
    private static final String FRAG =
        "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
    private final float[] grid = buildGrid();

    private float[] buildGrid() {
        float[] a = new float[41*4*3]; int k=0;
        for(int i=-10;i<=10;i++){
            a[k++]=i; a[k++]=0; a[k++]=-10; a[k++]=i; a[k++]=0; a[k++]=10;
            a[k++]=-10; a[k++]=0; a[k++]=i; a[k++]=10; a[k++]=0; a[k++]=i;
        }
        return java.util.Arrays.copyOf(a,k);
    }
    private FloatBuffer buffer(float[] data) {
        FloatBuffer b=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(data).position(0); return b;
    }
    private int shader(int type,String src){
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        GLES20.glClearColor(0.96f,0.96f,0.96f,1);
        program=GLES20.glCreateProgram();
        GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,VERT));
        GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,FRAG));
        GLES20.glLinkProgram(program);
        positionHandle=GLES20.glGetAttribLocation(program,"aPos");
        colorHandle=GLES20.glGetUniformLocation(program,"uColor");
        mvpHandle=GLES20.glGetUniformLocation(program,"uMVP");
        vertices=buffer(grid);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){
        GLES20.glViewport(0,0,w,h);
        Matrix.perspectiveM(projection,0,55f,(float)w/Math.max(1,h),0.1f,100f);
    }
    private void draw(float[] data,int mode,float r,float g,float b){
        FloatBuffer buf=buffer(data);
        GLES20.glUseProgram(program);
        Matrix.multiplyMM(mvp,0,view,0,model,0);
        float[] finalM=new float[16]; Matrix.multiplyMM(finalM,0,projection,0,mvp,0);
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,finalM,0);
        GLES20.glUniform4f(colorHandle,r,g,b,1);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,0,buf);
        GLES20.glDrawArrays(mode,0,data.length/3);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.setLookAtM(view,0,0,5,9,0,0,0,0,1,0);
        Matrix.setIdentityM(model,0);
        draw(grid,GLES20.GL_LINES,0.72f,0.72f,0.72f);
    }
}
