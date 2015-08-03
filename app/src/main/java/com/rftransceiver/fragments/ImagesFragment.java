package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.adapter.ImagesAdapter;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.ImageDirsPopWindow;
import com.rftransceiver.datasets.ImageFolderData;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageLoader;
import com.rftransceiver.util.ImageUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-11.
 * this fragment displays all pictures in device
 */
public class ImagesFragment extends Fragment implements ImageDirsPopWindow.OnPictureDirSelected{

    @InjectView(R.id.grid_images)
    GridView gridImages;
    @InjectView(R.id.tv_images_all_pics)
    TextView tvAllPics;
    @InjectView(R.id.rl_images_bottom)
    RelativeLayout bottom;
    @InjectView(R.id.tv_sure_images)
    TextView tvSure;

    private File maxFolder; //to represent max image folder
    private int maxImgCouns; //to represent max count in the folder
    private List<ImageFolderData> imageFolders; //save all image folders
    private ImagesAdapter adapter;
    private ImageDirsPopWindow imageDirsPopWindow;
    private int screenHeight;
    private int selectDirIndex; //selected dir index
    private ImageView selecteImage; //the reference of selected Imageview in the gridView
    private String selectImgPath;   //save selected image's path in gridView

    public static final int REQUEST_IMAGE_CPTURE = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageFolders = new ArrayList<>();
        screenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_iamgs,container,false);
        initView(v);
        iniEvents();
        loadImages();
        return v;
    }

    private void iniEvents() {
        gridImages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0) {
                    openCamera();
                }else{
                   if(selecteImage != null) {
                       selecteImage.setColorFilter(null);
                   }
                    selecteImage = (ImageView) view
                            .findViewById(R.id.img_grid_images);
                    selecteImage.setColorFilter(Color.parseColor("#99000000"));
                    selectImgPath = adapter.getDirPath()+"/"+adapter.getItem(i);
                    tvSure.setClickable(true);
                }
            }
        });
        tvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(selectImgPath)) {
                    pictureFinished(selectImgPath);
                }
            }
        });
        tvSure.setClickable(false);
    }

    //open camera to take a picture
    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //ensure the process can handle with the return intent
        if(takePicture.resolveActivity(getActivity().getPackageManager()) != null) {
            String takePhotoPath = getActivity().getExternalFilesDir(null) + Constants.PHOTO_NAME;
            Uri imageUri = Uri.fromFile(new File(takePhotoPath));
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePicture, REQUEST_IMAGE_CPTURE);
            takePhotoPath = null;
        }
    }


    private void initView(View v) {
        ButterKnife.inject(this,v);
    }

    /**
     * find all images in the device
     */
    private void loadImages() {
        new LoadImagesAsync().execute();
    }

    /**
     * search in background
     */
    class LoadImagesAsync extends AsyncTask<Void,Void,Void> {
        private HashSet<String> dirPaths = new HashSet<>();;   //the set save all folders

        @Override
        protected Void doInBackground(Void... voids) {
            Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor cursor = resolver.query(imagesUri,new String[]{MediaStore.Images.Media.DATA},
                    null,null,
                    MediaStore.Images.Media.DATE_MODIFIED);
            if(cursor == null) return null;
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                File parentFile = new File(path).getParentFile();
                if(parentFile == null) {
                    continue;
                }
                //get dir path
                String dirPath = parentFile.getAbsolutePath();
                if(dirPaths.contains(dirPath)) {
                    continue;
                }
                dirPaths.add(dirPath);
                //init ImageFolderData
                if(parentFile.list() == null) continue;
                ImageFolderData imageFolderData = new ImageFolderData();
                imageFolderData.setAbPath(dirPath);
                String[] pathes = null;
                try {
                    pathes = parentFile.list(filenameFilter);
                }catch (Exception e) {
                    continue;
                }
                if(pathes == null) continue;
                imageFolderData.setPaths(pathes);
                imageFolders.add(imageFolderData);
                int picSize = imageFolderData.getCounts();
                if(picSize > maxImgCouns) {
                    maxImgCouns = picSize;
                    maxFolder = parentFile;
                }
            }

            //complete the scan
            cursor.close();
            dirPaths = null;
            return null;
        }

        final FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.endsWith(".jpg") || s.endsWith(".jpeg")
                        || s.endsWith(".png")) {
                    return true;
                }
                return false;
            }
        };

        @Override
        protected void onPostExecute(Void aVoid) {
            for(int i = 0; i < imageFolders.size();i++) {
                if(imageFolders.get(i).getAbPath().equals(maxFolder.getAbsolutePath())) {
                    imageFolders.get(i).setSelected(true);
                    selectDirIndex = i;
                    adapter = new ImagesAdapter(maxFolder.getAbsolutePath(),
                            imageFolders.get(i).getPaths(),getActivity(),R.layout.grid_item_images);
                    gridImages.setAdapter(adapter);
                    break;
                }
            }
            //init bottom view
            initPopwindow();
        }
    }

    private void initPopwindow() {
        imageDirsPopWindow = new ImageDirsPopWindow(LayoutInflater.from(getActivity())
                .inflate(R.layout.popwindow_img_dirs, null),
                ViewGroup.LayoutParams.MATCH_PARENT,(int)(screenHeight*0.6),imageFolders);

        imageDirsPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.alpha = 1.0f;
                getActivity().getWindow().setAttributes(lp);
            }
        });
        imageDirsPopWindow.setPictureDirSelected(this);
        tvAllPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show popwindow
                imageDirsPopWindow.setAnimationStyle(R.style.anim_popup_dir);
                imageDirsPopWindow.showAsDropDown(bottom,0,0);
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.alpha = 0.3f;
                getActivity().getWindow().setAttributes(lp);
            }
        });
    }

    @Override
    public void dirSelected(ImageFolderData data,int selectId) {
        if(selectId == selectDirIndex) {
            imageDirsPopWindow.dismiss();
            return;
        }

        imageFolders.get(selectDirIndex).setSelected(false);
        imageFolders.get(selectId).setSelected(true);
        selectDirIndex = selectId;

        adapter = new ImagesAdapter(data.getAbPath(),data.getPaths(),getActivity(),R.layout.grid_item_images);
        gridImages.setAdapter(adapter);
        imageDirsPopWindow.dismiss();
    }

    private void pictureFinished(String path) {
        if(getTargetFragment() != null) {
            Intent intent = new Intent();
            intent.putExtra(Constants.PHOTO_PATH,path);
            getTargetFragment().onActivityResult(getArguments().
                    getInt("request"),Activity.RESULT_CANCELED,
                    intent);
            intent = null;
        }
    }

    public static ImagesFragment getInstance(int request){
        ImagesFragment fragment = new ImagesFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("request",request);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ImagesFragment.REQUEST_IMAGE_CPTURE && resultCode == Activity.RESULT_OK) {
           pictureFinished(getActivity().getExternalFilesDir(null)+Constants.PHOTO_NAME);
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //回收bitmap
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).recycleBitmap();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(imageDirsPopWindow != null) {
            imageDirsPopWindow.setPictureDirSelected(null);
        }
        imageDirsPopWindow = null;
        imageFolders = null;
        adapter = null;
        selecteImage = null;
    }
}
