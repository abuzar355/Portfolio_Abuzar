using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace myAuto
{
    public class OAuthTokens
    {
        [JsonProperty("access_token")]
        public string AccessToken { get; set; }

        [JsonProperty("refresh_token")]
        public string RefreshToken { get; set; }

        [JsonProperty("expires_in")]
        public int ExpiresIn { get; set; }

        public DateTime AccessTokenObtainedAt { get; set; }

       
        // Check if the access token is expired
        public static bool IsAccessTokenExpired(OAuthTokens tokens)
        {
            // Assuming tokens contain the time when the access token was obtained and its lifespan
            return DateTime.UtcNow > tokens.AccessTokenObtainedAt.AddSeconds(tokens.ExpiresIn);
        }

    }
}
