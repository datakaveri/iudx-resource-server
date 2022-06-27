import requests,json,argparse
from requests.structures import CaseInsensitiveDict

parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
parser.add_argument("-p", "--pune", action='count', help="generate token for pune-env-flood resource-group")
parser.add_argument("-s", "--surat", action='count' ,help="generate token for surat-itms-live-eta resource")
args = parser.parse_args()

def token(clientId,clientSecret,data,url):
        url = "https://authvertx.iudx.io/auth/v1/token"

        headers = CaseInsensitiveDict()
        headers["clientId"] = clientId
        headers["clientSecret"] = clientSecret
        headers["Content-Type"] = "application/json"

        resp = requests.post(url, headers=headers, data=json.dumps(data))
        json_object = json.loads(resp.text)
        print(json_object["results"]["accessToken"])

# with open("example-config.json") as file:
with open("/home/ubuntu/configs/3.5.0/rs-token-config.json") as file:
    config = json.load(file)

if args.pune:
        token(config["clientID"],config["clientSecret"],config["pune-request-body"],config["auth-server-url"])

if args.surat:       
        token(config["clientID"],config["clientSecret"],config["surat-request-body"],config["auth-server-url"])
