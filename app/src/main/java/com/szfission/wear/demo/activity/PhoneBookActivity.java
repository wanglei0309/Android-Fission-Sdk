package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.PhoneBook;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

public class PhoneBookActivity extends BaseActivity {

    EditText ed_name1;
    EditText ed_name2;
    EditText ed_name3;
    EditText ed_name4;
    EditText ed_name5;
    EditText ed_name6;
    EditText ed_name7;
    EditText ed_name8;
    EditText ed_name9;
    EditText ed_name10;

    EditText ed_remark1;
    EditText ed_remark2;
    EditText ed_remark3;
    EditText ed_remark4;
    EditText ed_remark5;
    EditText ed_remark6;
    EditText ed_remark7;
    EditText ed_remark8;
    EditText ed_remark9;
    EditText ed_remark10;

    EditText ed_phone1;
    EditText ed_phone2;
    EditText ed_phone3;
    EditText ed_phone4;
    EditText ed_phone5;
    EditText ed_phone6;
    EditText ed_phone7;
    EditText ed_phone8;
    EditText ed_phone9;
    EditText ed_phone10;

    Button btn_set_phone, btn_set_phone_sos, btn_del_phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syn_phone_book);
        setTitle(R.string.FUNC_SYN_PHONE_BOOK);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ed_name1 = findViewById(R.id.ed_name1);
        ed_name2 = findViewById(R.id.ed_name2);
        ed_name3 = findViewById(R.id.ed_name3);
        ed_name4 = findViewById(R.id.ed_name4);
        ed_name5 = findViewById(R.id.ed_name5);
        ed_name6 = findViewById(R.id.ed_name6);
        ed_name7 = findViewById(R.id.ed_name7);
        ed_name8 = findViewById(R.id.ed_name8);
        ed_name9 = findViewById(R.id.ed_name9);
        ed_name10 = findViewById(R.id.ed_name10);

        ed_remark1 = findViewById(R.id.ed_remark1);
        ed_remark2 = findViewById(R.id.ed_remark2);
        ed_remark3 = findViewById(R.id.ed_remark3);
        ed_remark4 = findViewById(R.id.ed_remark4);
        ed_remark5 = findViewById(R.id.ed_remark5);
        ed_remark6 = findViewById(R.id.ed_remark6);
        ed_remark7 = findViewById(R.id.ed_remark7);
        ed_remark8 = findViewById(R.id.ed_remark8);
        ed_remark9 = findViewById(R.id.ed_remark9);
        ed_remark10 = findViewById(R.id.ed_remark10);

        ed_phone1 = findViewById(R.id.ed_phone1);
        ed_phone2 = findViewById(R.id.ed_phone2);
        ed_phone3 = findViewById(R.id.ed_phone3);
        ed_phone4 = findViewById(R.id.ed_phone4);
        ed_phone5 = findViewById(R.id.ed_phone5);
        ed_phone6 = findViewById(R.id.ed_phone6);
        ed_phone7 = findViewById(R.id.ed_phone7);
        ed_phone8 = findViewById(R.id.ed_phone8);
        ed_phone9 = findViewById(R.id.ed_phone9);
        ed_phone10 = findViewById(R.id.ed_phone10);

        btn_set_phone = findViewById(R.id.btn_set_phone);
        btn_set_phone_sos = findViewById(R.id.btn_set_phone_sos);
        btn_del_phone = findViewById(R.id.btn_get_phone);

        btn_set_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPhoneBook();
            }
        });

        btn_set_phone_sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setPhoneBookSos();
            }
        });

        btn_del_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                deletePhoneBook();
            }
        });

        addCmdResultListener(new FissionBigDataCmdResultListener() {
            @Override
            public void sendSuccess(String cmdId) {

            }

            @Override
            public void sendFail(String cmdId) {

            }

            @Override
            public void onResultTimeout(String cmdId) {

            }

            @Override
            public void onResultError(String errorMsg) {
                dismissProgress();
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setPhoneBook() {
        String name1 = ed_name1.getText().toString();
        String name2 = ed_name2.getText().toString();
        String name3 = ed_name3.getText().toString();
        String name4 = ed_name4.getText().toString();
        String name5 = ed_name5.getText().toString();
        String name6 = ed_name6.getText().toString();
        String name7 = ed_name7.getText().toString();
        String name8 = ed_name8.getText().toString();
        String name9 = ed_name9.getText().toString();
        String name10 = ed_name10.getText().toString();

        String remark1 = ed_remark1.getText().toString();
        String remark2 = ed_remark2.getText().toString();
        String remark3 = ed_remark3.getText().toString();
        String remark4 = ed_remark4.getText().toString();
        String remark5 = ed_remark5.getText().toString();
        String remark6 = ed_remark6.getText().toString();
        String remark7 = ed_remark7.getText().toString();
        String remark8 = ed_remark8.getText().toString();
        String remark9 = ed_remark9.getText().toString();
        String remark10 = ed_remark10.getText().toString();

        String phone1 = ed_phone1.getText().toString();
        String phone2 = ed_phone2.getText().toString();
        String phone3 = ed_phone3.getText().toString();
        String phone4 = ed_phone4.getText().toString();
        String phone5 = ed_phone5.getText().toString();
        String phone6 = ed_phone6.getText().toString();
        String phone7 = ed_phone7.getText().toString();
        String phone8 = ed_phone8.getText().toString();
        String phone9 = ed_phone9.getText().toString();
        String phone10 = ed_phone10.getText().toString();

        PhoneBook phoneBook = new PhoneBook(name1, remark1, phone1);
        PhoneBook phoneBook2 = new PhoneBook(name2, remark2, phone2);
        PhoneBook phoneBook3 = new PhoneBook(name3, remark3, phone3);
        PhoneBook phoneBook4 = new PhoneBook(name4, remark4, phone4);
        PhoneBook phoneBook5 = new PhoneBook(name5, remark5, phone5);
        PhoneBook phoneBook6 = new PhoneBook(name6, remark6, phone6);
        PhoneBook phoneBook7 = new PhoneBook(name7, remark7, phone7);
        PhoneBook phoneBook8 = new PhoneBook(name8, remark8, phone8);
        PhoneBook phoneBook9 = new PhoneBook(name9, remark9, phone9);
        PhoneBook phoneBook10 = new PhoneBook(name10, remark10, phone10);

        List<PhoneBook> list = new ArrayList<>();
        list.add(phoneBook);
        list.add(phoneBook2);
        list.add(phoneBook3);
        list.add(phoneBook4);
        list.add(phoneBook5);
        list.add(phoneBook6);
        list.add(phoneBook7);
        list.add(phoneBook8);
        list.add(phoneBook9);
        list.add(phoneBook10);

        FissionSdkBleManage.getInstance().setPhoneBooks(list);
    }

    private void setPhoneBookSos() {
        String name1 = ed_name1.getText().toString();
        String name2 = ed_name2.getText().toString();
        String name3 = ed_name3.getText().toString();

        String remark1 = ed_remark1.getText().toString();
        String remark2 = ed_remark2.getText().toString();
        String remark3 = ed_remark3.getText().toString();

        String phone1 = ed_phone1.getText().toString();
        String phone2 = ed_phone2.getText().toString();
        String phone3 = ed_phone3.getText().toString();

        PhoneBook phoneBook = new PhoneBook(name1, remark1, phone1);
        PhoneBook phoneBook2 = new PhoneBook(name2, remark2, phone2);
        PhoneBook phoneBook3 = new PhoneBook(name3, remark3, phone3);

        List<PhoneBook> list = new ArrayList<>();
        list.add(phoneBook);
        list.add(phoneBook2);
        list.add(phoneBook3);

        FissionSdkBleManage.getInstance().setPhoneBooks(list, true);
    }

    private void deletePhoneBook() {
        List<PhoneBook> list = new ArrayList<>();
        FissionSdkBleManage.getInstance().setPhoneBooks(list);
    }
}
