# Patch vosk-browser (node_modules) so its IDBFS persistence sync is skipped.
# Reason: Chrome's IndexedDB-based IDBFS fails on large Vosk model payloads
# ('Failed to sync file system: Error: FS error') during syncfs. Skipping
# persistence means the model is re-downloaded/extracted into memory per page
# session, which is fine for the assistant use-case.
# Re-run after `npm install` overwrites node_modules.
import re, base64

DIST = r'node_modules\vosk-browser\dist\vosk.js'
s = open(DIST, encoding='utf8', errors='replace').read()

m = re.search(r'''createBase64WorkerFactory\(\s*["'`]([A-Za-z0-9+/=_-]+)["'`]''', s)
if not m:
    raise SystemExit('worker factory not found - vosk-browser layout changed')
raw = base64.b64decode(m.group(1)).decode('utf8', errors='replace')

before_true = raw.count('return this.Vosk.syncFilesystem(true);')
before_false = raw.count('return this.Vosk.syncFilesystem(false);')
raw = raw.replace('return this.Vosk.syncFilesystem(true);', 'return Promise.resolve();')
raw = raw.replace('return this.Vosk.syncFilesystem(false);', 'return Promise.resolve();')
after = raw.count('return Promise.resolve();')

new_b64 = base64.b64encode(raw.encode('utf8')).decode('ascii')
s2 = s[:m.start(1)] + new_b64 + s[m.end(1):]
open(DIST, 'w', encoding='utf8', newline='').write(s2)
print(f'patched: syncfs(true)={before_true} syncfs(false)={before_false} -> resolve() x{after}')