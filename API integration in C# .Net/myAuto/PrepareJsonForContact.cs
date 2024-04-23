
    using Newtonsoft.Json;

    namespace myAuto
    {
        public class PrepareJsonForContact
        {


            // Implement PrepareContactJson method to return contact data in JSON format
            public static string PrepareContactJson(List<Contact> contacts, List<string> listIds)
            {
                var importData = contacts.Select(contact => new Dictionary<string, object>
                {
                    ["email"] = contact.Email,
                    ["first_name"] = contact.FirstName,
                    ["last_name"] = contact.LastName,
                    ["job_title"] = contact.JobTitle,
                    ["company_name"] = contact.CompanyName,
                    ["anniversary"] = contact.Anniversary,
                    ["birthday_month"] = int.TryParse(contact.BirthdayMonth, out var bm) ? bm : (int?)null,
                    ["birthday_day"] = int.TryParse(contact.BirthdayDay, out var bd) ? bd : (int?)null,
                    ["phone"] = contact.Phone,
                    ["street"] = contact.Street,
                    ["street2"] = contact.Street2,
                    ["city"] = contact.City,
                    ["state"] = contact.State,
                    ["zip"] = contact.Zip,
                    ["country"] = contact.Country,
                    ["cf:account_number"] = contact.AccountNumber,
                    ["cf:age"] = contact.Age,
                    ["cf:last_user_number"] = contact.LastUserNumber,
                    ["cf:last_amount"] = contact.LastAmount,
                    ["cf:last_transaction"] = contact.LastTransaction
                }).ToList();

                var payload = new
                {
                    import_data = importData,
                    list_ids = listIds
                };

                return JsonConvert.SerializeObject(payload);
            }
        }
    }
