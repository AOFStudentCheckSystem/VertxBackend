/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.mail

import cn.codetector.util.Configuration.Configuration
import cn.codetector.util.Configuration.ConfigurationManager
import java.util.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


/**
 * Created by Codetector on 02/12/2016.
 */
object MailService {
    private var config: Configuration = ConfigurationManager.getConfiguration("smtp.json")
    private val smtpProps = Properties()
    private var sharedSession: Session? = null

    init {
        smtpProps.setProperty("mail.host", config.getStringValue("smtp_host", "42.236.82.169"))
        smtpProps.setProperty("mail.transport.protocol", "smtp")
        smtpProps.setProperty("mail.smtp.auth", config.getStringValue("smtp_auth", "true"))
        sharedSession = Session.getInstance(smtpProps)
    }

    fun sendMessage(message: MimeMessage) {
        try {
            val ts = sharedSession!!.transport
            ts.connect(config.getStringValue("smtp_host", "localhost"), config.getStringValue("smtp_user", ""), config.getStringValue("smtp_password", ""))
            ts.sendMessage(message, message.allRecipients)
            ts.close()
        } catch (e: MessagingException) {
            e.printStackTrace()
        }

    }

    fun getEmail(temp: MailTemplate): MimeMessage {
        val msg = MimeMessage(sharedSession)
        try {
            msg.setFrom(InternetAddress(config.getStringValue("send_addr", "codetector@codetector.cn")))
            val recipientAddress = arrayOfNulls<InternetAddress>(temp.getRecipients().size)
            var counter = 0
            for (recipient in temp.getRecipients()) {
                recipientAddress[counter] = InternetAddress(recipient.trim({ it <= ' ' }))
                counter++
            }
            msg.setRecipients(Message.RecipientType.TO, recipientAddress)
            msg.subject = temp.getTitle()
            msg.setContent(temp.getPage(), "text/html;charset=UTF-8")
        } catch (e: MessagingException) {
            e.printStackTrace()
        }

        return msg
    }
}