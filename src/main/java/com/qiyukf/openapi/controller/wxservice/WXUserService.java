package com.qiyukf.openapi.controller.wxservice;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qiyukf.openapi.controller.QiyuSessionService;
import com.qiyukf.openapi.session.model.CommonResult;
import com.qiyukf.openapi.session.util.HttpClientPool;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhoujianghua on 2015/10/27.
 */
@Service("wxUserService")
public class WXUserService {

    private static Logger logger = Logger.getLogger(WXUserService.class);

    // 这应该是要持久化的，并有过期时间
    private Map<String, String> userNickMap = new HashMap<>();

    @Autowired
    private WXAuthService wxAuthService;

    @Autowired
    private QiyuSessionService qiyuSessionService;

    public String queryWxUserNick(String openId) throws IOException {
        String nick = userNickMap.get(openId);
        if (TextUtils.isEmpty(nick)) {
            String url = "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s";
            url = String.format(url, wxAuthService.queryAccessToken(), openId);

            String res = HttpClientPool.getInstance().get(url);
            if (TextUtils.isEmpty(res)) {
                return null;
            } else {
                JSONObject json = JSONObject.parseObject(res);
                nick = json.getString("name");
                userNickMap.put(openId, nick);
                // 更新到七鱼
                updateWxUserToQiyu(openId, json);
            }
        }
        return nick;
    }

    private void updateWxUserToQiyu(String openId, JSONObject wxUser) {
        JSONArray crm = new JSONArray();
        crm.add(item(null, "real_name", wxUser.getString("name")));
        crm.add(item(null, "mobile_phone", wxUser.getString("mobile")));
        crm.add(item(null, "email", wxUser.getString("email")));
        crm.add(item("职位", "position", wxUser.getString("position")));
        String gender = wxUser.getString("gender");
        if (gender.equals("1")){
            crm.add(item("性别", "sex", "男"));
        }else if (gender.equals("2")){
            crm.add(item("性别", "sex", "女"));
        }
        try {
            CommonResult result = qiyuSessionService.updateCrmInfo(openId, crm);
            logger.debug("update crm " + openId + " result: " + result);
        } catch (Exception e) {
            logger.debug("update crm error: " + e);
        }
    }

    private JSONObject item(String label, String key, String value) {
        JSONObject item = new JSONObject();
        item.put("key", key);
        item.put("value", value);
        if (!TextUtils.isEmpty(label)) {
            item.put("label", label);
        }
        return item;
    }
}
