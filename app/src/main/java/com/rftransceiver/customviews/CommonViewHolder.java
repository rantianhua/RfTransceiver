package com.rftransceiver.customviews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.util.ImageLoader;

public class CommonViewHolder
{
	private final SparseArray<View> mViews;
	private int mPosition;
	private View mConvertView;

	private CommonViewHolder(Context context, ViewGroup parent, int layoutId,
                             int position)
	{
		this.mPosition = position;
		this.mViews = new SparseArray<View>();
		mConvertView = LayoutInflater.from(context).inflate(layoutId, parent,
				false);
		// setTag
		mConvertView.setTag(this);
	}

	/**
	 * 拿到一个ViewHolder对象
	 * 
	 * @param context
	 * @param convertView
	 * @param parent
	 * @param layoutId
	 * @param position
	 * @return
	 */
	public static CommonViewHolder get(Context context, View convertView,
			ViewGroup parent, int layoutId, int position)
	{
		if (convertView == null)
		{
			return new CommonViewHolder(context, parent, layoutId, position);
		}
		return (CommonViewHolder) convertView.getTag();
	}

	public View getConvertView()
	{
		return mConvertView;
	}

	/**
	 * 通过控件的Id获取对于的控件，如果没有则加入views
	 * 
	 * @param viewId
	 * @return
	 */
	public <T extends View> T getView(int viewId)
	{
		View view = mViews.get(viewId);
		if (view == null)
		{
			view = mConvertView.findViewById(viewId);
			mViews.put(viewId, view);
		}
		return (T) view;
	}

	/**
	 * 为TextView设置字符串
	 * 
	 * @param viewId
	 * @param text
	 * @return
	 */
	public CommonViewHolder setText(int viewId, String text)
	{
		TextView view = getView(viewId);
		view.setText(text);
		return this;
	}

	/**
	 * 为ImageView设置图片
	 * 
	 * @param viewId
	 * @param drawableId
	 * @return
	 */
	public CommonViewHolder setImageResource(int viewId, int drawableId)
	{
		ImageView view = getView(viewId);
        //if(view != null) view.setImageResource(drawableId);
        view.setImageResource(drawableId);
		return this;
	}

	/**
	 * 为ImageView设置图片
	 * 
	 * @param viewId
	 * @return
	 */
	public CommonViewHolder setImageBitmap(int viewId, Bitmap bm)
	{
        if (bm != null) {
            ImageView view = getView(viewId);
            view.setImageBitmap(bm);
        }
		return this;
	}

    public CommonViewHolder setImageDrawable(int viewId, Drawable drawable)
    {
        if(drawable != null) {
            ImageView view = getView(viewId);
            view.setImageDrawable(drawable);
        }
        return this;
    }

	/**
	 * 为ImageView设置图片
	 * 
	 * @param viewId
	 * @return
	 */
	public CommonViewHolder setImageByUrl(int viewId, String url)
	{
		ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(url,
				(ImageView) getView(viewId));
		return this;
	}
}
