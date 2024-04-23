
using System.Net;

namespace myAuto
{
    public class LocalHttpServer
    {
        private HttpListener _listener;
        private string _authorizationCode;

        public LocalHttpServer(string uriPrefix)
        {
            _listener = new HttpListener();
            _listener.Prefixes.Add(uriPrefix);
        }

        public async Task<string> StartAsync()
        {
            _listener.Start();
            Console.WriteLine("Listening for authorization code...");

            var context = await _listener.GetContextAsync();
            var request = context.Request;
            _authorizationCode = request.QueryString["code"];

            var response = context.Response;
            var responseString = "<html><script>window.close();</script><body>Authorization code received. You can close this window if it remains open.</body></html>";
            var buffer = System.Text.Encoding.UTF8.GetBytes(responseString);
            response.ContentLength64 = buffer.Length;
            var responseOutput = response.OutputStream;
            await responseOutput.WriteAsync(buffer, 0, buffer.Length);
            responseOutput.Close();

            _listener.Stop();
            return _authorizationCode;
        }
    }
}