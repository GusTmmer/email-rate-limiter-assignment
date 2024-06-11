package com.timmermans.email

import com.timmermans.SendEmailRequest

interface EmailSender {
    fun send(emailRequest: SendEmailRequest)
}