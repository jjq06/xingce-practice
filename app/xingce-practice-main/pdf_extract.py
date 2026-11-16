import re, sys, zlib, codecs

path = sys.argv[1]
data = open(path, 'rb').read()

# index objects
objs = {}
for m in re.finditer(rb'(\d+)\s+(\d+)\s+obj\b', data):
    num = int(m.group(1))
    objs[num] = m.end()

def obj_body(num):
    start = objs[num]
    end = data.find(b'endobj', start)
    return data[start:end]

def get_stream(num):
    body = obj_body(num)
    i = body.find(b'stream')
    if i < 0:
        return None
    j = i + 6
    if body[j:j+2] == b'\r\n':
        j += 2
    elif body[j:j+1] in (b'\n', b'\r'):
        j += 1
    k = body.find(b'endstream', j)
    raw = body[j:k]
    filters = re.search(rb'/Filter\s*(\[[^\]]*\]|/\w+)', body)
    chain = []
    if filters:
        chain = re.findall(rb'/(\w+)', filters.group(1))
    if not chain:
        chain = [b'FlateDecode']
    data = raw
    for f in chain:
        if f == b'ASCII85Decode':
            import base64
            s = data.strip()
            if s.endswith(b'~>'):
                s = s[:-2]
            data = base64.a85decode(s, adobe=False)
        elif f == b'FlateDecode':
            try:
                data = zlib.decompress(data)
            except Exception:
                try:
                    data = zlib.decompressobj().decompress(data)
                except Exception:
                    pass
    return data

# find pages: objects with /Type /Page
pages = []
for num in sorted(objs):
    body = obj_body(num)
    if re.search(rb'/Type\s*/Page[^s]', body):
        pages.append((num, body))

def refs(body, key):
    m = re.search(rb'/' + key + rb'\s*(\[[^\]]*\]|\d+\s+\d+\s+R)', body, re.S)
    if not m:
        return []
    return [int(x) for x in re.findall(rb'(\d+)\s+\d+\s+R', m.group(1))]

# cmaps: map font object -> code->unicode
cmaps = {}
for num in sorted(objs):
    s = get_stream(num)
    if s and b'beginbfchar' in s:
        mapping = {}
        for blk in re.findall(rb'beginbfchar(.*?)endbfchar', s, re.S):
            for src, dst in re.findall(rb'<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>', blk):
                code = int(src, 16)
                txt = bytes.fromhex(dst.decode()).decode('utf-16-be', 'ignore')
                mapping[code] = txt
        for blk in re.findall(rb'beginbfrange(.*?)endbfrange', s, re.S):
            for lo, hi, dst in re.findall(rb'<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>', blk):
                lo_i, hi_i = int(lo, 16), int(hi, 16)
                base = int(dst, 16)
                for c in range(lo_i, hi_i + 1):
                    mapping[c] = chr(base + c - lo_i)
        cmaps[num] = mapping
    if s is None:
        pass

# fonts: /Font << /F1 5 0 R >>  or /Font 1 0 R
font_map = {}
for num in sorted(objs):
    body = obj_body(num)
    m = re.search(rb'/Font\s*(<<(.*?)>>|(\d+)\s+\d+\s+R)', body, re.S)
    if m:
        inner = m.group(2)
        if inner is None:
            inner = obj_body(int(m.group(3)))
        for name, ref in re.findall(rb'/([A-Za-z0-9+_.-]+)\s+(\d+)\s+\d+\s+R', inner):
            font_map[name.decode()] = int(ref)

def font_cmap(fontobj):
    body = obj_body(fontobj)
    m = re.search(rb'/ToUnicode\s+(\d+)\s+\d+\s+R', body)
    if m:
        return cmaps.get(int(m.group(1)))
    return None

def unescape_pdf(raw):
    out = bytearray()
    i = 0
    n = len(raw)
    while i < n:
        c = raw[i]
        if c == 0x5c and i + 1 < n:
            e = raw[i+1]
            if e == 0x6e: out.append(10); i += 2
            elif e == 0x72: out.append(13); i += 2
            elif e == 0x74: out.append(9); i += 2
            elif e == 0x62: out.append(8); i += 2
            elif e == 0x66: out.append(12); i += 2
            elif e in (0x28, 0x29, 0x5c): out.append(e); i += 2
            elif 0x30 <= e <= 0x37:
                j = i + 1
                oct_ = bytearray()
                while j < n and j < i + 4 and 0x30 <= raw[j] <= 0x37:
                    oct_.append(raw[j]); j += 1
                out.append(int(oct_.decode(), 8) & 0xFF); i = j
            elif e in (0x0a, 0x0d):
                i += 2
            else:
                out.append(e); i += 2
        else:
            out.append(c); i += 1
    return bytes(out)


def decode_string(b, cmap):
    if cmap is None:
        return ''.join(chr(c) for c in b)
    return ''.join(cmap.get(c, '') for c in b)

out_lines = []
for pnum, body in pages:
    contents = refs(body, b'Contents')
    out_lines.append('===== PAGE obj %d =====' % pnum)
    # merge streams
    buf = b''
    for c in contents:
        s = get_stream(c)
        if s:
            buf += s + b'\n'
    # tokenize
    cur_cmap = None
    italic = ''
    i = 0
    text = []
    for m in re.finditer(rb'/([A-Za-z0-9+_.-]+)\s+[\d.]+\s+Tf|\((?:[^()\\]|\\.)*\)\s*Tj|\[(?:[^\[\]\\]|\\.)*\]\s*TJ|<([0-9A-Fa-f\s]+)>\s*Tj|T\*|TD|Td|ET', buf, re.S):
        tok = m.group(0)
        if tok.endswith(b'Tf'):
            name = m.group(1).decode()
            f = font_map.get(name)
            cur_cmap = font_cmap(f) if f else None
        elif tok.endswith(b'Tj') or tok.endswith(b'TJ'):
            if tok.startswith(b'<'):
                hx = re.sub(rb'\s', b'', m.group(2))
                b = bytes.fromhex(hx.decode()) if len(hx) % 2 == 0 else b''
                text.append(decode_string(b, cur_cmap))
            else:
                for sm in re.finditer(rb'\((?:[^()\\]|\\.)*\)', tok):
                    raw = unescape_pdf(sm.group(0)[1:-1])
                    text.append(decode_string(raw, cur_cmap))
            if tok.endswith(b'TJ'):
                text.append('')
        elif tok in (b'T*', b'TD', b'Td', b'ET'):
            text.append('\n')
    out_lines.append(''.join(text))

print('\n'.join(out_lines))
