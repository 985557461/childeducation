package com.xiaoyu.HeartConsultation.chat.chatuidemo.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.easemob.chat.*;
import com.easemob.util.DateUtils;
import com.easemob.util.EMLog;
import com.xiaoyu.HeartConsultation.R;
import com.xiaoyu.HeartConsultation.background.HCApplicaton;
import com.xiaoyu.HeartConsultation.background.UserInfoModel;
import com.xiaoyu.HeartConsultation.background.config.ServerConfig;
import com.xiaoyu.HeartConsultation.chat.chatuidemo.Constant;
import com.xiaoyu.HeartConsultation.chat.chatuidemo.utils.SmileUtils;
import com.xiaoyu.HeartConsultation.chat.chatuidemo.utils.UserUtils;
import com.xiaoyu.HeartConsultation.util.ChatUserInfoUtil;
import com.xiaoyu.HeartConsultation.util.Request;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by xiaoyu on 2015/7/2.
 */
public class ChatAllHistoryAdapter extends ArrayAdapter<EMConversation> {
    private static final String TAG = "ChatAllHistoryAdapter";
    private LayoutInflater inflater;
    private List<EMConversation> conversationList;
    private List<EMConversation> copyConversationList;
    private ConversationFilter conversationFilter;
    private boolean notiyfyByFilter;

    public ChatAllHistoryAdapter(Context context, int textViewResourceId, List<EMConversation> objects) {
        super(context, textViewResourceId, objects);
        this.conversationList = objects;
        copyConversationList = new ArrayList<EMConversation>();
        copyConversationList.addAll(objects);
        inflater = LayoutInflater.from(context);
    }

    ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_chat_history, parent, false);
        }
        holder = (ViewHolder) convertView.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.unreadLabel = (TextView) convertView.findViewById(R.id.unread_msg_number);
            holder.message = (TextView) convertView.findViewById(R.id.message);
            holder.time = (TextView) convertView.findViewById(R.id.time);
            holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
            holder.msgState = convertView.findViewById(R.id.msg_state);
            holder.list_item_layout = (RelativeLayout) convertView.findViewById(R.id.list_item_layout);
            convertView.setTag(holder);
        }
        if (position % 2 == 0) {
            holder.list_item_layout.setBackgroundResource(R.drawable.mm_listitem);
        } else {
            holder.list_item_layout.setBackgroundResource(R.drawable.mm_listitem_grey);
        }

        // 获取与此用户/群组的会话
        EMConversation conversation = getItem(position);
        // 获取用户username或者群组groupid
        final String username = conversation.getUserName();
        if (conversation.getType() == EMConversation.EMConversationType.GroupChat) {
            // 群聊消息，显示群聊头像
            holder.avatar.setImageResource(R.drawable.group_icon);
            EMGroup group = EMChatManager.getInstance().getGroup(username);
            holder.name.setText(group != null ? group.getGroupName() : username);
        } else if (conversation.getType() == EMConversation.EMConversationType.ChatRoom) {
            holder.avatar.setImageResource(R.drawable.group_icon);
            EMChatRoom room = EMChatManager.getInstance().getChatRoom(username);
            holder.name.setText(room != null && !TextUtils.isEmpty(room.getName()) ? room.getName() : username);
        } else {
            final ChatUserInfoUtil.UserInfo userInfo = ChatUserInfoUtil.getUserInfo(username);
            if (userInfo != null) {
                UserUtils.setUserAvatar(getContext(), userInfo.avatar, holder.avatar);
                holder.name.setText(userInfo.name);
            } else {
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("userid", username));
                Request.doRequest(getContext(), pairs, ServerConfig.URL_QUERY_USER_INFO, Request.GET, new Request.RequestListener() {
                    @Override
                    public void onException(Request.RequestException e) {
                        UserUtils.setUserAvatar(getContext(), "", holder.avatar);
                        holder.name.setText(username);
                    }

                    @Override
                    public void onComplete(String response) {
                        UserInfoModel model = HCApplicaton.getInstance().getGson().fromJsonWithNoException(response,UserInfoModel.class);
                        if(model != null && model.result == 1){
                            ChatUserInfoUtil.UserInfo userInfo1 = new ChatUserInfoUtil.UserInfo();
                            userInfo1.avatar = model.imagepath;
                            userInfo1.name = model.nickname;
                            ChatUserInfoUtil.putUserInfo(username, userInfo1);
                            UserUtils.setUserAvatar(getContext(), userInfo1.avatar, holder.avatar);
                            holder.name.setText(userInfo1.name);
                        }else{
                            UserUtils.setUserAvatar(getContext(), "", holder.avatar);
                            holder.name.setText(username);
                        }
                    }
                });
            }

            if (username.equals(Constant.GROUP_USERNAME)) {
                holder.name.setText("qunliao");

            } else if (username.equals(Constant.NEW_FRIENDS_USERNAME)) {
                holder.name.setText("shenqingtongzhi");
            }
        }

        if (conversation.getUnreadMsgCount() > 0) {
            // 显示与此用户的消息未读数
            holder.unreadLabel.setText(String.valueOf(conversation.getUnreadMsgCount()));
            holder.unreadLabel.setVisibility(View.VISIBLE);
        } else {
            holder.unreadLabel.setVisibility(View.INVISIBLE);
        }

        if (conversation.getMsgCount() != 0) {
            // 把最后一条消息的内容作为item的message内容
            EMMessage lastMessage = conversation.getLastMessage();
            holder.message.setText(SmileUtils.getSmiledText(getContext(), getMessageDigest(lastMessage, (this.getContext()))),
                    TextView.BufferType.SPANNABLE);

            holder.time.setText(DateUtils.getTimestampString(new Date(lastMessage.getMsgTime())));
            if (lastMessage.direct == EMMessage.Direct.SEND && lastMessage.status == EMMessage.Status.FAIL) {
                holder.msgState.setVisibility(View.VISIBLE);
            } else {
                holder.msgState.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    /**
     * 根据消息内容和消息类型获取消息内容提示
     *
     * @param message
     * @param context
     * @return
     */
    private String getMessageDigest(EMMessage message, Context context) {
        String digest = "";
        switch (message.getType()) {
            case LOCATION: // 位置消息
                if (message.direct == EMMessage.Direct.RECEIVE) {
                    // 从sdk中提到了ui中，使用更简单不犯错的获取string的方法
                    // digest = EasyUtils.getAppResourceString(context,
                    // "location_recv");
                    digest = getStrng(context, R.string.location_recv);
                    digest = String.format(digest, message.getFrom());
                    return digest;
                } else {
                    // digest = EasyUtils.getAppResourceString(context,
                    // "location_prefix");
                    digest = getStrng(context, R.string.location_prefix);
                }
                break;
            case IMAGE: // 图片消息
                ImageMessageBody imageBody = (ImageMessageBody) message.getBody();
                digest = getStrng(context, R.string.picture) + imageBody.getFileName();
                break;
            case VOICE:// 语音消息
                digest = getStrng(context, R.string.voice);
                break;
            case VIDEO: // 视频消息
                digest = getStrng(context, R.string.video);
                break;
            case TXT: // 文本消息
                if (!message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)) {
                    TextMessageBody txtBody = (TextMessageBody) message.getBody();
                    digest = txtBody.getMessage();
                } else {
                    TextMessageBody txtBody = (TextMessageBody) message.getBody();
                    digest = getStrng(context, R.string.voice_call) + txtBody.getMessage();
                }
                break;
            case FILE: // 普通文件消息
                digest = getStrng(context, R.string.file);
                break;
            default:
                EMLog.e(TAG, "unknow type");
                return "";
        }

        return digest;
    }

    private static class ViewHolder {
        /**
         * 和谁的聊天记录
         */
        TextView name;
        /**
         * 消息未读数
         */
        TextView unreadLabel;
        /**
         * 最后一条消息的内容
         */
        TextView message;
        /**
         * 最后一条消息的时间
         */
        TextView time;
        /**
         * 用户头像
         */
        ImageView avatar;
        /**
         * 最后一条消息的发送状态
         */
        View msgState;
        /**
         * 整个list中每一行总布局
         */
        RelativeLayout list_item_layout;

    }

    String getStrng(Context context, int resId) {
        return context.getResources().getString(resId);
    }


    @Override
    public Filter getFilter() {
        if (conversationFilter == null) {
            conversationFilter = new ConversationFilter(conversationList);
        }
        return conversationFilter;
    }

    private class ConversationFilter extends Filter {
        List<EMConversation> mOriginalValues = null;

        public ConversationFilter(List<EMConversation> mList) {
            mOriginalValues = mList;
        }

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                mOriginalValues = new ArrayList<EMConversation>();
            }
            if (prefix == null || prefix.length() == 0) {
                results.values = copyConversationList;
                results.count = copyConversationList.size();
            } else {
                String prefixString = prefix.toString();
                final int count = mOriginalValues.size();
                final ArrayList<EMConversation> newValues = new ArrayList<EMConversation>();

                for (int i = 0; i < count; i++) {
                    final EMConversation value = mOriginalValues.get(i);
                    String username = value.getUserName();

                    EMGroup group = EMChatManager.getInstance().getGroup(username);
                    if (group != null) {
                        username = group.getGroupName();
                    }

                    // First match against the whole ,non-splitted value
                    if (username.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = username.split(" ");
                        final int wordCount = words.length;

                        // Start at index 0, in case valueText starts with space(s)
                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            conversationList.clear();
            conversationList.addAll((List<EMConversation>) results.values);
            if (results.count > 0) {
                notiyfyByFilter = true;
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }

        }

    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (!notiyfyByFilter) {
            copyConversationList.clear();
            copyConversationList.addAll(conversationList);
            notiyfyByFilter = false;
        }
    }
}
