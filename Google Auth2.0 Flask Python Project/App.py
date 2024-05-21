from flask import Flask, jsonify, request, redirect, url_for, session, render_template,render_template_string
import requests
from authlib.integrations.flask_client import OAuth
import os
import base64
import json

app = Flask(__name__)

oauth = OAuth(app)
google = oauth.register(
    name='google',
    
    access_token_url='https://oauth2.googleapis.com/token',
    authorize_url='https://accounts.google.com/o/oauth2/auth',
    api_base_url='https://www.googleapis.com/oauth2/v1/',
    server_metadata_url='https://accounts.google.com/.well-known/openid-configuration',
    client_kwargs={'scope': 'openid email profile'},
    redirect_uri='http://127.0.0.1:5000/login/callback'
)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/login')
def login():
    nonce = base64.b64encode(os.urandom(16)).decode('utf-8')
    session['nonce'] = nonce
    redirect_uri = url_for('authorize', _external=True)
    return google.authorize_redirect(redirect_uri, nonce=nonce)

@app.route('/login/callback')
def authorize():
    try:
        token = google.authorize_access_token()
        nonce = session.pop('nonce', None)
        userinfo = google.parse_id_token(token, nonce)
        if userinfo:
            lambda_url = 'https://49vrl95j02.execute-api.us-east-1.amazonaws.com/prod1/users'
            headers = {'Content-Type': 'application/json'}
            response = requests.post(lambda_url, json={'idToken': token['id_token']}, headers=headers)
            if response.status_code == 200:
                data = response.json()
                body = json.loads(data['body'])
                if 'jwt' in body:
                    return render_template_string('''
                        <script>
                            localStorage.setItem('jwt', '{{ jwt }}');
                            alert('Logged in');
                            window.location.href = "/welcome";
                        </script>
                    ''', jwt=body['jwt'])
                else:
                    return jsonify({'error': 'JWT not found in the response'}), 400
            else:
                return jsonify({'error': 'Failed to authenticate with Lambda'}), response.status_code
        else:
            return jsonify({"error": "Invalid or expired ID token"}), 401
    except Exception as e:
        return jsonify({'error': str(e)}), 400

@app.route('/welcome')
def welcome():
    return render_template('welcome.html')

@app.route('/view_message')
def view_message():
    return render_template('view_message.html')




@app.route('/submit_message', methods=['POST'])
def submit_message():
    auth_header = request.headers.get('Authorization')
    if not auth_header:
        return jsonify({'success': False}), 401

    jwt_token = auth_header.split(' ')[1]
    message = request.json.get('message')
    lambda_url = 'https://49vrl95j02.execute-api.us-east-1.amazonaws.com/prod1/users'
    headers = {'Content-Type': 'application/json'}
    response = requests.post(lambda_url, json={'jwtToken': jwt_token, 'message': message}, headers=headers)
    if response.status_code == 200:
        return jsonify({'success': True})
    else:
        return jsonify({'success': False}), response.status_code




@app.route('/get_message', methods=['POST'])
def get_message():
    auth_header = request.headers.get('Authorization')
    if not auth_header:
        return jsonify({'valid': False}), 401

    jwt_token = auth_header.split(' ')[1]
    lambda_url = 'https://49vrl95j02.execute-api.us-east-1.amazonaws.com/prod1/users'
    headers = {'Content-Type': 'application/json'}
    response = requests.post(lambda_url, json={'jwtToken': jwt_token, 'getMessage': True}, headers=headers)
    if response.status_code == 200:
        data = response.json()
        body = json.loads(data.get('body', '{}'))
        user = body.get('user', {})
        message = user.get('message', '')
        return jsonify({'valid': True, 'message': message})
    else:
        return jsonify({'valid': False}), response.status_code






@app.route('/get_user_info', methods=['POST'])
def get_user_info():
    auth_header = request.headers.get('Authorization')
    if not auth_header:
        return jsonify({'valid': False}), 401

    jwt_token = auth_header.split(' ')[1]
    lambda_url = 'https://49vrl95j02.execute-api.us-east-1.amazonaws.com/prod1/users'
    headers = {'Content-Type': 'application/json'}
    response = requests.post(lambda_url, json={'jwtToken': jwt_token}, headers=headers)
    if response.status_code == 200:
        data = response.json()
        body = json.loads(data['body'])
        return jsonify({'valid': True, 'user': body['user']})
    else:
        return jsonify({'valid': False}), response.status_code

if __name__ == "__main__":
    app.run(debug=True)
