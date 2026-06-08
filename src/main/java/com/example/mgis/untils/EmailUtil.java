package com.example.mgis.untils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender mailSender;

    // 和配置文件里一致的【发件邮箱】
    private static final String FROM_EMAIL = "heshiheng0808@163.com";

    /**
     * 发送纯文本验证码邮件
     * @param toEmail 收件人邮箱（用户自己的邮箱）
     * @param code 验证码
     */
    public void sendForgetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_EMAIL);    // 发件人：你的邮箱
        message.setTo(toEmail);         // 收件人：系统用户邮箱
        message.setSubject("佛山无人车系统 - 找回密码验证码");
        message.setText("您好，您的找回密码验证码：" + code + "\n"
                + "验证码有效期5分钟，请勿转发给他人。\n"
                + "如非本人操作，请忽略本邮件。");

        mailSender.send(message);
    }
}