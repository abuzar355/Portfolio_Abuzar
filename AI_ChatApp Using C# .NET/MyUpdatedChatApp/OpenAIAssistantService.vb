Imports System.Net.Http
Imports System.Net.Http.Headers
Imports System.Text
Imports Newtonsoft.Json
Imports Newtonsoft.Json.Linq


Namespace GTDealsAPI.API.ProfitAnalysis
    Public Class Message
        Public Property id As String
        Public Property role As String
        Public Property created_at As Long
        Public Property content As List(Of Content)
    End Class

    Public Class Content
        Public Property text As Text
    End Class

    Public Class Text
        Public Property value As String
    End Class

    Public Class MessagesResponse
        Public Property data As List(Of Message)
    End Class

    Public Class OpenAIAssistantService
        Private ReadOnly _httpClient As HttpClient
        Private threadId As String = String.Empty

        Public Sub New(httpClient As HttpClient)
            If httpClient Is Nothing Then Throw New ArgumentNullException(NameOf(httpClient))
            _httpClient = httpClient
            InitializeHttpClient()
        End Sub

        Private Sub InitializeHttpClient()
            _httpClient.DefaultRequestHeaders.Authorization = New AuthenticationHeaderValue("Bearer", apiKey)
            _httpClient.DefaultRequestHeaders.Add("OpenAI-Beta", "assistants=v1")
        End Sub

        Public Async Function GetResponseFromAssistant(conversation As List(Of String), assistantId As String) As Task(Of String)
            If String.IsNullOrEmpty(threadId) Then
                threadId = Await CreateThread()
            End If

            Dim messageResponse As String = Await SendMessage(conversation)
            If IsErrorResponse(messageResponse) Then Return messageResponse

            Dim runId As String = Await CreateRun(threadId, assistantId)
            If IsErrorResponse(runId) Then Return runId

            Dim isComplete As Boolean = Await PollForCompletion(runId)
            Return If(isComplete, Await RetrieveMessage(threadId), "Error: Assistant did not complete in expected time.")
        End Function

        Private Function IsErrorResponse(response As String) As Boolean
            Return response.StartsWith("Error") OrElse response.StartsWith("Exception")
        End Function

        Private Async Function PollForCompletion(runId As String) As Task(Of Boolean)
            Await Task.Delay(2000)
            Dim runStatus As String = Await CheckRunStatus(runId)
            Return runStatus = "completed" OrElse runStatus = "failed"
        End Function

        Private Async Function CheckRunStatus(runId As String) As Task(Of String)
            Dim response As HttpResponseMessage = Await _httpClient.GetAsync($"https://api.openai.com/v1/threads/{threadId}/runs/{runId}")

            If Not response.IsSuccessStatusCode Then
                Return "failed"
            End If

            Dim responseContent As String = Await response.Content.ReadAsStringAsync()
            Dim parsedResponse As JObject = JObject.Parse(responseContent)

            Return If(parsedResponse("status")?.ToString(), "unknown")
        End Function

        Private Async Function SendMessage(conversation As List(Of String)) As Task(Of String)
            Dim combinedPrompt As String = String.Join(vbCrLf, conversation)

            Dim requestBody As Object = New With {
                .role = "user",
                .content = combinedPrompt
            }

            Dim requestBodyJson As String = JsonConvert.SerializeObject(requestBody)
            Dim content As New StringContent(requestBodyJson, Encoding.UTF8, "application/json")

            Dim response As HttpResponseMessage = Await _httpClient.PostAsync($"https://api.openai.com/v1/threads/{threadId}/messages", content)

            If response.IsSuccessStatusCode Then
                Dim responseContent As String = Await response.Content.ReadAsStringAsync()
                Dim parsedResponse As JObject = JObject.Parse(responseContent)

                If parsedResponse IsNot Nothing AndAlso parsedResponse("id") IsNot Nothing Then
                    Return parsedResponse("id").ToString()
                Else
                    Return "Error: Message ID not found."
                End If
            Else
                Dim errorContent As String = Await response.Content.ReadAsStringAsync()
                Return $"Error: {response.StatusCode} - {response.ReasonPhrase} - Details: {errorContent}"
            End If
        End Function

        Private Async Function CreateThread() As Task(Of String)
            Dim requestBodyJson As String = ""
            Dim content As New StringContent(requestBodyJson, Encoding.UTF8, "application/json")

            Dim response As HttpResponseMessage = Await _httpClient.PostAsync("https://api.openai.com/v1/threads", content)

            If Not response.IsSuccessStatusCode Then
                Dim errorContent As String = Await response.Content.ReadAsStringAsync()
                Return $"Error: {response.StatusCode} - {response.ReasonPhrase} - Details: {errorContent}"
            End If

            Dim responseContent As String = Await response.Content.ReadAsStringAsync()
            Dim parsedResponse As JObject = JObject.Parse(responseContent)
            If parsedResponse Is Nothing OrElse parsedResponse("id") Is Nothing Then
                Return "Error: No ID in response"
            End If

            threadId = parsedResponse("id").ToString()

            Return threadId
        End Function

        Private Async Function CreateRun(threadId As String, assistantId As String) As Task(Of String)
            Dim requestBody = New With {
                .assistant_id = assistantId
            }

            Dim requestBodyJson = JsonConvert.SerializeObject(requestBody)
            Dim content = New StringContent(requestBodyJson, Encoding.UTF8, "application/json")

            Dim response = Await _httpClient.PostAsync($"https://api.openai.com/v1/threads/{threadId}/runs", content)

            If Not response.IsSuccessStatusCode Then
                Dim errorContent = Await response.Content.ReadAsStringAsync()
                Return $"Error: {response.StatusCode} - {response.ReasonPhrase} - Details: {errorContent}"
            End If

            Dim responseContent = Await response.Content.ReadAsStringAsync()
            Dim parsedResponse As JObject = JObject.Parse(responseContent)
            If parsedResponse Is Nothing OrElse parsedResponse("id") Is Nothing Then
                Return "Error: No ID in response"
            End If

            Dim runId = parsedResponse("id").ToString()

            Dim status = parsedResponse("status").ToString()

            While status <> "completed"
                Await Task.Delay(2000)
                Dim statusResponse = Await _httpClient.GetAsync($"https://api.openai.com/v1/threads/{threadId}/runs/{runId}")

                If Not statusResponse.IsSuccessStatusCode Then
                    Return "Error: Unable to check run status."
                End If

                Dim statusContent = Await statusResponse.Content.ReadAsStringAsync()
                Dim statusParsedResponse As JObject = JObject.Parse(statusContent)
                status = statusParsedResponse("status")
            End While

            Return If(status = "completed", runId, "Error: Run did not complete successfully.")
        End Function

        Private Async Function RetrieveMessage(threadId As String) As Task(Of String)
            Dim response = Await _httpClient.GetAsync($"https://api.openai.com/v1/threads/{threadId}/messages")

            If Not response.IsSuccessStatusCode Then
                Dim errorContent = Await response.Content.ReadAsStringAsync()
                Return $"Error: {response.StatusCode} - {response.ReasonPhrase} - Details: {errorContent}"
            End If

            Dim responseContent = Await response.Content.ReadAsStringAsync()
            Dim parsedResponse As JObject = JObject.Parse(responseContent)

            If parsedResponse("data") Is Nothing OrElse Not parsedResponse("data").Any() Then
                Return "No messages found in response."
            End If
            Dim messages As List(Of Message) = parsedResponse("data").ToObject(Of List(Of Message))()

            Dim sortedMessages = messages.OrderByDescending(Function(m) m.created_at).ToList()

            For Each message In sortedMessages
                If message.role = "assistant" Then
                    Dim contentItem = message.content.FirstOrDefault()
                    If contentItem?.text IsNot Nothing Then
                        Return contentItem.text.value
                    End If
                End If
            Next

            Return "No assistant message found."
        End Function
    End Class
End Namespace



