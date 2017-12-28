package com.qiyukf.openapi.session.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.qiyukf.openapi.session.SessionClient;
import com.qiyukf.openapi.session.model.ApplyStaffInfo;
import com.qiyukf.openapi.session.model.ApplyStaffResult;
import com.qiyukf.openapi.session.model.CommonResult;
import com.qiyukf.openapi.controller.wxaes.WXBizMsgCrypt;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
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
        String sToken = "zhouqingyu";
        String sCorpID = "ww58a61eadc9f605f7";
        String sEncodingAESKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String sReqMsgSig = "e7bd548fc9a938c73d0b6870981398b3b69cd2f9";
        // String sReqTimeStamp = HttpUtils.ParseUrl("timestamp");
        String sReqTimeStamp = "1511598098";
        // String sReqNonce = HttpUtils.ParseUrl("nonce");
        String sReqNonce = "1038862710";
        // post请求的密文数据
        // sReqData = HttpUtils.PostData();
        String sReqData = "<xml><ToUserName><![CDATA[ww58a61eadc9f605f7]]></ToUserName><Encrypt><![CDATA[POHiCMwJw7un/P4qcp6EJIhoDPVCgiTVE2HbO0Yc7Fu8+HDq8vfRzRjSHl5pNaRyxKIiQmcPzM43QPncMEzqS1kunZ/tSRrrwo0MOy8R5JSTeRvo1DrtGRZ1cdKo/l/bxdwL9JI2tODnASKvZ2y/iwP6GLsrbPpYUJl9SO4BXCy8RMSpq4PV9gAboFv0T8qi4U/CqjlXOdKHHNwYBp13XmSASzj44ki6d6/gZwJilq5LgElsoFAJqvkPDjRJXeMN/q/uws3VhHOVDY/lxK6qFCW1CBXZm6qq6vnfZ8bivog1BIeuTDDjJQzM1I9ibxeldSUsyQunAb6VGvw1YvoHBT6fugSMYmftqodwGKEMxmJHy+8aOUt7rnUGQNTTQero12/QUZ4Pm0VT7a7PBYxZnVRE4IGGdX/jOLiPRwouQtg=]]></Encrypt><AgentID><![CDATA[1000002]]></AgentID></xml>";

        try {
            WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, sCorpID);

            String sMsg = wxcpt.DecryptMsg(sReqMsgSig, sReqTimeStamp, sReqNonce, sReqData);
            System.out.println("after decrypt msg: " + sMsg);
            // TODO: 解析出明文xml标签的内容进行处理
            // For example:
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader(sMsg);
            InputSource is = new InputSource(sr);
            Document document = db.parse(is);

            Element root = document.getDocumentElement();
            NodeList nodelist1 = root.getElementsByTagName("Content");
            String Content = nodelist1.item(0).getTextContent();
            System.out.println("Content：" + Content);

        } catch (Exception e) {
            // TODO
            // 解密失败，失败原因请查看异常
            e.printStackTrace();
        }

    }

    @Test(priority = 12)
    public void testNew(){
        String content = "abcd1234ABCD";

    }

    @Test(priority = 13)
    public void testWXMsgCrypt(){
        String sToken = "zhouqingyu";
        String sCorpID = "ww58a61eadc9f605f7";
        String sEncodingAESKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
//        WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(sToken, sEncodingAESKey, sCorpID);

        String sEchoStr; //需要返回的明文

        String sVerifyMsgSig = "ab2d5eb465e156abcbe675e02538619e2883611b";
        String sVerifyTimeStamp = "1514352375";
        String sVerifyNonce = "340831515";
        String sVerifyEchoStr = "r21Qw/8bnB3Ie6Xi1B/ddbAPop11Waeg1ion5VH5fpsN7+nvftsGiZjPggJmZtsiLtVe8RzRJn9OiYy957uDgg==";

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
