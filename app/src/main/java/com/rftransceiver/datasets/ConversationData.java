package com.rftransceiver.datasets;

import com.rftransceiver.adapter.ListConversationAdapter;

/**
 * Created by Rth on 2015/4/27.
 */
public class ConversationData {

    private String content;

    private ListConversationAdapter.ConversationType conversationType;

    public ListConversationAdapter.ConversationType getConversationType() {
        return conversationType;
    }

    public void setConversationType(ListConversationAdapter.ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
