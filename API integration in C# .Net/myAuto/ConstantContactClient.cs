using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;



namespace myAuto
{



    public class ConstantContactClient
    {
        private readonly string _apiKey;
        private readonly string _apiSecret;
        private readonly HttpClient _httpClient;

        public ConstantContactClient(string apiKey, string apiSecret)
        {
            _apiKey = apiKey;
            _apiSecret = apiSecret;
            _httpClient = new HttpClient();
        }




        public async Task<OAuthTokens> GetAccessTokenAsync(string clientId, string clientSecret, string code, string redirectUri)
        {
            var tokenEndpoint = "https://authz.constantcontact.com/oauth2/default/v1/token";
            var clientCredentials = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes($"{clientId}:{clientSecret}"));

            var tokenRequestParams = new Dictionary<string, string>
             {
                   {"grant_type", "authorization_code"},
                   {"code", code},
                   {"redirect_uri", redirectUri}
             };

            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", clientCredentials);
            var requestContent = new FormUrlEncodedContent(tokenRequestParams);
            var response = await httpClient.PostAsync(tokenEndpoint, requestContent);

            if (!response.IsSuccessStatusCode)
            {
                // Handle the response error.
                Console.WriteLine($"Error fetching access token: {response.StatusCode}");
                return null;
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var tokenResponse = JsonConvert.DeserializeObject<OAuthTokens>(responseContent);
            tokenResponse.AccessTokenObtainedAt = DateTime.UtcNow;


            return tokenResponse;
        }


        public async Task<OAuthTokens> RefreshAccessTokenAsync(string refreshToken)
        {
            var tokenEndpoint = "https://authz.constantcontact.com/oauth2/default/v1/token";

            var requestContent = new FormUrlEncodedContent(new Dictionary<string, string>
             {
                 {"grant_type", "refresh_token"},
                 {"refresh_token", refreshToken}
             });

            var clientCredentials = Convert.ToBase64String(Encoding.UTF8.GetBytes($"{_apiKey}:{_apiSecret}"));
            _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", clientCredentials);

            var response = await _httpClient.PostAsync(tokenEndpoint, requestContent);
            response.EnsureSuccessStatusCode();

            var responseContent = await response.Content.ReadAsStringAsync();
            var tokenResponse = JsonConvert.DeserializeObject<OAuthTokens>(responseContent);
            tokenResponse.AccessTokenObtainedAt = DateTime.UtcNow;

            return tokenResponse;
        }




        public string GetAuthorizationUrl(string clientId, string redirectUri, string scope, string state)
        {
            var authorizationEndpoint = "https://authz.constantcontact.com/oauth2/default/v1/authorize";
            var url = $"{authorizationEndpoint}?response_type=code&client_id={clientId}&redirect_uri={Uri.EscapeDataString(redirectUri)}&scope={Uri.EscapeDataString(scope)}&state={Uri.EscapeDataString(state)}";
            return url;
        }




        public async Task<HttpResponseMessage> ExportContactstoAPI(string accessToken, string contactJson)
        {
            var requestUri = "https://api.cc.email/v3/activities/contacts_json_import";
            _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);

            var content = new StringContent(contactJson, Encoding.UTF8, "application/json");
            return await _httpClient.PostAsync(requestUri, content);
        }



        public async Task<List<string>> GetContactListsAsync(string accessToken)
        {
            var requestUri = "https://api.cc.email/v3/contact_lists?include_count=true&status=active&include_membership_count=all";
            _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
            var listIds = new List<string>();

            try
            {
                HttpResponseMessage response = await _httpClient.GetAsync(requestUri);
                response.EnsureSuccessStatusCode();

                string content = await response.Content.ReadAsStringAsync();
                var lists = JsonConvert.DeserializeObject<JObject>(content)["lists"];

                foreach (var list in lists)
                {
                    string listId = list["list_id"].ToString();
                    listIds.Add(listId);
                }
            }
            catch (HttpRequestException e)
            {
                Console.WriteLine("\nException Caught!");
                Console.WriteLine("Message :{0} ", e.Message);
            }

            return listIds;
        }

    


    }
}
