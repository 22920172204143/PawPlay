"""Serve local review assets only, bound to loopback. No publishing."""
import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT=Path(__file__).resolve().parents[2]
ALLOWED=[ROOT/p for p in ['tools/preview','.local/web/vendor','.local/reviews/p1',
                         'app/src/main/assets/models','app/src/main/assets/profiles']]
VIDEO=ROOT/'.local/reference/reference.mp4'


class Handler(SimpleHTTPRequestHandler):
    def __init__(self,*args,**kwargs):super().__init__(*args,directory=str(ROOT),**kwargs)
    def do_GET(self):
        if urlparse(self.path).path=='/':
            self.send_response(302);self.send_header('Location','/tools/preview/index.html');self.end_headers();return
        path=(ROOT/unquote(urlparse(self.path).path).lstrip('/')).resolve()
        if path!=VIDEO and not any(path.is_relative_to(p) for p in ALLOWED):
            self.send_error(404);return
        super().do_GET()
    def do_HEAD(self):
        path=(ROOT/unquote(urlparse(self.path).path).lstrip('/')).resolve()
        if path!=VIDEO and not any(path.is_relative_to(p) for p in ALLOWED):
            self.send_error(404);return
        super().do_HEAD()


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--port',type=int,default=8765);args=parser.parse_args()
    print(f'PawPlay review: http://127.0.0.1:{args.port}',flush=True)
    ThreadingHTTPServer(('127.0.0.1',args.port),Handler).serve_forever()
