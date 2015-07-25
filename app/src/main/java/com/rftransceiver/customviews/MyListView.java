package com.rftransceiver.customviews;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import com.rftransceiver.R;

/**
 * Created by rth on 15-7-23.
 */
public class MyListView extends ListView implements AbsListView.OnScrollListener {

    /**
     * header view
     */
    private View header;
    /**
     * the height of header view
     */
    private int headerHeight;
    /**
     * the max height the user can pull down to
     */
    private int maxHeight;

    /**
     * half of maxHeight,if total pull height is larger than the value ,the show
     * set header view's top padding to maxheight,and then set sate to refreshing
     */
    private int readyHeight;
    /**
     * index of first visible item in listview
     */
    private int firstVisibleItem;

    /**
     * current scroll state of listview
     */
    private int scrollState;
    /**
     * as a remark to decide response touch event or not
     */
    private boolean isRemark;

    /**
     * down y coordination
     */
    private int startY;

    /**
     * the loading imageview
     */
    private ImageView loading;

    private AnimationDrawable animationDrawable;


    private State state = State.NONE;// 当前的状态；

    private enum State {
        NONE,
        READY,
        LOADING,
    }

    private ILoadingListener iLoadingListener;

    public MyListView(Context context) {
        super(context);
        initView(context);
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MyListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    /**
     * @param context
     */
    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        header = inflater.inflate(R.layout.chat_list_header, null);
        loading = (ImageView) header.findViewById(R.id.img_header_list);
        animationDrawable = (AnimationDrawable)loading.getDrawable();
        animationDrawable.stop();
        measureView(header);
        headerHeight = header.getMeasuredHeight();
        int loadHeight = loading.getMeasuredHeight();
        maxHeight = (headerHeight - loadHeight) / 2;
        readyHeight = maxHeight / 2;
        topPadding(-headerHeight);
        header.invalidate();
        this.addHeaderView(header);
        this.setOnScrollListener(this);
    }

    /**
     *
     * @param view
     */
    private void measureView(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int height;
        int tempHeight = p.height;
        if (tempHeight > 0) {
            height = MeasureSpec.makeMeasureSpec(tempHeight,
                    MeasureSpec.EXACTLY);
        } else {
            height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(width, height);
    }

    /**
     * set top padding of the header view
     * @param topPadding
     */
    private void topPadding(int topPadding) {
        header.setPadding(header.getPaddingLeft(), topPadding,
                header.getPaddingRight(), header.getPaddingBottom());
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.scrollState = scrollState;
    }

    /**
     * update state and the top padding of header view
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (firstVisibleItem == 0 && state != State.LOADING) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isRemark = true;
                    startY = (int) ev.getY();
                    animationDrawable.start();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(ev);
                break;
            case MotionEvent.ACTION_UP:
                if (state == State.READY) {
                    //after user loose his pointer,the goto loading state
                    state = State.LOADING;
                    //next to load data
                    refreshViewByState();
                    if(iLoadingListener != null) {
                        iLoadingListener.onLoad();
                    }
                } else{
                    //after user loose his pointer,the recover the header view,do nothing
                    state = State.NONE;
                    isRemark = false;
                    refreshViewByState();
                    animationDrawable.stop();
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * get current y coordination and compare to pre y coordination
     * and then update header view
     * @param ev
     */
    private void onMove(MotionEvent ev) {
        if (!isRemark) {
            return;
        }
        int tempY = (int) ev.getY();
        int space = tempY - startY;
        int topPadding = space - headerHeight;
        if (space >= 0) {
            if(topPadding <= maxHeight) topPadding(topPadding);
            if(topPadding >= readyHeight) state = State.READY;  //is pulling down the listView)
        }else {
            state = State.NONE;
            topPadding(topPadding);
        }
    }

    /**
     * change header view by current state
     */
    private void refreshViewByState() {
        switch (state) {
            case NONE:     //hide header view
                topPadding(-headerHeight);
                break;
            case LOADING:
                topPadding(readyHeight);
                break;
        }
    }

    /**
     */
    public void loadComplete() {
        state = State.NONE;
        isRemark = false;
        refreshViewByState();
    }

    public void setInterface(ILoadingListener iLoadingListener){
        this.iLoadingListener = iLoadingListener;
    }

    /**
     * the interface to loading data
     * @author Administrator
     */
    public interface ILoadingListener {
        public void onLoad();
    }
}
