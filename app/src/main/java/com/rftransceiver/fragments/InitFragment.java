package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
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
import com.rftransceiver.util.ImageUtil;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-1.
 * this fragment used to setting some content such as user's nickname, the app's password
 */
public class InitFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.img_init_camera)
    ImageView camera;
    @InjectView(R.id.img_cancel_input)
    ImageView ibnCancel;
    @InjectView(R.id.btn_init_ok)
    Button btnOk;
    @InjectView(R.id.et_limit_input)
    EditText etNickName;
    @InjectView(R.id.tv_input_length)
    TextView tvCounter;

    private final int etSize = 10;  //昵称的最大长度
    private static final int REQUEST_IMAGE_CPTURE = 200;    //请求系统拍照的代码
    private static final int RESULT_LOAD_IMAGE = 201;    //请求图库的代码

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

        ibnCancel.setImageResource(R.drawable.cancel);
        etNickName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(etSize)});
        etNickName.setTextColor(Color.WHITE);
        etNickName.setHint(R.string.hint_et_nickname);
        etNickName.addTextChangedListener(watcher);

        tvCounter.setText(0 + "/" + etSize);
        tvCounter.setTextColor(Color.WHITE);
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
                saveBaseInfo(nickname,photoPath,getActivity().getSharedPreferences(Constants.SP_USER,0));
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
                etNickName.setText("");
                break;
            case R.id.img_cancel_input:
                etNickName.setText("");
                break;
            case R.id.img_init_camera:
                chooseAction();
                break;
            default:
                break;
        }
    }

    /**
     * 选择获取图片的方式
     */
    private void chooseAction() {
        new AlertDialog.Builder(getActivity()).setItems(new String[]{"打开图库", "拍一张"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0) {
                    //打开系统图库
                    openGallery();
                }else if(i == 1){
                    //打开系统相机
                    openCamera();
                }
            }
        }).show();
    }

    /**
     * 打开系统图库
     */
    private void openGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(i.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //确保进程能够获取返回的intent
        if(takePicture.resolveActivity(getActivity().getPackageManager()) != null) {
            photoPath = getActivity().getExternalFilesDir(null)+Constants.PHOTO_NAME;
            Uri imageUri = Uri.fromFile(new File(photoPath));
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePicture, REQUEST_IMAGE_CPTURE);
        }
    }

    /**
     *
     * @param nickname
     * 保存昵称
     */
    public static void saveBaseInfo(String nickname,String photoPath,SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();
        if(!TextUtils.isEmpty(nickname)){
            editor.putString(Constants.NICKNAME,nickname);
        }
        if(!TextUtils.isEmpty(photoPath)) {
            editor.putString(Constants.PHOTO_PATH, photoPath);
        }
        editor.apply();
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
        if(requestCode == REQUEST_IMAGE_CPTURE && resultCode == Activity.RESULT_OK) {
            //显示图片
            showBitmap();
        }else if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            photoPath = ImageUtil.getImgPathFromIntent(data,getActivity());
            showBitmap();
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 显示获取到的图片
     */
    private void showBitmap() {
        if(photoPath == null) return;
        int size = (int)(getResources().getDisplayMetrics().density * 100 + 0.5f);
        Bitmap bitmap = ImageUtil.createImageThumbnail(photoPath,size * size);
        if(bitmap != null) {
            CircleImageDrawable drawable = new CircleImageDrawable(bitmap);
            setPhoto(drawable);
        }
    }
}
