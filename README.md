# 企业微信对接七鱼客服

这个项目根据七鱼客服给出的demo基础上进行二次开发，对接了企业微信。

## 接口封装

有关七鱼消息接口的使用文档，请参阅[七鱼官网开发指南](http://qiyukf.com/newdoc/html/message_interface.html)。

有关企业微信消息接口的使用文档，请参阅[企业微信API](https://work.weixin.qq.com/api/doc)。


## 说明

本套代码实现网易七鱼与企业微信进行对接。

七鱼以及微信公众号的APP_KEY，APP_SECRET等参数配置，在 `com.qiyukf.openapi.controller.Constants` 中，使用时需要修改自己企业的对应值。

使用前请注意Java AES加密的illegal key size问题。
