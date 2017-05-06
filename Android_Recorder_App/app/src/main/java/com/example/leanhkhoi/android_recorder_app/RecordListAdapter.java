package com.example.leanhkhoi.android_recorder_app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Le Anh Khoi on 4/17/2017.
 */

public class RecordListAdapter extends BaseAdapter {
    private List<RecordFile> listData;
    private LayoutInflater layoutInflater;
    private Context context;
    private int posState = -1;
    private String state;
    public RecordListAdapter(Context aContext,  List<RecordFile> listData) {
        this.listData = listData;
        this.context = aContext;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_voice_record_layout, null);
            holder = new ViewHolder();
            holder.fileName = (TextView) convertView.findViewById(R.id.tv_filenamerecord);
            holder.sizeFile = (TextView) convertView.findViewById(R.id.tv_sizereocord);
            holder.stateBtn = (Button) convertView.findViewById(R.id.btn_pause_run);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        RecordFile rcf = this.listData.get(position);
        holder.fileName.setText(rcf.getName());
        holder.sizeFile.setText(String.valueOf(rcf.getSize()) + " KB");
        if(posState != -1){
            if(position==posState){
                holder.stateBtn.setVisibility(View.VISIBLE);
                holder.stateBtn.setText(state);
            }
            else holder.stateBtn.setVisibility(View.GONE);
        }

        //set event for button
       /* holder.stateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.stateBtn.getText().equals("Running")){
                    MainActivity.PauseOrRunCurrnetPlayerFromAdapter("Running");
                    holder.stateBtn.setText("Pausing");
                }
                else{
                    MainActivity.PauseOrRunCurrnetPlayerFromAdapter("Pausing");
                    holder.stateBtn.setText("Running");
                }

            }
        });*/
        return convertView;
    }
    public void setCurrentPosRunning(RecordFile f, String st){
        posState = listData.indexOf(f);
        state = st;
        notifyDataSetChanged();
    }
    public void setCurrentPosRunningByName(String name, String st){
        for(int i = 0; i < listData.size(); i++){
            if(listData.get(i).getName().equals(name)){
                posState = i;
                state = st;
                notifyDataSetChanged();
                return;
            }
        }

    }
    static class ViewHolder {
        TextView fileName;
        TextView sizeFile;
        Button stateBtn;
    }
}
