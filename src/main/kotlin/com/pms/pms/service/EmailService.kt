package com.pms.pms.service

import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    @Value("\${spring.mail.username}")
    private lateinit var fromEmail: String

    @Value("\${app.host-url}")
    private lateinit var baseUrl: String

    private fun createMimeMessage(): MimeMessage {
        return mailSender.createMimeMessage()
    }

    fun sendOtpEmail(toEmail: String, otpCode: String, userName: String) {
        val subject = "Email Verification - Project Management System"
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$subject</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                        border-top: 5px solid #007bff; /* Accent color */
                    }
                    .header {
                        text-align: center;
                        padding-bottom: 20px;
                        border-bottom: 1px solid #eee;
                    }
                    .header h1 {
                        margin: 0;
                        color: #007bff;
                        font-size: 24px;
                    }
                    .content {
                        padding: 20px 0;
                        font-size: 16px;
                    }
                    .otp-code {
                        display: inline-block;
                        background-color: #e0f7fa;
                        color: #007bff;
                        font-size: 28px;
                        font-weight: bold;
                        padding: 10px 20px;
                        border-radius: 5px;
                        margin: 20px 0;
                        letter-spacing: 2px;
                    }
                    .footer {
                        text-align: center;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                        font-size: 12px;
                        color: #777;
                    }
                    .button {
                        display: inline-block;
                        background-color: #007bff;
                        color: #ffffff;
                        padding: 10px 20px;
                        text-decoration: none;
                        border-radius: 5px;
                        margin-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Project Management System</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>$userName</strong>,</p>
                        <p>Thank you for registering with our Project Management System!</p>
                        <p>Your verification code is:</p>
                        <p class="otp-code">$otpCode</p>
                        <p>This code will expire in <strong>10 minutes</strong>. Please enter it on the verification page to activate your account.</p>
                        <p>If you didn't request this verification, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>The Project Management System Team</p>
                        <p>&copy; ${java.time.Year.now()} Project Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendHtmlEmail(toEmail, subject, htmlContent)
    }

    fun sendResendOtpEmail(toEmail: String, otpCode: String, userName: String) {
        val subject = "New Verification Code - Project Management System"
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$subject</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                        border-top: 5px solid #ffc107; /* Accent color for resend */
                    }
                    .header {
                        text-align: center;
                        padding-bottom: 20px;
                        border-bottom: 1px solid #eee;
                    }
                    .header h1 {
                        margin: 0;
                        color: #ffc107;
                        font-size: 24px;
                    }
                    .content {
                        padding: 20px 0;
                        font-size: 16px;
                    }
                    .otp-code {
                        display: inline-block;
                        background-color: #fff3e0;
                        color: #e65100;
                        font-size: 28px;
                        font-weight: bold;
                        padding: 10px 20px;
                        border-radius: 5px;
                        margin: 20px 0;
                        letter-spacing: 2px;
                    }
                    .footer {
                        text-align: center;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                        font-size: 12px;
                        color: #777;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Project Management System</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>$userName</strong>,</p>
                        <p>You requested a new verification code for your account. Here it is:</p>
                        <p class="otp-code">$otpCode</p>
                        <p>This code will expire in <strong>10 minutes</strong>. Please use this latest code to verify your account.</p>
                        <p>If you didn't request this, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>The Project Management System Team</p>
                        <p>&copy; ${java.time.Year.now()} Project Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendHtmlEmail(toEmail, subject, htmlContent)
    }

    fun sendWelcomeEmail(toEmail: String, userName: String) {
        val subject = "Welcome to Project Management System! 🎉"
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$subject</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                        border-top: 5px solid #28a745; /* Accent color for welcome */
                    }
                    .header {
                        text-align: center;
                        padding-bottom: 20px;
                        border-bottom: 1px solid #eee;
                    }
                    .header h1 {
                        margin: 0;
                        color: #28a745;
                        font-size: 24px;
                    }
                    .content {
                        padding: 20px 0;
                        font-size: 16px;
                    }
                    .footer {
                        text-align: center;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                        font-size: 12px;
                        color: #777;
                    }
                    .button {
                        display: inline-block;
                        background-color: #28a745;
                        color: #ffffff;
                        padding: 12px 25px;
                        text-decoration: none;
                        border-radius: 5px;
                        font-size: 18px;
                        margin-top: 25px;
                        transition: background-color 0.3s ease;
                    }
                    .button:hover {
                        background-color: #218838;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Project Management System</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>$userName</strong>,</p>
                        <p>Welcome to our Project Management System! 🎉</p>
                        <p>Your email has been successfully verified, and your account is now active.</p>
                        <p>You can now log in to your account and start managing your projects, collaborating with your team, and achieving your goals.</p>
                        <p>We're excited to have you on board!</p>
                        <p style="text-align: center;">
                            <a href="$baseUrl/auth/login" class="button">Log In to Your Account</a>
                        </p>
                        <p>If you have any questions or need assistance, please don't hesitate to contact our <a href="mailto:$fromEmail" style="color: #007bff; text-decoration: none;">support team</a>.</p>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>The Project Management System Team</p>
                        <p>&copy; ${java.time.Year.now()} Project Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendHtmlEmail(toEmail, subject, htmlContent)
    }

    fun sendWorkspaceInvitation(toEmail: String, workspaceName: String, inviterName: String, invitationId: String) {
        val subject = "Workspace Invitation - Project Management System"
        val invitationLink = "$baseUrl/app/workspaces" // Consider making this configurable
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$subject</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                        border-top: 5px solid #17a2b8; /* Accent color for invitation */
                    }
                    .header {
                        text-align: center;
                        padding-bottom: 20px;
                        border-bottom: 1px solid #eee;
                    }
                    .header h1 {
                        margin: 0;
                        color: #17a2b8;
                        font-size: 24px;
                    }
                    .content {
                        padding: 20px 0;
                        font-size: 16px;
                    }
                    .footer {
                        text-align: center;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                        font-size: 12px;
                        color: #777;
                    }
                    .button {
                        display: inline-block;
                        background-color: #17a2b8;
                        color: #ffffff;
                        padding: 12px 25px;
                        text-decoration: none;
                        border-radius: 5px;
                        font-size: 18px;
                        margin-top: 25px;
                        transition: background-color 0.3s ease;
                    }
                    .button:hover {
                        background-color: #138496;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Project Management System</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>Great news! You've been invited to collaborate on a new workspace:</p>
                        <p style="font-size: 18px; font-weight: bold; color: #17a2b8;">"$workspaceName"</p>
                        <p>This invitation was sent by <strong>$inviterName</strong>.</p>
                        <p>To accept this invitation and join the workspace, please click the button below:</p>
                        <p style="text-align: center;">
                            <a href="$invitationLink" class="button">Accept Invitation</a>
                        </p>
                        <p>This invitation will expire in <strong>7 days</strong>. Don't miss out!</p>
                        <p>If you didn't expect this invitation or do not wish to join, you can simply ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>The Project Management System Team</p>
                        <p>&copy; ${java.time.Year.now()} Project Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendHtmlEmail(toEmail, subject, htmlContent)
    }

    private fun sendHtmlEmail(toEmail: String, subject: String, htmlContent: String) {
        val message = createMimeMessage()
        val helper = MimeMessageHelper(message, true) // 'true' enables multipart for HTML content
        helper.setFrom(fromEmail)
        helper.setTo(toEmail)
        helper.setSubject(subject)
        helper.setText(htmlContent, true) // 'true' indicates that the content is HTML
        mailSender.send(message)
    }
}