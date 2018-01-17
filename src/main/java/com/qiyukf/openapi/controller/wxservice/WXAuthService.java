package com.qiyukf.openapi.controller.wxservice;

import com.alibaba.fastjson.JSONObject;
import com.qiyukf.openapi.controller.Constants;
import com.qiyukf.openapi.session.util.HttpClientPool;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhoujianghua on 2015/10/10.
 *
 * 微信第三方开发平台授权登录流程：
 * <p>
 * 1.
 */
@Service("wxAuthService")
public class WXAuthService {

    private static Logger logger = Logger.getLogger(WXAuthService.class);

//    private String accessToken;

//    private long expireTime;

    private AtomicBoolean fetching = new AtomicBoolean(false);

    //这里将token使用HashMap存下来，无需使用缓存
    private Map<String, String> tokenMap = new HashMap<>();


    /**
     * 获取第三方公众号的access token
     * @return 第三方公众号的access token
     */
    public String queryAccessToken() {
        if (tokenMap.get("expireTime") == null || Long.parseLong(tokenMap.get("expireTime")) < System.currentTimeMillis()){
            updateAccessTokenFromWx();
        }
        return tokenMap.get("accessToken");
    }

    private void updateAccessTokenFromWx() {
        if (!fetching.compareAndSet(false, true)) {
            return;
        }
        String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
        url = String.format(url, Constants.WX_CORP_ID, Constants.WX_CORP_SECRET);
        try {
            String ret = HttpClientPool.getInstance().get(url);
            JSONObject json = JSONObject.parseObject(ret);
            tokenMap.put("expireTime",String.valueOf(System.currentTimeMillis() + json.getIntValue("expires_in") * 1000));
            tokenMap.put("accessToken",json.getString("access_token"));
        } catch (IOException e) {
            logger.debug("query accessToken error: " + e);
        } finally {
            fetching.set(false);
        }
    }
}
