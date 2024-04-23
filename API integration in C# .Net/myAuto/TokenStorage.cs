
using Newtonsoft.Json;

namespace myAuto
{
    public static class TokenStorage
    {
        private static readonly string tokenFilePath = "token.txt";

        public static void StoreTokens(OAuthTokens tokens)
        {
            var json = JsonConvert.SerializeObject(tokens);
            File.WriteAllText(tokenFilePath, json);
        }

        public static OAuthTokens GetStoredTokens()
        {
            if (!File.Exists(tokenFilePath))
            {
                return null;
            }

            var json = File.ReadAllText(tokenFilePath);
            return JsonConvert.DeserializeObject<OAuthTokens>(json);
        }
    }
}
