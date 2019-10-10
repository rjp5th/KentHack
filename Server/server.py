import http.server
import json
import ssl
import sqlite3
import time
import urllib.parse


def parse_path(path):
    location = path.split("?")[0]
    arguments = {}
    if len(path.split("?")) > 1:
        arguments = parse_arguments(path.split("?")[1])
    return location, arguments


def parse_arguments(args):
    arguments = {}
    for item in args.split("&"):
        if "=" not in item:
            continue
        arguments[item.split("=")[0].replace('+', ' ')] = urllib.parse.unquote(item.split("=")[1].replace('+', ' '))
    return arguments


conn = sqlite3.connect("application.db")


class MyHandler(http.server.BaseHTTPRequestHandler):

    # Handler for the GET requests
    def do_GET(self):
        location, arguments = parse_path(self.path)
        if location == "/shelters_info":
            c = conn.cursor()
            self.send_response(200)
            self.send_header(b'Content-type', b'text/json')
            self.end_headers()

            query_data = c.execute("""SELECT l.id, l.name, l.lat, l.long, l.description, l.capacity, l.predictionCoefficient, l.predictionConstant,
                    l.predictionStartDate, (SUM(o.dependents) + COUNT(o.id)) AS occupantCount
                    FROM locations l LEFT JOIN occupants o on l.id = o.locationId GROUP BY l.id;""")
            output_array = []
            for row in query_data:
                entry = {}
                for num in range(len(row)):
                    entry[query_data.description[num][0]] = row[num]
                output_array.append(entry)
            self.wfile.write(json.dumps({"data":output_array}).encode())
            c.close()
        else:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Bad Request")

    def do_POST(self):
        location, arguments = parse_path(self.path)
        if location == "/sign_in":
            length = int(self.headers['Content-Length'])
            data = self.rfile.read(length).decode('utf-8')
            post_data = parse_arguments(data)
            try:
                #post_data = json.loads(data)
                sql = """INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, 
                        address, scanTime) VALUES ({0}, '{1}', '{2}', {3}, {4}, {5}, '{6}', {7});"""\
                    .format(post_data["locationId"], post_data["firstName"], post_data["lastName"],
                            post_data["gender"] == "Male", post_data["dependents"], post_data["phoneNumber"],
                            post_data["address"], time.time())
                c = conn.cursor()
                result = c.execute(sql)
                conn.commit()
                c.close()
                self.send_response(200)
                self.send_header(b'Content-type', b'text/plain')
                self.end_headers()
                self.wfile.write(b"Success")
                print("Successfully signed in " + post_data["firstName"] + " " + post_data["lastName"])
            except:
                print("Bad sign in request")
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"This request cannot be processed at this time")
        elif location == "/sign_out":
            length = int(self.headers['Content-Length'])
            data = self.rfile.read(length).decode('utf-8')
            post_data = parse_arguments(data)
            try:
                #post_data = json.loads(data)
                sql = """DELETE FROM occupants WHERE firstName = '{0}' AND lastName = '{1}'""" \
                    .format(post_data["firstName"], post_data["lastName"])
                c = conn.cursor()
                result = c.execute(sql)
                if result.rowcount == 0:
                    self.send_response(404)
                    self.end_headers()
                    self.wfile.write(b"Unable to find record")
                    c.close()
                    return
                conn.commit()
                c.close()
                self.send_response(200)
                self.send_header(b'Content-type', b'text/plain')
                self.end_headers()
                self.wfile.write(b"Success")
                print("Succesfully signed out", post_data["firstName"], post_data["lastName"])
            except:
                print("Bad sign out request")
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"This request cannot be processed at this time")
        elif location == "/get_location":
            length = int(self.headers['Content-Length'])
            data = self.rfile.read(length).decode('utf-8')
            post_data = parse_arguments(data)
            try:
                #post_data = json.loads(data)
                sql = """SELECT locationId FROM occupants WHERE firstName = '{0}' AND lastName = '{1}'""" \
                    .format(post_data["firstName"], post_data["lastName"])
                c = conn.cursor()
                result = c.execute(sql)
                location = -1
                for row in result:
                    location = row[0]
                c.close()
                self.send_response(200)
                self.send_header(b'Content-type', b'text/plain')
                self.end_headers()
                self.wfile.write(str(location).encode())
            except:
                print("Bad locate request")
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"This request cannot be processed at this time")
        else:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Bad Request")


server_address = ('0.0.0.0', 8443)
httpd = http.server.HTTPServer(server_address, MyHandler)
httpd.socket = ssl.wrap_socket(httpd.socket,
                               server_side=True,
                               certfile='hosting.pem',
                               ssl_version=ssl.PROTOCOL_TLSv1_2)
try:
    httpd.serve_forever()
finally:
    conn.close()
