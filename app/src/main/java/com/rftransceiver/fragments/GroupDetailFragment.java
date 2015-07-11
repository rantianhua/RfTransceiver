package com.rftransceiver.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-28.
 */
public class GroupDetailFragment extends Fragment {

    @InjectView(R.id.img_top_left)
    ImageView imgBack;
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;
    @InjectView(R.id.grid_members)
    GridView gridView;
    @InjectView(R.id.tv_group_name)
    TextView tvGroupName;
    @InjectView(R.id.tv_group_channel)
    TextView tvChannel;
    @InjectView(R.id.img_switch_add_contacts)
    ImageView imgContacts;
    @InjectView(R.id.img_switch_group_sounds)
    ImageView imgSounds;
    @InjectView(R.id.btn_clear_chat)
    Button btnClearChat;
    @InjectView(R.id.btn_exit_connect)
    Button btnExitConnect;
    @InjectView(R.id.btn_exit_and_quit)
    Button btnExitAndDelete;

    private GroupEntity groupEntity;
    private String textBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupEntity = getArguments().getParcelable("group");
        textBack = getString(R.string.back);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_detail,container,false);
        initView(view);
        inttEvent();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        imgBack.setImageResource(R.drawable.back);
        tvTitle.setText(textBack);
        gridView.setAdapter(new CommonAdapter<GroupMember>(getActivity(),groupEntity.getMembers(),
                R.layout.grid_item_members) {
            @Override
            public void convert(CommonViewHolder helper, GroupMember item) {
                Drawable drawable = item.getDrawable();
                if(drawable != null) {
                    helper.setImageDrawable(R.id.img_member_photo,drawable);
                }
                helper.setText(R.id.tv_member_name,item.getName());
            }
        });
        tvGroupName.setText(groupEntity.getName());
    }

    private void inttEvent() {
        tvChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        imgContacts.setSelected(true);
        imgContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imgContacts.isSelected()) {
                    imgContacts.setSelected(false);
                }else {
                    imgContacts.setSelected(true);
                }
            }
        });
        imgSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imgSounds.isSelected()) {
                    imgSounds.setSelected(false);
                }else {
                    imgSounds.setSelected(true);
                }
            }
        });
        btnClearChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        btnExitConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        btnExitAndDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    public static GroupDetailFragment getInstance(GroupEntity groupEntity) {
        GroupDetailFragment fragment = new GroupDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("group",groupEntity);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        groupEntity = null;
    }
}
