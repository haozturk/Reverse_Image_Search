import argparse
import io
import base64
import os
import string
import random
import pycurl
import json

from google.cloud import vision
from google.cloud.vision import types
from werkzeug.utils import secure_filename
from flask import Flask, url_for, jsonify, request,send_from_directory
#from flask_cors import CORS, cross_origin
from bs4 import BeautifulSoup
from apiclient.discovery import build

python3 = False
try:
    from StringIO import StringIO
except ImportError:
    python3 = True
    import io as bytesIOModule
if python3:
    import certifi

response = ""

UPLOAD_FOLDER = '/home/tony/stajProject/googleCloudVision/static'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])


app = Flask(__name__,static_url_path='/static')
app.debug = True
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

def name_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))

# serving some static html files
@app.route('/<path:path>')
def send_url(path):
    return send_from_directory('static', path)


@app.route('/upload', methods=['POST'])
def upload():
    if request.headers['Content-Type'] != 'application/json':
        return "Requests must be in JSON format. Please make sure the header is 'application/json' and the JSON is valid."

    client_json = json.dumps(request.json)
    client_data = json.loads(client_json)

    currentName = name_generator()

    f = os.path.join(UPLOAD_FOLDER, currentName + '.png')

    b = client_data['bitmap'].encode('utf-8')

    file = open(f,"wb")
    #file.write("bitmap: " + client_data['bitmap'])
    file.write(base64.decodebytes(b))
    file.close()
    

    data = {}
    data['imageName'] = currentName
    json_data = json.dumps(data)

    return json_data



@app.route('/search', methods = ['POST'])
def search():
    if request.headers['Content-Type'] != 'application/json':
        return "Requests must be in JSON format. Please make sure the header is 'application/json' and the JSON is valid."
    response = ""
    client_json = json.dumps(request.json)
    client_data = json.loads(client_json)

    #report(annotate(client_data['image_url']))
    response = vision_image_manager(client_data['image_url'])
    response = json.dumps(response)

    returnJson = '{"return":"successfull"}'

    print("\nFinal Response:\n")
    print(response)

    #return returnJson
    return response



def vision_image_manager(image_file):

    # Instantiates a client
    service = build('vision', 'v1')
    # text.png is the image file.
    file_name = str(image_file)

    if image_file.startswith('http') or image_file.startswith('gs:'):
        image = types.Image()
        image.source.image_uri = image_file
        service_request = service.images().annotate(body={
            'requests': [{
                'image': {
                    'source': {
                        'imageUri':image.source.image_uri
                    }
                },
                'features': [{
                    'type': 'WEB_DETECTION',
                }]
            }]
        })        
    else:
        with open(file_name, 'rb') as image:
            image_content = base64.b64encode(image.read())
            #print("\n------------->" + image_content.decode('UTF-8'))
            service_request = service.images().annotate(body={
                'requests': [{
                    'image': {
                        'content': image_content.decode('UTF-8')
                    },
                    'features': [{
                        'type': 'WEB_DETECTION',
                    }]
                }]  
            })

    
    response = service_request.execute()
    #response = ''.join(response['responses'])
    #print(type(response))
    print("\nFirst Response:\n")
    print(response)
    #res_dict = dict(response)
    #return res_dict
    return response





def annotate(path):
    """Returns web annotations given the path to an image."""
    client = vision.ImageAnnotatorClient()

    if path.startswith('http') or path.startswith('gs:'):
        image = types.Image()
        image.source.image_uri = path

    else:
        with io.open(path, 'rb') as image_file:
            content = image_file.read()

        image = types.Image(content=content)

    web_detection = client.web_detection(image=image).web_detection

    return web_detection



def report(annotations):
    """Prints detected features in the provided web annotations."""
    if annotations.pages_with_matching_images:
        print('\n{} Pages with matching images retrieved'.format(
            len(annotations.pages_with_matching_images)))

        for page in annotations.pages_with_matching_images:
            print('Url   : {}'.format(page.url))

    if annotations.full_matching_images:
        print('\n{} Full Matches found: '.format(
              len(annotations.full_matching_images)))

        for image in annotations.full_matching_images:
            print('Url  : {}'.format(image.url))

    if annotations.partial_matching_images:
        print('\n{} Partial Matches found: '.format(
              len(annotations.partial_matching_images)))

        for image in annotations.partial_matching_images:
            print('Url  : {}'.format(image.url))

    if annotations.web_entities:
        print('\n{} Web entities found: '.format(
              len(annotations.web_entities)))

        for entity in annotations.web_entities:
            print('Score      : {}'.format(entity.score))
            print('Description: {}'.format(entity.description))

    if annotations.visually_similar_images:
    	print('\n{} Visually Similar_Images found: '.format(
              len(annotations.visually_similar_images)))
    	for image in annotations.visually_similar_images:
            print('Url  : {}'.format(image.url))


    if annotations.best_guess_labels:
    	print('\n{} Best Guess Label found: '.format(
              len(annotations.best_guess_labels)))
    	for label in annotations.best_guess_labels:
            print('Best Guess  : {}'.format(label.label))



if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    path_help = str('The image to detect, can be web URI, '
                    'Google Cloud Storage, or path to local file.')
    parser.add_argument('-p', '--port', type=int, default=5000, help='port number')
    args = parser.parse_args()

    #parser.add_argument('image_url', help=path_help)
    #args = parser.parse_args()
    #vision_image_manager(args.image_url)

    #report(annotate(args.image_url))

    app.run(host='0.0.0.0', port=args.port)