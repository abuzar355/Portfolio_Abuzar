import json
import boto3
from google.oauth2 import id_token
from google.auth.transport import requests
from jose import jwt
from jose import JWTError
import os

# Initialize DynamoDB client
dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('Users')
messages = dynamodb.Table('userMessages')


def lambda_handler(event, conntext):
    try:
        # Step 1: Check if Google ID token or jwtToken is present in the request
       
        if 'idToken' in event :
            # Step 2: Verify the Google ID token
            google_id_token = event['idToken']
            user_info = validate_google_id_token(google_id_token)

        elif 'jwtToken' in event and 'getMessage' not in event and 'message' not  in event :
            # Step 2: Verify the JWT token
            jwt_token = event['jwtToken']
            user_info = validate_jwt_token(jwt_token)
        
        elif 'jwtToken' in event and 'getMessage' in event and 'message' not  in event:   

            jwt_token = event['jwtToken']
            message_info = validate_jwt_token(jwt_token)        
            user_message = get_message_info_from_dynamodb(message_info)
            return {
                'statusCode': 200,
                'body': json.dumps({'user':user_message})

                }
        

        elif 'jwtToken' in event and 'message' in event and 'getMessage' not  in event:    
            jwt_token = event['jwtToken']
            message_info = validate_jwt_token(jwt_token)         
            save_message_in_dynamodb(message_info, event)


        else:
            return {
                'statusCode': 401,
                'body': json.dumps({'error': 'Not a valid api!'})
            }
        

        
        

        
        
        
       

        # Step 3: Check if the user already exists in DynamoDB
        user_id = user_info.get('sub', user_info.get('user_id', None))
        if user_id is None:
            return {
                'statusCode': 401,
                'body': json.dumps({'error': 'User ID not found in token payload'})
            }
        if user_exists(user_id):
            user_info = get_user_info_from_dynamodb(user_id)
        else:
            create_user_in_dynamodb(user_info)
        
        # Step 4: Generate a JWT
        encoded_jwt = generate_jwt(user_id)
        
        # Step 5: Return the JWT to the client
        return {
            'statusCode': 200,
            'body': json.dumps({'user':user_info,'jwt': encoded_jwt})
        }

    except ValueError as e:
        # Invalid token
        return {
            'statusCode': 401,
            'body': json.dumps({'error': str(e)})
        }

def validate_google_id_token(google_id_token):
    """
    Validates the Google ID token using Google's public keys.
    Args:
        id_token (str): The Google ID token received from the client-side.
    Returns:
        dict: User information extracted from the validated ID token.
    Raises:
        ValueError: If the ID token is invalid.
    """
    user_info = id_token.verify_oauth2_token(google_id_token, requests.Request())
    return user_info
    raise ValueError("Invalid Google ID token signature")


def validate_jwt_token(jwt_token):
    """
    Validates the JWT token.
    Args:
        jwt_token (str): The JWT token received from the client-side.
    Returns:
        dict: User information extracted from the validated JWT token.
    Raises:
        JWTError: If the JWT token is invalid.
    """
    try:
        payload = jwt.decode(jwt_token, os.environ['JWT_SECRET'], algorithms=['HS256'])
        return {'sub': payload['sub']}
    except JWTError as e:
        raise JWTError("Invalid JWT token") from e


def user_exists(user_id):
    """
    Checks if a user with the same Google ID already exists in DynamoDB.
    Args:
        user_id (str): User ID extracted from the Google ID token.
    Returns:
        bool: True if the user exists, False otherwise.
    """
    response = table.get_item(Key={'user_id': user_id})
    return 'Item' in response


def create_user_in_dynamodb(user_info):
    """
    Creates a new user account in DynamoDB.
    Args:
        user_info (dict): User information extracted from the Google ID token.
    """
    table.put_item(Item={
        'user_id': user_info['sub'],
        'email': user_info['email'],
        'name': user_info['name'],
    })

def save_message_in_dynamodb(user_info,event):
    
    messages.put_item(Item={
        'user_id': user_info['sub'],
        'message': event['message'],       

    })



def generate_jwt(user_id):
    """
    Generates a JWT containing user information.
    Args:
        user_id (str): User ID extracted from the Google ID token.
    Returns:
        str: Encoded JWT.
    """
    jwt_payload = {'sub': user_id}
    encoded_jwt = jwt.encode(jwt_payload, os.environ['JWT_SECRET'], algorithm='HS256')
    return encoded_jwt

def get_user_info_from_dynamodb(user_id):
    """
    Retrieves user information from DynamoDB.
    Args:
        user_id (str): User ID extracted from the Google ID token or JWT token.
    Returns:
        dict: User information retrieved from DynamoDB.
    """
    response = table.get_item(Key={'user_id': user_id})
    if 'Item' in response:
        return response['Item']
    else:
        raise ValueError(f"User with ID {user_id} not found in DynamoDB")
    
def get_message_info_from_dynamodb(user_info):
    
    user_id = user_info.get('sub', user_info.get('user_id', None))
    response = messages.get_item(Key={'user_id': user_id})
    if 'Item' in response:
        return response['Item']
    else:
        raise ValueError(f"User with ID {user_id} not found in DynamoDB")
    
    
