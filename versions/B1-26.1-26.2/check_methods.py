import os
import re

def check_file(filename):
    print(f"--- Strings in {filename} ---")
    with open(filename, 'rb') as f:
        data = f.read()
    # Constant pool strings often have these characters
    strs = re.findall(b'[a-zA-Z0-9_/]{5,}', data)
    for s in strs:
        s_dec = s.decode('utf-8', errors='ignore')
        if 'render' in s_dec.lower() or 'draw' in s_dec.lower() or 'background' in s_dec.lower() or 'text' in s_dec.lower():
            print(s_dec)

check_file('Screen.class')
