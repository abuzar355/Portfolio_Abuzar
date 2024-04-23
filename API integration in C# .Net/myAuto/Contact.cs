using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CsvHelper.Configuration.Attributes;

namespace myAuto
{
    public class Contact
    {
        [Name("email")]
        public string Email { get; set; }

        [Name("first_name")]
        public string FirstName { get; set; }

        [Name("last_name")]
        public string LastName { get; set; }

        [Name("cf:AccountNumber")]
        public string AccountNumber { get; set; }

        [Name("cf:Age")]
        public string Age { get; set; }

        [Name("job_title")]
        public string JobTitle { get; set; }

        [Name("company_name")]
        public string CompanyName { get; set; }

        [Name("anniversary")]
        public string Anniversary { get; set; }

        [Name("birthday_month")]
        public string BirthdayMonth { get; set; }

        [Name("birthday_day")]
        public string BirthdayDay { get; set; }

        [Name("phone")]
        public string Phone { get; set; }

        [Name("street")]
        public string Street { get; set; }

        [Name("street2")]
        public string Street2 { get; set; }

        [Name("city")]
        public string City { get; set; }

        [Name("state")]
        public string State { get; set; }

        [Name("zip")]
        public string Zip { get; set; }

        [Name("country")]
        public string Country { get; set; }

        [Name("cf:LastUserNumber")]
        public string LastUserNumber { get; set; }

        [Name("cf:LastAmount")]
        public string LastAmount { get; set; }

        [Name("cf:LastTransaction")]
        public string LastTransaction { get; set; }

    }
}
