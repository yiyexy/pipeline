package com.example.socket_test;

import android.os.Bundle;


import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;


public class MainActivity extends AppCompatActivity {

    private EditText et_send;
    private Button bt_send;
    private TextView tv_recv;

    private String send_buff=null;
    private String recv_buff=null;
    private String result = null;
    private String info = "";
    double thresh = 0.1;

    private Handler handler = null;

    Socket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        handler = new Handler();

        //单开一个线程来进行socket通信
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.0.134" , 7655);
//                    socket = new Socket("10.0.3.2",7654);
//                    socket.setReceiveBufferSize(4096000);
                    if (socket!=null) {
//                        System.out.println("###################");
                        while (true) {      //循环进行收发
                            recv();
                        }
                    }
                    else
                        System.out.println("socket is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        send();
    }




    private void recv() {
        //单开一个线程循环接收来自服务器端的消息
        long startTime = System.currentTimeMillis(); //获取开始时间

        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        info = "";
//        BufferedInputStream bufferIn = new BufferedInputStream(inputStream);
//        byte[] buf = new byte[1024];
//        try {
//            int len;
//            while ((len = bufferIn.read(buf)) != -1) {
//                System.out.print(new String(buf, 0, len, Charset.forName("UTF-8")));
//            }
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            socket.shutdownInput();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        int s=0;
        while(inputStream != null) {
            try {
                byte[] bytes = new byte[2048000];
                DataInputStream input = new DataInputStream(inputStream);
//                String ret = input.readUTF();
                int count = inputStream.read(bytes);
                recv_buff = new String(bytes,0,count, "GBK");//socket通信传输的是byte类型，需要转为String类型
//                if(recv_buff.substring(recv_buff.length()-1) == "\u0000" )
//                    info += recv_buff.substring(0,recv_buff.indexOf("\u0000"));
//                else
                    info += recv_buff;

//                    if ((ret = input.readUTF()) != null) {
//                        info += ret;
//                        System.out.println(ret);
//                    }
//                System.out.println(recv_buff);
                if(s == 10){
                    break;
                }
                s++;
                if(recv_buff.length()==0){
                    break;
                }
                if(recv_buff.substring(count-3).equals("]]]"))
                    break;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("数据接收时间：" + (endTime - startTime) + "ms"); //输出程序运行时间
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        info = info.substring(0,info.indexOf("]]]]]")+5);
        // 创建一个map用于将一张图片中的多条车道线组合到一起
        /*
        startTime = System.currentTimeMillis(); //获取开始时间
        ArrayList<int[][]> lanes = new ArrayList<>();
        int lane_id = 0;
        // 将接收到的数据解构成数组
        try{
            Double[][][][][] result = JSON.parseObject(info, Double[][][][][].class);



        Double[][][][] t_2d_delta = result[0];
        Double[][][][] t_2d_point = result[1];
        Double[][][][] t_2d_height = result[2];
        int feat_h= t_2d_delta[0][0].length;
        int feat_w =  t_2d_delta[0][0][0].length;

        for(int i=1;i<feat_h-1;i++){
            for(int j=1;j<feat_w-1;j++){
                if(t_2d_point[0][0][i][j] > thresh && t_2d_point[0][0][i][j] == Collections.max(Arrays.asList(new Double[] {
                        t_2d_point[0][0][i - 1][j - 1], t_2d_point[0][0][i - 1][j], t_2d_point[0][0][i - 1][j + 1],
                        t_2d_point[0][0][i][j - 1], t_2d_point[0][0][i][j], t_2d_point[0][0][i][j + 1],
                        t_2d_point[0][0][i + 1][j - 1], t_2d_point[0][0][i + 1][j], t_2d_point[0][0][i + 1][j + 1]
                }))){
                    double new_x = j;
                    double new_y = i;
                    double height = t_2d_height[0][0][i][j];
                    int[][] points = new int[(int)(height+1)][2];
                    for(int delta_y=1; delta_y < Math.abs((int)height)+2;delta_y++){
                        if(new_x > feat_w){
                            continue;
                        }
                        points[delta_y-1] = new int[] {(int)(new_x*10),(int)(new_y*10)};
                        new_x -= (double)t_2d_delta[0][0][(int)new_y][(int)new_x] > 0 ? Math.sqrt(Math.abs(t_2d_delta[0][0][(int)new_y][(int)new_x])):-Math.sqrt(Math.abs(t_2d_delta[0][0][(int)new_y][(int)new_x]));
                        new_y -= 1;
                    }
                    lanes.add(points);
                    lane_id ++;
                }
            }
        }
        send_buff = JSONArray.toJSONString(lanes);

        endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("数据处理时间：" + (endTime - startTime) + "ms"); //输出程序运行时间
        }catch (Exception e){
            send_buff = "error";
        }
         */
        send_buff = "1";
        send();
        //将受到的数据显示在TextView上
//        if (recv_buff!=""){
//            handler.post(runnableUi);
//
//        }
    }

    //不能在子线程中刷新UI，应为textView是主线程建立的
//    Runnable runnableUi = new Runnable() {
//        @Override
//        public void run() {
//            tv_recv.append("\n"+recv_buff);
//        }
//    };

    private void send() {
        //向服务器端发送消息
//        System.out.println("------------------------");
        OutputStream outputStream=null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(outputStream!=null) {
            try {
                outputStream.write(send_buff.getBytes());
//                System.out.println("1111111111111111111111");
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initView() {
        et_send = (EditText) findViewById(R.id.et_send);
        bt_send = (Button) findViewById(R.id.bt_send);
        tv_recv = (TextView) findViewById(R.id.tv_recv);
    }
}

