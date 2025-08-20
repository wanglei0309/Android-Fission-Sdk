package com.szfission.wear.demo.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.fission.wear.sdk.v2.constant.AiChatSpKey;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.session.ChatSession;
//import com.fission.wear.sdk.v2.session.ChatSessionCallback;
import com.fission.wear.sdk.v2.utils.AFlashChatGptUtils;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.RtkChatGptManage;
import com.realsil.sdk.core.bluetooth.GlobalGatt;
import com.realsil.sdk.core.utility.DataConverter;
import com.realtek.sdk.media.AudioCodecListener;
import com.realtek.sdk.media.MediaDecoder;
import com.realtek.sdk.media.RtkMediaCodec;
import com.realtek.sdk.media.RtkMediaManager;
import com.realtek.sdk.media.opus.OpusDecoder;
import com.realtek.sdk.wear.WearConfigure;
import com.realtek.sdk.wear.WearManager;
import com.realtek.sdk.wear.applicationlayer.ai.AiConstants;
import com.realtek.sdk.wear.applicationlayer.ai.ChatRequestVoiceDataParameters;
import com.realtek.sdk.wear.applicationlayer.ai.ChatResponseDataTransferBufferControl;
import com.realtek.sdk.wear.applicationlayer.ai.ChatResponseTextDataPacket;
import com.realtek.sdk.wear.applicationlayer.ai.ChatResponseVoiceDataStartPacket;
import com.realtek.sdk.wear.applicationlayer.ai.ChatResponseVoiceRealtimeSender;
import com.realtek.sdk.wear.applicationlayer.ai.ChatResponseVoiceSender;
import com.realtek.sdk.wear.applicationlayer.ai.ReplayChatResponseData;
import com.realtek.sdk.wear.applicationlayer.ai.WhisperResponse;
import com.realtek.sdk.wear.callback.WearAiCallback;
import com.realtek.sdk.wear.callback.WearManagerCallback;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.chat.fixtures.MessagesFixtures;
import com.szfission.wear.demo.chat.model.Message;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StyledMessagesActivity extends DemoMessagesActivity
        implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        DateFormatter.Formatter {


    private MessagesList messagesList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_styled_messages);

        init();

        messagesList = findViewById(R.id.messagesList);
        initAdapter();

        MessageInput input = findViewById(R.id.input);
        input.setInputListener(this);
        input.setAttachmentsListener(this);
    }

    @Override
    public boolean onSubmit(CharSequence input) {
        messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(input.toString()), true);

        return true;
    }

    @Override
    public void onAddAttachments() {
        messagesAdapter.addToStart(MessagesFixtures.getVoiceMessage(), true);
    }

    @Override
    public String format(Date date) {
        if (DateFormatter.isToday(date)) {
            return getString(R.string.date_header_today);
        } else if (DateFormatter.isYesterday(date)) {
            return getString(R.string.date_header_yesterday);
        } else {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
        }
    }

    private void init(){
        RtkChatGptManage.getInstance().init(this);

        AFlashChatGptUtils.getInstance().setGptAiVoiceListener(new AFlashChatGptUtils.GptAiVoiceListener() {
            @Override
            public void onChat(String question, String answer) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messagesAdapter.addToStart(
                                MessagesFixtures.getTextMessage(question), true);
                        messagesAdapter.addToStart(
                                MessagesFixtures.getTextMessage(answer), true);
                    }
                });
                if(!TextUtils.isEmpty(question)){
                    RtkChatGptManage.getInstance().sendQuestion(question);
                }
                if(!TextUtils.isEmpty(answer)){
                    RtkChatGptManage.getInstance().sendAnswer(answer);
                }
            }

            @Override
            public void onCreateDial(List<String> imgPaths) {

            }

            @Override
            public void onSpeechResult(String result, String type) {

            }

            @Override
            public void onError(int code, String msg) {

            }
        });
    }

    private void initAdapter() {
        super.messagesAdapter = new MessagesListAdapter<>(super.senderId, super.imageLoader);
        super.messagesAdapter.enableSelectionMode(this);
        super.messagesAdapter.setLoadMoreListener(this);
        super.messagesAdapter.setDateHeadersFormatter(this);
        messagesList.setAdapter(super.messagesAdapter);

        messagesAdapter.setOnMessageClickListener(new MessagesListAdapter.OnMessageClickListener<Message>() {
            @Override
            public void onMessageClick(Message message) {

            }
        });
    }

}
