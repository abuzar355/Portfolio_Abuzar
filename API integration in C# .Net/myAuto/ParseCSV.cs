using System.Formats.Asn1;
using System.Globalization;
using System.Text.RegularExpressions;
using CsvHelper;
using Newtonsoft.Json;

namespace myAuto
{
    public class ParseCSV
    {
        public static List<Contact> GetValidContactsFromFile(string filePath)
        {
            var validContacts = new List<Contact>();

            try
            {
                using (var reader = new StreamReader(filePath))
                using (var csv = new CsvReader(reader, CultureInfo.InvariantCulture))
                {
                    var records = csv.GetRecords<Contact>();
                    foreach (var record in records)
                    {
                        if (IsValidContact(record))
                        {
                            validContacts.Add(record);
                        }
                        else
                        {
                            Console.WriteLine($"Invalid contact data found for: {record.Email}\n");
                        }
                    }
                }


            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error occurred while reading file {filePath}: {ex.Message}\n");
            }
            finally
            {


               
            }
            return validContacts;

        }
        private static bool IsValidContact(Contact contact)
        {
            
            // Required field validation
            if (string.IsNullOrEmpty(contact.Email) || string.IsNullOrWhiteSpace(contact.FirstName) || string.IsNullOrWhiteSpace(contact.LastName))
                return false;

            // Email format validation
            if (!IsValidEmail(contact.Email))
                return false;

            // Anniversary date validation
            if (!string.IsNullOrWhiteSpace(contact.Anniversary) && !DateTime.TryParse(contact.Anniversary, out _))
                return false;

            // Last transaction date validation
            if (!string.IsNullOrWhiteSpace(contact.LastTransaction) && !DateTime.TryParse(contact.LastTransaction, out _))
                return false;

            return true;
        }


        private static bool IsValidEmail(string email)
        {
            // Simple regex for email validation
            return Regex.IsMatch(email, @"^[^@\s]+@[^@\s]+\.[^@\s]+$");
        }
    }
}
