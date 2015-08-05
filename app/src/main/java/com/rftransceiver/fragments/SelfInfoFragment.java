package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.rftransceiver.R;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.activity.SettingActivity;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;


import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Wuyang on 2015/7/30.
 */
public class SelfInfoFragment extends Fragment {

    @InjectView(R.id.img_head_selfinfo)
    ImageView imgHead;
    @InjectView(R.id.ed_name_selfinfo)
    EditText edName;
    @InjectView(R.id.btn_confirm_selfinfo)
    Button btnConfirm;
    private RelativeLayout content;
    private Drawable dwHead,dwClean,dwEdit;
    private String name;
    private RelativeLayout.LayoutParams lp;
    private float dentisy;
    //标识是否可以修改名称
    private boolean changeInfo = false;
    //背景图片
    private Bitmap backGround;
    private String photoPath;
    private static final int REQUEST_IMAGE_CPTURE = 200;    //请求系统拍照的代码
    private static final int RESULT_LOAD_IMAGE = 201;    //请求图库的代码
    public static final int REQUEST_SETTING = 306;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dentisy = getResources().getDisplayMetrics().density;
        dwClean = getResources().getDrawable(R.drawable.cancel1);
        dwEdit = getResources().getDrawable(R.drawable.pen);
        //加载背景图片
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 4;
        backGround = BitmapFactory.decodeResource(getResources(), R.drawable.chatbackground, op);
        Constants.INVO = -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        content = (RelativeLayout) inflater.inflate(R.layout.fragment_selfnfo,container,false);
        initVierw(content);
        initEvent();
        return content;
    }

    private void initVierw(View view) {

        ButterKnife.inject(this, view);
        content.setBackground(new BitmapDrawable(backGround));
        if(dwHead != null) {
            //展示头像
            imgHead.setImageDrawable(dwHead);
            imgHead.requestFocus();
        }
        if(!TextUtils.isEmpty(name)) {
            //展示用户�?
            edName.setText(name);
        }

        lp = (RelativeLayout.LayoutParams) edName.getLayoutParams();
        //������ʾ��edName�Ҳ����ΪdwClean��dwEdit��CompoundDrawables��С
        dwClean.setBounds(0, 0, (int) (dentisy * 20 + 0.5f), (int) (dentisy * 20 + 0.5f));
        dwEdit.setBounds(0, 0, (int) (dentisy * 20 + 0.5f), (int) (dentisy * 20 + 0.5f));
        edName.setCompoundDrawables(null, null, dwEdit, null);
        edName.setCompoundDrawablePadding((int) (dentisy * 5 + 0.5f));
        if(!changeInfo) {
            //设置EditText不能编辑
            edName.setClickable(false);
            edName.setFocusable(false);
            edName.setEnabled(false);
        }
    }

    public void initEvent() {
        imgHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (changeInfo) {//能编辑状态下进行更改头像
                    chooseAction();
                }

            }
        });
        edName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                    lp.setMargins(150, 0, 0, 0);
                    edName.setLayoutParams(lp);
                    edName.setGravity(Gravity.LEFT);
                    btnConfirm.setVisibility(View.VISIBLE);
                    if (edName.length() != 0) {
                        edName.setCompoundDrawables(null, null, dwClean, null);
                    }
                } else {
                    lp.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    lp.setMargins(0, 0, 0, 0);
                    edName.setLayoutParams(lp);
                    edName.setGravity(Gravity.CENTER);
                    edName.setCompoundDrawables(null, null, dwEdit, null);
                }
            }
        });
        edName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edName.length() != 0)
                    edName.setCompoundDrawables(null, null, dwClean, null);
                else
                    edName.setCompoundDrawables(null, null, null, null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        edName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (edName.hasFocus() && event.getRawX() > edName.getX() + edName.getWidth() - dentisy * 30 + 0.5f)
                    edName.setText("");
                return false;
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                edName.clearFocus();
                imgHead.requestFocus();
                btnConfirm.setVisibility(View.INVISIBLE);
                String newName = edName.getText().toString();
                saveBaseInfo(newName, photoPath, getActivity().getSharedPreferences(Constants.SP_USER, 0));

                setName(newName);
                if (!newName.equals(name)) {
                    if (getTargetFragment() != null) {
                        Intent intent = new Intent();
                        intent.putExtra("name", name);
                        getTargetFragment().onActivityResult(SettingFragment.REQUEST_CHANGEINFO,
                                Activity.RESULT_OK, intent);
                    }
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(backGround != null) {
            backGround.recycle();
        }
    }


    /**
     * 设置头像
     * @param head
     */
    public void setHead(Drawable head){
        this.dwHead=head;
    }

    /**
     * 设置名称
     * @param name
     */
    public void setName(String name){
        this.name=name;
    }

    /**
     * 设置是否可以修改个人信息
     * @param changeInfo
     */
    public void setChangeInfo(boolean changeInfo) {
        this.changeInfo = changeInfo;
    }
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
    private void openGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(i.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }
    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //确保进程能够获取返回的intent
        if(takePicture.resolveActivity(getActivity().getPackageManager()) != null) {
            photoPath = getActivity().getExternalFilesDir(null)+ Constants.PHOTO_NAME;
            Uri imageUri = Uri.fromFile(new File(photoPath));
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePicture, REQUEST_IMAGE_CPTURE);
        }
    }
    private void showBitmap() {
        if(photoPath == null) return;
        int size = (int)(getResources().getDisplayMetrics().density * 100 + 0.5f);
        Bitmap bitmap = ImageUtil.createImageThumbnail(photoPath, size * size);
        if(bitmap != null) {
            CircleImageDrawable drawable = new CircleImageDrawable(bitmap);
            setPhoto(drawable);
        }
    }
    private void setPhoto(CircleImageDrawable drawable) {
        imgHead.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgHead.setImageDrawable(drawable);
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
        btnConfirm.setVisibility(View.VISIBLE);
    }
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
}
