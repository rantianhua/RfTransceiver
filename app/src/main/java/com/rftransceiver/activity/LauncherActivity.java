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

        String nickName = getSharedPreferences(Constants.SP_USER,0).getString(Constants.NICKNAME,"");
        if(TextUtils.isEmpty(nickName)) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.activity_launcher);

        if(TextUtils.isEmpty(nickName)) {
            final InitFragment initFragment = new InitFragment();
            initFragment.setListener(new InitFragment.OnNicknameFinished() {
                @Override
                public void finishNicname() {
                    Toast.makeText(LauncherActivity.this,"finish nickname",Toast.LENGTH_SHORT).show();
                    initFragment.setListener(null);
                }
            });
            getFragmentManager().beginTransaction()
                    .add(R.id.container_launcher,initFragment)
                    .commit();
        }else {

        }
    }

}
