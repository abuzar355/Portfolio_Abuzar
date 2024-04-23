Imports System.Reflection.Emit

Namespace MyUpdatedChatApp

    Partial Class Form1
        Inherits System.Windows.Forms.Form

        Private WithEvents txtMessage As System.Windows.Forms.RichTextBox
        Private WithEvents btnSend As System.Windows.Forms.Button
        Private WithEvents rtxtConversation As System.Windows.Forms.RichTextBox
        Private lblHeading As System.Windows.Forms.Label
        Private WithEvents lblType As System.Windows.Forms.Label

        Private MaximumRichTextBoxHeight As Integer



        ' Required designer variable.
        Private components As System.ComponentModel.IContainer = Nothing

        ' Clean up any resources being used.
        Protected Overrides Sub Dispose(disposing As Boolean)
            If disposing AndAlso (components IsNot Nothing) Then
                components.Dispose()
            End If
            AddHandler Me.txtMessage.TextChanged, AddressOf Me.txtMessage_TextChanged

            MyBase.Dispose(disposing)
        End Sub


        Private Sub txtMessage_TextChanged(sender As Object, e As EventArgs) Handles txtMessage.TextChanged
            ' Cast the sender back to a RichTextBox
            Dim rtb As RichTextBox = DirectCast(sender, RichTextBox)

            ' Measure the text
            Dim text As String = rtb.Text
            Dim proposedSize As Size = New Size(rtb.Width, Int32.MaxValue)
            Dim textSize As Size = TextRenderer.MeasureText(text, rtb.Font, proposedSize, TextFormatFlags.WordBreak)

            ' Set the minimum visible height for the txtMessage (e.g., the height of one line)
            Dim minimumVisibleHeight As Integer = TextRenderer.MeasureText("Test", rtb.Font).Height * 2.5

            ' Calculate new height but limit it to a maximum and ensure it's not less than the minimum height
            Dim newHeight As Integer = Math.Max(Math.Min(textSize.Height, MaximumRichTextBoxHeight), minimumVisibleHeight)

            ' Adjust txtMessage
            Me.txtMessage.Height = newHeight



            ' Ensure the caret is visible in txtMessage
            Me.txtMessage.SelectionStart = Me.txtMessage.Text.Length
            Me.txtMessage.ScrollToCaret()
        End Sub


        Private Sub label1_Click(sender As Object, e As EventArgs) Handles lblType.Click
            ' Dispose of the Label control when it is clicked
            lblType.Dispose()
        End Sub






        ' Required method for Designer support - do not modify
        ' the contents of this method with the code editor.
        Private Sub InitializeComponent()
            Me.rtxtConversation = New System.Windows.Forms.RichTextBox()
            Me.txtMessage = New System.Windows.Forms.RichTextBox()
            Me.btnSend = New System.Windows.Forms.Button()
            Me.lblHeading = New System.Windows.Forms.Label()
            Me.lblType = New System.Windows.Forms.Label()
            Me.SuspendLayout()
            '
            'rtxtConversation
            '
            Me.rtxtConversation.BackColor = System.Drawing.Color.Black
            Me.rtxtConversation.BorderStyle = System.Windows.Forms.BorderStyle.None
            Me.rtxtConversation.Font = New System.Drawing.Font("Segoe UI", 9.0!)
            Me.rtxtConversation.ForeColor = System.Drawing.Color.White
            Me.rtxtConversation.Location = New System.Drawing.Point(8, 60)
            Me.rtxtConversation.Name = "rtxtConversation"
            Me.rtxtConversation.ReadOnly = True
            Me.rtxtConversation.Size = New System.Drawing.Size(571, 290)
            Me.rtxtConversation.TabIndex = 2
            Me.rtxtConversation.TabStop = False
            Me.rtxtConversation.Text = ""
            '
            'txtMessage
            '
            Me.txtMessage.BackColor = System.Drawing.Color.FromArgb(CType(CType(30, Byte), Integer), CType(CType(30, Byte), Integer), CType(CType(30, Byte), Integer))
            Me.txtMessage.ForeColor = System.Drawing.Color.White
            Me.txtMessage.Location = New System.Drawing.Point(9, 13)
            Me.txtMessage.Name = "txtMessage"
            Me.txtMessage.Size = New System.Drawing.Size(501, 31)
            Me.txtMessage.TabIndex = 0
            Me.txtMessage.Text = ""
            '
            'btnSend
            '
            Me.btnSend.BackColor = System.Drawing.Color.FromArgb(CType(CType(100, Byte), Integer), CType(CType(149, Byte), Integer), CType(CType(237, Byte), Integer))
            Me.btnSend.Cursor = System.Windows.Forms.Cursors.Hand
            Me.btnSend.FlatStyle = System.Windows.Forms.FlatStyle.Flat
            Me.btnSend.Font = New System.Drawing.Font("Segoe UI", 9.0!, System.Drawing.FontStyle.Bold)
            Me.btnSend.ForeColor = System.Drawing.Color.White
            Me.btnSend.Location = New System.Drawing.Point(516, 12)
            Me.btnSend.Name = "btnSend"
            Me.btnSend.Size = New System.Drawing.Size(65, 32)
            Me.btnSend.TabIndex = 1
            Me.btnSend.Text = "Send"
            Me.btnSend.UseVisualStyleBackColor = True
            '
            'lblHeading
            '
            Me.lblHeading.Font = New System.Drawing.Font("Jokerman", 36.0!, System.Drawing.FontStyle.Bold)
            Me.lblHeading.ForeColor = System.Drawing.Color.LightSlateGray
            Me.lblHeading.Location = New System.Drawing.Point(145, 154)
            Me.lblHeading.Margin = New System.Windows.Forms.Padding(2, 0, 2, 0)
            Me.lblHeading.Name = "lblHeading"
            Me.lblHeading.Size = New System.Drawing.Size(246, 66)
            Me.lblHeading.TabIndex = 0
            Me.lblHeading.Text = "Chat-GT"
            Me.lblHeading.TextAlign = System.Drawing.ContentAlignment.MiddleCenter
            '
            'lblType
            '
            Me.lblType.BackColor = System.Drawing.Color.FromArgb(CType(CType(30, Byte), Integer), CType(CType(30, Byte), Integer), CType(CType(30, Byte), Integer))
            Me.lblType.BorderStyle = System.Windows.Forms.BorderStyle.Fixed3D
            Me.lblType.ForeColor = System.Drawing.Color.Transparent
            Me.lblType.ImageAlign = System.Drawing.ContentAlignment.TopLeft
            Me.lblType.Location = New System.Drawing.Point(9, 12)
            Me.lblType.Margin = New System.Windows.Forms.Padding(2)
            Me.lblType.Name = "lblType"
            Me.lblType.Padding = New System.Windows.Forms.Padding(0, 2, 0, 0)
            Me.lblType.Size = New System.Drawing.Size(501, 32)
            Me.lblType.TabIndex = 0
            Me.lblType.Text = "Type your message here..."
            '
            'Form1
            '
            Me.AutoScaleDimensions = New System.Drawing.SizeF(6.0!, 13.0!)
            Me.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font
            Me.BackColor = System.Drawing.Color.Black
            Me.ClientSize = New System.Drawing.Size(591, 362)
            Me.Controls.Add(Me.lblHeading)
            Me.Controls.Add(Me.lblType)
            Me.Controls.Add(Me.rtxtConversation)
            Me.Controls.Add(Me.btnSend)
            Me.Controls.Add(Me.txtMessage)
            Me.Margin = New System.Windows.Forms.Padding(2)
            Me.Name = "Form1"
            Me.Text = "Support Chat"
            Me.ResumeLayout(False)

        End Sub

        Private Sub Form1_Load(sender As Object, e As EventArgs)
            ' Center the label when the form loads
            lblHeading.Location = New System.Drawing.Point((Me.ClientSize.Width - lblHeading.Width) / 2, (Me.ClientSize.Height - lblHeading.Height) / 2)


            ' Calculate the height of one line of text in txtMessage
            Dim oneLineHeight As Integer = TextRenderer.MeasureText("Test", txtMessage.Font).Height

            ' Define the maximum number of lines you want to display in txtMessage
            Dim maxLines As Integer = 5

            ' Calculate the maximum height for txtMessage based on the number of lines
            MaximumRichTextBoxHeight = oneLineHeight * maxLines


        End Sub




    End Class

End Namespace
