package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.fragments.ImagesFragment;
import com.rftransceiver.fragments.InitFragment;
import com.rftransceiver.util.Constants;

import org.w3c.dom.Text;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-1.
 */
public class LauncherActivity extends Activity{

    InitFragment initFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        String name = getSharedPreferences(Constants.SP_USER,0).getString(
                Constants.NICKNAME,""
        );
        if(TextUtils.isEmpty(name)) {
            //first launch
            initFragment = new InitFragment();
            getFragmentManager().beginTransaction().add(R.id.frame_content_launcher,initFragment)
                    .commit();
        }else {
            //have set nickname
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

}
