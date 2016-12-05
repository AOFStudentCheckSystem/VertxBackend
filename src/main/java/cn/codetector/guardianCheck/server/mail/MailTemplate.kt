/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.mail

import java.util.*

class MailTemplate constructor(var templateContent: String) {

    private var sender: String = ""
    private var title: String = ""
    private val recipients: MutableList<String> = ArrayList()

    constructor(other: MailTemplate) : this(other.templateContent) {
        this.title = other.title
        this.sender = other.sender
        other.recipients.forEach { item ->
            this.recipients.add(item)
        }
    }

    fun set(templateKey: String, value: Any) {
        this.templateContent = this.templateContent.replace("{{" + templateKey + "}}", value.toString())
    }

    fun setList(templateKey: String, value: List<Any>) {
        val sb: StringBuilder = StringBuilder()
        value.forEach { v ->
            sb.append(v.toString()).append("<br>")
        }
        this.set(templateKey, sb.toString())
    }

    fun addRecipients(recipients: List<String>) {
        this.recipients.addAll(recipients)
    }

    fun addRecipient(recipient: String) {
        this.recipients.add(recipient)
    }

    fun setSender(sender: String) {
        this.sender = sender
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun isComplete(): Boolean {
        return (this.sender.isNotBlank() && this.title.isNotBlank() && (this.recipients.size > 0))
    }

    fun getRecipients(): List<String> {
        return ArrayList<String>(recipients)
    }

    fun getTitle(): String {
        return title
    }

    fun getPage(): String {
        return templateContent
    }


}