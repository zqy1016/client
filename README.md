# 七鱼消息接口接入示例

这个项目根据七鱼客服demo，再次基础上对接了企业微信。

## 接口封装

有关七鱼消息接口的使用文档，请参阅[七鱼官网开发指南](http://qiyukf.com/newdoc/html/message_interface.html)。

有关企业微信消息接口的使用文档，请参阅[企业微信API](https://work.weixin.qq.com/api/doc)。

在这个封装包中，`SessionClient` 封装了向七鱼发送请求的接口，`ResponseParser` 封装了收到七鱼消息后的响应处理。为了简化微信公众号的接入流程，`SessionClient`中还封装了直接转发微信消息到七鱼服务器的接口，包括普通文本消息和需要先下载，然后在上传到七鱼的图片和语音消息。

## 说明

本套代码实现网易七鱼与企业微信进行对接。

七鱼以及微信公众号的APP_KEY，APP_SECRET等参数配置，在 `com.qiyukf.openapi.controller.Constants` 中，使用时需要修改自己企业的对应值。

使用前请注意Java AES加密的illegal key size问题。
