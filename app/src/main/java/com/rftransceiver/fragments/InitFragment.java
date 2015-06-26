package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.util.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-1.
 * this fragment used to setting some content such as user's nickname, the app's password
 */
public class InitFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.img_init_camera)
    ImageView camera;
    @InjectView(R.id.ibn_init_cancel)
    ImageButton ibnCancel;
    @InjectView(R.id.btn_init_ok)
    Button btnOk;
    @InjectView(R.id.et_init_nickname)
    EditText etNickName;
    @InjectView(R.id.tv_init_counter)
    TextView tvCounter;

    private final int etSize = 10;
    public static final int REQUEST_FRAGMENT = 200;

    private String photoPath;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_init,container,false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        camera.setOnClickListener(this);
        ibnCancel.setOnClickListener(this);
        btnOk.setOnClickListener(this);
        etNickName.addTextChangedListener(watcher);
        tvCounter.setText(0+"/"+etSize);
    }

    /**
     * the text watcher to update tvCounter
     */
    private TextWatcher watcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            tvCounter.setText(editable.toString().length()+"/"+etSize);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_init_ok:
                String nickname = etNickName.getText().toString();
                if(TextUtils.isEmpty(nickname)) {
                    Toast.makeText(getActivity(),"请输入昵称",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(photoPath)) {
                    Toast.makeText(getActivity(),"请设置头像",Toast.LENGTH_SHORT).show();
                    return;
                }
                saveBaseInfo(nickname);
                etNickName.setText("");
                break;
            case R.id.ibn_init_cancel:
                etNickName.setText("");
                break;
            case R.id.img_init_camera:
                changeFragment(ImagesFragment.getInstance(REQUEST_FRAGMENT));
                break;
            default:
                break;
        }
    }

    /**
     *
     * @param fragment the ImagesFragment to get user photo
     */
    private void changeFragment(Fragment fragment) {
        fragment.setTargetFragment(this,REQUEST_FRAGMENT);
        getFragmentManager().beginTransaction().
                replace(R.id.frame_content_launcher,fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     *
     * @param nickname
     * save user base info
     */
    private void saveBaseInfo(String nickname) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Constants.SP_USER,0).edit();
        editor.putString(Constants.NICKNAME,nickname);
        editor.putString(Constants.PHOTO_PATH,photoPath);
        editor.apply();
        startActivity(new Intent(getActivity(), MainActivity.class));
        getActivity().finish();
    }

    /**
     * set photo for user
     * @param drawable the round drawable
     */
    private void setPhoto(CircleImageDrawable drawable) {
        camera.setScaleType(ImageView.ScaleType.CENTER_CROP);
        camera.setImageDrawable(drawable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_FRAGMENT && resultCode == Activity.RESULT_CANCELED) {
            getFragmentManager().popBackStackImmediate();
            photoPath = data.getStringExtra(Constants.PHOTO_PATH);
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            CircleImageDrawable drawable = new CircleImageDrawable(bitmap);
            setPhoto(drawable);
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
