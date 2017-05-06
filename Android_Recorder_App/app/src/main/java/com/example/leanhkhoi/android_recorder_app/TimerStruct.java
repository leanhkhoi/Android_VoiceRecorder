package com.example.leanhkhoi.android_recorder_app;

import android.app.Dialog;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by Le Anh Khoi on 3/10/2017.
 */

public class TimerStruct {
    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public int getHour() {

        return hour;
    }
    public void setRunInBackground(boolean values){
        runInBackGround = values;
    }
    public void setHour(int hour) {
        this.hour = hour;
    }
    public int getTotalTime() {
        return totalTime;
    }
    public void setStop(boolean stop) {
        this.stop = stop;
    }
    public boolean getStop(){return this.stop;}
    public  void setTextView(TextView tv)
    {
        timeV = tv;
    }
    private int hour = 0;
    private int minute = 0;
    private  int second = 0;
    private int milisecond = 0;
    private int totalTime = 0;
    private boolean stop = true;
    private boolean runInBackGround = false;
    private TextView timeV;

    public  void startCount() {
        stop = false;

        new CountDownTimer(1, 1000) {

            //Tip : At the start of every tick, before onTick() is called,
            // the remaining time until the end of the countdown is calculated.
            // If this time is smaller than the countdown time interval, onTick is not called anymore.
            // Instead only the next tick (where the onFinish() method will be called) is scheduled.
            public void onTick(long millisUntilFinished)
            {
                //Following Tip: we will not install anything in this function
                //1000 millisUntilFinished

            }
            public void onFinish()
            {
                milisecond++;
                Log.d("time",""+milisecond + "" + second + " " + minute);

                if(!runInBackGround)
                formatTimer();
                else formatTimerInBackground();

                UpdateTimerView();

                if(stop==false) {
                    this.start();
                }
            }

        }.start();
    }

    private void formatTimer() {

        if(milisecond>=60)
        {
            milisecond =0;
            second++;
        }
        if(second==60)
        {
            second=0;
            minute++;
        }
        if(minute==60)
        {
            minute=0;
            hour++;
        }
    }
    private void formatTimerInBackground() {

        if(milisecond==500)
        {
            milisecond =0;
            second++;
        }
        if(second==60)
        {
            second=0;
            minute++;
        }
        if(minute==60)
        {
            minute=0;
            hour++;
        }
    }
    public void UpdateTimerView() {
        String Ssecond , Sminute, Shour;
        if(this.second <= 9)
        {
            Ssecond = "0"+String.valueOf(this.second);
        }
        else
        {
            Ssecond = String.valueOf(this.second);
        }
        if(this.minute <= 9)
        {
            Sminute = "0"+String.valueOf(this.minute);
        }
        else
        {
            Sminute = String.valueOf(this.minute);
        }
        if(this.hour <= 9)
        {
            Shour = "0"+String.valueOf(this.hour);
        }
        else
        {
            Shour = String.valueOf(this.hour);
        }
        timeV.setText(Shour + ":" + Sminute + ":" + Ssecond);

    }
    public int TotalTime()
    {
        return second + minute*60 + hour*3600;
    }

}
