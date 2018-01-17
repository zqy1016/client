package com.qiyukf.openapi.controller.wxservice;

import com.alibaba.fastjson.JSONObject;
import com.qiyukf.openapi.controller.Constants;
import com.qiyukf.openapi.controller.wxutil.EmojiConverter;
import com.qiyukf.openapi.session.model.QiyuMessage;
import com.qiyukf.openapi.session.util.HttpClientPool;
import com.qiyukf.openapi.session.util.MD5;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;

/**
 * Created by zhoujianghua on 2015/10/24.
 */
@Service("wxMsgService")
public class WxMessageService {

    private static final String TAG_TO_USER = "touser";
    private static final String TAG_MSG_TYPE = "msgtype";
    private static final String TAG_AGENTID = "agentid";
    private static final int defaultRetryTimes = 2;
    private static final String sendRetryQueue = "mq_send_retry_queue";

    private static Logger logger = Logger.getLogger(WxMessageService.class);

    private static final int MAX_BYTES_LIMIT = 2000;

    @Autowired
    private WXAuthService wxAuthService;

    @Autowired
    private EmojiConverter emojiConverter;

    public void handleMessage(String openId, String content, String msgType) throws IOException{
        if (TextUtils.isEmpty(content)) {
            return;
        }
        switch (msgType){
            case QiyuMessage.TYPE_TEXT:{
                sendText(openId, content);
            }
            break;
            case QiyuMessage.TYPE_AUDIO:{
                String mediaType = "voice";
                String mediaId = getMediaId(content,mediaType);
                sendMedia(openId,mediaId,mediaType);
            }
            break;
            case QiyuMessage.TYPE_PICTURE:{
                String mediaType = "image";
                String mediaId = getMediaId(content,mediaType);
                sendMedia(openId,mediaId,mediaType);
            }
            break;
        }
    }

    /**
     * 这个方法已废弃，但仍有部分调用，同时考虑到直接回复文字需要，故保留
     * @param openId
     * @param text
     * @throws IOException
     */
    public void replyText(String openId, String text) throws IOException {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        sendText(openId, text);
    }

    private String getMediaId(String content, String mediaType) throws IOException{

        //根据content里的url下载图片
        JSONObject json = JSONObject.parseObject(content);
        byte[] buffer = HttpClientPool.getInstance().downloadFile(json.getString("url"), 30 * 1000);
        if (buffer == null) {
            throw new IOException("download qy file error");
        }

        //将下载的图片上传至微信服务器，获取media_id
        String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=%s", wxAuthService.queryAccessToken(), mediaType);
        String ret = HttpClientPool.getInstance().uploadFileToWx(url,buffer,returnRandomName());
        if (ret==null){
            throw new IOException("upload wx file error");
        }
        JSONObject jsonRes = JSONObject.parseObject(ret);
        return jsonRes.getString("media_id");
    }

    private void sendMedia(String openId, String meidaId, String msgType) throws IOException {
        JSONObject body = new JSONObject();
        body.put("media_id", meidaId);

        JSONObject json = new JSONObject();
        json.put(TAG_MSG_TYPE, msgType);
        json.put(TAG_TO_USER, openId);
        json.put(TAG_AGENTID, Constants.WX_AGENT_ID);
        json.put(msgType, body);

        String sendStr = json.toJSONString();

        replyMessage(sendStr, "replyText");
    }

    private void sendText( String openId, String text) {
        JSONObject body = new JSONObject();
        body.put("content", emojiConverter.convertNim(text));

        JSONObject json = new JSONObject();
        json.put(TAG_TO_USER, openId);
        json.put(TAG_MSG_TYPE, "text");
        json.put(TAG_AGENTID, Constants.WX_AGENT_ID);
        json.put("text", body);

        String sendStr = json.toJSONString();

        replyMessage(sendStr, "replyText");
    }

    private String msgUrl() {
        return Constants.WX_MSG_URL + "?access_token=" + wxAuthService.queryAccessToken();
    }

    private void replyMessage(String sendStr, String func) {

        String msgUrl = msgUrl();
        try {
            String ret = null;
            for (int i = 0; i <= defaultRetryTimes; i++) {
                try {
                    ret = HttpClientPool.getInstance().post(msgUrl, sendStr);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    logger.warn("post request exception: " + ex);
                }
                if (ret != null) break;
            }

            if (ret == null) {
                logger.warn(String.format("[wx] failed and retry !! sendStr = %s", sendStr));
            }
            logger.debug(String.format("[%s] url=%s, send=%s, ret=%s", func, msgUrl, sendStr, ret));
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.warn("replyMessage error: " + ex.toString());
        }
    }

    private String  returnRandomName(){
        long randomInt = new Random().nextInt()+System.currentTimeMillis();
        return MD5.md5(randomInt + "");
    }

}
