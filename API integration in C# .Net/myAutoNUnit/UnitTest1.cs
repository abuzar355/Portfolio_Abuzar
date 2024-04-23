using Moq;
using Microsoft.Extensions.Configuration;
using System.Text;

namespace myAuto.Tests
{

    public class MockHttpMessageHandler : HttpMessageHandler
    {
        private readonly HttpResponseMessage _mockResponse;

        public MockHttpMessageHandler(HttpResponseMessage response)
        {
            _mockResponse = response;
        }

        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            return await Task.FromResult(_mockResponse);
        }
    }

    [TestFixture]
    public class ConstantContactClientTests
    {
        protected Mock<HttpMessageHandler> _mockHttpMessageHandler;
        private HttpClient _mockHttpClient;
        private ConstantContactClient _client;
        private Mock<IConfiguration> _mockConfiguration;

        [SetUp]
        public void Setup()
        {
            _mockHttpMessageHandler = new Mock<HttpMessageHandler>();
            _mockHttpClient = new HttpClient(_mockHttpMessageHandler.Object);
            _mockConfiguration = new Mock<IConfiguration>();
            _client = new ConstantContactClient("e80b5b5f-433c-4816-bd5f-375d62d9232b", "WQ-epYmWfIp8gBqMPOessw");
        }

        [Test]
        public async Task GetAccessTokenAsync_ReturnsTokens()
        {

            var random = new Random();
            var state = Guid.NewGuid().ToString("N") + random.Next(10000, 99999).ToString();

            // Step 1: Redirect the user to the authorization URL
            var authorizationUrl = _client.GetAuthorizationUrl("e80b5b5f-433c-4816-bd5f-375d62d9232b", "http://localhost:5000/", "contact_data offline_access", state);
            Console.WriteLine("Go to the following URL and authorize the app:");
            Console.WriteLine(authorizationUrl);


            //Listenning to the authorization code generated at provided redirect url
            var localHttpServer = new LocalHttpServer("http://localhost:5000/");
            var authorizationCodeTask = localHttpServer.StartAsync();

            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = authorizationUrl,
                UseShellExecute = true
            });

            var authorizationCode = await authorizationCodeTask;
            // Arrange: Set up the mock response
            var mockResponse = new HttpResponseMessage
            {
                StatusCode = System.Net.HttpStatusCode.OK,
                Content = new StringContent("{'access_token': 'eyJraWQiOiJOMWJRN0JZU0E0UHNYTUF2UUl0WnFsTU5LWHVUa0l2R3VNWDRNY3p2azVRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULmFfY1c5Wk5zdWRMRUtjbHIyd2hRTWRieTdudF9hb2hVWEZkTEdjdjQ1UUkub2FyMTVncHY5ZlVCaFBkVnowaDciLCJpc3MiOiJodHRwczovL2lkZW50aXR5LmNvbnN0YW50Y29udGFjdC5jb20vb2F1dGgyL2F1czFsbTNyeTltRjd4MkphMGg4IiwiYXVkIjoiaHR0cHM6Ly9hcGkuY2MuZW1haWwvdjMiLCJpYXQiOjE3MDQ3OTYxNDMsImV4cCI6MTcwNDg4MjU0MywiY2lkIjoiZTgwYjViNWYtNDMzYy00ODE2LWJkNWYtMzc1ZDYyZDkyMzJiIiwidWlkIjoiMDB1MXVtem9qYnFYMUI4Ym8waDgiLCJzY3AiOlsib2ZmbGluZV9hY2Nlc3MiLCJjb250YWN0X2RhdGEiXSwiYXV0aF90aW1lIjoxNzA0Nzk1MDMxLCJzdWIiOiJhYnV6YXJtaXJ6YTkxOEBnbWFpbC5jb20iLCJwbGF0Zm9ybV91c2VyX2lkIjoiNTQ4NTc0NWQtN2I4Mi00ZTBhLWJkOWYtZmRiYTFjODA4MDU5In0.sXpd3ILpXeyjDXBfLRFPNoVg4qwsb4Bs8YeJ3aK_McOzPFE3qu8K6REDkT3jzkVEwUFblXpdNvFXAKJXtoUCemgjFrjAP66wbSAlTI_ekebyKz3CM78-BtgUZTInhcEPgeYgqxlcOX_7iz58wl5J04bNGnlm7D1sWZBwwRPg0-jAaWvp4qtUzvli0qF6dpnJjb8ceYyb6IhS9pAHj-W5tdDzX9eokFEp7LspGyYbnrMxvIobd7NF40OQXmqutmut-LCMnYDhygpN7rU7KsfzlfVm9RxT1Oo7juzQfKZqYmvh_3I_MqtsE71JmqjIL6H6TE8EfwjqWrD-vPX7EOOaxg'," +
                " 'refresh_token': 'xv-FutBjYJP9r_EUCCuf7FR-4uflEema0x-nCOWorFg', 'expires_in': 3600}", Encoding.UTF8, "application/json")
            };

            var mockHttpMessageHandler = new MockHttpMessageHandler(mockResponse);
            var client = new HttpClient(mockHttpMessageHandler);

            // Act: Call the method under test
            var tokens = await _client.GetAccessTokenAsync("e80b5b5f-433c-4816-bd5f-375d62d9232b", "WQ-epYmWfIp8gBqMPOessw", authorizationCode, "http://localhost:5000/");
            var newToken = await _client.RefreshAccessTokenAsync(tokens.RefreshToken);
            // Assert: Verify the method behaves as expected


            Assert.AreNotEqual(null, tokens);
            Assert.AreNotEqual(null, newToken);

        }

    }
}
