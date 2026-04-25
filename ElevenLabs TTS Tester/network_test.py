import requests
import socket
import dns.resolver
from datetime import datetime

def test_network_connectivity():
    print(f"Network Connectivity Test - {datetime.now()}")
    print("=" * 50)
    
    # Test 1: Basic internet connectivity
    print("1. Testing basic internet connectivity...")
    try:
        response = requests.get("https://www.google.com", timeout=10)
        print(f"   ✓ Internet connection: OK (Status: {response.status_code})")
    except Exception as e:
        print(f"   ✗ Internet connection: FAILED - {e}")
        return
    
    # Test 2: DNS resolution for ElevenLabs
    print("\n2. Testing DNS resolution for api.elevenlabs.io...")
    try:
        ip_addresses = socket.gethostbyname_ex('api.elevenlabs.io')[2]
        print(f"   ✓ DNS resolution: OK - IPs: {ip_addresses}")
    except Exception as e:
        print(f"   ✗ DNS resolution: FAILED - {e}")
        
        # Try alternative DNS servers
        print("   Trying alternative DNS servers...")
        for dns_server in ['8.8.8.8', '1.1.1.1', '208.67.222.222']:
            try:
                resolver = dns.resolver.Resolver()
                resolver.nameservers = [dns_server]
                answers = resolver.resolve('api.elevenlabs.io', 'A')
                ips = [str(answer) for answer in answers]
                print(f"   ✓ DNS via {dns_server}: OK - IPs: {ips}")
                break
            except Exception as dns_e:
                print(f"   ✗ DNS via {dns_server}: FAILED - {dns_e}")
    
    # Test 3: Direct connection to ElevenLabs API
    print("\n3. Testing direct connection to ElevenLabs API...")
    try:
        response = requests.get("https://api.elevenlabs.io", timeout=10)
        print(f"   ✓ ElevenLabs API connection: OK (Status: {response.status_code})")
    except requests.exceptions.ConnectionError as e:
        print(f"   ✗ ElevenLabs API connection: CONNECTION ERROR - {e}")
    except requests.exceptions.Timeout as e:
        print(f"   ✗ ElevenLabs API connection: TIMEOUT - {e}")
    except Exception as e:
        print(f"   ✗ ElevenLabs API connection: FAILED - {e}")
    
    # Test 4: Check if behind proxy
    print("\n4. Checking proxy settings...")
    import os
    proxy_vars = ['HTTP_PROXY', 'HTTPS_PROXY', 'http_proxy', 'https_proxy']
    proxy_found = False
    for var in proxy_vars:
        if os.environ.get(var):
            print(f"   ! Proxy detected: {var} = {os.environ.get(var)}")
            proxy_found = True
    
    if not proxy_found:
        print("   ✓ No proxy environment variables detected")
    
    print("\n" + "=" * 50)
    print("Test completed. If ElevenLabs API connection failed:")
    print("1. Check your firewall/antivirus settings")
    print("2. Try using a VPN if in a restricted network")
    print("3. Contact your network administrator if behind corporate firewall")

if __name__ == "__main__":
    test_network_connectivity()