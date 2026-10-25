"""أبعاد صورة JPEG/PNG/WebP بلا مكتبات خارجية (تعمل على الـCI دون تثبيت شيء)."""
import struct


def image_size(path):
    b = open(path, 'rb').read()
    if b[:8] == b'\x89PNG\r\n\x1a\n':
        return struct.unpack('>II', b[16:24])
    if b[:4] == b'RIFF' and b[8:12] == b'WEBP':
        kind = b[12:16]
        if kind == b'VP8 ':
            return struct.unpack('<H', b[26:28])[0] & 0x3FFF, struct.unpack('<H', b[28:30])[0] & 0x3FFF
        if kind == b'VP8L':
            w = 1 + (((b[22] & 0x3F) << 8) | b[21])
            h = 1 + (((b[24] & 0xF) << 10) | (b[23] << 2) | ((b[22] & 0xC0) >> 6))
            return w, h
        if kind == b'VP8X':
            return 1 + int.from_bytes(b[24:27], 'little'), 1 + int.from_bytes(b[27:30], 'little')
    if b[:2] == b'\xff\xd8':
        i = 2
        while i + 9 < len(b):
            if b[i] != 0xFF:
                i += 1
                continue
            m = b[i + 1]
            if m in (0xD8, 0x01) or 0xD0 <= m <= 0xD7:
                i += 2
                continue
            ln = struct.unpack('>H', b[i + 2:i + 4])[0]
            if m in (0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF):
                h, w = struct.unpack('>HH', b[i + 5:i + 9])
                return w, h
            i += 2 + ln
    raise ValueError('صيغة صورة غير مدعومة: ' + path)
