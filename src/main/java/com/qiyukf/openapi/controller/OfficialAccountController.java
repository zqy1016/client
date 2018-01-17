package com.qiyukf.openapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qiyukf.openapi.controller.wxservice.WXAuthService;
import com.qiyukf.openapi.controller.wxservice.WXUserService;
import com.qiyukf.openapi.controller.wxservice.WxMessageService;
//import com.qiyukf.openapi.controller.wxutil.SHA1;
import com.qiyukf.openapi.controller.wxutil.WXOpenException;
import com.qiyukf.openapi.session.constant.OpenApiTags;
import com.qiyukf.openapi.session.model.ApplyStaffInfo;
import com.qiyukf.openapi.session.model.ApplyStaffResult;
import com.qiyukf.openapi.session.model.CommonResult;
import com.qiyukf.openapi.session.model.Session;
import com.qiyukf.openapi.session.util.StringUtil;
import com.qiyukf.openapi.controller.wxaes.WXBizMsgCrypt;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Async HTTP request controller。
 */
@Controller
@RequestMapping(value = {"/api"}, produces = {"application/json;charset=UTF-8"})
public class OfficialAccountController {

    private Logger logger = Logger.getLogger(OfficialAccountController.class);

    @Autowired
    private QiyuSessionManager sessionManager;

    @Autowired
    private AsyncTaskManager taskManager;

    @Autowired
    private QiyuMessageReceiver qiyuMessageReceiver;

    @Autowired
    private WXAuthService wxAuthService;

    @Autowired
    private WXUserService wxUserService;

    @Autowired
    private WxMessageService wxMessageService;

    @Autowired
    private QiyuSessionService qiyuSessionService;

    private Map<String, String> testHashMap = new HashMap<>();

    /**
     * 处理来自七鱼的HTTP请求
     * @param time
     * @param checksum
     * @param eventType
     * @param is
     * @return
     */
    @RequestMapping(value = "/recv_qiyu", method = RequestMethod.POST)
    @ResponseBody
    public String onUnicornMsg(@RequestParam(value = "time") Long time,
                               @RequestParam(value = "checksum") String checksum,
                               @RequestParam(value = "eventType") String eventType,
                               InputStream is) {
        try {
            String content = StringUtil.isToString(is);
            logger.debug("receive qiyu: " + eventType + " content: " + content);
            return qiyuMessageReceiver.onReceive(time, checksum, eventType, content);
        } catch (Throwable e) {
            logger.warn("onUnicornMsg error: " + e);
        }
        return "";
    }

    /**
     * 处理来自企业微信的HTTP请求。
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echoStr
     * @param request
     * @param is
     * @return
     */
    @RequestMapping(value = "/recv_wx")
    @ResponseBody
    public String onWxMessage(
            @RequestParam(value = "msg_signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echoStr,
            HttpServletRequest request,
            InputStream is) {
        try {
            //这里本应该先验证签名，再判断echostr，但是微信官方给的方法中已封装好验签，所以无需重复验签。
            WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(Constants.WX_TOKEN,Constants.WX_ENCODING_AESKEY,Constants.WX_CORP_ID);

            if (TextUtils.isEmpty(echoStr)) {
                // 收到消息
                String msg = StringUtil.isToString(is);
                //解密消息
                String sMsg = wxcpt.DecryptMsg(signature, timestamp, nonce, msg);
                logger.debug("receive wechat:" + sMsg);
                // 分离处理消息
                return parseWxMessage(sMsg);
            } else {
                //解密echoStr
                String sEchoStr = wxcpt.VerifyURL(signature, timestamp,nonce, echoStr);
                logger.debug("verify url: " + " - " + signature + " - " + echoStr);
                return sEchoStr;
            }
        } catch (Throwable e) {
            logger.warn("onWxMessage error, " + e);
        }
        return Constants.WX_RET_SUCCESS;
    }

    private String parseWxMessage(String xmlContent) throws WXOpenException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader(xmlContent);
            InputSource is = new InputSource(sr);
            Document document = db.parse(is);

            final Element root = document.getDocumentElement();

            final String msgType = xmlTextContent(root, "MsgType");
            if (!"event".equals(msgType)) {
                // 这里用了一个简单的异步任务去实现，真正实现时可考虑使用消息队列等方式
                taskManager.getExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        handleNormalWxMsg(msgType, root);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("parse WXMessage error, " + e);
            throw new WXOpenException(WXOpenException.ParseXmlError);
        }

        return "";
    }

    private void handleNormalWxMsg(String msgType, Element root) {
        String fromUser = xmlTextContent(root, "FromUserName");

        if (!shouldIntercept(fromUser, msgType, root)) {
            try {
                wxUserService.queryWxUserNick(fromUser);
                CommonResult result = qiyuSessionService.forwardWxMessage(root, wxAuthService.queryAccessToken());
                logger.debug("forward message result: " + result);
            } catch (Exception e) {
                logger.debug("forward message error: " + e);
            }
        }
    }

    private boolean shouldIntercept(String fromUser, String msgType, Element root) {
        if ("text".equals(msgType)) {
            String content = xmlTextContent(root, "Content");
            if (("RG".equalsIgnoreCase(content) || "人工".equals(content)|| "人工客服".equals(content)) && !sessionManager.isInSession(fromUser)) {
                //如果fromUser不存在于session中，并且消息内容为RG或人工，则进入请求分配人工客服处理逻辑
                ApplyStaffInfo staffInfo = new ApplyStaffInfo();
                staffInfo.setUid(fromUser);
                staffInfo.setStaffType(1);
                staffInfo.setProductId("公众号APPID");
                staffInfo.setFromTitle("公众号名字");
                try {
                    ApplyStaffResult result = qiyuSessionService.applyStaff(staffInfo);
                    if (result.getCode() == 200) {
                        sessionManager.onSessionStart(result.getSession());
                        wxMessageService.replyText(fromUser, result.getMessage());
                    } else if(result.getCode() == 14005) {
                        wxMessageService.replyText(fromUser, "没有客服在线");
                    } else if (result.getCode() == 14006) { 
                        wxMessageService.replyText(fromUser, "客服忙，请等待，你前面还有 " + (result.getCount() + 1) + " 位");
                    } else if (result.getCode() == 14515) {
                        wxMessageService.replyText(fromUser, "没有权限，请购买专业版七鱼客服");
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (sessionManager.isEvaluationMsg(fromUser, content)) {
                return true;
            }
        }
        return false;
    }

    private static String xmlTextContent(Element node, String tagName) {
        return node.getElementsByTagName(tagName).item(0).getTextContent();
    }



    public static void main(String[] args) {
        QiyuSessionManager sessionManager = new QiyuSessionManager();
        JSONObject json = JSONObject.parseObject("{\"uid\":\"oxWh0uI0j33bSTO_CY9eFxqap9MI\",\"message\":\"哈哈\",\"evaluationModel\":{\"type\":3,\"title\":\"模式二\",\"note\":\"三级评价模式\",\"list\":[{\"value\":100,\"name\":\"满意\"},{\"value\":50,\"name\":\"一般\"},{\"value\":1,\"name\":\"不满意\"}]},\"staffId\":142,\"sessionId\":274491,\"staffName\":\"客服44\",\"staffType\":1,\"staffIcon\":\"http://nos.netease.com/ysf/29C25737ABC2524667D223A90FEF156D\",\"code\":200}");
        ApplyStaffResult result = new ApplyStaffResult();
        result.setCode(json.getIntValue(OpenApiTags.CODE));
        result.setMessage(json.getString(OpenApiTags.MESSAGE));
        if (result.getCode() == 200) {
            result.setSession(JSONObject.toJavaObject(json, Session.class));
        } else if (result.getCode() == 14008) {
            result.setCount(json.getIntValue(OpenApiTags.COUNT));
        }

        sessionManager.onSessionStart(result.getSession());

        json = JSONObject.parseObject("{\"uid\":\"oxWh0uI0j33bSTO_CY9eFxqap9MI\",\"message\":\"哈哈\",\"staffId\":142,\"sessionId\":274491,\"staffName\":\"客服44\",\"staffType\":1,\"staffIcon\":\"http://nos.netease.com/ysf/29C25737ABC2524667D223A90FEF156D\",\"code\":200,\"closeReason\":0}");
        Session session = JSON.toJavaObject(json, Session.class);
        sessionManager.onSessionEnd(session);

        sessionManager.isEvaluationMsg(json.getString("uid"), "1");

        String content = "<xml><ToUserName><![CDATA[gh_fbe6f8d3398e]]></ToUserName>\n" +
                "<FromUserName><![CDATA[oxWh0uI0j33bSTO_CY9eFxqap9MI]]></FromUserName>\n" +
                "<CreateTime>1477017096</CreateTime>\n" +
                "<MsgType><![CDATA[image]]></MsgType>\n" +
                "<PicUrl><![CDATA[http://mmbiz.qpic.cn/mmbiz_jpg/Nt8emaxicaPAXmZSFRePg51tpJd5XNdWt1uDrRCPVfpl3K1nsNzaYcPBmSREOib8cQS155CnOzNvvoYmiatujibWMQ/0]]></PicUrl>\n" +
                "<MsgId>6343740123371811732</MsgId>\n" +
                "<MediaId><![CDATA[YMRGXA98VRaHPFCZY69_qKnfFqxVAcJKUlrNSXIS-hGBENaN9gRaqBK_RbG2Tycq]]></MediaId>\n" +
                "</xml>\n";

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader(content);
            InputSource is = new InputSource(sr);
            Document document = db.parse(is);

            final Element root = document.getDocumentElement();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
