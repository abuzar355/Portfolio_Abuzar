Imports MyUpdatedChatApp.GTDealsAPI.API.ProfitAnalysis
Imports System.Net.Http


Namespace MyUpdatedChatApp

    Partial Public Class Form1
        Inherits Form

        Private typingAnimationState As Integer = 0
        Private typingTimer As System.Windows.Forms.Timer = New System.Windows.Forms.Timer()
        Private Shared client As HttpClient = New HttpClient()
        Private AI As OpenAIAssistantService = New OpenAIAssistantService(client)
        Private conversationHistory As List(Of String) = New List(Of String)()

        Public Sub New()
            typingTimer.Interval = 500 ' 500 milliseconds
            AddHandler typingTimer.Tick, AddressOf typingTimer_Tick
            InitializeComponent()

        End Sub

        Private Async Sub btnSend_Click(sender As Object, e As EventArgs) Handles btnSend.Click
            Dim userMessage As String = txtMessage.Text.Trim()

            If lblHeading IsNot Nothing Then
                lblHeading.Dispose()
                lblHeading = Nothing
            End If

            If Not String.IsNullOrEmpty(userMessage) Then
                Dim userDisplayText As String = "Me: " & userMessage & Environment.NewLine & Environment.NewLine
                conversationHistory.Add(userDisplayText)
                rtxtConversation.AppendText(userDisplayText)
                rtxtConversation.AppendText("AI is typing")
                typingTimer.Start()
                txtMessage.Clear()

                Try
                    Dim response As String = Await AI.GetResponseFromAssistant(conversationHistory, "asst_XDT63AFTVSy5LPVQvaHB1Z5y")
                    Await Task.Delay(2000)
                    typingTimer.Stop()
                    Dim aiDisplayText As String = "AI: " & response & Environment.NewLine & Environment.NewLine
                    conversationHistory.Add(aiDisplayText)
                    rtxtConversation.Text = rtxtConversation.Text.Substring(0, rtxtConversation.Text.Length - ("AI is typing" & New String("."c, typingAnimationState)).Length)
                    rtxtConversation.AppendText(aiDisplayText)
                Catch ex As Exception
                    ' Handle any exceptions here
                    MessageBox.Show("Exception occurred: " & ex.Message)
                End Try
            End If
        End Sub

        Private Sub typingTimer_Tick(sender As Object, e As EventArgs)
            ' Update the typingAnimationState
            typingAnimationState = (typingAnimationState + 1) Mod 4

            ' Remove the "AI is typing" message and trailing dots
            If rtxtConversation.Text.EndsWith("AI is typing...") Then
                rtxtConversation.Text = rtxtConversation.Text.Substring(0, rtxtConversation.Text.Length - 15)
            ElseIf rtxtConversation.Text.EndsWith("AI is typing..") Then
                rtxtConversation.Text = rtxtConversation.Text.Substring(0, rtxtConversation.Text.Length - 14)
            ElseIf rtxtConversation.Text.EndsWith("AI is typing.") Then
                rtxtConversation.Text = rtxtConversation.Text.Substring(0, rtxtConversation.Text.Length - 13)
            ElseIf rtxtConversation.Text.EndsWith("AI is typing") Then
                rtxtConversation.Text = rtxtConversation.Text.Substring(0, rtxtConversation.Text.Length - 12)
            End If

            ' Append new dots based on typingAnimationState
            rtxtConversation.AppendText("AI is typing" & New String("."c, typingAnimationState))
        End Sub

        Private Sub Form1_Load_1(sender As Object, e As EventArgs) Handles MyBase.Load

        End Sub
    End Class
End Namespace

