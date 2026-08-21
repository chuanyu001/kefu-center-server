#!/usr/bin/env python3
"""测试 SOP 网关 queryCar1.0 的各种签名方式"""
import base64, hashlib, json, time, urllib.request
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

APP_KEY = "202201251485851319225810944"
PRIVATE_KEY = """MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCMz7+zmBgGfSbWm/s/LgXWDi3x5gIzE69gEF0KLLIdqh3Tbss2mP2uBnr25Swcfje+vDBLCSXx8yuRuvK3/iSqoBj33uMAP9rm6SkW5QB68+O10U9XeYqJvruBJnO5MT3zcMhsq6pVTodcGFgUVz6KXfIvrYU8tORps0VFpjJLjLfYnJxcyZ6nhsamA8/kxx5nycujRtmfAEXiqt8fFAXl6OuBQhJMzcgXMVhEGB4LIXw+IZUjjA/28Pp9HSuXvQRYQmSs9QFPfOiZKnYZ0PnZTzRG5dlQOPIXutoNc4JRISTWUqru6dGWuEYbB0d5fYYip9jBWFRVP8jd85prCQIjAgMBAAECggEAPj111sRbmkKncCTyITMOkVcjJ9ErF7iTxDp3ZY/sCwCuvk4ytKdbOhjTMV6K6O1BZqV4EzKv+wopL/kaYAmN2314i9eRLwMKlxZoex4t4Cr9c9hY4alvewM7jhKZBDqnz3sCnEYkvCBB5Is+I3+MvUxZSInCYtryO2ZqMCcOBCY4kfrTWst8uiPMIjpNBDyxIAjUu10PlY+rxcIQX2sHFOVY3F9vPflnTKzalpUXcW65VA1p/ZxawFplKYulWW8B7fiZ2FN1BHM+9KrceqlvMe+IYHbDB13f98PS1n7zO/3f6zDxurnld6lvcDYz4C4Rc+i9ar5coihdz1HgJbDsAQKBgQD69G3A+fGMyfDYu4BHkFTNgG2Mc6nESD/WzmZk3bRpqXbLdJJKs4ER3f+1WNmeh6o7OdYVDKp861yd2C9/9POCKra4w7E8lnVpa9Njw3RvRKjSzO0uCow/u2i/IX6cwN7smJVXH6Hk8W0+ReMH/oP4cIyYnSMSuzOnyFiND7RO4wKBgQCPpHQYNUDrkV4p0Lj8yJbj/GlIsXilwM5roz1IZlGgDfUmU5hGmLp11XjWJSYx+FjUQfSO6MzbQfjYwZ/7ewQWh84zXJkgEPnkfTaudkPZ3/SNl6wkZUTYoSXTJFu/gGmQdrcP+im7/b8yTtW8fW6duhN2RA9L606D/qGhwdWjwQKBgGpjCtA/ZXZg+bh3rIcqGblQ06AyPGsYke+3alMiZeRRUiooTghbFsGDUm8HrqH1M2aOO0KLLw9sG8RSrLhbGIw7HWEwnMdppXa+nkvxxT+SZNuQwo+9Kv2trlcwlONRJHA6szzPDSvoaX531CpEbJ/63q+oFgFP2TMszNVoLfEvAoGAOaplyQooA+oaCWN3wFOu79v3UG2e57wdomyoP5aEEmNIFZjduwm5YXDPz/id+tWeo6fOzEh9ZVB43FvvJABgxcLDby8vcgYerDHwb92eo6sa/HT1cK8PEoCvNLKV+q1Ms+hU2Z8ufgACb+niOIeKjtuS0JnGIk5W+PqSXOP6JoECgYEAoGMKIsFlsJm79oQLWD3J/QfQoFvlz1e41wJtl9RHLP1GKIOfbIt12rCguNIFZVLl4vgH+0y/4shdON9c8SZ9HmH4li+DXPnjU2E7R1nPaormm8286vruIupR5XKjFUO2VmRb/xYFYlWcItV9JRY/CgJ54EkJF5OG8DrX4+EUAv4="""

VIN = "LFWSRX9M6TAB29962"
URL = "https://sop.smartlink.com.cn/api/open-eop/queryCar1.0"

def rsa_sign(content):
    key = serialization.load_pem_private_key(
        b"-----BEGIN PRIVATE KEY-----\n" + PRIVATE_KEY.encode() + b"\n-----END PRIVATE KEY-----",
        password=None
    )
    sig = key.sign(content.encode(), padding.PKCS1v15(), hashes.SHA256())
    return base64.b64encode(sig).decode()

def call(label, url, body, headers=None):
    try:
        req = urllib.request.Request(url, data=json.dumps(body).encode(), headers=headers or {"Content-Type":"application/json"}, method="POST")
        with urllib.request.urlopen(req, timeout=10) as r:
            resp = r.read().decode()
        print(f"[{label}] {resp[:200]}")
    except Exception as e:
        print(f"[{label}] ERROR: {e}")

ts = str(int(time.time()))

# 方式1: 参数签名放query，appKey放query
params = {
    "appKey": APP_KEY,
    "method": "queryCar1.0",
    "version": "1.0",
    "signType": "RSA2",
    "timestamp": ts,
}
# 排序签名
sorted_items = sorted(params.items())
sign_content = "&".join(f"{k}={v}" for k,v in sorted_items)
sign = rsa_sign(sign_content)
q = "&".join(f"{k}={v}" for k,v in sorted_items) + f"&sign={sign}"
call("方式1-appKey在query", f"{URL}?{q}", {"req":{"carVin":VIN}})

# 方式2: appKey在URL路径，其他签名
params2 = {
    "method": "queryCar1.0",
    "version": "1.0",
    "signType": "RSA2",
    "timestamp": ts,
}
sorted2 = sorted(params2.items())
sign_content2 = "&".join(f"{k}={v}" for k,v in sorted2)
sign2 = rsa_sign(sign_content2)
q2 = "&".join(f"{k}={v}" for k,v in sorted2) + f"&sign={sign2}"
call("方式2-appKey在路径", f"https://sop.smartlink.com.cn/api/open-eop/{APP_KEY}?{q2}", {"req":{"carVin":VIN}})

# 方式3: 所有参数+sign都在body
body3 = dict(params)
body3["sign"] = sign
body3["req"] = {"carVin": VIN}
call("方式3-全部在body", URL, body3)

# 方式4: 签名内容包含body
body4 = json.dumps({"req":{"carVin":VIN}}, ensure_ascii=False)
sign_content4 = sign_content + "&" + body4
sign4 = rsa_sign(sign_content4)
q4 = "&".join(f"{k}={v}" for k,v in sorted_items) + f"&sign={sign4}"
call("方式4-签名含body", f"{URL}?{q4}", {"req":{"carVin":VIN}})
