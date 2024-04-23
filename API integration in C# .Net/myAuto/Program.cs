
using System.Collections.Generic;
using Microsoft.Extensions.Configuration;
using myAuto;
using Newtonsoft.Json;

class Program
{
    static IConfigurationRoot Configuration { get; set; }
    static async Task Main(string[] args)
    {

        Console.WriteLine($"Current Directory: {AppContext.BaseDirectory}\n");

        // Build configuration
        var builder = new ConfigurationBuilder().SetBasePath(AppContext.BaseDirectory).AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

        Configuration = builder.Build();

        // Read settings
        var clientId = Configuration["ConstantContactSettings:ClientId"];
        var clientSecret = Configuration["ConstantContactSettings:ClientSecret"];
        var redirectUri = Configuration["ConstantContactSettings:RedirectUri"];
        var csvFilePath = Configuration["ConstantContactSettings:InProgressFolderPath"];
        var scope = Configuration["ConstantContactSettings:Scope"];
        var random = new Random();
        var state = Guid.NewGuid().ToString("N") + random.Next(10000, 99999).ToString();
        var hpcuTransactionsList = Configuration["ConstantContactSettings:HPCUTransactionsList"];
        var hpcuMemebrsList = Configuration["ConstantContactSettings:HPCUMembersList"];
        var storedTokens = TokenStorage.GetStoredTokens();
        var client = new ConstantContactClient(!string.IsNullOrEmpty(clientId)?clientId:"", !string.IsNullOrEmpty(clientSecret)?clientSecret:"");
        int count = 0;


        while (count==0)
        {
            try
            {
                // Check for CSV files in the "InProgress" folder
                string[] fileEntries = Directory.GetFiles(csvFilePath, "*.csv");
                if (fileEntries.Length > 0)
                {
                    foreach (string filePath in fileEntries)
                    {
                        try
                        {
                            var contacts = ParseCSV.GetValidContactsFromFile(filePath);
                            if (contacts.Count != 0)
                            {

                                Console.WriteLine($"File Data is Parsed Successfully...\n");

                                //Process each file
                                await ProcessFile(contacts, client, storedTokens, hpcuTransactionsList, hpcuMemebrsList, clientId, redirectUri, scope, state, clientSecret);
                                
                                // Move the processed file
                                MarkFileProcessed.markAsProcessed(filePath);


                            }
                            else
                            {
                                count =1;
                            }
                        }

                        catch (Exception e){
                            Console.WriteLine("Exception Occured While Processing" + e.Message +"\n");

                            MarkFileAsFailed.markAsFailed(filePath);

                        }
                    }
                }
                else
                {
                    Console.WriteLine("No CSV files found. Please Place CSV Fie in inProgress folder\n");
                    Thread.Sleep(10000);
                    return;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error occurred" + ex.Message+"\n");
                Thread.Sleep(15000); // Wait before reattempting

            }
        }
    }

    static async Task ProcessFile(List<Contact> contacts, ConstantContactClient client, OAuthTokens storedTokens, string hpcuTransactionsList, 
                                  string hpcuMemebrsList,string clientId,string redirectUri, string scope, string state, string clientSecret)
    {



        if (storedTokens == null)
        {

            // Step 1: Redirect the user to the authorization URL
            var authorizationUrl = client.GetAuthorizationUrl(clientId, redirectUri, scope, state);
            

            //Listenning to the authorization code generated at provided redirect url
            var localHttpServer = new LocalHttpServer(redirectUri);
            var authorizationCodeTask = localHttpServer.StartAsync();

            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = authorizationUrl,
                UseShellExecute = true
            });

            var authorizationCode = await authorizationCodeTask;

            // Step 2: Exchange the authorization code for an access token
            var oAuthTokens = await client.GetAccessTokenAsync(clientId, clientSecret, authorizationCode, redirectUri);

            // Get new tokens and store them
            if (oAuthTokens != null)
            {
                TokenStorage.StoreTokens(oAuthTokens);
                storedTokens = oAuthTokens;
            }
            else
            {
                Console.WriteLine("Failed to obtain access tokens....\n");
                return;
            }
        }

        if (OAuthTokens.IsAccessTokenExpired(storedTokens))

        {
            storedTokens = await client.RefreshAccessTokenAsync(storedTokens.RefreshToken);

            if (storedTokens != null)
            {
                TokenStorage.StoreTokens(storedTokens);
                storedTokens = storedTokens;
            }
            else
            {
                Console.WriteLine("Failed to obtain access tokens...\n");
                return;
            }

        }


        if (storedTokens != null && !string.IsNullOrEmpty(storedTokens.AccessToken))
        {
            Console.WriteLine("Access token obtained successfully, Exporting File Now.....\n");

            // Step 3: Use the access token to make API requests
            List<string> lists = new List<string>();

            if (string.IsNullOrEmpty(contacts[0].LastUserNumber) && string.IsNullOrEmpty(contacts[0].LastTransaction) && string.IsNullOrEmpty(contacts[0].LastAmount)) {

                lists.Add(hpcuMemebrsList);
            }
            else
            {
                lists.Add(hpcuTransactionsList);
            }

            //List<string> lists = await client.GetContactListsAsync(storedTokens.AccessToken);

            // Check if there are any list IDs returned
            if (lists.Any())
            {
                var contactJson = PrepareJsonForContact.PrepareContactJson(contacts, lists); // Prepare JSON for all contacts

                // Call ExportContactsToAPI with the concatenated list IDs
                var responseExp = await client.ExportContactstoAPI(storedTokens.AccessToken, contactJson);
                responseExp.EnsureSuccessStatusCode();
                Console.WriteLine("File Exported to Constant Contact Account with Status Code: " + responseExp.StatusCode + "\n");

            }
        }
        else
        {
            Console.WriteLine("Failed to obtain access token...\n");
        }

        
    }

}
