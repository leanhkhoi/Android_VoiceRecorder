package com.example.leanhkhoi.android_recorder_app;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Le Anh Khoi on 4/24/2017.
 */

public class RecordListSelectAdapter extends BaseAdapter {
    private List<RecordFile> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    //danh cac ten file da chon
    ArrayList<String> listFileName = new ArrayList<String>();
    boolean[] checkStates; //

    public RecordListSelectAdapter(Context aContext,  List<RecordFile> listData) {
        this.context = aContext;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
        checkStates = new boolean[listData.size()];

        //ham duoi de dam bao khi load listview lên thì không con item nao dang co san trong danh sach (trong maintAcivity)
        MainActivity.eventBackFromListView(listFileName);
    }

    public ArrayList<String> getListSelected(){
        return  this.listFileName;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder2 holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_voice_record_layout, null);
            holder = new ViewHolder2();
            holder.fileName = (TextView) convertView.findViewById(R.id.tv_filenamerecord);
            holder.sizeFile = (TextView) convertView.findViewById(R.id.tv_sizereocord);
            holder.itemSelected = (CheckBox) convertView.findViewById(R.id.cb_itemSelected);
            holder.itemSelected.setVisibility(View.VISIBLE);

            convertView.setTag(holder);
        } else {

            //ham getTag có thể lấy luôn tình trạng checkbox không mong muốn vì nó lấy holder của thằng trước đó
            holder = (ViewHolder2) convertView.getTag();
            Log.d("getV",String.valueOf(position) + " " +  holder.itemSelected.isChecked());
           // holder.itemSelected.setTag(holder);
        }
        final RecordFile rcf = this.listData.get(position);
        holder.itemSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStates[position] = !checkStates[position];
                if(checkStates[position]) {
                    listFileName.add(rcf.getName());
                }
                else{
                    listFileName.remove((Object)rcf.getName());
                }
                MainActivity.eventBackFromListView(listFileName);
            }
        });


        holder.fileName.setText(rcf.getName());
        holder.sizeFile.setText(String.valueOf(rcf.getSize()) + " KB");

        //hàm này xử lý sự không mong muốn trên
        holder.itemSelected.setChecked(checkStates[position]);
        return convertView;
    }
    public void receiveEventDeleted(List<RecordFile> newList){
        //delete element in data and update listview
        //newList la danh sach da duoc cap nhap sau khi xoa
        listData = newList;
        checkStates = new boolean[newList.size()];
        notifyDataSetChanged();
    }
    public void reciveEventSelectAll(){
        for(int i = 0; i < checkStates.length; i++){
            checkStates[i] = true;
        }
        listFileName.clear();
        for(int i = 0; i < listData.size(); i++){
            listFileName.add(listData.get(i).getName());
        }
        MainActivity.eventBackFromListView(listFileName);
        notifyDataSetChanged();
    }


    static class ViewHolder2 {
        TextView fileName;
        TextView sizeFile;
        CheckBox itemSelected;
    }
}
