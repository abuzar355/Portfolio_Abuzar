using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace myAuto
{
    public class MarkFileAsFailed
    {

        public static void markAsFailed(string csvFilePath)
        {
            try
            {
                string processedFolderPath = "Failed"; // Specify the path to the processed folder

                string dateTimeFormat = DateTime.Now.ToString("yyyyMMdd_HHmmss");
                string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(csvFilePath);
                string fileExtension = Path.GetExtension(csvFilePath);

                // Create the new file name with date and time appended
                string newFileName = $"{fileNameWithoutExtension}_{dateTimeFormat}{fileExtension}";
                string failedFilePath = Path.Combine(processedFolderPath, newFileName);

                // Ensure the processed folder exists
                Directory.CreateDirectory(processedFolderPath);

                // Move the file to the processed folder
                File.Move(csvFilePath, failedFilePath, true);

                Console.WriteLine("File is Removed from InProgress folder and moved into Failed Folder.");
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error moving file: " + ex.Message);
            }
        }
    }
}
