Key Points:

•	OAuth Integration: Uses Google’s OAuth 2.0 for user authentication.
•	JWT Management: Manages user sessions using JWTs stored in the browser’s local storage.
•	Serverless Integration: Interacts with a Lambda function to validate tokens and fetch user information.
•	User Experience: Provides a smooth user experience with styled buttons and seamless redirection based on authentication status.

API Endpoints:
    
•	https://49vrl95j02.execute-api.us-east-1.amazonaws.com/prod1/users
•	headers = {'Content-Type': 'application/json'}
•	response = requests.post(lambda_url, json={'idToken':?? headers=???)
