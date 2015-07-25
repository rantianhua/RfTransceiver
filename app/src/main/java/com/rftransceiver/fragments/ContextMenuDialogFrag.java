//package com.rftransceiver.fragments;
//
//import android.content.res.Resources;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewTreeObserver;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.ListView;
//
//import com.rftransceiver.R;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import butterknife.ButterKnife;
//import butterknife.InjectView;
//
///**
// * Created by rth on 15-7-15.
// */
//public class ContextMenuDialogFrag extends BaseDialogFragment {
//    @InjectView(R.id.list_context_menu)
//    ListView listView;
//
//    private int width,topMargin,scrennWidth,screenHeight;
//
//    private boolean haveTopMargin = false;
//
//    private  List<String> menus;
//
//    private String textMute,textCancelMenu;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Resources res = getResources();
//        float density = res.getDisplayMetrics().density;
//        scrennWidth = res.getDisplayMetrics().widthPixels / 2;
//        screenHeight = res.getDisplayMetrics().heightPixels;
//
//        width = (int) (120 * density + 0.5f);
//        scrennWidth = scrennWidth + width / 2;
//        topMargin = (int) (50 * density + 0.5f);
//
//        textMute = getString(R.string.mute);
//        textCancelMenu = getString(R.string.cancel_mute);
//
//        menus = new ArrayList<>();
//        menus.add(getString(R.string.reset_scm));
//        menus.add(getString(R.string.see_group));
//        menus.add(textMute);
//
//    }
//
//    @Override
//    public View initContentView(LayoutInflater inflater) {
//        final View view = inflater.inflate(R.layout.dialogfrag_contextmenu,null);
//        ButterKnife.inject(this, view);
//
//        listView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.context_menu_item, R.id.tv_context_menu, menus
//        ));
//
//        ViewTreeObserver observer = view.getViewTreeObserver();
//        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                if (!haveTopMargin) {
//                    int height = view.getMeasuredHeight();
//                    screenHeight = (screenHeight - height) / 2;
//                    topMargin = topMargin - screenHeight;
//
//                    Window window = getDialog().getWindow();
//                    WindowManager.LayoutParams lp = window.getAttributes();
//                    lp.width = width;
//                    lp.x = scrennWidth - width - 5;
//                    lp.y = topMargin + 20;
//                    window.setAttributes(lp);
//                    haveTopMargin = true;
//                }
//                return true;
//            }
//        });
//
//        return view;
//    }
//
//    @Override
//    public void initEvent() {
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//
//                if(getTargetFragment() != null) {
//                    getTargetFragment().onActivityResult(HomeFragment.REQUEST_CONTEXT_MENU,
//                            i,null);
//                }
//
//                if(i == 2) {
//                    if(menus.get(i).equals(textMute)) {
//                        menus.remove(i);
//                        menus.add(textCancelMenu);
//                    }else {
//                        menus.remove(i);
//                        menus.add(textMute);
//                    }
//                    ArrayAdapter arrayAdapter = (ArrayAdapter)listView.getAdapter();
//                    arrayAdapter.notifyDataSetChanged();
//                }
//
//                ContextMenuDialogFrag.this.dismiss();
//            }
//        });
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        haveTopMargin = false;
//    }
//}
