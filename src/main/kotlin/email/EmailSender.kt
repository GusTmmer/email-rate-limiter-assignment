package com.timmermans.email

interface EmailSender {
    fun send(emailRequest: SendEmailRequest)
}