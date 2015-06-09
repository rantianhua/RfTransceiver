package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.fragments.InitFragment;
import com.rftransceiver.util.Constants;

/**
 * Created by rantianhua on 15-6-1.
 */
public class LauncherActivity extends Activity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launcher);

        getFragmentManager().beginTransaction()
                .add(R.id.frame_content,new InitFragment())
                .commit();
    }

}
