import zipfile
import struct
import sys

def get_methods(jar_path, class_path):
    print(f"--- Methods in {class_path} from {jar_path} ---")
    try:
        with zipfile.ZipFile(jar_path, 'r') as z:
            data = z.read(class_path)
    except Exception as e:
        print(f"Error: {e}")
        return
        
    cp_count = struct.unpack('>H', data[8:10])[0]
    pos = 10
    cp = [None] * cp_count
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            l = struct.unpack('>H', data[pos:pos+2])[0]
            pos += 2
            cp[i] = data[pos:pos+l].decode('utf-8', errors='ignore')
            pos += l
        elif tag in (7, 8, 16, 19, 20): pos += 2
        elif tag in (15,): pos += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        i += 1
    pos += 6
    if pos >= len(data): return
    interfaces_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2 + interfaces_count * 2
    fields_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(fields_count):
        pos += 6
        attr_count = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
        for _ in range(attr_count):
             pos += 2
             l = struct.unpack('>I', data[pos:pos+4])[0]
             pos += 4 + l
    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(methods_count):
        acc = struct.unpack('>H', data[pos:pos+2])[0]
        name = cp[struct.unpack('>H', data[pos+2:pos+4])[0]]
        desc = cp[struct.unpack('>H', data[pos+4:pos+6])[0]]
        print(f"{hex(acc)} {name}{desc}")
        pos += 6
        attr_count = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
        for _ in range(attr_count):
             pos += 2
             l = struct.unpack('>I', data[pos:pos+4])[0]
             pos += 4 + l

if __name__ == "__main__":
    if len(sys.argv) > 2:
        get_methods(sys.argv[1], sys.argv[2])
