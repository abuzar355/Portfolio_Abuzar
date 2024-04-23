using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace myAuto
{
    public class MarkFileProcessed
    {

        public static void markAsProcessed(string csvFilePath)
        {
            try
            {
                string processedFolderPath = "processed"; // Specify the path to the processed folder

                string dateTimeFormat = DateTime.Now.ToString("yyyyMMdd_HHmmss");
                string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(csvFilePath);
                string fileExtension = Path.GetExtension(csvFilePath);

                // Create the new file name with date and time appended
                string newFileName = $"{fileNameWithoutExtension}_{dateTimeFormat}{fileExtension}";
                string processedFilePath = Path.Combine(processedFolderPath, newFileName);

                // Ensure the processed folder exists
                Directory.CreateDirectory(processedFolderPath);

                // Move the file to the processed folder
                File.Move(csvFilePath, processedFilePath, true);

                Console.WriteLine("File is Removed from InProgress folder and moved into Processed Folder Successfully.");
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error moving file: " + ex.Message);
            }
        }
    }
}
