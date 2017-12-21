package com.qiyukf.openapi.session.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.qiyukf.openapi.session.SessionClient;
import com.qiyukf.openapi.session.model.ApplyStaffInfo;
import com.qiyukf.openapi.session.model.ApplyStaffResult;
import com.qiyukf.openapi.session.model.CommonResult;
import com.qiyukf.openapi.session.util.QiyuPushCheckSum;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
import org.testng.annotations.Test;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zhoujianghua on 2016/10/11.
 */
public class ClientTest {

    private static final String appKey = "065f70b3c5e1e7e35c7b269af3427d7e";
    private static final String foreignId = "zhouqingyu1234";
    private static final String appSecret = "pmiMITDS871DEghh7komcpdgwr2bg895j";

    private SessionClient client = new SessionClient(appKey, appSecret);

    private long sessionId;

    @Test(priority = 1)
    public void testApplyStaff() throws IOException {
        ApplyStaffInfo info = new ApplyStaffInfo();
        info.setFromPage("");
        info.setFromTitle("");
        info.setUid(foreignId);
        info.setProductId("zhouqingyu1234");
        info.setDeviceType("zhouqingyu1234");
        info.setStaffType(1);

        ApplyStaffResult result = client.applyStaff(info);
        if (result.getCode() == 200 && result.getSession().getStaffType() == 1) {
            sessionId = result.getSession().getSessionId();
        }

        out("applyStaff", result);
    }

    @Test(priority = 2)
    public void testSendTextMsg() throws IOException {
        out("send-text-message", client.sendTextMessage(foreignId, "test1"));
        out("text-anti-spam", client.sendTextMessage(foreignId, "习近平"));

        // 超长的消息
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            sb.append(i);
        }
        out("text-too-long", client.sendTextMessage(foreignId, sb.toString()));
    }

    @Test(priority = 3)
    public void testSendImageMsg() throws IOException, NoSuchAlgorithmException {
        out("normal image", client.sendImageMessage(foreignId, "e:/yyy.png", 108, 108));
        out("image without size", client.sendImageMessage(foreignId, "e:/zzz.png", 0, 0));
        out("not image", client.sendImageMessage(foreignId, "e:/xx.txt", 0, 0));
    }

    @Test(expectedExceptions = FileNotFoundException.class, priority = 4)
    public void testSendImageNotExist() throws IOException, NoSuchAlgorithmException {
        out("image not exist", client.sendImageMessage(foreignId, "e:/xeex.txt", 0, 0));
    }

    @Test(priority = 5)
    public void testSendAudioMsg() throws UnsupportedAudioFileException, IOException, LineUnavailableException, NoSuchAlgorithmException {
        out("amr audio", client.sendAudioMessage(foreignId, "e:/xxx.amr", 10000));
        out("wam audio", client.sendAudioMessage(foreignId, "e:/yyy.wma", 150000));
    }

    @Test(expectedExceptions = UnsupportedAudioFileException.class, priority = 6)
    public void testSendAudioMsgNotAudio() throws UnsupportedAudioFileException, IOException, LineUnavailableException, NoSuchAlgorithmException {
        out("testSendAudioMsgNotAudio", client.sendAudioMessage(foreignId, "e:/xx.txt", 0));
    }

    @Test(expectedExceptions = FileNotFoundException.class, priority = 7)
    public void testSendAudioNotExist() throws IOException, LineUnavailableException, UnsupportedAudioFileException, NoSuchAlgorithmException {
        out("testSendAudioNotExist", client.sendAudioMessage(foreignId, "e:/xx44.txt", 0));
    }

    @Test(priority = 8)
    public void testEvaluation() throws IOException {
        if (sessionId != 0) {
            out("testEvaluation", client.evaluate(foreignId, sessionId, 50));
        }
    }

    @Test(priority = 9)
    public void testGetQueueStatus() throws IOException {
        out("getQueueStatus", client.getQueueStatus(foreignId));
    }

    @Test(priority = 10)
    public void testCmrInfo() {
        JSONArray crm = JSONArray.parseArray("[{\"value\": \"abc11@163.com\", \"key\": \"email\"}, {\"index\": 5, \"value\": \"test\", \"key\": \"xyz\", \"label\": \"xyz11\"}]");

        try {
            CommonResult result = client.updateCrmInfo(foreignId, crm);
            out("crminfo", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(priority = 11)
    public void testForwardWxMessage() {

    }

    @Test(priority = 12)
    public void testNew(){
        String content = "abcd1234ABCD";
        String md5 = "abcd1234ABCD";
        long time = 121212;


        String checksum = QiyuPushCheckSum.encode(content, md5, time);

        System.out.println("checksum明文:" + content+md5+time);
        System.out.println("checksum:" + checksum);

/*
        try {

           for(int i=0 ;i<content.getBytes().length;i++){
                System.out.print("Arr["+ i +"]:" + content.getBytes()[i]+",");
            }
            System.out.println();

            MessageDigest messageDigest = MessageDigest.getInstance("sha1");
            messageDigest.update(content.getBytes());
            for(int i=0 ;i<messageDigest.digest().length;i++){
                System.out.print("Arr["+ i +"]:" + messageDigest.digest()[i]+",");
            }
            System.out.println();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
*/
    }

    @Test(priority = 13)
    public void testWXMsgCrypt(){
        String sToken = "zhouqingyu";
        String sCorpID = "ww58a61eadc9f605f7";
        String sEncodingAESKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
//        WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, sCorpID);

        String sEchoStr; //需要返回的明文

        String sVerifyMsgSig = "d0d817051171bdbec7c0c870c183c4b982786903";
        String sVerifyTimeStamp = "1513846766";
        String sVerifyNonce = "196443370";
        String sVerifyEchoStr = "UdGNveUq7feybSntKY2ivmkVRvk9xwD/PPqV4Z55wUipDGi6Gg6GvAz8ZN2GkexANXvMfGG9OqtqnycOiNaL0g==";

        try {
            WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken,sEncodingAESKey,sCorpID);
            sEchoStr = wxcpt.VerifyURL(sVerifyMsgSig, sVerifyTimeStamp,
                    sVerifyNonce, sVerifyEchoStr);
            System.out.println("verifyurl echostr: " + sEchoStr);
            // 验证URL成功，将sEchoStr返回
            // HttpUtils.SetResponse(sEchoStr);
        } catch (Exception e) {
            //验证URL失败，错误原因请查看异常
            e.printStackTrace();
        }
    }

    private void out(String tc, Object result) {
        System.out.println("------" + tc + "------");
        System.out.println(JSON.toJSONString(result));
    }
}
